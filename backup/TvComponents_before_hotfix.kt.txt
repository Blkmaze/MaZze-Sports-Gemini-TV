package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ApTeamRank
import com.example.model.Game
import com.example.model.NcaaGame
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    backgroundColor: Color = TvDarkCard,
    focusedBackgroundColor: Color = TvDarkCardElevated,
    borderColor: Color = Color.Transparent,
    focusedBorderColor: Color = TvOrangeFocus,
    focusedScale: Float = 1.04f,
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) focusedScale else 1.0f, label = "card_scale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(if (isFocused) focusedBackgroundColor else backgroundColor)
            .border(
                BorderStroke(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused) focusedBorderColor else borderColor
                ),
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        content(isFocused)
    }
}

@Composable
fun TvFocusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> TvOrangeFocus
        isPrimary -> TvDarkCardElevated
        else -> TvDarkCard
    }
    val contentColor = if (isFocused) Color.Black else TvTextPrimary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                BorderStroke(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color.Transparent),
                RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun TvNavigationRail(
    currentScreen: Int = 0,
    onNavigate: (Int) -> Unit = {},
    selectedScreen: Int = currentScreen,
    onScreenSelected: (Int) -> Unit = onNavigate,
    modifier: Modifier = Modifier
) {
    val effectiveScreen = if (selectedScreen != 0 || currentScreen == 0) selectedScreen else currentScreen
    val navItems = listOf(
        Triple(0, "Home", Icons.Default.Home),
        Triple(1, "Multiview", Icons.Default.GridView),
        Triple(2, "Channels", Icons.Default.Tv),
        Triple(3, "Squares", Icons.Default.Casino),
        Triple(4, "Settings", Icons.Default.Settings)
    )

    Column(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(TvDarkSurface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TvOrangeFocus),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MZ",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        navItems.forEach { (screenIndex, title, icon) ->
            val isSelected = effectiveScreen == screenIndex
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isFocused -> TvOrangeFocus
                            isSelected -> TvDarkCard
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        BorderStroke(
                            2.dp,
                            if (isFocused) Color.White else if (isSelected) TvOrangeFocus else Color.Transparent
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .focusable(interactionSource = interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        onScreenSelected(screenIndex)
                        onNavigate(screenIndex)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = when {
                            isFocused -> Color.Black
                            isSelected -> TvOrangeFocus
                            else -> TvTextSecondary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 9.sp,
                        color = when {
                            isFocused -> Color.Black
                            isSelected -> TvOrangeFocus
                            else -> TvTextSecondary
                        },
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TvLiveRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "LIVE",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TvClock(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }
    Text(
        text = currentTime,
        color = TvTextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
fun GameScoreCard(
    game: Game,
    spoilerFree: Boolean,
    isSpoilerRevealed: Boolean,
    onClick: () -> Unit,
    onToggleSpoiler: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = game.state == "in"
    val isFinal = game.state == "post"
    val hideScores = spoilerFree && !isSpoilerRevealed && (isLive || isFinal)

    TvFocusCard(
        onClick = onClick,
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: League & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.league,
                    color = TvLimeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isLive) {
                    LiveBadge()
                } else {
                    Text(
                        text = if (isFinal) "FINAL" else game.statusDetail,
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Away Team
            TeamScoreRow(
                teamName = game.awayTeam.displayName,
                teamLogo = game.awayTeam.logo,
                score = if (hideScores) "•" else game.awayScore,
                isWinner = isFinal && (game.awayScore.toIntOrNull() ?: 0) > (game.homeScore.toIntOrNull() ?: 0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Home Team
            TeamScoreRow(
                teamName = game.homeTeam.displayName,
                teamLogo = game.homeTeam.logo,
                score = if (hideScores) "•" else game.homeScore,
                isWinner = isFinal && (game.homeScore.toIntOrNull() ?: 0) > (game.awayScore.toIntOrNull() ?: 0)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Network / Match hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.network.isNotBlank()) {
                    Text(
                        text = game.network,
                        color = TvTextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (spoilerFree && (isLive || isFinal)) {
                    Text(
                        text = if (isSpoilerRevealed) "Hide" else "Reveal",
                        color = TvOrangeFocus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleSpoiler() }
                    )
                }
            }
        }
    }
}

fun formatGameSchedule(startTime: String, statusDetail: String): String {
    if (startTime.isNotBlank()) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanStr = if (startTime.length >= 16) startTime.substring(0, 16) else startTime
            val date = isoFormat.parse(cleanStr)
            if (date != null) {
                val displayFormat = SimpleDateFormat("EEE • h:mm a", Locale.getDefault())
                return displayFormat.format(date)
            }
        } catch (e: Throwable) {
            // Ignore
        }
    }
    return statusDetail.ifBlank { "Upcoming" }
}

@Composable
fun HotGameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = game.state.equals("in", ignoreCase = true)
    val scheduleText = formatGameSchedule(game.startTime, game.statusDetail)

    TvFocusCard(
        onClick = onClick,
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header: League & Status / Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (game.network.isNotBlank()) game.league + " · " + game.network else game.league,
                        color = TvLimeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvOrangeFocus.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "HOT",
                            color = TvOrangeFocus,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (isLive) {
                    LiveBadge()
                } else {
                    Text(
                        text = scheduleText,
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Away Team
            TeamScoreRow(
                teamName = game.awayTeam.displayName,
                teamLogo = game.awayTeam.logo,
                score = if (isLive) game.awayScore else ""
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Home Team
            TeamScoreRow(
                teamName = game.homeTeam.displayName,
                teamLogo = game.homeTeam.logo,
                score = if (isLive) game.homeScore else ""
            )
        }
    }
}

@Composable
fun TeamScoreRow(
    teamName: String,
    teamLogo: String,
    score: String,
    isWinner: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (teamLogo.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(teamLogo)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = teamName,
                color = if (isWinner) Color.White else TvTextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = score,
            color = if (isWinner) TvOrangeFocus else TvTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ApTop20Card(
    rankItem: ApTeamRank,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusCard(
        onClick = onClick,
        modifier = modifier.width(190.dp),
        shape = RoundedCornerShape(10.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (rankItem.isSec) TvOrangeFocus else TvDarkCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${rankItem.rank}",
                    color = if (rankItem.isSec) Color.Black else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rankItem.displayName,
                    color = TvTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (rankItem.isSec) {
                    Text(
                        text = "SEC",
                        color = TvLimeAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NcaaGameRow(
    game: NcaaGame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Matchup
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.awayRank != null && game.awayRank <= 25) {
                    Text(
                        text = "#${game.awayRank} ",
                        color = TvOrangeFocus,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = game.awayTeam.displayName,
                    color = TvTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "  @  ",
                    color = TvTextMuted,
                    fontSize = 12.sp
                )
                if (game.homeRank != null && game.homeRank <= 25) {
                    Text(
                        text = "#${game.homeRank} ",
                        color = TvOrangeFocus,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = game.homeTeam.displayName,
                    color = TvTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (game.isSecMatch) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEC",
                        color = TvLimeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Kickoff / Status
            Text(
                text = game.kickoffDisplay,
                color = if (game.state == "in") TvLiveRed else TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Network
            if (game.network.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvDarkCardElevated)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = game.network,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
