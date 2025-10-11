# =========================
#  Base / Debug helpers
# =========================
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

# =========================
#  GOOGLE CAST SDK
# =========================
# Keep CastOptionsProvider (interface) y tu implementación
-keep public class * implements com.google.android.gms.cast.framework.OptionsProvider
-keep class com.barriletecosmicotv.cast.CastOptionsProvider { *; }

# MediaRoute (UI botones de Cast)
-keep public class androidx.mediarouter.app.MediaRouteActionProvider { public <methods>; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keep class androidx.mediarouter.** { *; }

# Google Cast framework
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.auto.value.**
# Keep receivers/intents del módulo Cast (si tenés)
-keep class com.barriletecosmicotv.cast.** { *; }

# =========================
#  VLC / LibVLC (VideoLAN)
# =========================
# Mantener clases de LibVLC y no avisar por nativas
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.** { *; }
-dontwarn org.videolan.**
# Mantener anotaciones JNI si aparecen
-keepclasseswithmembernames class * {
    native <methods>;
}

# =========================
#  AndroidX Media3
# =========================
# Media3 usa mucha reflexión interna y proguard puede romper playback/cast UI.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# =========================
#  Hilt / Dagger
# =========================
# En general AGP añade reglas, pero reforzamos:
-dontwarn dagger.hilt.internal.**
-dontwarn dagger.**
-dontwarn javax.inject.**
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class **_HiltModules_** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep class * implements dagger.hilt.android.internal.managers.FragmentGetContextFix { *; }

# ViewModels con Hilt (evita que obfusque nombres usados por Hilt)
-keep class ** extends androidx.lifecycle.ViewModel { *; }

# =========================
#  Retrofit / OkHttp / Gson / Serialization
# =========================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class retrofit2.** { *; }

# Mantener anotaciones de Retrofit en interfaces
-keepattributes *Annotation*
-keep interface com.squareup.okhttp3.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlinx Serialization (si la usás)
-dontwarn kotlinx.serialization.**
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepattributes *Annotation*, InnerClasses

# =========================
#  Kotlin / Coroutines / Compose
# =========================
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }

# Compose normalmente no requiere reglas extra, pero por seguridad:
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

# =========================
#  Android Support (por legado)
# =========================
-keep class android.support.** { *; }

# =========================
#  Tu código / paquetes
# =========================
# Mantener Activities (intents por nombre), Services y Receivers
-keep class com.barriletecosmicotv.** extends android.app.Activity { *; }
-keep class com.barriletecosmicotv.** extends android.app.Service { *; }
-keep class com.barriletecosmicotv.** extends android.content.BroadcastReceiver { *; }
-keep class com.barriletecosmicotv.** extends android.content.ContentProvider { *; }