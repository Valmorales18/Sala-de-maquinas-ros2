# CYD (ESP32-2432S028) por serial

El transporte original: una pantalla táctil que envía las letras por USB. Se dañó la pantalla del CYD usado en la charla y por eso existe [la app](app-android.md), pero el firmware y el modo serial siguen siendo válidos.

```
pantalla → XPT2046 → ESP32 → USB (CH340) → kernel → /dev/ttyUSB0 → driver_tactil → /turtle1/cmd_vel → turtlesim
 (toque)   (táctil)  (F/B/L/R/S              (ch341)  (/dev/esp32_tactil)  (rclpy)   (geometry_msgs/Twist)
                      a 115200, ~20 Hz)
```

Los errores típicos están en [problemas](problemas.md#serial-y-cyd).

## Firmware

Está en `firmware/cyd_joystick_arcade/`: el sketch de Arduino y el `User_Setup.h` de TFT_eSPI.

Librerías (Gestor de librerías de Arduino): **TFT_eSPI** (Bodmer) y **XPT2046_Touchscreen** (Paul Stoffregen). Placa: *ESP32 Dev Module*.

> ⚠️ **TFT_eSPI IGNORA el `User_Setup.h` que está junto al sketch.** Hay que hacer una de dos cosas:
> - copiar `firmware/cyd_joystick_arcade/User_Setup.h` encima de `~/Arduino/libraries/TFT_eSPI/User_Setup.h`, o
> - copiarlo a `~/Arduino/libraries/TFT_eSPI/User_Setups/` y, en `User_Setup_Select.h`, comentar `#include <User_Setup.h>` e incluir ese archivo.
>
> Si la pantalla queda en blanco o con colores raros, casi siempre es esto. Ojo: al actualizar la librería se pierde el cambio.

**Variantes con ST7789** (algunos CYD, p. ej. los de dos puertos USB): cambia `ILI9341_2_DRIVER` por `ST7789_DRIVER` y agrega `#define TFT_INVERSION_ON`; si no, los colores salen invertidos.

El firmware envía una letra por línea (`\r\n`): `F` adelante, `B` atrás, `L` izquierda, `R` derecha, `S` stop.

## Linux

Permisos del puerto serial (después hay que CERRAR SESIÓN y volver a entrar):

```bash
sudo usermod -aG dialout $USER
```

### Regla udev (opcional)

> ℹ️ **La regla udev NO es obligatoria.** Sin ella todo funciona, pero **siempre hay que indicar el puerto** al lanzar: `port:=/dev/ttyUSB0`. Si lo olvidas, el nodo busca `/dev/esp32_tactil`, que no existe, y se queda reintentando cada 2 s sin mover la tortuga.

| | Sin regla udev | Con regla udev |
|---|---|---|
| Comando | `ros2 launch cyd_teleop demo.launch.py port:=/dev/ttyUSB0` | `ros2 launch cyd_teleop demo.launch.py` |
| Si el puerto cambia a `ttyUSB1` | Hay que relanzar con el número nuevo (`ls /dev/ttyUSB*`) | El enlace `/dev/esp32_tactil` lo sigue solo |

Para instalarla (requiere `sudo`, cada comando en **una sola línea**):

```bash
sudo cp udev/99-esp32-tactil.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules && sudo udevadm trigger
ls -l /dev/esp32_tactil
```

Para quitarla: `sudo rm /etc/udev/rules.d/99-esp32-tactil.rules && sudo udevadm control --reload-rules`.

### brltty

**brltty** (lector de braille) reclama el CH340 porque comparte VID:PID con un dispositivo braille: el puerto aparece y desaparece al instante. Se detecta con:

```bash
sudo dmesg | grep -iE 'ch341|brltty'
# ... usbfs: interface 0 claimed by ch341 while 'brltty' sets config #1
# ... ch341-uart converter now disconnected from ttyUSB0
```

Si no usas un display braille, quítalo: `sudo apt remove brltty` (o `sudo systemctl mask brltty-udev.service brltty.path`), y vuelve a conectar el CYD.

## Lanzar la demo paso a paso

Antes, compila el paquete: ver [nodo ROS](nodo-ros.md#instalar-y-compilar).

1. **Conecta el CYD** y comprueba que Linux lo ve:
   ```bash
   lsusb | grep 1a86      # debe salir 1a86:7523 ... CH340
   ls /dev/ttyUSB*        # normalmente /dev/ttyUSB0
   ```
   Si no aparece nada, es el cable (muchos solo cargan) o el conector: en algunos CYD el USB-C no funciona con cables C a C; usa uno **USB-A a USB-C** o el micro-USB.

2. **Cierra el Monitor Serie de Arduino** (y cualquier `cat`/`screen` sobre el puerto). Solo un programa puede leerlo a la vez; si no, el nodo dice `Device or resource busy`. Para ver quién lo tiene: `fuser -v /dev/ttyUSB0`.

3. **Lanza turtlesim + el driver** en una terminal:
   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   source install/setup.bash
   ros2 launch cyd_teleop demo.launch.py port:=/dev/ttyUSB0
   ```
   Debe abrirse la ventana de turtlesim y salir `Puerto /dev/ttyUSB0 abierto`.

   > ⚠️ **No olvides `port:=/dev/ttyUSB0`** si no instalaste la regla udev. Si el CYD salió con otro número en el paso 1 (`ttyUSB1`...), usa ese. Solo con la regla instalada puedes omitir `port:=`.

4. **Toca la pantalla**: FWD avanza, BACK retrocede, LEFT/RIGHT giran. Al soltar, la tortuga se detiene.

5. **Mira qué pasa por dentro** en otra terminal (con los mismos dos `source`):
   ```bash
   ros2 topic echo /tactil/raw          # la letra tal como llega del serial
   ros2 topic echo /turtle1/cmd_vel     # el Twist que recibe turtlesim
   ros2 topic hz /turtle1/cmd_vel       # ~20 Hz, el ritmo del firmware
   rqt_graph                            # el grafo de nodos y tópicos
   ```
   Todas las terminales deben tener el mismo `ROS_DOMAIN_ID` (compruébalo con `echo $ROS_DOMAIN_ID`).

6. **Para cerrar**: `Ctrl+C` en la terminal del launch. La tortuga recibe un Twist en cero y el puerto queda libre.

Si desconectas el USB en plena demo, la tortuga frena en 0,5 s y el nodo reintenta abrir el puerto cada 2 s hasta que vuelvas a conectarlo.

## Nota para Arch

- El grupo del puerto serial es `uucp`, no `dialout`: `sudo usermod -aG uucp $USER`.
- La regla `udev/99-esp32-tactil.rules` usa `GROUP="dialout"`, que no existe en Arch.
- ROS corre en Docker: ver [la nota del nodo](nodo-ros.md#nota-para-arch). Para el modo serial, pasa el puerto con `--device /dev/ttyUSB0`.
