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

    private val _hotThisWeekGames = MutableStateFlow<List<Game>>(emptyList())
    val hotThisWeekGames: StateFlow<List<Game>> = _hotThisWeekGames.asStateFlow()

    private var pollJob: Job? = null
    private var ncaaPollJob: Job? = null
    private var weekFetchJob: Job? = null
    private var cachedWeekGames: List<Game> = emptyList()
    private val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    init {
        try {
            _spoilerFree.value = _playlistConfig.value.spoilerFree
        } catch (e: Throwable) {}

        loadCrashReport()
        loadChannels()
        startScoreboardPolling()
        startNcaaPolling()
        startWeekScoreboardPolling()
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
        val isWorldSeries = combined.contains("world series") || (league == "mlb" && combined.contains("ws"))
        val isMlbPlayoffs = league == "mlb" && (isWorldSeries || combined.contains("playoff") ||
                combined.contains("postseason") || combined.contains("alcs") || combined.contains("nlcs") ||
                combined.contains("alds") || combined.contains("nlds"))

        if (isWorldSeries) {
            score += 440_000L
        } else if (isMlbPlayoffs) {
            score += 400_000L
        } else if (league == "mlb") {
            score += 120_000L
        }

        // 4. WNBA Finals
        val isWnbaFinals = league == "wnba" && (combined.contains("finals") || combined.contains("championship"))
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

        // 6. NFL games (general) and ranked NCAA football games
        if (league == "nfl" && !isMnf && !isSnf && !isTnf) {
            score += 180_000L
        }
        if (league == "cfb" || league.contains("college")) {
            val isRanked = combined.contains("#") ||
                    (game.homeTeam.displayName.startsWith("#") || game.awayTeam.displayName.startsWith("#"))
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
