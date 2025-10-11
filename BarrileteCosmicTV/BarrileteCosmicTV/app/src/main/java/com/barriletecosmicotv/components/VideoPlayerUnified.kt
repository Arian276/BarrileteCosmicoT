package com.barriletecosmicotv.components

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTvDevice = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectMode.AUTO) }

    var currentMs by remember { mutableLongStateOf(0L) }
    var lengthMs by remember { mutableLongStateOf(0L) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var userScrubbing by remember { mutableStateOf(false) }

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

    // ===== LibVLC args =====
    val vlcArgs = remember(isTvDevice) {
        if (isTvDevice) {
            arrayListOf(
                "--aout=opensles",
                "--audio-time-stretch",
                "--network-caching=2000",
                "--live-caching=2000",
                "--drop-late-frames",
                "--skip-frames",
                "--deinterlace=1",            // obligatorio en TV
                "--deinterlace-mode=yadif2x", // calidad alta para deportes
                "--codec=mediacodec_ndk,mediacodec"
            )
        } else {
            // MOBILE: igual que lo tenías
            arrayListOf(
                "--aout=opensles",
                "--audio-time-stretch",
                "--network-caching=1000",
                "--deinterlace=1",
                "--deinterlace-mode=bob"
            )
        }
    }

    val libVlc = remember { LibVLC(context, vlcArgs) }
    val mediaPlayer = remember { MediaPlayer(libVlc) }

    // ===== Aspecto =====
    fun applyAspect(mode: AspectMode) {
        try {
            if (isTvDevice) {
                when (mode) {
                    AspectMode.FILL_16_9 -> {
                        mediaPlayer.setAspectRatio("16:9")
                        mediaPlayer.setScale(0f)
                    }
                    AspectMode.AUTO -> {
                        mediaPlayer.setAspectRatio(null)
                        mediaPlayer.setScale(0f)
                    }
                }
            } else {
                mediaPlayer.setAspectRatio(null)
                mediaPlayer.setScale(0f)
            }
        } catch (_: Throwable) { }
    }

    DisposableEffect(mediaPlayer) {
        val listener = MediaPlayer.EventListener { ev ->
            when (ev.type) {
                MediaPlayer.Event.Playing -> {
                    isPlaying = true
                    lengthMs = mediaPlayer.length
                    currentMs = mediaPlayer.time
                    sliderValue = currentMs.toFloat()
                }
                MediaPlayer.Event.Paused -> isPlaying = false
                MediaPlayer.Event.Stopped,
                MediaPlayer.Event.EndReached -> isPlaying = false
                MediaPlayer.Event.TimeChanged -> {
                    if (!userScrubbing) {
                        currentMs = mediaPlayer.time
                        sliderValue = currentMs.toFloat()
                    }
                }
                MediaPlayer.Event.LengthChanged -> {
                    lengthMs = mediaPlayer.length
                    if (!userScrubbing) sliderValue = currentMs.toFloat()
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

    // ===== Preparación / reproducción =====
    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotBlank()) {
            try {
                mediaPlayer.stop()
                val media = Media(libVlc, Uri.parse(streamUrl)).apply {
                    if (isTvDevice) {
                        // TV: max calidad + filtros suaves para 1080i
                        setHWDecoderEnabled(true, true)
                        addOption(":demux=any")               // no forzamos TS
                        addOption(":network-caching=2000")
                        addOption(":live-caching=2000")
                        addOption(":drop-late-frames")
                        addOption(":skip-frames")
                        addOption(":deinterlace=1")
                        addOption(":deinterlace-mode=yadif2x")

                        // Mejora visual suave (sin tocar API incompatibles):
                        addOption(":video-filter=hqdn3d,sharpen,adjust")
                        addOption(":hqdn3d=2:1.5:3:3")
                        addOption(":sharpen-sigma=0.08")
                        addOption(":contrast=1.05")
                        addOption(":saturation=1.08")
                        addOption(":gamma=1.03")
                        addOption(":chroma=RV32")
                    } else {
                        // MOBILE: como lo tenías
                        setHWDecoderEnabled(false, false)
                        addOption(":network-caching=1000")
                        addOption(":demux=any")
                        addOption(":deinterlace=1")
                        addOption(":deinterlace-mode=bob")
                    }
                }
                mediaPlayer.media = media
                media.release()
                delay(200)
                mediaPlayer.play()
                isPlaying = true
                applyAspect(aspectMode)
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

    // ===== UI =====
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    keepScreenOn = true
                    mediaPlayer.attachViews(this, null, false, false)
                    applyAspect(aspectMode)
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
                // Controles superiores
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
                            contentDescription = "Aspect",
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
                            contentDescription = "Fullscreen",
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

                // Controles centrales
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    val seekEnabled = canSeek(mediaPlayer)
                    IconButton(enabled = seekEnabled, onClick = { seekBy(mediaPlayer, -10_000L) }) {
                        Icon(Icons.Default.Replay10, "Replay 10s", tint = Color.White)
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
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (isFullscreen) 60.dp else 50.dp)
                        )
                    }
                    IconButton(enabled = seekEnabled, onClick = { seekBy(mediaPlayer, +10_000L) }) {
                        Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White)
                    }
                }

                // Barra de progreso
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(if (isFullscreen) 24.dp else 16.dp)
                ) {
                    if (canSeek(mediaPlayer) && lengthMs > 0L) {
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
                        LiveProgressBar(Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(6.dp))
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