package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ApTeamRank
import com.example.model.Game
import com.example.model.NcaaGame
import com.example.model.Team
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    containerColor: Color = TvDarkCard,
    focusedContainerColor: Color = TvDarkCardElevated,
    focusedBorderColor: Color = TvOrangeFocus,
    unfocusedBorderColor: Color = Color.Transparent,
    borderWidth: Dp = 2.5.dp,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        label = "tv_card_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) focusedContainerColor else containerColor,
        label = "tv_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        label = "tv_card_border"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(borderWidth, borderColor, shape)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center
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

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, label = "btn_scale")
    val bgColor = when {
        isFocused -> TvOrangeFocus
        isPrimary -> TvDarkCardElevated
        else -> TvDarkCard
    }
    val contentColor = when {
        isFocused -> Color.Black
        isPrimary -> TvLimeAccent
        else -> TvTextPrimary
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else TvDarkCardElevated,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
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
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TvNavigationRail(
    selectedScreen: Int,
    onScreenSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("Multiview", Icons.Default.GridView, 1),
        Triple("Channels", Icons.Default.LiveTv, 2),
        Triple("Squares", Icons.Default.SportsScore, 3),
        Triple("Settings", Icons.Default.Settings, 4)
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .background(TvDarkSurface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App TV Brand Icon at Top
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(TvOrangeFocus, Color(0xFFE05300))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
            Text(
                text = "MaZze",
                color = TvLimeAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Nav Items
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { (title, icon, index) ->
                val isSelected = selectedScreen == index
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1.0f, label = "rail_scale")
                val itemBg = when {
                    isFocused -> TvOrangeFocus
                    isSelected -> TvDarkCardElevated
                    else -> Color.Transparent
                }
                val iconColor = when {
                    isFocused -> Color.Black
                    isSelected -> TvOrangeFocus
                    else -> TvTextSecondary
                }

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(itemBg)
                        .border(
                            width = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 0.dp,
                            color = if (isFocused) Color.White else if (isSelected) TvOrangeFocus else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .focusable(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onScreenSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = title,
                            color = if (isFocused) Color.Black else if (isSelected) Color.White else TvTextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Live status indicator dot at bottom
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TvLimeAccent)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "LIVE", color = TvLimeAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun GameScoreCard(
    game: Game,
    spoilerFree: Boolean,
    isSpoilerRevealed: Boolean,
    onRevealToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusCard(
        onClick = {
            if (spoilerFree && !isSpoilerRevealed) {
                onRevealToggle()
            } else {
                onClick()
            }
        },
        modifier = modifier
            .width(220.dp)
            .height(130.dp)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: League badge and Game state
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

                if (game.isLive) {
                    LiveBadge()
                } else {
                    Text(
                        text = if (game.isFinal) "FINAL" else game.statusDetail.ifEmpty { "UPCOMING" },
                        color = if (game.isFinal) TvTextMuted else TvTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Teams and Scores
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Away Team
                TeamScoreRow(
                    teamName = game.awayTeam.displayName,
                    teamAbbr = game.awayTeam.abbreviation,
                    logoUrl = game.awayTeam.logo,
                    score = if (spoilerFree && !isSpoilerRevealed && (game.isLive || game.isFinal)) "?" else game.awayScore
                )

                // Home Team
                TeamScoreRow(
                    teamName = game.homeTeam.displayName,
                    teamAbbr = game.homeTeam.abbreviation,
                    logoUrl = game.homeTeam.logo,
                    score = if (spoilerFree && !isSpoilerRevealed && (game.isLive || game.isFinal)) "?" else game.homeScore
                )
            }

            // Bottom detail / Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (game.isLive) game.statusDetail else if (game.isFinal) "Final" else game.statusDetail,
                    color = TvTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (spoilerFree && !isSpoilerRevealed && (game.isLive || game.isFinal)) {
                    Text(
                        text = "OK: Reveal",
                        color = TvOrangeFocus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isFocused) {
                    Text(
                        text = "OK: Watch",
                        color = TvOrangeFocus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TeamScoreRow(
    teamName: String,
    teamAbbr: String,
    logoUrl: String?,
    score: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (!logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = teamAbbr.ifEmpty { teamName },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = score,
            color = if (score == "?") TvOrangeFocus else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun ScoreBarOverlay(
    game: Game?,
    channelTitle: String,
    spoilerFree: Boolean,
    isSpoilerRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    if (game == null) {
        // Fallback channel overlay
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Playing: $channelTitle",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE60D0D10), Color(0x990D0D10), Color.Transparent)
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: League and Matchup
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (game.isLive) {
                    LiveBadge()
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    Text(
                        text = "${game.league} • ${game.name}",
                        color = TvTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Broadcast: $channelTitle  |  ${game.statusDetail}",
                        color = TvLimeAccent,
                        fontSize = 12.sp
                    )
                }
            }

            // Right: Big Score Board
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val awayDisplayScore = if (spoilerFree && !isSpoilerRevealed && (game.isLive || game.isFinal)) "?" else game.awayScore
                val homeDisplayScore = if (spoilerFree && !isSpoilerRevealed && (game.isLive || game.isFinal)) "?" else game.homeScore

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = game.awayTeam.abbreviation,
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = awayDisplayScore,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "-",
                    color = TvTextMuted,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = game.homeTeam.abbreviation,
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = homeDisplayScore,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun TvClock(modifier: Modifier = Modifier) {
    var dateTimeText by remember { mutableStateOf(formatLiveClock()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            dateTimeText = formatLiveClock()
            val millisToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(maxOf(1000L, millisToNextMinute))
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TvDarkCardElevated)
            .border(1.dp, TvDarkSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = TvLimeAccent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = dateTimeText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatLiveClock(): String {
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

@Composable
fun ApTop20Card(
    rankItem: ApTeamRank,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusCard(
        onClick = onClick,
        modifier = modifier
            .width(136.dp)
            .height(58.dp),
        shape = RoundedCornerShape(10.dp),
        containerColor = if (rankItem.isSec) Color(0xFF162016) else TvDarkCard,
        focusedBorderColor = TvOrangeFocus,
        unfocusedBorderColor = if (rankItem.isSec) TvLimeAccent else Color.Transparent,
        borderWidth = if (rankItem.isSec) 1.5.dp else 1.dp
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (rankItem.isSec) TvLimeAccent else TvOrangeFocus)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#${rankItem.rank}",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Logo
                if (!rankItem.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = rankItem.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Team name
                Text(
                    text = rankItem.displayName.ifEmpty { rankItem.name },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (rankItem.isSec) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x33A3E635))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "SEC",
                        color = TvLimeAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
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
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(10.dp),
        containerColor = if (game.isSecMatch) Color(0xFF141914) else TvDarkCard,
        focusedBorderColor = TvOrangeFocus,
        unfocusedBorderColor = if (game.isSecMatch) Color(0x33A3E635) else TvDarkCardElevated,
        borderWidth = if (game.isSecMatch) 1.dp else 1.dp
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Team Matchup with Rank Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.3f)
            ) {
                // Away Team with Rank Badge
                TeamRankDisplay(
                    rank = game.awayRank,
                    team = game.awayTeam
                )

                Text(
                    text = " vs ",
                    color = TvTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Home Team with Rank Badge
                TeamRankDisplay(
                    rank = game.homeRank,
                    team = game.homeTeam
                )
            }

            // Right: Kickoff / Live Score + TV Network + SEC Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kickoff day & time or live score
                Column(horizontalAlignment = Alignment.End) {
                    if (game.isLive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TvLiveRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE: ${game.awayScore} - ${game.homeScore}",
                                color = TvLiveRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = game.statusDetail,
                            color = TvTextSecondary,
                            fontSize = 10.sp
                        )
                    } else if (game.isFinal) {
                        Text(
                            text = "FINAL: ${game.awayScore} - ${game.homeScore}",
                            color = TvTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = game.kickoffDisplay,
                            color = TvTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Network Tag
                if (game.network.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvDarkCardElevated)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = game.network,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lime SEC Tag
                if (game.isSecMatch) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvLimeAccent)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SEC",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Play / Watch prompt on focus
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvOrangeFocus)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OK: Watch",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamRankDisplay(
    rank: Int?,
    team: Team
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (rank != null && rank in 1..20) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(TvOrangeFocus)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "#$rank",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
        }

        if (!team.logo.isNullOrEmpty()) {
            AsyncImage(
                model = team.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }

        Text(
            text = team.displayName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

