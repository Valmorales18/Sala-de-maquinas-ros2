# Nodo ROS 2: `driver_tactil`

El paquete `cyd_teleop` recibe letras (`F`, `B`, `L`, `R`, `S`) por serial o por UDP y mueve la tortuga de turtlesim. Todo está pensado para **Kubuntu 26.04 con ROS 2 Lyrical nativo**; lo de Arch está en [una nota al final](#nota-para-arch).

## Instalar y compilar

1. Con ROS 2 Lyrical ya instalado, agrega lo que usa la demo:

   ```bash
   sudo apt install ros-lyrical-turtlesim ros-lyrical-rqt-graph python3-serial python3-pytest netcat-openbsd
   ```

2. Clona el repo (solo la primera vez):

   ```bash
   git clone https://github.com/Valmorales18/Sala-de-maquinas-ros2.git ~/Sala-de-maquinas-ros2
   ```

3. Compila. Repite este paso cada vez que cambies el código:

   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   colcon build
   ```

4. En cada terminal nueva, carga el entorno:

   ```bash
   cd ~/Sala-de-maquinas-ros2/ros2_ws
   source /opt/ros/lyrical/setup.bash
   source install/setup.bash
   ```

## Lanzar

`demo.launch.py` abre turtlesim y el driver. Elige el comando según de dónde vengan las letras:

| Fuente de las letras | Comando |
|---|---|
| App por USB o Wi-Fi | `ros2 launch cyd_teleop demo.launch.py transport:=udp` |
| App por Bluetooth | `ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0` |
| CYD por serial, sin regla udev | `ros2 launch cyd_teleop demo.launch.py port:=/dev/ttyUSB0` |
| CYD por serial, con regla udev | `ros2 launch cyd_teleop demo.launch.py` |

Para cerrar: Ctrl+C en la terminal del launch. La tortuga recibe un Twist en cero y el puerto o socket queda libre.

### Probar sin celular

1. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

2. **Terminal 2:**

   ```bash
   echo F | nc -u -w0 127.0.0.1 5005
   ```

La tortuga avanza un poco y en la terminal 1 sale `Llegan datos UDP desde 127.0.0.1`. Se detiene sola: a los 0,5 s sale `Sin datos válidos en 0.5 s: tortuga detenida`.

### Cambiar parámetros

El launch solo acepta `transport` (`serial`), `port` (`/dev/esp32_tactil`) y `udp_port` (5005). Para cambiar los demás, lanza los nodos por separado.

1. **Terminal 1:**

   ```bash
   ros2 run turtlesim turtlesim_node
   ```

2. **Terminal 2**, por serial y más despacio:

   ```bash
   ros2 run cyd_teleop driver_tactil --ros-args -p port:=/dev/ttyUSB0 -p linear_speed:=1.0 -p angular_speed:=1.5
   ```

   O por UDP, en otro puerto:

   ```bash
   ros2 run cyd_teleop driver_tactil --ros-args -p transport:=udp -p udp_port:=6000
   ```

| Parámetro | Por defecto | Para qué |
|---|---|---|
| `transport` | `serial` | De dónde leer: `serial` o `udp`. Otro valor hace que el nodo no arranque. |
| `port` | `/dev/esp32_tactil` | Dispositivo serial, por ejemplo `/dev/ttyUSB0` o `/dev/rfcomm0` |
| `baud` | `115200` | Velocidad del serial |
| `udp_port` | `5005` | Puerto UDP donde escuchar |
| `udp_bind` | `0.0.0.0` | Dirección donde escuchar |
| `linear_speed` | `2.0` | Velocidad de `F` y `B` |
| `angular_speed` | `2.0` | Velocidad de giro de `L` y `R` |
| `timeout_s` | `0.5` | Si no llegan letras válidas en este tiempo, publica un Twist en cero |
| `cmd_topic` | `/turtle1/cmd_vel` | Tópico de velocidad |

## Ver qué pasa por dentro

En otra terminal, con el entorno cargado, corre el que quieras:

```bash
ros2 topic echo /tactil/raw
```

La letra tal como llega.

```bash
ros2 topic echo /turtle1/cmd_vel
```

El Twist que recibe turtlesim.

```bash
ros2 topic hz /turtle1/cmd_vel
```

El ritmo: debe marcar ~20 Hz, el de la app y el firmware.

```bash
rqt_graph
```

El grafo de nodos y tópicos.

Todas las terminales deben tener el mismo `ROS_DOMAIN_ID` (compruébalo con `echo $ROS_DOMAIN_ID`).

## Tópicos

| Tópico | Tipo | Contenido |
|---|---|---|
| `/tactil/raw` | `std_msgs/String` | Cada línea no vacía tal como llegó, aunque no sea válida |
| `/turtle1/cmd_vel` | `geometry_msgs/Twist` | La velocidad que recibe turtlesim |

| Letra | `linear.x` | `angular.z` |
|---|---|---|
| `F` | `+linear_speed` | 0 |
| `B` | `-linear_speed` | 0 |
| `L` | 0 | `+angular_speed` |
| `R` | 0 | `-angular_speed` |
| `S` | 0 | 0 |

## Pruebas

Prueban la parte pura (`test_traduccion.py` y `test_protocolo.py`):

```bash
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
colcon test --packages-select cyd_teleop && colcon test-result --verbose
```

## Cómo funciona

El código está en `ros2_ws/src/cyd_teleop/cyd_teleop/`:

| Archivo | Qué hace |
|---|---|
| `driver_tactil.py` | El nodo: abre el transporte, lee en un timer de 50 Hz y publica |
| `protocolo.py` | Separa un datagrama en líneas. Sin ROS, para poder probarlo |
| `traduccion.py` | Convierte una letra en velocidad. Sin ROS, para poder probarlo |

Cada transporte solo aporta su forma de abrir y de leer (`_abrir_puerto` y `_leer_serial`, o `_abrir_udp` y `_leer_udp`). Las líneas leídas pasan todas por el mismo `_procesar_linea`. Un timer de 50 Hz (`_tick`) lee sin bloquear, reintenta abrir cada 2 s y aplica el timeout de seguridad.

### Decisiones de diseño

- **Es el único lector del puerto.** El serial se abre con `exclusive=True`, para que no lo abra otra instancia del driver. `cat` no respeta ese bloqueo: ver [dos lectores en el mismo puerto](problemas.md#serial-y-cyd).
- **Sin `SO_REUSEADDR` ni `SO_REUSEPORT`.** Si otro proceso tiene el puerto, el kernel responde `Address already in use`. El nodo registra el error y reintenta cada 2 s. Es uno de los [choques de la charla](transportes.md#choque-el-puerto-ya-está-en-uso).
- **Escucha en `0.0.0.0`.** Así recibe broadcast y sigue funcionando si la interfaz del anclaje aparece después de lanzar el nodo. Con `udp_bind` en una IP concreta deja de recibir broadcast y falla si esa IP cambia.
- **Un datagrama llega entero o no llega.** Por eso, a diferencia del serial, no se guarda nada para el siguiente: el último `\n` es opcional y un datagrama puede traer varias líneas.
- **Vacía los datagramas pendientes en cada tick**, con un máximo de 1000, para que una inundación de paquetes no cuelgue el timer.
- **Registra la IP de origen** la primera vez y cada vez que cambia, para mostrar de dónde llegan los datos. Si dos equipos envían a la vez, esa línea se repetirá mucho.
- **Bluetooth no agrega código de lectura.** `/dev/rfcomm0` es un dispositivo serial más: ver [transportes](transportes.md#bluetooth).

## Nota para Arch

Arch no tiene paquete nativo de ROS 2 Lyrical, así que se prueba con Docker.

1. Construye la imagen desde la raíz del repo:

   ```bash
   docker build -t cyd-ros-test docker/
   ```

2. Lanza la demo por UDP:

   ```bash
   docker run --rm -it --name demo --net=host -e QT_QPA_PLATFORM=offscreen -v $PWD/ros2_ws/src:/src:ro cyd-ros-test bash -c 'source /opt/ros/lyrical/setup.bash; mkdir -p /ws/src; cp -r /src/cyd_teleop /ws/src; cd /ws; colcon build; source install/setup.bash; ros2 launch cyd_teleop demo.launch.py transport:=udp'
   ```

3. turtlesim corre sin ventana, así que el movimiento se ve en la posición:

   ```bash
   docker exec -it demo bash -c 'source /opt/ros/lyrical/setup.bash; ros2 topic echo /turtle1/pose --field x'
   ```

Para probar sin celular, si no tienes `nc`: `echo F | ncat -u 127.0.0.1 5005`.
