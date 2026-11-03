"""Traducción letra -> velocidad, sin dependencias de ROS (para poder probarla)."""

from typing import Optional, Tuple, Union

# Letras que envía el firmware del CYD
LETRAS_VALIDAS = ('F', 'B', 'L', 'R', 'S')


def traducir(linea: Union[str, bytes],
             linear_speed: float,
             angular_speed: float) -> Optional[Tuple[float, float]]:
    """Convierte una línea del serial en (linear.x, angular.z).

    Acepta str o bytes; limpia espacios y "\\r\\n". Devuelve None si la línea
    no es una letra válida (basura, vacía, varias letras, minúsculas...).
    """
    if isinstance(linea, bytes):
        linea = linea.decode('ascii', errors='ignore')
    letra = linea.strip()

    if letra == 'F':
        return (linear_speed, 0.0)
    if letra == 'B':
        return (-linear_speed, 0.0)
    if letra == 'L':
        return (0.0, angular_speed)
    if letra == 'R':
        return (0.0, -angular_speed)
    if letra == 'S':
        return (0.0, 0.0)
    return None
