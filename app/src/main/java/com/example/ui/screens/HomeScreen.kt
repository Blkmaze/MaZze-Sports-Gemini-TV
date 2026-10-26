package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.api.ChannelMatchingHelper
import com.example.model.Channel
import com.example.model.Game
import com.example.model.NcaaFootballData
import com.example.model.NcaaGame
import com.example.player.TvStreamPlayer
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

data class GameActionMenuData(
    val title: String,
    val candidates: List<Channel>,
    val network: String
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
    isHeroPlayerActive: Boolean = true,
    onSelectGame: (Game) -> Unit = {},
    onSelectNcaaGame: (NcaaGame) -> Unit = {},
    onSelectNcaaTeam: (String) -> Unit = {},
    onToggleSpoiler: (String) -> Unit = {},
    onExpandFullScreen: () -> Unit = {},
    onNextChannel: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onAddToMultiview: (String, List<Channel>) -> Int = { _, _ -> -1 },
    onWatchFullScreenChannel: (Channel) -> Unit = {},
    onFindChannel: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedGameActionMenu by remember { mutableStateOf<GameActionMenuData?>(null) }

    // First-frame gate: never initialize video decoders/PlayerView on the very first frame
    var isFirstFramePassed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        isFirstFramePassed = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Top Header Bar
        item {
            Spacer(modifier = Modifier.height(16.dp))
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
                    Spacer(modifier = Modifier.width(12.dp))
                    if (isOffline) {
                        Text(
                            text = "Offline Mode",
                            color = TvLiveRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Updated: $lastUpdatedTime",
                            color = TvTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TvClock()
                    Spacer(modifier = Modifier.width(16.dp))
                    TvFocusButton(
                        text = "Settings",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Hero Live Preview Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDarkSurface)
            ) {
                val shouldPlayHero = false

                // Safe Poster / Placeholder (video starts only when Fullscreen or Watch Live is selected)
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
                            tint = TvOrangeFocus,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentPlayingChannel?.name ?: "Select a Game or Channel to Preview",
                            color = TvTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Overlay Controls & Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentPlayingChannel?.name ?: "No Channel Loaded",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentPlayingChannel?.groupTitle ?: "Sports Hub",
                            color = TvOrangeFocus,
                            fontSize = 12.sp
                        )
                    }
                    Row {
                        if (currentPlayingChannel != null) {
                            TvFocusButton(
                                text = "Fullscreen",
                                icon = Icons.Default.Fullscreen,
                                onClick = onExpandFullScreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TvFocusButton(
                                text = "Next Channel",
                                icon = Icons.Default.SkipNext,
                                onClick = onNextChannel
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. AP Top 20 Football Rankings Row
        if (ncaaFootballData.apTop20.isNotEmpty()) {
            item {
                Text(
                    text = "AP Top 20 Rankings (SEC & Top Tier)",
                    color = TvTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ncaaFootballData.apTop20) { rankItem ->
                        ApTop20Card(
                            rankItem = rankItem,
                            onClick = { onSelectNcaaTeam(rankItem.displayName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 4. Live & Upcoming Scoreboard Row
        item {
            Text(
                text = "Live & Scheduled Sports",
                color = TvTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvDarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading scoreboard...",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(games) { game ->
                        GameScoreCard(
                            game = game,
                            spoilerFree = spoilerFree,
                            isSpoilerRevealed = revealedGameIds.contains(game.id),
                            onClick = {
                                val candidates = ChannelMatchingHelper.findCandidatesForGame(game, channels)
                                selectedGameActionMenu = GameActionMenuData(
                                    title = "${game.awayTeam.displayName} @ ${game.homeTeam.displayName}",
                                    candidates = candidates,
                                    network = game.network
                                )
                            },
                            onToggleSpoiler = { onToggleSpoiler(game.id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. NCAA Top Matchups List
        if (ncaaFootballData.games.isNotEmpty()) {
            item {
                Text(
                    text = "Featured NCAA Games",
                    color = TvTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            items(ncaaFootballData.games.take(15)) { ncaaGame ->
                NcaaGameRow(
                    game = ncaaGame,
                    onClick = {
                        val candidates = ChannelMatchingHelper.findCandidatesForNcaaGame(ncaaGame, channels)
                        selectedGameActionMenu = GameActionMenuData(
                            title = "${ncaaGame.awayTeam.displayName} @ ${ncaaGame.homeTeam.displayName}",
                            candidates = candidates,
                            network = ncaaGame.network
                        )
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // Game Action Dialog (Fullscreen, Add to Multiview, Search channels)
    selectedGameActionMenu?.let { menuData ->
        Dialog(onDismissRequest = { selectedGameActionMenu = null }) {
            Box(
                modifier = Modifier
                    .width(480.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDarkSurface)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = menuData.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (menuData.network.isNotBlank()) {
                        Text(
                            text = "Broadcast: ${menuData.network}",
                            color = TvLimeAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val topCandidate = menuData.candidates.firstOrNull()
                    if (topCandidate != null) {
                        TvFocusButton(
                            text = "Watch Live (${topCandidate.name})",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            onClick = {
                                selectedGameActionMenu = null
                                onWatchFullScreenChannel(topCandidate)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    TvFocusButton(
                        text = "Add to Multiview",
                        icon = Icons.Default.GridView,
                        onClick = {
                            selectedGameActionMenu = null
                            val slot = onAddToMultiview(menuData.title, menuData.candidates)
                            val msg = if (slot >= 0) "Added to Multiview Tile ${slot + 1}" else "Multiview updated"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (menuData.network.isNotBlank()) {
                        TvFocusButton(
                            text = "Search '${menuData.network}' Channels",
                            icon = Icons.Default.Search,
                            onClick = {
                                selectedGameActionMenu = null
                                onFindChannel(menuData.network)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    TvFocusButton(
                        text = "Close",
                        onClick = { selectedGameActionMenu = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
