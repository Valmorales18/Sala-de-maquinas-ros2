# Problemas

Busca lo que estás viendo en la tabla de esa conexión. Cada arreglo está pensado para resolverse rápido: si estás en plena charla y no sale en un minuto, pasa al siguiente transporte.

## Primero: ¿de qué lado está el problema?

Prueba el nodo sin celular.

1. **Terminal 1:**

   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   source install/setup.bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

2. **Terminal 2:**

   ```bash
   echo F | nc -u -w0 127.0.0.1 5005
   ```

Si la tortuga se mueve, el nodo está bien y el problema está en el celular o en la red.

**Último recurso**, para seguir con la charla moviendo la tortuga con el teclado. Corre cada comando en su propia terminal:

```bash
source /opt/ros/lyrical/setup.bash
ros2 run turtlesim turtlesim_node
```

```bash
source /opt/ros/lyrical/setup.bash
ros2 run turtlesim turtle_teleop_key
```

## Red: USB y Wi-Fi

| Lo que ves | Qué hacer |
|---|---|
| `Address already in use` | Otro proceso tiene el puerto. Míralo con `ss -ulpn \| grep 5005` y ciérralo. El nodo reintenta cada 2 s y abre solo cuando se libere. |
| La app marca 20 paq/s y al nodo no llega nada | Casi siempre es el firewall del PC. Revisa con `sudo ufw status` y permítelo con `sudo ufw allow 5005/udp`. |
| Cambia la IP o el nombre de la interfaz (`enp…` o `enx<MAC>`) | No afecta, porque el nodo escucha en `0.0.0.0`, también si la interfaz aparece después de lanzarlo. No fijes `udp_bind` a una IP: se rompe al cambiar y no recibe broadcast. |
| `ros2 topic hz` marca ~40 Hz en vez de 20 | El PC tiene dos interfaces en la misma subred (por ejemplo, cable y Wi-Fi) y el broadcast entra por las dos. La tortuga funciona igual. Para la demo, desconecta una o usa IP manual. |

## Anclaje USB

| Lo que ves | Qué hacer |
|---|---|
| `dmesg` no muestra nada y no aparece ninguna interfaz | El anclaje no está activo; se apaga al desconectar el cable. Actívalo de nuevo. Para comprobarlo: `adb shell getprop sys.usb.config`. Si dice solo `adb`, falta activarlo. |
| No se puede activar el anclaje | Revisa que el cable sea de datos. Si el PC ve el celular con `lsusb`, el cable sirve. |
| La interfaz aparece sin IP | Mírala con `nmcli device` y espera unos segundos. Si sigue igual, desactiva y activa el anclaje. |
| La app dice `No hay interfaz USB` | Elige **IP manual** y escribe la IP del PC que da `ip -br addr`. |
| `adb` dice `unauthorized` o desaparece al activar el anclaje | El celular cambió de modo USB. Acepta el aviso de depuración. No afecta al anclaje. |

**Si nada resuelve:** pasa a Bluetooth.

## Wi-Fi

| Lo que ves | Qué hacer |
|---|---|
| La app envía y al nodo no llega nada | Muchas redes bloquean el broadcast. Elige **IP manual** y escribe la IP del PC. |
| Tampoco llega con IP manual | La red aísla a los clientes. No tiene arreglo desde aquí: usa el anclaje USB. |

**Si nada resuelve:** usa el Wi-Fi como historia ("esto es lo que pasa en redes reales") y vuelve a USB.

## Bluetooth

