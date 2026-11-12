package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.api.ChannelMatchingHelper
import com.example.model.Channel
import com.example.ui.components.TvFocusButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ChannelsScreen(
    channels: List<Channel>,
    selectedChannel: Channel?,
    searchQuery: String = "",
    categoryFilter: String = "All",
    onClearSearch: () -> Unit = {},
    onChannelSelected: (Channel) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onExpandFullScreen: (Channel) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilterChip by remember { mutableStateOf(categoryFilter.ifBlank { "All" }) }
    var localSearchQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery, categoryFilter) {
        localSearchQuery = searchQuery
        selectedFilterChip = if (categoryFilter.isNotBlank()) categoryFilter else "All"
    }

    val filterChips = listOf("All", "Favorites", "ESPN", "NCAAW", "NCAA", "Motorsport", "Soccer", "News", "Other")

    val isFilterActive = localSearchQuery.isNotBlank() || selectedFilterChip != "All"

    val (filteredChannels, isFilterWidened) = remember(channels, selectedFilterChip, localSearchQuery) {
        if (!isFilterActive) {
            return@remember Pair(channels, false)
        }

        var list = channels

        // 1. Category Filter Chip
        when (selectedFilterChip) {
            "All" -> { /* no-op */ }
            "Favorites" -> {
                list = list.filter { it.isFavorite }
            }
            "NCAAW" -> {
                list = list.filter { ch ->
                    val name = ch.name.lowercase(Locale.ROOT)
                    val grp = ch.groupTitle.lowercase(Locale.ROOT)
                    name.contains("ncaaw") || name.contains("wbb") || name.contains("women") ||
                            name.contains("espn w") || name.contains("espnw") ||
                            grp.contains("ncaaw") || grp.contains("women") ||
                            name.contains("sec network") || name.contains("acc network") ||
                            name.contains("big ten") || name.contains("btn") ||
                            name.contains("espnu") || name.contains("espn2") ||
                            name.contains("cbs sports") || name.contains("fs1") || name.contains("fs2")
                }
            }
            "NCAA" -> {
                list = list.filter { ch ->
                    val name = ch.name.lowercase(Locale.ROOT)
                    val grp = ch.groupTitle.lowercase(Locale.ROOT)
                    name.contains("ncaa") || name.contains("college") ||
                            grp.contains("ncaa") || grp.contains("college") ||
                            name.contains("sec network") || name.contains("acc network") ||
                            name.contains("big ten") || name.contains("btn") ||
                            name.contains("espnu") || name.contains("cbs sports")
                }
            }
            "ESPN" -> {
                list = list.filter { ch ->
                    val name = ch.name.lowercase(Locale.ROOT)
                    val grp = ch.groupTitle.lowercase(Locale.ROOT)
                    name.contains("espn") || grp.contains("espn") ||
                            name.contains("sec") || name.contains("acc")
                }
            }
            else -> {
                list = list.filter {
                    it.name.contains(selectedFilterChip, ignoreCase = true) ||
                            it.groupTitle.contains(selectedFilterChip, ignoreCase = true)
                }
            }
        }

        // 2. Search query filter
        if (localSearchQuery.isNotBlank()) {
            val q = localSearchQuery.trim().lowercase(Locale.ROOT)
            val queryAliases = ChannelMatchingHelper.getAliasesForName(localSearchQuery)
            val tokens = (listOf(q) + queryAliases + q.split(" ", "-", "/"))
                .filter { it.length > 2 }
                .distinct()

            list = list.filter { ch ->
                val name = ch.name.lowercase(Locale.ROOT)
                val grp = ch.groupTitle.lowercase(Locale.ROOT)

                if (name.contains(q) || grp.contains(q)) return@filter true

                tokens.any { token ->
                    name.contains(token) || grp.contains(token)
                }
            }
        }

        // 3. Auto-widen an empty filter with a note banner
        if (list.isEmpty() && channels.isNotEmpty()) {
            val sportsChannels = channels.filter { it.isSports }
            val widenedList = if (sportsChannels.isNotEmpty()) sportsChannels else channels
            Pair(widenedList, true)
        } else {
            Pair(list, false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Channels Guide",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isFilterWidened) {
                        "${filteredChannels.size} sports channels (filter widened)"
                    } else {
                        "${filteredChannels.size} channels available"
                    },
                    color = if (isFilterWidened) TvOrangeFocus else TvTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (localSearchQuery.isNotBlank() || selectedFilterChip != "All") {
                val filterDesc = if (localSearchQuery.isNotBlank()) "'$localSearchQuery'" else selectedFilterChip
                TvFocusButton(
                    text = "Clear Filter: $filterDesc",
                    icon = Icons.Default.Close,
                    onClick = {
                        localSearchQuery = ""
                        selectedFilterChip = "All"
                        onClearSearch()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterChips) { chip ->
                val isSelected = selectedFilterChip == chip
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                isFocused -> TvOrangeFocus
                                isSelected -> TvDarkCardElevated
                                else -> TvDarkCard
                            }
                        )
                        .focusable(interactionSource = interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            selectedFilterChip = chip
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chip,
                        color = if (isFocused) Color.Black else if (isSelected) TvOrangeFocus else TvTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Note banner when filter was automatically widened
        if (isFilterWidened) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2E2415))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TvOrangeFocus,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "No channels matched '${localSearchQuery.ifBlank { selectedFilterChip }}' · Filter widened to all sports channels",
                        color = Color(0xFFFFD59E),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                TvFocusButton(
                    text = "Clear Filter",
                    icon = Icons.Default.Close,
                    onClick = {
                        localSearchQuery = ""
                        selectedFilterChip = "All"
                        onClearSearch()
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Channels List
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TvTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No channels available in playlist",
                        color = TvTextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredChannels, key = { it.id }) { ch ->
                    ChannelListItem(
                        channel = ch,
                        isSelected = selectedChannel?.id == ch.id,
                        onSelect = { onChannelSelected(ch); onExpandFullScreen(ch) },
                        onToggleFav = { onToggleFavorite(ch.id) },
                        onPlayFullscreen = { onExpandFullScreen(ch) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit,
    onPlayFullscreen: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> TvDarkCardElevated
                    isSelected -> TvDarkCard
                    else -> TvDarkSurface
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .focusable(interactionSource = interactionSource)
                .clickable(interactionSource = interactionSource, indication = null) {
                    onSelect()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = if (isFocused) TvOrangeFocus else TvTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.groupTitle,
                    color = TvTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TvFocusButton(
                text = if (channel.isFavorite) "Favorited" else "Favorite",
                icon = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                onClick = onToggleFav
            )
            Spacer(modifier = Modifier.width(8.dp))
            TvFocusButton(
                text = "Watch",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayFullscreen
            )
        }
    }
}
