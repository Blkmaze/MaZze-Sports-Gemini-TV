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
    private val squaresStorage by lazy { com.example.util.SquaresStorage(application) }

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

    private val _multiviewChannels = MutableStateFlow<List<Channel?>>(listOf(null, null))
    val multiviewChannels: StateFlow<List<Channel?>> = _multiviewChannels.asStateFlow()

    private val _multiviewGameTitles = MutableStateFlow<List<String?>>(listOf(null, null))
    val multiviewGameTitles: StateFlow<List<String?>> = _multiviewGameTitles.asStateFlow()

    private val _multiviewCandidates = MutableStateFlow<List<List<Channel>>>(
        listOf(emptyList(), emptyList())
    )
    val multiviewCandidates: StateFlow<List<List<Channel>>> = _multiviewCandidates.asStateFlow()

    private val manualTileIndices = mutableSetOf<Int>()
    private val tileEvictionQueue = mutableListOf(0, 1)

    private val _multiviewSoundIndex = MutableStateFlow(0)
    val multiviewSoundIndex: StateFlow<Int> = _multiviewSoundIndex.asStateFlow()

    private val _squaresSelectedGame = MutableStateFlow<Game?>(null)
    val squaresSelectedGame: StateFlow<Game?> = _squaresSelectedGame.asStateFlow()

    private val _squaresBoard = MutableStateFlow(createInitialSquaresBoard(""))
    val squaresBoard: StateFlow<SquaresBoard> = _squaresBoard.asStateFlow()

    private val _squaresBoardVersions = MutableStateFlow<List<Int>>(listOf(1))
    val squaresBoardVersions: StateFlow<List<Int>> = _squaresBoardVersions.asStateFlow()

    private val _isSquaresLoading = MutableStateFlow(false)
    val isSquaresLoading: StateFlow<Boolean> = _isSquaresLoading.asStateFlow()
    private var isSquaresInitialized = false

    private val _showSquaresQrDialog = MutableStateFlow(false)
    val showSquaresQrDialog: StateFlow<Boolean> = _showSquaresQrDialog.asStateFlow()

    private val _squaresPaymentMethod = MutableStateFlow("Cash App")
    val squaresPaymentMethod: StateFlow<String> = _squaresPaymentMethod.asStateFlow()

    private val _playlistConfig = MutableStateFlow(PlaylistConfig())
    val playlistConfig: StateFlow<PlaylistConfig> = _playlistConfig.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    private val _ncaaFootballData = MutableStateFlow(NcaaFootballData())
    val ncaaFootballData: StateFlow<NcaaFootballData> = _ncaaFootballData.asStateFlow()

    private val _ncaaWomensRankings = MutableStateFlow<List<ApTeamRank>>(emptyList())
    val ncaaWomensRankings: StateFlow<List<ApTeamRank>> = _ncaaWomensRankings.asStateFlow()

    private val _channelSearchFilter = MutableStateFlow("")
    val channelSearchFilter: StateFlow<String> = _channelSearchFilter.asStateFlow()

    private val _lastCrashReport = MutableStateFlow<String?>(null)
    val lastCrashReport: StateFlow<String?> = _lastCrashReport.asStateFlow()

    private val _hotThisWeekGames = MutableStateFlow<List<Game>>(emptyList())
    val hotThisWeekGames: StateFlow<List<Game>> = _hotThisWeekGames.asStateFlow()

    private var pollJob: Job? = null
    private var ncaaPollJob: Job? = null
    private var weekFetchJob: Job? = null
    private var cachedWeekGames: List<Game> = emptyList()
    private val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    init {
        try {
            loadConfigAsync()
            loadCrashReport()
            loadChannels()
            startScoreboardPolling()

            // STAGED DATA LOAD: Delay ~2s before fetching AP Top 20 & NCAAW sequentially on Dispatchers.IO
            viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
                delay(2000L)
                fetchNcaaFootball()
                fetchWomensBasketball()
                startNcaaPolling()

                // Delay 8 additional seconds before starting heavy multi-league week schedule queries
                delay(8000L)
                startWeekScoreboardPolling()
            }
        } catch (t: Throwable) {
            recordInitError(t)
        }
    }

    private fun recordInitError(t: Throwable) {
        try {
            val crashFile = java.io.File(getApplication<Application>().filesDir, "last_crash.txt")
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            t.printStackTrace(pw)
            crashFile.writeText("Crash Time: ${Date()}\nLocation: TvMainViewModel.init\n\n$sw")
            _lastCrashReport.value = crashFile.readText()
        } catch (_: Throwable) {}
    }

    private fun loadConfigAsync() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val cfg = playlistRepo.loadConfig()
                _playlistConfig.value = cfg
                _spoilerFree.value = cfg.spoilerFree
            } catch (e: Throwable) {
                // Keep default
            }
        }
    }

    fun loadCrashReport() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val crashFile = java.io.File(getApplication<Application>().filesDir, "last_crash.txt")
                _lastCrashReport.value = if (crashFile.exists()) crashFile.readText() else null
            } catch (e: Throwable) {
                _lastCrashReport.value = null
            }
        }
    }

    fun clearCrashReport() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val crashFile = java.io.File(getApplication<Application>().filesDir, "last_crash.txt")
                if (crashFile.exists()) {
                    crashFile.delete()
                }
            } catch (e: Throwable) {}
            _lastCrashReport.value = null
        }
    }

    fun setScreen(screenIndex: Int) {
        _currentScreen.value = screenIndex
        _isFullScreenPlayer.value = false
        if (screenIndex == 3) {
            ensureSquaresLoaded()
        }
    }

    fun ensureSquaresLoaded() {
        if (isSquaresInitialized) return
        isSquaresInitialized = true
        _isSquaresLoading.value = true
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val targetGame = _squaresSelectedGame.value ?: _games.value.firstOrNull()
                val gid = targetGame?.id ?: ""
                val activeVer = squaresStorage.getActiveVersion(gid)
                val existing = squaresStorage.loadBoard(gid, activeVer)
                val defaultPayout = getDefaultPayoutTypeForGame(targetGame)
                val board = existing ?: createInitialSquaresBoard(gid, activeVer, defaultPayout).also {
                    squaresStorage.saveBoard(it)
                }
                val versions = squaresStorage.getBoardVersions(gid)
                _squaresSelectedGame.value = targetGame
                _squaresBoard.value = board
                _squaresBoardVersions.value = versions
            } catch (_: Throwable) {
            } finally {
                _isSquaresLoading.value = false
            }
        }
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
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val newVal = playlistRepo.toggleSpoilerFree(_spoilerFree.value)
            _spoilerFree.value = newVal
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val isFav = playlistRepo.toggleFavorite(channelId)
            _channels.value = _channels.value.map {
                if (it.id == channelId) it.copy(isFavorite = isFav) else it
            }
        }
    }

    fun loadChannels() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
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
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            playlistRepo.saveConfig(config)
            _playlistConfig.value = config
            loadChannels()
        }
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
        if (tileIndex in 0..1) {
            _multiviewSoundIndex.value = tileIndex
        }
    }

    fun setMultiviewSlotChannel(slotIndex: Int, channel: Channel) {
        if (slotIndex in 0..1) {
            val list = _multiviewChannels.value.toMutableList()
            while (list.size < 2) list.add(null)
            list[slotIndex] = channel
            _multiviewChannels.value = list.take(2)

            val titles = _multiviewGameTitles.value.toMutableList()
            while (titles.size < 2) titles.add(null)
            titles[slotIndex] = channel.name
            _multiviewGameTitles.value = titles.take(2)

            val candList = _multiviewCandidates.value.toMutableList()
            while (candList.size < 2) candList.add(emptyList())
            candList[slotIndex] = listOf(channel)
            _multiviewCandidates.value = candList.take(2)

            manualTileIndices.add(slotIndex)
            tileEvictionQueue.remove(slotIndex)
            tileEvictionQueue.add(slotIndex)
        }
    }

    fun addGameToMultiview(gameTitle: String, candidateChannels: List<Channel>): Int {
        val currentChannels = _multiviewChannels.value
        val emptySlot = (0..1).firstOrNull { currentChannels.getOrNull(it) == null }

        val targetSlot = if (emptySlot != null) {
            emptySlot
        } else {
            // Both slots are full: replace the older stream
            val older = tileEvictionQueue.firstOrNull { it in 0..1 } ?: 0
            older
        }

        val list = currentChannels.toMutableList()
        while (list.size < 2) list.add(null)
        list[targetSlot] = candidateChannels.firstOrNull()
        _multiviewChannels.value = list.take(2)

        val titles = _multiviewGameTitles.value.toMutableList()
        while (titles.size < 2) titles.add(null)
        titles[targetSlot] = gameTitle
        _multiviewGameTitles.value = titles.take(2)

        val candList = _multiviewCandidates.value.toMutableList()
        while (candList.size < 2) candList.add(emptyList())
        candList[targetSlot] = candidateChannels
        _multiviewCandidates.value = candList.take(2)

        manualTileIndices.add(targetSlot)
        tileEvictionQueue.remove(targetSlot)
        tileEvictionQueue.add(targetSlot)

        setScreen(1) // Multiview
        return targetSlot
    }

    fun cycleMultiviewTileToNextCandidate(tileIndex: Int) {
        if (tileIndex !in 0..1) return
        val candidates = _multiviewCandidates.value.getOrNull(tileIndex) ?: return
        if (candidates.size <= 1) return

        val current = _multiviewChannels.value.getOrNull(tileIndex)
        val currentIndex = candidates.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % candidates.size else 0

        val nextChannel = candidates.getOrNull(nextIndex) ?: return
        val list = _multiviewChannels.value.toMutableList()
        list[tileIndex] = nextChannel
        _multiviewChannels.value = list.take(2)
    }

    fun autoFillMultiviewTop20(forceAll: Boolean = false) {
        viewModelScope.launch(exceptionHandler) {
            val allChannels = _channels.value
            if (allChannels.isEmpty()) return@launch

            val topGames = _ncaaFootballData.value.games
            val currentMultiview = _multiviewChannels.value.toMutableList()
            while (currentMultiview.size < 2) currentMultiview.add(null)

            val currentTitles = _multiviewGameTitles.value.toMutableList()
            while (currentTitles.size < 2) currentTitles.add(null)

            val currentCandidates = _multiviewCandidates.value.toMutableList()
            while (currentCandidates.size < 2) currentCandidates.add(emptyList())

            val usedChannelIds = mutableSetOf<String>()

            withContext(Dispatchers.Default) {
                for (i in 0 until 2) {
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

            _multiviewChannels.value = currentMultiview.take(2)
            _multiviewGameTitles.value = currentTitles.take(2)
            _multiviewCandidates.value = currentCandidates.take(2)
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
                fetchWomensBasketball()
                delay(60000)
            }
        }
    }

    fun refreshScoreboardsManual() {
        viewModelScope.launch(exceptionHandler) {
            fetchScoreboards()
            fetchNcaaFootball()
            fetchWomensBasketball()
            fetchWeekScoreboards()
        }
    }

    private fun startWeekScoreboardPolling() {
        weekFetchJob?.cancel()
        weekFetchJob = viewModelScope.launch(exceptionHandler) {
            while (true) {
                fetchWeekScoreboards()
                delay(10 * 60 * 1000L) // Refreshed about every 10 minutes
            }
        }
    }

    private suspend fun fetchWeekScoreboards() {
        try {
            val result = espnRepo.fetchWeekScoreboards()
            result.onSuccess { games ->
                cachedWeekGames = games
                recomputeHotGames()
            }
        } catch (e: Throwable) {
            // Failures ignored safely
        }
    }

    private fun recomputeHotGames() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            val todayGames = _games.value
            val weekGames = cachedWeekGames
            val ncaaRankedGames = _ncaaFootballData.value.games
                .filter { it.homeRank != null || it.awayRank != null || it.isSecMatch }
                .map { ncaa ->
                    Game(
                        id = ncaa.id,
                        name = ncaa.name,
                        shortName = "${ncaa.awayTeam.abbreviation} @ ${ncaa.homeTeam.abbreviation}",
                        sport = "football",
                        league = "CFB",
                        homeTeam = ncaa.homeTeam,
                        awayTeam = ncaa.awayTeam,
                        homeScore = ncaa.homeScore,
                        awayScore = ncaa.awayScore,
                        statusDetail = ncaa.statusDetail.ifBlank { ncaa.kickoffDisplay },
                        state = ncaa.state,
                        startTime = ncaa.startTime,
                        network = ncaa.network
                    )
                }

            val combined = (todayGames + weekGames + ncaaRankedGames)
                .distinctBy { it.id.ifBlank { "${it.homeTeam.displayName}-${it.awayTeam.displayName}-${it.startTime}" } }
                .filter { it.state != "post" } // Live or upcoming only

            val hotGames = combined
                .sortedWith(
                    compareByDescending<Game> { computeGameHotScore(it) }
                        .thenBy { it.startTime }
                )
                .take(10)

            _hotThisWeekGames.value = hotGames
        }
    }

    private fun computeGameHotScore(game: Game): Long {
        var score = 0L

        // 1. LIVE games first (highest priority)
        val isLive = game.state.equals("in", ignoreCase = true)
        if (isLive) {
            score += 1_000_000L
        }

        val name = game.name.lowercase(Locale.ROOT)
        val shortName = game.shortName.lowercase(Locale.ROOT)
        val league = game.league.lowercase(Locale.ROOT)
        val home = game.homeTeam.displayName.lowercase(Locale.ROOT)
        val away = game.awayTeam.displayName.lowercase(Locale.ROOT)
        val network = game.network.lowercase(Locale.ROOT)
        val combined = "$name $shortName $home $away $league $network"

        // 2. Monday Night Football, Thursday Night Football, Sunday Night Football
        val isMnf = combined.contains("monday night football") || combined.contains("mnf") ||
                (league == "nfl" && (network.contains("espn") || combined.contains("monday")))
        val isSnf = combined.contains("sunday night football") || combined.contains("snf") ||
                (league == "nfl" && (network.contains("nbc") || combined.contains("sunday night")))
        val isTnf = combined.contains("thursday night football") || combined.contains("tnf") ||
                (league == "nfl" && (network.contains("prime") || combined.contains("thursday")))

        if (isMnf) {
            score += 500_000L
        } else if (isSnf) {
            score += 480_000L
        } else if (isTnf) {
            score += 460_000L
        }

        // 3. World Series and MLB playoffs
        val startMonth = game.startTime.substring(5, 7).toIntOrNull() ?: 0
        val startDay = game.startTime.substring(8, 10).toIntOrNull() ?: 0
        // ESPN event names are only team names, so detect the Fall Classic by date: MLB games after Oct 15 are the World Series
        val isLateOctMlb = league == "mlb" && ((startMonth == 10 && startDay >= 15) || startMonth == 11)
        val isWorldSeries = combined.contains("world series") || isLateOctMlb
        val isMlbPlayoffs = league == "mlb" && (isWorldSeries || combined.contains("playoff") ||
                combined.contains("postseason") || combined.contains("alcs") || combined.contains("nlcs") ||
                combined.contains("alds") || combined.contains("nlds"))

        if (isWorldSeries) {
            score += 520_000L
        } else if (isMlbPlayoffs) {
            score += 400_000L
        } else if (league == "mlb") {
            score += 120_000L
        }

        // 4. WNBA Finals
        // ESPN event names are only team names, so any WNBA game in October is the WNBA Finals
        val isWnbaFinals = league == "wnba" && (combined.contains("finals") || combined.contains("championship") || startMonth == 10)
        if (isWnbaFinals) {
            score += 380_000L
        } else if (league == "wnba") {
            score += 110_000L
        }

        // 5. NBA season-opening matchups and rivalry or big-market games (Lakers, Warriors, Celtics, Knicks, etc.)
        if (league == "nba") {
            val bigMarketTeams = listOf(
                "lakers", "warriors", "celtics", "knicks", "heat", "bucks",
                "nuggets", "76ers", "suns", "mavericks", "clippers"
            )
            val homeBig = bigMarketTeams.any { home.contains(it) }
            val awayBig = bigMarketTeams.any { away.contains(it) }
            val isOpener = combined.contains("opening night") || combined.contains("season opener") ||
                    combined.contains("kia nba tip-off") || combined.contains("tip-off")

            var nbaScore = 200_000L
            if (isOpener) nbaScore += 100_000L
            if (homeBig && awayBig) {
                nbaScore += 80_000L
            } else if (homeBig || awayBig) {
                nbaScore += 40_000L
            }
            score += nbaScore
        }

        // 6. NFL games (general) and ranked college games (CFB, NCAAW, NCAAM)
        if (league == "nfl" && !isMnf && !isSnf && !isTnf) {
            score += 180_000L
        }
        if (league == "cfb" || league == "ncaaw" || league == "ncaam" || league.contains("college")) {
            val isRanked = combined.contains("#") ||
                    (game.homeTeam.displayName.startsWith("#") || game.awayTeam.displayName.startsWith("#")) ||
                    _ncaaWomensRankings.value.any { rankItem ->
                        val lower = rankItem.displayName.lowercase(Locale.ROOT)
                        lower.length > 2 && (home.contains(lower) || away.contains(lower))
                    }
            if (isRanked) {
                score += 170_000L
            } else {
                score += 90_000L
            }
        }

        // 7. NHL
        if (league == "nhl") {
            score += 80_000L
        }

        // 8. Other sports
        if (score == 0L || (isLive && score == 1_000_000L)) {
            score += 40_000L
        }

        return score
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
                recomputeHotGames()
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
                recomputeHotGames()
            }
        } catch (e: Throwable) {}
    }

    private suspend fun fetchWomensBasketball() {
        try {
            val result = espnRepo.fetchWomensBasketballRankings()
            result.onSuccess { rankings ->
                _ncaaWomensRankings.value = rankings
                recomputeHotGames()
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
            leagueLower == "ncaaw" || leagueLower.contains("womens-college-basketball") -> 680
            leagueLower == "ncaam" || leagueLower.contains("mens-college-basketball") -> 650
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
            listOf("espn w", "espnw"),
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

    private fun getDefaultPayoutTypeForGame(game: Game?): String {
        if (game == null) return "QUARTERS"
        val league = game.league.lowercase(Locale.ROOT)
        val name = game.name.lowercase(Locale.ROOT)
        val sport = game.sport.lowercase(Locale.ROOT)
        return if (league == "ncaam" || league.contains("college basketball") || (sport.contains("basketball") && league.contains("ncaa"))) {
            "HALVES"
        } else {
            "QUARTERS"
        }
    }

    fun selectGameForSquares(game: Game) {
        _squaresSelectedGame.value = game
        _isSquaresLoading.value = true
        isSquaresInitialized = true
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val activeVer = squaresStorage.getActiveVersion(game.id)
                val existing = squaresStorage.loadBoard(game.id, activeVer)
                val defaultPayout = getDefaultPayoutTypeForGame(game)
                val board = if (existing != null) {
                    existing
                } else {
                    val fresh = createInitialSquaresBoard(game.id, activeVer, defaultPayout)
                    squaresStorage.saveBoard(fresh)
                    fresh
                }
                val versions = squaresStorage.getBoardVersions(game.id)
                _squaresBoard.value = board
                _squaresBoardVersions.value = versions
            } finally {
                _isSquaresLoading.value = false
            }
        }
    }

    fun setSquaresPrice(price: Int) {
        val board = _squaresBoard.value
        if (board.sales.isNotEmpty() || board.pendingEntries.isNotEmpty()) return
        val updated = board.copy(pricePerSquare = price)
        _squaresBoard.value = updated
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updated)
        }
    }

    fun setSquaresPayoutType(type: String) {
        val board = _squaresBoard.value
        if (board.sales.isNotEmpty() || board.pendingEntries.isNotEmpty()) return
        val updated = board.copy(payoutType = type)
        _squaresBoard.value = updated
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updated)
        }
    }

    fun submitPendingEntry(initials: String, count: Int, paymentMethod: String): Boolean {
        val board = _squaresBoard.value
        if (board.isBoardLocked) return false
        val cleanInitials = initials.trim().uppercase().take(3)
        if (cleanInitials.length < 2) return false
        val soldCount = board.cells.count { it.ownerName.isNotBlank() }
        val pendingCount = board.pendingEntries.sumOf { it.count }
        val available = (100 - soldCount - pendingCount).coerceAtLeast(0)
        if (count <= 0 || count > available) return false

        val entry = SquarePendingEntry(
            id = UUID.randomUUID().toString(),
            initials = cleanInitials,
            count = count,
            paymentMethod = paymentMethod.uppercase(),
            totalAmount = count * board.pricePerSquare,
            timestamp = System.currentTimeMillis()
        )
        val updatedBoard = board.copy(pendingEntries = board.pendingEntries + entry)
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
        return true
    }

    fun confirmPendingPaid(entryId: String): List<Int> {
        val board = _squaresBoard.value
        val entry = board.pendingEntries.firstOrNull { it.id == entryId } ?: return emptyList()
        val openCells = board.cells.filter { it.ownerName.isBlank() }
        if (openCells.size < entry.count) return emptyList()

        val chosenCells = openCells.shuffled().take(entry.count)
        val chosenIndices = chosenCells.map { it.index }.toSet()

        val updatedCells = board.cells.map { cell ->
            if (cell.index in chosenIndices) {
                cell.copy(ownerName = entry.initials, isUserSquare = true)
            } else {
                cell
            }
        }

        val sale = SquareSale(
            id = UUID.randomUUID().toString(),
            initials = entry.initials,
            count = chosenCells.size,
            paymentMethod = entry.paymentMethod,
            assignedSquares = chosenCells.map { it.index },
            totalAmount = chosenCells.size * board.pricePerSquare,
            timestamp = System.currentTimeMillis()
        )

        val updatedPending = board.pendingEntries.filter { it.id != entryId }
        val updatedBoard = board.copy(
            cells = updatedCells,
            pendingEntries = updatedPending,
            sales = listOf(sale) + board.sales
        )
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
        return chosenCells.map { it.index }
    }

    fun rejectPendingEntry(entryId: String) {
        val board = _squaresBoard.value
        val updatedPending = board.pendingEntries.filter { it.id != entryId }
        val updatedBoard = board.copy(pendingEntries = updatedPending)
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
    }

    fun releaseAllPending() {
        val board = _squaresBoard.value
        val updatedBoard = board.copy(pendingEntries = emptyList())
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
    }

    fun toggleLockBoard() {
        val board = _squaresBoard.value
        val updatedBoard = board.copy(isBoardLocked = !board.isBoardLocked)
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
    }

    fun lockNumbers() {
        val board = _squaresBoard.value
        if (board.isNumbersLocked) return
        val updatedBoard = board.copy(
            isNumbersLocked = true,
            homeNumbers = (0..9).shuffled(),
            awayNumbers = (0..9).shuffled()
        )
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
    }

    fun createNextBoardVersion() {
        val gameId = _squaresSelectedGame.value?.id ?: _squaresBoard.value.gameId
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val versions = squaresStorage.getBoardVersions(gameId)
            val nextVer = (versions.maxOrNull() ?: 1) + 1
            val defaultPayout = _squaresBoard.value.payoutType
            val fresh = createInitialSquaresBoard(gameId, nextVer, defaultPayout).copy(
                pricePerSquare = _squaresBoard.value.pricePerSquare
            )
            squaresStorage.saveBoard(fresh)
            squaresStorage.setActiveVersion(gameId, nextVer)
            val updatedVersions = squaresStorage.getBoardVersions(gameId)
            _squaresBoard.value = fresh
            _squaresBoardVersions.value = updatedVersions
        }
    }

    fun switchBoardVersion(version: Int) {
        val gameId = _squaresSelectedGame.value?.id ?: _squaresBoard.value.gameId
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val loaded = squaresStorage.loadBoard(gameId, version) ?: return@launch
            squaresStorage.setActiveVersion(gameId, version)
            _squaresBoard.value = loaded
        }
    }

    fun sellSquares(initials: String, count: Int, paymentMethod: String): List<Int> {
        val board = _squaresBoard.value
        val openCells = board.cells.filter { it.ownerName.isBlank() }
        if (openCells.size < count) return emptyList()

        val chosenCells = openCells.shuffled().take(count)
        val chosenIndices = chosenCells.map { it.index }.toSet()

        val updatedCells = board.cells.map { cell ->
            if (cell.index in chosenIndices) {
                cell.copy(ownerName = initials.uppercase(), isUserSquare = true)
            } else {
                cell
            }
        }

        val sale = SquareSale(
            id = UUID.randomUUID().toString(),
            initials = initials.uppercase(),
            count = count,
            paymentMethod = paymentMethod.uppercase(),
            assignedSquares = chosenCells.map { it.index },
            totalAmount = count * board.pricePerSquare,
            timestamp = System.currentTimeMillis()
        )

        val updatedBoard = board.copy(
            cells = updatedCells,
            sales = listOf(sale) + board.sales
        )
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
        return chosenCells.map { it.index }
    }

    fun resetSquaresBoard() {
        val gameId = _squaresSelectedGame.value?.id ?: _squaresBoard.value.gameId
        val currentVer = _squaresBoard.value.boardVersion
        val defaultPayout = getDefaultPayoutTypeForGame(_squaresSelectedGame.value)
        val fresh = createInitialSquaresBoard(gameId, currentVer, defaultPayout).copy(
            pricePerSquare = _squaresBoard.value.pricePerSquare
        )
        _squaresBoard.value = fresh
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(fresh)
        }
    }

    fun toggleSquareSelection(row: Int, col: Int) {
        // Maintained for backward compatibility
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
        val updatedBoard = _squaresBoard.value.copy(cells = currentCells)
        _squaresBoard.value = updatedBoard
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            squaresStorage.saveBoard(updatedBoard)
        }
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

    private fun createInitialSquaresBoard(gameId: String, version: Int = 1, payoutType: String = "QUARTERS"): SquaresBoard {
        val homeNumbers = (0..9).shuffled()
        val awayNumbers = (0..9).shuffled()
        val cells = (0 until 100).map { i ->
            SquareCell(
                index = i,
                row = i / 10,
                col = i % 10,
                ownerName = "",
                isUserSquare = false
            )
        }
        return SquaresBoard(
            gameId = gameId,
            boardVersion = version,
            pricePerSquare = 10,
            payoutType = payoutType,
            isNumbersLocked = false,
            isBoardLocked = false,
            homeNumbers = homeNumbers,
            awayNumbers = awayNumbers,
            cells = cells,
            pendingEntries = emptyList(),
            sales = emptyList()
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
