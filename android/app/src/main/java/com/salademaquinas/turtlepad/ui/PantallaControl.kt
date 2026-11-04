package com.salademaquinas.turtlepad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salademaquinas.turtlepad.Ajustes
import com.salademaquinas.turtlepad.Conexion
import com.salademaquinas.turtlepad.EstadoEnvio
import com.salademaquinas.turtlepad.LETRA_STOP
import com.salademaquinas.turtlepad.PadViewModel
import com.salademaquinas.turtlepad.parsearIpv4
import com.salademaquinas.turtlepad.ui.theme.Colores
import com.salademaquinas.turtlepad.ui.theme.Fuentes
import com.salademaquinas.turtlepad.ui.theme.Textos

private val TAMANO_MINIMO_BOTON = 96.dp
private val TAMANO_MAXIMO_BOTON = 128.dp
private val DESPLAZAMIENTO_SOMBRA = 4.dp

private enum class Flecha { ARRIBA, ABAJO, IZQUIERDA, DERECHA }

private data class BotonPad(val letra: Char, val color: Color, val flecha: Flecha)

private val BOTONES = listOf(
    BotonPad('F', Colores.rojo, Flecha.ARRIBA),
    BotonPad('B', Colores.cian, Flecha.ABAJO),
    BotonPad('L', Colores.amarillo, Flecha.IZQUIERDA),
    BotonPad('R', Colores.azulProfundo, Flecha.DERECHA),
)

@Composable
fun PantallaControl(modelo: PadViewModel, alPresionar: () -> Unit, alPedirPermisoRed: () -> Unit) {
    val letra by modelo.mando.letra.collectAsStateWithLifecycle()
    val ajustes by modelo.ajustes.collectAsStateWithLifecycle()
    val estado by modelo.estado.collectAsStateWithLifecycle()
    val permisoRed by modelo.permisoRed.collectAsStateWithLifecycle()
    var mostrarAcercaDe by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().fondoSemitono()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Encabezado(alAbrirAcercaDe = { mostrarAcercaDe = true })

            ajustes?.let { a ->
                PanelAjustes(
                    ajustes = a,
                    modelo = modelo,
                    faltaPermisoRed = !permisoRed && a.conexion != Conexion.BLUETOOTH,
                    alPedirPermisoRed = alPedirPermisoRed,
                )
            }

            Cruceta(
                letra = letra,
                alPresionar = { l ->
                    alPresionar()
                    modelo.mando.presionar(l)
                },
                alSoltar = modelo.mando::soltar,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            BarraEstado(ajustes?.conexion, estado)
        }
    }

    if (mostrarAcercaDe) AcercaDe(alCerrar = { mostrarAcercaDe = false })
}

/** Azul cielo con puntos de semitono, dibujados en vez de usar una imagen pesada. */
private fun Modifier.fondoSemitono() = drawWithCache {
    val separacion = 14.dp.toPx()
    val ancho = size.width
    val alto = size.height
    val puntos = buildList {
        var y = 0f
        var fila = 0
        while (y < alto) {
            var x = if (fila % 2 == 0) 0f else separacion / 2
            while (x < ancho) {
                add(Offset(x, y))
                x += separacion
            }
            y += separacion * 0.87f
            fila++
        }
    }
    val radio = 3.dp.toPx()
    onDrawBehind {
        drawRect(Colores.azulCielo)
        drawPoints(puntos, PointMode.Points, Colores.puntosFondo, strokeWidth = radio * 2, cap = StrokeCap.Round)
    }
}

// ---------- Encabezado ----------
@Composable
private fun Encabezado(alAbrirAcercaDe: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            TituloComic(Textos.EVENTO, tamano = 34.sp)
            Text(
                Textos.LUGAR_Y_FECHA,
                color = Colores.blanco,
                fontFamily = Fuentes.textos,
                fontSize = 20.sp,
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .background(Colores.blanco, CircleShape)
                .border(2.dp, Colores.negro, CircleShape)
                .clickable(onClick = alAbrirAcercaDe),
            contentAlignment = Alignment.Center,
        ) {
            Text(Textos.BOTON_ACERCA_DE, color = Colores.negro, fontFamily = Fuentes.textos, fontSize = 20.sp)
        }
    }
}

