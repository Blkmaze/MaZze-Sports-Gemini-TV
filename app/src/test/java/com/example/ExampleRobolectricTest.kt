package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.api.PlaylistRepository
import com.example.model.Game
import com.example.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MaZze Sports TV", appName)
    }

    @Test
    fun testM3uParserAndSportsFilter() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PlaylistRepository(context)

        val m3uData = """
            #EXTM3U
            #EXTINF:-1 tvg-id="espn" tvg-name="ESPN HD" tvg-logo="http://logo.png" group-title="Sports USA",ESPN Live HD
            http://stream.example.com/espn.m3u8
            #EXTINF:-1 tvg-name="News 24" group-title="News",CNN News
            http://stream.example.com/news.m3u8
            #EXTINF:-1 tvg-name="Fox Sports 1" group-title="Sports USA",Fox Sports 1
            http://stream.example.com/fs1.m3u8
        """.trimIndent()

        val parsed = repo.parseM3uString(m3uData)
        assertEquals(3, parsed.size)
        assertTrue(repo.isSportsChannel(parsed[0].name, parsed[0].groupTitle))
        assertTrue(repo.isSportsChannel(parsed[2].name, parsed[2].groupTitle))
    }

    @Test
    fun testChannelMatchingForGame() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PlaylistRepository(context)

        val demoChannels = repo.getCuratedDemoSportsChannels()
        val game = Game(
            id = "test_game",
            sport = "football",
            league = "NFL",
            name = "Chiefs vs Ravens",
            shortName = "KC vs BAL",
            homeTeam = Team(displayName = "Kansas City Chiefs", abbreviation = "KC"),
            awayTeam = Team(displayName = "Baltimore Ravens", abbreviation = "BAL")
        )

        val matched = repo.matchChannelForGame(game, demoChannels)
        assertTrue(matched != null)
    }
}
