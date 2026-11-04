#!/usr/bin/env python3
"""Crea /dev/rfcomm0 hacia la app Turtle Pad, sin las herramientas obsoletas de BlueZ.

    python3 tools/bt_rfcomm.py canal <MAC>        # pregunta el canal por SDP (sin sudo)
    sudo python3 tools/bt_rfcomm.py bind <MAC>    # crea /dev/rfcomm0 con ese canal
    sudo python3 tools/bt_rfcomm.py release       # lo quita

Android elige el canal RFCOMM al abrir el servidor y puede cambiarlo si la app se
reinicia, así que no se puede dejar fijo: se pregunta por SDP cada vez. Solo usa la
biblioteca estándar de Python y el kernel: SDP va sobre un socket L2CAP y el dispositivo
se crea con el ioctl RFCOMMCREATEDEV (ver net/bluetooth/rfcomm/tty.c).
"""

import argparse
import errno
import fcntl
import socket
import struct
import sys

NOMBRE_SERVICIO = 'TurtlePad'
UUID_SPP = 0x1101
UUID_RFCOMM = 0x0003
PSM_SDP = 1
ATRIBUTO_PROTOCOLOS = 0x0004
ATRIBUTO_NOMBRE = 0x0100

PDU_ERROR = 0x01
PDU_PETICION = 0x06
PDU_RESPUESTA = 0x07

# _IOW('R', 200, int) y _IOW('R', 201, int), de include/net/bluetooth/rfcomm.h
RFCOMMCREATEDEV = 0x400452C8
RFCOMMRELEASEDEV = 0x400452C9
RFCOMM_HANGUP_NOW = 1 << 2
SUFIJO_BASE_UUID = bytes.fromhex('00001000800000805F9B34FB')


# ---------- SDP (parte pura, para poder probarla) ----------
def decodificar_elemento(datos, i=0):
    """Decodifica un elemento de datos SDP. Devuelve (valor, índice siguiente).

    Las secuencias son listas; los UUID de 16, 32 o 128 bits (sobre la base de
    Bluetooth) quedan como entero, para poder compararlos sin importar el tamaño.
    """
    cabecera = datos[i]
    tipo, tamano = cabecera >> 3, cabecera & 7
    i += 1
    if tipo == 0:
        return None, i
    if tamano <= 4:
        largo = 1 << tamano
    else:
        bytes_largo = 1 << (tamano - 5)
        largo = int.from_bytes(datos[i:i + bytes_largo], 'big')
        i += bytes_largo
    crudo = datos[i:i + largo]
    fin = i + largo
    if len(crudo) != largo:
        raise ValueError('respuesta SDP truncada')

    if tipo in (1, 2):   # entero sin y con signo
        return int.from_bytes(crudo, 'big', signed=tipo == 2), fin
    if tipo == 3:        # UUID
        if largo == 16 and crudo[4:] == SUFIJO_BASE_UUID:
            return int.from_bytes(crudo[:4], 'big'), fin
        return int.from_bytes(crudo, 'big'), fin
    if tipo in (4, 8):   # texto o URL
        return crudo.split(b'\0')[0].decode('utf-8', errors='replace'), fin
    if tipo == 5:        # booleano
        return bool(crudo[0]), fin
    if tipo in (6, 7):   # secuencia o alternativa
        elementos = []
        j = i
        while j < fin:
            valor, j = decodificar_elemento(datos[:fin], j)
            elementos.append(valor)
        return elementos, fin
    raise ValueError(f'tipo de elemento SDP desconocido: {tipo}')


def _canal_de_protocolos(protocolos):
    for protocolo in protocolos or []:
        if len(protocolo) >= 2 and protocolo[0] == UUID_RFCOMM:
            return protocolo[1]
    return None


def canales_de_respuesta(lista_atributos):
    """Convierte la respuesta SDP en una lista de (nombre, canal RFCOMM), uno por servicio."""
    registros, _ = decodificar_elemento(lista_atributos)
    resultado = []
    for registro in registros:
        atributos = dict(zip(registro[0::2], registro[1::2]))
        canal = _canal_de_protocolos(atributos.get(ATRIBUTO_PROTOCOLOS))
        if canal is not None:
            resultado.append((atributos.get(ATRIBUTO_NOMBRE), canal))
    return resultado


def elegir_canal(servicios):
    """El canal del servicio de Turtle Pad; si nadie se llama así, el único SPP que haya."""
    for nombre, canal in servicios:
        if nombre == NOMBRE_SERVICIO:
            return canal
    return servicios[0][1] if len(servicios) == 1 else None


def peticion_sdp(id_transaccion, continuacion=b''):
    parametros = (
        b'\x35\x03\x19' + struct.pack('>H', UUID_SPP)            # servicios buscados
        + struct.pack('>H', 0xFFFF)                              # máximo de bytes
        + b'\x35\x06\x09' + struct.pack('>H', ATRIBUTO_PROTOCOLOS)
        + b'\x09' + struct.pack('>H', ATRIBUTO_NOMBRE)           # atributos pedidos
        + bytes([len(continuacion)]) + continuacion)
    return struct.pack('>BHH', PDU_PETICION, id_transaccion, len(parametros)) + parametros


