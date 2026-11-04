package com.salademaquinas.turtlepad

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.salademaquinas.turtlepad.ui.theme.Textos
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread

// El PC busca el servicio por este nombre (tools/bt_rfcomm.py)
private const val NOMBRE_SERVICIO = "TurtlePad"
private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val PERIODO_REINTENTO_MS = 2000L

/**
 * Servidor SPP: el PC se conecta y lee las mismas líneas por /dev/rfcomm0.
 *
 * El socket servidor sigue abierto al pasar a segundo plano y entre conexiones, porque
 * Android puede asignar otro canal RFCOMM al reabrirlo, y /dev/rfcomm0 quedó atado al
 * canal anterior. Por eso cerrar() no hace nada; el servidor se apaga con detener().
 */
class TransporteBluetooth(contexto: Context) : Transporte {
    private val adaptador: BluetoothAdapter? =
        contexto.getSystemService(BluetoothManager::class.java)?.adapter

    @Volatile private var hilo: Thread? = null
    @Volatile private var servidor: BluetoothServerSocket? = null
    @Volatile private var conexion: BluetoothSocket? = null
    @Volatile private var canal: Int? = null
    @Volatile private var errorActual: String? = null

    override val destino: String
        get() {
            val conectado = conexion
            return when {
                conectado != null -> "${Textos.BT_CONECTADO}: ${nombreDe(conectado)}"
                canal != null -> "${Textos.BT_ESPERANDO} (${Textos.BT_CANAL} $canal)"
                else -> Textos.BT_ESPERANDO
            }
        }

    override val error: String?
        get() = errorActual

    @Synchronized
    fun iniciar() {
        if (hilo != null) return
        hilo = thread(isDaemon = true, name = "servidor-spp") { aceptarConexiones() }
    }

    @Synchronized
    fun detener() {
        val anterior = hilo
        hilo = null
        anterior?.interrupt()
        cerrarServidor()
        soltarConexion(conexion)
    }

    override fun enviar(letra: Char): Boolean {
        val abierta = conexion ?: return false
        return try {
            abierta.outputStream.write("$letra\n".toByteArray(Charsets.US_ASCII))
            true
        } catch (e: IOException) {
            soltarConexion(abierta)   // el PC se fue; el servidor sigue esperando otro
            false
        }
    }

    override fun cerrar() = Unit

    private fun aceptarConexiones() {
        val yo = Thread.currentThread()
        while (hilo === yo) {
            try {
                val abierto = abrirServidor()
                errorActual = null
                while (hilo === yo) {
                    val nueva = abierto.accept()
                    soltarConexion(conexion)
                    conexion = nueva
                }
            } catch (e: IOException) {
                if (hilo === yo) esperarParaReintentar(mensajeDeError())
            } catch (e: SecurityException) {
                if (hilo === yo) esperarParaReintentar(Textos.ERROR_PERMISO_BLUETOOTH)
            } finally {
                cerrarServidor()
                soltarConexion(conexion)
            }
        }
    }

    private fun abrirServidor(): BluetoothServerSocket {
        val nuevo = adaptador?.listenUsingRfcommWithServiceRecord(NOMBRE_SERVICIO, UUID_SPP)
            ?: throw IOException("sin adaptador")
        servidor = nuevo
        // Android no publica el canal; solo aparece en toString(). Es solo informativo.
        canal = Regex("Channel: (\\d+)").find(nuevo.toString())
            ?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
        return nuevo
    }

    private fun mensajeDeError(): String = when {
        adaptador == null -> Textos.ERROR_SIN_BLUETOOTH
        !adaptador.isEnabled -> Textos.ERROR_BLUETOOTH_APAGADO
        else -> Textos.ERROR_SERVIDOR_BLUETOOTH
    }

    private fun esperarParaReintentar(mensaje: String) {
        errorActual = mensaje
        try {
            Thread.sleep(PERIODO_REINTENTO_MS)
        } catch (e: InterruptedException) {
            // detener(): el bucle ve que ya no es el hilo actual y termina
        }
    }

    private fun cerrarServidor() {
        try {
            servidor?.close()
        } catch (e: IOException) {
            // ya estaba cerrado
        }
        servidor = null
        canal = null
    }

    @Synchronized
    private fun soltarConexion(cual: BluetoothSocket?) {
        if (cual == null) return
        try {
            cual.close()
        } catch (e: IOException) {
            // ya estaba cerrada
        }
        if (conexion === cual) conexion = null
    }

    private fun nombreDe(socket: BluetoothSocket): String = try {
        socket.remoteDevice.name ?: socket.remoteDevice.address
    } catch (e: SecurityException) {
        socket.remoteDevice.address
    }
}
