# Como correr las pruebas

Para los 3 casos necesitas haber compilado los nodos de ROS, para eso tenes que tener el repo al dia.

Despues:

```bash
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
colcon build
```

Con eso ya quedaron los nodos listos para usar, ahora necesitamos dejar 3 terminales listos para usar con este comando:

```bash
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
source install/setup.bash
```

Ahora pasamos a los 3 modos de controlar a la tortuga desde el cel

## Por cable

### 1. Asegurate de que tu cel le este pasando internet a tu PC por el cable

### 2. Corre el siguiente comando

```bash
ros2 launch cyd_teleop demo.launch.py transport:=udp
```

### 3. Opcional, pero si quieres ver como llegan las letras en el segundo terminal puedes hacer

```bash
ros2 topic echo /tactil/raw
```

## Por wifi

### 1. Asegurate de que tu cel y PC esten en la misma red wifi, y que se puedan ver

### 2. Corre el siguiente comando

```bash
ros2 launch cyd_teleop demo.launch.py transport:=udp
```

### 3. Opcional, pero si quieres ver como llegan las letras en el segundo terminal puedes hacer

```bash
ros2 topic echo /tactil/raw
```

## Por Bluetooth

### 1. Conecta tu cel y PC por Bluetooth y saca la MAC

Con este comando deberia ser mas facil, solo copiar y pegar

```bash
bluetoothctl
devices Connected
```

### 2. Ahora debes usar el helper en python ya preparado, solo necesitamos la MAC del cel

```bash
cd ~/Sala-de-maquinas-ros2
python3 tools/bt_rfcomm.py canal <MAC> # Reemplaza <MAC> por la MAC de tu cel
sudo python3 tools/bt_rfcomm.py bind <MAC> # Reemplaza <MAC> por la MAC de tu cel
ls -l /dev/rfcomm0
```

Con la MAC de tu cel quedaria:

```bash
cd ~/Sala-de-maquinas-ros2
python3 tools/bt_rfcomm.py canal 0C:B9:83:FB:7E:55
sudo python3 tools/bt_rfcomm.py bind 0C:B9:83:FB:7E:55
ls -l /dev/rfcomm0
```

### 3. Ahora corre el nodo de ROS con el puerto donde está llegando la info de tu bluetooth

```bash
ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0
```

### 4. Opcional, pero si quieres ver como llegan las letras en el segundo terminal puedes hacer

```bash
ros2 topic echo /tactil/raw
```

### 5. Cuando termines libera el helper del Bluetooth

```bash
sudo python3 tools/bt_rfcomm.py release
```

## LISTOOOOOOO, ya probaste correctamente los 3, felicidades
