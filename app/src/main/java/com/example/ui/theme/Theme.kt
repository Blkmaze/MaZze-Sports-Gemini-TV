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
