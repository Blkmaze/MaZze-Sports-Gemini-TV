package com.example.api

import com.example.model.ApTeamRank
import com.example.model.Channel
import com.example.model.Game
import com.example.model.NcaaGame
import java.util.Locale

object ChannelMatchingHelper {

    val networkSynonyms: Map<String, List<String>> = mapOf(
        "espn" to listOf("espn"),
        "espn2" to listOf("espn2", "espn 2", "espn ii"),
        "espnu" to listOf("espnu", "espn u"),
        "espnews" to listOf("espnews", "espn news"),
        "espn news" to listOf("espnews", "espn news"),
        "espn+" to listOf("espn+", "espn plus", "espn +"),
        "espn plus" to listOf("espn+", "espn plus", "espn +"),
        "espn w" to listOf("espn w", "espnw"),
        "espnw" to listOf("espn w", "espnw"),
        "espn3" to listOf("espn3", "espn 3"),
        "sec" to listOf("sec network", "secn", "sec net", "sec+", "sec network+", "secn+", "sec"),
        "sec network" to listOf("sec network", "secn", "sec net", "sec+", "sec network+", "secn+", "sec"),
        "secn" to listOf("sec network", "secn", "sec net", "sec+", "sec network+", "secn+", "sec"),
        "sec+" to listOf("sec+", "sec network+", "secn+", "sec network", "secn"),
        "sec network+" to listOf("sec+", "sec network+", "secn+", "sec network", "secn"),
        "acc" to listOf("acc network", "accn", "accnx", "acc+", "acc network+", "acc"),
        "acc network" to listOf("acc network", "accn", "accnx", "acc+", "acc network+", "acc"),
        "accn" to listOf("acc network", "accn", "accnx", "acc+", "acc network+", "acc"),
        "accnx" to listOf("accnx", "acc network", "accn", "acc+"),
        "btn" to listOf("btn", "big ten network", "big ten", "b1g", "b1g+"),
        "big ten network" to listOf("btn", "big ten network", "big ten", "b1g", "b1g+"),
        "big ten" to listOf("btn", "big ten network", "big ten", "b1g", "b1g+"),
        "b1g" to listOf("btn", "big ten network", "big ten", "b1g", "b1g+"),
        "abc" to listOf(
            "abc", "abc affiliate", "local abc", "abc east", "abc west", "abc hd",
            // Major & college-market ABC local affiliate call signs
            "wsb", "wabc", "kabc", "wls", "wpvi", "wcvb", "wfaa", "ktrk", "wjla",
            "wxyz", "kgo", "komo", "knxv", "wfts", "wplg", "wftv", "wsoc", "wtvd",
            "wmar", "kstp", "kmgh", "wtae", "katu", "kxtv", "kdnl", "wrtv", "wkrn",
            "wsyx", "wcpo", "wews", "kmbc", "wisn", "ksat", "kvue", "wgno", "wbma",
            "wolo", "wjbf", "wtvc", "wate", "wbrz", "wtxl", "wcjb", "ktul", "koco",
            "wset", "wric", "wvec", "khbs", "waay", "wear", "whtm"
        ),
        "cbs" to listOf(
            "cbs", "cbs affiliate", "local cbs", "cbs east", "cbs west", "cbs hd",
            // Major & college-market CBS local affiliate call signs
            "wcbs", "kcbs", "wbbm", "kyw", "wbz", "ktvt", "khou", "wusa", "wwj",
            "wanf", "wgcl", "kpix", "kiro", "kpho", "wtsp", "wfor", "wkmg", "wbtv",
            "wncn", "wjz", "wcco", "kcnc", "kdka", "koin", "kovr", "kmov", "wttv",
            "wtvf", "wbns", "wkrc", "woio", "kctv", "wdjt", "kens", "keye", "wwl",
            "wiat", "wltx", "wrdw", "wvlt", "wdef", "wafb", "kthv", "kfsm", "kwtv",
            "kotv", "wdbj", "wtvr", "wtkr", "wctv", "wgfl", "wkrg", "whnt", "wcbi",
            "wjtv", "krcg", "wkyt"
        ),
        "cbs sports" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "cbssn" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "cbs sports network" to listOf("cbs sports", "cbssn", "cbs sports network"),
        "fox" to listOf(
            "fox", "fox affiliate", "local fox", "fox east", "fox west", "fox hd",
            // Major & college-market FOX local affiliate call signs
            "wnyw", "kttv", "wfld", "wtxf", "wfxt", "kdfw", "kriv", "wttg", "wjbk",
            "waga", "ktvu", "kcpq", "ksaz", "wtvt", "wsvn", "wofl", "wjzy", "wraz",
            "wbff", "kmsp", "kdvr", "wpgh", "kptv", "ktxl", "ktvi", "wxin", "wztv",
            "wtte", "wxix", "wjw", "wdaf", "witi", "kabb", "ktbc", "wvue", "wbrc",
            "wach", "wfxg", "wtnz", "wgxa", "wgmb", "klrt", "kfta", "kokh", "koki",
            "wfxr", "wrlh", "wvbt", "wtlh", "wogx", "wala", "wzdx", "wdbd", "wdky"
        ),
        "fs1" to listOf("fs1", "fox sports 1", "fox sport 1"),
        "fs2" to listOf("fs2", "fox sports 2", "fox sport 2"),
        "nbc" to listOf(
            "nbc", "nbc affiliate", "local nbc", "nbc east", "nbc west", "nbc hd",
            // Major & college-market NBC local affiliate call signs
            "wnbc", "knbc", "wmaq", "wcau", "wbts", "kxas", "kprc", "wrc", "wdiv",
            "wxia", "kntv", "king", "kpnx", "wfla", "wtvj", "wesh", "wcnc", "wral",
            "wbal", "kare", "kusa", "wpxi", "kgw", "kcra", "ksdk", "wthr", "wsmv",
            "wcmh", "wlwt", "wkyc", "kshb", "wtmj", "woai", "kxan", "wdsu", "wvtm",
            "wis", "wbir", "wrcb", "wvla", "kark", "knwa", "kfor", "kjrh", "wsls",
            "wwbt", "wavy", "wtwc", "wnbw", "waff", "wlbt", "komu", "wlex"
        ),
        "nbc sports" to listOf("nbc sports", "nbcsn"),
        "nbcsn" to listOf("nbc sports", "nbcsn"),
        "peacock" to listOf("peacock"),
        "paramount" to listOf("paramount+", "paramount plus", "paramount"),
        "paramount+" to listOf("paramount+", "paramount plus", "paramount"),
        "tnt" to listOf("tnt"),
        "tbs" to listOf("tbs"),
        "trutv" to listOf("trutv", "tru tv"),
        "cw" to listOf("the cw", "cw"),
        "the cw" to listOf("the cw", "cw"),
        "usa" to listOf("usa network"),
        "usa network" to listOf("usa network"),
        "nfl network" to listOf("nfl network", "nfl net"),
        "nfl" to listOf("nfl network", "nfl net"),
        "nba tv" to listOf("nba tv", "nbatv"),
        "golf" to listOf("golf channel"),
        "golf channel" to listOf("golf channel"),
        "mlb network" to listOf("mlb network", "mlbn"),
        "nhl network" to listOf("nhl network", "nhln"),
        "prime video" to listOf("prime video", "amazon prime"),
        "apple tv" to listOf("apple tv", "apple tv+"),
        "longhorn network" to listOf("longhorn network", "lhn")
    )

