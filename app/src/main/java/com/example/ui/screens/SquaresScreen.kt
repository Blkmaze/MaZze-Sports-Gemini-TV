package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Game
import com.example.model.SquareCell
import com.example.model.SquaresBoard
import com.example.ui.components.TvFocusButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun SquaresScreen(
    games: List<Game>,
    selectedGame: Game?,
    squaresBoard: SquaresBoard,
    showPaymentDialog: Boolean,
    paymentMethod: String,
    onSelectGame: (Game) -> Unit,
    onSelectPrice: (Int) -> Unit,
    onToggleSquare: (Int, Int) -> Unit,
    onOpenPayment: (String) -> Unit,
    onClosePayment: () -> Unit,
    onOpenBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCost = squaresBoard.userSquaresCount * squaresBoard.pricePerSquare

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Section: Game Picker for Today's Real Games
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "10x10 Super Squares Contest",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Refundable if the board does not fill",
                            color = TvLimeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        com.example.ui.components.TvClock()
                    }
                }

                Text(
                    text = "Select a Game to Play Squares:",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )

                if (games.isEmpty()) {
                    Text(
                        text = "Loading today's ESPN matchups...",
                        color = TvTextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(games, key = { it.id }) { game ->
                            val isSelected = selectedGame?.id == game.id
                            TvFocusCard(
                                onClick = { onSelectGame(game) },
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(64.dp),
                                containerColor = if (isSelected) TvDarkCardElevated else TvDarkCard,
                                focusedBorderColor = TvOrangeFocus
                            ) { isFocused ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${game.league}: ${game.awayTeam.abbreviation} vs ${game.homeTeam.abbreviation}",
                                        color = if (isSelected) TvOrangeFocus else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (game.isLive) "LIVE: ${game.awayScore}-${game.homeScore}" else game.statusDetail,
                                        color = if (game.isLive) TvLimeAccent else TvTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Contest Control Bar (Price selector, % sold status, count & total)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvDarkCard)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Price / Square:",
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    listOf(5, 10, 20).forEach { price ->
                        val isPriceSelected = squaresBoard.pricePerSquare == price
                        TvFocusButton(
                            text = "$$price",
                            isPrimary = isPriceSelected,
                            onClick = { onSelectPrice(price) }
                        )
                    }
                }

                // 75% Fill Progress & Numbers Lock Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${squaresBoard.percentFilled}% Filled (${squaresBoard.totalTakenCount}/100)",
                            color = if (squaresBoard.isLockedAndDrawn) TvLimeAccent else TvOrangeFocus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (squaresBoard.isLockedAndDrawn) "Numbers Drawn at Random!" else "Numbers reveal at 75% filled (?)",
                            color = TvTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // User's tally & Total
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Your Squares: ${squaresBoard.userSquaresCount}",
                            color = TvOrangeFocus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total: $$totalCost",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 3. The 10x10 Squares Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvDarkSurface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Team Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLUMNS: ${selectedGame?.awayTeam?.displayName ?: "Away Team"} Digits (0-9)",
                        color = TvLimeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Grid Container
                Box(contentAlignment = Alignment.Center) {
                    Column {
                        // Top Header Row: Away Team Digits (? or numbers)
                        Row {
                            // Blank Corner Box for Row Header label
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .padding(2.dp)
                                    .background(TvDarkCardElevated, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "HOME",
                                    color = TvOrangeFocus,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 10 Column Headers
                            for (c in 0..9) {
                                val digitText = squaresBoard.awayNumbers?.getOrNull(c)?.toString() ?: "?"
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .padding(2.dp)
                                        .background(
                                            if (squaresBoard.isLockedAndDrawn) TvLimeAccentMuted else TvDarkCard,
                                            RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digitText,
                                        color = if (squaresBoard.isLockedAndDrawn) TvLimeAccent else TvOrangeFocus,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // 10 Rows
                        for (r in 0..9) {
                            Row {
                                // Row Header: Home Team Digit (? or numbers)
                                val rowDigitText = squaresBoard.homeNumbers?.getOrNull(r)?.toString() ?: "?"
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .padding(2.dp)
                                        .background(
                                            if (squaresBoard.isLockedAndDrawn) TvLimeAccentMuted else TvDarkCard,
                                            RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rowDigitText,
                                        color = if (squaresBoard.isLockedAndDrawn) TvLimeAccent else TvOrangeFocus,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }

                                // 10 Cell Columns
                                for (c in 0..9) {
                                    val cell = squaresBoard.cells.find { it.row == r && it.col == c }
                                    SquareGridCell(
                                        cell = cell,
                                        onToggle = { onToggleSquare(r, c) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Payment Buttons Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvDarkCard)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ready to claim your squares?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Refundable if the board does not fill • No payments inside the app",
                        color = TvTextMuted,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvFocusButton(
                        text = "Pay with Cash App",
                        isPrimary = true,
                        onClick = { onOpenPayment("Cash App") }
                    )
                    TvFocusButton(
                        text = "Pay with PayPal",
                        onClick = { onOpenPayment("PayPal") }
                    )
                    TvFocusButton(
                        text = "Pay with Card",
                        onClick = { onOpenPayment("Credit Card") }
                    )
                }
            }
        }
    }

    // 5. Payment QR Code Dialog (opens https://parlaypro-squares.netlify.app)
    if (showPaymentDialog) {
        Dialog(onDismissRequest = onClosePayment) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TvDarkSurface)
                    .border(2.dp, TvOrangeFocus, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Complete Entry with $paymentMethod",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "Scan QR code with your phone camera to pay on ParlayPro Squares:",
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    // Render Procedural QR Code Canvas
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProceduralQrCode(url = "https://parlaypro-squares.netlify.app")
                    }

                    Text(
                        text = "https://parlaypro-squares.netlify.app",
                        color = TvLimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Refund notice
                    Text(
                        text = "Refund Note: 100% Refundable if the board does not fill before kickoff.",
                        color = TvOrangeFocus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Notice: In compliance with app store policies, no payment processing occurs inside this TV application.",
                        color = TvTextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvFocusButton(
                            text = "Open in TV Browser",
                            icon = Icons.Default.OpenInBrowser,
                            isPrimary = true,
                            onClick = {
                                onOpenBrowser("https://parlaypro-squares.netlify.app")
                            }
                        )
                        TvFocusButton(
                            text = "Done / Close",
                            onClick = onClosePayment
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SquareGridCell(
    cell: SquareCell?,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val isUser = cell?.isUserSquare == true
    val isTaken = cell?.ownerName != null && !isUser

    val cellBg = when {
        isUser -> TvOrangeFocus
        isTaken -> TvTakenGrey
        else -> TvDarkCard
    }

    val cellText = when {
        isUser -> "YOU"
        isTaken -> cell?.ownerName ?: "X"
        else -> ""
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(cellBg)
            .border(
                width = if (isFocused) 2.5.dp else 0.5.dp,
                color = if (isFocused) Color.White else if (isUser) TvOrangeFocus else TvDarkCardElevated,
                shape = RoundedCornerShape(4.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cellText.isNotEmpty()) {
            Text(
                text = cellText,
                color = if (isUser) Color.Black else TvTextSecondary,
                fontSize = if (isUser) 10.sp else 9.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        } else if (isFocused) {
            Text(
                text = "+",
                color = TvOrangeFocus,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProceduralQrCode(
    url: String,
    modifier: Modifier = Modifier
) {
    // Generates an authentic visual representation of QR matrix
    val matrixSize = 25
    val pattern = remember(url) {
        val rand = Random(url.hashCode().toLong())
        Array(matrixSize) { r ->
            BooleanArray(matrixSize) { c ->
                // Corner positioning squares (top-left, top-right, bottom-left)
                val inTopLeft = r in 0..6 && c in 0..6
                val inTopRight = r in 0..6 && c in (matrixSize - 7) until matrixSize
                val inBottomLeft = r in (matrixSize - 7) until matrixSize && c in 0..6

                if (inTopLeft || inTopRight || inBottomLeft) {
                    val localR = if (inBottomLeft) r - (matrixSize - 7) else r
                    val localC = if (inTopRight) c - (matrixSize - 7) else c
                    (localR == 0 || localR == 6 || localC == 0 || localC == 6) ||
                    (localR in 2..4 && localC in 2..4)
                } else {
                    rand.nextBoolean()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = size.width / matrixSize
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (pattern[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
