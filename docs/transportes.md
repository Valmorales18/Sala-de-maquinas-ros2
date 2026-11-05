# Transportes

Cómo llega la letra del celular al nodo por cada camino, y qué se ve en Linux en cada uno. Si solo quieres mover la tortuga, con [Cómo correr las pruebas](correr-pruebas.md) basta. Esta guía agrega lo que hay que mirar y mostrar en la charla.

Todo está pensado para **Kubuntu 26.04**; lo de Arch está en [una nota al final](#nota-para-arch).

| Transporte | Origen | Lo que se ve en Linux | Nodo |
|---|---|---|---|
| [USB (anclaje)](#usb-anclaje-usb) | App | Interfaz `enp…`, `enx…` o `usb0`; driver `rndis_host` o `cdc_ncm` | `transport:=udp` |
| [Wi-Fi](#wi-fi) | App | La interfaz Wi-Fi de siempre | `transport:=udp` |
| [Bluetooth](#bluetooth) | App | `/dev/rfcomm0` | `port:=/dev/rfcomm0` |
| [Serial](firmware-cyd.md) | CYD | `/dev/ttyUSB0`; driver `ch341` | `port:=/dev/ttyUSB0` |

USB y Wi-Fi son el mismo código en el nodo. Bluetooth y el CYD también comparten código: los dos son dispositivos seriales.

Orden recomendado para la charla: **USB, Bluetooth y Wi-Fi al final**. USB no depende del lugar ni de la red, y Wi-Fi sí.

## Antes de empezar (una sola vez)

1. Compila el nodo, como en [Cómo correr las pruebas](correr-pruebas.md#antes-de-empezar-una-sola-vez).
2. Si `ufw` está activo, abre el puerto para USB y Wi-Fi:

   ```bash
   sudo ufw status
   sudo ufw allow 5005/udp
   ```

3. Dale permiso a tu usuario para `/dev/rfcomm0`, y después cierra sesión y vuelve a entrar:

   ```bash
   sudo usermod -aG dialout $USER
   ```

4. Empareja el celular. Abre los ajustes de Bluetooth del celular para que sea visible, y en el PC corre esto. Reemplaza `<MAC>` por la de tu celular y acepta el aviso en el celular:

   ```bash
   bluetoothctl power on
   bluetoothctl --timeout 20 scan on
   bluetoothctl devices
   bluetoothctl --timeout 45 pair <MAC>
   ```

5. [Instala la app](app-android.md#instalar) y ábrela una vez en cada modo para conceder los permisos.

Para la charla, ensaya los tres transportes completos en el PC que vas a usar y lleva el **cable de datos**. Si el PC tiene cable de red y Wi-Fi en la misma subred, desconecta uno: si no, el broadcast entra dos veces.

## Cada vez que vayas a probar

Abre 3 terminales y en cada una corre esto:

```bash
cd ~/Sala-de-maquinas-ros2/ros2_ws
source /opt/ros/lyrical/setup.bash
source install/setup.bash
```

La **terminal 1** lleva el launch, la **2** muestra ROS y la **3** muestra Linux.

## USB (anclaje USB)

El celular se presenta como tarjeta de red y le asigna IP al PC por DHCP. La app envía datagramas UDP a esa red.

```
app → UDP → rndis0 (celular) → cable USB → rndis_host (kernel) → enp… → 0.0.0.0:5005 → driver_tactil
```

1. **Terminal 3:** deja el kernel a la vista antes de activar nada.

   ```bash
   sudo dmesg -w
   ```

2. **Celular:** conecta el cable y activa **Anclaje USB**. La ruta depende de la marca; suele estar en **Ajustes → Zona Wi-Fi y anclaje → Anclaje USB** o **Ajustes → Punto de acceso y anclaje**. Si el celular está en modo depuración, también puedes abrir esa pantalla con:

   ```bash
   adb shell am start -a android.intent.action.MAIN -n com.android.settings/.TetherSettings
   ```

   En `dmesg` debe salir algo así. El driver reclama el celular, la interfaz nace como `usb0` y al instante systemd la renombra:

   ```
   rndis_host 2-5:1.0 usb0: register 'rndis_host' at usb-0000:00:14.0-5, RNDIS device, 86:76:29:c8:63:ee
   rndis_host 2-5:1.0 enp0s20f0u5: renamed from usb0
   ```

3. **Terminal 3:** corta con Ctrl+C y mira la interfaz nueva, con la IP que le dio el celular.

   ```bash
   ip -br addr
   ```

   ```
   enp0s20f0u5      UNKNOWN        172.16.222.62/24 ...
   ```

   El nombre y la IP cambian según el PC, el puerto USB y el celular. Puede salir `enx…` o `usb0`. Al nodo no le importa, porque escucha en `0.0.0.0`.

4. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

   Debe salir `Socket UDP escuchando en 0.0.0.0:5005`.

5. En la app elige **USB (anclaje)** y **Automático**. La barra de estado debe mostrar `<subred>.255:5005` y 20 paq/s. En la terminal 1 debe salir `Llegan datos UDP desde <IP del celular>`.

6. **Terminal 2:** mantén presionada una flecha y mira cómo llegan las letras. Corta con Ctrl+C y mira el ritmo, que debe ser de ~20 Hz:

   ```bash
   ros2 topic echo /tactil/raw
   ```

   ```bash
   ros2 topic hz /turtle1/cmd_vel
   ```

7. **Terminal 3:** muestra quién tiene el puerto y qué driver se usó.

   ```bash
   ss -ulpn | grep 5005
   lsusb -t
   ```

8. Cuando termines, Ctrl+C en la terminal 1.

Al activar el anclaje, el celular cambia de identificador USB (en el Infinix, de `0e8d:201c` a `0e8d:2005`), y `adb` puede cortarse o pedir autorización de nuevo. No afecta al anclaje.

### Choque: el puerto ya está en uso

Con el launch corriendo, en la **terminal 2**:

```bash
ros2 run cyd_teleop driver_tactil --ros-args -p transport:=udp -r __node:=otro
```

Sale cada 2 s, hasta que lo cortes con Ctrl+C:

```
No se pudo abrir UDP 0.0.0.0:5005: Address already in use. Otro proceso tiene el puerto (míralo con: ss -ulpn | grep 5005). Reintento en 2 s
```

### Choque: se va la fuente de datos

Manda la app a segundo plano con el botón de inicio. La tortuga se queda quieta y sale `Sin datos válidos en 0.5 s: tortuga detenida`. Al volver a la app, el envío se retoma solo.

## Wi-Fi

```
app → UDP → wlan0 (celular) → router → interfaz Wi-Fi del PC → 0.0.0.0:5005 → driver_tactil
```

1. Pon el PC y el celular en la **misma red**.
2. **Terminal 3:** saca la IP del PC, por si la necesitas en el paso 4.

   ```bash
   ip -br addr
   ```

3. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```

4. En la app elige **Wi-Fi** y **Automático**, que envía al broadcast de la subred. En la terminal 1 debe salir `Llegan datos UDP desde <IP del celular>`.

   Si no llega nada, usa **IP manual** con la IP del paso 2: muchas redes bloquean el broadcast. Si tampoco llega, la red aísla a los clientes; usa el anclaje USB.

Es el mismo código de nodo que por USB; solo cambió la interfaz por donde entra.

Si el PC tiene cable y Wi-Fi en la misma subred, `ros2 topic hz` marca ~40 Hz. La tortuga funciona igual, pero para la demo desconecta uno o usa IP manual.

## Bluetooth

La app abre un servidor SPP (RFCOMM) y el PC se conecta a él. El nodo lo lee con el mismo modo serial que usa el CYD.

```
app (servidor SPP) → RFCOMM → /dev/rfcomm0 (kernel) → modo serial de driver_tactil
```

1. En la app elige **Bluetooth** y concede el permiso. Debe decir `esperando conexión`.
2. **Terminal 3:** pregunta el canal y crea el dispositivo. Reemplaza `<MAC>` por la de tu celular (sale con `bluetoothctl devices Paired`):

   ```bash
   cd ~/Sala-de-maquinas-ros2
   python3 tools/bt_rfcomm.py canal <MAC>
   sudo python3 tools/bt_rfcomm.py bind <MAC>
   ls -l /dev/rfcomm0
   ```

   Ahora es un archivo en `/dev`, con major 216 (RFCOMM). Todavía no hay conexión: se abre cuando alguien abre el archivo.

   ```
   crw-rw---- 1 root dialout 216, 0 nov  7 10:00 /dev/rfcomm0
   ```

3. **Terminal 1:**

   ```bash
   ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0
   ```

   Debe salir `Puerto /dev/rfcomm0 abierto`, y la app pasa a `conectado`.

4. **Terminal 2:** presiona una flecha y mira cómo llegan las letras.

   ```bash
   ros2 topic echo /tactil/raw
   ```

5. **Terminal 3:** muestra quién tiene abierto el archivo.

   ```bash
   fuser -v /dev/rfcomm0
   ```

6. Cuando termines, Ctrl+C en la terminal 1, y luego libera el Bluetooth en la terminal 3:

   ```bash
   sudo python3 tools/bt_rfcomm.py release
   ```

### Permisos de `/dev/rfcomm0`

En Kubuntu, el dispositivo es del grupo `dialout`. Agrégate una vez y luego cierra sesión y vuelve a entrar:

```bash
sudo usermod -aG dialout $USER
```

Otra opción es la regla udev opcional, que da acceso al usuario con sesión activa, sin grupo. Se aplica cuando el dispositivo se crea, así que instálala antes del `bind`:

```bash
cd ~/Sala-de-maquinas-ros2
sudo cp udev/70-rfcomm.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules
```

### Cuándo cambia el canal

`/dev/rfcomm0` queda atado al canal con el que se creó, así que la app evita cambiarlo: el servidor sigue abierto al pasar a segundo plano y entre conexiones.

| Qué pasa | Canal | Qué hacer |
|---|---|---|
| La app pasa a segundo plano y vuelve | Se mantiene | Nada |
| Se corta la conexión | Se mantiene | Nada; el nodo reintenta cada 2 s |
| La app se cierra por completo y se reabre | Puede cambiar | Vuelve a elegir **Bluetooth** y, si no conecta, repite el `bind` |

El `bind` siempre vuelve a preguntar el canal y reemplaza el dispositivo anterior. La barra de estado de la app también muestra el canal mientras espera, si Android lo informa.

### Por qué `tools/bt_rfcomm.py`

Android elige el canal RFCOMM al abrir el servidor; la API pública no permite fijarlo. Hay que preguntarlo por SDP antes de conectarse.

`rfcomm` y `sdptool`, las herramientas clásicas para esto, están marcadas como obsoletas en BlueZ, y algunas distribuciones ya no las traen. `bt_rfcomm.py` no las necesita, ni instala nada: solo usa la biblioteca estándar de Python y el kernel.

- **El canal** se pregunta por SDP sobre un socket L2CAP. Si el celular publica varios servicios SPP, se elige el que se llama `TurtlePad`.
- **El dispositivo** se crea con el `ioctl` `RFCOMMCREATEDEV`, la misma interfaz del kernel que usa `rfcomm bind`. Por eso necesita `sudo`: el kernel exige `CAP_NET_ADMIN`.

### Probar la herramienta

Cubre la parte pura: decodificar SDP y armar el `ioctl`.

```bash
cd ~/Sala-de-maquinas-ros2/tools
python3 -m pytest test_bt_rfcomm.py
```

## El protocolo

Una letra por línea, a ~20 Hz, aunque la letra sea `S`:

| Letra | Significado |
|---|---|
| `F` | Adelante |
| `B` | Atrás |
| `L` | Izquierda |
| `R` | Derecha |
| `S` | Stop |

El CYD termina cada línea en `\r\n`, y la app en `\n`. El nodo acepta ambos.

## Nota para Arch

- **ROS:** corre en Docker, y lo demás no cambia. Ver [la nota del nodo](nodo-ros.md#nota-para-arch). Corre sin ventana: la posición se lee con `ros2 topic echo /turtle1/pose --field x`.
- **Bluetooth con Docker:** crea `/dev/rfcomm0` antes de lanzar y pásalo al contenedor con `--device`, sin `--net=host`:
  ```bash
  docker run --rm -it --name demo --device /dev/rfcomm0 -e QT_QPA_PLATFORM=offscreen -v $PWD/ros2_ws/src:/src:ro cyd-ros-test bash -c 'source /opt/ros/lyrical/setup.bash; mkdir -p /ws/src; cp -r /src/cyd_teleop /ws/src; cd /ws; colcon build; source install/setup.bash; ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0'
  ```
- **Grupo de `/dev/rfcomm0`:** es `uucp`, no `dialout`. En Docker no importa, porque el contenedor corre como root.
- **`nc`:** puede no estar. Usa `echo F | ncat -u 127.0.0.1 5005`.
- **`rfcomm` y `sdptool`:** tampoco hacen falta. Están en el paquete `bluez-deprecated-tools`, que no recomendamos instalar.
