package com.barriletecosmicotv.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriletecosmicotv.ui.theme.*

@Composable
fun TSVideoControlsImproved(
    isPlaying: Boolean,
    isFullscreen: Boolean,
    likesCount: Int,                 // se ignora (se pidió quitar “me gusta”)
    hasUserLiked: Boolean,           // se ignora
    onPlayPause: () -> Unit,
    onFullscreen: (() -> Unit)?,
    onLikeClick: (() -> Unit)?,      // se ignora (mantengo firma para no romper)
    onCastClick: () -> Unit,         // no usado aquí (se pidió limpiar arriba)
    modifier: Modifier = Modifier,
    onAspectToggle: (() -> Unit)? = null // NUEVO opcional para ajuste de pantalla
) {
    Box(modifier = modifier) {
        // Overlay degradado sutil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isFullscreen) listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ) else listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Controles superiores: más lindos y mejor acomodados (chips)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (isFullscreen) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ajuste de pantalla (aspect ratio)
            IconButton(
                onClick = { onAspectToggle?.invoke() },
                modifier = Modifier
                    .size(if (isFullscreen) 40.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Ajuste de pantalla",
                    tint = Color.White,
                    modifier = Modifier.size(if (isFullscreen) 20.dp else 18.dp)
                )
            }

            // Pantalla completa (enter/exit)
            if (onFullscreen != null) {
                IconButton(
                    onClick = onFullscreen,
                    modifier = Modifier
                        .size(if (isFullscreen) 40.dp else 36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Salir de pantalla completa" else "Pantalla completa",
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 20.dp else 18.dp)
                    )
                }
            }
        }

        // Botón play/pause central (se mantiene)
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "playButtonPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isFullscreen) 1.1f else 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(if (isFullscreen) 100.dp else 80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CosmicPrimary.copy(alpha = 0.9f),
                                CosmicSecondary.copy(alpha = 0.7f)
                            )
                        ),
                        CircleShape
                    )
                    .graphicsLayer {
                        if (!isPlaying) {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(if (isFullscreen) 40.dp else 32.dp)
                )
            }
        }

        // Controles inferiores (solo barra de progreso y breve fila derecha)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(if (isFullscreen) 24.dp else 16.dp)
        ) {
            // Barra de progreso (idéntica a la que querías)
            TSProgressBar(
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fila derecha mínima (solo por si querés dejar ajustes rápidos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // (Opcional) un botón de ajustes rápidos; lo dejo minimal
                // Podés quitarlo si no lo usás.
                IconButton(
                    onClick = { /* opcional */ },
                    modifier = Modifier
                        .size(if (isFullscreen) 40.dp else 36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 20.dp else 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TSProgressBar(
    modifier: Modifier = Modifier
) {
    // Barra de progreso animada para streams en vivo
    val infiniteTransition = rememberInfiniteTransition(label = "liveProgress")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liveProgressAnimation"
    )

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.3f))
    ) {
        // Relleno degradado (branding)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f + progress * 0.3f)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CosmicPrimary,
                            CosmicSecondary,
                            ArgentinaPassion
                        )
                    )
                )
        )

        // Indicador de vivo
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(LiveIndicator, CircleShape)
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp)
        )
    }
}