    // School-to-school aliases and specific team nicknames (NO TV network names, NO ambiguous 2-letter codes)
    val schoolAliases: Map<String, List<String>> = mapOf(
        "south carolina" to listOf("south carolina", "gamecocks", "south carolina gamecocks"),
        "uconn" to listOf("uconn", "connecticut", "uconn huskies", "connecticut huskies"),
        "connecticut" to listOf("uconn", "connecticut", "uconn huskies", "connecticut huskies"),
        "texas" to listOf("texas", "longhorns", "texas longhorns"),
        "lsu" to listOf("lsu", "louisiana state", "lsu tigers"),
        "usc" to listOf("usc", "southern california", "usc trojans", "southern california trojans"),
        "southern california" to listOf("usc", "southern california", "usc trojans", "southern california trojans"),
        "notre dame" to listOf("notre dame", "fighting irish", "notre dame fighting irish"),
        "ucla" to listOf("ucla", "ucla bruins"),
        "stanford" to listOf("stanford", "stanford cardinal"),
        "iowa" to listOf("iowa", "hawkeyes", "iowa hawkeyes"),
        "ohio state" to listOf("ohio state", "buckeyes", "ohio state buckeyes"),
        "maryland" to listOf("maryland", "terrapins", "terps", "maryland terrapins"),
        "nc state" to listOf("nc state", "north carolina state", "wolfpack", "nc state wolfpack"),
        "north carolina state" to listOf("nc state", "north carolina state", "wolfpack"),
        "oklahoma" to listOf("oklahoma", "sooners", "oklahoma sooners"),
        "duke" to listOf("duke", "blue devils", "duke blue devils"),
        "kansas state" to listOf("kansas state", "k-state", "kansas state wildcats"),
        "north carolina" to listOf("north carolina", "tar heels", "unc", "north carolina tar heels"),
        "tcu" to listOf("tcu", "horned frogs", "tcu horned frogs"),
        "louisville" to listOf("louisville", "louisville cardinals"),
        "baylor" to listOf("baylor", "baylor bears"),
        "tennessee" to listOf("tennessee", "lady vols", "volunteers", "tennessee volunteers"),
        "kentucky" to listOf("kentucky", "kentucky wildcats"),
        "alabama" to listOf("alabama", "crimson tide", "alabama crimson tide"),
        "georgia" to listOf("georgia", "lady bulldogs", "georgia bulldogs", "uga"),
        "florida" to listOf("florida", "florida gators"),
        "michigan" to listOf("michigan", "wolverines", "michigan wolverines"),
        "indiana" to listOf("indiana", "hoosiers", "indiana hoosiers"),
        "west virginia" to listOf("west virginia", "mountaineers", "wvu"),
        "ole miss" to listOf("ole miss", "mississippi", "ole miss rebels"),
        "mississippi" to listOf("ole miss", "mississippi", "ole miss rebels"),
        "iowa state" to listOf("iowa state", "cyclones", "iowa state cyclones"),
        "colorado" to listOf("colorado", "colorado buffaloes"),
        "oregon" to listOf("oregon", "oregon ducks"),
        "miami" to listOf("miami", "miami hurricanes"),
        "penn state" to listOf("penn state", "nittany lions", "penn state nittany lions"),
        "clemson" to listOf("clemson", "clemson tigers"),
        "illinois" to listOf("illinois", "fighting illini", "illini"),
        "kansas" to listOf("kansas", "jayhawks", "kansas jayhawks"),
        "texas a&m" to listOf("texas a&m", "texas a&m aggies", "tamu"),
        "texas tech" to listOf("texas tech", "red raiders", "texas tech red raiders"),
        "michigan state" to listOf("michigan state", "spartans", "michigan state spartans"),
        "florida state" to listOf("florida state", "seminoles", "fsu", "florida state seminoles"),
        "oklahoma state" to listOf("oklahoma state", "oklahoma state cowboys"),
        "georgia tech" to listOf("georgia tech", "yellow jackets", "georgia tech yellow jackets"),
        "virginia tech" to listOf("virginia tech", "hokies", "virginia tech hokies"),
        "vanderbilt" to listOf("vanderbilt", "commodores"),
        "arkansas" to listOf("arkansas", "razorbacks"),
        "auburn" to listOf("auburn", "auburn tigers"),
        "missouri" to listOf("missouri", "mizzou", "missouri tigers"),
        "mississippi state" to listOf("mississippi state", "mississippi state bulldogs"),
        "pittsburgh" to listOf("pittsburgh", "pitt", "pitt panthers"),
        "pitt" to listOf("pittsburgh", "pitt", "pitt panthers"),
        "smu" to listOf("smu", "southern methodist", "smu mustangs"),
        "ucf" to listOf("ucf", "central florida", "ucf knights"),
        "byu" to listOf("byu", "brigham young", "byu cougars"),
        "utah" to listOf("utah", "utah utes"),
        "wisconsin" to listOf("wisconsin", "badgers"),
        "nebraska" to listOf("nebraska", "cornhuskers", "huskers"),
        "purdue" to listOf("purdue", "boilermakers"),
        "minnesota" to listOf("minnesota", "golden gophers"),
        "washington" to listOf("washington", "washington huskies"),
        "arizona" to listOf("arizona", "arizona wildcats"),
        "arizona state" to listOf("arizona state", "sun devils"),
        "syracuse" to listOf("syracuse", "syracuse orange"),
        "wake forest" to listOf("wake forest", "demon deacons"),
        "virginia" to listOf("virginia", "cavaliers", "uva"),
        "boise state" to listOf("boise state", "boise state broncos"),
        "notre dame fighting irish" to listOf("notre dame", "fighting irish")
    )

