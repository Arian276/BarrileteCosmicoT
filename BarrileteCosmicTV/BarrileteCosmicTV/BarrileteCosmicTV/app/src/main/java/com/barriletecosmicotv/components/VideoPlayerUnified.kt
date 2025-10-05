package com.barriletecosmicotv.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private enum class AspectMode { AUTO, FILL_16_9 }

@Composable
fun VideoPlayerUnified(
    streamUrl: String,
    streamTitle: String = "",
    streamId: String = "",
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {},
    onExitFullscreen: () -> Unit = {},
    onLikeClick: (() -> Unit)? = null,
    likesCount: Int = 0,
    hasUserLiked: Boolean = false,
    isInPictureInPictureMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectMode.AUTO) }

    // === Estados de progreso para barra real ===
    var currentMs by remember { mutableLongStateOf(0L) }
    var lengthMs by remember { mutableLongStateOf(0L) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var userScrubbing by remember { mutableStateOf(false) }

    // Helpers de seek
    fun canSeek(mp: MediaPlayer): Boolean = mp.isSeekable && mp.length > 0
    fun clampSeekTarget(target: Long, maxLen: Long): Long {
        val max = (maxLen - 1).coerceAtLeast(0)
        return target.coerceIn(0L, max)
    }
    fun seekBy(mp: MediaPlayer, deltaMs: Long) {
        if (!canSeek(mp)) return
        val target = clampSeekTarget(mp.time + deltaMs, mp.length)
        mp.time = target
        currentMs = target
        sliderValue = target.toFloat()
    }

    val vlcArgs = remember {
        arrayListOf(
            "--aout=opensles",
            "--audio-time-stretch",
            "--network-caching=1000",
            "--deinterlace=1",
            "--deinterlace-mode=bob"
        )
    }
    val libVlc = remember { LibVLC(context, vlcArgs) }
    val mediaPlayer = remember { MediaPlayer(libVlc) }

    fun applyAspect(@Suppress("UNUSED_PARAMETER") mode: AspectMode) {
        // Placeholder para control manual de aspect ratio futuro
    }

    DisposableEffect(mediaPlayer) {
        val listener = MediaPlayer.EventListener { ev ->
            when (ev.type) {
                MediaPlayer.Event.Playing -> {
                    isPlaying = true
                    // Sincronizamos al entrar en reproducción
                    lengthMs = mediaPlayer.length
                    currentMs = mediaPlayer.time
                    sliderValue = currentMs.toFloat()
                }
                MediaPlayer.Event.Paused -> isPlaying = false
                MediaPlayer.Event.Stopped,
                MediaPlayer.Event.EndReached -> isPlaying = false

                // Actualizaciones de tiempo/longitud para la barra de progreso
                MediaPlayer.Event.TimeChanged -> {
                    if (!userScrubbing) {
                        currentMs = mediaPlayer.time
                        sliderValue = currentMs.toFloat()
                    }
                }
                MediaPlayer.Event.LengthChanged -> {
                    lengthMs = mediaPlayer.length
                    if (!userScrubbing) {
                        sliderValue = currentMs.toFloat()
                    }
                }
            }
        }
        mediaPlayer.setEventListener(listener)
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.detachViews()
            } catch (_: Throwable) {}
            mediaPlayer.release()
            libVlc.release()
        }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotBlank()) {
            try {
                mediaPlayer.stop()
                val media = Media(libVlc, Uri.parse(streamUrl)).apply {
                    setHWDecoderEnabled(false, false)
                    addOption(":network-caching=1000")
                    addOption(":demux=any")
                    addOption(":deinterlace=1")
                    addOption(":deinterlace-mode=bob")
                }
                mediaPlayer.media = media
                media.release()
                delay(200)
                mediaPlayer.play()
                isPlaying = true
                applyAspect(aspectMode)

                // Reset de progreso al cargar fuente
                currentMs = 0L
                lengthMs = mediaPlayer.length
                sliderValue = 0f
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(aspectMode) { applyAspect(aspectMode) }

    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable() // Para DPAD/TV
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    keepScreenOn = true
                    mediaPlayer.attachViews(this, null, false, false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showControls && !isInPictureInPictureMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // ======= Controles superiores (aspect, fullscreen, cast) =======
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isFullscreen) 16.dp else 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            aspectMode = when (aspectMode) {
                                AspectMode.AUTO -> AspectMode.FILL_16_9
                                AspectMode.FILL_16_9 -> AspectMode.AUTO
                            }
                        },
                        modifier = Modifier
                            .size(if (isFullscreen) 40.dp else 36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Screen Aspect",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isFullscreen) onExitFullscreen() else onFullscreenToggle()
                        },
                        modifier = Modifier
                            .size(if (isFullscreen) 40.dp else 36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }

                    AndroidView(
                        factory = { ctx ->
                            MediaRouteButton(ctx).apply {
                                CastButtonFactory.setUpMediaRouteButton(ctx, this)
                            }
                        },
                        modifier = Modifier
                            .size(if (isFullscreen) 40.dp else 36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(6.dp)
                    )
                }

                // ======= Controles centrales (±10s y play/pause) =======
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    val seekEnabled = canSeek(mediaPlayer)

                    IconButton(
                        enabled = seekEnabled,
                        onClick = { seekBy(mediaPlayer, -10_000L) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Replay 10 seconds",
                            tint = if (seekEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(if (isFullscreen) 40.dp else 36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isFullscreen) 100.dp else 80.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable {
                                if (isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                isPlaying = !isPlaying
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(if (isFullscreen) 60.dp else 50.dp)
                        )
                    }

                    IconButton(
                        enabled = seekEnabled,
                        onClick = { seekBy(mediaPlayer, +10_000L) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = if (seekEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(if (isFullscreen) 40.dp else 36.dp)
                        )
                    }
                }

                // ======= Barra de progreso (Slider si seekable, LiveProgress si no) =======
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(if (isFullscreen) 24.dp else 16.dp)
                ) {
                    if (canSeek(mediaPlayer) && lengthMs > 0L) {
                        // Slider real con scrubbing
                        Slider(
                            value = sliderValue.coerceIn(0f, lengthMs.toFloat()),
                            onValueChange = {
                                userScrubbing = true
                                sliderValue = it
                            },
                            onValueChangeFinished = {
                                val target = clampSeekTarget(sliderValue.toLong(), lengthMs)
                                mediaPlayer.time = target
                                currentMs = target
                                userScrubbing = false
                            },
                            valueRange = 0f..lengthMs.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Modo LIVE (no seekable): animación que ya tenías
                        LiveProgressBar(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveProgressBar(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "liveProgress")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liveProgressAnim"
    )

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f + t * 0.3f)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            com.barriletecosmicotv.ui.theme.CosmicPrimary,
                            com.barriletecosmicotv.ui.theme.CosmicSecondary,
                            com.barriletecosmicotv.ui.theme.ArgentinaPassion
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(com.barriletecosmicotv.ui.theme.LiveIndicator, CircleShape)
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp)
        )
    }
}