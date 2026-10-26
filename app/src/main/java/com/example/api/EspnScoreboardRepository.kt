package com.example.api

import com.example.model.ApTeamRank
import com.example.model.Game
import com.example.model.NcaaFootballData
import com.example.model.NcaaGame
import com.example.model.Team
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class EspnScoreboardRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    data class LeagueTarget(val sport: String, val league: String, val displayName: String)

    val leagues: List<LeagueTarget> = listOf(
        LeagueTarget("football", "nfl", "NFL"),
        LeagueTarget("baseball", "mlb", "MLB"),
        LeagueTarget("basketball", "wnba", "WNBA"),
        LeagueTarget("basketball", "nba", "NBA"),
        LeagueTarget("hockey", "nhl", "NHL"),
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
        try {
            val allGames = mutableListOf<Game>()
            for (target in leagues) {
                try {
                    val url = "https://site.api.espn.com/apis/site/v2/sports/${target.sport}/${target.league}/scoreboard"
                    val req = Request.Builder().url(url).header("User-Agent", "MaZzeSportsTV/1.0").build()
                    client.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            allGames.addAll(parseEspnScoreboard(body, target))
                        }
                    }
                } catch (e: Throwable) {
                    // Safe swallow per league
                }
            }
            Result.success(sortGames(allGames))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    val weekLeagues: List<LeagueTarget> = listOf(
        LeagueTarget("football", "nfl", "NFL"),
        LeagueTarget("baseball", "mlb", "MLB"),
        LeagueTarget("basketball", "wnba", "WNBA"),
        LeagueTarget("basketball", "nba", "NBA"),
        LeagueTarget("hockey", "nhl", "NHL")
    )

    suspend fun fetchWeekScoreboards(): Result<List<Game>> = withContext(Dispatchers.IO) {
        try {
            val allGames = mutableListOf<Game>()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val ymd = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val startDate = ymd.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 7)
            val endDate = ymd.format(cal.time)
            val dateRange = "$startDate-$endDate"

            for (target in weekLeagues) {
                try {
                    val url = "https://site.api.espn.com/apis/site/v2/sports/${target.sport}/${target.league}/scoreboard?dates=$dateRange"
                    val req = Request.Builder().url(url).header("User-Agent", "MaZzeSportsTV/1.0").build()
                    client.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            allGames.addAll(parseEspnScoreboard(body, target))
                        }
                    }
                } catch (e: Throwable) {
                    // Safe swallow per league
                }
            }
            Result.success(allGames)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun parseEspnScoreboard(jsonString: String, target: LeagueTarget): List<Game> {
        val games = mutableListOf<Game>()
        try {
            val root = JSONObject(jsonString)
            val events = root.optJSONArray("events") ?: return games
            for (i in 0 until events.length()) {
                val event = events.optJSONObject(i) ?: continue
                val id = event.optString("id", "")
                val name = event.optString("name", "")
                val shortName = event.optString("shortName", "")
                val dateStr = event.optString("date", "")

                val competitions = event.optJSONArray("competitions")
                val comp = competitions?.optJSONObject(0) ?: continue
                val competitors = comp.optJSONArray("competitors") ?: continue

                var homeTeam = Team()
                var awayTeam = Team()
                var homeScore = ""
                var awayScore = ""

                for (c in 0 until competitors.length()) {
                    val compObj = competitors.optJSONObject(c) ?: continue
                    val homeAway = compObj.optString("homeAway", "")
                    val score = compObj.optString("score", "0")
                    val teamObj = compObj.optJSONObject("team")
                    val tId = teamObj?.optString("id", "") ?: ""
                    val tName = teamObj?.optString("displayName", "") ?: ""
                    val tAbbr = teamObj?.optString("abbreviation", "") ?: ""
                    val tLogo = teamObj?.optString("logo", "") ?: ""
                    val tColor = teamObj?.optString("color", "") ?: ""
                    val team = Team(tId, tName, tAbbr, tLogo, tColor)

                    if (homeAway == "home") {
                        homeTeam = team
                        homeScore = score
                    } else {
                        awayTeam = team
                        awayScore = score
                    }
                }

                val statusObj = comp.optJSONObject("status")
                val typeObj = statusObj?.optJSONObject("type")
                val state = typeObj?.optString("state", "pre") ?: "pre"
                val statusDetail = typeObj?.optString("shortDetail", "") ?: ""
                val network = extractNetwork(comp)

                games.add(
                    Game(
                        id = id,
                        name = name,
                        shortName = shortName,
                        sport = target.sport,
                        league = target.displayName,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        statusDetail = statusDetail,
                        state = state,
                        startTime = dateStr,
                        network = network
                    )
                )
            }
        } catch (e: Throwable) {
            // Safe ignore
        }
        return games
    }

    private fun extractNetwork(comp: JSONObject): String {
        return try {
            val broadcasts = comp.optJSONArray("broadcasts")
            if (broadcasts != null && broadcasts.length() > 0) {
                val b = broadcasts.optJSONObject(0)
                val names = b?.optJSONArray("names")
                if (names != null && names.length() > 0) {
                    return names.optString(0, "")
                }
            }
            ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun sortGames(games: List<Game>): List<Game> {
        return games.sortedWith(
            compareBy<Game> { game ->
                when (game.state) {
                    "in" -> 0
                    "pre" -> 1
                    "post" -> 2
                    else -> 3
                }
            }.thenBy { it.startTime }
        )
    }

    fun isSecTeam(teamName: String, location: String = "", abbrev: String = ""): Boolean {
        val n = teamName.lowercase(Locale.ROOT)
        val l = location.lowercase(Locale.ROOT)
        val a = abbrev.lowercase(Locale.ROOT)
        val lowerSec = secTeams.map { it.lowercase(Locale.ROOT) }
        val matchesSec = lowerSec.any { n.contains(it) || l.contains(it) }
        return matchesSec || secAbbrs.contains(a)
    }

    suspend fun fetchNcaaFootballAndRankings(): Result<NcaaFootballData> = withContext(Dispatchers.IO) {
        try {
            val rankings = mutableListOf<ApTeamRank>()
            val rankMap = mutableMapOf<String, Int>()

            // 1. Fetch AP Top 25
            try {
                val rankUrl = "https://site.api.espn.com/apis/site/v2/sports/football/college-football/rankings"
                val req = Request.Builder().url(rankUrl).header("User-Agent", "MaZzeSportsTV/1.0").build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val root = JSONObject(body)
                        val rankingsArr = root.optJSONArray("rankings")
                        if (rankingsArr != null && rankingsArr.length() > 0) {
                            val poll = rankingsArr.optJSONObject(0)
                            val ranks = poll?.optJSONArray("ranks")
                            if (ranks != null) {
                                for (i in 0 until ranks.length()) {
                                    val rObj = ranks.optJSONObject(i) ?: continue
                                    val currentRank = rObj.optInt("current", i + 1)
                                    val teamObj = rObj.optJSONObject("team") ?: continue
                                    val tId = teamObj.optString("id", "")
                                    val tName = teamObj.optString("name", "")
                                    val tDisplay = teamObj.optString("nickname", tName)
                                    val tAbbr = teamObj.optString("abbreviation", "")
                                    val tLogo = teamObj.optString("logo", "")
                                    val isSec = isSecTeam(tName, tDisplay, tAbbr)

                                    if (currentRank <= 25) {
                                        rankMap[tId] = currentRank
                                        rankMap[tName.lowercase(Locale.ROOT)] = currentRank
                                        rankMap[tAbbr.lowercase(Locale.ROOT)] = currentRank
                                        if (currentRank <= 20) {
                                            rankings.add(
                                                ApTeamRank(
                                                    rank = currentRank,
                                                    name = tName,
                                                    displayName = tDisplay,
                                                    abbreviation = tAbbr,
                                                    logoUrl = tLogo,
                                                    isSec = isSec,
                                                    teamId = tId
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                // Ignore failure
            }

            // 2. Fetch College Football scoreboard
            val games = mutableListOf<NcaaGame>()
            try {
                val sbUrl = "https://site.api.espn.com/apis/site/v2/sports/football/college-football/scoreboard"
                val req = Request.Builder().url(sbUrl).header("User-Agent", "MaZzeSportsTV/1.0").build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val root = JSONObject(body)
                        val events = root.optJSONArray("events")
                        if (events != null) {
                            for (i in 0 until events.length()) {
                                val event = events.optJSONObject(i) ?: continue
                                val id = event.optString("id", "")
                                val name = event.optString("name", "")
                                val dateStr = event.optString("date", "")

                                val comp = event.optJSONArray("competitions")?.optJSONObject(0) ?: continue
                                val competitors = comp.optJSONArray("competitors") ?: continue

                                var homeTeam = Team()
                                var awayTeam = Team()
                                var homeScore = ""
                                var awayScore = ""

                                for (c in 0 until competitors.length()) {
                                    val compObj = competitors.optJSONObject(c) ?: continue
                                    val homeAway = compObj.optString("homeAway", "")
                                    val score = compObj.optString("score", "0")
                                    val teamObj = compObj.optJSONObject("team")
                                    val tId = teamObj?.optString("id", "") ?: ""
                                    val tName = teamObj?.optString("displayName", "") ?: ""
                                    val tAbbr = teamObj?.optString("abbreviation", "") ?: ""
                                    val tLogo = teamObj?.optString("logo", "") ?: ""
                                    val tColor = teamObj?.optString("color", "") ?: ""
                                    val team = Team(tId, tName, tAbbr, tLogo, tColor)

                                    if (homeAway == "home") {
                                        homeTeam = team
                                        homeScore = score
                                    } else {
                                        awayTeam = team
                                        awayScore = score
                                    }
                                }

                                val statusObj = comp.optJSONObject("status")
                                val typeObj = statusObj?.optJSONObject("type")
                                val state = typeObj?.optString("state", "pre") ?: "pre"
                                val shortDetail = typeObj?.optString("shortDetail", "") ?: ""
                                val network = extractNetwork(comp)

                                val homeRank = rankMap[homeTeam.id] ?: rankMap[homeTeam.displayName.lowercase(Locale.ROOT)]
                                val awayRank = rankMap[awayTeam.id] ?: rankMap[awayTeam.displayName.lowercase(Locale.ROOT)]

                                val bestRank = minOf(homeRank ?: 999, awayRank ?: 999)
                                val isSecMatch = isSecTeam(homeTeam.displayName, abbrev = homeTeam.abbreviation) ||
                                        isSecTeam(awayTeam.displayName, abbrev = awayTeam.abbreviation)

                                val tier = when {
                                    homeRank != null && awayRank != null && homeRank <= 25 && awayRank <= 25 -> 1
                                    homeRank != null && homeRank <= 20 -> 2
                                    awayRank != null && awayRank <= 20 -> 2
                                    isSecMatch -> 3
                                    else -> 4
                                }

                                val kickoffDisplay = formatKickoffDisplay(dateStr, state, shortDetail, awayScore, homeScore)

                                games.add(
                                    NcaaGame(
                                        id = id,
                                        name = name,
                                        homeTeam = homeTeam,
                                        awayTeam = awayTeam,
                                        homeRank = homeRank,
                                        awayRank = awayRank,
                                        homeScore = homeScore,
                                        awayScore = awayScore,
                                        statusDetail = shortDetail,
                                        state = state,
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
                }
            } catch (e: Throwable) {
                // Ignore failure
            }

            val sortedGames = games.sortedWith(
                compareBy<NcaaGame> { it.tier }
                    .thenBy { it.bestRank }
                    .thenBy {
                        when (it.state) {
                            "in" -> 0
                            "pre" -> 1
                            else -> 2
                        }
                    }
            )

            val nowFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            Result.success(NcaaFootballData(rankings, sortedGames, nowFormatted))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun formatKickoffDisplay(dateStr: String, state: String, shortDetail: String, awayScore: String, homeScore: String): String {
        if (state == "in") return "$awayScore - $homeScore • $shortDetail"
        if (state == "post") return "Final • $awayScore - $homeScore"
        return try {
            val date = isoFormat.parse(dateStr)
            if (date != null) {
                val localFormat = SimpleDateFormat("EEE • h:mm a", Locale.getDefault())
                localFormat.format(date)
            } else {
                shortDetail.ifBlank { "Upcoming" }
            }
        } catch (e: Throwable) {
            shortDetail.ifBlank { "Upcoming" }
        }
    }
}
