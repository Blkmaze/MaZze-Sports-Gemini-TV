package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.TvStreamPlayer
import com.example.ui.components.TvNavigationRail
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvDarkBackground
import com.example.viewmodel.TvMainViewModel
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

class MainActivity : ComponentActivity() {

    private val viewModel: TvMainViewModel by viewModels()

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
                    val channelIndex = channels.indexOfFirst { it.id == currentPlayingChannel?.id }
                    TvStreamPlayer(
                        channel = currentPlayingChannel,
                        isSoundActive = true,
                        isFullScreen = true,
                        onBack = { viewModel.setFullScreenPlayer(false) },
                        onNextChannel = {
                            if (channels.isNotEmpty()) {
                                val nextIdx = if (channelIndex >= 0) (channelIndex + 1) % channels.size else 0
                                channels.getOrNull(nextIdx)?.let { viewModel.setPlayingChannel(it) }
                            }
                        },
                        onPreviousChannel = {
                            if (channels.isNotEmpty()) {
                                val prevIdx = if (channelIndex > 0) channelIndex - 1 else channels.size - 1
                                channels.getOrNull(prevIdx)?.let { viewModel.setPlayingChannel(it) }
                            }
                        },
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
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(TvDarkBackground)
                            ) {
                                when (currentScreen) {
                                    0 -> HomeScreen(
                                        games = games,
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
                                        onNextChannel = {
                                            if (channels.isNotEmpty()) {
                                                val currIdx = channels.indexOfFirst { it.id == currentPlayingChannel?.id }
                                                val nextIdx = if (currIdx >= 0) (currIdx + 1) % channels.size else 0
                                                channels.getOrNull(nextIdx)?.let { viewModel.setPlayingChannel(it) }
                                            }
                                        },
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
