"""Pruebas de la parte pura de bt_rfcomm.py (decodificar SDP y armar el ioctl)."""

import pytest

from bt_rfcomm import (
    canales_de_respuesta,
    decodificar_elemento,
    elegir_canal,
    empaquetar_peticion_dev,
    peticion_sdp,
    trozo_de_respuesta,
)


def servicio(canal, nombre=None, uuid_rfcomm=b'\x19\x00\x03'):
    protocolos = (b'\x35\x03\x19\x01\x00'                       # L2CAP
                  + b'\x35' + bytes([len(uuid_rfcomm) + 2]) + uuid_rfcomm + b'\x08' + bytes([canal]))
    protocolos = b'\x35' + bytes([len(protocolos)]) + protocolos
    cuerpo = b'\x09\x00\x04' + protocolos
    if nombre:
        texto = nombre.encode()
        cuerpo += b'\x09\x01\x00\x25' + bytes([len(texto)]) + texto
    return b'\x35' + bytes([len(cuerpo)]) + cuerpo


def lista(*servicios):
    cuerpo = b''.join(servicios)
    return b'\x36' + len(cuerpo).to_bytes(2, 'big') + cuerpo


def test_decodifica_tipos_basicos():
    assert decodificar_elemento(b'\x08\x05') == (5, 2)
    assert decodificar_elemento(b'\x09\x01\x00') == (256, 3)
    assert decodificar_elemento(b'\x25\x02hi') == ('hi', 4)
    assert decodificar_elemento(b'\x19\x00\x03') == (3, 3)
    assert decodificar_elemento(b'\x00') == (None, 1)


def test_uuid_de_128_bits_equivale_al_corto():
    uuid = bytes.fromhex('0000000300001000800000805F9B34FB')
    assert decodificar_elemento(b'\x1c' + uuid)[0] == 3


def test_extrae_el_canal_de_un_servicio():
    assert canales_de_respuesta(lista(servicio(5, 'TurtlePad'))) == [('TurtlePad', 5)]


def test_rfcomm_con_uuid_largo():
    uuid = b'\x1c' + bytes.fromhex('0000000300001000800000805F9B34FB')
    assert canales_de_respuesta(lista(servicio(7, uuid_rfcomm=uuid))) == [(None, 7)]


def test_elige_turtlepad_entre_varios_servicios_spp():
    servicios = canales_de_respuesta(lista(servicio(3, 'Otro'), servicio(12, 'TurtlePad')))
    assert elegir_canal(servicios) == 12


@pytest.mark.parametrize('servicios, esperado', [
    ([], None),
    ([('Otro', 4)], 4),                      # uno solo: no hay duda
    ([('A', 4), ('B', 5)], None),            # varios sin Turtle Pad: no adivina
])
def test_elegir_canal_sin_nombre(servicios, esperado):
    assert elegir_canal(servicios) == esperado


def test_respuesta_truncada_falla_con_error_claro():
    with pytest.raises(ValueError):
        decodificar_elemento(b'\x35\x10\x08\x05')


def test_trozo_de_respuesta_con_continuacion():
    assert trozo_de_respuesta(b'\x07\x00\x01\x00\x07\x00\x02ab\x01\x09') == (b'ab', b'\x09')
    assert trozo_de_respuesta(b'\x07\x00\x01\x00\x05\x00\x02ab\x00') == (b'ab', b'')
    with pytest.raises(ValueError):
        trozo_de_respuesta(b'\x01\x00\x01\x00\x02\x00\x03')


def test_peticion_sdp_declara_bien_su_largo():
    peticion = peticion_sdp(1, b'\x09')
    assert peticion[0] == 0x06
    assert int.from_bytes(peticion[3:5], 'big') == len(peticion) - 5
    assert peticion[-2:] == b'\x01\x09'


def test_estructura_del_ioctl_coincide_con_rfcomm_dev_req():
    crudo = empaquetar_peticion_dev(0, 'AA:BB:CC:DD:EE:FF', 5)
    assert len(crudo) == 24                                   # con el relleno de C
    assert crudo[14:20] == bytes.fromhex('FFEEDDCCBBAA')      # bdaddr_t va al revés
    assert crudo[20] == 5
