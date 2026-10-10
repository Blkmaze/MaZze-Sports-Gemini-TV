package com.example.api

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EspnScoreboardRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    data class LeagueTarget(val sport: String, val league: String, val displayName: String)

    val leagues = listOf(
        LeagueTarget("football", "nfl", "NFL"),
        LeagueTarget("basketball", "nba", "NBA"),
        LeagueTarget("hockey", "nhl", "NHL"),
        LeagueTarget("baseball", "mlb", "MLB"),
        LeagueTarget("football", "college-football", "CFB"),
        LeagueTarget("basketball", "mens-college-basketball", "NCAAM"),
        LeagueTarget("soccer", "eng.1", "EPL"),
        LeagueTarget("soccer", "usa.1", "MLS")
    )

    val secTeams = listOf(
        "Alabama", "Arkansas", "Auburn", "Florida", "Georgia", "Kentucky",
        "LSU", "Mississippi State", "Missouri", "Oklahoma", "Ole Miss",
        "South Carolina", "Tennessee", "Texas", "Texas A&M", "Vanderbilt"
    )

    private val secAbbrs = setOf(
        "ala", "ark", "aub", "fla", "uga", "uk", "lsu", "msst",
        "miz", "ou", "miss", "sc", "tenn", "tex", "ta&m", "van", "a&m"
    )

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun fetchAllScoreboards(): Result<List<Game>> = withContext(Dispatchers.IO) {
        val allGames = mutableListOf<Game>()
        var successCount = 0

        for (target in leagues) {
            val url = "https://site.api.espn.com/apis/site/v2/sports/${target.sport}/${target.league}/scoreboard"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MaZzeSportsTV/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val parsed = parseEspnScoreboard(body, target)
                            allGames.addAll(parsed)
                            successCount++
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue with other leagues
            }
        }

        if (successCount > 0 || allGames.isNotEmpty()) {
            val sorted = sortGames(allGames)
            Result.success(sorted)
        } else {
            Result.failure(Exception("Failed to fetch ESPN scoreboards"))
        }
    }

    private fun parseEspnScoreboard(jsonString: String, target: LeagueTarget): List<Game> {
        val list = mutableListOf<Game>()
        try {
            val root = JSONObject(jsonString)
            val events = root.optJSONArray("events") ?: return emptyList()

            for (i in 0 until events.length()) {
                val event = events.optJSONObject(i) ?: continue
                val eventId = event.optString("id", "")
                val eventName = event.optString("name", "Matchup")
                val shortName = event.optString("shortName", eventName)
                val dateStr = event.optString("date", "")

                val competitions = event.optJSONArray("competitions")
                val competition = competitions?.optJSONObject(0) ?: continue
                val competitors = competition.optJSONArray("competitors") ?: continue

                var homeTeam = Team()
                var awayTeam = Team()
                var homeScore = "0"
                var awayScore = "0"

                for (c in 0 until competitors.length()) {
                    val comp = competitors.optJSONObject(c) ?: continue
                    val homeAway = comp.optString("homeAway", "home")
                    val score = comp.optString("score", "0")
                    val teamObj = comp.optJSONObject("team")
                    val teamId = teamObj?.optString("id", "") ?: ""
                    val location = teamObj?.optString("location", "") ?: ""
                    val teamName = teamObj?.optString("name", "") ?: ""
                    val displayName = location.ifEmpty { teamObj?.optString("displayName", teamName) ?: teamName }
                    val abbrev = teamObj?.optString("abbreviation", "") ?: ""
                    val logo = teamObj?.optString("logo", null)
                    val color = teamObj?.optString("color", null)

                    val team = Team(
                        id = teamId,
                        displayName = displayName.ifEmpty { "Team" },
                        abbreviation = if (abbrev.isNotBlank()) abbrev else displayName.take(3).uppercase(),
                        logo = logo,
                        color = color
                    )

                    if (homeAway == "home") {
                        homeTeam = team
                        homeScore = score
                    } else {
                        awayTeam = team
                        awayScore = score
                    }
                }

                val statusObj = competition.optJSONObject("status")
                val typeObj = statusObj?.optJSONObject("type")
                val state = typeObj?.optString("state", "pre") ?: "pre"
                val shortDetail = typeObj?.optString("shortDetail", "") ?: ""

                val game = Game(
                    id = eventId.ifEmpty { "${target.league}_$i" },
                    sport = target.sport,
                    league = target.displayName,
                    name = eventName,
                    shortName = shortName,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    homeScore = homeScore,
                    awayScore = awayScore,
                    state = state,
                    statusDetail = shortDetail,
                    startTime = dateStr
                )
                list.add(game)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun sortGames(games: List<Game>): List<Game> {
        return games.sortedWith(
            compareBy<Game> { game ->
                when (game.state) {
                    "in" -> 0    // Live games first
                    "pre" -> 1   // Upcoming games second
                    "post" -> 2  // Finals last
                    else -> 3
                }
            }.thenBy { game ->
                game.startTime
            }
        )
    }

    // --- NCAA College Football: AP Top 20 & SEC Focus ---

    fun isSecTeam(teamName: String, location: String = "", abbrev: String = ""): Boolean {
        val lowerSec = secTeams.map { it.lowercase(Locale.ROOT) }
        val n = teamName.lowercase(Locale.ROOT)
        val l = location.lowercase(Locale.ROOT)
        val a = abbrev.lowercase(Locale.ROOT)

        return lowerSec.any { sec ->
            n.contains(sec) || l.contains(sec)
        } || secAbbrs.contains(a)
    }

    suspend fun fetchNcaaFootballAndRankings(): Result<NcaaFootballData> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch AP Poll
            val rankingsUrl = "https://site.api.espn.com/apis/site/v2/sports/football/college-football/rankings"
            val rankRequest = Request.Builder()
                .url(rankingsUrl)
                .header("User-Agent", "MaZzeSportsTV/1.0")
                .build()

            val apTop20 = mutableListOf<ApTeamRank>()
            val apLookup = mutableMapOf<String, Int>()

            client.newCall(rankRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val root = JSONObject(body)
                        val rankingsArr = root.optJSONArray("rankings")
                        val apPoll = rankingsArr?.optJSONObject(0) // AP Top 25
                        val ranksArr = apPoll?.optJSONArray("ranks")

                        if (ranksArr != null) {
                            val limit = minOf(20, ranksArr.length())
                            for (i in 0 until limit) {
                                val rObj = ranksArr.optJSONObject(i) ?: continue
                                val currentRank = rObj.optInt("current", i + 1)
                                val teamObj = rObj.optJSONObject("team") ?: continue
                                val tId = teamObj.optString("id", "")
                                val location = teamObj.optString("location", "")
                                val nickname = teamObj.optString("name", "")
                                val abbrev = teamObj.optString("abbreviation", "")
                                val logo = teamObj.optString("logo", null)
                                    ?: teamObj.optJSONArray("logos")?.optJSONObject(0)?.optString("href", null)

                                val dispName = location.ifEmpty { nickname }
                                val isSec = isSecTeam(nickname, location, abbrev)

                                val item = ApTeamRank(
                                    rank = currentRank,
                                    teamId = tId,
                                    name = nickname,
                                    displayName = dispName,
                                    abbreviation = abbrev,
                                    logoUrl = logo,
                                    isSec = isSec
                                )
                                apTop20.add(item)

                                if (tId.isNotEmpty()) apLookup[tId] = currentRank
                                if (location.isNotEmpty()) apLookup[location.lowercase(Locale.ROOT)] = currentRank
                                if (abbrev.isNotEmpty()) apLookup[abbrev.lowercase(Locale.ROOT)] = currentRank
                                if (nickname.isNotEmpty()) apLookup[nickname.lowercase(Locale.ROOT)] = currentRank
                            }
                        }
                    }
                }
            }

            // 2. Fetch Scoreboard: START is today, END is today plus 7 days in YYYYMMDD
            val cal = Calendar.getInstance()
            val ymdFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
            val startDate = ymdFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 7)
            val endDate = ymdFormat.format(cal.time)

            val primaryScoreboardUrl = "https://site.api.espn.com/apis/site/v2/sports/football/college-football/scoreboard?groups=80&limit=200&dates=$startDate-$endDate"
            var scoreboardBody: String? = null

            try {
                val sbReq = Request.Builder()
                    .url(primaryScoreboardUrl)
                    .header("User-Agent", "MaZzeSportsTV/1.0")
                    .build()
                client.newCall(sbReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        scoreboardBody = resp.body?.string()
                    }
                }
            } catch (e: Exception) {
                // Will try fallback
            }

            // If range returned 400 or failed, fetch default current week scoreboard
            if (scoreboardBody.isNullOrEmpty()) {
                val fallbackUrl = "https://site.api.espn.com/apis/site/v2/sports/football/college-football/scoreboard?groups=80&limit=200"
                val fbReq = Request.Builder()
                    .url(fallbackUrl)
                    .header("User-Agent", "MaZzeSportsTV/1.0")
                    .build()
                client.newCall(fbReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        scoreboardBody = resp.body?.string()
                    }
                }
            }

            val games = mutableListOf<NcaaGame>()
            if (!scoreboardBody.isNullOrEmpty()) {
                val sbRoot = JSONObject(scoreboardBody!!)
                val eventsArr = sbRoot.optJSONArray("events")

                if (eventsArr != null) {
                    for (i in 0 until eventsArr.length()) {
                        val ev = eventsArr.optJSONObject(i) ?: continue
                        val evId = ev.optString("id", "ncaa_$i")
                        val evName = ev.optString("name", "College Football Matchup")
                        val dateStr = ev.optString("date", "")

                        val competitions = ev.optJSONArray("competitions")
                        val comp = competitions?.optJSONObject(0) ?: continue
                        val competitors = comp.optJSONArray("competitors") ?: continue
                        if (competitors.length() < 2) continue

                        var homeTeam = Team()
                        var awayTeam = Team()
                        var homeScore = "0"
                        var awayScore = "0"
                        var homeRank: Int? = null
                        var awayRank: Int? = null
                        var isHomeSec = false
                        var isAwaySec = false

                        for (c in 0 until competitors.length()) {
                            val compObj = competitors.optJSONObject(c) ?: continue
                            val ha = compObj.optString("homeAway", "home")
                            val score = compObj.optString("score", "0")
                            val tObj = compObj.optJSONObject("team") ?: continue

                            val tId = tObj.optString("id", "")
                            val location = tObj.optString("location", "")
                            val tName = tObj.optString("name", "")
                            val disp = location.ifEmpty { tObj.optString("displayName", tName) }
                            val abbrev = tObj.optString("abbreviation", "")
                            val logo = tObj.optString("logo", null)

                            val team = Team(
                                id = tId,
                                displayName = disp,
                                abbreviation = if (abbrev.isNotBlank()) abbrev else disp.take(3).uppercase(),
                                logo = logo
                            )

                            // Lookup AP rank
                            var rank: Int? = apLookup[tId]
                                ?: apLookup[location.lowercase(Locale.ROOT)]
                                ?: apLookup[abbrev.lowercase(Locale.ROOT)]
                                ?: apLookup[tName.lowercase(Locale.ROOT)]

                            if (rank == null) {
                                val cRank = compObj.optJSONObject("curatedRank")?.optInt("current", 99) ?: 99
                                if (cRank in 1..20) rank = cRank
                            }

                            val isSec = isSecTeam(tName, location, abbrev)

                            if (ha == "home") {
                                homeTeam = team
                                homeScore = score
                                homeRank = rank
                                isHomeSec = isSec
                            } else {
                                awayTeam = team
                                awayScore = score
                                awayRank = rank
                                isAwaySec = isSec
                            }
                        }

                        val isSecMatch = isHomeSec || isAwaySec
                        val hasTop20Team = (homeRank != null && homeRank <= 20) || (awayRank != null && awayRank <= 20)

                        // Filter: "Show every game where at least one team is in the AP Top 20 or is an SEC team"
                        if (!isSecMatch && !hasTop20Team) {
                            continue
                        }

                        // Determine sorting tier
                        val bestRank = minOf(homeRank ?: 99, awayRank ?: 99)
                        val tier = when {
                            isSecMatch && hasTop20Team -> 0 // SEC ranked game
                            !isSecMatch && hasTop20Team -> 1 // Other ranked game
                            else -> 2 // SEC unranked game
                        }

                        // Extract TV network
                        var network = ""
                        val broadcasts = comp.optJSONArray("broadcasts")
                        if (broadcasts != null) {
                            for (bIdx in 0 until broadcasts.length()) {
                                val b = broadcasts.optJSONObject(bIdx) ?: continue
                                val media = b.optJSONObject("media")
                                val shortName = media?.optString("shortName", "") ?: ""
                                if (shortName.isNotBlank()) {
                                    network = shortName
                                    break
                                }
                                val names = b.optJSONArray("names")
                                if (names != null && names.length() > 0) {
                                    network = names.optString(0, "")
                                    if (network.isNotBlank()) break
                                }
                            }
                        }
                        if (network.isBlank()) {
                            network = if (isSecMatch) "SECN / ESPN" else "TV"
                        }

                        // Status & Date format
                        val statusObj = comp.optJSONObject("status")
                        val typeObj = statusObj?.optJSONObject("type")
                        val state = typeObj?.optString("state", "pre") ?: "pre"
                        val shortDetail = typeObj?.optString("shortDetail", "") ?: ""

                        val kickoffDisplay = formatKickoffDisplay(dateStr, state, shortDetail, awayScore, homeScore)

                        games.add(
                            NcaaGame(
                                id = evId,
                                name = evName,
                                homeTeam = homeTeam,
                                awayTeam = awayTeam,
                                homeRank = homeRank,
                                awayRank = awayRank,
                                homeScore = homeScore,
                                awayScore = awayScore,
                                state = state,
                                statusDetail = shortDetail,
                                startTime = dateStr,
                                kickoffDisplay = kickoffDisplay,
                                network = network,
                                isSecMatch = isSecMatch,
                                tier = tier,
                                bestRank = bestRank
                            )
                        )
                    }
                }
            }

            // Sort: SEC ranked games first (tier 0), then other ranked games by best rank (tier 1), then SEC unranked (tier 2)
            games.sortWith(
                compareBy<NcaaGame> { it.tier }
                    .thenBy { it.bestRank }
                    .thenBy { it.startTime }
            )

            val updateTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            Result.success(
                NcaaFootballData(
                    apTop20 = apTop20,
                    games = games,
                    lastUpdated = updateTime
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun formatKickoffDisplay(
        dateStr: String,
        state: String,
        shortDetail: String,
        awayScore: String,
        homeScore: String
    ): String {
        return when (state) {
            "in" -> "$awayScore - $homeScore • $shortDetail"
            "post" -> "Final • $awayScore - $homeScore"
            else -> {
                try {
                    val date = isoFormat.parse(dateStr)
                    if (date != null) {
                        val localFormat = SimpleDateFormat("EEE • h:mm a", Locale.getDefault())
                        localFormat.format(date)
                    } else shortDetail.ifEmpty { "Upcoming" }
                } catch (e: Exception) {
                    shortDetail.ifEmpty { "Upcoming" }
                }
            }
        }
    }
}