    // Unambiguous single-school mascots safe for exact team matching
    private val uniqueMascotAliases: Map<String, String> = mapOf(
        "south carolina" to "gamecocks",
        "alabama" to "crimson tide",
        "notre dame" to "fighting irish",
        "north carolina" to "tar heels",
        "iowa" to "hawkeyes",
        "ohio state" to "buckeyes",
        "michigan" to "wolverines",
        "oklahoma" to "sooners",
        "tcu" to "horned frogs",
        "texas" to "longhorns",
        "penn state" to "nittany lions",
        "indiana" to "hoosiers",
        "duke" to "blue devils",
        "kansas" to "jayhawks",
        "maryland" to "terrapins",
        "tennessee" to "lady vols",
        "georgia" to "lady bulldogs",
        "nebraska" to "cornhuskers",
        "purdue" to "boilermakers",
        "florida state" to "seminoles",
        "georgia tech" to "yellow jackets",
        "illinois" to "fighting illini",
        "wake forest" to "demon deacons",
        "arizona state" to "sun devils",
        "vanderbilt" to "commodores",
        "arkansas" to "razorbacks",
        "west virginia" to "mountaineers",
        "virginia tech" to "hokies",
        "nc state" to "wolfpack",
        "texas tech" to "red raiders"
    )

