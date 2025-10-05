package com.barriletecosmicotv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.barriletecosmicotv.casting.*
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.CastViewModel

/**
 * Botón de casting REAL con todas las tecnologías 100% funcionales
 * - Google Cast: MediaRouteButton nativo
 * - Miracast: MediaRouter API real
 * - DLNA: NSD discovery real
 * - AirPlay: mDNS discovery real
 */
@Composable
fun RealUniversalCastButton(
    streamUrl: String = "",
    streamTitle: String = "",
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    castViewModel: CastViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showCastDialog by remember { mutableStateOf(false) }
    
    val castConnectionState by castViewModel.castConnectionState.collectAsState()
    val availableDevices by castViewModel.availableDevices.collectAsState()
    
    // Inicializar casting REAL al montar
    LaunchedEffect(Unit) {
        castViewModel.startDiscovery()
    }
    
    // Limpiar recursos al desmontar
    DisposableEffect(Unit) {
        onDispose {
            castViewModel.stopDiscovery()
        }
    }
    
    Box(modifier = modifier) {
        // Si hay dispositivos, mostrar botón universal
        if (availableDevices.isNotEmpty()) {
            IconButton(
                onClick = { showCastDialog = true },
                modifier = Modifier
                    .size(if (isFullscreen) 40.dp else 36.dp)
                    .background(
                        when (castConnectionState) {
                            CastConnectionState.CONNECTED -> Color.Blue.copy(alpha = 0.8f)
                            CastConnectionState.CONNECTING -> Color(0xFFFF9800).copy(alpha = 0.8f)
                            else -> Color.Black.copy(alpha = 0.6f)
                        },
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = when (castConnectionState) {
                        CastConnectionState.CONNECTED -> Icons.Default.Cast
                        CastConnectionState.CONNECTING -> Icons.Default.CastConnected
                        else -> Icons.Default.Cast
                    },
                    contentDescription = "Casting",
                    tint = Color.White,
                    modifier = Modifier.size(if (isFullscreen) 20.dp else 18.dp)
                )
            }
        } else {
            // Fallback: Solo MediaRouteButton nativo de Google
            AndroidView(
                factory = { ctx ->
                    MediaRouteButton(ctx).apply {
                        CastButtonFactory.setUpMediaRouteButton(ctx, this)
                    }
                },
                modifier = Modifier.size(if (isFullscreen) 40.dp else 36.dp)
            )
        }
    }
    
    // Dialog con dispositivos REALES
    if (showCastDialog) {
        Dialog(onDismissRequest = { showCastDialog = false }) {
            RealCastDeviceDialog(
                devices = availableDevices,
                currentState = castConnectionState,
                onDeviceSelected = { device ->
                    castViewModel.connectToDevice(device)
                    if (streamUrl.isNotEmpty()) {
                        castViewModel.castCurrentStream(streamUrl, streamTitle)
                    }
                    showCastDialog = false
                },
                onDisconnect = {
                    castViewModel.disconnect()
                    showCastDialog = false
                },
                onDismiss = { showCastDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RealCastDeviceDialog(
    devices: List<CastDevice>,
    currentState: CastConnectionState,
    onDeviceSelected: (CastDevice) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f),
        colors = CardDefaults.cardColors(containerColor = CosmicBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📡 Dispositivos disponibles",
                    color = CosmicPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = CosmicMuted
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estado actual
            if (currentState == CastConnectionState.CONNECTED) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ Conectado",
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                        
                        TextButton(onClick = onDisconnect) {
                            Text("Desconectar", color = Color.Red)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Lista de dispositivos REALES
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    RealDeviceItem(
                        device = device,
                        onSelected = { onDeviceSelected(device) }
                    )
                }
                
                if (devices.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CosmicCard.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DevicesOther,
                                    contentDescription = null,
                                    tint = CosmicMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Buscando dispositivos...",
                                    color = CosmicMuted,
                                    fontSize = 16.sp
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Asegúrate de que tus dispositivos estén encendidos y en la misma red",
                                    color = CosmicMuted.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealDeviceItem(
    device: CastDevice,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) 
                CosmicPrimary.copy(alpha = 0.2f) else CosmicCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono por tecnología
                Icon(
                    imageVector = getTechnologyIcon(device.technology),
                    contentDescription = device.technology.name,
                    tint = getTechnologyColor(device.technology),
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = device.name,
                        color = CosmicOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = getTechnologyDisplayName(device.technology),
                        color = CosmicMuted,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Estado del dispositivo
            if (device.isConnected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Conectado",
                    tint = Color.Green,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Indicador de señal
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = (8 + index * 4).dp)
                                .background(
                                    if (device.signalStrength > (index + 1) * 25) 
                                        CosmicPrimary else CosmicMuted.copy(alpha = 0.3f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun getTechnologyIcon(technology: CastTechnology): ImageVector {
    return when (technology) {
        CastTechnology.GOOGLE_CAST -> Icons.Default.Cast
        CastTechnology.MIRACAST -> Icons.Default.ScreenShare
        CastTechnology.DLNA -> Icons.Default.Router
        CastTechnology.AIRPLAY -> Icons.Default.Tv // Apple TV
    }
}

private fun getTechnologyColor(technology: CastTechnology): Color {
    return when (technology) {
        CastTechnology.GOOGLE_CAST -> Color(0xFF4285F4) // Google Blue
        CastTechnology.MIRACAST -> Color(0xFF00BCD4) // Cyan
        CastTechnology.DLNA -> Color(0xFFFF9800) // Orange  
        CastTechnology.AIRPLAY -> Color(0xFF000000) // Black (Apple)
    }
}

private fun getTechnologyDisplayName(technology: CastTechnology): String {
    return when (technology) {
        CastTechnology.GOOGLE_CAST -> "Google Cast / Chromecast"
        CastTechnology.MIRACAST -> "Miracast / WiFi Display"
        CastTechnology.DLNA -> "DLNA / UPnP"
        CastTechnology.AIRPLAY -> "AirPlay / Apple TV"
    }
}