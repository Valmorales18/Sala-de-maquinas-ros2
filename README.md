# La sala de máquinas de ROS 2 — joystick táctil para turtlesim

Material de la charla [**"La sala de máquinas de ROS 2"**](ENLACE_A_LA_CHARLA) (ROS Meetup 2026): una pantalla táctil ESP32-2432S028 (CYD) que mueve la tortuga de turtlesim por USB.

```
pantalla → XPT2046 → ESP32 → USB (CH340) → kernel → /dev/ttyUSB0 → driver_tactil → /turtle1/cmd_vel → turtlesim
 (toque)   (táctil)  (F/B/L/R/S              (ch341)  (/dev/esp32_tactil)  (rclpy)   (geometry_msgs/Twist)
                      a 115200, ~20 Hz)
```

```
firmware/cyd_joystick_arcade/   sketch de Arduino + User_Setup.h de TFT_eSPI
ros2_ws/src/cyd_teleop/         paquete ROS 2 (nodo driver_tactil + demo.launch.py)
udev/99-esp32-tactil.rules      enlace estable /dev/esp32_tactil
```

## Firmware

Librerías (Gestor de librerías de Arduino): **TFT_eSPI** (Bodmer) y **XPT2046_Touchscreen** (Paul Stoffregen). Placa: *ESP32 Dev Module*.

> ⚠️ **TFT_eSPI IGNORA el `User_Setup.h` que está junto al sketch.** Hay que hacer una de dos cosas:
> - copiar `firmware/cyd_joystick_arcade/User_Setup.h` encima de `~/Arduino/libraries/TFT_eSPI/User_Setup.h`, o
> - copiarlo a `~/Arduino/libraries/TFT_eSPI/User_Setups/` y, en `User_Setup_Select.h`, comentar `#include <User_Setup.h>` e incluir ese archivo.
>
> Si la pantalla queda en blanco o con colores raros, casi siempre es esto. Ojo: al actualizar la librería se pierde el cambio.

**Variantes con ST7789** (algunos CYD, p. ej. los de dos puertos USB): cambia `ILI9341_2_DRIVER` por `ST7789_DRIVER` y agrega `#define TFT_INVERSION_ON`; si no, los colores salen invertidos.

El firmware envía una letra por línea (`\r\n`): `F` adelante, `B` atrás, `L` izquierda, `R` derecha, `S` stop.

## Linux

```bash
# 1. Permisos del puerto serial (después hay que CERRAR SESIÓN y volver a entrar)
sudo usermod -aG dialout $USER

# 2. Regla udev -> /dev/esp32_tactil
sudo cp udev/99-esp32-tactil.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules && sudo udevadm trigger
ls -l /dev/esp32_tactil
```

**brltty** (lector de braille) reclama el CH340 porque comparte VID:PID con un dispositivo braille: el puerto aparece y desaparece al instante. Se detecta con:

```bash
sudo dmesg | grep -iE 'ch341|brltty'
# ... usbfs: interface 0 claimed by ch341 while 'brltty' sets config #1
# ... ch341-uart converter now disconnected from ttyUSB0
```

Si no usas un display braille, quítalo: `sudo apt remove brltty` (o `sudo systemctl mask brltty-udev.service brltty.path`), y vuelve a conectar el CYD.

## ROS 2 (Lyrical Luth)

```bash
cd ros2_ws
source /opt/ros/lyrical/setup.bash
colcon build
source install/setup.bash
ros2 launch cyd_teleop demo.launch.py                     # usa /dev/esp32_tactil
ros2 launch cyd_teleop demo.launch.py port:=/dev/ttyUSB0  # sin la regla udev
```

En otras terminales (con el `source install/setup.bash`):

```bash
ros2 topic echo /tactil/raw        # la letra tal como llega del serial
ros2 topic echo /turtle1/cmd_vel   # el Twist que recibe turtlesim
```

Parámetros de `driver_tactil`: `port` (`/dev/esp32_tactil`), `baud` (115200), `linear_speed` (2.0), `angular_speed` (2.0), `timeout_s` (0.5: si no llegan letras válidas, publica un Twist en cero), `cmd_topic` (`/turtle1/cmd_vel`). Si el puerto no existe o se desconecta, el nodo reintenta cada 2 s.

Pruebas: `colcon test --packages-select cyd_teleop && colcon test-result --verbose`.

## Errores típicos

- **`Permission denied` en el puerto**: falta el grupo `dialout` o no cerraste sesión tras agregarlo (verifica con `groups`).
- **El puerto cambia de nombre** (`ttyUSB0` → `ttyUSB1`): usa `/dev/esp32_tactil` de la regla udev en vez de `ttyUSBx`.
- **Dos lectores en el mismo puerto**: no corras `cat /dev/ttyUSB0`, `screen` ni el monitor serial de Arduino con el nodo abierto; se reparten los bytes y llegan letras cortadas.
- **turtlesim no se mueve aunque hay mensajes**: QoS o tipo distintos; compara publicador y suscriptor con `ros2 topic info -v /turtle1/cmd_vel`.
