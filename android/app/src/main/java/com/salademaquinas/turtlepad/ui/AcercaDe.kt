package com.salademaquinas.turtlepad.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salademaquinas.turtlepad.BuildConfig
import com.salademaquinas.turtlepad.R
import com.salademaquinas.turtlepad.ui.theme.Colores
import com.salademaquinas.turtlepad.ui.theme.Fuentes
import com.salademaquinas.turtlepad.ui.theme.Textos

/** Sobre azul profundo porque los dos logos son blancos con fondo transparente. */
@Composable
fun AcercaDe(alCerrar: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    val enlaces = LocalUriHandler.current
    Dialog(onDismissRequest = alCerrar) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Colores.azulProfundo, forma)
                .border(3.dp, Colores.negro, forma)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TituloComic(Textos.ACERCA_DE, tamano = 24.sp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.ieee_univalle_sb_blanco),
                    contentDescription = null,
                    modifier = Modifier.weight(1f, fill = false).height(32.dp),
                )
                Image(
                    painterResource(R.drawable.univalle_blanco),
                    contentDescription = null,
                    modifier = Modifier.height(64.dp),
                )
            }
            TextoAcercaDe(Textos.HECHO_POR)
            TextoAcercaDe(Textos.AUTORES)
            Text(
                Textos.URL_REPO,
                color = Colores.amarillo,
                fontFamily = Fuentes.textos,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { enlaces.openUri(Textos.URL_REPO) },
            )
            TextoAcercaDe("${Textos.VERSION} ${BuildConfig.VERSION_NAME}")
            TextoAcercaDe(Textos.CREDITO_TUX)
            BotonOpcion(Textos.CERRAR, elegido = false, alTocar = alCerrar)
        }
    }
}

@Composable
private fun TextoAcercaDe(texto: String) {
    Text(
        texto,
        color = Colores.blanco,
        fontFamily = Fuentes.textos,
        fontSize = 17.sp,
        textAlign = TextAlign.Center,
    )
}
