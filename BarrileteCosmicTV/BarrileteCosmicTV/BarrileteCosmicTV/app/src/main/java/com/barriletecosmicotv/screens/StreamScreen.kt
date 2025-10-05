package com.barriletecosmicotv.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.barriletecosmicotv.components.VideoPlayerUnified
import com.barriletecosmicotv.components.SuperChatComponent
import com.barriletecosmicotv.components.ViewerCount
import com.barriletecosmicotv.data.LikesRepository
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.StreamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    streamId: String,
    navController: NavController? = null,
    onBackClick: (() -> Unit)? = null,
    viewModel: StreamViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION

    val stream by viewModel.currentStream.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewerCount by viewModel.viewerCount.collectAsState()
    var showDonationModal by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(isTv) } // En TV, empieza en fullscreen

    // Orientación
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Likes
    val likesRepository: LikesRepository = viewModel.likesRepository
    val scope = rememberCoroutineScope()
    val likesCount by likesRepository.getLikes(streamId).collectAsState(initial = 0)
    val hasUserLiked by likesRepository.hasUserLiked(streamId).collectAsState(initial = false)
    fun toggleLike() { scope.launch { likesRepository.toggleLike(streamId) } }

    // Cargar stream (sin abrir modal automáticamente)
    LaunchedEffect(streamId) {
        viewModel.loadStreamById(streamId)
        // (Antes aquí había un delay + showDonationModal = true)
    }

    // Para centrar el contenido en TV
    val mainModifier = if (isTv && !isFullscreen) {
        Modifier
            .width(411.dp) // Ancho típico de celu
            .fillMaxHeight()
    } else {
        Modifier.fillMaxSize()
    }

    // Contenedor principal que centra el contenido en TV
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = mainModifier) {
            if (isLoading || stream == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CosmicBackground),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = CosmicPrimary) }
            } else {
                if (isFullscreen) {
                    // --- VISTA FULLSCREEN (TV y Celular) ---
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }

                    VideoPlayerUnified(
                        streamUrl = stream?.streamUrl ?: "",
                        streamTitle = stream?.title ?: "",
                        streamId = streamId,
                        isFullscreen = true,
                        onFullscreenToggle = { isFullscreen = !isTv }, // En TV, no se puede salir de fullscreen
                        onExitFullscreen = { if (!isTv) isFullscreen = false },
                        onLikeClick = ::toggleLike,
                        likesCount = likesCount,
                        hasUserLiked = hasUserLiked,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                            .focusable()
                    )
                } else {
                    // --- VISTA NO-FULLSCREEN (solo celular) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CosmicBackground)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onBackClick?.invoke() ?: navController?.navigateUp() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Volver",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "BarrileteCosmicoTv",
                                        color = CosmicPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = getStreamTitle(streamId),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = LiveIndicator),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "EN VIVO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                ViewerCount(streamId = streamId, viewerCount = viewerCount)
                            }
                        }

                        // Player
                        VideoPlayerUnified(
                            streamUrl = stream?.streamUrl ?: "",
                            streamTitle = stream?.title ?: "",
                            streamId = streamId,
                            isFullscreen = false,
                            onFullscreenToggle = { isFullscreen = true },
                            onExitFullscreen = { },
                            onLikeClick = ::toggleLike,
                            likesCount = likesCount,
                            hasUserLiked = hasUserLiked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16 / 9f)
                        )

                        // Botón Apoyar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showDonationModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Donar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apoyar", fontSize = 14.sp)
                            }
                        }

                        // Info del stream
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📺 ${getStreamTitle(streamId)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicPrimary
                                )
                                Text(
                                    text = "Transmisión de deportes en vivo - Fútbol argentino",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Chat
                        SuperChatComponent(
                            streamId = streamId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }

            // Modal donación
            if (showDonationModal && !isFullscreen) {
                DonationModal(onDismiss = { showDonationModal = false })
            }
        }
    }
}


@Composable
private fun DonationModal(
    onDismiss: () -> Unit
) {
    fun d(px: Int) = px.dp
    fun s(px: Int) = px.sp

    // Cierra con botón atrás como en el celu
    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .focusable()
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(d(16)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(d(20)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d(16))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Text(text = "❌", fontSize = s(18))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d(8))
                ) {
                    Text(text = "❤️", fontSize = s(20))
                    Text(
                        text = "Apoyá el proyecto",
                        fontSize = s(18),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Text(
                    text = "Alias: barriletecosmicoTv",
                    fontSize = s(14),
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelLogoBox(text = "TNT", bg = Color.Black)
                    ChannelLogoBox(text = "ESPN", bg = Color.Red)
                    ChannelLogoBox(text = "DSports", bg = Color(0xFF0377BC))
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                    shape = RoundedCornerShape(d(10))
                ) { Text(text = "Cerrar", color = Color.White) }
            }
        }
    }
}

@Composable
private fun ChannelLogoBox(text: String, bg: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bg, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Auxiliares
private fun getStreamTitle(streamId: String): String {
    return when (streamId) {
        "tnt-sports-hd" -> "TNT Sports HD"
        "espn-premium-hd" -> "ESPN Premium HD"
        "directv-sport" -> "DirecTV Sport"
        "directv-plus" -> "DirecTV+"
        "espn-hd" -> "ESPN HD"
        "espn2-hd" -> "ESPN 2 HD"
        "espn3-hd" -> "ESPN 3 HD"
        "fox-sports-hd" -> "Fox Sports HD"
        else -> "Canal Deportivo"
    }
}

private fun getStreamUrl(streamId: String): String {
    return when (streamId) {
        "espn-hd" -> "https://video-weaver.sao01.hls.ttvnw.net/v1/playlist/..."
        "fox-sports" -> "https://video-weaver.sao01.hls.ttvnw.net/v1/playlist/..."
        "tntsports-hd" -> "https://video-weaver.sao01.hls.ttvnw.net/v1/playlist/..."
        "directv-sport" -> "https://video-weaver.sao01.hls.ttvnw.net/v1/playlist/..."
        "tyc-sports" -> "https://video-weaver.sao01.hls.ttvnw.net/v1/playlist/..."
        else -> "https://video-weaver.sao01.hls.ttvnw.net/v1/playlist/..."
    }
}