package com.example.player

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.model.Channel
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*

@OptIn(UnstableApi::class)
@Composable
fun TvStreamPlayer(
    channel: Channel?,
    modifier: Modifier = Modifier,
    isSoundActive: Boolean = true,
    isFullScreen: Boolean = false,
    onBack: () -> Unit = {},
    onNextChannel: () -> Unit = {},
    onPreviousChannel: () -> Unit = {},
    onToggleSound: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showOsd by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isReleased by remember { mutableStateOf(false) }

    val exoPlayer = remember(context) {
        try {
            val renderersFactory = DefaultRenderersFactory(context).apply {
                setEnableDecoderFallback(true)
            }
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Fire TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)

            val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                }
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        if (exoPlayer != null && !isReleased) {
            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    hasError = true
                    val cause = error.cause?.localizedMessage ?: error.localizedMessage ?: ""
                    errorMessage = when {
                        cause.contains("404", ignoreCase = true) || error.errorCodeName.contains("IO", ignoreCase = true) ->
                            "Stream feed unavailable (HTTP 404)"
                        cause.contains("timeout", ignoreCase = true) ->
                            "Connection timed out"
                        else ->
                            "Stream error (${error.errorCodeName})"
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        hasError = false
                    }
                }
            }
            try {
                exoPlayer.addListener(listener)
            } catch (e: Exception) {
                // Ignore
            }

            val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    try {
                        isReleased = true
                        exoPlayer.stop()
                        exoPlayer.release()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                try {
                    isReleased = true
                    exoPlayer.removeListener(listener)
                    exoPlayer.stop()
                    exoPlayer.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else {
            onDispose { }
        }
    }

    // Update volume when sound active state changes
    LaunchedEffect(isSoundActive, isReleased) {
        if (!isReleased && exoPlayer != null) {
            try {
                exoPlayer.volume = if (isSoundActive) 1.0f else 0.0f
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Update Media Item when channel or retryCount changes
    LaunchedEffect(channel?.streamUrl, retryCount, isReleased) {
        if (isReleased || exoPlayer == null) return@LaunchedEffect
        val streamUrl = channel?.streamUrl
        if (!streamUrl.isNullOrEmpty()) {
            hasError = false
            try {
                val uri = Uri.parse(streamUrl)
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                hasError = true
                errorMessage = e.localizedMessage ?: "Failed to open stream"
            }
        } else {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (isFullScreen) {
                                onNextChannel()
                                showOsd = true
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (isFullScreen) {
                                onPreviousChannel()
                                showOsd = true
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (isFullScreen) {
                                showOsd = !showOsd
                                true
                            } else {
                                onToggleSound()
                                true
                            }
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (isFullScreen) {
                                onBack()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        if (channel != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        try {
                            if (!isReleased) player = exoPlayer
                        } catch (e: Exception) {
                            // Ignore
                        }
                        useController = false
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    try {
                        if (!isReleased && view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Empty placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TvDarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TvTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No channel selected",
                        color = TvTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Error overlay with retry
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0D0D10)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TvOrangeFocus,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Playback Error",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = errorMessage,
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvFocusButton(
                            text = "Retry Stream",
                            icon = Icons.Default.Refresh,
                            onClick = { retryCount++ }
                        )
                        TvFocusButton(
                            text = "Next Channel",
                            icon = Icons.Default.SkipNext,
                            isPrimary = true,
                            onClick = {
                                hasError = false
                                onNextChannel()
                            }
                        )
                    }
                }
            }
        }

        // Fullscreen OSD HUD
        AnimatedVisibility(
            visible = isFullScreen && showOsd,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE60D0D10))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = channel?.name ?: "Unknown Channel",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "D-Pad Up/Down: Change Channel • Back: Exit Player",
                            color = TvLimeAccent,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TvFocusButton(
                            text = "Hide Info",
                            onClick = { showOsd = false }
                        )
                        TvFocusButton(
                            text = "Exit",
                            icon = Icons.Default.FullscreenExit,
                            onClick = onBack
                        )
                    }
                }
            }
        }
    }
}