/** Título: mayúsculas en rojo con contorno negro y una franja amarilla detrás. */
@Composable
fun TituloComic(texto: String, tamano: TextUnit) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .padding(vertical = 6.dp)
                .rotate(-1.5f)
                .background(Colores.amarillo),
        )
        val estilo = TextStyle(fontFamily = Fuentes.titulos, fontSize = tamano)
        val anchoContorno = with(LocalDensity.current) { 3.dp.toPx() }
        Text(
            texto,
            color = Colores.negro,
            style = estilo.copy(drawStyle = Stroke(anchoContorno, join = StrokeJoin.Round)),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(texto, color = Colores.rojo, style = estilo, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

// ---------- Cruceta ----------
@Composable
private fun Cruceta(
    letra: Char,
    alPresionar: (Char) -> Unit,
    alSoltar: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val celda = (minOf(maxWidth, maxHeight) / 3).coerceIn(TAMANO_MINIMO_BOTON, TAMANO_MAXIMO_BOTON)
        BOTONES.forEach { boton ->
            val (x, y) = when (boton.flecha) {
                Flecha.ARRIBA -> Pair(0.dp, -celda)
                Flecha.ABAJO -> Pair(0.dp, celda)
                Flecha.IZQUIERDA -> Pair(-celda, 0.dp)
                Flecha.DERECHA -> Pair(celda, 0.dp)
            }
            BotonCruceta(
                boton = boton,
                presionado = letra == boton.letra,
                tamano = celda,
                alPresionar = alPresionar,
                alSoltar = alSoltar,
                modifier = Modifier.offset(x, y),
            )
        }
        Indicador(letra, celda * 0.86f)
    }
}

@Composable
private fun BotonCruceta(
    boton: BotonPad,
    presionado: Boolean,
    tamano: Dp,
    alPresionar: (Char) -> Unit,
    alSoltar: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(16.dp)
    val cuerpo = tamano - DESPLAZAMIENTO_SOMBRA - 2.dp
    // La zona táctil no se mueve; al hundirse solo se desplaza lo que se dibuja
    Box(
        modifier
            .size(tamano)
            .pointerInput(boton.letra) {
                awaitEachGesture {
                    val dedo = awaitFirstDown(requireUnconsumed = false)
                    alPresionar(boton.letra)
                    try {
                        // Igual que en el CYD: deslizar el dedo fuera del botón también lo suelta
                        do {
                            val cambio = awaitPointerEvent().changes.firstOrNull { it.id == dedo.id }
                            val sigue = cambio != null && cambio.pressed &&
                                cambio.position.x in 0f..size.width.toFloat() &&
                                cambio.position.y in 0f..size.height.toFloat()
                        } while (sigue)
                    } finally {
                        alSoltar(boton.letra)
                    }
                }
            },
    ) {
        if (!presionado) {
            Box(
                Modifier
                    .offset(DESPLAZAMIENTO_SOMBRA, DESPLAZAMIENTO_SOMBRA)
                    .size(cuerpo)
                    .background(Colores.negro, forma),
            )
        }
        val hundido = if (presionado) DESPLAZAMIENTO_SOMBRA else 0.dp
        Box(
            Modifier
                .offset(hundido, hundido)
                .size(cuerpo)
                .background(boton.color, forma)
                .border(3.dp, Colores.negro, forma),
            contentAlignment = Alignment.Center,
        ) {
            FlechaGruesa(boton.flecha, cuerpo * 0.42f)
        }
    }
}

@Composable
private fun FlechaGruesa(flecha: Flecha, tamano: Dp) {
    Canvas(Modifier.size(tamano)) {
        val w = size.width
        val h = size.height
        val (a, b, c) = when (flecha) {
            Flecha.ARRIBA -> Triple(Offset(w / 2, 0f), Offset(0f, h), Offset(w, h))
            Flecha.ABAJO -> Triple(Offset(w / 2, h), Offset(0f, 0f), Offset(w, 0f))
            Flecha.IZQUIERDA -> Triple(Offset(0f, h / 2), Offset(w, 0f), Offset(w, h))
            Flecha.DERECHA -> Triple(Offset(w, h / 2), Offset(0f, 0f), Offset(0f, h))
        }
        val triangulo = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            close()
        }
        // Borde blanco: sobre el azul profundo el negro solo no se distingue bien
        drawPath(triangulo, Colores.blanco, style = Stroke(6.dp.toPx(), join = StrokeJoin.Round))
        drawPath(triangulo, Colores.negro)
        drawPath(triangulo, Colores.negro, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
    }
}

/** Indicador central: la letra que se está enviando; en rojo cuando es S. */
@Composable
private fun Indicador(letra: Char, tamano: Dp) {
    Box(
        Modifier
            .size(tamano)
            .background(Colores.blanco, CircleShape)
            .border(3.dp, Colores.negro, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letra.toString(),
            color = if (letra == LETRA_STOP) Colores.rojo else Colores.negro,
            fontFamily = Fuentes.titulos,
            fontSize = 46.sp,
        )
    }
}

// ---------- Ajustes ----------
@Composable
private fun Panel(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(Colores.panel, forma)
            .border(2.dp, Colores.negro, forma)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        contenido()
    }
}

