package com.example.api

import com.example.model.Channel
import com.example.model.Game
import com.example.model.NcaaGame
import java.util.Locale

object ChannelMatchingHelper {

    // Major sports networks list
    private val networkSynonyms = mapOf(
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

    /**
     * Finds a ranked list of up to 3 candidate channels for a given game.
     * Tries:
     * 1. Team names and abbreviations in channel names (and group title)
     * 2. Broadcast network matched against channel name AND group title
     * 3. League / sport keywords in channel name AND group title
     * 4. Remaining sports channels as fallback candidates
     */
    fun findCandidates(
        channels: List<Channel>,
        homeDisplayName: String,
        homeAbbr: String,
        awayDisplayName: String,
        awayAbbr: String,
        network: String,
        leagueOrSport: String = "",
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 3
    ): List<Channel> {
        val available = channels.filter { it.id !in excludeChannelIds }
        if (available.isEmpty()) return emptyList()

        val scored = mutableListOf<Pair<Channel, Int>>()

        val hName = homeDisplayName.lowercase(Locale.ROOT)
        val aName = awayDisplayName.lowercase(Locale.ROOT)
        val hAbbr = homeAbbr.lowercase(Locale.ROOT)
        val aAbbr = awayAbbr.lowercase(Locale.ROOT)
        val net = network.lowercase(Locale.ROOT)
        val league = leagueOrSport.lowercase(Locale.ROOT)

        // Split team names into individual significant words (> 3 chars)
        val hWords = hName.split(" ", "-", "/").map { it.trim() }.filter { it.length > 3 && it !in commonGenericWords }
        val aWords = aName.split(" ", "-", "/").map { it.trim() }.filter { it.length > 3 && it !in commonGenericWords }

        // Determine all target network tokens to search
        val netKeywords = mutableSetOf<String>()
        if (net.isNotBlank() && net != "tv") {
            val rawParts = net.split("/", ",", "&", "|", "+").map { it.trim() }.filter { it.isNotBlank() }
            for (p in rawParts) {
                netKeywords.add(p)
                for ((key, synonyms) in networkSynonyms) {
                    if (p.contains(key) || key.contains(p)) {
                        netKeywords.addAll(synonyms)
                    }
                }
            }
        }

        for (ch in available) {
            val cName = ch.name.lowercase(Locale.ROOT)
            val cGrp = ch.groupTitle.lowercase(Locale.ROOT)
            var score = 0

            // 1. Team abbreviations (high priority if token matches)
            if (hAbbr.length >= 2 && (containsToken(cName, hAbbr) || containsToken(cGrp, hAbbr))) {
                score += 120
            }
            if (aAbbr.length >= 2 && (containsToken(cName, aAbbr) || containsToken(cGrp, aAbbr))) {
                score += 120
            }

            // Full team display name
            if (hName.length > 3 && (cName.contains(hName) || cGrp.contains(hName))) {
                score += 150
            }
            if (aName.length > 3 && (cName.contains(aName) || cGrp.contains(aName))) {
                score += 150
            }

            // Team words
            for (word in hWords) {
                if (cName.contains(word)) score += 70
                else if (cGrp.contains(word)) score += 30
            }
            for (word in aWords) {
                if (cName.contains(word)) score += 70
                else if (cGrp.contains(word)) score += 30
            }

            // 2. Broadcast network (e.g. ESPN, ESPN2, SEC Network, ABC, CBS, FOX, FS1, NBC, TNT, TBS...)
            for (kw in netKeywords) {
                if (kw.isNotBlank()) {
                    val nameMatch = containsToken(cName, kw)
                    val grpMatch = containsToken(cGrp, kw)
                    if (nameMatch) {
                        score += 90
                    } else if (grpMatch) {
                        score += 50
                    }
                }
            }

            // 3. League or sport match
            if (league.isNotBlank()) {
                if (cName.contains(league)) score += 40
                if (cGrp.contains(league)) score += 25
            }

            // Bonus for sports channels
            if (ch.isSports) {
                score += 5
            }

            // Only consider channels with a meaningful match score
            if (score > 0) {
                scored.add(Pair(ch, score))
            }
        }

        val sorted = scored.sortedByDescending { it.second }.map { it.first }.distinctBy { it.id }
        if (sorted.isNotEmpty()) {
            return sorted.take(limit)
        }

        return emptyList()
    }

    private fun containsToken(text: String, token: String): Boolean {
        if (token.length <= 3) {
            // Check word boundary or surrounding spaces/symbols to avoid false positives (e.g., "fox" in "foxes")
            val regex = Regex("(^|[^a-zA-Z0-9])${Regex.escape(token)}([^a-zA-Z0-9]|$)", RegexOption.IGNORE_CASE)
            return regex.containsMatchIn(text)
        }
        return text.contains(token)
    }

    private val commonGenericWords = setOf(
        "state", "city", "north", "south", "east", "west", "central",
        "club", "team", "united", "real", "tech", "college", "university"
    )

    fun findCandidatesForGame(
        game: Game,
        channels: List<Channel>,
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 3
    ): List<Channel> {
        return findCandidates(
            channels = channels,
            homeDisplayName = game.homeTeam.displayName,
            homeAbbr = game.homeTeam.abbreviation,
            awayDisplayName = game.awayTeam.displayName,
            awayAbbr = game.awayTeam.abbreviation,
            network = game.network,
            leagueOrSport = "${game.league} ${game.sport}",
            excludeChannelIds = excludeChannelIds,
            limit = limit
        )
    }

    fun findCandidatesForNcaaGame(
        game: NcaaGame,
        channels: List<Channel>,
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 3
    ): List<Channel> {
        return findCandidates(
            channels = channels,
            homeDisplayName = game.homeTeam.displayName,
            homeAbbr = game.homeTeam.abbreviation,
            awayDisplayName = game.awayTeam.displayName,
            awayAbbr = game.awayTeam.abbreviation,
            network = game.network,
            leagueOrSport = "ncaa football cfb ${if (game.isSecMatch) "sec" else ""}",
            excludeChannelIds = excludeChannelIds,
            limit = limit
        )
    }
}
