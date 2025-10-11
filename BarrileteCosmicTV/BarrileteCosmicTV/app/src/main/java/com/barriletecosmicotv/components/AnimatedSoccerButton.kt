package com.barriletecosmicotv.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriletecosmicotv.ui.theme.GradientEnd
import com.barriletecosmicotv.ui.theme.GradientHoverEnd
import com.barriletecosmicotv.ui.theme.GradientHoverMid
import com.barriletecosmicotv.ui.theme.GradientHoverStart
import com.barriletecosmicotv.ui.theme.GradientMid
import com.barriletecosmicotv.ui.theme.GradientStart

@Composable
fun AnimatedSoccerButton(
    streamId: String,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animaciones exactas de la web
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    // Gradientes exactos de la web
    val gradient = if (isHovered) {
        Brush.linearGradient(
            colors = listOf(
                GradientHoverStart, // #FF6B6B
                GradientHoverMid,   // #FF8E53
                GradientHoverEnd    // #FF6B6B
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GradientStart, // #4ECDC4
                GradientMid,   // #44A08D
                GradientEnd    // #4ECDC4
            )
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(60.dp)
                .scale(scale)
                .clip(RoundedCornerShape(30.dp))
                .background(gradient)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onWatchClick()
                }
                .padding(2.dp), // Para el borde blanco
            contentAlignment = Alignment.Center
        ) {
            // Efecto de brillo animado (shimmer)
            if (isHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                start = androidx.compose.ui.geometry.Offset(
                                    shimmerOffset * 300f - 100f,
                                    0f
                                ),
                                end = androidx.compose.ui.geometry.Offset(
                                    shimmerOffset * 300f + 100f,
                                    200f
                                )
                            )
                        )
                )
            }
            
            // Contenido del botón
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "VER AHORA",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Flecha animada
                val arrowOffset by animateFloatAsState(
                    targetValue = if (isHovered) 5f else 0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "arrowOffset"
                )
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .offset(x = arrowOffset.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Partículas decorativas (efecto radial)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isHovered) 0.1f else 0.05f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(0.2f, 0.8f),
                            radius = 300f
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isHovered) 0.1f else 0.05f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(0.8f, 0.2f),
                            radius = 300f
                        )
                    )
            )
        }
        
        // Efecto de brillo exterior en hover (exacto de la web)
        if (isHovered) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3B82F6).copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            radius = 400f
                        ),
                        RoundedCornerShape(40.dp)
                    )
            )
        }
    }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.HoverInteraction.Enter -> {
                    isHovered = true
                }
                is androidx.compose.foundation.interaction.HoverInteraction.Exit -> {
                    isHovered = false
                }
                is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                    isHovered = true
                }
                is androidx.compose.foundation.interaction.PressInteraction.Release -> {
                    isHovered = false
                }
            }
        }
    }
}