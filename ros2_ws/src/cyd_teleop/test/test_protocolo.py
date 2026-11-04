"""Pruebas de la separación de un datagrama en líneas."""

import pytest

from cyd_teleop.protocolo import lineas_de_datagrama
from cyd_teleop.traduccion import traducir


@pytest.mark.parametrize('datagrama, esperado', [
    (b'F\n', [b'F']),
    (b'F', [b'F']),                         # sin "\n" final también cuenta
    (b'F\r\n', [b'F']),                     # igual que el firmware del CYD
    (b'F\nL\nS\n', [b'F', b'L', b'S']),     # varias líneas en un datagrama
    (b'F\r\nL\r\n', [b'F', b'L']),
    (b'  R \n', [b'R']),
])
def test_separa_las_lineas(datagrama, esperado):
    assert lineas_de_datagrama(datagrama) == esperado


@pytest.mark.parametrize('datagrama', [b'', b'\n', b'\r\n', b'\n\n\n', b'   \n'])
def test_datagrama_sin_lineas(datagrama):
    assert lineas_de_datagrama(datagrama) == []


def test_conserva_el_orden_y_deja_la_validacion_a_traducir():
    lineas = lineas_de_datagrama(b'F\nhola\n\xff\nS\n')
    assert lineas == [b'F', b'hola', b'\xff', b'S']
    assert [traducir(linea, 2.0, 1.0) for linea in lineas] == [
        (2.0, 0.0), None, None, (0.0, 0.0)]
