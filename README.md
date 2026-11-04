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

## Inicio rápido

En **Kubuntu 26.04 con ROS 2 Lyrical**:

```bash
sudo apt install ros-lyrical-turtlesim python3-serial netcat-openbsd
git clone https://github.com/Valmorales18/Sala-de-maquinas-ros2.git ~/Sala-de-maquinas-ros2
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
colcon build
source install/setup.bash
ros2 launch cyd_teleop demo.launch.py transport:=udp
```

Se abre turtlesim. En otra terminal, mueve la tortuga sin celular:

```bash
echo F | nc -u -w0 127.0.0.1 5005
```

Para usar el celular, [descarga el APK](https://github.com/Valmorales18/Sala-de-maquinas-ros2/releases/latest) ([cómo instalarlo](docs/app-android.md#instalar)) y sigue [los transportes](docs/transportes.md).

## Documentación

| Documento | Para qué |
|---|---|
| [**Demo en vivo**](docs/demo-en-vivo.md) | **La guía de la charla:** qué lanzar, en qué orden, qué mostrar y el plan B |
| [Transportes](docs/transportes.md) | USB, Wi-Fi y Bluetooth paso a paso, y qué pasa en el kernel |
| [App Android](docs/app-android.md) | Compilar, firmar y publicar; arquitectura y cómo cambiar el tema |
| [Nodo ROS](docs/nodo-ros.md) | Lanzar, parámetros, tópicos, diseño y pruebas |
| [CYD por serial](docs/firmware-cyd.md) | Firmware, udev, brltty y la demo original |
| [Problemas](docs/problemas.md) | Errores típicos, por síntoma |

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
