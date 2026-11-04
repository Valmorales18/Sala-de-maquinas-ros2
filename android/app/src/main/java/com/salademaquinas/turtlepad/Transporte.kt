package com.salademaquinas.turtlepad

/** Por dónde salen las letras. Lo llama siempre el mismo bucle de 20 Hz. */
interface Transporte {
    /** Descripción para la barra de estado: IP:puerto o dispositivo Bluetooth. */
    val destino: String?

    /** Último error en español, o null si todo va bien. */
    val error: String?

    /** Envía una línea con la letra. Devuelve true si salió. No debe lanzar por errores de E/S. */
    fun enviar(letra: Char): Boolean

    fun cerrar()
}
