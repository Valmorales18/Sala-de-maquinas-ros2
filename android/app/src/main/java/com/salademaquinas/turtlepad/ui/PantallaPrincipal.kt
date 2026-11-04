package com.salademaquinas.turtlepad.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salademaquinas.turtlepad.PadViewModel
import com.salademaquinas.turtlepad.ui.theme.Colores
import com.salademaquinas.turtlepad.ui.theme.Fuentes

private val estiloTexto = TextStyle(fontFamily = Fuentes.textos)

@Composable
fun PantallaPrincipal(modelo: PadViewModel, alPresionar: () -> Unit, alPedirPermisos: () -> Unit) {
    val presentacionTerminada by modelo.presentacionTerminada.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Colores.azulProfundo, error = Colores.errorSobrePanel),
        typography = Typography(
            bodyLarge = estiloTexto,
            bodySmall = estiloTexto,
            labelLarge = estiloTexto,
        ),
    ) {
        if (presentacionTerminada) {
            PantallaControl(modelo, alPresionar, alPedirPermisos)
        } else {
            PantallaPresentacion(modelo::terminarPresentacion)
        }
    }
}
