package com.example.util

import android.content.Context
import com.example.model.SquareCell
import com.example.model.SquarePendingEntry
import com.example.model.SquareSale
import com.example.model.SquaresActivityLog
import com.example.model.SquaresBoard
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SquaresStorage(context: Context) {
    private val prefs = context.getSharedPreferences("squares_boards_prefs", Context.MODE_PRIVATE)

    fun getActiveVersion(gameId: String): Int {
        val gid = gameId.ifBlank { "default" }
        return prefs.getInt("active_version_$gid", 1)
    }

    fun setActiveVersion(gameId: String, version: Int) {
        val gid = gameId.ifBlank { "default" }
        prefs.edit().putInt("active_version_$gid", version).apply()
    }

    fun getBoardVersions(gameId: String): List<Int> {
        val gid = gameId.ifBlank { "default" }
        val raw = prefs.getString("versions_list_$gid", null)
        if (raw.isNullOrBlank()) return listOf(1)
        return try {
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).map { arr.getInt(it) }.distinct().sorted()
            if (list.isEmpty()) listOf(1) else list
        } catch (e: Throwable) {
            listOf(1)
        }
    }

    fun registerBoardVersion(gameId: String, version: Int) {
        val gid = gameId.ifBlank { "default" }
        val current = getBoardVersions(gid).toMutableSet()
        current.add(version)
        val arr = JSONArray(current.sorted())
        prefs.edit().putString("versions_list_$gid", arr.toString()).apply()
    }

    fun loadBoard(gameId: String, version: Int = 1): SquaresBoard? {
        val gid = gameId.ifBlank { "default" }
        val keyWithVersion = "board_${gid}_v$version"
        val legacyKey = "board_$gid"

        var jsonStr = prefs.getString(keyWithVersion, null)
        if (jsonStr == null && version == 1) {
            // Check legacy key
            jsonStr = prefs.getString(legacyKey, null)
        }
        if (jsonStr == null) return null

        return try {
            val json = JSONObject(jsonStr)
            val boardVersion = json.optInt("version", version)
            val price = json.optInt("price", 10)
            val payoutType = json.optString("payoutType", "QUARTERS")
            val isNumbersLocked = json.optBoolean("numbersLocked", false)
            val isBoardLocked = json.optBoolean("boardLocked", false)

            val homeNumbers = json.optJSONArray("home")?.let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            } ?: (0..9).shuffled()
            val awayNumbers = json.optJSONArray("away")?.let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            } ?: (0..9).shuffled()

            val cellsArr = json.optJSONArray("cells")
            val cellsMap = (0 until (cellsArr?.length() ?: 0)).mapNotNull { i ->
                cellsArr?.optJSONObject(i)?.let { c ->
                    SquareCell(
                        index = c.optInt("index", i),
                        row = c.optInt("row", i / 10),
                        col = c.optInt("col", i % 10),
                        ownerName = c.optString("owner", ""),
                        isUserSquare = c.optBoolean("isUser", false)
                    )
                }
            }.associateBy { it.index }
            var cells = (0 until 100).map { i ->
                cellsMap[i] ?: SquareCell(index = i, row = i / 10, col = i % 10, ownerName = "", isUserSquare = false)
            }

            val pendingArr = json.optJSONArray("pending")
            val pendingEntries = if (pendingArr != null) {
                (0 until pendingArr.length()).map { i ->
                    val p = pendingArr.getJSONObject(i)
                    SquarePendingEntry(
                        id = p.optString("id", UUID.randomUUID().toString()),
                        initials = p.optString("initials", ""),
                        count = p.optInt("count", 1),
                        paymentMethod = p.optString("method", "CASH"),
                        totalAmount = p.optInt("amount", 0),
                        timestamp = p.optLong("time", System.currentTimeMillis())
                    )
                }
            } else emptyList()

            val salesArr = json.optJSONArray("sales")
            val sales = if (salesArr != null) {
                (0 until salesArr.length()).map { i ->
                    val s = salesArr.getJSONObject(i)
                    val sqArr = s.optJSONArray("squares")
                    val squares = if (sqArr != null) (0 until sqArr.length()).map { sqArr.getInt(it) } else emptyList()
                    SquareSale(
                        id = s.optString("id", ""),
                        initials = s.optString("initials", ""),
                        count = s.optInt("count", 1),
                        paymentMethod = s.optString("method", "CASH"),
                        assignedSquares = squares,
                        totalAmount = s.optInt("amount", 0),
                        timestamp = s.optLong("time", System.currentTimeMillis())
                    )
                }
            } else emptyList()

            // Auto-heal: If sales have assignedSquares, ensure cells reflect the owner
            sales.forEach { sale ->
                if (sale.assignedSquares.isNotEmpty() && sale.initials.isNotBlank()) {
                    cells = cells.map { cell ->
                        if (cell.index in sale.assignedSquares && cell.ownerName.isBlank()) {
                            cell.copy(ownerName = sale.initials, isUserSquare = true)
                        } else {
                            cell
                        }
                    }
                }
            }

            val logsArr = json.optJSONArray("logs")
            val activityLogs = if (logsArr != null) {
                (0 until logsArr.length()).map { i ->
                    val l = logsArr.getJSONObject(i)
                    SquaresActivityLog(
                        id = l.optString("id", UUID.randomUUID().toString()),
                        timestamp = l.optLong("time", System.currentTimeMillis()),
                        action = l.optString("action", ""),
                        description = l.optString("desc", ""),
                        isError = l.optBoolean("error", false)
                    )
                }
            } else emptyList()

            SquaresBoard(
                gameId = gameId,
                boardVersion = boardVersion,
                pricePerSquare = price,
                payoutType = payoutType,
                isNumbersLocked = isNumbersLocked,
                isBoardLocked = isBoardLocked,
                homeNumbers = homeNumbers,
                awayNumbers = awayNumbers,
                cells = cells,
                pendingEntries = pendingEntries,
                sales = sales,
                activityLogs = activityLogs
            )
        } catch (e: Throwable) {
            null
        }
    }

    fun saveBoard(board: SquaresBoard) {
        val gid = board.gameId.ifBlank { "default" }
        val key = "board_${gid}_v${board.boardVersion}"
        registerBoardVersion(gid, board.boardVersion)

        val json = JSONObject().apply {
            put("version", board.boardVersion)
            put("price", board.pricePerSquare)
            put("payoutType", board.payoutType)
            put("numbersLocked", board.isNumbersLocked)
            put("boardLocked", board.isBoardLocked)
            put("home", JSONArray(board.homeNumbers))
            put("away", JSONArray(board.awayNumbers))

            val cellsArr = JSONArray()
            board.cells.forEach { cell ->
                cellsArr.put(JSONObject().apply {
                    put("index", cell.index)
                    put("row", cell.row)
                    put("col", cell.col)
                    put("owner", cell.ownerName)
                    put("isUser", cell.isUserSquare)
                })
            }
            put("cells", cellsArr)

            val pendingArr = JSONArray()
            board.pendingEntries.forEach { entry ->
                pendingArr.put(JSONObject().apply {
                    put("id", entry.id)
                    put("initials", entry.initials)
                    put("count", entry.count)
                    put("method", entry.paymentMethod)
                    put("amount", entry.totalAmount)
                    put("time", entry.timestamp)
                })
            }
            put("pending", pendingArr)

            val salesArr = JSONArray()
            board.sales.forEach { sale ->
                salesArr.put(JSONObject().apply {
                    put("id", sale.id)
                    put("initials", sale.initials)
                    put("count", sale.count)
                    put("method", sale.paymentMethod)
                    put("squares", JSONArray(sale.assignedSquares))
                    put("amount", sale.totalAmount)
                    put("time", sale.timestamp)
                })
            }
            put("sales", salesArr)

            val logsArr = JSONArray()
            board.activityLogs.take(50).forEach { log ->
                logsArr.put(JSONObject().apply {
                    put("id", log.id)
                    put("time", log.timestamp)
                    put("action", log.action)
                    put("desc", log.description)
                    put("error", log.isError)
                })
            }
            put("logs", logsArr)
        }
        prefs.edit().putString(key, json.toString()).apply()
    }
}
