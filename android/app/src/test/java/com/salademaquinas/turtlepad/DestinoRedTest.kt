package com.salademaquinas.turtlepad

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinoRedTest {
    private fun ip(texto: String) = parsearIpv4(texto)!!
    private fun interfaz(nombre: String) = InterfazIpv4(nombre, ip("192.168.42.129"), 24)

    @Test
    fun broadcastDeSubredes() {
        assertEquals("192.168.42.255", ipATexto(calcularBroadcast(ip("192.168.42.129"), 24)!!))
        assertEquals("10.0.15.255", ipATexto(calcularBroadcast(ip("10.0.12.7"), 22)!!))
        assertEquals("172.16.255.255", ipATexto(calcularBroadcast(ip("172.16.0.200"), 16)!!))
        assertEquals("192.168.1.127", ipATexto(calcularBroadcast(ip("192.168.1.100"), 25)!!))
    }

    @Test
    fun subredesSinBroadcast() {
        assertNull(calcularBroadcast(ip("10.0.0.1"), 32))
        assertNull(calcularBroadcast(ip("10.0.0.1"), 31))
        assertNull(calcularBroadcast(ip("10.0.0.1"), 0))
    }

    @Test
    fun eligeLaInterfazDelAnclajeUsb() {
        val lista = listOf(interfaz("rmnet_data0"), interfaz("wlan0"), interfaz("ncm0"))
        assertEquals("ncm0", elegirInterfazUsb(lista)?.nombre)
        assertEquals("rndis0", elegirInterfazUsb(listOf(interfaz("usb0"), interfaz("rndis0")))?.nombre)
        assertNull(elegirInterfazUsb(listOf(interfaz("rmnet_data0"), interfaz("wlan0"))))
    }

    @Test
    fun eligeLaInterfazWifi() {
        val lista = listOf(interfaz("rmnet_data0"), interfaz("ncm0"), interfaz("wlan1"), interfaz("wlan0"))
        assertEquals("wlan0", elegirInterfazWifi(lista)?.nombre)
        assertEquals("swlan0", elegirInterfazWifi(listOf(interfaz("swlan0")))?.nombre)
        assertNull(elegirInterfazWifi(listOf(interfaz("rmnet_data0"))))
    }

    @Test
    fun parseaIpv4SinResolverNombres() {
        assertArrayEquals(byteArrayOf(192.toByte(), 168.toByte(), 1, 10), parsearIpv4(" 192.168.1.10 "))
        listOf("", "1.2.3", "1.2.3.4.5", "256.1.1.1", "a.b.c.d", "pc.local", "1..2.3", "1.2.3.-4")
            .forEach { assertNull(it, parsearIpv4(it)) }
    }
}
