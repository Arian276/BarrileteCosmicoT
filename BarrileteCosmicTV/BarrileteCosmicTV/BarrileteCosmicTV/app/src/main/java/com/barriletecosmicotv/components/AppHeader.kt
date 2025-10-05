package com.barriletecosmicotv.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    // SOLO ajusta en TV/Box; en celular queda igual
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val scale = if (isTv) 0.8f else 1f
    fun d(px: Int) = (px.dp * scale)

    val titleStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * scale).sp
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d(16), vertical = d(12)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.primary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSearchClick?.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        modifier = Modifier.size(d(24))
                    )
                }
                Spacer(modifier = Modifier.width(d(4)))
                IconButton(onClick = { onSettingsClick?.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        modifier = Modifier.size(d(24))
                    )
                }
            }
        }
    }
}