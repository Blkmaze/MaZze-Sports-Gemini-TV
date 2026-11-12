package com.example.model

data class Channel(
    val id: String = "",
    val name: String = "",
    val streamUrl: String = "",
    val groupTitle: String = "General",
    val logoUrl: String? = null,
    val isSports: Boolean = true,
    val isFavorite: Boolean = false
)

data class Team(
    val id: String = "",
    val displayName: String = "",
    val abbreviation: String = "",
    val logo: String = "",
    val color: String = ""
)

data class Game(
    val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val sport: String = "",
    val league: String = "",
    val homeTeam: Team = Team(),
    val awayTeam: Team = Team(),
    val homeScore: String = "",
    val awayScore: String = "",
    val statusDetail: String = "",
    val state: String = "",
    val startTime: String = "",
    val network: String = "",
    val matchedChannel: Channel? = null
)

data class ApTeamRank(
    val rank: Int = 0,
    val name: String = "",
    val displayName: String = "",
    val abbreviation: String = "",
    val logoUrl: String = "",
    val isSec: Boolean = false,
    val teamId: String = ""
)

data class NcaaGame(
    val id: String = "",
    val name: String = "",
    val homeTeam: Team = Team(),
    val awayTeam: Team = Team(),
    val homeRank: Int? = null,
    val awayRank: Int? = null,
    val homeScore: String = "",
    val awayScore: String = "",
    val statusDetail: String = "",
    val state: String = "",
    val startTime: String = "",
    val kickoffDisplay: String = "",
    val network: String = "",
    val isSecMatch: Boolean = false,
    val tier: Int = 0,
    val bestRank: Int = 999,
    val matchedChannel: Channel? = null
)

data class NcaaFootballData(
    val apTop20: List<ApTeamRank> = emptyList(),
    val games: List<NcaaGame> = emptyList(),
    val lastUpdated: String = ""
)

data class SquareCell(
    val index: Int = 0,
    val row: Int = 0,
    val col: Int = 0,
    val ownerName: String = "",
    val isUserSquare: Boolean = false
)

data class SquaresBoard(
    val gameId: String = "",
    val pricePerSquare: Int = 10,
    val homeNumbers: List<Int> = emptyList(),
    val awayNumbers: List<Int> = emptyList(),
    val cells: List<SquareCell> = emptyList()
)

data class PlaylistConfig(
    val type: String = "demo",
    val m3uUrl: String = "",
    val xtreamServer: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = "",
    val spoilerFree: Boolean = false
)

data class UpdateInfo(
    val currentBuildNumber: Int = 1,
    val latestBuildNumber: Int = 1,
    val hasUpdate: Boolean = false,
    val releaseTitle: String = "",
    val downloadUrl: String = "",
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val readyToInstallPath: String? = null,
    val errorMessage: String? = null
)
