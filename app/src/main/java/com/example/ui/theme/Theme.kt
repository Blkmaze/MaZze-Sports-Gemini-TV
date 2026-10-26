package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Game
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

private val TvColorScheme = darkColorScheme(
    primary = TvOrangeFocus,
    onPrimary = Color.Black,
    secondary = TvLimeAccent,
    onSecondary = Color.Black,
    tertiary = TvLimeAccent,
    background = TvDarkBackground,
    onBackground = TvTextPrimary,
    surface = TvDarkSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvDarkCard,
    onSurfaceVariant = TvTextSecondary
)

/** Patio Mode: bigger text for viewing from across the patio. Saved on the device. */
object PatioMode {
    var enabled by mutableStateOf(false)
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        try {
            enabled = context.getSharedPreferences("mazze_ui", Context.MODE_PRIVATE)
                .getBoolean("patio_mode", false)
        } catch (e: Throwable) {
        }
    }

    fun toggle(context: Context) {
        enabled = !enabled
        try {
            context.getSharedPreferences("mazze_ui", Context.MODE_PRIVATE)
                .edit().putBoolean("patio_mode", enabled).apply()
        } catch (e: Throwable) {
        }
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    PatioMode.load(context)
    val base = LocalDensity.current
    val scale = if (PatioMode.enabled) 1.3f else 1f
    CompositionLocalProvider(
        LocalDensity provides Density(base.density, base.fontScale * scale)
    ) {
        MaterialTheme(
            colorScheme = TvColorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun buildTickerItems(games: List<Game>, spoilerFree: Boolean): List<String> {
    fun line(g: Game): String {
        val away = g.awayTeam.abbreviation.ifBlank { g.awayTeam.displayName }
        val home = g.homeTeam.abbreviation.ifBlank { g.homeTeam.displayName }
        return when (g.state) {
            "in" -> if (spoilerFree) "LIVE  $away @ $home" else "LIVE  $away ${g.awayScore} - $home ${g.homeScore}  ${g.statusDetail}"
            "post" -> if (spoilerFree) "FINAL  $away @ $home" else "FINAL  $away ${g.awayScore} - $home ${g.homeScore}"
            else -> "$away @ $home  ${g.statusDetail}"
        }
    }
    val live = games.filter { it.state == "in" }
    val upcoming = games.filter { it.state == "pre" }.take(12)
    val finals = games.filter { it.state == "post" }.take(12)
    return (live + upcoming + finals).map { line(it) }
}

/** Scrolling live-score ticker for the bottom of the screen. Not focusable, so it never traps the remote. */
@Composable
fun ScoreTickerBar(games: List<Game>, spoilerFree: Boolean, modifier: Modifier = Modifier) {
    val items = remember(games, spoilerFree) { buildTickerItems(games, spoilerFree) }
    if (items.isEmpty()) return
    val listState = rememberLazyListState()
    LaunchedEffect(items.size) {
        while (true) {
            listState.scrollBy(2f)
            delay(30)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(TvDarkSurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(TvLiveRed)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "SCORES", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        LazyRow(
            state = listState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(Int.MAX_VALUE) { i ->
                Text(
                    text = items[i % items.size],
                    color = TvTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

/** Scores Break: full-screen scoreboard shown on top of the video (press Menu on the remote). */
object ScoresBreak {
    var visible by mutableStateOf(false)
}

@Composable
fun ScoresBreakOverlay(games: List<Game>, spoilerFree: Boolean) {
    val live = games.filter { it.state == "in" }
    val upcoming = games.filter { it.state == "pre" }
    val finals = games.filter { it.state == "post" }
    val ordered = (live + upcoming + finals).take(30)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2050B18))
            .padding(32.dp)
    ) {
        Text(
            text = "SCORES BREAK",
            color = TvOrangeFocus,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Press Back to return to the video",
            color = TvTextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (ordered.isEmpty()) {
            Text(text = "No games available right now", color = TvTextPrimary, fontSize = 18.sp)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ordered) { g -> ScoresBreakCard(g, spoilerFree) }
            }
        }
    }
}

@Composable
private fun ScoresBreakCard(g: Game, spoilerFree: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val context = LocalContext.current
    Reminders.load(context)
    val isLive = g.state == "in"
    val showScore = !spoilerFree && g.state != "pre"
    val away = g.awayTeam.abbreviation.ifBlank { g.awayTeam.displayName }
    val home = g.homeTeam.abbreviation.ifBlank { g.homeTeam.displayName }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) TvDarkCardElevated else TvDarkCard)
            .border(2.dp, if (focused) TvOrangeFocus else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null) {
                if (g.state == "pre") Reminders.toggle(context, g.id)
            }
            .focusable(interactionSource = interaction)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = g.league.uppercase(),
                color = TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isLive) {
                Text(text = "LIVE", color = TvLiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = away, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = if (showScore) g.awayScore else "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = home, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = if (showScore) g.homeScore else "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = g.statusDetail,
            color = if (isLive) TvLimeAccent else TvTextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (g.state == "pre") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (Reminders.has(g.id)) "REMINDER ON (15 min before)" else "Press OK for a reminder",
                color = if (Reminders.has(g.id)) TvOrangeFocus else TvTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Game reminders: saved game ids; a toast pops up about 15 minutes before kickoff while the app is open. */
object Reminders {
    val ids = androidx.compose.runtime.mutableStateListOf<String>()
    private var loaded = false
    val notified = mutableSetOf<String>()

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        try {
            val saved = context.getSharedPreferences("mazze_ui", Context.MODE_PRIVATE)
                .getStringSet("reminder_ids", emptySet()) ?: emptySet()
            ids.addAll(saved)
        } catch (e: Throwable) {
        }
    }

    fun has(id: String): Boolean = ids.contains(id)

    fun toggle(context: Context, id: String) {
        if (ids.contains(id)) ids.remove(id) else ids.add(id)
        try {
            context.getSharedPreferences("mazze_ui", Context.MODE_PRIVATE)
                .edit().putStringSet("reminder_ids", ids.toSet()).apply()
        } catch (e: Throwable) {
        }
    }
}

private fun minutesUntilStart(startTime: String): Long? {
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val start = fmt.parse(startTime.take(16)) ?: return null
        (start.time - System.currentTimeMillis()) / 60000L
    } catch (e: Throwable) {
        null
    }
}

/** Put this once near the top of the screen. It checks reminders every 30 seconds. */
@Composable
fun ReminderWatcher(games: List<Game>) {
    val context = LocalContext.current
    LaunchedEffect(games) {
        Reminders.load(context)
        while (true) {
            for (g in games) {
                if (g.state != "pre" || !Reminders.has(g.id) || Reminders.notified.contains(g.id)) continue
                val mins = minutesUntilStart(g.startTime) ?: continue
                if (mins in -5..15) {
                    Reminders.notified.add(g.id)
                    val away = g.awayTeam.abbreviation.ifBlank { g.awayTeam.displayName }
                    val home = g.homeTeam.abbreviation.ifBlank { g.homeTeam.displayName }
                    val msg = "REMINDER: " + away + " @ " + home + (if (mins > 0) " starts in " + mins + " min" else " is starting now") + (if (g.network.isNotBlank()) " on " + g.network else "")
                    try {
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    } catch (e: Throwable) {
                    }
                }
            }
            delay(30000)
        }
    }
}
