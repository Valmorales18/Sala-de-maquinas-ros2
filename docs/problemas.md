# Problemas

Errores típicos, por síntoma. Si estás en plena charla, ve al [plan B de la demo](demo-en-vivo.md#plan-b).

Para saber de qué lado está el problema, prueba el nodo sin celular:

```bash
echo F | nc -u -w0 127.0.0.1 5005
```

Si la tortuga se mueve, el nodo está bien y el problema está en el celular o en la red.

## Red: USB y Wi-Fi

- **`Address already in use`**: otro proceso tiene el puerto. Míralo con `ss -ulpn | grep 5005`. El nodo reintenta cada 2 s y abre solo cuando se libere.
- **La app marca 20 paq/s pero el nodo no recibe nada**: casi siempre es el firewall del PC. `sudo ufw status`; para permitirlo: `sudo ufw allow 5005/udp`.
- **Cambia la IP o el nombre de la interfaz** (`enp…` o `enx<MAC>` dependen del puerto USB y del celular): no afecta, porque el nodo escucha en `0.0.0.0`, también si la interfaz aparece después de lanzarlo. No fijes `udp_bind` a una IP: se rompe al cambiar y no recibe broadcast.
- **El broadcast no llega por Wi-Fi**: la red lo bloquea o aísla a los clientes. Usa IP manual o el anclaje USB.
- **`ros2 topic hz` marca ~40 Hz en vez de 20**: el PC tiene dos interfaces en la misma subred (p. ej. cable y Wi-Fi) y el broadcast entra por las dos. La tortuga funciona, pero para la demo desconecta una o usa IP manual.

## Anclaje USB

- **`dmesg` no muestra nada y no aparece ninguna interfaz**: el anclaje no está activo. Compruébalo con `adb shell getprop sys.usb.config`: si dice solo `adb`, falta activarlo. Se apaga al desconectar el cable.
- **No se puede activar el anclaje**: revisa que el cable sea de datos. Si el PC ve el celular con `lsusb`, el cable sirve.
- **La interfaz aparece sin IP**: mírala con `nmcli device` y espera unos segundos. Si sigue igual, desactiva y activa el anclaje.
- **`adb` dice `unauthorized` o desaparece al activar el anclaje**: el celular cambió de modo USB. Acepta el aviso de depuración. No afecta al anclaje.

## Bluetooth

- **`no publica el servicio TurtlePad`**: la app no está en modo Bluetooth. Al abrirla desde cero, la presentación puede preseleccionar USB o Wi-Fi: vuelve a elegir **Bluetooth**.
- **`No se pudo consultar SDP`**: enciende el Bluetooth del PC con `bluetoothctl power on`, revisa el del celular y acércalo.
- **`Falta permiso: esta orden necesita sudo`**: `bind` y `release` requieren `sudo`; `canal` no.
- **`Permission denied` en `/dev/rfcomm0`**: falta el grupo `dialout` o no cerraste sesión tras agregarlo (verifica con `groups`). También puedes usar la regla `udev/70-rfcomm.rules`: ver [permisos](transportes.md#permisos-de-devrfcomm0).
- **La app sigue en `esperando conexión` con el nodo corriendo**: cerraste la app y el canal pudo cambiar. Repite el `bind`; el nodo reintenta cada 2 s.
- **No conecta aunque el canal esté bien**: empareja de nuevo con `bluetoothctl remove <MAC>` y luego `bluetoothctl --timeout 45 pair <MAC>`.

## App

Estos mensajes salen en la barra de estado:

| Mensaje | Qué hacer |
|---|---|
| `No hay interfaz USB` | El anclaje no está activo, o el celular usa un nombre de interfaz no reconocido. Usa IP manual. |
| `No hay interfaz Wi-Fi con IP` | Conecta el celular a la red, o usa IP manual. |
| `La red no tiene broadcast` | Usa IP manual. |
| `IP inválida` | Escribe una IPv4, como `192.168.1.10`. No acepta nombres. |
| `Red inalcanzable` | Revisa la conexión del celular y la IP. |
| `Falta el permiso de red local` | Android 17: toca **Conceder permiso**, o ve a Ajustes › Apps › Turtle Pad › Permisos. |
| `Android bloqueó el envío` | Falta el permiso de dispositivos cercanos, que ahora también cubre la red local. Concédelo desde el botón de la app o en Ajustes. |
| `Falta el permiso de Bluetooth` | Toca **Conceder permiso**, o ve a Ajustes › Apps › Turtle Pad › Permisos. |
| `El Bluetooth está apagado` | Actívalo en el celular. |
| `No se pudo abrir el servidor Bluetooth` | La app reintenta sola cada 2 s. Si sigue, apaga y prende el Bluetooth. |

## Serial y CYD

- **`Permission denied` en el puerto**: falta el grupo `dialout` o no cerraste sesión tras agregarlo (verifica con `groups`).
- **`No se pudo abrir /dev/esp32_tactil ... No such file or directory`**: lanzaste sin `port:=` y no tienes la regla udev; agrega `port:=/dev/ttyUSB0`.
- **El puerto cambia de nombre** (`ttyUSB0` → `ttyUSB1`): relanza con el número nuevo (`ls /dev/ttyUSB*`), o instala la regla udev para usar `/dev/esp32_tactil`.
- **Dos lectores en el mismo puerto**: no corras `cat /dev/ttyUSB0`, `screen` ni el monitor serial de Arduino con el nodo abierto; se reparten los bytes y llegan letras cortadas, o el nodo no abre (`Device or resource busy`, ver `fuser -v /dev/ttyUSB0`).
- **`lsusb` no muestra el CH340**: cable solo de carga o USB-C con cable C a C; prueba un cable USB-A a USB-C o el micro-USB. Para verlo en vivo al enchufar: `journalctl -kf`.
- **El puerto aparece y desaparece al instante**: es brltty. Ver [cómo quitarlo](firmware-cyd.md#brltty).
- **La pantalla queda en blanco o con colores raros**: TFT_eSPI ignora el `User_Setup.h` junto al sketch. Ver [firmware](firmware-cyd.md#firmware).

## ROS

- **turtlesim no se mueve aunque hay mensajes**: QoS o tipo distintos; compara publicador y suscriptor con `ros2 topic info -v /turtle1/cmd_vel`.
- **`ros2 topic echo` no muestra nada desde otra terminal**: revisa que tenga el entorno cargado y el mismo `ROS_DOMAIN_ID` (`echo $ROS_DOMAIN_ID`).
- **`transport debe ser 'serial' o 'udp'`**: hay un error en el valor de `transport:=`.
- **`Línea ignorada`**: llegó algo que no es una letra válida. Míralo con `ros2 topic echo /tactil/raw`.

## Nota para Arch

- **`Permission denied` en el puerto o en `/dev/rfcomm0`**: el grupo es `uucp`, no `dialout`. En Docker no aplica, porque el contenedor corre como root.
- **`nc: command not found`**: usa `echo F | ncat -u 127.0.0.1 5005`.
- **`ros2: command not found`**: ROS corre en Docker. Ver [la nota del nodo](nodo-ros.md#nota-para-arch).
