package com.example.model

data class Team(
    val id: String = "",
    val displayName: String = "",
    val abbreviation: String = "",
    val logo: String? = null,
    val color: String? = null
)

data class Game(
    val id: String,
    val sport: String,
    val league: String,
    val name: String,
    val shortName: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val homeScore: String = "0",
    val awayScore: String = "0",
    val state: String = "pre", // "in", "pre", "post"
    val statusDetail: String = "",
    val startTime: String = "",
    val matchedChannel: Channel? = null
) {
    val isLive: Boolean get() = state == "in"
    val isPre: Boolean get() = state == "pre"
    val isFinal: Boolean get() = state == "post"
}

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val groupTitle: String = "Sports",
    val logoUrl: String? = null,
    val isSports: Boolean = true,
    val isFavorite: Boolean = false
)

data class SquareCell(
    val index: Int,
    val row: Int, // 0..9 (Home team last digit)
    val col: Int, // 0..9 (Away team last digit)
    val ownerName: String? = null, // e.g. "YOU" or "JD", "MK"
    val isUserSquare: Boolean = false
)

data class SquaresBoard(
    val gameId: String,
    val pricePerSquare: Int = 10,
    val homeNumbers: List<Int>? = null, // null until 75% filled
    val awayNumbers: List<Int>? = null,
    val cells: List<SquareCell> = emptyList()
) {
    val userSquaresCount: Int get() = cells.count { it.isUserSquare }
    val totalTakenCount: Int get() = cells.count { it.ownerName != null }
    val percentFilled: Int get() = totalTakenCount
    val isLockedAndDrawn: Boolean get() = percentFilled >= 75
}

data class PlaylistConfig(
    val type: String = "demo", // "m3u", "xtream", "demo"
    val m3uUrl: String = "",
    val xtreamServer: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = "",
    val spoilerFree: Boolean = false
)

data class UpdateInfo(
    val latestBuildNumber: Int = 0,
    val currentBuildNumber: Int = 1,
    val hasUpdate: Boolean = false,
    val releaseTitle: String = "",
    val downloadUrl: String? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val readyToInstallPath: String? = null
)
