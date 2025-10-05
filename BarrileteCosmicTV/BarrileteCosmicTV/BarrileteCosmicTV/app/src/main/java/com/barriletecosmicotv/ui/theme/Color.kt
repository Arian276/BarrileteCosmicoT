package com.barriletecosmicotv.ui.theme

import androidx.compose.ui.graphics.Color

// Colores originales (pueden eliminarse si ya no se usan)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 🎨 Paleta Barrilete Cósmico
val ArgentinaCeleste = Color(0xFF36B5D8)   // Celeste bandera
val ArgentinaOrange = Color(0xFFF77A2B)    // Naranja energía
val ArgentinaEnergy = Color(0xFFE740B7)    // Rosa vibrante
val ArgentinaGold   = Color(0xFFD4AF37)    // Dorado
val ArgentinaPassion = Color(0xFFE83D3D)   // Rojo pasión

// Principales
val CosmicPrimary = ArgentinaCeleste
val CosmicPrimaryVariant = Color(0xFF2A91AA)
val CosmicSecondary = ArgentinaOrange
val CosmicSecondaryVariant = Color(0xFFD4621A)

// Fondos oscuros
val CosmicBackground = Color(0xFF0C1218)
val CosmicSurface = Color(0xFF1A242E)
val CosmicCard = Color(0xFF1E2A38)
val CosmicBorder = Color(0xFF2A3441)

// Texto
val CosmicOnPrimary = Color.White
val CosmicOnSecondary = Color.White
val CosmicOnBackground = Color(0xFFF5F5F5)
val CosmicOnSurface = Color(0xFFE8E8E8)
val CosmicMuted = Color(0xFF9CA3AF)

// Estado
val LiveIndicator = ArgentinaPassion  // 🔴 EN VIVO coherente con la barra
val LiveGlow = ArgentinaPassion.copy(alpha = 0.7f)

// Gradientes unificados
val GradientStart = CosmicPrimary
val GradientMid = CosmicSecondary
val GradientEnd = CosmicPrimary

val GradientHoverStart = CosmicSecondary
val GradientHoverMid = ArgentinaPassion
val GradientHoverEnd = CosmicSecondary