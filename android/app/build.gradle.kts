import com.android.build.api.variant.impl.VariantOutputImpl
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// La firma se lee de android/keystore.properties o de variables de entorno; nada de esto se versiona
val propiedadesFirma = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun valorFirma(clave: String, variable: String): String? =
    propiedadesFirma.getProperty(clave) ?: System.getenv(variable)

val rutaLlave = valorFirma("storeFile", "TURTLEPAD_STORE_FILE")

android {
    namespace = "com.salademaquinas.turtlepad"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.salademaquinas.turtlepad"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (rutaLlave != null) {
            create("release") {
                storeFile = rootProject.file(rutaLlave)
                storePassword = valorFirma("storePassword", "TURTLEPAD_STORE_PASSWORD")
                keyAlias = valorFirma("keyAlias", "TURTLEPAD_KEY_ALIAS")
                keyPassword = valorFirma("keyPassword", "TURTLEPAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variante ->
        variante.outputs.forEach { salida ->
            (salida as VariantOutputImpl).outputFileName.set(
                salida.versionName.map { "turtle-pad-v$it.apk" })
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    testImplementation("junit:junit:4.13.2")
}
