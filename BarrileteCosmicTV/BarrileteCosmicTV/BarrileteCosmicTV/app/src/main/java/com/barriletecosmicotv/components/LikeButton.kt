package com.barriletecosmicotv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriletecosmicotv.ui.theme.*

@Composable
fun LikeButton(
    likesCount: Int,
    hasUserLiked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimating by remember { mutableStateOf(false) }
    
    // Animación de escala cuando se hace click
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 1f,
        animationSpec = tween(150),
        finishedListener = { isAnimating = false },
        label = "like_scale"
    )
    
    // Color animado del corazón
    val heartColor by animateColorAsState(
        targetValue = if (hasUserLiked) LiveIndicator else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "heart_color"
    )
    
    Row(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {
                isAnimating = true
                onLikeClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (hasUserLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (hasUserLiked) "Quitar like" else "Dar like",
            tint = heartColor,
            modifier = Modifier.scale(scale)
        )
        
        Text(
            text = when {
                likesCount == 0 -> "Me gusta"
                likesCount < 1000 -> likesCount.toString()
                likesCount < 1000000 -> "${likesCount / 1000}K"
                else -> "${likesCount / 1000000}M"
            },
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}