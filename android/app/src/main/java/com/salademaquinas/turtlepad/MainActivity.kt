package com.salademaquinas.turtlepad

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.salademaquinas.turtlepad.ui.PantallaPrincipal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val DURACION_VIBRACION_MS = 25L
private const val API_ANDROID_12 = 31
private const val API_ANDROID_17 = 37

class MainActivity : ComponentActivity() {
    private val modelo: PadViewModel by viewModels()

    private val pedirPermisosDelSistema = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { actualizarPermisos() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        actualizarPermisos()
        // Pide solo lo que necesita la conexión elegida, y una vez por cada cambio
        lifecycleScope.launch {
            modelo.presentacionTerminada.first { it }
            modelo.ajustes.filterNotNull().map { it.conexion }.distinctUntilChanged()
                .collect { pedirPermisos(it) }
        }

        // STARTED: al pasar a segundo plano se cancela, envía S y deja de enviar
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                modelo.presentacionTerminada.first { it }
                actualizarPermisos()
                modelo.enviarMientrasVisible()
            }
        }

        setContent {
            PantallaPrincipal(
                modelo = modelo,
                alPresionar = ::vibrar,
                alPedirPermisos = { modelo.ajustes.value?.conexion?.let(::pedirPermisos) },
            )
        }
    }

    private fun concedido(permiso: String) =
        ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED

    private fun tienePermisoRed() =
        Build.VERSION.SDK_INT < API_ANDROID_17 || concedido(Manifest.permission.ACCESS_LOCAL_NETWORK)

    private fun tienePermisoBluetooth() =
        Build.VERSION.SDK_INT < API_ANDROID_12 || concedido(Manifest.permission.BLUETOOTH_CONNECT)

    private fun actualizarPermisos() {
        modelo.permisoRed.value = tienePermisoRed()
        modelo.permisoBluetooth.value = tienePermisoBluetooth()
    }

    private fun pedirPermisos(conexion: Conexion) {
        if (conexion == Conexion.BLUETOOTH) {
            if (!tienePermisoBluetooth()) {
                pedirPermisosDelSistema.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            }
        } else if (!tienePermisoRed()) {
            pedirPermisosDelSistema.launch(arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK))
        }
    }

    private fun vibrar() {
        getSystemService(Vibrator::class.java)?.vibrate(
            VibrationEffect.createOneShot(DURACION_VIBRACION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
