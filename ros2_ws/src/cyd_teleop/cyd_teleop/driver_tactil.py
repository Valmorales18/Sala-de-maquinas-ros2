"""Nodo driver_tactil: lee las letras del CYD por serial y mueve turtlesim.

Es el ÚNICO lector del puerto serial. Lee sin bloquear en un timer de 50 Hz.
"""

import errno

import rclpy
from rclpy.executors import ExternalShutdownException
from rclpy.node import Node
from rclpy.signals import SignalHandlerOptions
import serial

from geometry_msgs.msg import Twist
from std_msgs.msg import String

from cyd_teleop.traduccion import traducir

PERIODO_LECTURA_S = 1.0 / 50.0   # timer de 50 Hz
PERIODO_REINTENTO_S = 2.0        # reintento de apertura del puerto
MAX_BUFFER = 1024                # si llega basura sin '\n', no crecer sin límite


def _es_error_de_permisos(e: Exception) -> bool:
    # pyserial envuelve el PermissionError en SerialException con errno EACCES
    return isinstance(e, PermissionError) or getattr(e, 'errno', None) == errno.EACCES


class DriverTactil(Node):

    def __init__(self):
        super().__init__('driver_tactil')

        # ---------- Parámetros ----------
        self.port = self.declare_parameter('port', '/dev/esp32_tactil').value
        self.baud = int(self.declare_parameter('baud', 115200).value)
        self.v = float(self.declare_parameter('linear_speed', 2.0).value)
        self.w = float(self.declare_parameter('angular_speed', 2.0).value)
        self.timeout_s = float(self.declare_parameter('timeout_s', 0.5).value)
        cmd_topic = self.declare_parameter('cmd_topic', '/turtle1/cmd_vel').value

        # ---------- Publicadores ----------
        # turtlesim (Lyrical) se suscribe con geometry_msgs/Twist, RELIABLE
        self.pub_cmd = self.create_publisher(Twist, cmd_topic, 10)
        self.pub_raw = self.create_publisher(String, '/tactil/raw', 10)

        # ---------- Estado ----------
        self.ser = None
        self.buffer = b''
        self.ultimo_intento = None          # instante del último intento de apertura
        self.ultima_valida = self._ahora()  # instante de la última letra válida
        self.parado_por_timeout = True      # al arrancar no hay nada que parar

        self._abrir_puerto()
        self.create_timer(PERIODO_LECTURA_S, self._tick)

        self.get_logger().info(
            f'driver_tactil: puerto={self.port} baud={self.baud} '
            f'v={self.v} w={self.w} timeout={self.timeout_s}s -> {cmd_topic}')

    # ---------- Utilidades ----------
    def _ahora(self) -> float:
        return self.get_clock().now().nanoseconds * 1e-9

    def _publicar_twist(self, lin_x: float, ang_z: float):
        msg = Twist()
        msg.linear.x = lin_x
        msg.angular.z = ang_z
        self.pub_cmd.publish(msg)

    # ---------- Puerto serial ----------
    def _abrir_puerto(self):
        self.ultimo_intento = self._ahora()
        try:
            # timeout=0: read() nunca bloquea. exclusive=True evita que otra
            # instancia del driver abra el mismo puerto (cat no lo respeta).
            self.ser = serial.Serial(self.port, self.baud, timeout=0, exclusive=True)
            self.ser.reset_input_buffer()   # descarta lo acumulado antes de abrir
            self.buffer = b''
            self.get_logger().info(f'Puerto {self.port} abierto')
        except (serial.SerialException, OSError, ValueError) as e:
            self._cerrar_puerto()
            if _es_error_de_permisos(e):
                self.get_logger().error(
                    f'Sin permiso para abrir {self.port}: {e}. Agrega tu usuario al '
                    f'grupo dialout (sudo usermod -aG dialout $USER) y cierra sesión. '
                    f'Reintento en {PERIODO_REINTENTO_S:.0f} s')
            else:
                self.get_logger().error(
                    f'No se pudo abrir {self.port}: {e}. '
                    f'Reintento en {PERIODO_REINTENTO_S:.0f} s')

    def _cerrar_puerto(self):
        if self.ser is not None:
            try:
                self.ser.close()
            except Exception:
                pass
        self.ser = None
        self.buffer = b''

    # ---------- Bucle principal (50 Hz) ----------
    def _tick(self):
        ahora = self._ahora()

        if self.ser is None:
            if ahora - self.ultimo_intento >= PERIODO_REINTENTO_S:
                self._abrir_puerto()
        else:
            self._leer_serial()

        # Seguridad: sin letra válida durante timeout_s -> Twist en cero, una vez
        if not self.parado_por_timeout and ahora - self.ultima_valida > self.timeout_s:
            self._publicar_twist(0.0, 0.0)
            self.parado_por_timeout = True
            self.get_logger().warning(
                f'Sin datos válidos en {self.timeout_s} s: tortuga detenida')

    def _leer_serial(self):
        try:
            n = self.ser.in_waiting
            if n:
                self.buffer += self.ser.read(n)
        except (serial.SerialException, OSError) as e:
            self.get_logger().error(
                f'Se perdió el puerto {self.port}: {e}. '
                f'Reintento en {PERIODO_REINTENTO_S:.0f} s')
            self._cerrar_puerto()
            self.ultimo_intento = self._ahora()
            return

        # Procesa TODAS las líneas completas; lo que queda es una línea a medias
        *lineas, self.buffer = self.buffer.split(b'\n')
        if len(self.buffer) > MAX_BUFFER:
            self.buffer = b''
        for linea in lineas:
            self._procesar_linea(linea)

    def _procesar_linea(self, linea_bytes: bytes):
        texto = linea_bytes.decode('ascii', errors='ignore').strip()
        if not texto:
            return

        self.pub_raw.publish(String(data=texto))

        vel = traducir(texto, self.v, self.w)
        if vel is None:
            self.get_logger().warning(
                f'Línea ignorada del serial: {texto!r}', throttle_duration_sec=2.0)
            return

        self._publicar_twist(*vel)
        self.ultima_valida = self._ahora()
        self.parado_por_timeout = False

    def detener(self):
        """Deja la tortuga quieta y libera el puerto al cerrar el nodo."""
        try:
            self._publicar_twist(0.0, 0.0)
        except (Exception, KeyboardInterrupt):
            pass   # un segundo Ctrl+C durante el cierre no debe soltar traceback
        self._cerrar_puerto()


def main(args=None):
    # Sin el handler de señales de rclpy: así, con Ctrl+C, el contexto sigue vivo
    # y se alcanza a publicar el Twist en cero antes de cerrar.
    rclpy.init(args=args, signal_handler_options=SignalHandlerOptions.NO)
    nodo = DriverTactil()
    try:
        rclpy.spin(nodo)
    except (KeyboardInterrupt, ExternalShutdownException):
        pass
    finally:
        nodo.detener()
        try:
            nodo.destroy_node()
            rclpy.try_shutdown()
        except KeyboardInterrupt:
            pass


if __name__ == '__main__':
    main()
