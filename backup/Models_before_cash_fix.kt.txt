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
    val teamId: String = "",
    val record: String = ""
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

data class SquarePendingEntry(
    val id: String = "",
    val initials: String = "",
    val count: Int = 1,
    val paymentMethod: String = "CASH",
    val totalAmount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class SquareSale(
    val id: String = "",
    val initials: String = "",
    val count: Int = 1,
    val paymentMethod: String = "CASH",
    val assignedSquares: List<Int> = emptyList(),
    val totalAmount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class SquaresBoard(
    val gameId: String = "",
    val boardVersion: Int = 1,
    val pricePerSquare: Int = 10,
    val payoutType: String = "QUARTERS",
    val isNumbersLocked: Boolean = false,
    val isBoardLocked: Boolean = false,
    val homeNumbers: List<Int> = emptyList(),
    val awayNumbers: List<Int> = emptyList(),
    val cells: List<SquareCell> = emptyList(),
    val pendingEntries: List<SquarePendingEntry> = emptyList(),
    val sales: List<SquareSale> = emptyList()
)

object SquaresConfig {
    const val ADMIN_FEE_PERCENT: Double = 15.0

    fun calculatePayouts(pricePerSquare: Int, payoutType: String): SquaresPayouts {
        val totalPotCents = pricePerSquare * 100 * 100L
        val adminFeeCents = Math.round(totalPotCents * (ADMIN_FEE_PERCENT / 100.0))
        val prizePoolCents = totalPotCents - adminFeeCents
        val isHalves = payoutType.equals("HALVES", ignoreCase = true)

        val q1Cents: Long
        val q2Cents: Long
        val q3Cents: Long
        val finalCents: Long

        if (isHalves) {
            q1Cents = 0L
            q2Cents = Math.round(prizePoolCents * 0.50)
            q3Cents = 0L
            finalCents = prizePoolCents - q2Cents
        } else {
            val qBase = Math.round(prizePoolCents * 0.25)
            q1Cents = qBase
            q2Cents = qBase
            q3Cents = qBase
            finalCents = prizePoolCents - (q1Cents + q2Cents + q3Cents)
        }

        return SquaresPayouts(
            pricePerSquare = pricePerSquare,
            totalPot = totalPotCents / 100.0,
            adminFee = adminFeeCents / 100.0,
            prizePool = prizePoolCents / 100.0,
            q1Payout = q1Cents / 100.0,
            q2Payout = q2Cents / 100.0,
            q3Payout = q3Cents / 100.0,
            finalPayout = finalCents / 100.0,
            isHalves = isHalves
        )
    }

    fun formatMoney(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "$%,d", amount.toLong())
        } else {
            String.format(java.util.Locale.US, "$%,.2f", amount)
        }
    }
}

data class SquaresPayouts(
    val pricePerSquare: Int,
    val totalPot: Double,
    val adminFee: Double,
    val prizePool: Double,
    val q1Payout: Double,
    val q2Payout: Double,
    val q3Payout: Double,
    val finalPayout: Double,
    val isHalves: Boolean
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
