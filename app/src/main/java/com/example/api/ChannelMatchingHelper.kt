package com.example.api

import com.example.model.ApTeamRank
import com.example.model.Channel
import com.example.model.Game
import com.example.model.NcaaGame
import java.util.Locale

object ChannelMatchingHelper {
    val networkSynonyms: Map<String, List<String>> = mapOf(
        "espn" to listOf("espn"),
        "espn2" to listOf("espn2", "espn 2"),
        "espnu" to listOf("espnu", "espn u"),
        "espn+" to listOf("espn+", "espn plus", "espn"),
        "espn w" to listOf("espn w", "espnw", "espn"),
        "espnw" to listOf("espn w", "espnw", "espn"),
        "sec" to listOf("sec network", "secn", "sec"),
        "sec network" to listOf("sec network", "secn", "sec"),
        "secn" to listOf("sec network", "secn", "sec"),
        "acc" to listOf("acc network", "accn", "acc"),
        "acc network" to listOf("acc network", "accn", "acc"),
        "accn" to listOf("acc network", "accn", "acc"),
        "btn" to listOf("btn", "big ten network", "big ten", "b1g"),
        "big ten network" to listOf("btn", "big ten network", "big ten", "b1g"),
        "big ten" to listOf("btn", "big ten network", "big ten", "b1g"),
        "b1g" to listOf("btn", "big ten network", "big ten", "b1g"),
        "abc" to listOf("abc"),
        "cbs" to listOf("cbs"),
        "cbs sports" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "cbssn" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "cbs sports network" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "fox" to listOf("fox"),
        "fs1" to listOf("fs1", "fox sports 1", "fox sport 1"),
        "fs2" to listOf("fs2", "fox sports 2", "fox sport 2"),
        "nbc" to listOf("nbc"),
        "nbc sports" to listOf("nbc sports", "nbcsn"),
        "peacock" to listOf("peacock"),
        "paramount" to listOf("paramount+", "paramount", "cbs"),
        "tnt" to listOf("tnt"),
        "tbs" to listOf("tbs"),
        "nfl network" to listOf("nfl network", "nfl"),
        "nfl" to listOf("nfl network", "nfl"),
        "nba tv" to listOf("nba tv", "nba"),
        "nba" to listOf("nba tv", "nba"),
        "golf" to listOf("golf channel", "golf"),
        "mlb network" to listOf("mlb network", "mlb"),
        "nhl network" to listOf("nhl network", "nhl")
    )

    val schoolAliases: Map<String, List<String>> = mapOf(
        "south carolina" to listOf("south carolina", "gamecocks", "scar", "sec network", "secn"),
        "gamecocks" to listOf("south carolina", "gamecocks", "scar"),
        "uconn" to listOf("uconn", "connecticut", "huskies"),
        "connecticut" to listOf("uconn", "connecticut", "huskies"),
        "huskies" to listOf("uconn", "connecticut"),
        "texas" to listOf("texas", "longhorns", "sec network", "secn"),
        "longhorns" to listOf("texas", "longhorns"),
        "lsu" to listOf("lsu", "louisiana state", "tigers", "sec network", "secn"),
        "usc" to listOf("usc", "southern california", "trojans", "big ten", "btn"),
        "trojans" to listOf("usc", "southern california", "trojans"),
        "notre dame" to listOf("notre dame", "fighting irish", "irish", "acc network", "accn"),
        "fighting irish" to listOf("notre dame", "fighting irish"),
        "ucla" to listOf("ucla", "bruins", "big ten", "btn"),
        "bruins" to listOf("ucla", "bruins"),
        "stanford" to listOf("stanford", "cardinal", "acc network", "accn"),
        "cardinal" to listOf("stanford", "cardinal"),
        "iowa" to listOf("iowa", "hawkeyes", "big ten", "btn"),
        "hawkeyes" to listOf("iowa", "hawkeyes"),
        "ohio state" to listOf("ohio state", "buckeyes", "osu", "big ten", "btn"),
        "buckeyes" to listOf("ohio state", "buckeyes"),
        "maryland" to listOf("maryland", "terrapins", "terps", "big ten", "btn"),
        "terrapins" to listOf("maryland", "terrapins", "terps"),
        "nc state" to listOf("nc state", "north carolina state", "wolfpack", "acc network", "accn"),
        "wolfpack" to listOf("nc state", "north carolina state", "wolfpack"),
        "oklahoma" to listOf("oklahoma", "sooners", "ou", "sec network", "secn"),
        "sooners" to listOf("oklahoma", "sooners"),
        "duke" to listOf("duke", "blue devils", "acc network", "accn"),
        "blue devils" to listOf("duke", "blue devils"),
        "kansas state" to listOf("kansas state", "k-state", "wildcats", "ksu"),
        "north carolina" to listOf("north carolina", "tar heels", "unc", "acc network", "accn"),
        "tar heels" to listOf("north carolina", "tar heels", "unc"),
        "tcu" to listOf("tcu", "horned frogs"),
        "horned frogs" to listOf("tcu", "horned frogs"),
        "louisville" to listOf("louisville", "cardinals", "acc network", "accn"),
        "cardinals" to listOf("louisville", "cardinals"),
        "baylor" to listOf("baylor", "bears"),
        "bears" to listOf("baylor", "bears"),
        "tennessee" to listOf("tennessee", "lady vols", "volunteers", "vols", "ut", "sec network", "secn"),
        "volunteers" to listOf("tennessee", "lady vols", "volunteers", "vols"),
        "lady vols" to listOf("tennessee", "lady vols"),
        "kentucky" to listOf("kentucky", "wildcats", "uk", "sec network", "secn"),
        "alabama" to listOf("alabama", "crimson tide", "bama", "sec network", "secn"),
        "crimson tide" to listOf("alabama", "crimson tide", "bama"),
        "georgia" to listOf("georgia", "lady bulldogs", "bulldogs", "uga", "sec network", "secn"),
        "florida" to listOf("florida", "gators", "uf", "sec network", "secn"),
        "gators" to listOf("florida", "gators"),
        "michigan" to listOf("michigan", "wolverines", "big ten", "btn"),
        "wolverines" to listOf("michigan", "wolverines"),
        "indiana" to listOf("indiana", "hoosiers", "big ten", "btn"),
        "hoosiers" to listOf("indiana", "hoosiers"),
        "west virginia" to listOf("west virginia", "mountaineers", "wvu"),
        "ole miss" to listOf("ole miss", "rebels", "mississippi", "sec network", "secn"),
        "iowa state" to listOf("iowa state", "cyclones", "isu"),
        "colorado" to listOf("colorado", "buffaloes", "buffs"),
        "oregon" to listOf("oregon", "ducks", "big ten", "btn"),
        "miami" to listOf("miami", "hurricanes", "canes", "acc network", "accn"),
        "penn state" to listOf("penn state", "nittany lions", "big ten", "btn"),
        "clemson" to listOf("clemson", "tigers", "acc network", "accn"),
        "illinois" to listOf("illinois", "fighting illini", "illini", "big ten", "btn"),
        "kansas" to listOf("kansas", "jayhawks", "ku")
    )

