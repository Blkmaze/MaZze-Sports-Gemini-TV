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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Channel
import com.example.player.TvStreamPlayer
import com.example.ui.components.TvClock
import com.example.ui.components.TvFocusButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

// Main Sports Channels keywords requested by the user
val MAIN_SPORTS_KEYWORDS = listOf(
    "ESPN", "ESPN2", "ESPNU", "SEC Network", "FS1", "FS2",
    "CBS Sports", "NBC Sports", "Golf", "NFL Network",
    "NBA TV", "MLB Network", "NHL Network", "TNT", "TBS",
    "ABC", "CBS", "NBC", "FOX"
)

@Composable
fun ChannelsScreen(
    channels: List<Channel>,
    selectedChannel: Channel?,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {},
    onChannelSelected: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onExpandFullScreen: (Channel) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Filter Chips: "Main Sports", "All", "Sports", "ESPN", "SEC", "NFL", "NBA", "MLB", "NHL", "Local", "Favorites"
    var selectedFilterChip by remember { mutableStateOf("All") }
    var localSearchQuery by remember(searchQuery) { mutableStateOf(searchQuery) }
    var showSearchDialog by remember { mutableStateOf(false) }

    val displayedChannels = remember(channels, selectedFilterChip, localSearchQuery) {
        channels.filter { ch ->
            val name = ch.name
            val group = ch.groupTitle

            val matchesChip = when (selectedFilterChip) {
                "All" -> true
                "Main Sports" -> {
                    MAIN_SPORTS_KEYWORDS.any { kw ->
                        name.contains(kw, ignoreCase = true)
                    }
                }
                "Sports" -> {
                    name.contains("sports", ignoreCase = true) ||
                    group.contains("sports", ignoreCase = true) ||
                    group.contains("sport", ignoreCase = true)
                }
                "ESPN" -> {
                    name.contains("ESPN", ignoreCase = true) || group.contains("ESPN", ignoreCase = true)
                }
                "SEC" -> {
                    name.contains("SEC", ignoreCase = true) || group.contains("SEC", ignoreCase = true)
                }
                "NFL" -> {
                    name.contains("NFL", ignoreCase = true) || group.contains("NFL", ignoreCase = true)
                }
                "NBA" -> {
                    name.contains("NBA", ignoreCase = true) || group.contains("NBA", ignoreCase = true)
                }
                "MLB" -> {
                    name.contains("MLB", ignoreCase = true) || group.contains("MLB", ignoreCase = true)
                }
                "NHL" -> {
                    name.contains("NHL", ignoreCase = true) || group.contains("NHL", ignoreCase = true)
                }
                "Local" -> {
                    listOf("ABC", "CBS", "NBC", "FOX").any { loc ->
                        name.contains(loc, ignoreCase = true) || group.contains(loc, ignoreCase = true)
                    }
                }
                "Favorites" -> ch.isFavorite
                else -> true
            }

            val matchesSearch = if (localSearchQuery.isNotBlank()) {
                val qTerms = localSearchQuery.lowercase(Locale.ROOT).split(" ").filter { it.isNotBlank() }
                val nameLower = name.lowercase(Locale.ROOT)
                val groupLower = group.lowercase(Locale.ROOT)
                qTerms.all { term -> nameLower.contains(term) || groupLower.contains(term) }
            } else true

            matchesChip && matchesSearch
        }
    }

    val alphabet = ('A'..'Z').toList()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Filters, Search, A-Z Strip, Channels List
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Screen Title, Channel Count, and TvClock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Channels",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${displayedChannels.size} of ${channels.size})",
                        color = TvLimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TvClock()
            }

            // Quick Filter Chips Bar (Focusable D-pad Buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shortcut: Main Sports Channels
                ChannelFilterChip(
                    text = "Main Sports Channels",
                    icon = Icons.Default.Bolt,
                    isSelected = selectedFilterChip == "Main Sports",
                    isSpecial = true,
                    onClick = {
                        selectedFilterChip = if (selectedFilterChip == "Main Sports") "All" else "Main Sports"
                    }
                )

                // Quick Filter Chips: Sports, ESPN, SEC, NFL, NBA, MLB, NHL, Local, Favorites, All
                val chipNames = listOf("Sports", "ESPN", "SEC", "NFL", "NBA", "MLB", "NHL", "Local", "Favorites", "All")
                chipNames.forEach { chip ->
                    ChannelFilterChip(
                        text = if (chip == "Local") "Local (ABC/CBS/NBC/FOX)" else chip,
                        isSelected = selectedFilterChip == chip,
                        onClick = {
                            selectedFilterChip = if (selectedFilterChip == chip && chip != "All") "All" else chip
                        }
                    )
                }

                // Search Button
                ChannelFilterChip(
                    text = if (localSearchQuery.isNotBlank()) "Search: '$localSearchQuery'" else "Search",
                    icon = Icons.Default.Search,
                    isSelected = localSearchQuery.isNotBlank(),
                    onClick = { showSearchDialog = true }
                )
            }

            // Search query banner if active
            if (localSearchQuery.isNotBlank() || selectedFilterChip != "All") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvDarkCardElevated)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append("Filter: ")
                            if (selectedFilterChip != "All") append(selectedFilterChip)
                            if (localSearchQuery.isNotBlank()) {
                                if (selectedFilterChip != "All") append(" • ")
                                append("Search: '$localSearchQuery'")
                            }
                        },
                        color = TvOrangeFocus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reset All [X]",
                        color = TvLimeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                selectedFilterChip = "All"
                                localSearchQuery = ""
                                onClearSearch()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // A-Z Jump Strip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(alphabet) { letter ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isFocused) TvOrangeFocus else TvDarkCardElevated)
                            .focusable(interactionSource = interactionSource)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    val index = displayedChannels.indexOfFirst {
                                        it.name.trim().startsWith(letter.toString(), ignoreCase = true)
                                    }
                                    if (index != -1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(index)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Channels List
            if (displayedChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvDarkCard),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TvOff,
                            contentDescription = null,
                            tint = TvTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No channels match filter '$selectedFilterChip'",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TvFocusButton(
                            text = "Show All Channels",
                            onClick = {
                                selectedFilterChip = "All"
                                localSearchQuery = ""
                                onClearSearch()
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayedChannels, key = { it.id }) { channel ->
                        ChannelListItem(
                            channel = channel,
                            isSelected = selectedChannel?.id == channel.id,
                            onSelect = { onChannelSelected(channel) },
                            onToggleFav = { onToggleFavorite(channel.id) }
                        )
                    }
                }
            }
        }

        // Right Column: Live Video Preview + Channel Info Bar
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvDarkCard)
                    .border(1.5.dp, TvDarkCardElevated, RoundedCornerShape(12.dp))
            ) {
                TvStreamPlayer(
                    channel = selectedChannel,
                    isSoundActive = true,
                    isFullScreen = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Preview Info Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvDarkCard)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedChannel?.name ?: "Select a channel",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Category: ${selectedChannel?.groupTitle ?: "Sports"}",
                        color = TvLimeAccent,
                        fontSize = 12.sp
                    )
                }

                if (selectedChannel != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TvFocusButton(
                            text = if (selectedChannel.isFavorite) "Favorited" else "Favorite",
                            icon = if (selectedChannel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            onClick = { onToggleFavorite(selectedChannel.id) }
                        )
                        TvFocusButton(
                            text = "Fullscreen",
                            icon = Icons.Default.Fullscreen,
                            isPrimary = true,
                            onClick = { onExpandFullScreen(selectedChannel) }
                        )
                    }
                }
            }
        }
    }

    // On-screen Search Box Dialog
    if (showSearchDialog) {
        var tempQuery by remember { mutableStateOf(localSearchQuery) }
        val quickQueries = listOf("ESPN", "SEC", "FOX", "CBS", "NBC", "ABC", "NFL", "NBA", "MLB", "College")

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            containerColor = TvDarkSurface,
            title = {
                Text(
                    text = "Search Sports Channels",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempQuery,
                        onValueChange = { tempQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search channel name or group...", color = TvTextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TvTextMuted)
                        },
                        trailingIcon = {
                            if (tempQuery.isNotEmpty()) {
                                IconButton(onClick = { tempQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
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

                    // Quick Search Keywords Strip
                    Text(
                        text = "Quick Search Shortcuts:",
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickQueries.forEach { keyword ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isFocused) TvOrangeFocus else TvDarkCardElevated)
                                    .focusable(interactionSource = interactionSource)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            localSearchQuery = keyword
                                            showSearchDialog = false
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = keyword,
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvFocusButton(
                        text = "Apply Search",
                        isPrimary = true,
                        onClick = {
                            localSearchQuery = tempQuery
                            showSearchDialog = false
                        }
                    )
                    TvFocusButton(
                        text = "Clear",
                        onClick = {
                            tempQuery = ""
                            localSearchQuery = ""
                            onClearSearch()
                            showSearchDialog = false
                        }
                    )
                }
            },
            dismissButton = {
                TvFocusButton(
                    text = "Cancel",
                    onClick = { showSearchDialog = false }
                )
            }
        )
    }
}

@Composable
fun ChannelFilterChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean = false,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> TvOrangeFocus
                    isSelected -> TvLimeAccent
                    isSpecial -> Color(0xFF2A2416)
                    else -> TvDarkCard
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected || isSpecial) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> Color.White
                    isSelected -> TvLimeAccent
                    isSpecial -> TvOrangeFocus
                    else -> TvDarkCardElevated
                },
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused || isSelected) Color.Black else if (isSpecial) TvOrangeFocus else Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = when {
                isFocused || isSelected -> Color.Black
                isSpecial -> TvOrangeFocus
                else -> Color.White
            },
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> TvOrangeFocus
                    isSelected -> TvDarkCardElevated
                    else -> TvDarkCard
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else if (isSelected) TvOrangeFocus else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) Color.Black else TvDarkCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = if (isFocused) TvOrangeFocus else TvTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Text(
                    text = channel.name,
                    color = if (isFocused) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.groupTitle,
                    color = if (isFocused) Color(0xFF222222) else TvTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Favorite icon toggle
        IconButton(
            onClick = onToggleFav,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Toggle Favorite",
                tint = if (channel.isFavorite) TvOrangeFocus else if (isFocused) Color.Black else TvTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
