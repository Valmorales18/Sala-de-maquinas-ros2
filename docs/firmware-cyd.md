# CYD (ESP32-2432S028) por serial

El transporte original: una pantalla táctil que envía las letras por USB. Se dañó la pantalla del CYD usado en la charla y por eso existe [la app](app-android.md), pero el firmware y el modo serial siguen siendo válidos.

```
pantalla → XPT2046 → ESP32 → USB (CH340) → kernel → /dev/ttyUSB0 → driver_tactil → /turtle1/cmd_vel → turtlesim
 (toque)   (táctil)  (F/B/L/R/S              (ch341)  (/dev/esp32_tactil)  (rclpy)   (geometry_msgs/Twist)
                      a 115200, ~20 Hz)
```

Son tres pasos: cargar el firmware, preparar Linux y lanzar. Si algo falla, ve a [problemas](problemas.md#serial-y-cyd).

## 1. Cargar el firmware

Solo hace falta si el CYD todavía no lo tiene.

1. En el Gestor de librerías de Arduino instala **TFT_eSPI** (Bodmer) y **XPT2046_Touchscreen** (Paul Stoffregen).
2. Copia la configuración de la pantalla encima de la de la librería. **TFT_eSPI ignora el `User_Setup.h` que está junto al sketch**, y si te saltas esto la pantalla queda en blanco o con colores raros:

   ```bash
   cd ~/Sala-de-maquinas-ros2
   cp firmware/cyd_joystick_arcade/User_Setup.h ~/Arduino/libraries/TFT_eSPI/User_Setup.h
   ```

   Al actualizar la librería se pierde este cambio, así que hay que repetirlo. Otra opción es copiarlo a `~/Arduino/libraries/TFT_eSPI/User_Setups/` y, en `User_Setup_Select.h`, comentar `#include <User_Setup.h>` e incluir ese archivo.

3. Solo en las variantes con ST7789 (algunos CYD, por ejemplo los de dos puertos USB): en ese archivo cambia `ILI9341_2_DRIVER` por `ST7789_DRIVER` y agrega `#define TFT_INVERSION_ON`. Si no, los colores salen invertidos.
4. Abre `firmware/cyd_joystick_arcade/cyd_joystick_arcade.ino`, elige la placa *ESP32 Dev Module* y súbelo.

El firmware envía una letra por línea (`\r\n`): `F` adelante, `B` atrás, `L` izquierda, `R` derecha, `S` stop.

## 2. Preparar Linux (una sola vez)

1. Compila el paquete: ver [nodo ROS](nodo-ros.md#instalar-y-compilar).
2. Dale permiso a tu usuario para el puerto serial, y después **cierra sesión y vuelve a entrar**:

   ```bash
   sudo usermod -aG dialout $USER
   ```

3. Quita brltty si el puerto aparece y desaparece al instante: ver [brltty](#brltty).

## 3. Lanzar la demo

1. **Conecta el CYD** y comprueba que Linux lo ve:

   ```bash
   lsusb | grep 1a86
   ls /dev/ttyUSB*
   ```

   Debe salir `1a86:7523 ... CH340` y normalmente `/dev/ttyUSB0`. Si no aparece nada, es el cable (muchos solo cargan) o el conector: en algunos CYD el USB-C no funciona con cables C a C. Usa uno **USB-A a USB-C** o el micro-USB.

2. **Cierra el Monitor Serie de Arduino** y cualquier `cat` o `screen` sobre el puerto. Solo un programa puede leerlo a la vez; si no, el nodo dice `Device or resource busy`. Para ver quién lo tiene: `fuser -v /dev/ttyUSB0`.

3. **Terminal 1:** lanza turtlesim y el driver. Si el CYD salió con otro número en el paso 1 (`ttyUSB1`…), usa ese:

   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   source install/setup.bash
   ros2 launch cyd_teleop demo.launch.py port:=/dev/ttyUSB0
   ```

   Debe abrirse la ventana de turtlesim y salir `Puerto /dev/ttyUSB0 abierto`.

4. **Toca la pantalla:** FWD avanza, BACK retrocede, LEFT y RIGHT giran. Al soltar, la tortuga se detiene.

5. Opcional, para ver qué pasa por dentro, en la **terminal 2**:

   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   source install/setup.bash
   ros2 topic echo /tactil/raw
   ```

   Más comandos en [ver qué pasa por dentro](nodo-ros.md#ver-qué-pasa-por-dentro).

6. **Para cerrar:** Ctrl+C en la terminal 1. La tortuga recibe un Twist en cero y el puerto queda libre.

Si desconectas el USB en plena demo, la tortuga frena en 0,5 s y el nodo reintenta abrir el puerto cada 2 s hasta que vuelvas a conectarlo.

## Regla udev (opcional)

Sin ella todo funciona, pero **siempre hay que indicar el puerto** al lanzar. Si lo olvidas, el nodo busca `/dev/esp32_tactil`, que no existe, y se queda reintentando cada 2 s sin mover la tortuga.

| | Sin regla udev | Con regla udev |
|---|---|---|
| Comando | `ros2 launch cyd_teleop demo.launch.py port:=/dev/ttyUSB0` | `ros2 launch cyd_teleop demo.launch.py` |
| Si el puerto cambia a `ttyUSB1` | Hay que relanzar con el número nuevo (`ls /dev/ttyUSB*`) | El enlace `/dev/esp32_tactil` lo sigue solo |

Para instalarla:

```bash
cd ~/Sala-de-maquinas-ros2
sudo cp udev/99-esp32-tactil.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules && sudo udevadm trigger
ls -l /dev/esp32_tactil
```

Para quitarla:

```bash
sudo rm /etc/udev/rules.d/99-esp32-tactil.rules
sudo udevadm control --reload-rules
```

## brltty

**brltty** (lector de braille) reclama el CH340 porque comparte VID:PID con un dispositivo braille: el puerto aparece y desaparece al instante. Se detecta con:

```bash
sudo dmesg | grep -iE 'ch341|brltty'
```

```
... usbfs: interface 0 claimed by ch341 while 'brltty' sets config #1
... ch341-uart converter now disconnected from ttyUSB0
```

Si no usas un display braille, quítalo y vuelve a conectar el CYD:

```bash
sudo apt remove brltty
```

Si prefieres no desinstalarlo: `sudo systemctl mask brltty-udev.service brltty.path`.

## Nota para Arch

- El grupo del puerto serial es `uucp`, no `dialout`: `sudo usermod -aG uucp $USER`.
- La regla `udev/99-esp32-tactil.rules` usa `GROUP="dialout"`, que no existe en Arch.
- ROS corre en Docker: ver [la nota del nodo](nodo-ros.md#nota-para-arch). Para el modo serial, pasa el puerto con `--device /dev/ttyUSB0`.
