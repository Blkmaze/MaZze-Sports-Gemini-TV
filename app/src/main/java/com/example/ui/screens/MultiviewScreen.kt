package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.player.TvStreamPlayer
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*

@Composable
fun MultiviewScreen(
    channels: List<Channel>,
    multiviewChannels: List<Channel?>,
    activeSoundIndex: Int,
    onSetSoundIndex: (Int) -> Unit,
    onSlotChannelChange: (Int, Channel) -> Unit,
    onExpandTile: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var channelPickerSlot by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Multiview 2x2",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tip: Press OK on any tile to switch sound to it",
                    color = TvLimeAccent,
                    fontSize = 12.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Active Sound: Tile ${activeSoundIndex + 1}",
                    color = TvOrangeFocus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvDarkCardElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                com.example.ui.components.TvClock()
            }
        }

        // 2x2 Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column (Tiles 0 & 2)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MultiviewTile(
                    tileIndex = 0,
                    channel = multiviewChannels.getOrNull(0),
                    isSoundActive = activeSoundIndex == 0,
                    onSelectSound = { onSetSoundIndex(0) },
                    onChangeChannel = { channelPickerSlot = 0 },
                    onExpand = { multiviewChannels.getOrNull(0)?.let(onExpandTile) },
                    modifier = Modifier.weight(1f)
                )
                MultiviewTile(
                    tileIndex = 2,
                    channel = multiviewChannels.getOrNull(2),
                    isSoundActive = activeSoundIndex == 2,
                    onSelectSound = { onSetSoundIndex(2) },
                    onChangeChannel = { channelPickerSlot = 2 },
                    onExpand = { multiviewChannels.getOrNull(2)?.let(onExpandTile) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Right Column (Tiles 1 & 3)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MultiviewTile(
                    tileIndex = 1,
                    channel = multiviewChannels.getOrNull(1),
                    isSoundActive = activeSoundIndex == 1,
                    onSelectSound = { onSetSoundIndex(1) },
                    onChangeChannel = { channelPickerSlot = 1 },
                    onExpand = { multiviewChannels.getOrNull(1)?.let(onExpandTile) },
                    modifier = Modifier.weight(1f)
                )
                MultiviewTile(
                    tileIndex = 3,
                    channel = multiviewChannels.getOrNull(3),
                    isSoundActive = activeSoundIndex == 3,
                    onSelectSound = { onSetSoundIndex(3) },
                    onChangeChannel = { channelPickerSlot = 3 },
                    onExpand = { multiviewChannels.getOrNull(3)?.let(onExpandTile) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Channel Picker Dialog for Slot
    if (channelPickerSlot != null) {
        val slot = channelPickerSlot!!
        AlertDialog(
            onDismissRequest = { channelPickerSlot = null },
            containerColor = TvDarkSurface,
            title = {
                Text(
                    text = "Select Channel for Tile ${slot + 1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(channels.size) { idx ->
                            val ch = channels[idx]
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) TvOrangeFocus else TvDarkCard)
                                    .focusable(interactionSource = interactionSource)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            onSlotChannelChange(slot, ch)
                                            channelPickerSlot = null
                                        }
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ch.name,
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = ch.groupTitle,
                                    color = if (isFocused) Color.Black else TvTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TvFocusButton(
                    text = "Close",
                    onClick = { channelPickerSlot = null }
                )
            }
        )
    }
}

@Composable
fun MultiviewTile(
    tileIndex: Int,
    channel: Channel?,
    isSoundActive: Boolean,
    onSelectSound: () -> Unit,
    onChangeChannel: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TvDarkCard)
            .border(
                width = if (isFocused) 3.5.dp else if (isSoundActive) 2.dp else 1.dp,
                color = when {
                    isFocused -> TvOrangeFocus
                    isSoundActive -> TvLimeAccent
                    else -> TvDarkCardElevated
                },
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelectSound
            )
    ) {
        // Player
        TvStreamPlayer(
            channel = channel,
            isSoundActive = isSoundActive,
            isFullScreen = false,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xB30D0D10))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channel?.name ?: "Tile ${tileIndex + 1} (Empty)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            // Audio Indicator
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
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUDIO ON",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = "Muted",
                        tint = TvTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MUTED",
                        color = TvTextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Focus Action Overlays at Bottom
        if (isFocused) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCC0D0D10))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OK: Toggle Sound",
                    color = TvOrangeFocus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Change Channel",
                        color = TvLimeAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onChangeChannel() }
                            .padding(4.dp)
                    )
                    if (channel != null) {
                        Text(
                            text = "Fullscreen",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onExpand() }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