def trozo_de_respuesta(respuesta):
    """Devuelve (bytes de atributos, estado de continuación) de una respuesta SDP."""
    pdu, _, _ = struct.unpack('>BHH', respuesta[:5])
    if pdu == PDU_ERROR:
        raise ValueError(f'el celular respondió error SDP {respuesta[5:7].hex()}')
    if pdu != PDU_RESPUESTA:
        raise ValueError(f'respuesta SDP inesperada: {pdu:#x}')
    cuantos = struct.unpack('>H', respuesta[5:7])[0]
    atributos = respuesta[7:7 + cuantos]
    largo_continuacion = respuesta[7 + cuantos]
    return atributos, respuesta[8 + cuantos:8 + cuantos + largo_continuacion]


def empaquetar_peticion_dev(id_dispositivo, mac, canal, banderas=0):
    """struct rfcomm_dev_req: dev_id, flags, src, dst, channel (con su relleno de C)."""
    dst = bytes.fromhex(mac.replace(':', ''))[::-1]   # bdaddr_t va al revés
    return struct.pack('=hxxI6s6sBxxx', id_dispositivo, banderas, bytes(6), dst, canal)


# ---------- Con el kernel ----------
def consultar_servicios(mac):
    with socket.socket(socket.AF_BLUETOOTH, socket.SOCK_SEQPACKET, socket.BTPROTO_L2CAP) as s:
        s.settimeout(15)
        s.connect((mac, PSM_SDP))
        atributos = b''
        continuacion = b''
        id_transaccion = 1
        while True:
            s.send(peticion_sdp(id_transaccion, continuacion))
            trozo, continuacion = trozo_de_respuesta(s.recv(4096))
            atributos += trozo
            id_transaccion += 1
            if not continuacion:
                return canales_de_respuesta(atributos)


def buscar_canal(mac):
    try:
        servicios = consultar_servicios(mac)
    except OSError as e:
        sys.exit(f'No se pudo consultar SDP en {mac}: {e}\n'
                 '¿Está encendido el Bluetooth del PC (bluetoothctl power on)? '
                 '¿El celular está cerca?')
    except (ValueError, IndexError, struct.error) as e:
        sys.exit(f'Respuesta SDP inválida de {mac}: {e}')
    canal = elegir_canal(servicios)
    if canal is None:
        sys.exit(f'{mac} no publica el servicio {NOMBRE_SERVICIO} (servicios SPP: {servicios}).\n'
                 'Abre la app en el celular y elige Bluetooth.')
    return canal


def _control():
    return socket.socket(socket.AF_BLUETOOTH, socket.SOCK_RAW, socket.BTPROTO_RFCOMM)


def liberar(id_dispositivo):
    """Quita /dev/rfcommN. Devuelve False si no existía."""
    peticion = empaquetar_peticion_dev(id_dispositivo, '00:00:00:00:00:00', 0, RFCOMM_HANGUP_NOW)
    with _control() as control:
        try:
            fcntl.ioctl(control, RFCOMMRELEASEDEV, peticion)
        except OSError as e:
            if e.errno == errno.ENODEV:
                return False
            raise
    return True


def crear(id_dispositivo, mac, canal):
    with _control() as control:
        fcntl.ioctl(control, RFCOMMCREATEDEV, empaquetar_peticion_dev(id_dispositivo, mac, canal))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = parser.add_subparsers(dest='orden', required=True)
    p_canal = sub.add_parser('canal', help='muestra el canal RFCOMM de la app')
    p_canal.add_argument('mac')
    p_bind = sub.add_parser('bind', help='crea /dev/rfcommN (requiere sudo)')
    p_bind.add_argument('mac')
    p_bind.add_argument('--canal', type=int, help='no preguntar por SDP y usar este canal')
    p_bind.add_argument('--dev', type=int, default=0)
    p_release = sub.add_parser('release', help='quita /dev/rfcommN (requiere sudo)')
    p_release.add_argument('--dev', type=int, default=0)
    args = parser.parse_args()

    try:
        if args.orden == 'canal':
            print(buscar_canal(args.mac))
        elif args.orden == 'bind':
            canal = args.canal or buscar_canal(args.mac)
            liberar(args.dev)   # puede haber quedado con un canal viejo
            crear(args.dev, args.mac, canal)
            print(f'/dev/rfcomm{args.dev} -> {args.mac} canal {canal}. '
                  'La conexión se abre cuando el nodo abra el archivo.')
        elif liberar(args.dev):
            print(f'/dev/rfcomm{args.dev} liberado')
        else:
            print(f'/dev/rfcomm{args.dev} no existía')
    except PermissionError:
        sys.exit('Falta permiso: esta orden necesita sudo.')


if __name__ == '__main__':
    main()
