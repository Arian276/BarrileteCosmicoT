package com.barriletecosmicotv.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriletecosmicotv.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ArgentinaHeaderActions(
    onDownloadLatest: () -> Unit,
    onChangePassword: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var daysRemaining by remember { mutableStateOf<Int?>(null) }

    val appContext = LocalContext.current.applicationContext as Context
    val sessionManager = remember { SessionManager(appContext) }

    // Refrescar días cuando se abre el menú
    LaunchedEffect(expanded) {
        if (expanded) {
            val remaining = withContext(Dispatchers.IO) {
                sessionManager.getDaysRemaining()
            }
            daysRemaining = if (remaining >= 0) remaining else null
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(30.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(230.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // ✅ Mostrar días restantes
                daysRemaining?.let { d ->
                    val color = when {
                        d <= 3 -> Color.Red
                        d <= 7 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Días restantes: $d/30",
                                color = color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = { expanded = false }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Descargar última versión", fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onDownloadLatest()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Cambiar contraseña", fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onChangePassword()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            "Cerrar sesión",
                            fontSize = 15.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    },
                    onClick = { expanded = false }
                )
            }
        }
    }
}