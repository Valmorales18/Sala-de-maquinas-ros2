# Cómo correr las pruebas

## Antes de empezar (una sola vez)

Clona el repo (o actualízalo si ya lo tienes) e instala lo necesario:

```bash
git clone https://github.com/Valmorales18/Sala-de-maquinas-ros2.git ~/Sala-de-maquinas-ros2
cd ~/Sala-de-maquinas-ros2
git pull
sudo apt install ros-lyrical-turtlesim python3-serial
sudo usermod -aG dialout $USER   # para Bluetooth; después cierra sesión y vuelve a entrar
```

Compila los nodos:

```bash
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
colcon build
```

Abre 2 terminales y en cada una corre esto:

```bash
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
source install/setup.bash
```

Ahora sí, los 3 modos de controlar la tortuga desde el celular.

## Por cable

1. Conecta el celular al PC con un cable de datos y activa **Anclaje USB** (la opción con la que se comparte internet con el cel basicamente) en los ajustes del celular.
2. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

3. En la app elige **USB (anclaje)** y **Automático**. En la terminal 1 debe salir `Llegan datos UDP desde ...`.
4. Opcional, para ver cómo llegan las letras, en la **terminal 2**:

   ```bash
   ros2 topic echo /tactil/raw
   ```

## Por Wi-Fi

1. Pon el celular y el PC en la misma red Wi-Fi.
2. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

3. En la app elige **Wi-Fi** y **Automático**. Si no llega nada, usa **IP manual** con la IP del PC (sale con `ip -br addr`).
4. Opcional, en la **terminal 2**:

   ```bash
   ros2 topic echo /tactil/raw
   ```

## Por Bluetooth

1. Empareja el celular con el PC (desde los ajustes de Bluetooth) y saca la MAC del celular:

   ```bash
   bluetoothctl devices Paired
   ```

   La MAC es la parte del medio de la línea que sale, por ejemplo `0C:B9:83:FB:7E:55`.

2. En la app elige **Bluetooth**. Debe decir `esperando conexión`.
3. En la **terminal 2**, crea `/dev/rfcomm0` con el helper. Reemplaza `<MAC>` por la de tu celular:

   ```bash
   cd ~/Sala-de-maquinas-ros2
   python3 tools/bt_rfcomm.py canal <MAC>
   sudo python3 tools/bt_rfcomm.py bind <MAC>
   ls -l /dev/rfcomm0
   ```

   Ejemplo listo para copiar y pegar con el telefono de pruebas:

   ```bash
   cd ~/Sala-de-maquinas-ros2
   python3 tools/bt_rfcomm.py canal 0C:B9:83:FB:7E:55
   sudo python3 tools/bt_rfcomm.py bind 0C:B9:83:FB:7E:55
   ls -l /dev/rfcomm0
   ```

4. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0
   ```

   La app debe pasar a `conectado`.

5. Opcional, en la **terminal 2**:

   ```bash
   ros2 topic echo /tactil/raw
   ```

6. Cuando termines, libera el Bluetooth:

   ```bash
   sudo python3 tools/bt_rfcomm.py release
   ```

## LISTOOOOOOO, ya probaste los 3. Felicidades
