package com.example.ui.screens

import android.content.Context
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.model.Channel
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*

@Composable
fun MultiviewScreen(
    channels: List<Channel>,
    multiviewChannels: List<Channel?>,
    multiviewGameTitles: List<String?>,
    activeSoundIndex: Int,
    onSetSoundIndex: (Int) -> Unit,
    onSlotChannelChange: (Int, Channel) -> Unit,
    onAutoFillTop20: () -> Unit,
    onExpandTile: (Channel) -> Unit,
    onCycleTileBackup: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeMenuTile by remember { mutableStateOf<Int?>(null) }
    var channelPickerSlot by remember { mutableStateOf<Int?>(null) }
    var secondaryPlayingTile by remember { mutableIntStateOf((activeSoundIndex + 1) % 4) }

    // Ensure secondary tile is always distinct from sound active tile
    LaunchedEffect(activeSoundIndex) {
        if (secondaryPlayingTile == activeSoundIndex) {
            secondaryPlayingTile = (activeSoundIndex + 1) % 4
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(16.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Quad Multiview",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Audio: Tile ${activeSoundIndex + 1} • Max 2 Decoders Active",
                    color = TvLimeAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row {
                TvFocusButton(
                    text = "Auto-Fill AP Top 20",
                    icon = Icons.Default.AutoFixHigh,
                    onClick = onAutoFillTop20
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Multiview Grid
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1 (Tiles 0 & 1)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (idx in 0..1) {
                    val isSound = idx == activeSoundIndex
                    // Only active sound tile and at most 1 secondary tile decode video
                    val shouldPlayVideo = (idx == activeSoundIndex || idx == secondaryPlayingTile)

                    MultiviewTile(
                        tileIndex = idx,
                        channel = multiviewChannels.getOrNull(idx),
                        gameTitle = multiviewGameTitles.getOrNull(idx),
                        isSoundActive = isSound,
                        shouldPlayVideo = shouldPlayVideo,
                        onClick = {
                            if (activeSoundIndex != idx) {
                                onSetSoundIndex(idx)
                            }
                            secondaryPlayingTile = idx
                        },
                        onOpenMenu = { activeMenuTile = idx },
                        onStartPlayback = {
                            secondaryPlayingTile = idx
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            // Row 2 (Tiles 2 & 3)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (idx in 2..3) {
                    val isSound = idx == activeSoundIndex
                    val shouldPlayVideo = (idx == activeSoundIndex || idx == secondaryPlayingTile)

                    MultiviewTile(
                        tileIndex = idx,
                        channel = multiviewChannels.getOrNull(idx),
                        gameTitle = multiviewGameTitles.getOrNull(idx),
                        isSoundActive = isSound,
                        shouldPlayVideo = shouldPlayVideo,
                        onClick = {
                            if (activeSoundIndex != idx) {
                                onSetSoundIndex(idx)
                            }
                            secondaryPlayingTile = idx
                        },
                        onOpenMenu = { activeMenuTile = idx },
                        onStartPlayback = {
                            secondaryPlayingTile = idx
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    // Tile Menu Dialog (Switch Channel, Backup stream, Fullscreen, Change Audio)
    activeMenuTile?.let { slot ->
        val currentChannel = multiviewChannels.getOrNull(slot)
        Dialog(onDismissRequest = { activeMenuTile = null }) {
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDarkSurface)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Tile ${slot + 1} Options",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentChannel?.name ?: "Empty Tile",
                        color = TvOrangeFocus,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TvFocusButton(
                        text = "Select / Change Channel",
                        icon = Icons.Default.Tv,
                        isPrimary = true,
                        onClick = {
                            val targetSlot = activeMenuTile
                            activeMenuTile = null
                            channelPickerSlot = targetSlot
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TvFocusButton(
                        text = "Set Sound Active",
                        icon = Icons.Default.VolumeUp,
                        onClick = {
                            onSetSoundIndex(slot)
                            secondaryPlayingTile = slot
                            activeMenuTile = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentChannel != null) {
                        TvFocusButton(
                            text = "Watch Fullscreen",
                            icon = Icons.Default.Fullscreen,
                            onClick = {
                                activeMenuTile = null
                                onExpandTile(currentChannel)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        TvFocusButton(
                            text = "Cycle to Backup Channel",
                            icon = Icons.Default.SwapHoriz,
                            onClick = {
                                onCycleTileBackup(slot)
                                activeMenuTile = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    TvFocusButton(
                        text = "Close",
                        onClick = { activeMenuTile = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Channel Picker Dialog for Slot
    channelPickerSlot?.let { slot ->
        MultiviewChannelPickerDialog(
            slot = slot,
            allChannels = channels,
            onDismiss = { channelPickerSlot = null },
            onChannelChosen = { ch ->
                onSlotChannelChange(slot, ch)
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
    shouldPlayVideo: Boolean,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
    onStartPlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var isPlayerError by remember { mutableStateOf(false) }
    var errorDetail by remember { mutableStateOf("") }

    // Quality capped ExoPlayer: only instantiated if channel present AND decoder slot allowed
    val exoPlayer = remember(context, channel?.streamUrl, shouldPlayVideo) {
        if (!shouldPlayVideo || channel == null || channel.streamUrl.isBlank()) {
            null
        } else {
            try {
                val trackSelector = DefaultTrackSelector(context).apply {
                    // Cap non-sound tiles to 360p / 1.2Mbps to prevent hardware decoder starvation
                    if (!isSoundActive) {
                        setParameters(
                            buildUponParameters()
                                .setMaxVideoSize(640, 360)
                                .setMaxVideoBitrate(1200000)
                        )
                    }
                }

                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(2000, 5000, 1000, 1000)
                    .setBackBuffer(0, false)
                    .build()

                val renderersFactory = DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true)

                ExoPlayer.Builder(context, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build().apply {
                        val mediaItem = MediaItem.fromUri(channel.streamUrl)
                        setMediaItem(mediaItem)
                        volume = if (isSoundActive) 1.0f else 0.0f
                        prepare()
                        playWhenReady = true
                    }
            } catch (e: Throwable) {
                null
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                isPlayerError = true
                errorDetail = error.errorCodeName
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isPlayerError = false
                }
            }
        }
        try {
            exoPlayer?.addListener(listener)
        } catch (e: Throwable) {}

        onDispose {
            try {
                exoPlayer?.removeListener(listener)
                exoPlayer?.stop()
                exoPlayer?.release()
            } catch (e: Throwable) {}
        }
    }

    LaunchedEffect(exoPlayer, isSoundActive) {
        try {
            exoPlayer?.volume = if (isSoundActive) 1.0f else 0.0f
        } catch (e: Throwable) {}
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TvDarkCard)
            .border(
                BorderStroke(
                    width = if (isFocused) 3.dp else if (isSoundActive) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else if (isSoundActive) TvOrangeFocus else Color.Transparent
                ),
                RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            }
    ) {
        if (channel != null && !channel.streamUrl.isBlank()) {
            if (shouldPlayVideo && exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            keepScreenOn = true
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        try {
                            if (view.player != exoPlayer) {
                                view.player = exoPlayer
                            }
                        } catch (e: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Defensive Still Poster view when hardware decoders are reserved
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvDarkSurface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = TvTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TvOrangeFocus)
                                .clickable { onStartPlayback() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Press OK to play",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Empty slot
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onOpenMenu() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = TvTextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tile ${tileIndex + 1} (Empty)",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Click to assign channel",
                        color = TvTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Top Tile Status Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Tile ${tileIndex + 1}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                if (isSoundActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvOrangeFocus)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AUDIO ON",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Channel Name
        if (channel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = gameTitle ?: channel.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MultiviewChannelPickerDialog(
    slot: Int,
    allChannels: List<Channel>,
    onDismiss: () -> Unit,
    onChannelChosen: (Channel) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, allChannels) {
        if (searchQuery.isBlank()) allChannels
        else allChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .height(440.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TvDarkSurface)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Assign Channel to Tile ${slot + 1}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvDarkCard)
                                .clickable { onChannelChosen(ch) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ch.name,
                                color = TvTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ch.groupTitle,
                                color = TvOrangeFocus,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TvFocusButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
