package com.salademaquinas.turtlepad

import android.app.Application
import android.os.SystemClock
import com.salademaquinas.turtlepad.ui.theme.Textos
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

private const val PERIODO_ENVIO_MS = 50L   // 20 Hz, igual que el firmware del CYD
private const val PERIODO_ESTADO_MS = 1000L

data class EstadoEnvio(
    val destino: String? = null,
    val paquetesPorSegundo: Int = 0,
    val error: String? = null,
)

class PadViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = RepositorioAjustes(aplicacion)

    val mando = Mando()
    val ajustes: StateFlow<Ajustes?> =
        repositorio.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _estado = MutableStateFlow(EstadoEnvio())
    val estado: StateFlow<EstadoEnvio> = _estado

    /** El envío arranca al terminar la pantalla de presentación. */
    val presentacionTerminada = MutableStateFlow(false)

    /** Android 17: hay que concederlo antes de tocar la red local. */
    val permisoRed = MutableStateFlow(true)

    /** Preselecciona lo que encontró la presentación; si no encontró nada, deja lo guardado. */
    fun terminarPresentacion(disponible: Conexion?) = viewModelScope.launch {
        disponible?.let { repositorio.guardarConexion(it) }
        presentacionTerminada.value = true
    }

    fun guardarConexion(valor: Conexion) = viewModelScope.launch { repositorio.guardarConexion(valor) }
    fun guardarManual(valor: Boolean) = viewModelScope.launch { repositorio.guardarManual(valor) }
    fun guardarIp(valor: String) = viewModelScope.launch { repositorio.guardarIp(valor) }
    fun guardarPuerto(valor: Int) = viewModelScope.launch { repositorio.guardarPuerto(valor) }

    /**
     * Envía a 20 Hz mientras la pantalla esté visible; la Activity cancela esta función al
     * pasar a segundo plano. Si cambian los ajustes, reinicia el transporte.
     */
    suspend fun enviarMientrasVisible() {
        try {
            combine(repositorio.ajustes, permisoRed, ::Pair)
                .distinctUntilChanged()
                .collectLatest { (ajustes, permiso) ->
                    withContext(Dispatchers.IO) { enviarCon(ajustes, permiso) }
                }
        } finally {
            mando.soltarTodo()
        }
    }

    private suspend fun enviarCon(ajustes: Ajustes, permiso: Boolean) {
        val necesitaRed = ajustes.conexion != Conexion.BLUETOOTH
        if (necesitaRed && !permiso) {
            _estado.value = EstadoEnvio(error = Textos.ERROR_PERMISO_RED)
            return
        }
        val transporte = crearTransporte(ajustes) ?: return
        try {
            bucleDeEnvio(transporte)
        } finally {
            // Al salir (segundo plano o cambio de ajustes) deja la tortuga quieta
            withContext(NonCancellable) {
                transporte.enviar(LETRA_STOP)
                transporte.cerrar()
            }
        }
    }

    private fun crearTransporte(ajustes: Ajustes): Transporte? = when (ajustes.conexion) {
        Conexion.USB, Conexion.WIFI -> TransporteUdp(getApplication(), ajustes)
        Conexion.BLUETOOTH -> null
    }

    /** Periodo fijo con hora absoluta: enviar tarde en un ciclo no atrasa los siguientes. */
    private suspend fun bucleDeEnvio(transporte: Transporte) {
        var siguienteEnvio = SystemClock.elapsedRealtime()
        var inicioVentana = siguienteEnvio
        var enviados = 0
        _estado.value = EstadoEnvio(destino = transporte.destino)

        while (coroutineContext.isActive) {
            if (transporte.enviar(mando.letra.value)) enviados++

            val ahora = SystemClock.elapsedRealtime()
            if (ahora - inicioVentana >= PERIODO_ESTADO_MS) {
                _estado.value = EstadoEnvio(transporte.destino, enviados, transporte.error)
                inicioVentana = ahora
                enviados = 0
            }

            siguienteEnvio += PERIODO_ENVIO_MS
            if (siguienteEnvio <= ahora) siguienteEnvio = ahora + PERIODO_ENVIO_MS
            delay(siguienteEnvio - ahora)
        }
    }
}
