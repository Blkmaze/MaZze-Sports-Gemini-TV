package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.EspnScoreboardRepository
import com.example.api.PlaylistRepository
import com.example.api.UpdaterRepository
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class TvMainViewModel(application: Application) : AndroidViewModel(application) {

    private val espnRepo = EspnScoreboardRepository()
    private val playlistRepo = PlaylistRepository(application)
    private val updaterRepo = UpdaterRepository(application)

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

    // Spoiler free feature
    private val _spoilerFree = MutableStateFlow(false)
    val spoilerFree: StateFlow<Boolean> = _spoilerFree.asStateFlow()

    private val _revealedGameIds = MutableStateFlow<Set<String>>(emptySet())
    val revealedGameIds: StateFlow<Set<String>> = _revealedGameIds.asStateFlow()

    // Multiview state: 4 slots & audio tile index (0..3)
    private val _multiviewChannels = MutableStateFlow<List<Channel?>>(listOf(null, null, null, null))
    val multiviewChannels: StateFlow<List<Channel?>> = _multiviewChannels.asStateFlow()

    private val _multiviewGameTitles = MutableStateFlow<List<String?>>(listOf(null, null, null, null))
    val multiviewGameTitles: StateFlow<List<String?>> = _multiviewGameTitles.asStateFlow()

    private val manualTileIndices = mutableSetOf<Int>()

    private val _multiviewSoundIndex = MutableStateFlow(0)
    val multiviewSoundIndex: StateFlow<Int> = _multiviewSoundIndex.asStateFlow()

    // Squares Contest state
    private val _squaresSelectedGame = MutableStateFlow<Game?>(null)
    val squaresSelectedGame: StateFlow<Game?> = _squaresSelectedGame.asStateFlow()

    private val _squaresBoard = MutableStateFlow<SquaresBoard>(createInitialSquaresBoard(""))
    val squaresBoard: StateFlow<SquaresBoard> = _squaresBoard.asStateFlow()

    private val _showSquaresQrDialog = MutableStateFlow(false)
    val showSquaresQrDialog: StateFlow<Boolean> = _showSquaresQrDialog.asStateFlow()

    private val _squaresPaymentMethod = MutableStateFlow("Cash App")
    val squaresPaymentMethod: StateFlow<String> = _squaresPaymentMethod.asStateFlow()

    // Settings & Updater state
    private val _playlistConfig = MutableStateFlow(playlistRepo.loadConfig())
    val playlistConfig: StateFlow<PlaylistConfig> = _playlistConfig.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    // NCAA CFB AP Top 20 & SEC Focus state
    private val _ncaaFootballData = MutableStateFlow(NcaaFootballData())
    val ncaaFootballData: StateFlow<NcaaFootballData> = _ncaaFootballData.asStateFlow()

    private val _channelSearchFilter = MutableStateFlow("")
    val channelSearchFilter: StateFlow<String> = _channelSearchFilter.asStateFlow()

    private var pollJob: Job? = null
    private var ncaaPollJob: Job? = null
    private val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    init {
        _spoilerFree.value = _playlistConfig.value.spoilerFree
        loadChannels()
        startScoreboardPolling()
        startNcaaPolling()
    }

    fun setScreen(screenIndex: Int) {
        _currentScreen.value = screenIndex
        if (screenIndex == 1) {
            autoFillMultiviewTop20(forceAll = false)
        }
    }

    fun setChannelSearchFilter(query: String) {
        _channelSearchFilter.value = query
    }

    fun openChannelsWithFilter(query: String) {
        _channelSearchFilter.value = query
        _currentScreen.value = 2 // Channels Screen
    }

    private fun startNcaaPolling() {
        ncaaPollJob?.cancel()
        ncaaPollJob = viewModelScope.launch {
            while (isActive) {
                fetchNcaaFootball()
                delay(5 * 60 * 1000L) // 5 minutes refresh
            }
        }
    }

    suspend fun fetchNcaaFootball() {
        val res = espnRepo.fetchNcaaFootballAndRankings()
        if (res.isSuccess) {
            _ncaaFootballData.value = res.getOrNull() ?: NcaaFootballData()
            autoFillMultiviewTop20(forceAll = false)
        }
    }

    fun matchChannelForNcaaGame(game: NcaaGame, excludeIds: Set<String> = emptySet()): Channel? {
        val currentChannels = _channels.value.filter { it.id !in excludeIds }
        if (currentChannels.isEmpty()) return null

        val homeName = game.homeTeam.displayName.lowercase(Locale.ROOT)
        val awayName = game.awayTeam.displayName.lowercase(Locale.ROOT)
        val homeAbbr = game.homeTeam.abbreviation.lowercase(Locale.ROOT)
        val awayAbbr = game.awayTeam.abbreviation.lowercase(Locale.ROOT)
        val netRaw = game.network.lowercase(Locale.ROOT)

        // 1. Try exact team match in playlist
        currentChannels.find { ch ->
            val cName = ch.name.lowercase(Locale.ROOT)
            (homeAbbr.length >= 3 && cName.contains(homeAbbr)) ||
            (awayAbbr.length >= 3 && cName.contains(awayAbbr)) ||
            (homeName.length > 3 && cName.contains(homeName)) ||
            (awayName.length > 3 && cName.contains(awayName))
        }?.let { return it }

        // 2. Try network match in playlist (e.g. ESPN, ESPN2, SEC Network, ABC, CBS, FOX, NBC)
        val netKeywords = mutableListOf<String>()
        if (netRaw.isNotBlank() && netRaw != "tv") {
            netKeywords.add(netRaw)
            when {
                netRaw.contains("sec") -> {
                    netKeywords.add("sec network")
                    netKeywords.add("secn")
                    netKeywords.add("sec")
                }
                netRaw.contains("espn2") -> {
                    netKeywords.add("espn2")
                    netKeywords.add("espn 2")
                }
                netRaw.contains("espnu") -> {
                    netKeywords.add("espnu")
                    netKeywords.add("espn u")
                }
                netRaw.contains("espn") -> {
                    netKeywords.add("espn")
                }
                netRaw.contains("abc") -> {
                    netKeywords.add("abc")
                }
                netRaw.contains("cbs") -> {
                    netKeywords.add("cbs")
                }
                netRaw.contains("fox") -> {
                    netKeywords.add("fox")
                }
                netRaw.contains("nbc") -> {
                    netKeywords.add("nbc")
                }
                netRaw.contains("fs1") -> {
                    netKeywords.add("fs1")
                    netKeywords.add("fox sports 1")
                }
            }
        }

        for (kw in netKeywords) {
            if (kw.length >= 3) {
                currentChannels.find { ch ->
                    ch.name.lowercase(Locale.ROOT).contains(kw)
                }?.let { return it }
            }
        }

        // 3. Try generic CFB / college sports
        return currentChannels.find { ch ->
            val cName = ch.name.lowercase(Locale.ROOT)
            val cGrp = ch.groupTitle.lowercase(Locale.ROOT)
            cName.contains("college") || cName.contains("ncaa") || cName.contains("acc") ||
            cName.contains("sec") || cGrp.contains("college") || cGrp.contains("sports")
        }
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
        _playlistConfig.value = _playlistConfig.value.copy(spoilerFree = newVal)
    }

    fun toggleFavorite(channelId: String) {
        playlistRepo.toggleFavorite(channelId)
        val favs = playlistRepo.getFavorites()
        _channels.value = _channels.value.map {
            it.copy(isFavorite = favs.contains(it.id))
        }
    }

    fun loadChannels() {
        viewModelScope.launch {
            val list = playlistRepo.loadChannels()
            _channels.value = list

            // Set default stream if none playing or current is outdated
            if ((_currentPlayingChannel.value == null || list.none { it.id == _currentPlayingChannel.value?.id }) && list.isNotEmpty()) {
                _currentPlayingChannel.value = list.first()
            }

            // Populate multiview initial slots via Top 20 auto-fill if available
            autoFillMultiviewTop20(forceAll = false)
            if (_multiviewChannels.value.all { it == null } && list.isNotEmpty()) {
                val slots = MutableList<Channel?>(4) { null }
                for (i in 0 until minOf(4, list.size)) {
                    slots[i] = list[i]
                }
                _multiviewChannels.value = slots
            }
        }
    }

    fun updatePlaylistConfig(config: PlaylistConfig) {
        playlistRepo.saveConfig(config)
        _playlistConfig.value = config
        loadChannels()
    }

    fun loadDemoChannels() {
        val demoConfig = PlaylistConfig(type = "demo", spoilerFree = _spoilerFree.value)
        updatePlaylistConfig(demoConfig)
    }

    fun setMultiviewSound(tileIndex: Int) {
        _multiviewSoundIndex.value = tileIndex
    }

    fun setMultiviewSlotChannel(slotIndex: Int, channel: Channel) {
        val slots = _multiviewChannels.value.toMutableList()
        val titles = _multiviewGameTitles.value.toMutableList()
        if (slotIndex in 0..3) {
            slots[slotIndex] = channel
            titles[slotIndex] = null // Clear auto-fill title so channel name is displayed
            manualTileIndices.add(slotIndex)
            _multiviewChannels.value = slots
            _multiviewGameTitles.value = titles
        }
    }

    fun autoFillMultiviewTop20(forceAll: Boolean = false) {
        if (forceAll) {
            manualTileIndices.clear()
        }
        val allChannels = _channels.value
        if (allChannels.isEmpty()) return

        val ncaaGames = _ncaaFootballData.value.games
        // 1. Prioritize games involving AP Top 20 teams from the Top 20 section
        val top20Games = ncaaGames.filter { game ->
            (game.homeRank != null && game.homeRank <= 20) || (game.awayRank != null && game.awayRank <= 20)
        }

        val candidateGames = if (top20Games.isNotEmpty()) {
            top20Games.sortedWith(
                compareBy<NcaaGame> { !it.isLive } // Live games first
                    .thenBy { if (it.state == "pre") 0 else 1 } // Soonest today
                    .thenBy { it.startTime }
                    .thenBy { it.bestRank }
            )
        } else if (ncaaGames.isNotEmpty()) {
            ncaaGames.sortedWith(
                compareBy<NcaaGame> { !it.isLive }
                    .thenBy { if (it.state == "pre") 0 else 1 }
                    .thenBy { it.startTime }
            )
        } else {
            emptyList()
        }

        val currentSlots = _multiviewChannels.value.toMutableList()
        val currentTitles = _multiviewGameTitles.value.toMutableList()

        val assignedChannelIds = mutableSetOf<String>()
        // If not forcing all, preserve channels of manual slots
        for (i in 0..3) {
            if (!forceAll && manualTileIndices.contains(i)) {
                currentSlots[i]?.id?.let { assignedChannelIds.add(it) }
            }
        }

        // Match games to available channels
        val matchedList = mutableListOf<Pair<NcaaGame, Channel>>()
        for (game in candidateGames) {
            val matched = matchChannelForNcaaGame(game, excludeIds = assignedChannelIds)
                ?: matchChannelForNcaaGame(game)
            if (matched != null) {
                matchedList.add(Pair(game, matched))
                assignedChannelIds.add(matched.id)
                if (matchedList.size >= 4) break
            }
        }

        var matchIdx = 0
        for (i in 0..3) {
            if (forceAll || !manualTileIndices.contains(i)) {
                if (matchIdx < matchedList.size) {
                    val (g, ch) = matchedList[matchIdx++]
                    currentSlots[i] = ch
                    val awayR = if (g.awayRank != null) "#${g.awayRank} " else ""
                    val homeR = if (g.homeRank != null) "#${g.homeRank} " else ""
                    currentTitles[i] = "${awayR}${g.awayTeam.displayName} vs ${homeR}${g.homeTeam.displayName}"
                } else if (forceAll && currentSlots[i] == null && i < allChannels.size) {
                    currentSlots[i] = allChannels[i]
                    currentTitles[i] = null
                }
            }
        }

        _multiviewChannels.value = currentSlots
        _multiviewGameTitles.value = currentTitles
    }

    private fun startScoreboardPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                fetchScoreboards()
                delay(20_000) // Poll every 20 seconds
            }
        }
    }

    fun refreshScoreboardsManual() {
        viewModelScope.launch {
            fetchScoreboards()
        }
    }

    private suspend fun fetchScoreboards() {
        val result = espnRepo.fetchAllScoreboards()
        if (result.isSuccess) {
            val fetchedGames = result.getOrNull() ?: emptyList()
            if (fetchedGames.isNotEmpty()) {
                _games.value = fetchedGames
                _isOffline.value = false
                _lastUpdatedTime.value = timeFormat.format(Date())

                // If no top squares game picked, pick first live or upcoming
                if (_squaresSelectedGame.value == null) {
                    val gameForSquares = fetchedGames.firstOrNull { it.isLive } ?: fetchedGames.firstOrNull()
                    if (gameForSquares != null) {
                        selectGameForSquares(gameForSquares)
                    }
                }

                // Match channel for top live game if available
                val topLive = fetchedGames.firstOrNull { it.isLive } ?: fetchedGames.firstOrNull()
                if (topLive != null && _channels.value.isNotEmpty()) {
                    val matched = playlistRepo.matchChannelForGame(topLive, _channels.value)
                    if (matched != null && _currentPlayingChannel.value == null) {
                        _currentPlayingChannel.value = matched
                    }
                }
            }
        } else {
            // Keep last data, set offline indicator
            _isOffline.value = true
        }
    }

    // --- Squares Logic ---
    fun selectGameForSquares(game: Game) {
        _squaresSelectedGame.value = game
        _squaresBoard.value = createInitialSquaresBoard(game.id)
    }

    fun setSquaresPrice(price: Int) {
        _squaresBoard.value = _squaresBoard.value.copy(pricePerSquare = price)
    }

    fun toggleSquareSelection(row: Int, col: Int) {
        val currentBoard = _squaresBoard.value
        val updatedCells = currentBoard.cells.map { cell ->
            if (cell.row == row && cell.col == col) {
                if (cell.isUserSquare) {
                    cell.copy(ownerName = null, isUserSquare = false)
                } else if (cell.ownerName == null) {
                    cell.copy(ownerName = "YOU", isUserSquare = true)
                } else {
                    cell // Already taken by another participant
                }
            } else {
                cell
            }
        }

        val totalTaken = updatedCells.count { it.ownerName != null }
        var homeNums = currentBoard.homeNumbers
        var awayNums = currentBoard.awayNumbers

        // Draw numbers if >= 75% filled
        if (totalTaken >= 75 && (homeNums == null || awayNums == null)) {
            val random = Random(currentBoard.gameId.hashCode().toLong())
            homeNums = (0..9).shuffled(random)
            awayNums = (0..9).shuffled(random)
        } else if (totalTaken < 75) {
            homeNums = null
            awayNums = null
        }

        _squaresBoard.value = currentBoard.copy(
            cells = updatedCells,
            homeNumbers = homeNums,
            awayNumbers = awayNums
        )
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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createInitialSquaresBoard(gameId: String): SquaresBoard {
        val random = Random(if (gameId.isNotBlank()) gameId.hashCode().toLong() else 42L)
        val initialCells = mutableListOf<SquareCell>()

        val participantInitials = listOf("JD", "MK", "SL", "CB", "RT", "AW", "PL", "TN", "BH", "EZ", "DW", "RM")

        // Pre-fill approximately 72 squares to represent an active game contest close to the 75% threshold
        var cellIndex = 0
        for (r in 0..9) {
            for (c in 0..9) {
                val isTaken = random.nextFloat() < 0.72f
                val owner = if (isTaken) participantInitials[random.nextInt(participantInitials.size)] else null
                initialCells.add(
                    SquareCell(
                        index = cellIndex++,
                        row = r,
                        col = c,
                        ownerName = owner,
                        isUserSquare = false
                    )
                )
            }
        }

        val totalTaken = initialCells.count { it.ownerName != null }
        val homeNums = if (totalTaken >= 75) (0..9).shuffled(random) else null
        val awayNums = if (totalTaken >= 75) (0..9).shuffled(random) else null

        return SquaresBoard(
            gameId = gameId,
            pricePerSquare = 10,
            homeNumbers = homeNums,
            awayNumbers = awayNums,
            cells = initialCells
        )
    }

    // --- Updates Logic ---
    fun checkForUpdates() {
        viewModelScope.launch {
            _updateInfo.value = _updateInfo.value.copy(isChecking = true, errorMessage = null)
            val info = updaterRepo.checkForUpdates()
            _updateInfo.value = info.copy(isChecking = false)
        }
    }

    fun downloadAndInstallUpdate() {
        val downloadUrl = _updateInfo.value.downloadUrl ?: return
        viewModelScope.launch {
            _updateInfo.value = _updateInfo.value.copy(isDownloading = true, downloadProgress = 0f)
            val res = updaterRepo.downloadAndInstallApk(downloadUrl) { progress ->
                _updateInfo.value = _updateInfo.value.copy(downloadProgress = progress)
            }

            if (res.isSuccess) {
                val file = res.getOrNull()
                _updateInfo.value = _updateInfo.value.copy(
                    isDownloading = false,
                    readyToInstallPath = file?.absolutePath
                )
                if (file != null) {
                    updaterRepo.promptInstall(file)
                }
            } else {
                _updateInfo.value = _updateInfo.value.copy(
                    isDownloading = false,
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Download failed"
                )
            }
        }
    }
}
