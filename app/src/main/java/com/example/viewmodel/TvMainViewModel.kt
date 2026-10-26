package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ChannelMatchingHelper
import com.example.api.EspnScoreboardRepository
import com.example.api.PlaylistRepository
import com.example.api.UpdaterRepository
import com.example.model.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TvMainViewModel(application: Application) : AndroidViewModel(application) {

    private val espnRepo = EspnScoreboardRepository()
    private val playlistRepo = PlaylistRepository(application)
    private val updaterRepo = UpdaterRepository(application)

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        // Safe swallow: prevent unhandled coroutine failure from killing the app process
    }

    private val _currentScreen = MutableStateFlow(0)
    val currentScreen: StateFlow<Int> = _currentScreen.asStateFlow()

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _currentPlayingChannel = MutableStateFlow<Channel?>(null)
    val currentPlayingChannel: StateFlow<Channel?> = _currentPlayingChannel.asStateFlow()

    private val _isFullScreenPlayer = MutableStateFlow(false)
    val isFullScreenPlayer: StateFlow<Boolean> = _isFullScreenPlayer.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow("Updating...")
    val lastUpdatedTime: StateFlow<String> = _lastUpdatedTime.asStateFlow()

    private val _spoilerFree = MutableStateFlow(false)
    val spoilerFree: StateFlow<Boolean> = _spoilerFree.asStateFlow()

    private val _revealedGameIds = MutableStateFlow<Set<String>>(emptySet())
    val revealedGameIds: StateFlow<Set<String>> = _revealedGameIds.asStateFlow()

    private val _multiviewChannels = MutableStateFlow<List<Channel?>>(listOf(null, null, null, null))
    val multiviewChannels: StateFlow<List<Channel?>> = _multiviewChannels.asStateFlow()

    private val _multiviewGameTitles = MutableStateFlow<List<String?>>(listOf(null, null, null, null))
    val multiviewGameTitles: StateFlow<List<String?>> = _multiviewGameTitles.asStateFlow()

    private val _multiviewCandidates = MutableStateFlow<List<List<Channel>>>(
        listOf(emptyList(), emptyList(), emptyList(), emptyList())
    )
    val multiviewCandidates: StateFlow<List<List<Channel>>> = _multiviewCandidates.asStateFlow()

    private val manualTileIndices = mutableSetOf<Int>()
    private val tileEvictionQueue = mutableListOf(0, 1, 2, 3)

    private val _multiviewSoundIndex = MutableStateFlow(0)
    val multiviewSoundIndex: StateFlow<Int> = _multiviewSoundIndex.asStateFlow()

    private val _squaresSelectedGame = MutableStateFlow<Game?>(null)
    val squaresSelectedGame: StateFlow<Game?> = _squaresSelectedGame.asStateFlow()

    private val _squaresBoard = MutableStateFlow(createInitialSquaresBoard(""))
    val squaresBoard: StateFlow<SquaresBoard> = _squaresBoard.asStateFlow()

    private val _showSquaresQrDialog = MutableStateFlow(false)
    val showSquaresQrDialog: StateFlow<Boolean> = _showSquaresQrDialog.asStateFlow()

    private val _squaresPaymentMethod = MutableStateFlow("Cash App")
    val squaresPaymentMethod: StateFlow<String> = _squaresPaymentMethod.asStateFlow()

    private val _playlistConfig = MutableStateFlow(playlistRepo.loadConfig())
    val playlistConfig: StateFlow<PlaylistConfig> = _playlistConfig.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    private val _ncaaFootballData = MutableStateFlow(NcaaFootballData())
    val ncaaFootballData: StateFlow<NcaaFootballData> = _ncaaFootballData.asStateFlow()

    private val _channelSearchFilter = MutableStateFlow("")
    val channelSearchFilter: StateFlow<String> = _channelSearchFilter.asStateFlow()

    private val _lastCrashReport = MutableStateFlow<String?>(null)
    val lastCrashReport: StateFlow<String?> = _lastCrashReport.asStateFlow()

    private var pollJob: Job? = null
    private var ncaaPollJob: Job? = null
    private val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    init {
        try {
            _spoilerFree.value = _playlistConfig.value.spoilerFree
        } catch (e: Throwable) {}

        loadCrashReport()
        loadChannels()
        startScoreboardPolling()
        startNcaaPolling()
    }

    fun loadCrashReport() {
        try {
            val crashFile = java.io.File(getApplication<Application>().filesDir, "last_crash.txt")
            _lastCrashReport.value = if (crashFile.exists()) crashFile.readText() else null
        } catch (e: Throwable) {
            _lastCrashReport.value = null
        }
    }

    fun clearCrashReport() {
        try {
            val crashFile = java.io.File(getApplication<Application>().filesDir, "last_crash.txt")
            if (crashFile.exists()) {
                crashFile.delete()
            }
            _lastCrashReport.value = null
        } catch (e: Throwable) {
            _lastCrashReport.value = null
        }
    }

    fun setScreen(screenIndex: Int) {
        _currentScreen.value = screenIndex
        _isFullScreenPlayer.value = false
    }

    fun setChannelSearchFilter(query: String) {
        _channelSearchFilter.value = query
    }

    fun openChannelsWithFilter(query: String) {
        _channelSearchFilter.value = query
        setScreen(2) // Navigate to Channels
    }

    fun setFullScreenPlayer(fullScreen: Boolean) {
        _isFullScreenPlayer.value = fullScreen
    }

    fun setPlayingChannel(channel: Channel?) {
        _currentPlayingChannel.value = channel
    }

    fun toggleSpoilerReveal(gameId: String) {
        val current = _revealedGameIds.value.toMutableSet()
        if (current.contains(gameId)) {
            current.remove(gameId)
        } else {
            current.add(gameId)
        }
        _revealedGameIds.value = current
    }

    fun toggleSpoilerFreeMode() {
        val newVal = playlistRepo.toggleSpoilerFree(_spoilerFree.value)
        _spoilerFree.value = newVal
    }

    fun toggleFavorite(channelId: String) {
        val isFav = playlistRepo.toggleFavorite(channelId)
        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(isFavorite = isFav) else it
        }
    }

    fun loadChannels() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val list = playlistRepo.loadChannels()
                _channels.value = list
                if (_currentPlayingChannel.value == null) {
                    autoPickHeroChannel()
                }
                autoFillMultiviewTop20(forceAll = false)
            } catch (e: Throwable) {
                // Keep existing channels if reload failed
            }
        }
    }

    fun updatePlaylistConfig(config: PlaylistConfig) {
        playlistRepo.saveConfig(config)
        _playlistConfig.value = config
        loadChannels()
    }

    fun loadDemoChannels() {
        viewModelScope.launch(exceptionHandler) {
            val config = PlaylistConfig(type = "demo")
            playlistRepo.saveConfig(config)
            _playlistConfig.value = config
            val list = playlistRepo.getCuratedDemoSportsChannels()
            _channels.value = list
            _currentPlayingChannel.value = list.firstOrNull()
            autoFillMultiviewTop20(forceAll = true)
        }
    }

    fun setMultiviewSound(tileIndex: Int) {
        if (tileIndex in 0..3) {
            _multiviewSoundIndex.value = tileIndex
        }
    }

    fun setMultiviewSlotChannel(slotIndex: Int, channel: Channel) {
        if (slotIndex in 0..3) {
            val list = _multiviewChannels.value.toMutableList()
            while (list.size < 4) list.add(null)
            list[slotIndex] = channel
            _multiviewChannels.value = list

            val titles = _multiviewGameTitles.value.toMutableList()
            while (titles.size < 4) titles.add(null)
            titles[slotIndex] = channel.name
            _multiviewGameTitles.value = titles

            manualTileIndices.add(slotIndex)
            tileEvictionQueue.remove(slotIndex)
            tileEvictionQueue.add(slotIndex)
        }
    }

    fun addGameToMultiview(gameTitle: String, candidateChannels: List<Channel>): Int {
        val targetSlot = tileEvictionQueue.firstOrNull { it !in manualTileIndices }
            ?: tileEvictionQueue.firstOrNull()
            ?: 0

        val list = _multiviewChannels.value.toMutableList()
        while (list.size < 4) list.add(null)
        list[targetSlot] = candidateChannels.firstOrNull()
        _multiviewChannels.value = list

        val titles = _multiviewGameTitles.value.toMutableList()
        while (titles.size < 4) titles.add(null)
        titles[targetSlot] = gameTitle
        _multiviewGameTitles.value = titles

        val candList = _multiviewCandidates.value.toMutableList()
        while (candList.size < 4) candList.add(emptyList())
        candList[targetSlot] = candidateChannels
        _multiviewCandidates.value = candList

        manualTileIndices.add(targetSlot)
        tileEvictionQueue.remove(targetSlot)
        tileEvictionQueue.add(targetSlot)

        setScreen(1) // Multiview
        return targetSlot
    }

    fun cycleMultiviewTileToNextCandidate(tileIndex: Int) {
        if (tileIndex !in 0..3) return
        val candidates = _multiviewCandidates.value.getOrNull(tileIndex) ?: return
        if (candidates.size <= 1) return

        val current = _multiviewChannels.value.getOrNull(tileIndex)
        val currentIndex = candidates.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % candidates.size else 0

        val nextChannel = candidates.getOrNull(nextIndex) ?: return
        val list = _multiviewChannels.value.toMutableList()
        list[tileIndex] = nextChannel
        _multiviewChannels.value = list
    }

    fun autoFillMultiviewTop20(forceAll: Boolean = false) {
        viewModelScope.launch(exceptionHandler) {
            val allChannels = _channels.value
            if (allChannels.isEmpty()) return@launch

            val topGames = _ncaaFootballData.value.games
            val currentMultiview = _multiviewChannels.value.toMutableList()
            while (currentMultiview.size < 4) currentMultiview.add(null)

            val currentTitles = _multiviewGameTitles.value.toMutableList()
            while (currentTitles.size < 4) currentTitles.add(null)

            val currentCandidates = _multiviewCandidates.value.toMutableList()
            while (currentCandidates.size < 4) currentCandidates.add(emptyList())

            val usedChannelIds = mutableSetOf<String>()

            withContext(Dispatchers.Default) {
            for (i in 0 until 4) {
                if (forceAll || currentMultiview.getOrNull(i) == null) {
                    val candidateGame = topGames.getOrNull(i)
                    if (candidateGame != null) {
                        val candidates = ChannelMatchingHelper.findCandidatesForNcaaGame(
                            candidateGame, allChannels, usedChannelIds
                        )
                        val topMatch = candidates.firstOrNull()
                        if (topMatch != null) {
                            usedChannelIds.add(topMatch.id)
                            currentMultiview[i] = topMatch
                            currentTitles[i] = "${candidateGame.awayTeam.displayName} @ ${candidateGame.homeTeam.displayName}"
                            currentCandidates[i] = candidates
                        }
                    } else {
                        val fallback = allChannels.firstOrNull { it.id !in usedChannelIds }
                        if (fallback != null) {
                            usedChannelIds.add(fallback.id)
                            currentMultiview[i] = fallback
                            currentTitles[i] = fallback.name
                            currentCandidates[i] = listOf(fallback)
                        }
                    }
                }
            }
            }

            _multiviewChannels.value = currentMultiview
            _multiviewGameTitles.value = currentTitles
            _multiviewCandidates.value = currentCandidates
        }
    }

    private fun startScoreboardPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(exceptionHandler) {
            while (true) {
                fetchScoreboards()
                delay(30000)
            }
        }
    }

    private fun startNcaaPolling() {
        ncaaPollJob?.cancel()
        ncaaPollJob = viewModelScope.launch(exceptionHandler) {
            while (true) {
                fetchNcaaFootball()
                delay(60000)
            }
        }
    }

    fun refreshScoreboardsManual() {
        viewModelScope.launch(exceptionHandler) {
            fetchScoreboards()
            fetchNcaaFootball()
        }
    }

    private suspend fun fetchScoreboards() {
        try {
            val result = espnRepo.fetchAllScoreboards()
            result.onSuccess { gameList ->
                _games.value = gameList
                _isOffline.value = false
                _lastUpdatedTime.value = timeFormat.format(Date())
                if (_currentPlayingChannel.value == null) {
                    autoPickHeroChannel()
                }
            }.onFailure {
                _isOffline.value = true
            }
        } catch (e: Throwable) {
            _isOffline.value = true
        }
    }

    private suspend fun fetchNcaaFootball() {
        try {
            val result = espnRepo.fetchNcaaFootballAndRankings()
            result.onSuccess { data ->
                _ncaaFootballData.value = data
                if (_currentPlayingChannel.value == null) {
                    autoPickHeroChannel()
                }
            }
        } catch (e: Throwable) {}
    }

    private fun getMarqueeScore(game: Game): Int {
        val nameLower = game.name.lowercase(Locale.ROOT)
        val leagueLower = game.league.lowercase(Locale.ROOT)
        val shortNameLower = game.shortName.lowercase(Locale.ROOT)
        val combined = "$nameLower $leagueLower $shortNameLower"

        return when {
            combined.contains("monday night football") -> 1100
            leagueLower == "nfl" -> 1000
            combined.contains("world series") -> 950
            leagueLower == "mlb" -> 900
            combined.contains("wnba finals") -> 850
            leagueLower == "wnba" -> 800
            leagueLower == "cfb" || leagueLower.contains("college-football") || leagueLower.contains("cfb") -> 700
            leagueLower == "nba" -> 600
            leagueLower == "nhl" -> 500
            else -> 100
        }
    }

    fun getLiveGameChannels(channels: List<Channel>): List<Channel> {
        val result = mutableListOf<Channel>()
        val seenIds = mutableSetOf<String>()

        val liveStandardGames = _games.value
            .filter { it.state.equals("in", ignoreCase = true) }
            .sortedByDescending { getMarqueeScore(it) }

        for (game in liveStandardGames) {
            val candidates = ChannelMatchingHelper.findCandidatesForGame(game, channels, seenIds)
            for (ch in candidates) {
                if (seenIds.add(ch.id)) {
                    result.add(ch)
                }
            }
        }

        val liveNcaaGames = _ncaaFootballData.value.games
            .filter { it.state.equals("in", ignoreCase = true) }

        for (ncaaGame in liveNcaaGames) {
            val candidates = ChannelMatchingHelper.findCandidatesForNcaaGame(ncaaGame, channels, seenIds)
            for (ch in candidates) {
                if (seenIds.add(ch.id)) {
                    result.add(ch)
                }
            }
        }

        return result
    }

    private fun findFallbackSportsChannel(channels: List<Channel>): Channel? {
        if (channels.isEmpty()) return null

        // 1. ESPN (main, excluding ESPN2, ESPNews, ESPNU)
        val espn = channels.firstOrNull { ch ->
            val n = ch.name.lowercase(Locale.ROOT)
            n.contains("espn") && !n.contains("espn2") && !n.contains("espn 2") &&
                    !n.contains("espnews") && !n.contains("espn news") &&
                    !n.contains("espnu") && !n.contains("espn u") && !n.contains("espn+")
        }
        if (espn != null) return espn

        // 2. ESPN2
        val espn2 = channels.firstOrNull { ch ->
            val n = ch.name.lowercase(Locale.ROOT)
            n.contains("espn2") || n.contains("espn 2")
        }
        if (espn2 != null) return espn2

        // 3. ESPNews
        val espnews = channels.firstOrNull { ch ->
            val n = ch.name.lowercase(Locale.ROOT)
            n.contains("espnews") || n.contains("espn news")
        }
        if (espnews != null) return espnews

        // 4. ESPNU
        val espnu = channels.firstOrNull { ch ->
            val n = ch.name.lowercase(Locale.ROOT)
            n.contains("espnu") || n.contains("espn u")
        }
        if (espnu != null) return espnu

        // 5. Any ESPN channel
        val anyEspn = channels.firstOrNull { ch ->
            ch.name.lowercase(Locale.ROOT).contains("espn")
        }
        if (anyEspn != null) return anyEspn

        // 6. Other major sports networks
        val otherSports = listOf("fs1", "fox sports 1", "sec network", "nfl network", "mlb network", "nba tv", "nhl network", "tnt", "tbs")
        for (pattern in otherSports) {
            val found = channels.firstOrNull { it.name.lowercase(Locale.ROOT).contains(pattern) }
            if (found != null) return found
        }

        return channels.firstOrNull { it.isSports } ?: channels.firstOrNull()
    }

    fun autoPickHeroChannel(force: Boolean = false) {
        val currentChannels = _channels.value
        if (currentChannels.isEmpty()) return
        if (!force && _currentPlayingChannel.value != null) return

        val liveChannels = getLiveGameChannels(currentChannels)
        val picked = liveChannels.firstOrNull() ?: findFallbackSportsChannel(currentChannels)
        if (picked != null) {
            _currentPlayingChannel.value = picked
        }
    }

    fun getCuratedChannelList(): List<Channel> {
        val allChannels = _channels.value
        if (allChannels.isEmpty()) return emptyList()

        val result = mutableListOf<Channel>()
        val seenIds = mutableSetOf<String>()

        // 1. Live game channels first (ordered by marquee event / live status)
        val liveChannels = getLiveGameChannels(allChannels)
        for (ch in liveChannels) {
            if (seenIds.add(ch.id)) {
                result.add(ch)
            }
        }

        // 2. ESPN / ESPNews style channels
        val networkPriorities = listOf(
            listOf("espn2", "espn 2"),
            listOf("espnews", "espn news"),
            listOf("espnu", "espn u"),
            listOf("sec network", "secn"),
            listOf("acc network", "accn"),
            listOf("fs1", "fox sports 1"),
            listOf("fs2", "fox sports 2"),
            listOf("mlb network"),
            listOf("nfl network"),
            listOf("nba tv"),
            listOf("nhl network"),
            listOf("tnt"),
            listOf("tbs"),
            listOf("cbs sports", "cbssn"),
            listOf("nbc sports")
        )

        val espnMain = allChannels.filter { ch ->
            val n = ch.name.lowercase(Locale.ROOT)
            n.contains("espn") && !n.contains("espn2") && !n.contains("espn 2") &&
                    !n.contains("espnews") && !n.contains("espn news") &&
                    !n.contains("espnu") && !n.contains("espn u") && !n.contains("espn+")
        }
        for (ch in espnMain) {
            if (seenIds.add(ch.id)) result.add(ch)
        }

        for (patterns in networkPriorities) {
            val matches = allChannels.filter { ch ->
                val n = ch.name.lowercase(Locale.ROOT)
                patterns.any { n.contains(it) }
            }
            for (ch in matches) {
                if (seenIds.add(ch.id)) result.add(ch)
            }
        }

        for (ch in allChannels) {
            if (ch.isSports && seenIds.add(ch.id)) {
                result.add(ch)
            }
        }

        return result.ifEmpty { allChannels }
    }

    fun cycleNextChannel() {
        val pool = getCuratedChannelList()
        if (pool.isEmpty()) return
        val current = _currentPlayingChannel.value
        val currIdx = pool.indexOfFirst { it.id == current?.id }
        val nextIdx = if (currIdx >= 0) (currIdx + 1) % pool.size else 0
        _currentPlayingChannel.value = pool[nextIdx]
    }

    fun cyclePreviousChannel() {
        val pool = getCuratedChannelList()
        if (pool.isEmpty()) return
        val current = _currentPlayingChannel.value
        val currIdx = pool.indexOfFirst { it.id == current?.id }
        val prevIdx = if (currIdx > 0) currIdx - 1 else pool.size - 1
        _currentPlayingChannel.value = pool[prevIdx]
    }

    fun selectGameForSquares(game: Game) {
        _squaresSelectedGame.value = game
        _squaresBoard.value = createInitialSquaresBoard(game.id)
    }

    fun setSquaresPrice(price: Int) {
        _squaresBoard.value = _squaresBoard.value.copy(pricePerSquare = price)
    }

    fun toggleSquareSelection(row: Int, col: Int) {
        if (row !in 0..9 || col !in 0..9) return
        val currentCells = _squaresBoard.value.cells.toMutableList()
        val index = row * 10 + col
        val cell = currentCells.getOrNull(index) ?: return

        val updatedCell = if (cell.isUserSquare) {
            cell.copy(isUserSquare = false, ownerName = "")
        } else {
            cell.copy(isUserSquare = true, ownerName = "Me")
        }
        currentCells[index] = updatedCell
        _squaresBoard.value = _squaresBoard.value.copy(cells = currentCells)
    }

    fun openPaymentQr(method: String) {
        _squaresPaymentMethod.value = method
        _showSquaresQrDialog.value = true
    }

    fun closePaymentQr() {
        _showSquaresQrDialog.value = false
    }

    fun openBrowserLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Throwable) {}
    }

    private fun createInitialSquaresBoard(gameId: String): SquaresBoard {
        val homeNumbers = (0..9).shuffled()
        val awayNumbers = (0..9).shuffled()
        val cells = (0 until 100).map { i ->
            SquareCell(
                index = i,
                row = i / 10,
                col = i % 10,
                ownerName = if (i % 7 == 0) "Player ${i % 5 + 1}" else "",
                isUserSquare = false
            )
        }
        return SquaresBoard(
            gameId = gameId,
            pricePerSquare = 10,
            homeNumbers = homeNumbers,
            awayNumbers = awayNumbers,
            cells = cells
        )
    }

    fun checkForUpdates() {
        viewModelScope.launch(exceptionHandler) {
            _updateInfo.value = _updateInfo.value.copy(isChecking = true)
            val info = updaterRepo.checkForUpdates()
            _updateInfo.value = info
        }
    }

    fun downloadAndInstallUpdate() {
        val downloadUrl = _updateInfo.value.downloadUrl
        if (downloadUrl.isBlank()) return

        viewModelScope.launch(exceptionHandler) {
            _updateInfo.value = _updateInfo.value.copy(isDownloading = true, downloadProgress = 0f)
            val result = updaterRepo.downloadAndInstallApk(downloadUrl) { progress ->
                _updateInfo.value = _updateInfo.value.copy(downloadProgress = progress)
            }
            result.onSuccess { apkFile ->
                _updateInfo.value = _updateInfo.value.copy(
                    isDownloading = false,
                    readyToInstallPath = apkFile.absolutePath
                )
                updaterRepo.promptInstall(apkFile)
            }.onFailure { err ->
                _updateInfo.value = _updateInfo.value.copy(
                    isDownloading = false,
                    errorMessage = err.localizedMessage
                )
            }
        }
    }
}
