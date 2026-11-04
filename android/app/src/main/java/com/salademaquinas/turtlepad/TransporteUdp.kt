package com.salademaquinas.turtlepad

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock
import android.util.Log
import com.salademaquinas.turtlepad.ui.theme.Textos
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketAddress

private const val PERIODO_REVISION_MS = 1000L

fun listarInterfaces(): List<InterfazIpv4> = try {
    NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().flatMap { interfaz ->
        interfaz.interfaceAddresses
            .filter { it.address is Inet4Address && !it.address.isLoopbackAddress }
            .map { InterfazIpv4(interfaz.name, it.address.address, it.networkPrefixLength.toInt()) }
    }
} catch (e: IOException) {
    emptyList()
}

/**
 * Envía datagramas UDP por el anclaje USB o por Wi-Fi.
 *
 * Revisa las interfaces cada segundo: el anclaje puede activarse después de abrir la app,
 * y la IP o la red pueden cambiar en plena demo.
 */
class TransporteUdp(contexto: Context, private val ajustes: Ajustes) : Transporte {
    private val conectividad = contexto.getSystemService(ConnectivityManager::class.java)

    private var socket: DatagramSocket? = null
    private var ipLocalDelSocket: InetAddress? = null
    private var direccionDestino: InetAddress? = null
    private var ultimaRevision = 0L
    private var errorDeRevision: String? = null
    private var errorDeEnvio: String? = null

    override val destino: String?
        get() = direccionDestino?.let { "${it.hostAddress}:${ajustes.puerto}" }

    override val error: String?
        get() = errorDeRevision ?: errorDeEnvio

    override fun enviar(letra: Char): Boolean {
        revisarDestino()
        val direccion = direccionDestino ?: return false
        val abierto = socket ?: return false
        val datos = "$letra\n".toByteArray(Charsets.US_ASCII)
        return try {
            abierto.send(DatagramPacket(datos, datos.size, direccion, ajustes.puerto))
            errorDeEnvio = null
            true
        } catch (e: IOException) {
            errorDeEnvio = mensajeDeError(e)
            cerrar()   // se reabre en la próxima revisión
            false
        }
    }

    override fun cerrar() {
        socket?.close()
        socket = null
        ipLocalDelSocket = null
    }

    private fun revisarDestino() {
        val ahora = SystemClock.elapsedRealtime()
        if (ultimaRevision != 0L && ahora - ultimaRevision < PERIODO_REVISION_MS) return
        ultimaRevision = ahora

        val interfaz = interfazDeLaConexion()
        val destinoBytes = if (ajustes.manual) {
            parsearIpv4(ajustes.ip)
                ?: return sinDestino(Textos.ERROR_IP_INVALIDA)
        } else {
            if (interfaz == null) {
                return sinDestino(
                    if (ajustes.conexion == Conexion.USB) Textos.ERROR_SIN_USB else Textos.ERROR_SIN_WIFI)
            }
            calcularBroadcast(interfaz.ip, interfaz.prefijo)
                ?: return sinDestino(Textos.ERROR_SIN_BROADCAST)
        }
        errorDeRevision = null
        direccionDestino = InetAddress.getByAddress(destinoBytes)

        val ipLocal = interfaz?.let { InetAddress.getByAddress(it.ip) }
        if (socket == null || ipLocal != ipLocalDelSocket) {
            cerrar()
            abrirSocket(interfaz, ipLocal)
        }
    }

    private fun sinDestino(mensaje: String) {
        errorDeRevision = mensaje
        direccionDestino = null
        cerrar()
    }

    private fun abrirSocket(interfaz: InterfazIpv4?, ipLocal: InetAddress?) {
        val nuevo = DatagramSocket(null as SocketAddress?)
        try {
            nuevo.broadcast = true
            nuevo.bind(ipLocal?.let { InetSocketAddress(it, 0) })
            interfaz?.let { atarARed(nuevo, it.nombre) }
            socket = nuevo
            ipLocalDelSocket = ipLocal
        } catch (e: IOException) {
            nuevo.close()
            errorDeEnvio = mensajeDeError(e)
        }
    }

    /**
     * Si no se ata a la red de la interfaz, el paquete sigue la red por defecto del celular,
     * que puede ser los datos móviles (p. ej. en un Wi-Fi sin internet). El anclaje USB no es
     * una Network de Android: ahí basta la IP local del bind.
     */
    private fun atarARed(nuevo: DatagramSocket, interfaz: String) {
        val red = conectividad.allNetworks.firstOrNull {
            conectividad.getLinkProperties(it)?.interfaceName == interfaz
        } ?: return
        try {
            red.bindSocket(nuevo)
        } catch (e: IOException) {
            Log.w("TurtlePad", "No se pudo atar el socket a $interfaz", e)
        }
    }

    private fun interfazDeLaConexion(): InterfazIpv4? {
        val interfaces = listarInterfaces()
        return if (ajustes.conexion == Conexion.USB) elegirInterfazUsb(interfaces) else elegirInterfazWifi(interfaces)
    }

    private fun mensajeDeError(e: IOException): String {
        val detalle = e.message.orEmpty()
        return when {
            "EPERM" in detalle || "not permitted" in detalle -> Textos.ERROR_RED_BLOQUEADA
            "ENETUNREACH" in detalle || "unreachable" in detalle -> Textos.ERROR_RED_INALCANZABLE
            else -> "${Textos.ERROR_ENVIO}: $detalle"
        }
    }
}
