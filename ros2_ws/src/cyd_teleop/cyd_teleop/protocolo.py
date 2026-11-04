"""Separación de datagramas en líneas, sin dependencias de ROS (para poder probarla)."""

from typing import List


def lineas_de_datagrama(datagrama: bytes) -> List[bytes]:
    """Separa un datagrama UDP en sus líneas no vacías, sin espacios ni "\\r\\n".

    A diferencia del serial, un datagrama llega entero o no llega: no hay líneas
    a medias que guardar para el siguiente, y el último "\\n" es opcional.
    """
    return [linea.strip() for linea in datagrama.split(b'\n') if linea.strip()]
