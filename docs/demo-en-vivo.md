# Demo en vivo

Guía para la charla del sábado 7 de noviembre de 2026 en **Kubuntu 26.04 con ROS 2 Lyrical nativo**. Lo de Arch está en [una nota al final](#nota-para-arch).

La idea: una misma letra (`F`, `B`, `L`, `R`, `S`) llega a `driver_tactil` por caminos distintos, y cada uno deja un rastro visible en Linux.

```
                                            kernel y /dev                    ROS 2
USB (anclaje)  celular ─ RNDIS ──── rndis_host, interfaz enp…/enx… ─┐
                                                                    ├─ UDP 5005 ─┐
Wi-Fi          celular ─ red ────── wlan del PC ────────────────────┘            ├─ driver_tactil ─ /turtle1/cmd_vel ─ turtlesim
Bluetooth      celular ─ RFCOMM ─── /dev/rfcomm0 ────────────────── serial ──────┘
```

Orden recomendado: **USB, Bluetooth y Wi-Fi al final**. USB no depende del lugar ni de la red, y Wi-Fi sí.

## Antes de la charla

Hazlo una vez en el PC de la charla, con tiempo.

- [ ] **Compilar**
  ```bash
  git clone https://github.com/Valmorales18/Sala-de-maquinas-ros2.git ~/Sala-de-maquinas-ros2
  cd ~/Sala-de-maquinas-ros2/ros2_ws
  source /opt/ros/lyrical/setup.bash
  colcon build
  ```
- [ ] **Abrir el firewall**, si `ufw` está activo
  ```bash
  sudo ufw status
  sudo ufw allow 5005/udp
  ```
- [ ] **Permiso para `/dev/rfcomm0`.** Después hay que cerrar sesión y volver a entrar.
  ```bash
  sudo usermod -aG dialout $USER
  ```
- [ ] **Emparejar el celular.** Abre los ajustes de Bluetooth del celular y acepta el aviso.
  ```bash
  bluetoothctl power on
  bluetoothctl --timeout 20 scan on
  bluetoothctl devices
  bluetoothctl --timeout 45 pair <MAC>
  ```
- [ ] **Instalar la app** y abrirla una vez en cada modo para conceder los permisos.
- [ ] **Ensayar los tres transportes completos en este PC.** Ver [qué falta confirmar](#qué-falta-confirmar-en-kubuntu).

La MAC del Infinix usado en las pruebas es `44:E7:61:94:62:B0`. Si usas otro celular, cambia `<MAC>` por la suya.

## Al llegar al lugar

- [ ] Celular cargado y **cable de datos** a mano.
- [ ] Si el PC tiene cable de red y Wi-Fi en la misma subred, desconecta uno. Si no, el broadcast entra dos veces y `ros2 topic hz` marca ~40 Hz en vez de 20.
- [ ] Abre tres terminales, y en cada una carga el entorno:
  ```bash
  cd ~/Sala-de-maquinas-ros2/ros2_ws
  source /opt/ros/lyrical/setup.bash
  source install/setup.bash
  ```

En la demo: la **terminal 1** lleva el launch, la **2** muestra ROS y la **3** muestra Linux.

## 1. USB (anclaje USB)

**Terminal 3:** deja el kernel a la vista antes de activar nada.
```bash
sudo dmesg -w
```

**Celular:** conecta el cable y activa **Anclaje USB** en los ajustes.

**Qué mostrar en `dmesg`:** el driver reclama el celular y la interfaz nace como `usb0`, pero al instante la renombra systemd. Así se vio en Arch:
```
rndis_host 2-5:1.0 usb0: register 'rndis_host' at usb-0000:00:14.0-5, RNDIS device, 86:76:29:c8:63:ee
rndis_host 2-5:1.0 enp0s20f0u5: renamed from usb0
```

**Terminal 3:** corta con Ctrl+C y mira la interfaz nueva, con la IP que le dio el celular.
```bash
ip -br addr
```
```
enp0s20f0u5      UNKNOWN        172.16.222.62/24 ...
```
El nombre y la IP cambian según el PC, el puerto USB y el celular. Puede salir `enx…` o `usb0`. Al nodo no le importa, porque escucha en `0.0.0.0`.

**Terminal 1:**
```bash
ros2 launch cyd_teleop demo.launch.py transport:=udp
```
Debe salir `Socket UDP escuchando en 0.0.0.0:5005`.

**Celular:** elige **USB (anclaje)** y **Automático**. La barra de estado debe mostrar `<subred>.255:5005` y 20 paq/s. En la terminal 1 sale `Llegan datos UDP desde <IP del celular>`.

**Terminal 2:** mantén presionada una flecha y mira cómo llegan las letras.
```bash
ros2 topic echo /tactil/raw
```
```bash
ros2 topic hz /turtle1/cmd_vel
```

**Terminal 3:** muestra quién tiene el puerto.
```bash
ss -ulpn | grep 5005
```

### Choque: el puerto ya está en uso

Con el launch corriendo, en la **terminal 2**:
```bash
ros2 run cyd_teleop driver_tactil --ros-args -p transport:=udp -r __node:=otro
```
Sale cada 2 s:
```
No se pudo abrir UDP 0.0.0.0:5005: Address already in use. Otro proceso tiene el puerto (míralo con: ss -ulpn | grep 5005). Reintento en 2 s
```
Corta con Ctrl+C.

### Choque: se va la fuente de datos

Manda la app a segundo plano con el botón de inicio. La tortuga se queda quieta y sale `Sin datos válidos en 0.5 s: tortuga detenida`. Al volver a la app, el envío se retoma solo.

**Para cerrar:** Ctrl+C en la terminal 1.

## 2. Bluetooth

**Celular:** elige **Bluetooth**. Debe decir `esperando conexión`.

**Terminal 3:** pregunta el canal y crea el dispositivo.
```bash
cd ~/Sala-de-maquinas-ros2
python3 tools/bt_rfcomm.py canal <MAC>
sudo python3 tools/bt_rfcomm.py bind <MAC>
ls -l /dev/rfcomm0
```

**Qué mostrar:** ahora es un archivo en `/dev`, con major 216 (RFCOMM). Android elige el canal, y por eso hay que preguntarlo por SDP.
```
crw-rw---- 1 root dialout 216, 0 nov  7 10:00 /dev/rfcomm0
```
Todavía no hay conexión: se abre cuando alguien abre el archivo.

**Terminal 1:** el mismo modo serial del CYD, sin código nuevo.
```bash
ros2 launch cyd_teleop demo.launch.py port:=/dev/rfcomm0
```
Debe salir `Puerto /dev/rfcomm0 abierto`, y la app pasa a `conectado`.

**Terminal 2:** presiona una flecha.
```bash
ros2 topic echo /tactil/raw
```

**Terminal 3:** muestra quién tiene abierto el archivo.
```bash
fuser -v /dev/rfcomm0
```

**Para cerrar:** Ctrl+C en la terminal 1, y luego en la terminal 3:
```bash
sudo python3 tools/bt_rfcomm.py release
```

## 3. Wi-Fi

Conecta el PC y el celular a la misma red.

**Terminal 3:**
```bash
ip -br addr
```

**Terminal 1:**
```bash
ros2 launch cyd_teleop demo.launch.py transport:=udp
```

**Celular:** elige **Wi-Fi** y **Automático**. En la terminal 1 sale `Llegan datos UDP desde <IP del celular>`. Es el mismo código de nodo que por USB; solo cambió la interfaz por donde entra.

## Plan B

Cada plan B está pensado para resolver en menos de un minuto. Si no sale, pasa al siguiente transporte.

### USB

| Síntoma | Qué hacer |
|---|---|
| No aparece ninguna interfaz; `dmesg` no muestra nada | El anclaje no está activo o se apagó al desconectar el cable. Actívalo de nuevo. Si no se puede activar, cambia el cable: puede ser solo de carga. |
| La interfaz aparece sin IP | `nmcli device` y espera unos segundos. Desactiva y activa el anclaje. |
| La app dice `No hay interfaz USB` | Elige **IP manual** y escribe la IP del PC que da `ip -br addr`. |
| La app marca 20 paq/s y al nodo no llega nada | `sudo ufw status`; luego `sudo ufw allow 5005/udp`. |
| `Address already in use` | Otro proceso tiene el puerto: `ss -ulpn | grep 5005`. Ciérralo; el nodo abre solo al liberarse. |

**Si nada resuelve:** pasa a Bluetooth.

### Bluetooth

| Síntoma | Qué hacer |
|---|---|
| `bind` dice `no publica el servicio TurtlePad` | La app no está en modo Bluetooth. Al abrir la app desde cero, la presentación puede preseleccionar USB o Wi-Fi: vuelve a elegir **Bluetooth**. |
| `No se pudo consultar SDP` | `bluetoothctl power on` en el PC; revisa el Bluetooth del celular y acércalo. |
| El nodo dice `Permission denied` | Falta `dialout` o no cerraste sesión. Para seguir ya: `sudo chmod 666 /dev/rfcomm0`. |
| La app sigue en `esperando conexión` con el nodo corriendo | Cerraste la app y el canal pudo cambiar: repite el `bind`. El nodo reintenta cada 2 s. |
| No conecta aunque el canal esté bien | Empareja de nuevo: `bluetoothctl remove <MAC>` y el `pair` de la preparación. |

**Si nada resuelve:** vuelve a USB, que ya funcionó. Muestra la parte de `/dev` con el CYD si lo tienes conectado, o solo explícala.

### Wi-Fi

| Síntoma | Qué hacer |
|---|---|
| La app envía y al nodo no llega nada | Muchas redes bloquean el broadcast: elige **IP manual** y escribe la IP del PC. |
| Tampoco llega con IP manual | La red aísla a los clientes. No tiene arreglo desde aquí. |
| `ros2 topic hz` marca ~40 Hz | El PC tiene dos interfaces en la misma subred. La tortuga funciona igual; desconecta una o usa IP manual. |

**Si nada resuelve:** usa el Wi-Fi como historia ("esto es lo que pasa en redes reales") y vuelve a USB.

### Si falla la app o el celular

Prueba el nodo sin celular, para saber de qué lado está el problema:
```bash
ros2 launch cyd_teleop demo.launch.py transport:=udp
```
```bash
echo F | nc -u -w0 127.0.0.1 5005
```
La tortuga avanza un poco. Si se mueve, el nodo está bien y el problema está en el celular o en la red.

**Último recurso**, para seguir con la charla moviendo la tortuga con el teclado:
```bash
ros2 run turtlesim turtlesim_node
```
```bash
ros2 run turtlesim turtle_teleop_key
```

## Qué falta confirmar en Kubuntu

Todo lo de esta guía se probó en un Arch con ROS en Docker (el contenedor no abre la ventana de turtlesim). Ninguna prueba se ha hecho todavía en el PC de la charla. Hay que confirmar allí:

- [ ] La ventana de turtlesim abre y la tortuga se mueve.
- [ ] El nombre y la IP de la interfaz del anclaje, y lo que muestra `dmesg`.
- [ ] `/dev/rfcomm0` se crea y queda en el grupo `dialout`, y el nodo lo abre **sin** `sudo`.
- [ ] `ufw`: si está activo, que la regla deje pasar el puerto 5005.
- [ ] Wi-Fi en la red del lugar, si se puede.

| Prueba en Arch | Resultado |
|---|---|
| Wi-Fi: celular → nodo, la tortuga se mueve | Funcionó |
| USB (anclaje): celular → nodo, la tortuga se mueve | Funcionó |
| Bluetooth: `/dev/rfcomm0`, la tortuga se mueve, reconexión | Funcionó |
| Permiso de red local de Android 17 | Sin probar: el celular de las pruebas tiene Android 14 |

## Nota para Arch

Solo si ensayas en Arch; en la charla no aplica.

- **ROS:** no hay paquete nativo, así que se usa Docker. Ver [`docker/Dockerfile`](../docker/Dockerfile). Corre sin ventana: la posición se lee con `ros2 topic echo /turtle1/pose --field x`.
- **Grupo de `/dev/rfcomm0`:** es `uucp`, no `dialout`. En Docker no importa, porque el contenedor corre como root.
- **`nc`:** puede no estar. Usa `echo F | ncat -u 127.0.0.1 5005`.
- **Bluetooth con Docker:** crea primero `/dev/rfcomm0` y pasa `--device /dev/rfcomm0` al contenedor, sin `--net=host`.
