package com.salademaquinas.turtlepad

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val LETRA_STOP = 'S'

/**
 * Qué letra toca enviar según los botones presionados.
 * Con dos dedos manda el último botón; al soltarlo vuelve al que sigue presionado.
 */
class Mando {
    private val presionados = mutableListOf<Char>()
    private val _letra = MutableStateFlow(LETRA_STOP)
    val letra: StateFlow<Char> = _letra

    @Synchronized
    fun presionar(letra: Char) {
        presionados.remove(letra)
        presionados.add(letra)
        _letra.value = letra
    }

    @Synchronized
    fun soltar(letra: Char) {
        presionados.remove(letra)
        _letra.value = presionados.lastOrNull() ?: LETRA_STOP
    }

    @Synchronized
    fun soltarTodo() {
        presionados.clear()
        _letra.value = LETRA_STOP
    }
}
