package com.example.ui.screens

import android.net.Uri
import android.widget.FrameLayout
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import android.util.Log
import com.example.model.Channel
import com.example.ui.components.TvClock
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MultiviewScreen(
    channels: List<Channel>,
    multiviewChannels: List<Channel?>,
    multiviewGameTitles: List<String?> = listOf(null, null, null, null),
    activeSoundIndex: Int,
    onSetSoundIndex: (Int) -> Unit,
    onSlotChannelChange: (Int, Channel) -> Unit,
    onAutoFillTop20: () -> Unit = {},
    onExpandTile: (Channel) -> Unit,
    onCycleTileBackup: (Int) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    var activeMenuTile by remember { mutableStateOf<Int?>(null) }
    var channelPickerSlot by remember { mutableStateOf<Int?>(null) }

    // (1) Never run more than 2 hardware video decoders at once:
    // only activeSoundIndex and secondaryPlayingTile get full playback.
    var secondaryPlayingTile by remember {
        mutableIntStateOf(if (activeSoundIndex == 0) 1 else 0)
    }

    LaunchedEffect(activeSoundIndex) {
        if (secondaryPlayingTile == activeSoundIndex) {
            secondaryPlayingTile = (activeSoundIndex + 1) % 4
        }
    }

    // Focus requesters for the 4 grid tiles
    val tileFocusRequesters = remember { List(4) { FocusRequester() } }

    LaunchedEffect(Unit) {
        val initialFocusIndex = if (activeSoundIndex in 0..3) activeSoundIndex else 0
        kotlinx.coroutines.delay(300)
        try { tileFocusRequesters.getOrNull(initialFocusIndex)?.requestFocus() } catch (e: Exception) { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Compact Top Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Multiview 2x2",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Press OK on any tile for Action Menu  •  Sound: Tile ${activeSoundIndex + 1}",
                    color = TvLimeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvFocusButton(
                    text = "Auto-fill Top 20",
                    icon = Icons.Default.Bolt,
                    onClick = onAutoFillTop20
                )
                TvClock()
            }
        }

        // 2x2 Grid - Tiles fill the whole screen area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Row: Tile 0 (Left), Tile 1 (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MultiviewTile(
                    tileIndex = 0,
                    channel = multiviewChannels.getOrNull(0),
                    gameTitle = multiviewGameTitles.getOrNull(0),
                    isSoundActive = activeSoundIndex == 0,
                    isPlaybackEnabled = (0 == activeSoundIndex || 0 == secondaryPlayingTile),
                    focusRequester = tileFocusRequesters[0],
                    onStartPlayback = { secondaryPlayingTile = 0 },
                    onOpenMenu = { activeMenuTile = 0 },
                    onTryBackup = { onCycleTileBackup(0) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusProperties {
                            right = tileFocusRequesters[1]
                            down = tileFocusRequesters[2]
                        }
                )

                MultiviewTile(
                    tileIndex = 1,
                    channel = multiviewChannels.getOrNull(1),
                    gameTitle = multiviewGameTitles.getOrNull(1),
                    isSoundActive = activeSoundIndex == 1,
                    isPlaybackEnabled = (1 == activeSoundIndex || 1 == secondaryPlayingTile),
                    focusRequester = tileFocusRequesters[1],
                    onStartPlayback = { secondaryPlayingTile = 1 },
                    onOpenMenu = { activeMenuTile = 1 },
                    onTryBackup = { onCycleTileBackup(1) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusProperties {
                            left = tileFocusRequesters[0]
                            down = tileFocusRequesters[3]
                        }
                )
            }

            // Bottom Row: Tile 2 (Left), Tile 3 (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MultiviewTile(
                    tileIndex = 2,
                    channel = multiviewChannels.getOrNull(2),
                    gameTitle = multiviewGameTitles.getOrNull(2),
                    isSoundActive = activeSoundIndex == 2,
                    isPlaybackEnabled = (2 == activeSoundIndex || 2 == secondaryPlayingTile),
                    focusRequester = tileFocusRequesters[2],
                    onStartPlayback = { secondaryPlayingTile = 2 },
                    onOpenMenu = { activeMenuTile = 2 },
                    onTryBackup = { onCycleTileBackup(2) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusProperties {
                            up = tileFocusRequesters[0]
                            right = tileFocusRequesters[3]
                        }
                )

                MultiviewTile(
                    tileIndex = 3,
                    channel = multiviewChannels.getOrNull(3),
                    gameTitle = multiviewGameTitles.getOrNull(3),
                    isSoundActive = activeSoundIndex == 3,
                    isPlaybackEnabled = (3 == activeSoundIndex || 3 == secondaryPlayingTile),
                    focusRequester = tileFocusRequesters[3],
                    onStartPlayback = { secondaryPlayingTile = 3 },
                    onOpenMenu = { activeMenuTile = 3 },
                    onTryBackup = { onCycleTileBackup(3) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusProperties {
                            up = tileFocusRequesters[1]
                            left = tileFocusRequesters[2]
                        }
                )
            }
        }
    }

    // Action Menu Dialog when OK is pressed on a tile
    if (activeMenuTile != null) {
        val slot = activeMenuTile!!
        val currentChannel = multiviewChannels.getOrNull(slot)
        val gameTitle = multiviewGameTitles.getOrNull(slot)

        AlertDialog(
            onDismissRequest = { activeMenuTile = null },
            containerColor = TvDarkSurface,
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tile ${slot + 1} Actions",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (activeSoundIndex == slot) {
                            Text(
                                text = "AUDIO ACTIVE",
                                color = TvLimeAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E3A1E))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (!gameTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = gameTitle,
                            color = TvLimeAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (currentChannel != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Channel: ${currentChannel.name}",
                            color = TvTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Sound On Here
                    MultiviewMenuButton(
                        text = if (activeSoundIndex == slot) "Sound On Here (Active)" else "Sound On Here",
                        icon = Icons.Default.VolumeUp,
                        isHighlighted = activeSoundIndex == slot,
                        autoFocus = true,
                        onClick = {
                            val prevSound = activeSoundIndex
                            onSetSoundIndex(slot)
                            if (secondaryPlayingTile == slot) {
                                secondaryPlayingTile = prevSound
                            }
                            activeMenuTile = null
                        }
                    )

                    // Button 1b: Play Video in This Tile (if not currently decoding video)
                    if (slot != activeSoundIndex && slot != secondaryPlayingTile && currentChannel != null) {
                        MultiviewMenuButton(
                            text = "Play Video in This Tile",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                secondaryPlayingTile = slot
                                activeMenuTile = null
                            }
                        )
                    }

                    // Button 2: Fullscreen
                    MultiviewMenuButton(
                        text = "Fullscreen",
                        icon = Icons.Default.Fullscreen,
                        enabled = currentChannel != null,
                        onClick = {
                            activeMenuTile = null
                            currentChannel?.let(onExpandTile)
                        }
                    )

                    // Button 3: Change Channel
                    MultiviewMenuButton(
                        text = "Change Channel",
                        icon = Icons.Default.LiveTv,
                        onClick = {
                            val targetSlot = slot
                            activeMenuTile = null
                            channelPickerSlot = targetSlot
                        }
                    )

                    // Button 4: Auto-fill Top 20 Games
                    MultiviewMenuButton(
                        text = "Auto-fill Top 20 Games",
                        icon = Icons.Default.Bolt,
                        onClick = {
                            activeMenuTile = null
                            onAutoFillTop20()
                        }
                    )
                }
            },
            confirmButton = {
                TvFocusButton(
                    text = "Close",
                    onClick = { activeMenuTile = null }
                )
            }
        )
    }

    // Change Channel Picker Dialog with Quick Filter Chips & Search Box
    if (channelPickerSlot != null) {
        val slot = channelPickerSlot!!
        MultiviewChannelPickerDialog(
            slot = slot,
            allChannels = channels,
            onDismiss = { channelPickerSlot = null },
            onChannelChosen = { chosenChannel ->
                onSlotChannelChange(slot, chosenChannel)
                channelPickerSlot = null
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MultiviewTile(
    tileIndex: Int,
    channel: Channel?,
    gameTitle: String?,
    isSoundActive: Boolean,
    isPlaybackEnabled: Boolean,
    focusRequester: FocusRequester,
    onStartPlayback: () -> Unit,
    onOpenMenu: () -> Unit,
    onTryBackup: () -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var isReleased by remember { mutableStateOf(false) }

    // Dedicated status state for this tile
    var playerStatus by remember { mutableStateOf("Initializing") }
    var statusIsError by remember { mutableStateOf(false) }
    var statusDetailText by remember { mutableStateOf("") }
    var isTryingBackup by remember { mutableStateOf(false) }

    // (1) & (2) Independent ExoPlayer for this tile, created ONLY when playback is enabled.
    // Capped: DefaultTrackSelector (640x360, 1.2Mbps for non-sound), small buffer (2s min, 5s max),
    // and DefaultRenderersFactory with decoder fallback.
    val exoPlayer = remember(tileIndex, channel?.id, isPlaybackEnabled) {
        val streamUrl = channel?.streamUrl
        if (!isPlaybackEnabled || streamUrl.isNullOrBlank()) {
            null
        } else {
            try {
                val renderersFactory = DefaultRenderersFactory(context).apply {
                    setEnableDecoderFallback(true)
                }

                val trackSelector = DefaultTrackSelector(context).apply {
                    if (!isSoundActive) {
                        parameters = buildUponParameters()
                            .setMaxVideoSize(640, 360)
                            .setMaxVideoBitrate(1_200_000)
                            .build()
                    }
                }

                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 2000,
                        /* maxBufferMs = */ 5000,
                        /* bufferForPlaybackMs = */ 1000,
                        /* bufferForPlaybackAfterRebufferMs = */ 1500
                    )
                    .setBackBuffer(
                        /* backBufferDurationMs = */ 0,
                        /* retainBackBufferFromKeyframe = */ false
                    )
                    .build()

                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android 14; Fire TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(8000)
                    .setReadTimeoutMs(8000)

                val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

                ExoPlayer.Builder(context, renderersFactory)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build().apply {
                        repeatMode = Player.REPEAT_MODE_OFF
                        volume = if (isSoundActive) 1.0f else 0.0f
                        val mediaItem = MediaItem.Builder().setUri(Uri.parse(streamUrl)).build()
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
            } catch (e: Exception) {
                Log.e("MultiviewTile", "Error creating ExoPlayer for tile $tileIndex", e)
                null
            }
        }
    }

    // (4) Track playback state & errors, and release player when channel changes,
    // tile disabled, or screen leaves composition or activity stops
    DisposableEffect(exoPlayer, lifecycleOwner) {
        if (exoPlayer != null && !isReleased) {
            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    statusIsError = true
                    val cause = error.cause?.localizedMessage ?: error.localizedMessage ?: ""
                    statusDetailText = when {
                        cause.contains("404", ignoreCase = true) -> "HTTP 404 Feed Not Found"
                        cause.contains("timeout", ignoreCase = true) -> "Stream Timed Out"
                        else -> "Error: ${error.errorCodeName}"
                    }
                    playerStatus = "Error"
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            if (!statusIsError) playerStatus = "Loading"
                        }
                        Player.STATE_READY -> {
                            statusIsError = false
                            isTryingBackup = false
                            playerStatus = "Playing"
                        }
                        Player.STATE_ENDED -> {
                            playerStatus = "Stream Ended"
                        }
                        Player.STATE_IDLE -> {
                            if (!statusIsError) playerStatus = "Idle"
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        statusIsError = false
                        isTryingBackup = false
                        playerStatus = "Playing"
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

    // Audio & Track quality routing
    LaunchedEffect(exoPlayer, isSoundActive, isReleased) {
        if (!isReleased && exoPlayer != null) {
            try {
                exoPlayer.volume = if (isSoundActive) 1.0f else 0.0f
                val selector = exoPlayer.trackSelector as? DefaultTrackSelector
                if (selector != null) {
                    selector.parameters = selector.buildUponParameters().apply {
                        if (!isSoundActive) {
                            setMaxVideoSize(640, 360)
                            setMaxVideoBitrate(1_200_000)
                        } else {
                            clearVideoSizeConstraints()
                            setMaxVideoBitrate(Int.MAX_VALUE)
                        }
                    }.build()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Reset status when channel changes
    LaunchedEffect(channel?.id) {
        if (channel != null) {
            playerStatus = "Loading"
            statusIsError = false
            statusDetailText = ""
            isTryingBackup = false
        } else {
            playerStatus = "No channel found"
            statusIsError = false
            statusDetailText = ""
        }
    }

    // Watchdog: If stream errors or stalls in "Loading" for > 8 seconds, automatically try backup candidate channel
    LaunchedEffect(channel?.id, playerStatus, statusIsError, isPlaybackEnabled) {
        if (isPlaybackEnabled && channel != null && (statusIsError || playerStatus == "Loading")) {
            delay(8000)
            if (statusIsError || playerStatus == "Loading") {
                isTryingBackup = true
                playerStatus = "Trying backup channel"
                val swapped = onTryBackup()
                if (!swapped) {
                    isTryingBackup = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TvDarkCard)
            .border(
                width = if (isFocused) 3.5.dp else if (isSoundActive) 2.dp else 1.dp,
                color = when {
                    isFocused -> TvOrangeFocus
                    isSoundActive -> TvLimeAccent
                    else -> TvDarkCardElevated
                },
                shape = RoundedCornerShape(10.dp)
            )
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (channel != null && !isPlaybackEnabled) {
                        onStartPlayback()
                    } else {
                        onOpenMenu()
                    }
                }
            )
    ) {
        // Video View surface, Still poster, or Empty state
        if (channel != null && isPlaybackEnabled && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        try {
                            if (!isReleased) this.player = exoPlayer
                        } catch (e: Exception) {
                            Log.e("MultiviewTile", "Error assigning player to PlayerView", e)
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
                        Log.e("MultiviewTile", "Error updating PlayerView", e)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (channel != null && !isPlaybackEnabled) {
            // (1) Still poster with channel name and 'Press OK to play' hint
            StillPosterTileView(
                channel = channel,
                gameTitle = gameTitle,
                isFocused = isFocused
            )
        } else {
            // No Channel found state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TvDarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TvOrangeFocus,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No channel found",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Press OK to pick one",
                        color = TvLimeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Top Header: Game Name / Channel Title & Sound Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC0D0D10))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!gameTitle.isNullOrBlank()) {
                    Text(
                        text = gameTitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channel != null) {
                        Text(
                            text = channel.name,
                            color = TvLimeAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = channel?.name ?: "Tile ${tileIndex + 1} (Empty)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Audio Indicator Badge
            if (isSoundActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvLimeAccent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Sound Active",
                        tint = Color.Black,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "AUDIO ON",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x77000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = "Muted",
                        tint = TvTextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "MUTED",
                        color = TvTextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // Status Overlay: Loading / Playing / Error text / Backup notification (only when playback enabled)
        if (channel != null && isPlaybackEnabled) {
            when {
                isTryingBackup -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE60D0D10))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = TvOrangeFocus,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trying backup channel...",
                                color = TvOrangeFocus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                statusIsError -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xEE1A0C0C))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = TvOrangeFocus,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Playback Error",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (statusDetailText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = statusDetailText,
                                    color = Color(0xFFF87171),
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = channel.name,
                                color = TvTextMuted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                playerStatus == "Loading" -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC0D0D10))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = TvLimeAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Loading...",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = channel.name,
                                    color = TvTextMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                playerStatus == "Playing" -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 34.dp, end = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC0D0D10))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(TvLimeAccent)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Playing",
                                color = TvLimeAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Focused Action Hint at Bottom
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xE60D0D10))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (channel != null && !isPlaybackEnabled) "Press OK to play" else "Press OK for Action Menu",
                    color = TvOrangeFocus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StillPosterTileView(
    channel: Channel,
    gameTitle: String?,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF13151B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Play Icon Pill with 'Press OK to play' hint
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isFocused) TvOrangeFocus else Color(0x33FFFFFF))
                    .border(
                        width = 1.5.dp,
                        color = if (isFocused) TvOrangeFocus else TvLimeAccent,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isFocused) Color.Black else TvLimeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Press OK to play",
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!gameTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = gameTitle,
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Standby • Video Decoder Saved",
                color = TvTextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun MultiviewMenuButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    isHighlighted: Boolean = false,
    autoFocus: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
        try { focusRequester.requestFocus() } catch (e: Exception) { }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !enabled -> Color(0xFF1E1E22)
                    isFocused -> TvOrangeFocus
                    isHighlighted -> TvDarkCardElevated
                    else -> TvDarkCard
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else if (isHighlighted) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> Color.White
                    isHighlighted -> TvLimeAccent
                    else -> Color(0xFF2E2E36)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .then(if (autoFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> TvTextMuted
                isFocused -> Color.Black
                isHighlighted -> TvLimeAccent
                else -> Color.White
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = when {
                !enabled -> TvTextMuted
                isFocused -> Color.Black
                else -> Color.White
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun MultiviewChannelPickerDialog(
    slot: Int,
    allChannels: List<Channel>,
    onDismiss: () -> Unit,
    onChannelChosen: (Channel) -> Unit
) {
    val filterChips = remember {
        listOf("All", "ESPN", "SEC", "NFL", "NBA", "MLB", "NHL", "Local")
    }
    var selectedChip by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredChannels = remember(allChannels, selectedChip, searchQuery) {
        allChannels.filter { channel ->
            val name = channel.name
            val group = channel.groupTitle

            val matchesChip = when (selectedChip) {
                "All" -> true
                "Local" -> {
                    listOf("ABC", "CBS", "NBC", "FOX").any { loc ->
                        name.contains(loc, ignoreCase = true) || group.contains(loc, ignoreCase = true)
                    }
                }
                else -> {
                    name.contains(selectedChip, ignoreCase = true) || group.contains(selectedChip, ignoreCase = true)
                }
            }

            val matchesSearch = if (searchQuery.isNotBlank()) {
                val qTerms = searchQuery.lowercase(Locale.ROOT).split(" ").filter { it.isNotBlank() }
                val nameLower = name.lowercase(Locale.ROOT)
                val groupLower = group.lowercase(Locale.ROOT)
                qTerms.all { term -> nameLower.contains(term) || groupLower.contains(term) }
            } else true

            matchesChip && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvDarkSurface,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assign Channel: Tile ${slot + 1}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredChannels.size} found",
                        color = TvLimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a quick filter or enter text to pick a channel for this pane",
                    color = TvTextMuted,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Filter Chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterChips.forEach { chip ->
                        val isSelected = selectedChip == chip
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isFocused -> TvOrangeFocus
                                        isSelected -> TvLimeAccent
                                        else -> TvDarkCard
                                    }
                                )
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) Color.White else Color(0xFF2E2E36),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .focusable(interactionSource = interactionSource)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { selectedChip = chip }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chip,
                                color = if (isFocused || isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search channel name or team...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TvOrangeFocus,
                        unfocusedBorderColor = TvDarkCardElevated,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = TvDarkCard,
                        unfocusedContainerColor = TvDarkCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Channels List
                if (filteredChannels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvDarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No channels match '$selectedChip' / search query",
                            color = TvTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredChannels, key = { it.id }) { ch ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) TvOrangeFocus else TvDarkCard)
                                    .border(
                                        width = if (isFocused) 2.dp else 0.dp,
                                        color = if (isFocused) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusable(interactionSource = interactionSource)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onChannelChosen(ch) }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ch.name,
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ch.groupTitle,
                                    color = if (isFocused) Color(0xFF222222) else TvTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TvFocusButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    )
}
