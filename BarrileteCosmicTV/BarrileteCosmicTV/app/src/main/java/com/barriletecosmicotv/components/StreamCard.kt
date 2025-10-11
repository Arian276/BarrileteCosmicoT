package com.barriletecosmicotv.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.barriletecosmicotv.model.Stream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamCard(
    stream: Stream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // ⭐ soporte de favoritos (con defaults para no romper usos existentes)
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    // 🔧 Solo ajustar tamaños en TV/Box; en celular queda igual
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val scale = if (isTv) 0.75f else 1f
    fun d(px: Int) = (px.dp * scale)

    // Estilos de texto escalados SOLO en TV (sin remember: evita error de composable dentro de remember)
    val titleStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = MaterialTheme.typography.titleMedium.fontSize * scale
    )
    val bodySmallStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = MaterialTheme.typography.bodySmall.fontSize * scale
    )
    val labelSmallStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = MaterialTheme.typography.labelSmall.fontSize * scale
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = d(4))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d(12))
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(d(120), d(80))
                    .clip(RoundedCornerShape(d(8)))
            ) {
                AsyncImage(
                    model = stream.thumbnailUrl,
                    contentDescription = stream.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Play button overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(d(32))
                    )
                }

                // Featured badge
                if (stream.viewerCount > 1500) {
                    TopBadge(
                        text = "Destacado",
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(d(12)))

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = stream.title,
                    style = titleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(d(4)))

                Text(
                    text = stream.description,
                    style = bodySmallStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(d(8)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stream.category,
                        style = labelSmallStyle,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 👁️ Visitas
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Visualizaciones",
                            modifier = Modifier.size(d(16)),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(d(4)))
                        Text(
                            text = "${stream.viewerCount}",
                            style = labelSmallStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // ⭐ Favorito (si hay callback, mostramos el botón)
                        if (onToggleFavorite != null) {
                            Spacer(modifier = Modifier.width(d(8)))
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(d(24))
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                                    tint = if (isFavorite)
                                        MaterialTheme.colorScheme.secondary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}