    // Modifiers that distinguish a different school sharing a prefix (e.g. "Georgia" vs "Georgia Tech")
    private val schoolModifiers = setOf(
        "state", "st", "st.", "tech", "southern", "northern", "eastern", "western",
        "central", "a&m", "christian", "atlantic", "coast", "coastal", "poly",
        "international", "college", "university", "methodist", "baptist", "lutheran",
        "wesleyan", "pacific", "valley", "bay", "beach", "up", "park", "corpus",
        "missouri", "carolina", "virginia", "dakota", "mexico", "illinois"
    )

    private val precedingModifiers = setOf(
        "north", "south", "east", "west", "central", "northern", "southern",
        "eastern", "western", "middle", "new", "old"
    )

    private val nonSportsChannelPhrases = listOf(
        "abc news", "cbs news", "nbc news", "msnbc", "cnbc",
        "fox news", "fox business", "fox weather"
    )

    private val otherSportKeywords = listOf(
        "volleyball", "softball", "soccer", "lacrosse", "wrestling",
        "baseball", "hockey", "golf", "tennis", "motorsport", "racing",
        "mma", "ufc", "boxing", "red bull"
    )

    private val rankPrefixRegex = Regex("^#\\d+\\s+")
    private val wordRegexCache = mutableMapOf<String, Regex>()

