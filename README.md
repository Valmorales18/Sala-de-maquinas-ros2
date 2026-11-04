# La sala de máquinas de ROS 2 — joystick táctil para turtlesim

Material de la charla [**"La sala de máquinas de ROS 2"**](https://luma.com/dc5m2f13) (ROS MeetUp Cartagena 2026, 7 de noviembre, UTB · [diapositivas](https://www.canva.com/d/1zZUh7bgfFz6q4D)): una pantalla táctil ESP32-2432S028 (CYD) que mueve la tortuga de turtlesim por USB.

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

**brltty** (lector de braille) reclama el CH340 porque comparte VID:PID con un dispositivo braille: el puerto aparece y desaparece al instante. Se detecta con:

```bash
sudo dmesg | grep -iE 'ch341|brltty'
# ... usbfs: interface 0 claimed by ch341 while 'brltty' sets config #1
# ... ch341-uart converter now disconnected from ttyUSB0
```

Si no usas un display braille, quítalo: `sudo apt remove brltty` (o `sudo systemctl mask brltty-udev.service brltty.path`), y vuelve a conectar el CYD.

## ROS 2 (Lyrical Luth)

### Compilar (una vez, y cada vez que cambies el código)

```bash
cd ~/Documentos/ROS_MEETUP_CARTAGENA/ros2_ws
source /opt/ros/lyrical/setup.bash
colcon build
```

Pruebas: `colcon test --packages-select cyd_teleop && colcon test-result --verbose`.

### Lanzar la demo paso a paso

1. **Conecta el CYD** y comprueba que Linux lo ve:
   ```bash
   lsusb | grep 1a86      # debe salir 1a86:7523 ... CH340
   ls /dev/ttyUSB*        # normalmente /dev/ttyUSB0
   ```
   Si no aparece nada, es el cable (muchos solo cargan) o el conector: en algunos CYD el USB-C no funciona con cables C a C; usa uno **USB-A a USB-C** o el micro-USB.

2. **Cierra el Monitor Serie de Arduino** (y cualquier `cat`/`screen` sobre el puerto). Solo un programa puede leerlo a la vez; si no, el nodo dice `Device or resource busy`. Para ver quién lo tiene: `fuser -v /dev/ttyUSB0`.

3. **Lanza turtlesim + el driver** en una terminal:
   ```bash
   cd ~/Documentos/ROS_MEETUP_CARTAGENA/ros2_ws
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

### Lanzar los nodos por separado (para cambiar parámetros)

```bash
ros2 run turtlesim turtlesim_node                     # terminal 1
ros2 run cyd_teleop driver_tactil --ros-args \
  -p port:=/dev/ttyUSB0 -p linear_speed:=1.0 -p angular_speed:=1.5   # terminal 2
```

Parámetros de `driver_tactil`: `transport` (`serial`; o `udp`), `port` (`/dev/esp32_tactil`), `baud` (115200), `udp_port` (5005), `udp_bind` (`0.0.0.0`), `linear_speed` (2.0), `angular_speed` (2.0), `timeout_s` (0.5: si no llegan letras válidas, publica un Twist en cero), `cmd_topic` (`/turtle1/cmd_vel`).

### Probar la versión de red sin celular

Con `transport:=udp`, el nodo escucha datagramas con el mismo protocolo (una letra por línea). Para mover la tortuga desde la misma máquina:

```bash
ros2 launch cyd_teleop demo.launch.py transport:=udp   # terminal 1
echo F | nc -u -w0 127.0.0.1 5005                      # terminal 2: la tortuga avanza un poco
```

## App Android

Se dañó la pantalla del CYD, así que `android/` tiene **Turtle Pad**, una app que manda exactamente el mismo protocolo (una letra por línea, a 20 Hz). El CYD sigue siendo un transporte válido.

| Transporte | Lo que se ve en Linux | Nodo |
|---|---|---|
| USB (anclaje) | `usb0` o `enx…` (`rndis_host`/`cdc_ncm`) | `transport:=udp` |
| Wi-Fi | la interfaz Wi-Fi de siempre | `transport:=udp` |
| Bluetooth | `/dev/rfcomm0` | `port:=/dev/rfcomm0` (modo serial) |

### Compilar e instalar

Requiere JDK 17 a 25 y el Android SDK con la plataforma 37 (`sdkmanager 'platforms;android-37.0'`), con `ANDROID_HOME` definido.

```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Firmar el APK de release

Crea la llave una sola vez y **guárdala fuera de git** (`*.jks`, `*.keystore` y `keystore.properties` están ignorados):

```bash
cd android
keytool -genkeypair -v -keystore turtle-pad.jks -alias turtle-pad -keyalg RSA -keysize 4096 -validity 10000
cp keystore.properties.example keystore.properties   # y pon tus contraseñas
./gradlew assembleRelease
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs app/build/outputs/apk/release/turtle-pad-v1.0.apk
```

También se pueden usar las variables `TURTLEPAD_STORE_FILE`, `TURTLEPAD_STORE_PASSWORD`, `TURTLEPAD_KEY_ALIAS` y `TURTLEPAD_KEY_PASSWORD`. Sin llave, el APK de release sale **sin firmar** y Android no lo instala. Para publicarlo:

```bash
gh release create v1.0 android/app/build/outputs/apk/release/turtle-pad-v1.0.apk --title "Turtle Pad v1.0" --notes "App de la charla"
```

### USB (anclaje USB)

1. Conecta el celular y activa **Ajustes → Zona Wi-Fi y anclaje → Anclaje USB**. El celular asigna la IP por DHCP.
2. Míralo en Linux:
   ```bash
   sudo dmesg | grep -iE 'rndis|cdc_ncm'
   ip -br addr                 # aparece usb0 o enx<MAC> con una IP
   ```
3. Lanza el nodo y elige **USB (anclaje)** en la app:
   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```
   Debe salir `Llegan datos UDP desde <IP del celular>`.

### Wi-Fi

1. Conecta el PC y el celular a la **misma red**.
2. Lanza el mismo comando y elige **Wi-Fi** en la app. En modo automático envía al broadcast de la subred.
3. Si no llega nada, usa **IP manual** con la IP del PC (`ip -br addr`): muchas redes aíslan a los clientes o bloquean el broadcast. Si la red los aísla, el plan B es el anclaje USB.

### Bluetooth

Android elige el canal RFCOMM al abrir el servidor, así que hay que preguntarlo por SDP. `tools/bt_rfcomm.py` lo hace y crea el dispositivo con los sockets y el `ioctl` del kernel; **no necesita** `rfcomm` ni `sdptool`, que BlueZ marcó como obsoletos, ni instalar nada.

1. Con el Bluetooth activo en ambos lados, y abriendo los ajustes de Bluetooth del celular para que sea visible, empareja una sola vez:
   ```bash
   bluetoothctl --timeout 20 scan on
   bluetoothctl devices                 # copia la MAC del celular
   bluetoothctl --timeout 45 pair <MAC> # acepta el aviso en el celular
   ```
2. Elige **Bluetooth** en la app y concede el permiso. Debe decir `esperando conexión`.
3. Crea el dispositivo y míralo:
   ```bash
   python3 tools/bt_rfcomm.py canal <MAC>       # solo informa el canal
   sudo python3 tools/bt_rfcomm.py bind <MAC>
   ls -l /dev/rfcomm0
   ```
4. Lanza el nodo con el modo serial de siempre. Al abrir el archivo se conecta y la app dice `conectado`:
   ```bash
   ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0
   ```

**Permisos:** `/dev/rfcomm0` es del grupo `dialout` en Ubuntu y `uucp` en Arch. Agrégate a ese grupo y cierra sesión, o instala la regla opcional `udev/70-rfcomm.rules`.

**Si la app se reinicia**, el canal puede cambiar y `/dev/rfcomm0` queda apuntando al viejo: repite el `bind`, que siempre vuelve a preguntar. Pasar a segundo plano o cortar la conexión no cambia el canal, porque el servidor sigue abierto. Para quitar el dispositivo: `sudo python3 tools/bt_rfcomm.py release`.

### Errores típicos de la versión de red

- **`Address already in use`**: otro proceso tiene el puerto. Míralo con `ss -ulpn | grep 5005`. El nodo reintenta cada 2 s y abre solo cuando se libere.
- **La app marca 20 paq/s pero el nodo no recibe nada**: casi siempre es el firewall del PC. `sudo ufw status`; para permitirlo: `sudo ufw allow 5005/udp`.
- **Cambia la IP o el nombre de la interfaz** (`enx<MAC>` depende del celular): no afecta, porque el nodo escucha en `0.0.0.0`, también si la interfaz aparece después de lanzarlo. No fijes `udp_bind` a una IP: se rompe al cambiar y no recibe broadcast.
- **El broadcast no llega por Wi-Fi**: la red lo bloquea o aísla a los clientes. Usa IP manual o el anclaje USB.
- **`ros2 topic hz` marca ~40 Hz en vez de 20**: el PC tiene dos interfaces en la misma subred (p. ej. cable y Wi-Fi) y el broadcast entra por las dos. La tortuga funciona, pero para la demo desconecta una o usa IP manual.
- **Android 17 y `Android bloqueó el envío`**: falta el permiso de dispositivos cercanos, que ahora también cubre la red local. Concédelo desde el botón de la app o en Ajustes.
- **`No hay interfaz USB`** en la app: el anclaje no está activo, o el celular usa un nombre de interfaz no reconocido. Usa IP manual.

## Errores típicos

- **`Permission denied` en el puerto**: falta el grupo `dialout` o no cerraste sesión tras agregarlo (verifica con `groups`).
- **`No se pudo abrir /dev/esp32_tactil ... No such file or directory`**: lanzaste sin `port:=` y no tienes la regla udev; agrega `port:=/dev/ttyUSB0`.
- **El puerto cambia de nombre** (`ttyUSB0` → `ttyUSB1`): relanza con el número nuevo (`ls /dev/ttyUSB*`), o instala la regla udev para usar `/dev/esp32_tactil`.
- **Dos lectores en el mismo puerto**: no corras `cat /dev/ttyUSB0`, `screen` ni el monitor serial de Arduino con el nodo abierto; se reparten los bytes y llegan letras cortadas, o el nodo no abre (`Device or resource busy`, ver `fuser -v /dev/ttyUSB0`).
- **`lsusb` no muestra el CH340**: cable solo de carga o USB-C con cable C a C; prueba un cable USB-A a USB-C o el micro-USB. Para verlo en vivo al enchufar: `journalctl -kf`.
- **turtlesim no se mueve aunque hay mensajes**: QoS o tipo distintos; compara publicador y suscriptor con `ros2 topic info -v /turtle1/cmd_vel`.
