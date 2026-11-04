package com.salademaquinas.turtlepad

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salademaquinas.turtlepad.ui.theme.Textos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val PUERTO_POR_DEFECTO = 5005

enum class Conexion(val texto: String) {
    USB(Textos.CONEXION_USB),
    WIFI(Textos.CONEXION_WIFI),
    BLUETOOTH(Textos.CONEXION_BLUETOOTH),
}

data class Ajustes(
    val conexion: Conexion = Conexion.USB,
    val manual: Boolean = false,
    val ip: String = "",
    val puerto: Int = PUERTO_POR_DEFECTO,
)

private val Context.dataStore by preferencesDataStore(name = "ajustes")

class RepositorioAjustes(private val contexto: Context) {
    private val claveConexion = stringPreferencesKey("conexion")
    private val claveManual = booleanPreferencesKey("manual")
    private val claveIp = stringPreferencesKey("ip")
    private val clavePuerto = intPreferencesKey("puerto")

    val ajustes: Flow<Ajustes> = contexto.dataStore.data.map { prefs ->
        Ajustes(
            conexion = Conexion.entries.firstOrNull { it.name == prefs[claveConexion] } ?: Conexion.USB,
            manual = prefs[claveManual] ?: false,
            ip = prefs[claveIp] ?: "",
            puerto = prefs[clavePuerto] ?: PUERTO_POR_DEFECTO,
        )
    }

    suspend fun guardarConexion(valor: Conexion) = contexto.dataStore.edit { it[claveConexion] = valor.name }
    suspend fun guardarManual(valor: Boolean) = contexto.dataStore.edit { it[claveManual] = valor }
    suspend fun guardarIp(valor: String) = contexto.dataStore.edit { it[claveIp] = valor }
    suspend fun guardarPuerto(valor: Int) = contexto.dataStore.edit { it[clavePuerto] = valor }
}
