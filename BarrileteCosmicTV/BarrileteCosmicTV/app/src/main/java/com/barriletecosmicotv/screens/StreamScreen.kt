package com.barriletecosmicotv.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowInsets
import android.view.WindowInsetsController
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.barriletecosmicotv.components.VideoPlayerUnified
import com.barriletecosmicotv.components.SuperChatComponent
import com.barriletecosmicotv.components.ViewerCount
import com.barriletecosmicotv.data.LikesRepository
import com.barriletecosmicotv.data.SessionManager
import com.barriletecosmicotv.ui.theme.*
import com.barriletecosmicotv.viewmodel.StreamViewModel
import com.barriletecosmicotv.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    streamId: String,
    navController: NavController? = null,
    onBackClick: (() -> Unit)? = null,
    viewModel: StreamViewModel = hiltViewModel(),
    sessionManager: SessionManager? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val cfg = LocalConfiguration.current
    val isTv = (cfg.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION

    // ✅ SessionManager efectivo
    val effectiveSessionManager = remember {
        sessionManager ?: SessionManager(context.applicationContext)
    }

    val stream by viewModel.currentStream.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewerCount by viewModel.viewerCount.collectAsState()

    var showDonationModal by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(isTv) }

    // ✅ Controlar barras del sistema al entrar/salir del fullscreen
    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Restaurar orientación al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window
            val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Likes
    val likesRepository: LikesRepository = viewModel.likesRepository
    val scope = rememberCoroutineScope()
    val likesCount by likesRepository.getLikes(streamId).collectAsState(initial = 0)
    val hasUserLiked by likesRepository.hasUserLiked(streamId).collectAsState(initial = false)
    fun toggleLike() { scope.launch { likesRepository.toggleLike(streamId) } }

    // Cargar stream
    LaunchedEffect(streamId) {
        viewModel.loadStreamById(streamId)
    }

    // BackHandler
    BackHandler(enabled = true) {
        when {
            showDonationModal -> showDonationModal = false
            else -> onBackClick?.invoke() ?: navController?.popBackStack()
        }
    }

    val mainModifier = if (isTv && !isFullscreen) {
        Modifier.width(411.dp).fillMaxHeight()
    } else {
        Modifier.fillMaxSize()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = mainModifier) {
            if (isLoading || stream == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(CosmicBackground),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CosmicPrimary)
                }
            } else {
                if (isFullscreen) {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }

                    VideoPlayerUnified(
                        streamUrl = stream?.streamUrl ?: "",
                        streamTitle = stream?.title ?: "",
                        streamId = streamId,
                        isFullscreen = true,
                        onFullscreenToggle = { isFullscreen = !isTv },
                        onExitFullscreen = { if (!isTv) isFullscreen = false },
                        onLikeClick = ::toggleLike,
                        likesCount = likesCount,
                        hasUserLiked = hasUserLiked,
                        modifier = Modifier.fillMaxSize()
                            .focusRequester(focusRequester)
                            .focusable()
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().background(CosmicBackground)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        onBackClick?.invoke() ?: navController?.popBackStack()
                                    }
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

                        // Banner de vencimiento
                        ExpirationBannerAutoHide(sessionManager = effectiveSessionManager)

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
                            modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f)
                        )

                        // Botón Apoyar
                        Row(
                            modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                            modifier = Modifier.fillMaxWidth()
                                .weight(1f)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }

            if (showDonationModal && !isFullscreen) {
                DonationModal(onDismiss = { showDonationModal = false })
            }
        }
    }
}

/* ========================== Componentes auxiliares ========================== */

@Composable
private fun DonationModal(
    onDismiss: () -> Unit
) {
    fun d(px: Int) = px.dp
    fun s(px: Int) = px.sp

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

/**
 * Banner de advertencia que aparece si faltan 0..3 días y se oculta solo a los 10s.
 * Se refresca al entrar y cuando cambia expiresAt en DataStore.
 */
@Composable
private fun ExpirationBannerAutoHide(
    sessionManager: SessionManager,
    autoHideMillis: Long = 10_000L
) {
    val vm: UserViewModel = hiltViewModel()

    var daysRemaining by remember { mutableStateOf<Int?>(null) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var dismissedOnce by rememberSaveable { mutableStateOf(false) }

    // Observar cambios de expiresAt
    val expiresAtFlow = remember { sessionManager.getExpiresAt() }
    val expiresAt by expiresAtFlow.collectAsState(initial = null)

    suspend fun recalcAndDecide() {
        val d = withContext(Dispatchers.IO) { sessionManager.getDaysRemaining() }
        daysRemaining = d
        if (!dismissedOnce) {
            visible = d in 0..3   // ✅ incluye 0
        }
    }

    // Al entrar: refrescar backend y calcular
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { vm.refreshSubscription() }
        recalcAndDecide()
    }

    // Si cambia expiresAt, recalcular
    LaunchedEffect(expiresAt) { recalcAndDecide() }

    // Auto-hide a los 10s
    LaunchedEffect(visible) {
        if (visible) {
            delay(autoHideMillis)
            visible = false
            dismissedOnce = true
        }
    }

    val d = daysRemaining ?: 999
    if (visible && d in 0..3) {
        Surface(
            color = MaterialTheme.colorScheme.error,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⚠️ Tu suscripción vence pronto • Días restantes: $d/30",
                    fontWeight = FontWeight.Bold
                )
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