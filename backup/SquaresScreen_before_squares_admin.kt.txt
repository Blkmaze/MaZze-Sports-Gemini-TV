package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Game
import com.example.model.SquaresBoard
import com.example.ui.components.TvFocusButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.*

@Composable
fun SquaresScreen(
    games: List<Game>,
    selectedGame: Game?,
    squaresBoard: SquaresBoard,
    showPaymentDialog: Boolean,
    paymentMethod: String,
    onSelectGame: (Game) -> Unit = {},
    onSetPrice: (Int) -> Unit = {},
    onSelectPrice: (Int) -> Unit = onSetPrice,
    onToggleSquare: (Int, Int) -> Unit = { _, _ -> },
    onOpenPaymentQr: (String) -> Unit = {},
    onOpenPayment: (String) -> Unit = onOpenPaymentQr,
    onClosePaymentQr: () -> Unit = {},
    onClosePayment: () -> Unit = onClosePaymentQr,
    onOpenLink: (String) -> Unit = {},
    onOpenBrowser: (String) -> Unit = onOpenLink,
    modifier: Modifier = Modifier
) {
    val handleSetPrice: (Int) -> Unit = { p -> onSelectPrice(p); onSetPrice(p) }
    val handleOpenPayment: (String) -> Unit = { m -> onOpenPayment(m); onOpenPaymentQr(m) }
    val handleClosePayment: () -> Unit = { onClosePayment(); onClosePaymentQr() }
    val handleOpenBrowser: (String) -> Unit = { l -> onOpenBrowser(l); onOpenLink(l) }
    val prices = listOf(5, 10, 20, 50)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sports Squares (10x10 Pool)",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Pick a game and claim your squares for cash prizes",
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Price: ",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                    prices.forEach { p ->
                        val isSel = squaresBoard.pricePerSquare == p
                        TvFocusButton(
                            text = "$$p",
                            isPrimary = isSel,
                            onClick = { handleSetPrice(p) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Available Games Row
        item {
            Text(
                text = "Select Matchup for Pool",
                color = TvTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (games.isEmpty()) {
                Text(
                    text = "No games scheduled today",
                    color = TvTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(games) { g ->
                        val isSelected = selectedGame?.id == g.id
                        TvFocusCard(
                            onClick = { onSelectGame(g) },
                            modifier = Modifier.width(220.dp)
                        ) { isFocused ->
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = g.league,
                                    color = TvOrangeFocus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${g.awayTeam.displayName} @ ${g.homeTeam.displayName}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 10x10 Grid representation
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvDarkSurface)
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedGame?.let { "${it.awayTeam.displayName} (Columns) vs ${it.homeTeam.displayName} (Rows)" } ?: "Super Bowl / Championship Pool",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Column headers (Away numbers 0-9)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.size(28.dp)) // empty top-left
                        squaresBoard.awayNumbers.forEach { num ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(1.dp)
                                    .background(TvDarkCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$num",
                                    color = TvLimeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 10 Rows with Home numbers
                    for (r in 0 until 10) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            // Row Header (Home number)
                            val rowNum = squaresBoard.homeNumbers.getOrNull(r) ?: r
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(1.dp)
                                    .background(TvDarkCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rowNum",
                                    color = TvOrangeFocus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 10 Cells in this row
                            for (c in 0 until 10) {
                                val cellIndex = r * 10 + c
                                val cell = squaresBoard.cells.getOrNull(cellIndex)
                                val isUser = cell?.isUserSquare == true
                                val isClaimed = !cell?.ownerName.isNullOrBlank()

                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(1.dp)
                                        .background(
                                            when {
                                                isFocused -> Color.White
                                                isUser -> TvOrangeFocus
                                                isClaimed -> TvDarkCard
                                                else -> TvDarkBackground
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isFocused) TvOrangeFocus else TvDarkSurface
                                        )
                                        .focusable(interactionSource = interactionSource)
                                        .clickable(interactionSource = interactionSource, indication = null) {
                                            onToggleSquare(r, c)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUser) {
                                        Text(
                                            text = "ME",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    } else if (isClaimed) {
                                        Text(
                                            text = cell?.ownerName?.take(2)?.uppercase() ?: "",
                                            color = TvTextMuted,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvFocusButton(
                            text = "Pay via Cash App ($${squaresBoard.pricePerSquare})",
                            icon = Icons.Default.QrCode,
                            isPrimary = true,
                            onClick = { handleOpenPayment("Cash App") }
                        )
                        TvFocusButton(
                            text = "Pay via Venmo",
                            icon = Icons.Default.Payment,
                            onClick = { handleOpenPayment("Venmo") }
                        )
                    }
                }
            }
        }
    }

    // Payment QR Dialog
    if (showPaymentDialog) {
        Dialog(onDismissRequest = handleClosePayment) {
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDarkSurface)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pay with $paymentMethod",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send $${squaresBoard.pricePerSquare} per claimed square to lock your picks.",
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color.Black,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TvFocusButton(
                        text = "Done",
                        onClick = handleClosePayment,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
