plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.barriletecosmicotv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.barriletecosmicotv"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // 🔓 IMPORTANTE: sin filtros de ABI para que instale en ARM y x86 (móvil, tablet, Android TV/Fire TV)
        // ndk { abiFilters += listOf("arm64-v8a") }  // ← eliminado
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // Mantenemos Java 8 como tenías
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // No tocamos jniLibs: LibVLC trae .so para múltiples ABIs
    }

    // ✅ OPCIONAL (si querés bajar tamaño generando APKs por ABI al compilar local)
    // splits {
    //     abi {
    //         isEnable = true
    //         reset()
    //         include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    //         isUniversalApk = true // además genera un APK universal
    //     }
    // }

    // ✅ OPCIONAL (si subís AAB a Play, que haga split por ABI en la entrega)
    // bundle {
    //     abi {
    //         enableSplit = true
    //     }
    // }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // ======== VIDEO PLAYER ========>
    implementation("org.videolan.android:libvlc-all:3.6.0")

    // ======== Media3 (servicios / notificaciones / controller) ========
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    // HLS opcional con Media3 (no necesario si usás VLC):
    // implementation(libs.androidx.media3.exoplayer.hls)

    // ======== CAST ========
    implementation("com.google.android.gms:play-services-cast-framework:21.3.0")

    // Networking / JSON
    implementation("javax.jmdns:jmdns:3.5.8")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Animations / System UI
    implementation("androidx.compose.animation:animation:1.5.4")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")

    // Custom Tabs
    implementation("androidx.browser:browser:1.7.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}