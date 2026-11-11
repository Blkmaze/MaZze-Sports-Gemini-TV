package com.example.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.model.Channel
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun extractHttpCode(throwable: Throwable?): Int? {
    var current: Throwable? = throwable
    while (current != null) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            return current.responseCode
        }
        val msg = current.message ?: ""
        val match = Regex("""response code:\s*(\d+)""", RegexOption.IGNORE_CASE).find(msg)
            ?: Regex("""HTTP\s*(\d{3})""", RegexOption.IGNORE_CASE).find(msg)
        if (match != null) {
            match.groupValues[1].toIntOrNull()?.let { return it }
        }
        current = current.cause
    }
    return null
}

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
    var errorDetail by remember { mutableStateOf("") }
    var showOsd by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }
    var autoRetryCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var retryJob by remember { mutableStateOf<Job?>(null) }
    val retryFocusRequester = remember { FocusRequester() }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isReleased by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

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

            // Live sports buffering tuned for quick starts and resilience against short network hiccups
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // minBufferMs (~15s)
                    50000, // maxBufferMs (~50s)
                    2500,  // bufferForPlaybackMs (~2.5s)
                    5000   // bufferForPlaybackAfterRebufferMs (~5s)
                )
                .build()

            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                }
        } catch (e: Exception) {
            null
        }
    }

    // Auto-focus the Retry button whenever the error overlay appears
    LaunchedEffect(hasError) {
        if (hasError) {
            delay(100)
            try {
                retryFocusRequester.requestFocus()
            } catch (e: Exception) {
                delay(150)
                try {
                    retryFocusRequester.requestFocus()
                } catch (e2: Exception) {
                    // Ignore if node not yet active
                }
            }
        }
    }

    // Smooth motion frame-rate matching helper for Android 11+ (API 30+)
    val applySmoothMotion: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val fps = exoPlayer?.videoFormat?.frameRate ?: -1f
                if (fps > 0f) {
                    val surface = (playerViewRef?.videoSurfaceView as? SurfaceView)?.holder?.surface
                    if (surface != null && surface.isValid) {
                        surface.setFrameRate(
                            fps,
                            Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
                            Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        if (exoPlayer != null && !isReleased) {
            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Auto-reconnect up to 3 times with backoff (~1s, 3s, 6s) before showing error overlay
                    if (autoRetryCount < 3) {
                        autoRetryCount++
                        val delayMs = when (autoRetryCount) {
                            1 -> 1000L
                            2 -> 3000L
                            else -> 6000L
                        }
                        retryJob?.cancel()
                        retryJob = coroutineScope.launch {
                            delay(delayMs)
                            if (!isReleased && exoPlayer != null) {
                                try {
                                    val streamUrl = channel?.streamUrl
                                    if (!streamUrl.isNullOrEmpty()) {
                                        val uri = Uri.parse(streamUrl)
                                        val mediaItem = MediaItem.Builder()
                                            .setUri(uri)
                                            .build()
                                        exoPlayer.setMediaItem(mediaItem)
                                    }
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        }
                    } else {
                        hasError = true
                        val httpCode = extractHttpCode(error)
                        val cause = error.cause?.localizedMessage ?: error.localizedMessage ?: ""
                        errorMessage = when {
                            httpCode == 404 || cause.contains("404", ignoreCase = true) || error.errorCodeName.contains("IO", ignoreCase = true) ->
                                "Stream feed unavailable (HTTP 404)"
                            cause.contains("timeout", ignoreCase = true) ->
                                "Connection timed out"
                            else ->
                                "Stream error (${error.errorCodeName})"
                        }

                        val codeName = error.errorCodeName
                        val causeName = error.cause?.javaClass?.simpleName
                        val causeInfo = if (!causeName.isNullOrEmpty() && causeName != "PlaybackException") " • $causeName" else ""
                        errorDetail = if (httpCode != null) {
                            "$codeName • HTTP $httpCode"
                        } else {
                            "$codeName$causeInfo"
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        hasError = false
                        errorMessage = ""
                        errorDetail = ""
                        autoRetryCount = 0
                        retryJob?.cancel()
                        applySmoothMotion()
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    applySmoothMotion()
                }

                override fun onRenderedFirstFrame() {
                    applySmoothMotion()
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
                        retryJob?.cancel()
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
                retryJob?.cancel()
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
        autoRetryCount = 0
        retryJob?.cancel()
        val streamUrl = channel?.streamUrl
        if (!streamUrl.isNullOrEmpty()) {
            hasError = false
            errorMessage = ""
            errorDetail = ""
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
                errorDetail = e.javaClass.simpleName
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
                            if (!hasError && isFullScreen) {
                                onNextChannel()
                                showOsd = true
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!hasError && isFullScreen) {
                                onPreviousChannel()
                                showOsd = true
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (hasError) {
                                false // Allow focused button (Retry Stream / Next Channel / Back) to receive Enter
                            } else if (isFullScreen) {
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
                        playerViewRef = this
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            (videoSurfaceView as? SurfaceView)?.holder?.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    applySmoothMotion()
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                    applySmoothMotion()
                                }
                                override fun surfaceDestroyed(holder: SurfaceHolder) {}
                            })
                        }
                    }
                },
                update = { view ->
                    playerViewRef = view
                    try {
                        if (!isReleased && view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                },
                onRelease = { view ->
                    try {
                        view.player = null
                    } catch (e: Exception) {
                        // Ignore
                    }
                    if (playerViewRef == view) {
                        playerViewRef = null
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
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                    if (errorDetail.isNotBlank()) {
                        Text(
                            text = errorDetail,
                            color = TvTextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvFocusButton(
                            text = "Retry Stream",
                            icon = Icons.Default.Refresh,
                            modifier = Modifier.focusRequester(retryFocusRequester),
                            onClick = {
                                hasError = false
                                errorMessage = ""
                                errorDetail = ""
                                autoRetryCount = 0
                                retryJob?.cancel()
                                retryCount++
                            }
                        )
                        TvFocusButton(
                            text = "Next Channel",
                            icon = Icons.Default.SkipNext,
                            isPrimary = true,
                            onClick = {
                                hasError = false
                                errorMessage = ""
                                errorDetail = ""
                                autoRetryCount = 0
                                retryJob?.cancel()
                                onNextChannel()
                            }
                        )
                        if (isFullScreen) {
                            TvFocusButton(
                                text = "Back",
                                icon = Icons.Default.FullscreenExit,
                                onClick = {
                                    hasError = false
                                    errorMessage = ""
                                    errorDetail = ""
                                    autoRetryCount = 0
                                    retryJob?.cancel()
                                    onBack()
                                }
                            )
                        }
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