    private val commonGenericWords = setOf(
        "state", "city", "north", "south", "east", "west", "central",
        "club", "team", "united", "real", "tech", "college", "university"
    )

    fun getAliasesForName(name: String): List<String> {
        val lower = name.trim().lowercase(Locale.ROOT)
        val result = mutableSetOf<String>()
        result.add(lower)
        schoolAliases[lower]?.let { result.addAll(it) }
        for ((key, list) in schoolAliases) {
            if (lower.contains(key)) {
                result.addAll(list)
            }
        }
        return result.toList()
    }

    fun findCandidates(
        channels: List<Channel>,
        homeDisplayName: String,
        homeAbbr: String,
        awayDisplayName: String,
        awayAbbr: String,
        network: String,
        leagueOrSport: String = "",
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 8
    ): List<Channel> {
        val available = channels.filter { it.id !in excludeChannelIds }
        if (available.isEmpty()) return emptyList()

        val hName = homeDisplayName.lowercase(Locale.ROOT)
        val aName = awayDisplayName.lowercase(Locale.ROOT)
        val hAbbr = homeAbbr.lowercase(Locale.ROOT)
        val aAbbr = awayAbbr.lowercase(Locale.ROOT)
        val net = network.lowercase(Locale.ROOT)
        val league = leagueOrSport.lowercase(Locale.ROOT)

        val hAliases = getAliasesForName(homeDisplayName)
        val aAliases = getAliasesForName(awayDisplayName)

        val hWords = (hName.split(" ", "-", "/") + hAliases)
            .filter { it.length > 2 && it !in commonGenericWords }
            .distinct()
        val aWords = (aName.split(" ", "-", "/") + aAliases)
            .filter { it.length > 2 && it !in commonGenericWords }
            .distinct()

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

        val isWbb = league.contains("ncaaw") || league.contains("women") || league.contains("wbb")

        val scored = available.map { channel ->
            val cName = channel.name.lowercase(Locale.ROOT)
            val cGroup = channel.groupTitle.lowercase(Locale.ROOT)
            var score = 0
            var hasNetworkMatch = false
            var hasTeamMatch = false

            // Network match check
            for (kw in netKeywords) {
                if (containsToken(cName, kw) || containsToken(cGroup, kw)) {
                    score += 150
                    hasNetworkMatch = true
                    break
                }
            }

            // Team name & alias match check
            for (w in hWords) {
                if (containsToken(cName, w) || containsToken(cGroup, w)) {
                    score += 120
                    hasTeamMatch = true
                }
            }
            for (w in aWords) {
                if (containsToken(cName, w) || containsToken(cGroup, w)) {
                    score += 120
                    hasTeamMatch = true
                }
            }

            // Abbreviations
            if (hAbbr.isNotBlank() && hAbbr.length >= 2 && containsToken(cName, hAbbr)) {
                score += 40
                hasTeamMatch = true
            }
            if (aAbbr.isNotBlank() && aAbbr.length >= 2 && containsToken(cName, aAbbr)) {
                score += 40
                hasTeamMatch = true
            }

            // PREFER channels that match BOTH the team and the network
            if (hasNetworkMatch && hasTeamMatch) {
                score += 300
            }

            // League bonus
            if (league.isNotBlank() && (containsToken(cName, league) || containsToken(cGroup, league))) {
                score += 30
            }

            // NCAAW / Women's basketball match bonus
            if (isWbb) {
                val matchesWbb = containsToken(cName, "ncaaw") || containsToken(cName, "wbb") ||
                        containsToken(cName, "women") || containsToken(cName, "espn w") ||
                        containsToken(cName, "espnw") || containsToken(cGroup, "ncaaw") ||
                        containsToken(cGroup, "women")
                if (matchesWbb) {
                    score += 80
                }
            }

            if (channel.isSports) score += 10
            channel to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        return scored
    }

    private fun containsToken(text: String, token: String): Boolean {
        if (token.isBlank() || text.isBlank()) return false
        return text.contains(token)
    }

    fun findCandidatesForGame(
        game: Game,
        channels: List<Channel>,
        excludeChannelIds: Set<String> = emptySet(),
        limit: Int = 8
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
        limit: Int = 8
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

    fun findGameForTeam(
        team: ApTeamRank,
        isWomens: Boolean,
        allGames: List<Game>,
        ncaaGames: List<NcaaGame> = emptyList()
    ): Game? {
        val ncaaConverted = ncaaGames.map { n ->
            Game(
                id = n.id,
                name = n.name,
                shortName = "${n.awayTeam.abbreviation} @ ${n.homeTeam.abbreviation}",
                sport = "football",
                league = "CFB",
                homeTeam = n.homeTeam,
                awayTeam = n.awayTeam,
                homeScore = n.homeScore,
                awayScore = n.awayScore,
                statusDetail = n.statusDetail.ifBlank { n.kickoffDisplay },
                state = n.state,
                startTime = n.startTime,
                network = n.network
            )
        }

        val pool = (allGames + ncaaConverted).distinctBy {
            it.id.ifBlank { "${it.league}-${it.homeTeam.displayName}-${it.awayTeam.displayName}-${it.startTime}" }
        }

        val tId = team.teamId.lowercase(Locale.ROOT)
        val tDisplay = team.displayName.lowercase(Locale.ROOT)
        val tName = team.name.lowercase(Locale.ROOT)
        val tAbbr = team.abbreviation.lowercase(Locale.ROOT)

        val teamTokens = (getAliasesForName(team.displayName) + getAliasesForName(team.name))
            .toMutableSet()
        if (tDisplay.isNotBlank()) teamTokens.add(tDisplay)
        if (tName.isNotBlank()) teamTokens.add(tName)
        if (tAbbr.isNotBlank() && tAbbr.length >= 2) teamTokens.add(tAbbr)

        val words = (tDisplay.split(" ", "-", "/") + tName.split(" ", "-", "/"))
            .filter { it.length > 2 && it !in commonGenericWords }
        teamTokens.addAll(words)

        val filteredGames = pool.filter { g ->
            val leagueLower = g.league.lowercase(Locale.ROOT)
            val sportLower = g.sport.lowercase(Locale.ROOT)
            val nameLower = g.name.lowercase(Locale.ROOT)
            if (isWomens) {
                leagueLower == "ncaaw" || leagueLower.contains("women") ||
                        sportLower == "basketball" || nameLower.contains("women") ||
                        nameLower.contains("wbb")
            } else {
                leagueLower == "cfb" || sportLower == "football" || leagueLower.contains("college")
            }
        }

        val matchedGames = filteredGames.mapNotNull { g ->
            val hId = g.homeTeam.id.lowercase(Locale.ROOT)
            val aId = g.awayTeam.id.lowercase(Locale.ROOT)
            val hName = g.homeTeam.displayName.lowercase(Locale.ROOT)
            val aName = g.awayTeam.displayName.lowercase(Locale.ROOT)
            val gName = g.name.lowercase(Locale.ROOT)
            val gShort = g.shortName.lowercase(Locale.ROOT)

            var score = 0
            if (tId.isNotBlank() && (hId == tId || aId == tId)) {
                score += 100
            }
            for (token in teamTokens) {
                if (hName.contains(token) || aName.contains(token)) {
                    score += 80
                    break
                }
            }
            for (token in teamTokens) {
                if (gName.contains(token) || gShort.contains(token)) {
                    score += 50
                    break
                }
            }

            if (score > 0) g to score else null
        }

        if (matchedGames.isEmpty()) return null

        return matchedGames
            .sortedWith(
                compareBy<Pair<Game, Int>> {
                    when (it.first.state) {
                        "in" -> 0
                        "pre" -> 1
                        else -> 2
                    }
                }.thenByDescending { it.second }
                    .thenBy { it.first.startTime }
            )
            .map { it.first }
            .firstOrNull()
    }
}
