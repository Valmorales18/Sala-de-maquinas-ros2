package com.salademaquinas.turtlepad.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.salademaquinas.turtlepad.R

/** Colores del evento (estilo cómic / pop art). */
object Colores {
    val azulCielo = Color(0xFF0475C8)
    val azulProfundo = Color(0xFF0D52AD)
    val rojo = Color(0xFFE31016)
    val amarillo = Color(0xFFEBEB2B)
    val cian = Color(0xFF5DE0E6)
    val blanco = Color(0xFFFFFFFF)
    val negro = Color(0xFF111111)

    val puntosFondo = Color(0x1AFFFFFF)         // semitono muy sutil
    val panel = Color(0xE6FFFFFF)               // blanco semitransparente, texto negro legible
    val errorSobrePanel = Color(0xFFA80008)     // rojo más oscuro: contraste sobre el panel
    val okSobrePanel = Color(0xFF0A6B2E)
    val textoApagado = Color(0xFF444444)

    // Presentación: muestreados de los bordes de splash_ilustracion.webp
    val presentacionArriba = Color(0xFF487CA9)
    val presentacionAbajo = Color(0xFF285171)
    val barraCarga = Color(0xFFC4E02B)
}

object Fuentes {
    val titulos = FontFamily(Font(R.font.permanent_marker))
    val textos = FontFamily(Font(R.font.patrick_hand))
}

object Textos {
    // ---------- Encabezado ----------
    const val EVENTO = "ROS MEETUP"
    const val LUGAR_Y_FECHA = "Cartagena · 07/11/2026"

    // ---------- Cruceta ----------
    const val SIN_DESTINO = "sin destino"
    const val PAQUETES_POR_SEGUNDO = "paq/s"

    // ---------- Presentación ----------
    const val CARGANDO = "CARGANDO…"
    const val BUSCANDO_USB = "Buscando anclaje USB…"
    const val REVISANDO_WIFI = "Revisando Wi-Fi…"
    const val REVISANDO_BLUETOOTH = "Revisando Bluetooth…"

    // ---------- Ajustes ----------
    const val CONEXION_USB = "USB (anclaje)"
    const val CONEXION_WIFI = "Wi-Fi"
    const val CONEXION_BLUETOOTH = "Bluetooth"
    const val DESTINO_AUTOMATICO = "Automático"
    const val DESTINO_MANUAL = "IP manual"
    const val CAMPO_IP = "IP del PC"
    const val CAMPO_PUERTO = "Puerto"
    const val PEDIR_PERMISO = "Conceder permiso"

    // ---------- Acerca de ----------
    const val BOTON_ACERCA_DE = "i"
    const val ACERCA_DE = "ACERCA DE"
    const val HECHO_POR = "Hecho por la Rama Estudiantil IEEE de la Universidad del Valle"
    const val AUTORES = "Valentina Morales y Gerardo González"
    const val URL_REPO = "https://github.com/Valmorales18/Sala-de-maquinas-ros2"
    const val VERSION = "Versión"
    const val CREDITO_TUX = "Tux: Larry Ewing, creado con The GIMP"
    const val CERRAR = "Cerrar"

    // ---------- Bluetooth ----------
    const val BT_ESPERANDO = "esperando conexión"
    const val BT_CONECTADO = "conectado"
    const val BT_CANAL = "canal"

    // ---------- Errores ----------
    const val ERROR_PERMISO_BLUETOOTH =
        "Falta el permiso de Bluetooth (si no aparece el aviso: Ajustes › Apps › Turtle Pad › Permisos)"
    const val ERROR_SIN_BLUETOOTH = "Este celular no tiene Bluetooth"
    const val ERROR_BLUETOOTH_APAGADO = "El Bluetooth está apagado: actívalo en el celular"
    const val ERROR_SERVIDOR_BLUETOOTH = "No se pudo abrir el servidor Bluetooth; reintentando"
    const val ERROR_PERMISO_RED =
        "Falta el permiso de red local (si no aparece el aviso: Ajustes › Apps › Turtle Pad › Permisos)"
    const val ERROR_RED_BLOQUEADA = "Android bloqueó el envío: revisa el permiso de red local"
    const val ERROR_RED_INALCANZABLE = "Red inalcanzable: revisa la conexión o la IP"
    const val ERROR_SIN_USB = "No hay interfaz USB: activa el anclaje USB en el celular"
    const val ERROR_SIN_WIFI = "No hay interfaz Wi-Fi con IP: conéctate a la red"
    const val ERROR_SIN_BROADCAST = "La red no tiene broadcast: usa IP manual"
    const val ERROR_IP_INVALIDA = "IP inválida: escribe algo como 192.168.1.10"
    const val ERROR_ENVIO = "Error al enviar"
}
