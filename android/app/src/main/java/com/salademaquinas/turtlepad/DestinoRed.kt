package com.salademaquinas.turtlepad

/** Lógica pura para elegir a dónde enviar, sin dependencias de Android (para poder probarla). */

/** Una interfaz de red con IPv4. La dirección son los 4 bytes de la IP. */
data class InterfazIpv4(val nombre: String, val ip: ByteArray, val prefijo: Int)

private val PREFIJOS_USB = listOf("rndis", "ncm", "usb", "eth")
// wlan es el cliente Wi-Fi; los demás son el punto de acceso del propio celular
private val PREFIJOS_WIFI = listOf("wlan", "swlan", "ap", "softap")

private fun elegir(interfaces: List<InterfazIpv4>, prefijos: List<String>): InterfazIpv4? {
    for (prefijo in prefijos) {
        interfaces.filter { it.nombre.startsWith(prefijo) }
            .minByOrNull { it.nombre }
            ?.let { return it }
    }
    return null
}

fun elegirInterfazUsb(interfaces: List<InterfazIpv4>) = elegir(interfaces, PREFIJOS_USB)

fun elegirInterfazWifi(interfaces: List<InterfazIpv4>) = elegir(interfaces, PREFIJOS_WIFI)

/**
 * Broadcast de la subred, calculado con el prefijo porque no todos los celulares lo reportan.
 * Devuelve null si la subred no tiene broadcast (/31, /32).
 */
fun calcularBroadcast(ip: ByteArray, prefijo: Int): ByteArray? {
    if (ip.size != 4 || prefijo !in 1..30) return null
    val mascara = -1 shl (32 - prefijo)
    val direccion = ip.fold(0) { acumulado, byte -> (acumulado shl 8) or (byte.toInt() and 0xFF) }
    val broadcast = direccion or mascara.inv()
    return ByteArray(4) { i -> (broadcast ushr (24 - 8 * i)).toByte() }
}

/** Convierte "192.168.1.10" en sus 4 bytes, sin resolver nombres. Null si no es una IPv4. */
fun parsearIpv4(texto: String): ByteArray? {
    val partes = texto.trim().split('.')
    if (partes.size != 4) return null
    val bytes = partes.map { parte ->
        if (parte.isEmpty() || parte.length > 3 || !parte.all { it in '0'..'9' }) return null
        val valor = parte.toInt()
        if (valor > 255) return null
        valor.toByte()
    }
    return bytes.toByteArray()
}

fun ipATexto(ip: ByteArray) = ip.joinToString(".") { (it.toInt() and 0xFF).toString() }
