package com.salademaquinas.turtlepad.ui

import android.bluetooth.BluetoothManager
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salademaquinas.turtlepad.Conexion
import com.salademaquinas.turtlepad.R
import com.salademaquinas.turtlepad.elegirInterfazUsb
import com.salademaquinas.turtlepad.elegirInterfazWifi
import com.salademaquinas.turtlepad.listarInterfaces
import com.salademaquinas.turtlepad.ui.theme.Colores
import com.salademaquinas.turtlepad.ui.theme.Fuentes
import com.salademaquinas.turtlepad.ui.theme.Textos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val DURACION_MINIMA_MS = 1500L
private const val DURACION_MAXIMA_MS = 3000L
private const val PAUSA_ENTRE_CHEQUEOS_MS = 450L
private const val PROPORCION_ILUSTRACION = 1752f / 1438f   // alto / ancho

private class Chequeo(val texto: String, val conexion: Conexion, val hayConexion: () -> Boolean)

/**
 * Presentación con chequeos reales: la barra avanza al revisar cada transporte, en orden
 * de prioridad, y al final preselecciona el primero disponible. Un toque la salta.
 */
@Composable
fun PantallaPresentacion(alTerminar: (Conexion?) -> Unit) {
    val contexto = LocalContext.current
    val saltar = remember { MutableStateFlow(false) }
    var textoChequeo by remember { mutableStateOf(Textos.BUSCANDO_USB) }
    var progreso by remember { mutableFloatStateOf(0f) }
    val progresoAnimado by animateFloatAsState(progreso, label = "progreso")

    LaunchedEffect(Unit) {
        val inicio = SystemClock.elapsedRealtime()
        val chequeos = listOf(
            Chequeo(Textos.BUSCANDO_USB, Conexion.USB) { elegirInterfazUsb(listarInterfaces()) != null },
            Chequeo(Textos.REVISANDO_WIFI, Conexion.WIFI) { elegirInterfazWifi(listarInterfaces()) != null },
            Chequeo(Textos.REVISANDO_BLUETOOTH, Conexion.BLUETOOTH) {
                contexto.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
            },
        )
        val esperarOSaltar: suspend (Long) -> Unit = { ms ->
            if (ms > 0) withTimeoutOrNull(ms) { saltar.first { it } }
        }

        var disponible: Conexion? = null
        withTimeoutOrNull(DURACION_MAXIMA_MS) {
            chequeos.forEachIndexed { i, chequeo ->
                textoChequeo = chequeo.texto
                val hay = withContext(Dispatchers.IO) { chequeo.hayConexion() }
                if (hay && disponible == null) disponible = chequeo.conexion
                progreso = (i + 1f) / chequeos.size
                esperarOSaltar(PAUSA_ENTRE_CHEQUEOS_MS)
            }
            esperarOSaltar(DURACION_MINIMA_MS - (SystemClock.elapsedRealtime() - inicio))
        }
        alTerminar(disponible)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null) { saltar.value = true },
    ) {
        // El degradado solo cambia donde está la ilustración, para que no se note el corte
        val altoIlustracion = minOf(maxWidth * PROPORCION_ILUSTRACION, maxHeight)
        val margen = (1f - altoIlustracion / maxHeight) / 2f
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        margen to Colores.presentacionArriba,
                        1f - margen to Colores.presentacionAbajo,
                    ),
                ),
        )
        Image(
            painterResource(R.drawable.splash_ilustracion),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.ieee_univalle_sb_blanco),
                    contentDescription = null,
                    modifier = Modifier.weight(1f, fill = false).height(30.dp),
                )
                Image(
                    painterResource(R.drawable.univalle_blanco),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 12.dp).fillMaxHeight(),
                )
            }

            Box(Modifier.weight(1f))

            Text(
                Textos.CARGANDO,
                color = Colores.blanco,
                fontFamily = Fuentes.titulos,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Box(
                Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Colores.negro)
                    .border(2.dp, Colores.blanco, RoundedCornerShape(10.dp))
                    .padding(4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progresoAnimado)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Colores.barraCarga),
                )
            }
            Text(
                textoChequeo,
                color = Colores.blanco,
                fontFamily = Fuentes.textos,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
