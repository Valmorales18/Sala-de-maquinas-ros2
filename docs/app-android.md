# App Android: Turtle Pad

Reemplaza la pantalla del CYD: una cruceta que manda el mismo protocolo (una letra por línea, a 20 Hz) por USB, Wi-Fi o Bluetooth. Está en `android/`, en Kotlin y Jetpack Compose. Lo de Arch está en [una nota al final](#nota-para-arch).

- **Paquete:** `com.salademaquinas.turtlepad`
- **Android:** desde 8.0 (minSdk 26), con targetSdk 37 (Android 17)
- **Probada en:** Infinix X6833B con Android 14

Para conectarla al PC, ver [transportes](transportes.md).

## Instalar

Descarga el APK firmado desde [la última versión publicada](https://github.com/Valmorales18/Sala-de-maquinas-ros2/releases/latest).

**Desde el celular:** abre ese enlace, descarga `turtle-pad-v<versión>.apk` y ábrelo. Android puede pedirte que permitas instalar apps de esa fuente.

**Desde Kubuntu:** activa la depuración USB en el celular, descarga el APK en el PC y corre esto en la carpeta donde quedó (aquí, la versión 1.0):

```bash
sudo apt install adb
adb install -r turtle-pad-v1.0.apk
```

Si el celular tiene una versión compilada en modo depuración, desinstálala primero con `adb uninstall com.salademaquinas.turtlepad`: tiene otra firma y Android rechaza instalar encima. Al desinstalar se borran los ajustes de la app.

## Compilar

Se necesita:

- **JDK:** JDK 25 (`sudo apt install openjdk-25-jdk`).
- **Android SDK**, con `ANDROID_HOME` definido. Se instala con Android Studio o con las herramientas de línea de comandos. Con `sdkmanager`:
  ```bash
  sdkmanager 'platform-tools' 'platforms;android-37.0' 'build-tools;37.0.0'
  ```

No hace falta instalar Gradle: el proyecto trae su wrapper.

```bash
cd ~/Sala-de-maquinas-ros2/android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para editar la app, abre la carpeta `android/` en Android Studio, no la raíz del repo.

### Pruebas

```bash
./gradlew testDebugUnitTest
```

Son 9 tests de la parte pura: qué letra manda el mando y a qué dirección enviar.

## Firmar el APK de release

Crea la llave una sola vez y **guárdala fuera de git** (`*.jks`, `*.keystore` y `keystore.properties` están ignorados):

```bash
cd ~/Sala-de-maquinas-ros2/android
keytool -genkeypair -v -keystore turtle-pad.jks -alias turtle-pad -keyalg RSA -keysize 4096 -validity 10000
cp keystore.properties.example keystore.properties   # y pon tus contraseñas
./gradlew assembleRelease
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs app/build/outputs/apk/release/turtle-pad-v1.0.apk
```

También se pueden usar las variables `TURTLEPAD_STORE_FILE`, `TURTLEPAD_STORE_PASSWORD`, `TURTLEPAD_KEY_ALIAS` y `TURTLEPAD_KEY_PASSWORD`. Sin llave, el APK de release sale **sin firmar** y Android no lo instala.

Sin esa llave y sus contraseñas no podrás firmar actualizaciones que se instalen encima de esa versión.

## Publicar

Desde la raíz del repo:

```bash
gh release create v1.0 android/app/build/outputs/apk/release/turtle-pad-v1.0.apk --title "Turtle Pad v1.0" --notes "App de la charla"
```

Para una versión nueva, sube `versionCode` y `versionName` en `android/app/build.gradle.kts`. El APK toma su nombre de `versionName`: `turtle-pad-v<versión>.apk`.

## Cómo se usa

1. **Presentación:** revisa en orden USB, Wi-Fi y Bluetooth, y preselecciona el primero disponible. Dura entre 1,5 y 3 s; un toque la salta. Se repite cada vez que la app se abre desde cero, así que puede cambiar la conexión elegida.
2. **Cruceta:** mientras presionas una flecha se envía su letra; al soltar, `S`. Con dos dedos manda el último botón. Deslizar el dedo fuera del botón también lo suelta. El círculo central muestra la letra que se envía, en rojo si es `S`.
3. **Ajustes:** conexión; en USB y Wi-Fi, destino automático o IP manual, y puerto. Se guardan en el celular.
4. **Barra de estado:** transporte, destino, paquetes por segundo y último error.
5. **Segundo plano:** envía `S` y deja de enviar. Al volver, retoma.

La pantalla queda siempre encendida y en vertical.

## Permisos

| Permiso | Cuándo | Para qué |
|---|---|---|
| `ACCESS_LOCAL_NETWORK` | Android 17 o superior, en USB y Wi-Fi | Sin él, el UDP local falla con `EPERM` |
| `BLUETOOTH_CONNECT` | Android 12 o superior, en Bluetooth | Abrir el servidor SPP |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | Hasta Android 11 | Lo mismo en versiones antiguas |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE` | Siempre; no piden aviso | Sockets, elegir la red y vibrar |

La app pide el permiso de la conexión elegida. Si se negó, muestra el botón **Conceder permiso**.

No se pide `BLUETOOTH_SCAN` porque la app no busca dispositivos: es el PC quien se conecta. Pese a eso, el aviso del sistema menciona "encontrar dispositivos cercanos", porque agrupa estos permisos.

## Arquitectura

El código está en `android/app/src/main/java/com/salademaquinas/turtlepad/`:

| Archivo | Qué hace |
|---|---|
| `MainActivity.kt` | Ventana, permisos, vibración; arranca y corta el envío según el ciclo de vida |
| `PadViewModel.kt` | El bucle de envío de 20 Hz y el estado de la pantalla |
| `Mando.kt` | Qué letra toca enviar según los botones presionados (lógica pura) |
| `Transporte.kt` | La interfaz común de los transportes |
| `TransporteUdp.kt` | Envío por USB y Wi-Fi |
| `TransporteBluetooth.kt` | Servidor SPP |
| `DestinoRed.kt` | Elegir interfaz, calcular el broadcast y validar la IP (lógica pura) |
| `Ajustes.kt` | Ajustes guardados con DataStore |
| `ui/PantallaPresentacion.kt` | Pantalla de carga con los chequeos reales |
| `ui/PantallaControl.kt` | Cruceta, ajustes y barra de estado |
| `ui/AcercaDe.kt` | Diálogo "Acerca de" |
| `ui/theme/Tema.kt` | Colores, fuentes y textos |

### Decisiones de diseño

- **Periodo fijo, no por evento.** Una corrutina envía cada 50 ms con hora absoluta, así que un envío tardío no atrasa a los siguientes. Igual que el firmware, envía aunque la letra sea `S`.
- **Revisa las interfaces cada segundo.** El anclaje puede activarse después de abrir la app, y la IP puede cambiar en plena demo.
- **Elige la interfaz por nombre:** para USB, `rndis`, `ncm`, `usb` o `eth`; para Wi-Fi, `wlan`, `swlan`, `ap` o `softap`. Si un celular usa otro nombre, la solución inmediata es la IP manual.
- **Calcula el broadcast con el prefijo de red**, porque no todos los celulares lo reportan.
- **Ata el socket a la interfaz.** Si no, el paquete sigue la red por defecto del celular, que puede ser los datos móviles (por ejemplo, en un Wi-Fi sin internet).
- **La IP manual no resuelve nombres.** Solo acepta IPv4, para no bloquear nunca el envío esperando DNS.
- **El servidor Bluetooth sigue abierto en segundo plano** para no cambiar de canal: ver [cuándo cambia el canal](transportes.md#cuándo-cambia-el-canal).

### Cambiar el tema

Todo está en `ui/theme/Tema.kt`: `Colores`, `Fuentes` y `Textos`. Solo estos valores están fuera de ese archivo:

| Qué | Dónde |
|---|---|
| Color del splash del sistema y del fondo de la ventana | `res/values/colors.xml` |
| Nombre visible de la app | `res/values/strings.xml` |
| Ícono | `res/drawable/ic_launcher_*.xml` y `res/mipmap-anydpi-v26/` |

### Recursos y licencias

| Recurso | Dónde | Licencia o crédito |
|---|---|---|
| Permanent Marker | `res/font/permanent_marker.ttf` | Apache 2.0; incluida en `assets/licencias/` |
| Patrick Hand | `res/font/patrick_hand.ttf` | OFL; incluida en `assets/licencias/` |
| Logos e ilustración | `res/drawable-nodpi/` | Rama Estudiantil IEEE y Universidad del Valle. Tux: Larry Ewing, creado con The GIMP |
| Ícono, en SVG y PNG de 512 | [`docs/icono/`](icono/) | Del proyecto |

## Nota para Arch

- **JDK:** `sudo pacman -S jdk25-openjdk`. Si tienes varias versiones, revisa cuál usa `java -version`.
- **`adb`:** con `/usr/bin/adb`, la compilación y la instalación funcionan igual que en Kubuntu.
