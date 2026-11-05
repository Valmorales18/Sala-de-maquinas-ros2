# La sala de máquinas de ROS 2 — mover una tortuga, viendo Linux por debajo

Material de la charla [**"La sala de máquinas de ROS 2"**](https://luma.com/dc5m2f13) (ROS MeetUp Cartagena 2026, 7 de noviembre, UTB · [diapositivas](https://www.canva.com/d/1zZUh7bgfFz6q4D)).

Un control envía una letra por línea (`F`, `B`, `L`, `R`, `S`) a ~20 Hz y un nodo de ROS 2 la convierte en movimiento de la tortuga de turtlesim. La charla trata de lo que pasa en el medio: cada transporte pasa por el kernel de forma visible en `dmesg`, `ip` y `/dev`.

```
                                            kernel y /dev                    ROS 2
App por USB      celular ─ RNDIS ──── rndis_host, interfaz enp…/enx… ─┐
                                                                      ├─ UDP 5005 ─┐
App por Wi-Fi    celular ─ red ────── interfaz Wi-Fi ─────────────────┘            │
                                                                                   ├─ driver_tactil ─ /turtle1/cmd_vel ─ turtlesim
App por BT       celular ─ RFCOMM ─── /dev/rfcomm0 ──────────────── serial ────────┤
                                                                                   │
CYD por USB      ESP32 ─── CH340 ──── ch341, /dev/ttyUSB0 ───────── serial ────────┘
```

## Probarlo sin celular

Necesitas **Kubuntu 26.04 con ROS 2 Lyrical**.

1. Instala lo necesario, clona el repo y compila:

   ```bash
   sudo apt install ros-lyrical-turtlesim python3-serial netcat-openbsd
   git clone https://github.com/Valmorales18/Sala-de-maquinas-ros2.git ~/Sala-de-maquinas-ros2
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   colcon build
   ```

2. **Terminal 1:** lanza la demo.

   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   source install/setup.bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

   Se abre la ventana de turtlesim.

3. **Terminal 2:** manda una letra.

   ```bash
   echo F | nc -u -w0 127.0.0.1 5005
   ```

   La tortuga avanza un poco y se detiene sola.

## Probarlo con el celular

Sigue [**Cómo correr las pruebas**](docs/correr-pruebas.md), la guía principal: instalar la app y conectar por cable, Wi-Fi y Bluetooth, paso a paso.

## Documentación

| Quiero… | Documento |
|---|---|
| **Correr las pruebas con el celular** | [**Cómo correr las pruebas**](docs/correr-pruebas.md) |
| Ver qué pasa en Linux con cada conexión, y qué mostrar en la charla | [Transportes](docs/transportes.md) |
| Arreglar algo que no funciona | [Problemas](docs/problemas.md) |
| Instalar, compilar o publicar la app | [App Android](docs/app-android.md) |
| Cambiar parámetros o entender el nodo | [Nodo ROS](docs/nodo-ros.md) |
| Usar la pantalla CYD por serial | [CYD por serial](docs/firmware-cyd.md) |

La documentación está pensada para Kubuntu 26.04. Cada documento termina con una nota para Arch cuando algo cambia allí.

## Estructura

```
android/                        app Turtle Pad (Kotlin + Jetpack Compose)
docker/Dockerfile               ROS 2 Lyrical con turtlesim, para probar sin ROS nativo
docs/                           documentación e ícono de la app
firmware/cyd_joystick_arcade/   sketch de Arduino + User_Setup.h de TFT_eSPI
ros2_ws/src/cyd_teleop/         paquete ROS 2 (nodo driver_tactil + demo.launch.py)
tools/bt_rfcomm.py              canal por SDP y creación de /dev/rfcomm0
udev/99-esp32-tactil.rules      enlace estable /dev/esp32_tactil (opcional)
udev/70-rfcomm.rules            acceso a /dev/rfcomm* sin grupo (opcional)
```