| Lo que ves | Qué hacer |
|---|---|
| `no publica el servicio TurtlePad` | La app no está en modo Bluetooth. Al abrirla desde cero, la presentación puede preseleccionar USB o Wi-Fi: vuelve a elegir **Bluetooth**. |
| `No se pudo consultar SDP` | Enciende el Bluetooth del PC con `bluetoothctl power on`, revisa el del celular y acércalo. |
| `Falta permiso: esta orden necesita sudo` | `bind` y `release` requieren `sudo`; `canal` no. |
| `Sin permiso para abrir /dev/rfcomm0` o `Permission denied` | Falta el grupo `dialout` o no cerraste sesión tras agregarlo (verifica con `groups`). Para seguir ya: `sudo chmod 666 /dev/rfcomm0`. Solución de fondo: [permisos](transportes.md#permisos-de-devrfcomm0). |
| La app sigue en `esperando conexión` con el nodo corriendo | Cerraste la app y el canal pudo cambiar. Repite el `bind`; el nodo reintenta cada 2 s. |
| No conecta aunque el canal esté bien | Empareja de nuevo: `bluetoothctl remove <MAC>` y luego `bluetoothctl --timeout 45 pair <MAC>`. |

**Si nada resuelve:** vuelve a USB. Muestra la parte de `/dev` con el CYD si lo tienes conectado, o solo explícala.

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

| Lo que ves | Qué hacer |
|---|---|
| `Sin permiso para abrir` el puerto o `Permission denied` | Falta el grupo `dialout` o no cerraste sesión tras agregarlo (verifica con `groups`). |
| `No se pudo abrir /dev/esp32_tactil ... No such file or directory` | Lanzaste sin `port:=` y no tienes la regla udev. Agrega `port:=/dev/ttyUSB0`. |
| El puerto cambia de nombre (`ttyUSB0` → `ttyUSB1`) | Relanza con el número nuevo (`ls /dev/ttyUSB*`), o instala la [regla udev](firmware-cyd.md#regla-udev-opcional) para usar `/dev/esp32_tactil`. |
| Letras cortadas, o `Device or resource busy` | Hay dos lectores en el mismo puerto. No corras `cat /dev/ttyUSB0`, `screen` ni el monitor serial de Arduino con el nodo abierto. Para ver quién lo tiene: `fuser -v /dev/ttyUSB0`. |
| `lsusb` no muestra el CH340 | Cable solo de carga o USB-C con cable C a C. Prueba un cable USB-A a USB-C o el micro-USB. Para verlo en vivo al enchufar: `journalctl -kf`. |
| El puerto aparece y desaparece al instante | Es brltty. Ver [cómo quitarlo](firmware-cyd.md#brltty). |
| La pantalla queda en blanco o con colores raros | TFT_eSPI ignora el `User_Setup.h` junto al sketch. Ver [el paso de configuración](firmware-cyd.md#1-cargar-el-firmware). |

## ROS

| Lo que ves | Qué hacer |
|---|---|
| `ros2: command not found` o `Package 'cyd_teleop' not found` | La terminal no tiene el entorno cargado. Corre `cd ~/Sala-de-maquinas-ros2/ros2_ws`, `source /opt/ros/lyrical/setup.bash` y `source install/setup.bash`. |
| turtlesim no se mueve aunque hay mensajes | QoS o tipo distintos. Compara publicador y suscriptor con `ros2 topic info -v /turtle1/cmd_vel`. |
| `ros2 topic echo` no muestra nada desde otra terminal | Revisa que tenga el entorno cargado y el mismo `ROS_DOMAIN_ID` (`echo $ROS_DOMAIN_ID`). |
| `transport debe ser 'serial' o 'udp'` | Hay un error en el valor de `transport:=`. |
| `Línea ignorada` | Llegó algo que no es una letra válida. Míralo con `ros2 topic echo /tactil/raw`. |

## Nota para Arch

- **`Permission denied` en el puerto o en `/dev/rfcomm0`:** el grupo es `uucp`, no `dialout`. En Docker no aplica, porque el contenedor corre como root.
- **`nc: command not found`:** usa `echo F | ncat -u 127.0.0.1 5005`.
- **`ros2: command not found`:** ROS corre en Docker. Ver [la nota del nodo](nodo-ros.md#nota-para-arch).
