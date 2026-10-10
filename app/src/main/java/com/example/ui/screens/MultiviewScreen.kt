package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.player.TvStreamPlayer
import com.example.ui.components.TvClock
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*
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
    modifier: Modifier = Modifier
) {
    var activeMenuTile by remember { mutableStateOf<Int?>(null) }
    var channelPickerSlot by remember { mutableStateOf<Int?>(null) }

    // Focus requesters for the 4 grid tiles
    val tileFocusRequesters = remember { List(4) { FocusRequester() } }

    LaunchedEffect(Unit) {
        val initialFocusIndex = if (activeSoundIndex in 0..3) activeSoundIndex else 0
        tileFocusRequesters.getOrNull(initialFocusIndex)?.requestFocus()
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
                    focusRequester = tileFocusRequesters[0],
                    onClick = { activeMenuTile = 0 },
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
                    focusRequester = tileFocusRequesters[1],
                    onClick = { activeMenuTile = 1 },
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
                    focusRequester = tileFocusRequesters[2],
                    onClick = { activeMenuTile = 2 },
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
                    focusRequester = tileFocusRequesters[3],
                    onClick = { activeMenuTile = 3 },
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
                            onSetSoundIndex(slot)
                            activeMenuTile = null
                        }
                    )

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

@Composable
fun MultiviewTile(
    tileIndex: Int,
    channel: Channel?,
    gameTitle: String?,
    isSoundActive: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

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
                onClick = onClick
            )
    ) {
        // Player stream
        TvStreamPlayer(
            channel = channel,
            isSoundActive = isSoundActive,
            isFullScreen = false,
            modifier = Modifier.fillMaxSize()
        )

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
                    text = "Press OK for Action Menu",
                    color = TvOrangeFocus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
            focusRequester.requestFocus()
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
                        text = "Change Channel • Tile ${slot + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${filteredChannels.size} matching",
                        color = TvLimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Filter Chips Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterChips.forEach { chip ->
                        val isSelected = selectedChip == chip
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isFocused -> TvOrangeFocus
                                        isSelected -> TvLimeAccent
                                        else -> TvDarkCard
                                    }
                                )
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) Color.White else if (isSelected) TvLimeAccent else TvDarkCardElevated,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .focusable(interactionSource = interactionSource)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { selectedChip = chip }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chip == "Local") "Local (ABC/CBS/NBC/FOX)" else chip,
                                color = if (isFocused || isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search channel name or category...", color = TvTextMuted, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TvTextMuted, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TvOrangeFocus,
                        unfocusedBorderColor = TvDarkCardElevated,
                        focusedContainerColor = TvDarkCard,
                        unfocusedContainerColor = TvDarkCard
                    )
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
