package com.salademaquinas.turtlepad

import org.junit.Assert.assertEquals
import org.junit.Test

class MandoTest {
    @Test
    fun presionarYSoltar() {
        val mando = Mando()
        assertEquals('S', mando.letra.value)
        mando.presionar('F')
        assertEquals('F', mando.letra.value)
        mando.soltar('F')
        assertEquals('S', mando.letra.value)
    }

    @Test
    fun conDosDedosMandaElUltimoYVuelveAlAnterior() {
        val mando = Mando()
        mando.presionar('F')
        mando.presionar('L')
        assertEquals('L', mando.letra.value)
        mando.soltar('L')
        assertEquals('F', mando.letra.value)
        mando.soltar('F')
        assertEquals('S', mando.letra.value)
    }

    @Test
    fun soltarElQueNoMandaNoCambiaLaLetra() {
        val mando = Mando()
        mando.presionar('F')
        mando.presionar('L')
        mando.soltar('F')
        assertEquals('L', mando.letra.value)
    }

    @Test
    fun soltarTodoVuelveAStop() {
        val mando = Mando()
        mando.presionar('F')
        mando.presionar('R')
        mando.soltarTodo()
        assertEquals('S', mando.letra.value)
    }
}
