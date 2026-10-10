package com.example.api

import com.example.model.Game
import com.example.model.Team
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
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

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
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
                // Log or continue with other leagues
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
                    val displayName = teamObj?.optString("displayName", "Team") ?: "Team"
                    val abbrev = teamObj?.optString("abbreviation", "") ?: ""
                    val logo = teamObj?.optString("logo", null)
                    val color = teamObj?.optString("color", null)

                    val team = Team(
                        id = teamId,
                        displayName = displayName,
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
                val state = typeObj?.optString("state", "pre") ?: "pre" // "in", "pre", "post"
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
}
