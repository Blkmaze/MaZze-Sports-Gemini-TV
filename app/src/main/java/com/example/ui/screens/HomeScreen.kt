package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.api.ChannelMatchingHelper
import com.example.model.Channel
import com.example.model.Game
import com.example.model.NcaaFootballData
import com.example.model.NcaaGame
import com.example.player.TvStreamPlayer
import com.example.ui.components.*
import com.example.ui.theme.*

data class GameActionMenuData(
    val title: String,
    val network: String,
    val candidateChannels: List<Channel>,
    val searchFilter: String
)

@Composable
fun HomeScreen(
    games: List<Game>,
    channels: List<Channel>,
    ncaaFootballData: NcaaFootballData,
    currentPlayingChannel: Channel?,
    isOffline: Boolean,
    lastUpdatedTime: String,
    spoilerFree: Boolean,
    revealedGameIds: Set<String>,
    onSelectGame: (Game) -> Unit = {},
    onSelectNcaaGame: (NcaaGame) -> Unit = {},
    onSelectNcaaTeam: (String) -> Unit = {},
    onToggleSpoiler: (String) -> Unit,
    onExpandFullScreen: () -> Unit,
    onNextChannel: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onAddToMultiview: (String, List<Channel>) -> Int = { _, _ -> 1 },
    onWatchFullScreenChannel: (Channel) -> Unit = {},
    onFindChannel: (String) -> Unit = {},
    isHeroPlayerActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedGameActionMenu by remember { mutableStateOf<GameActionMenuData?>(null) }

    val liveGames = remember(games) { games.filter { it.isLive } }
    val upcomingGames = remember(games) { games.filter { it.isPre } }
    val finalGames = remember(games) { games.filter { it.isFinal } }

    val topLiveGame = liveGames.firstOrNull() ?: games.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Status Bar at Top (Updated timestamp & offline badge)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MaZze Sports TV",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "LIVE HUB",
                        color = TvOrangeFocus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvDarkCardElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isOffline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFB91C1C))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline (Cached Data)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TvLimeAccent)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Synced: $lastUpdatedTime",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (channels.isEmpty()) {
                        TvFocusButton(
                            text = "Add Playlist",
                            icon = Icons.Default.Add,
                            onClick = onNavigateToSettings
                        )
                    }

                    TvClock()
                }
            }
        }

        // 1. Large Live Preview of the top game playing matching channel
        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDarkCard)
                    .border(
                        width = if (isFocused) 3.dp else 1.dp,
                        color = if (isFocused) TvOrangeFocus else TvDarkCardElevated,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .focusable(interactionSource = interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onExpandFullScreen
                    )
            ) {
                // Video Player surface (only active when on Home and not fullscreen)
                if (isHeroPlayerActive && currentPlayingChannel != null) {
                    TvStreamPlayer(
                        channel = currentPlayingChannel,
                        modifier = Modifier.fillMaxSize(),
                        isSoundActive = true,
                        isFullScreen = false,
                        onNextChannel = onNextChannel
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TvDarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentPlayingChannel?.name ?: "No Channel Selected",
                            color = TvTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                // Top score bar overlay
                ScoreBarOverlay(
                    game = topLiveGame,
                    channelTitle = currentPlayingChannel?.name ?: "Sports Broadcast",
                    spoilerFree = spoilerFree,
                    isSpoilerRevealed = topLiveGame?.let { revealedGameIds.contains(it.id) } ?: false,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Click to expand HUD at bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFocused) TvOrangeFocus else Color(0xCC0D0D10))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            tint = if (isFocused) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OK: Fullscreen",
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Row of Live Game Score Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Live Games",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (liveGames.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${liveGames.size} in progress",
                                color = TvLimeAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (spoilerFree) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TvOrangeFocus,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Spoiler-Free Active",
                                color = TvOrangeFocus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (games.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TvDarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connecting to ESPN Live Scoreboard...",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Show live games or recent games
                    val displayGames = if (liveGames.isNotEmpty()) liveGames else games.take(8)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(end = 24.dp)
                    ) {
                        items(displayGames, key = { it.id }) { game ->
                            GameScoreCard(
                                game = game,
                                spoilerFree = spoilerFree,
                                isSpoilerRevealed = revealedGameIds.contains(game.id),
                                onRevealToggle = { onToggleSpoiler(game.id) },
                                onClick = {
                                    onSelectGame(game)
                                    val candidates = ChannelMatchingHelper.findCandidatesForGame(game, channels, limit = 3)
                                    selectedGameActionMenu = GameActionMenuData(
                                        title = "${game.awayTeam.displayName} vs ${game.homeTeam.displayName}",
                                        network = game.network,
                                        candidateChannels = candidates,
                                        searchFilter = "${game.awayTeam.displayName} ${game.homeTeam.displayName}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Top 20 NCAA Football - SEC Focus Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Top 20 NCAA Football - SEC Focus",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TvLimeAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SEC",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    if (ncaaFootballData.lastUpdated.isNotBlank()) {
                        Text(
                            text = "AP Poll & SEC • Synced ${ncaaFootballData.lastUpdated}",
                            color = TvTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Horizontal strip of focusable cards 'AP Top 20' with rank and team
                if (ncaaFootballData.apTop20.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AP Top 20",
                                color = TvLimeAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• SEC teams highlighted in lime",
                                color = TvTextMuted,
                                fontSize = 10.sp
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 24.dp)
                        ) {
                            items(ncaaFootballData.apTop20, key = { it.teamId.ifEmpty { "rank_${it.rank}" } }) { rankItem ->
                                ApTop20Card(
                                    rankItem = rankItem,
                                    onClick = {
                                        onSelectNcaaTeam(rankItem.displayName.ifEmpty { rankItem.name })
                                    }
                                )
                            }
                        }
                    }
                }

                // Vertically scrollable list of NCAA games
                if (ncaaFootballData.games.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TvDarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading Top 20 & SEC matchups from ESPN...",
                            color = TvTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ncaaFootballData.games.forEach { ncaaGame ->
                            NcaaGameRow(
                                game = ncaaGame,
                                onClick = {
                                    onSelectNcaaGame(ncaaGame)
                                    val awayR = if (ncaaGame.awayRank != null) "#${ncaaGame.awayRank} " else ""
                                    val homeR = if (ncaaGame.homeRank != null) "#${ncaaGame.homeRank} " else ""
                                    val title = "${awayR}${ncaaGame.awayTeam.displayName} vs ${homeR}${ncaaGame.homeTeam.displayName}"
                                    val candidates = ChannelMatchingHelper.findCandidatesForNcaaGame(ncaaGame, channels, limit = 3)
                                    selectedGameActionMenu = GameActionMenuData(
                                        title = title,
                                        network = ncaaGame.network,
                                        candidateChannels = candidates,
                                        searchFilter = "${ncaaGame.awayTeam.displayName} ${ncaaGame.homeTeam.displayName}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Up Next List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Up Next & Today's Matchups",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (upcomingGames.isEmpty() && finalGames.isEmpty()) {
                    Text(
                        text = "No additional games scheduled today",
                        color = TvTextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    val remainingGames = (upcomingGames + finalGames).take(10)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        remainingGames.forEach { game ->
                            UpNextGameRow(
                                game = game,
                                onClick = {
                                    onSelectGame(game)
                                    val candidates = ChannelMatchingHelper.findCandidatesForGame(game, channels, limit = 3)
                                    selectedGameActionMenu = GameActionMenuData(
                                        title = "${game.awayTeam.displayName} vs ${game.homeTeam.displayName}",
                                        network = game.network,
                                        candidateChannels = candidates,
                                        searchFilter = "${game.awayTeam.displayName} ${game.homeTeam.displayName}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Game Action Menu Dialog (Watch Full Screen, Add to Multiview, Find Channel)
    if (selectedGameActionMenu != null) {
        val menuData = selectedGameActionMenu!!
        AlertDialog(
            onDismissRequest = { selectedGameActionMenu = null },
            containerColor = TvDarkCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Column {
                    Text(
                        text = menuData.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (menuData.network.isNotBlank() && menuData.network != "TV") "Broadcast: ${menuData.network}" else "Sports Broadcast",
                        color = TvLimeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (menuData.candidateChannels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Matched Channel: ${menuData.candidateChannels.first().name}",
                            color = TvTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Watch Full Screen
                    MultiviewMenuButton(
                        text = "Watch Full Screen",
                        icon = Icons.Default.Fullscreen,
                        autoFocus = true,
                        onClick = {
                            val targetChannel = menuData.candidateChannels.firstOrNull()
                            selectedGameActionMenu = null
                            if (targetChannel != null) {
                                onWatchFullScreenChannel(targetChannel)
                            } else {
                                Toast.makeText(context, "No channel found - searching channels", Toast.LENGTH_SHORT).show()
                                onFindChannel(menuData.searchFilter)
                            }
                        }
                    )

                    // Button 2: Add to Multiview
                    MultiviewMenuButton(
                        text = "Add to Multiview",
                        icon = Icons.Default.Dashboard,
                        onClick = {
                            val candidates = menuData.candidateChannels
                            val tileIndex = onAddToMultiview(menuData.title, candidates)
                            Toast.makeText(context, "Added to Multiview tile $tileIndex", Toast.LENGTH_SHORT).show()
                            selectedGameActionMenu = null
                        }
                    )

                    // Button 3: Find Channel
                    MultiviewMenuButton(
                        text = "Find Channel",
                        icon = Icons.Default.Search,
                        onClick = {
                            selectedGameActionMenu = null
                            onFindChannel(menuData.searchFilter)
                        }
                    )
                }
            },
            confirmButton = {
                TvFocusButton(
                    text = "Close",
                    onClick = { selectedGameActionMenu = null }
                )
            }
        )
    }
}

@Composable
fun UpNextGameRow(
    game: Game,
    onClick: () -> Unit
) {
    TvFocusCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // League badge & teams
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = game.league,
                    color = TvLimeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Away Team
                Text(
                    text = game.awayTeam.displayName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = " @ ",
                    color = TvTextMuted,
                    fontSize = 12.sp
                )

                // Home Team
                Text(
                    text = game.homeTeam.displayName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status or Time
            Text(
                text = if (game.isFinal) "FINAL: ${game.awayScore} - ${game.homeScore}" else game.statusDetail,
                color = if (game.isFinal) TvTextMuted else TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
