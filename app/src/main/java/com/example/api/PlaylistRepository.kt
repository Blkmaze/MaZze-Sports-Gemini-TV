package com.example.api

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Channel
import com.example.model.Game
import com.example.model.PlaylistConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlaylistRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mazze_sports_prefs", Context.MODE_PRIVATE)

    private val sportsKeywords = listOf(
        "sport", "espn", "fox sports", "nfl", "nba", "nhl", "mlb",
        "nbc sports", "cbs sports", "bein", "sky sports", "tnt", "dazn",
        "fight", "ufc", "golf", "tennis", "racing", "soccer", "football",
        "boxing", "motorsport", "f1", "nascar", "hockey", "baseball", "basketball"
    )

    fun loadConfig(): PlaylistConfig {
        return PlaylistConfig(
            type = prefs.getString("playlist_type", "demo") ?: "demo",
            m3uUrl = prefs.getString("m3u_url", "") ?: "",
            xtreamServer = prefs.getString("xtream_server", "") ?: "",
            xtreamUsername = prefs.getString("xtream_user", "") ?: "",
            xtreamPassword = prefs.getString("xtream_pass", "") ?: "",
            spoilerFree = prefs.getBoolean("spoiler_free", false)
        )
    }

    fun saveConfig(config: PlaylistConfig) {
        prefs.edit()
            .putString("playlist_type", config.type)
            .putString("m3u_url", config.m3uUrl)
            .putString("xtream_server", config.xtreamServer)
            .putString("xtream_user", config.xtreamUsername)
            .putString("xtream_pass", config.xtreamPassword)
            .putBoolean("spoiler_free", config.spoilerFree)
            .apply()
    }

    fun toggleSpoilerFree(current: Boolean): Boolean {
        val newVal = !current
        prefs.edit().putBoolean("spoiler_free", newVal).apply()
        return newVal
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorite_channels", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(channelId: String): Boolean {
        val favs = getFavorites().toMutableSet()
        val isFav = if (favs.contains(channelId)) {
            favs.remove(channelId)
            false
        } else {
            favs.add(channelId)
            true
        }
        prefs.edit().putStringSet("favorite_channels", favs).apply()
        return isFav
    }

    suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val config = loadConfig()
        val favorites = getFavorites()

        val rawChannels = when (config.type) {
            "m3u" -> if (config.m3uUrl.isNotBlank()) fetchM3u(config.m3uUrl) else emptyList()
            "xtream" -> if (config.xtreamServer.isNotBlank()) fetchXtream(config) else emptyList()
            else -> emptyList()
        }

        val sportsChannels = if (rawChannels.isNotEmpty()) {
            filterSportsChannels(rawChannels)
        } else {
            getCuratedDemoSportsChannels()
        }

        // Apply favorite state
        sportsChannels.map { ch ->
            ch.copy(isFavorite = favorites.contains(ch.id))
        }
    }

    private suspend fun fetchM3u(url: String): List<Channel> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", "MaZzeSportsTV/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val content = resp.body?.string() ?: ""
                    parseM3uString(content)
                } else emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseM3uString(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentTvgName = ""
        var currentGroup = "General"
        var currentLogo: String? = null
        var currentTitle = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                currentTvgName = extractAttribute(trimmed, "tvg-name")
                currentGroup = extractAttribute(trimmed, "group-title").ifEmpty { "Sports" }
                currentLogo = extractAttribute(trimmed, "tvg-logo").ifEmpty { null }

                // The channel name usually follows the last comma
                val commaIndex = trimmed.lastIndexOf(',')
                currentTitle = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    currentTvgName.ifEmpty { "Channel ${channels.size + 1}" }
                }
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val streamUrl = trimmed
                val name = currentTitle.ifEmpty { currentTvgName.ifEmpty { "Channel ${channels.size + 1}" } }
                val isSport = isSportsChannel(name, currentGroup)

                channels.add(
                    Channel(
                        id = "m3u_${channels.size}_${name.hashCode()}",
                        name = name,
                        streamUrl = streamUrl,
                        groupTitle = currentGroup,
                        logoUrl = currentLogo,
                        isSports = isSport
                    )
                )

                // Reset for next channel
                currentTvgName = ""
                currentGroup = "Sports"
                currentLogo = null
                currentTitle = ""
            }
        }
        return channels
    }

    private fun extractAttribute(line: String, attr: String): String {
        val pattern = """$attr="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    private suspend fun fetchXtream(config: PlaylistConfig): List<Channel> {
        return try {
            var server = config.xtreamServer.trim()
            if (!server.startsWith("http://") && !server.startsWith("https://")) {
                server = "http://$server"
            }
            if (server.endsWith("/")) server = server.dropLast(1)

            val url = "$server/player_api.php?username=${config.xtreamUsername}&password=${config.xtreamPassword}&action=get_live_streams"
            val req = Request.Builder().url(url).header("User-Agent", "MaZzeSportsTV/1.0").build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return emptyList()
                    val array = JSONArray(body)
                    val list = mutableListOf<Channel>()
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val streamId = obj.optString("stream_id", "")
                        val streamName = obj.optString("name", "Stream")
                        val streamIcon = obj.optString("stream_icon", null)
                        val categoryName = obj.optString("category_name", "Live")
                        val isSport = isSportsChannel(streamName, categoryName)

                        val streamUrl = "$server/live/${config.xtreamUsername}/${config.xtreamPassword}/$streamId.m3u8"
                        list.add(
                            Channel(
                                id = "xtream_$streamId",
                                name = streamName,
                                streamUrl = streamUrl,
                                groupTitle = categoryName,
                                logoUrl = streamIcon,
                                isSports = isSport
                            )
                        )
                    }
                    list
                } else emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun isSportsChannel(name: String, groupTitle: String): Boolean {
        val lowerName = name.lowercase(Locale.ROOT)
        val lowerGroup = groupTitle.lowercase(Locale.ROOT)

        return sportsKeywords.any { kw ->
            lowerName.contains(kw) || lowerGroup.contains(kw)
        }
    }

    private fun filterSportsChannels(channels: List<Channel>): List<Channel> {
        val sports = channels.filter { it.isSports }
        return if (sports.isNotEmpty()) sports else channels
    }

    fun matchChannelForGame(game: Game, channels: List<Channel>): Channel? {
        if (channels.isEmpty()) return null

        val homeName = game.homeTeam.displayName.lowercase(Locale.ROOT)
        val homeAbbr = game.homeTeam.abbreviation.lowercase(Locale.ROOT)
        val awayName = game.awayTeam.displayName.lowercase(Locale.ROOT)
        val awayAbbr = game.awayTeam.abbreviation.lowercase(Locale.ROOT)
        val league = game.league.lowercase(Locale.ROOT)

        // 1. Try exact team match
        channels.find { ch ->
            val chName = ch.name.lowercase(Locale.ROOT)
            (homeAbbr.length >= 3 && chName.contains(homeAbbr)) ||
            (awayAbbr.length >= 3 && chName.contains(awayAbbr)) ||
            homeName.split(" ").any { part -> part.length > 3 && chName.contains(part) } ||
            awayName.split(" ").any { part -> part.length > 3 && chName.contains(part) }
        }?.let { return it }

        // 2. Try league match (e.g., "NFL Network", "ESPN", "NBA TV")
        channels.find { ch ->
            val chName = ch.name.lowercase(Locale.ROOT)
            chName.contains(league) || (league == "nfl" && chName.contains("football")) ||
            (league == "nba" && chName.contains("basketball")) ||
            (league == "epl" && (chName.contains("premier") || chName.contains("soccer")))
        }?.let { return it }

        // Fallback to first channel
        return channels.firstOrNull()
    }

    /**
     * Curated legal free live sports streams & sports network feeds
     * for instant testing out-of-the box.
     */
    fun getCuratedDemoSportsChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "demo_redbull",
                name = "Red Bull TV Live Sports",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                groupTitle = "Extreme Sports",
                logoUrl = "https://resources.redbull.com/logos/redbull-logo.png",
                isSports = true
            ),
            Channel(
                id = "demo_acc",
                name = "ACC Digital Network (NCAA)",
                streamUrl = "https://raycom-accdn-firetv.amagi.tv/playlist.m3u8",
                groupTitle = "College Sports",
                logoUrl = "https://a.espncdn.com/i/teamlogos/ncaa/500/acc.png",
                isSports = true
            ),
            Channel(
                id = "demo_golf",
                name = "30A Golf Kingdom Live",
                streamUrl = "https://30a-tv.com/feeds/vidaa/golf.m3u8",
                groupTitle = "Golf",
                logoUrl = "https://a.espncdn.com/redesign/assets/img/logos/espn-icon.png",
                isSports = true
            ),
            Channel(
                id = "demo_aci_motorsport",
                name = "ACI Motorsport TV",
                streamUrl = "https://webstream.multistream.it/memfs/e2cb3629-c1a2-495b-b43a-9eb386f04ed8.m3u8",
                groupTitle = "Motorsports & Racing",
                logoUrl = "https://a.espncdn.com/i/teamlogos/default-team-logo-500.png",
                isSports = true
            ),
            Channel(
                id = "demo_mux_showcase",
                name = "Sports Showcase Live Feed",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                groupTitle = "Sports USA",
                logoUrl = "https://a.espncdn.com/redesign/assets/img/logos/espn-icon.png",
                isSports = true
            ),
            Channel(
                id = "demo_africa_sport",
                name = "Africa 24 Sport Live",
                streamUrl = "https://africa24.vedge.infomaniak.com/livecast/ik:africa24sport/manifest.m3u8",
                groupTitle = "Soccer & Athletics",
                logoUrl = "https://a.espncdn.com/i/teamlogos/default-team-logo-500.png",
                isSports = true
            ),
            Channel(
                id = "demo_iraq_sport",
                name = "Al Iraqia Sport HD",
                streamUrl = "https://imn-live.esite-lab.com/hls/iraqia-sports-1.m3u8",
                groupTitle = "World Sports",
                logoUrl = "https://a.espncdn.com/i/teamlogos/default-team-logo-500.png",
                isSports = true
            ),
            Channel(
                id = "demo_ado_tv",
                name = "ADO Sport TV",
                streamUrl = "https://strhls.streamakaci.tv/ortb/ortb2-multi/playlist.m3u8",
                groupTitle = "Soccer",
                logoUrl = "https://a.espncdn.com/i/teamlogos/default-team-logo-500.png",
                isSports = true
            )
        )
    }
}