    private fun cleanTeamText(raw: String): String {
        return raw.trim()
            .replace(rankPrefixRegex, "")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun wordRegexFor(token: String): Regex {
        return wordRegexCache.getOrPut(token) {
            Regex("(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9])")
        }
    }

    private fun containsWord(text: String, token: String): Boolean {
        if (token.isBlank() || text.isBlank()) return false
        return wordRegexFor(token).containsMatchIn(text)
    }

    /**
     * Used by ChannelsScreen when expanding search queries (includes both school aliases and network aliases).
     */
    fun getAliasesForName(name: String): List<String> {
        val cleaned = cleanTeamText(name)
        val result = mutableSetOf<String>()
        if (cleaned.isNotBlank()) result.add(cleaned)
        getSchoolAliasesForName(cleaned).forEach { result.add(it) }
        networkSynonyms[cleaned]?.let { result.addAll(it) }
        return result.toList()
    }

    /**
     * Pure school/team aliases (never matches sub-school prefix like "Michigan State" -> "Michigan").
     */
    private fun getSchoolAliasesForName(name: String): List<String> {
        val cleaned = cleanTeamText(name)
        if (cleaned.isBlank()) return emptyList()
        val result = mutableSetOf<String>()
        result.add(cleaned)
        schoolAliases[cleaned]?.let { result.addAll(it) }
        for ((key, list) in schoolAliases) {
            if (cleaned == key || matchesSchoolPhrase(cleaned, key)) {
                result.addAll(list)
            }
        }
        return result.toList()
    }

    /**
     * Returns true when [text] refers to [school] without being a different school
     * (e.g., "georgia bulldogs" matches "georgia", but "georgia tech" or "georgia state" does NOT match "georgia").
     */
    private fun matchesSchoolPhrase(text: String, school: String): Boolean {
        if (text.isBlank() || school.isBlank()) return false
        if (text == school) return true

        val regex = wordRegexFor(school)
        val match = regex.find(text) ?: return false

        // Check word immediately before match
        val prefix = text.substring(0, match.range.first).trim()
        if (prefix.isNotEmpty()) {
            val prevWord = prefix.substringAfterLast(' ')
            if (prevWord in precedingModifiers && !school.startsWith("$prevWord ")) {
                return false
            }
        }

        // Check word immediately after match
        val suffix = text.substring(match.range.last + 1).trim()
        if (suffix.isNotEmpty()) {
            val nextWord = suffix.substringBefore(' ').trimEnd(',', '.', ':', ';', ')')
            if (nextWord in schoolModifiers && !school.endsWith(" $nextWord")) {
                return false
            }
            if (school == "miami" && (nextWord == "(oh)" || nextWord == "ohio" || nextWord == "redhawks")) {
                return false
            }
        }

        return true
    }

    private fun channelMatchesBroadcaster(cName: String, cGroup: String, broadcasterRaw: String): Boolean {
        val parts = broadcasterRaw.lowercase(Locale.ROOT)
            .split("/", ",", ";", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (parts.isEmpty()) return false

        for (net in parts) {
            val keywords = mutableSetOf(net)
            networkSynonyms[net]?.let { keywords.addAll(it) }

            for (kw in keywords) {
                val matched = containsWord(cName, kw) || containsWord(cGroup, kw)
                if (!matched) continue

                // Guard specific sub-channels so "ESPN" does not falsely match "ESPN2", "ESPNU", "ESPNews", "ESPN+", "ESPNW"
                if (kw == "espn") {
                    val isSubEspn = containsWord(cName, "espn2") || containsWord(cName, "espn 2") ||
                            containsWord(cName, "espnu") || containsWord(cName, "espn u") ||
                            containsWord(cName, "espnews") || containsWord(cName, "espn news") ||
                            containsWord(cName, "espn+") || containsWord(cName, "espn plus") ||
                            containsWord(cName, "espnw") || containsWord(cName, "espn w") ||
                            containsWord(cName, "espn3") || containsWord(cName, "espn 3")
                    if (isSubEspn) continue
                }

                // Guard "ACC" from matching "ACC Digital Network" (highlight FAST loop) unless explicitly requested
                if (kw == "acc" && (cName.contains("acc digital") || cGroup.contains("acc digital"))) {
                    continue
                }

                // Guard broadcast CBS/NBC/FOX from matching CBS Sports / NBC Sports / FS1 / FS2 / Deportes
                if (kw == "cbs" && (containsWord(cName, "cbs sports") || containsWord(cName, "cbssn"))) {
                    continue
                }
                if (kw == "nbc" && (containsWord(cName, "nbc sports") || containsWord(cName, "nbcsn"))) {
                    continue
                }
                if (kw == "fox" && (
                            containsWord(cName, "fs1") || containsWord(cName, "fs2") ||
                                    containsWord(cName, "fox sports 1") || containsWord(cName, "fox sports 2") ||
                                    containsWord(cName, "fox deportes") || containsWord(cName, "fox soccer")
                            )
                ) {
                    continue
                }

                return true
            }
        }
        return false
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

        val hClean = cleanTeamText(homeDisplayName)
        val aClean = cleanTeamText(awayDisplayName)
        val hAbbr = homeAbbr.trim().lowercase(Locale.ROOT)
        val aAbbr = awayAbbr.trim().lowercase(Locale.ROOT)
        val net = network.trim().lowercase(Locale.ROOT)
        val league = leagueOrSport.trim().lowercase(Locale.ROOT)

        val hAliases = getSchoolAliasesForName(hClean)
        val aAliases = getSchoolAliasesForName(aClean)
        val isWbb = league.contains("ncaaw") || league.contains("women") || league.contains("wbb")

        val scored = available.mapNotNull { channel ->
            val cName = channel.name.lowercase(Locale.ROOT)
            val cGroup = channel.groupTitle.lowercase(Locale.ROOT)

            // Reject non-sports news/business/weather feeds
            val isNews = nonSportsChannelPhrases.any { containsWord(cName, it) || containsWord(cGroup, it) }
            if (isNews) return@mapNotNull null

            // Reject explicitly wrong sports (e.g. volleyball, Red Bull extreme sports, golf) when game isn't that sport
            val wrongSport = otherSportKeywords.any { sp ->
                (containsWord(cName, sp) || containsWord(cGroup, sp)) && !league.contains(sp)
            }
            if (wrongSport) return@mapNotNull null

            var score = 0
            var hasNetworkMatch = false
            var hasTeamMatch = false

            // 1. Broadcaster / network match (including local affiliate call signs)
            if (net.isNotBlank() && channelMatchesBroadcaster(cName, cGroup, net)) {
                score += 200
                hasNetworkMatch = true
            }

            // 2. Team match (exact cleaned name or pure school alias, word-bounded and school-disambiguated)
            for (alias in hAliases) {
                if (alias.length >= 3 && (matchesSchoolPhrase(cName, alias) || matchesSchoolPhrase(cGroup, alias))) {
                    score += 180
                    hasTeamMatch = true
                    break
                }
            }
            for (alias in aAliases) {
                if (alias.length >= 3 && (matchesSchoolPhrase(cName, alias) || matchesSchoolPhrase(cGroup, alias))) {
                    score += 180
                    hasTeamMatch = true
                    break
                }
            }

            // 3. Unambiguous 3+ letter team abbreviations
            if (!hasTeamMatch && hAbbr.length >= 3 && hAbbr != "usa" && containsWord(cName, hAbbr)) {
                score += 90
                hasTeamMatch = true
            }
            if (!hasTeamMatch && aAbbr.length >= 3 && aAbbr != "usa" && containsWord(cName, aAbbr)) {
                score += 90
                hasTeamMatch = true
            }

            // STRICT RULE: Must match EITHER the game's broadcaster OR one of the two teams.
            // NEVER fall back to random sports channels or unrelated feeds.
            if (!hasNetworkMatch && !hasTeamMatch) {
                return@mapNotNull null
            }

            // Strong boost when BOTH team and broadcaster match
            if (hasNetworkMatch && hasTeamMatch) {
                score += 300
            }

            // League / Women's basketball bonus (only when already qualified by network or team)
            if (league.isNotBlank() && (containsWord(cName, league) || containsWord(cGroup, league))) {
                score += 30
            }
            if (isWbb) {
                val matchesWbb = containsWord(cName, "ncaaw") || containsWord(cName, "wbb") ||
                        containsWord(cName, "women") || containsWord(cName, "espn w") ||
                        containsWord(cName, "espnw") || containsWord(cGroup, "ncaaw") ||
                        containsWord(cGroup, "women")
                if (matchesWbb) {
                    score += 60
                }
            }

            if (channel.isSports) score += 10
            channel to score
        }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        return scored
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
            leagueOrSport = "${game.league} ${game.sport}".trim(),
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
            leagueOrSport = "CFB",
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
        val ncaaConverted = if (isWomens) {
            emptyList()
        } else {
            ncaaGames.map { n ->
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
        }

        val pool = (allGames + ncaaConverted).distinctBy {
            it.id.ifBlank { "${it.league}-${it.homeTeam.displayName}-${it.awayTeam.displayName}-${it.startTime}" }
        }

        // Strict sport/league gate:
        // - Women's Top 20 -> ONLY NCAAW women's basketball
        // - Men's/Football Top 20 -> ONLY CFB college football
        val filteredGames = pool.filter { g ->
            val leagueLower = g.league.lowercase(Locale.ROOT)
            val sportLower = g.sport.lowercase(Locale.ROOT)
            if (isWomens) {
                leagueLower == "ncaaw" || leagueLower == "womens-college-basketball" ||
                        (sportLower == "basketball" && (leagueLower.contains("women") || leagueLower.contains("wbb")))
            } else {
                leagueLower == "cfb" || leagueLower == "college-football" || leagueLower == "ncaa football"
            }
        }

        val tId = team.teamId.trim().lowercase(Locale.ROOT)
        val tDisplay = cleanTeamText(team.displayName)
        val tName = cleanTeamText(team.name)
        val tFull = if (tDisplay.isNotBlank() && tName.isNotBlank() && !tDisplay.contains(tName)) {
            "$tDisplay $tName"
        } else {
            tDisplay
        }
        val tAbbr = team.abbreviation.trim().lowercase(Locale.ROOT)

        val schoolNames = mutableSetOf<String>()
        if (tDisplay.isNotBlank()) schoolNames.add(tDisplay)
        schoolAliases[tDisplay]?.let { aliases ->
            aliases.forEach { alias ->
                // Only keep school-level aliases (avoid ambiguous mascots like "tigers" or "bulldogs")
                if (alias !in setOf("bulldogs", "tigers", "wildcats", "bears", "eagles", "cardinals", "cougars", "aggies", "panthers", "owls", "huskies")) {
                    schoolNames.add(alias)
                }
            }
        }
        val uniqueMascot = uniqueMascotAliases[tDisplay]

        fun scoreSide(sideId: String, sideDisplayRaw: String, sideAbbr: String): Int {
            val sId = sideId.trim().lowercase(Locale.ROOT)
            val sClean = cleanTeamText(sideDisplayRaw)
            val sAbbr = sideAbbr.trim().lowercase(Locale.ROOT)

            // 1. Exact ESPN Team ID match (highest confidence)
            if (tId.isNotBlank() && sId == tId) {
                return 1000
            }

            // 2. Exact full team name (e.g. "south carolina gamecocks" == "south carolina gamecocks")
            if (tFull.isNotBlank() && sClean == tFull) {
                return 800
            }

            // 3. Exact display/school name or alias (disambiguated: "georgia" will NOT match "georgia tech")
            if (tDisplay.isNotBlank() && matchesSchoolPhrase(sClean, tDisplay)) {
                return 600
            }
            for (alias in schoolNames) {
                if (matchesSchoolPhrase(sClean, alias)) {
                    return 550
                }
            }

            // 4. Unique mascot alias (e.g. "gamecocks", "tar heels", "crimson tide")
            if (!uniqueMascot.isNullOrBlank() && containsWord(sClean, uniqueMascot)) {
                return 450
            }

            // 5. Exact 3+ letter team abbreviation match
            if (tAbbr.length >= 3 && sAbbr == tAbbr) {
                return 300
            }

            return 0
        }

        val matchedGames = filteredGames.mapNotNull { g ->
            val homeScore = scoreSide(g.homeTeam.id, g.homeTeam.displayName, g.homeTeam.abbreviation)
            val awayScore = scoreSide(g.awayTeam.id, g.awayTeam.displayName, g.awayTeam.abbreviation)
            val bestScore = maxOf(homeScore, awayScore)
            if (bestScore > 0) g to bestScore else null
        }

        if (matchedGames.isEmpty()) return null

        // Sort by match score FIRST, and only then by live/upcoming/completed status and start time
        return matchedGames
            .sortedWith(
                compareByDescending<Pair<Game, Int>> { it.second }
                    .thenBy {
                        when (it.first.state) {
                            "in" -> 0
                            "pre" -> 1
                            else -> 2
                        }
                    }
                    .thenBy { it.first.startTime }
            )
            .map { it.first }
            .firstOrNull()
    }
}
