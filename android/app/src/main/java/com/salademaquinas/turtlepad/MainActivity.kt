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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DURACION_VIBRACION_MS = 25L
private const val API_ANDROID_17 = 37

class MainActivity : ComponentActivity() {
    private val modelo: PadViewModel by viewModels()

    private val pedirPermisoRed = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { concedido ->
        modelo.permisoRed.value = concedido
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        modelo.permisoRed.value = tienePermisoRed()
        lifecycleScope.launch {
            modelo.presentacionTerminada.first { it }
            if (!tienePermisoRed()) pedirPermisoDeRed()
        }

        // STARTED: al pasar a segundo plano se cancela, envía S y deja de enviar
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                modelo.presentacionTerminada.first { it }
                modelo.permisoRed.value = tienePermisoRed()
                modelo.enviarMientrasVisible()
            }
        }

        setContent {
            PantallaPrincipal(
                modelo = modelo,
                alPresionar = ::vibrar,
                alPedirPermisoRed = ::pedirPermisoDeRed,
            )
        }
    }

    private fun tienePermisoRed(): Boolean =
        Build.VERSION.SDK_INT < API_ANDROID_17 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED

    private fun pedirPermisoDeRed() {
        if (Build.VERSION.SDK_INT >= API_ANDROID_17) {
            pedirPermisoRed.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    private fun vibrar() {
        getSystemService(Vibrator::class.java)?.vibrate(
            VibrationEffect.createOneShot(DURACION_VIBRACION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
