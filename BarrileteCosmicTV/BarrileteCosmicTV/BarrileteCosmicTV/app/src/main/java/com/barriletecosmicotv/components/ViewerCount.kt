package com.barriletecosmicotv.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.barriletecosmicotv.data.ViewerTracker
import com.barriletecosmicotv.ui.theme.LiveGlow
import com.barriletecosmicotv.ui.theme.LiveIndicator
import kotlinx.coroutines.delay

@Composable
fun ViewerCount(
    streamId: String,
    viewerCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Animación cuando cambia el conteo
    var showAnimation by remember { mutableStateOf(false) }
    var previousCount by remember { mutableStateOf(viewerCount) }
    
    // Animación del conteo
    val scale by animateFloatAsState(
        targetValue = if (showAnimation) 1.2f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        finishedListener = {
            if (showAnimation) {
                showAnimation = false
            }
        },
        label = "viewerScale"
    )
    
    // Efecto de pulso para el icono de live
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Detectar cambios en el conteo para animar
    LaunchedEffect(viewerCount) {
        if (viewerCount != previousCount) {
            showAnimation = true
            previousCount = viewerCount
        }
    }
    
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.7f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Indicador LIVE pulsante
            Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = "Espectadores",
                tint = LiveIndicator,
                modifier = Modifier
                    .size(12.dp)
                    .scale(pulseScale)
            )
            
            // Conteo de espectadores con animación
            Text(
                text = formatViewerCount(viewerCount),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.scale(scale)
            )
            
            // Punto pulsante que indica transmisión en vivo
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(pulseScale)
                    .background(
                        LiveGlow,
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

// Función eliminada - ahora usa conteo real del backend

// Formatea el conteo para mostrar K cuando es necesario
private fun formatViewerCount(count: Int): String {
    return when {
        count >= 1000 -> "${(count / 100) / 10.0}K"
        else -> count.toString()
    }
}