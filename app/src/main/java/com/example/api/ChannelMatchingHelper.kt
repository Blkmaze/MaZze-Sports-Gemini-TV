package com.example.api

import com.example.model.Channel
import com.example.model.Game
import com.example.model.NcaaGame
import java.util.Locale

object ChannelMatchingHelper {
    private val networkSynonyms: Map<String, List<String>> = mapOf(
        "espn" to listOf("espn"),
        "espn2" to listOf("espn2", "espn 2"),
        "espnu" to listOf("espnu", "espn u"),
        "sec" to listOf("sec network", "secn", "sec"),
        "sec network" to listOf("sec network", "secn", "sec"),
        "secn" to listOf("sec network", "secn", "sec"),
        "abc" to listOf("abc"),
        "cbs" to listOf("cbs"),
        "fox" to listOf("fox"),
        "fs1" to listOf("fs1", "fox sports 1", "fox sport 1"),
        "fs2" to listOf("fs2", "fox sports 2", "fox sport 2"),
        "nbc" to listOf("nbc"),
        "tnt" to listOf("tnt"),
        "tbs" to listOf("tbs"),
        "nfl network" to listOf("nfl network", "nfl"),
        "nfl" to listOf("nfl network", "nfl"),
        "nba tv" to listOf("nba tv", "nba"),
        "nba" to listOf("nba tv", "nba"),
        "cbs sports" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "cbssn" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "nbc sports" to listOf("nbc sports", "nbcsn"),
        "golf" to listOf("golf channel", "golf"),
        "mlb network" to listOf("mlb network", "mlb"),
        "nhl network" to listOf("nhl network", "nhl")
    )

    private val commonGenericWords = setOf(
        "state", "city", "north", "south", "east", "west", "central",
        "club", "team", "united", "real", "tech", "college", "university"
    )

    fun findCandidates(
        channels: List<Channel>,
        homeDisplayName: String,
        homeAbbr: String,
        awayDisplayName: String,
        awayAbbr: String,
        network: String,
        leagueOrSport: String = "",
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 5
    ): List<Channel> {
        val available = channels.filter { it.id !in excludeChannelIds }
        if (available.isEmpty()) return emptyList()

        val hName = homeDisplayName.lowercase(Locale.ROOT)
        val aName = awayDisplayName.lowercase(Locale.ROOT)
        val hAbbr = homeAbbr.lowercase(Locale.ROOT)
        val aAbbr = awayAbbr.lowercase(Locale.ROOT)
        val net = network.lowercase(Locale.ROOT)
        val league = leagueOrSport.lowercase(Locale.ROOT)

        val hWords = hName.split(" ", "-", "/").filter { it.length > 2 && it !in commonGenericWords }
        val aWords = aName.split(" ", "-", "/").filter { it.length > 2 && it !in commonGenericWords }

        val netKeywords = mutableSetOf<String>()
        if (net.isNotBlank()) {
            netKeywords.add(net)
            networkSynonyms[net]?.let { netKeywords.addAll(it) }
            for ((key, list) in networkSynonyms) {
                if (net.contains(key)) {
                    netKeywords.addAll(list)
                }
            }
        }

        val scored = available.map { channel ->
            val cName = channel.name.lowercase(Locale.ROOT)
            val cGroup = channel.groupTitle.lowercase(Locale.ROOT)
            var score = 0

            // Network match is highest confidence
            for (kw in netKeywords) {
                if (containsToken(cName, kw)) {
                    score += 150
                    break
                }
            }

            // Team name match
            for (w in hWords) {
                if (containsToken(cName, w)) score += 80
            }
            for (w in aWords) {
                if (containsToken(cName, w)) score += 80
            }

            // Abbreviations (require boundary)
            if (hAbbr.isNotBlank() && hAbbr.length >= 2 && containsToken(cName, hAbbr)) {
                score += 40
            }
            if (aAbbr.isNotBlank() && aAbbr.length >= 2 && containsToken(cName, aAbbr)) {
                score += 40
            }

            // League bonus
            if (league.isNotBlank() && (containsToken(cName, league) || containsToken(cGroup, league))) {
                score += 20
            }

            if (channel.isSports) score += 5
            channel to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        return scored
    }

    private fun containsToken(text: String, token: String): Boolean {
        if (token.isBlank() || text.isBlank()) return false
        val regex = Regex("(?:^|[^a-zA-Z0-9])${Regex.escape(token)}(?:$|[^a-zA-Z0-9])")
        return regex.containsMatchIn(text) || text.contains(token)
    }

    fun findCandidatesForGame(
        game: Game,
        channels: List<Channel>,
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 5
    ): List<Channel> {
        return findCandidates(
            channels = channels,
            homeDisplayName = game.homeTeam.displayName,
            homeAbbr = game.homeTeam.abbreviation,
            awayDisplayName = game.awayTeam.displayName,
            awayAbbr = game.awayTeam.abbreviation,
            network = game.network,
            leagueOrSport = game.league.ifBlank { game.sport },
            excludeChannelIds = excludeChannelIds,
            limit = limit
        )
    }

    fun findCandidatesForNcaaGame(
        game: NcaaGame,
        channels: List<Channel>,
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 5
    ): List<Channel> {
        return findCandidates(
            channels = channels,
            homeDisplayName = game.homeTeam.displayName,
            homeAbbr = game.homeTeam.abbreviation,
            awayDisplayName = game.awayTeam.displayName,
            awayAbbr = game.awayTeam.abbreviation,
            network = game.network,
            leagueOrSport = "ncaa football",
            excludeChannelIds = excludeChannelIds,
            limit = limit
        )
    }
}