@Composable
private fun PanelAjustes(
    ajustes: Ajustes,
    modelo: PadViewModel,
    faltaPermisoRed: Boolean,
    alPedirPermisoRed: () -> Unit,
) {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Conexion.entries.forEach { conexion ->
                BotonOpcion(
                    conexion.texto,
                    ajustes.conexion == conexion,
                    Modifier.weight(1f),
                ) { modelo.guardarConexion(conexion) }
            }
        }

        if (ajustes.conexion != Conexion.BLUETOOTH) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BotonOpcion(Textos.DESTINO_AUTOMATICO, !ajustes.manual, Modifier.weight(1f)) {
                    modelo.guardarManual(false)
                }
                BotonOpcion(Textos.DESTINO_MANUAL, ajustes.manual, Modifier.weight(1f)) {
                    modelo.guardarManual(true)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (ajustes.manual) {
                    CampoIp(ajustes.ip, modelo::guardarIp, Modifier.weight(2f))
                }
                CampoPuerto(ajustes.puerto, modelo::guardarPuerto, Modifier.weight(1f))
            }
        }

        if (faltaPermisoRed) {
            BotonOpcion(Textos.PEDIR_PERMISO, false, Modifier.fillMaxWidth(), alPedirPermisoRed)
        }
    }
}

@Composable
fun BotonOpcion(texto: String, elegido: Boolean, modifier: Modifier = Modifier, alTocar: () -> Unit) {
    OutlinedButton(
        onClick = alTocar,
        modifier = modifier,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        border = BorderStroke(2.dp, Colores.negro),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (elegido) Colores.amarillo else Colores.blanco,
            contentColor = Colores.negro,
        ),
    ) {
        Text(texto, fontFamily = Fuentes.textos, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CampoIp(ip: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    var texto by rememberSaveable { mutableStateOf(ip) }
    OutlinedTextField(
        value = texto,
        onValueChange = {
            texto = it
            alCambiar(it)
        },
        label = { Text(Textos.CAMPO_IP, fontFamily = Fuentes.textos) },
        isError = texto.isNotEmpty() && parsearIpv4(texto) == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = coloresCampo(),
        modifier = modifier,
    )
}

@Composable
private fun CampoPuerto(puerto: Int, alCambiar: (Int) -> Unit, modifier: Modifier = Modifier) {
    var texto by rememberSaveable { mutableStateOf(puerto.toString()) }
    // DataStore carga el valor guardado después de crear el campo
    LaunchedEffect(puerto) {
        if (texto.toIntOrNull() != puerto) texto = puerto.toString()
    }
    OutlinedTextField(
        value = texto,
        onValueChange = {
            texto = it
            val valor = it.toIntOrNull()
            if (valor != null && valor in 1..65535) alCambiar(valor)
        },
        label = { Text(Textos.CAMPO_PUERTO, fontFamily = Fuentes.textos) },
        isError = texto.toIntOrNull()?.let { it !in 1..65535 } ?: true,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = coloresCampo(),
        modifier = modifier,
    )
}

@Composable
private fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Colores.negro,
    unfocusedTextColor = Colores.negro,
    focusedBorderColor = Colores.negro,
    unfocusedBorderColor = Colores.negro,
    focusedLabelColor = Colores.negro,
    unfocusedLabelColor = Colores.textoApagado,
    cursorColor = Colores.negro,
)

// ---------- Barra de estado ----------
@Composable
private fun BarraEstado(conexion: Conexion?, estado: EstadoEnvio) {
    Panel {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                conexion?.texto.orEmpty(),
                color = Colores.negro,
                fontFamily = Fuentes.textos,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                estado.destino ?: Textos.SIN_DESTINO,
                color = Colores.negro,
                fontFamily = Fuentes.textos,
                fontSize = 16.sp,
            )
            Text(
                "${estado.paquetesPorSegundo} ${Textos.PAQUETES_POR_SEGUNDO}",
                color = if (estado.paquetesPorSegundo > 0) Colores.okSobrePanel else Colores.errorSobrePanel,
                fontFamily = Fuentes.textos,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        estado.error?.let {
            Text(it, color = Colores.errorSobrePanel, fontFamily = Fuentes.textos, fontSize = 15.sp)
        }
    }
}
