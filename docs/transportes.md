# Transportes

Cada transporte lleva el mismo protocolo por un camino distinto del kernel. Todo está pensado para **Kubuntu 26.04**; lo de Arch está en [una nota al final](#nota-para-arch). Para la charla, sigue [la guía de la demo](demo-en-vivo.md).

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

## Comparación

| Transporte | Origen | Lo que se ve en Linux | Nodo |
|---|---|---|---|
| [USB (anclaje)](#usb-anclaje-usb) | App | Interfaz `enp…`, `enx…` o `usb0`; driver `rndis_host` o `cdc_ncm` | `transport:=udp` |
| [Wi-Fi](#wi-fi) | App | La interfaz Wi-Fi de siempre | `transport:=udp` |
| [Bluetooth](#bluetooth) | App | `/dev/rfcomm0` | `port:=/dev/rfcomm0` |
| [Serial](firmware-cyd.md) | CYD | `/dev/ttyUSB0`; driver `ch341` | `port:=/dev/ttyUSB0` |

USB y Wi-Fi son el mismo código en el nodo. Bluetooth y el CYD también comparten código: los dos son dispositivos seriales.

Si `ufw` está activo, para USB y Wi-Fi hace falta abrir el puerto una vez:

```bash
sudo ufw status
sudo ufw allow 5005/udp
```

## USB (anclaje USB)

El celular se presenta como tarjeta de red y le asigna IP al PC por DHCP. La app envía datagramas UDP a esa red.

```
app → UDP → rndis0 (celular) → cable USB → rndis_host (kernel) → enp… → 0.0.0.0:5005 → driver_tactil
```

1. Conecta el celular con un cable de datos y activa el anclaje. La ruta depende de la marca; suele estar en **Ajustes → Zona Wi-Fi y anclaje → Anclaje USB** o **Ajustes → Punto de acceso y anclaje**. Si el celular está en modo depuración, puedes abrir esa pantalla con:
   ```bash
   adb shell am start -a android.intent.action.MAIN -n com.android.settings/.TetherSettings
   ```
2. Míralo en Linux:
   ```bash
   sudo dmesg | grep -iE 'rndis|cdc_ncm'
   ip -br addr                 # aparece enp…, enx… o usb0 con una IP
   ```
   Con el Infinix usado en las pruebas se vio esto. La interfaz nace como `usb0` y al instante systemd le da un nombre estable:
   ```
   rndis_host 2-5:1.0 usb0: register 'rndis_host' at usb-0000:00:14.0-5, RNDIS device, 86:76:29:c8:63:ee
   rndis_host 2-5:1.0 enp0s20f0u5: renamed from usb0
   enp0s20f0u5      UNKNOWN        172.16.222.62/24 ...
   ```
3. Lanza el nodo:
   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```
4. En la app, elige **USB (anclaje)** y **Automático**. La barra de estado debe mostrar `<subred>.255:5005` y 20 paq/s, y en el nodo debe salir `Llegan datos UDP desde <IP del celular>`.

Para saber qué driver se usó:

```bash
lsusb -t
```

Al activar el anclaje, el celular cambia de identificador USB (en el Infinix, de `0e8d:201c` a `0e8d:2005`), y `adb` puede cortarse o pedir autorización de nuevo. No afecta al anclaje.

## Wi-Fi

```
app → UDP → wlan0 (celular) → router → interfaz Wi-Fi del PC → 0.0.0.0:5005 → driver_tactil
```

1. Conecta el PC y el celular a la **misma red**.
2. Lanza el mismo comando:
   ```bash
   ros2 launch cyd_teleop demo.launch.py transport:=udp
   ```
3. En la app, elige **Wi-Fi** y **Automático**. Envía al broadcast de la subred.
4. Si no llega nada, usa **IP manual** con la IP del PC (`ip -br addr`): muchas redes aíslan a los clientes o bloquean el broadcast. Si la red los aísla, el plan B es el anclaje USB.

Si el PC tiene cable y Wi-Fi en la misma subred, el broadcast entra por las dos interfaces y `ros2 topic hz` marca ~40 Hz. La tortuga funciona igual; para la demo, desconecta una o usa IP manual.

## Bluetooth

La app abre un servidor SPP (RFCOMM) y el PC se conecta a él. El nodo lo lee con el mismo modo serial que usa el CYD.

```
app (servidor SPP) → RFCOMM → /dev/rfcomm0 (kernel) → modo serial de driver_tactil
```

1. Con el Bluetooth activo en ambos lados, y abriendo los ajustes de Bluetooth del celular para que sea visible, empareja una sola vez:
   ```bash
   bluetoothctl power on
   bluetoothctl --timeout 20 scan on
   bluetoothctl devices                 # copia la MAC del celular
   bluetoothctl --timeout 45 pair <MAC> # acepta el aviso en el celular
   ```
2. En la app, elige **Bluetooth** y concede el permiso. Debe decir `esperando conexión`.
3. Crea el dispositivo y míralo:
   ```bash
   cd ~/Sala-de-maquinas-ros2
   python3 tools/bt_rfcomm.py canal <MAC>       # solo informa el canal
   sudo python3 tools/bt_rfcomm.py bind <MAC>
   ls -l /dev/rfcomm0
   ```
4. Lanza el nodo. Al abrir el archivo se conecta, y la app pasa a `conectado`:
   ```bash
   ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0
   ```
5. Para quitar el dispositivo:
   ```bash
   sudo python3 tools/bt_rfcomm.py release
   ```

### Permisos de `/dev/rfcomm0`

En Kubuntu, el dispositivo es del grupo `dialout`. Agrégate una vez y luego cierra sesión y vuelve a entrar:

```bash
sudo usermod -aG dialout $USER
```

Otra opción es la regla udev opcional, que da acceso al usuario con sesión activa, sin grupo:

```bash
sudo cp udev/70-rfcomm.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules
```

La regla se aplica cuando el dispositivo se crea, así que instálala antes del `bind`.

### Por qué `tools/bt_rfcomm.py`

Android elige el canal RFCOMM al abrir el servidor; la API pública no permite fijarlo. Hay que preguntarlo por SDP antes de conectarse.

`rfcomm` y `sdptool`, las herramientas clásicas para esto, están marcadas como obsoletas en BlueZ, y algunas distribuciones ya no las traen. `bt_rfcomm.py` no las necesita, ni instala nada: solo usa la biblioteca estándar de Python y el kernel.

- **El canal** se pregunta por SDP sobre un socket L2CAP. Si el celular publica varios servicios SPP, se elige el que se llama `TurtlePad`.
- **El dispositivo** se crea con el `ioctl` `RFCOMMCREATEDEV`, la misma interfaz del kernel que usa `rfcomm bind`. Por eso necesita `sudo`: el kernel exige `CAP_NET_ADMIN`.

### Cuándo cambia el canal

`/dev/rfcomm0` queda atado al canal con el que se creó, así que la app evita cambiarlo: el servidor sigue abierto al pasar a segundo plano y entre conexiones.

| Qué pasa | Canal | Qué hacer |
|---|---|---|
| La app pasa a segundo plano y vuelve | Se mantiene | Nada |
| Se corta la conexión | Se mantiene | Nada; el nodo reintenta cada 2 s |
| La app se cierra por completo y se reabre | Puede cambiar | Vuelve a elegir **Bluetooth** y, si no conecta, repite el `bind` |

El `bind` siempre vuelve a preguntar el canal y reemplaza el dispositivo anterior. Si quieres verlo, la barra de estado de la app también muestra el canal mientras espera, si Android lo informa.

### Probar la herramienta

```bash
cd ~/Sala-de-maquinas-ros2/tools
python3 -m pytest test_bt_rfcomm.py
```

Cubre la parte pura: decodificar SDP y armar el `ioctl`.

## Nota para Arch

- **ROS:** corre en Docker, y lo demás no cambia. Ver [la nota del nodo](nodo-ros.md#nota-para-arch).
- **Bluetooth con Docker:** crea `/dev/rfcomm0` antes de lanzar y pásalo al contenedor con `--device`, sin `--net=host`:
  ```bash
  docker run --rm -it --name demo --device /dev/rfcomm0 -e QT_QPA_PLATFORM=offscreen -v $PWD/ros2_ws/src:/src:ro cyd-ros-test bash -c 'source /opt/ros/lyrical/setup.bash; mkdir -p /ws/src; cp -r /src/cyd_teleop /ws/src; cd /ws; colcon build; source install/setup.bash; ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0'
  ```
- **Grupo de `/dev/rfcomm0`:** es `uucp`, no `dialout`. En Docker no importa, porque el contenedor corre como root.
- **`rfcomm` y `sdptool`:** tampoco hacen falta. Están en el paquete `bluez-deprecated-tools`, que no recomendamos instalar.
