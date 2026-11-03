"""Pruebas de la traducción letra -> (linear.x, angular.z)."""

import pytest

from cyd_teleop.traduccion import traducir

V = 2.0
W = 1.5


@pytest.mark.parametrize('linea, esperado', [
    ('F', (V, 0.0)),
    ('B', (-V, 0.0)),
    ('L', (0.0, W)),
    ('R', (0.0, -W)),
    ('S', (0.0, 0.0)),
])
def test_letras_validas(linea, esperado):
    assert traducir(linea, V, W) == esperado


@pytest.mark.parametrize('linea', ['X', 'f', 'FF', 'hola', '1', '\x00\xff', '?'])
def test_basura_se_ignora(linea):
    assert traducir(linea, V, W) is None


@pytest.mark.parametrize('linea', ['', '\r\n', '   '])
def test_linea_vacia_se_ignora(linea):
    assert traducir(linea, V, W) is None


@pytest.mark.parametrize('linea', ['F\r', 'F\r\n', ' F \r'])
def test_letra_con_retorno_de_carro(linea):
    assert traducir(linea, V, W) == (V, 0.0)


def test_acepta_bytes():
    assert traducir(b'R\r\n', V, W) == (0.0, -W)
    assert traducir(b'\xffL\r\n', V, W) == (0.0, W)   # bytes no ASCII se descartan
