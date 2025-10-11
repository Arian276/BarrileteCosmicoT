package com.barriletecosmicotv.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===== Esquema de colores Barrilete Cósmico (oscuro por defecto) =====
private val BarrileteCosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,                 // Celeste bandera
    onPrimary = CosmicOnPrimary,
    primaryContainer = CosmicPrimaryVariant,
    onPrimaryContainer = CosmicOnPrimary,

    secondary = CosmicSecondary,             // Naranja energía
    onSecondary = CosmicOnSecondary,
    secondaryContainer = CosmicSecondaryVariant,
    onSecondaryContainer = CosmicOnSecondary,

    tertiary = ArgentinaEnergy,              // Magenta vibrante
    onTertiary = CosmicOnPrimary,

    background = CosmicBackground,
    onBackground = CosmicOnBackground,

    surface = CosmicSurface,
    onSurface = CosmicOnSurface,
    surfaceVariant = CosmicCard,
    onSurfaceVariant = CosmicMuted,

    outline = CosmicBorder,
    outlineVariant = CosmicBorder,

    // Alineamos "error" al rojo de marca para badges/estados
    error = ArgentinaPassion,
    onError = CosmicOnPrimary,

    inverseSurface = CosmicOnSurface,
    inverseOnSurface = CosmicSurface,
    inversePrimary = CosmicPrimaryVariant
)

// ===== Esquema claro (opcional, mantenido por compatibilidad) =====
private val BarrileteCosmicLightColorScheme = lightColorScheme(
    primary = CosmicPrimary,
    onPrimary = CosmicOnPrimary,
    secondary = CosmicSecondary,
    onSecondary = CosmicOnSecondary,
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F)
)

@Composable
fun BarrileteCosmicTVTheme(
    darkTheme: Boolean = true,     // Forzamos tema oscuro por defecto
    dynamicColor: Boolean = false, // Deshabilitamos dinámicos para mantener branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BarrileteCosmicColorScheme else BarrileteCosmicLightColorScheme

    // Status bar en modo oscuro consistente con el player
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CosmicBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}