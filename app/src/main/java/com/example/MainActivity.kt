package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.TvStreamPlayer
import com.example.ui.components.TvFocusButton
import com.example.ui.components.TvNavigationRail
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ScoreTickerBar
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ScoresBreak
import com.example.ui.theme.ScoresBreakOverlay
import com.example.ui.theme.TvDarkBackground
import com.example.ui.theme.TvDarkSurface
import com.example.ui.theme.TvOrangeFocus
import com.example.ui.theme.TvTextSecondary
import com.example.viewmodel.TvMainViewModel
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

class MainActivity : ComponentActivity() {

    private val viewModel: TvMainViewModel by viewModels()
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            com.example.ui.theme.ScoresBreak.visible = !com.example.ui.theme.ScoresBreak.visible
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Global Uncaught Exception Handler: saves stack trace to filesDir/last_crash.txt
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashFile = File(filesDir, "last_crash.txt")
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                crashFile.writeText("Crash Time: ${Date()}\nThread: ${thread.name}\n\n${sw}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            prevHandler?.uncaughtException(thread, throwable) ?: kotlin.system.exitProcess(1)
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val games by viewModel.games.collectAsStateWithLifecycle()
                val hotThisWeekGames by viewModel.hotThisWeekGames.collectAsStateWithLifecycle()
                val channels by viewModel.channels.collectAsStateWithLifecycle()
                val currentPlayingChannel by viewModel.currentPlayingChannel.collectAsStateWithLifecycle()
                val isFullScreen by viewModel.isFullScreenPlayer.collectAsStateWithLifecycle()
                val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
                val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsStateWithLifecycle()
                val spoilerFree by viewModel.spoilerFree.collectAsStateWithLifecycle()
                val revealedGameIds by viewModel.revealedGameIds.collectAsStateWithLifecycle()

                val multiviewChannels by viewModel.multiviewChannels.collectAsStateWithLifecycle()
                val multiviewGameTitles by viewModel.multiviewGameTitles.collectAsStateWithLifecycle()
                val multiviewSoundIndex by viewModel.multiviewSoundIndex.collectAsStateWithLifecycle()

                val squaresSelectedGame by viewModel.squaresSelectedGame.collectAsStateWithLifecycle()
                val squaresBoard by viewModel.squaresBoard.collectAsStateWithLifecycle()
                val showSquaresQrDialog by viewModel.showSquaresQrDialog.collectAsStateWithLifecycle()
                val squaresPaymentMethod by viewModel.squaresPaymentMethod.collectAsStateWithLifecycle()

                val playlistConfig by viewModel.playlistConfig.collectAsStateWithLifecycle()
                val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
                val ncaaFootballData by viewModel.ncaaFootballData.collectAsStateWithLifecycle()
                val channelSearchFilter by viewModel.channelSearchFilter.collectAsStateWithLifecycle()
                val lastCrashReport by viewModel.lastCrashReport.collectAsStateWithLifecycle()
                var showStartupCrashDialog by remember(lastCrashReport) {
                    mutableStateOf(!lastCrashReport.isNullOrBlank())
                }

                // TV Back navigation handler
                BackHandler(enabled = isFullScreen || currentScreen != 0) {
                    if (isFullScreen) {
                        viewModel.setFullScreenPlayer(false)
                    } else if (currentScreen != 0) {
                        viewModel.setScreen(0)
                    }
                }

                if (isFullScreen) {
                    // Full screen player mode
                    TvStreamPlayer(
                        channel = currentPlayingChannel,
                        isSoundActive = true,
                        isFullScreen = true,
                        onBack = { viewModel.setFullScreenPlayer(false) },
                        onNextChannel = { viewModel.cycleNextChannel() },
                        onPreviousChannel = { viewModel.cyclePreviousChannel() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TvDarkBackground),
                        containerColor = TvDarkBackground,
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Left TV Navigation Rail
                            TvNavigationRail(
                                selectedScreen = currentScreen,
                                onScreenSelected = { viewModel.setScreen(it) }
                            )

                            // Main Content Area
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
 Box(modifier = Modifier.weight(1f).fillMaxWidth().background(TvDarkBackground)) {
                                when (currentScreen) {
                                    0 -> HomeScreen(
                                        games = games,
                                        hotThisWeekGames = hotThisWeekGames,
                                        channels = channels,
                                        ncaaFootballData = ncaaFootballData,
                                        currentPlayingChannel = currentPlayingChannel,
                                        isOffline = isOffline,
                                        lastUpdatedTime = lastUpdatedTime,
                                        spoilerFree = spoilerFree,
                                        revealedGameIds = revealedGameIds,
                                        onSelectGame = { game ->
                                            viewModel.selectGameForSquares(game)
                                        },
                                        onSelectNcaaGame = { ncaaGame ->
                                        },
                                        onSelectNcaaTeam = { teamName ->
                                            viewModel.openChannelsWithFilter(teamName)
                                        },
                                        onToggleSpoiler = { viewModel.toggleSpoilerReveal(it) },
                                        onExpandFullScreen = { viewModel.setFullScreenPlayer(true) },
                                        onNextChannel = { viewModel.cycleNextChannel() },
                                        onPreviousChannel = { viewModel.cyclePreviousChannel() },
                                        onNavigateToSettings = { viewModel.setScreen(4) },
                                        onAddToMultiview = { title, candidates ->
                                            viewModel.addGameToMultiview(title, candidates)
                                        },
                                        onWatchFullScreenChannel = { ch ->
                                            viewModel.setPlayingChannel(ch)
                                            viewModel.setFullScreenPlayer(true)
                                        },
                                        onFindChannel = { query ->
                                            viewModel.openChannelsWithFilter(query)
                                        },
                                        isHeroPlayerActive = (currentScreen == 0 && !isFullScreen)
                                    )

                                    1 -> MultiviewScreen(
                                        channels = channels,
                                        multiviewChannels = multiviewChannels,
                                        multiviewGameTitles = multiviewGameTitles,
                                        activeSoundIndex = multiviewSoundIndex,
                                        onSetSoundIndex = { viewModel.setMultiviewSound(it) },
                                        onSlotChannelChange = { slot, ch -> viewModel.setMultiviewSlotChannel(slot, ch) },
                                        onAutoFillTop20 = { viewModel.autoFillMultiviewTop20(forceAll = true) },
                                        onExpandTile = { ch ->
                                            viewModel.setPlayingChannel(ch)
                                            viewModel.setFullScreenPlayer(true)
                                        },
                                        onCycleTileBackup = { viewModel.cycleMultiviewTileToNextCandidate(it) }
                                    )

                                    2 -> ChannelsScreen(
                                        channels = channels,
                                        selectedChannel = currentPlayingChannel,
                                        searchQuery = channelSearchFilter,
                                        onClearSearch = { viewModel.setChannelSearchFilter("") },
                                        onChannelSelected = { viewModel.setPlayingChannel(it) },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onExpandFullScreen = { ch ->
                                            viewModel.setPlayingChannel(ch)
                                            viewModel.setFullScreenPlayer(true)
                                        },
                                        onNavigateToSettings = { viewModel.setScreen(4) }
                                    )

                                    3 -> SquaresScreen(
                                        games = games,
                                        selectedGame = squaresSelectedGame,
                                        squaresBoard = squaresBoard,
                                        showPaymentDialog = showSquaresQrDialog,
                                        paymentMethod = squaresPaymentMethod,
                                        onSelectGame = { viewModel.selectGameForSquares(it) },
                                        onSelectPrice = { viewModel.setSquaresPrice(it) },
                                        onToggleSquare = { r, c -> viewModel.toggleSquareSelection(r, c) },
                                        onOpenPayment = { viewModel.openPaymentQr(it) },
                                        onClosePayment = { viewModel.closePaymentQr() },
                                        onOpenBrowser = { viewModel.openBrowserLink(it) }
                                    )

                                    4 -> SettingsScreen(
                                        playlistConfig = playlistConfig,
                                        updateInfo = updateInfo,
                                        spoilerFree = spoilerFree,
                                        lastCrashReport = lastCrashReport,
                                        onClearCrashReport = { viewModel.clearCrashReport() },
                                        onSaveConfig = { viewModel.updatePlaylistConfig(it) },
                                        onLoadDemo = { viewModel.loadDemoChannels() },
                                        onToggleSpoilerFree = { viewModel.toggleSpoilerFreeMode() },
                                        onCheckForUpdates = { viewModel.checkForUpdates() },
                                        onDownloadAndInstall = { viewModel.downloadAndInstallUpdate() }
                                    )
                                }
                            }
 ScoreTickerBar(games = games, spoilerFree = spoilerFree)
 }
                        }
                    }
                }
                com.example.ui.theme.ReminderWatcher(games = games)
                if (ScoresBreak.visible) {
                    Dialog(
                        onDismissRequest = { ScoresBreak.visible = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        ScoresBreakOverlay(games = games, spoilerFree = spoilerFree)
                    }
                }


                // Startup crash report dialog if previous crash was captured
                if (showStartupCrashDialog && !lastCrashReport.isNullOrBlank()) {
                    Dialog(onDismissRequest = { showStartupCrashDialog = false }) {
                        Box(
                            modifier = Modifier
                                .width(600.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(TvDarkSurface)
                                .border(2.dp, TvOrangeFocus, RoundedCornerShape(16.dp))
                                .padding(24.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Previous App Crash Detected",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A crash stack trace was captured from a previous session:",
                                    color = TvTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val scrollState = rememberScrollState()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TvDarkBackground)
                                        .padding(12.dp)
                                        .verticalScroll(scrollState)
                                ) {
                                    Text(
                                        text = lastCrashReport ?: "",
                                        color = Color(0xFFFFB4AB),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TvFocusButton(
                                        text = "Close",
                                        icon = Icons.Default.Close,
                                        onClick = { showStartupCrashDialog = false }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TvFocusButton(
                                        text = "Clear",
                                        icon = Icons.Default.Delete,
                                        isPrimary = true,
                                        onClick = {
                                            viewModel.clearCrashReport()
                                            showStartupCrashDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Activity stopped: all active players are released via lifecycle observers
    }
}
