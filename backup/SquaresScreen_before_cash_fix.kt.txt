package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Game
import com.example.model.SquaresBoard
import com.example.model.SquaresConfig
import com.example.model.SquaresPayouts
import com.example.ui.components.TvFocusButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.*
import com.example.util.SquaresAdminManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private sealed interface AdminDialogState {
    data object None : AdminDialogState
    data object SetInitialPin : AdminDialogState
    data object EnterPinForAdminZone : AdminDialogState
    data object ChangePinOld : AdminDialogState
    data object ChangePinNew : AdminDialogState
    data object ConfirmResetBoard : AdminDialogState
    data object ConfirmReleaseAllPending : AdminDialogState
    data object CustomPriceInput : AdminDialogState
}

@Composable
fun SquaresScreen(
    games: List<Game>,
    selectedGame: Game?,
    squaresBoard: SquaresBoard,
    boardVersions: List<Int> = listOf(1),
    isLoading: Boolean = false,
    showPaymentDialog: Boolean,
    paymentMethod: String,
    onSelectGame: (Game) -> Unit = {},
    onSetPrice: (Int) -> Unit = {},
    onSelectPrice: (Int) -> Unit = onSetPrice,
    onSelectPayoutType: (String) -> Unit = {},
    onToggleSquare: (Int, Int) -> Unit = { _, _ -> },
    onOpenPaymentQr: (String) -> Unit = {},
    onOpenPayment: (String) -> Unit = onOpenPaymentQr,
    onClosePaymentQr: () -> Unit = {},
    onClosePayment: () -> Unit = onClosePaymentQr,
    onOpenLink: (String) -> Unit = {},
    onOpenBrowser: (String) -> Unit = onOpenLink,
    onSubmitPendingEntry: (String, Int, String) -> Boolean = { _, _, _ -> false },
    onConfirmPendingPaid: (String) -> List<Int> = { emptyList() },
    onRejectPendingEntry: (String) -> Unit = {},
    onReleaseAllPending: () -> Unit = {},
    onToggleLockBoard: () -> Unit = {},
    onLockNumbers: () -> Unit = {},
    onCreateNextBoardVersion: () -> Unit = {},
    onSwitchBoardVersion: (Int) -> Unit = {},
    onResetBoard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isLoading && squaresBoard.cells.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TvDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = TvOrangeFocus,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Loading Squares Pool Board...",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val adminManager = remember { SquaresAdminManager(context.applicationContext) }

    // Standard ParlayPro price options:
    // $5, $10, $15, $20, $25, $30, $40, $50, $75, $100, $150, $200, Custom
    val standardPrices = listOf(5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 150, 200)

    // Admin Session State (unlocked for at most 5 minutes)
    var isAdminUnlocked by remember { mutableStateOf(adminManager.isSessionUnlocked()) }
    var sessionRemainingSeconds by remember { mutableIntStateOf(adminManager.getRemainingSessionSeconds()) }

    // Lockout countdown state
    var lockoutRemaining by remember { mutableIntStateOf(adminManager.getRemainingLockoutSeconds()) }

    // Buyer Entry State
    var buyerInitials by remember { mutableStateOf("") }
    var buyerSquareCount by remember { mutableIntStateOf(1) }
    var buyerPaymentMethod by remember { mutableStateOf("CASH APP") } // CASH, CASH APP, PAYPAL, ZELLE
    var showBuyerPaymentModal by remember { mutableStateOf(false) }

    // Admin Dialog state
    var adminDialog by remember { mutableStateOf<AdminDialogState>(AdminDialogState.None) }
    var pinFirstEntry by remember { mutableStateOf("") }
    var pinSecondEntry by remember { mutableStateOf("") }
    var pinCurrentEntry by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }
    var customPriceInputText by remember { mutableStateOf("") }

    // Confirmation message for admin actions
    var adminToastMessage by remember { mutableStateOf<String?>(null) }

    // Focused cell for inspector
    var focusedCellIndex by remember { mutableStateOf<Int?>(null) }

    // Auto-lock and lockout ticker (only ticks when unlocked or locked out to prevent useless recomposition)
    LaunchedEffect(Unit) {
        while (true) {
            val isUnlocked = adminManager.isSessionUnlocked()
            val remLock = adminManager.getRemainingLockoutSeconds()
            if (isUnlocked || isAdminUnlocked || remLock > 0 || lockoutRemaining > 0) {
                sessionRemainingSeconds = adminManager.getRemainingSessionSeconds()
                isAdminUnlocked = isUnlocked
                lockoutRemaining = remLock
            }
            delay(1000L)
        }
    }

    // Calculations
    val soldCells = remember(squaresBoard.cells) { squaresBoard.cells.filter { it.ownerName.isNotBlank() } }
    val soldCount = soldCells.size
    val pendingCount = remember(squaresBoard.pendingEntries) { squaresBoard.pendingEntries.sumOf { it.count } }
    val availableCount = (100 - soldCount - pendingCount).coerceAtLeast(0)

    // Check if price / board type can be changed (only when NO sales and NO pending entries exist)
    val isSetupLocked = soldCount > 0 || pendingCount > 0

    // Live Pot & 15% Admin Fee calculation
    val payouts = remember(squaresBoard.pricePerSquare, squaresBoard.payoutType) {
        SquaresConfig.calculatePayouts(squaresBoard.pricePerSquare, squaresBoard.payoutType)
    }
    val isHalves = payouts.isHalves

    // Sales summary calculations
    val totalCollected = squaresBoard.sales.sumOf { it.totalAmount }
    val totalCollectedCents = totalCollected * 100L
    val adminFeeCollectedCents = Math.round(totalCollectedCents * (SquaresConfig.ADMIN_FEE_PERCENT / 100.0))
    val prizePoolCollectedCents = totalCollectedCents - adminFeeCollectedCents
    val adminFeeCollected = adminFeeCollectedCents / 100.0
    val prizePoolCollected = prizePoolCollectedCents / 100.0

    val cashSales = squaresBoard.sales.filter { it.paymentMethod.equals("CASH", ignoreCase = true) }
    val cashAppSales = squaresBoard.sales.filter { it.paymentMethod.equals("CASH APP", ignoreCase = true) }
    val paypalSales = squaresBoard.sales.filter { it.paymentMethod.equals("PAYPAL", ignoreCase = true) }
    val zelleSales = squaresBoard.sales.filter { it.paymentMethod.equals("ZELLE", ignoreCase = true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        // 1. TOP HEADER & ADMIN ACCESS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sports Squares",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TvOrangeFocus.copy(alpha = 0.2f))
                                .border(1.dp, TvOrangeFocus, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Board #${squaresBoard.boardVersion}${if (soldCount >= 100) " · FULL" else ""}",
                                color = TvOrangeFocus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        if (squaresBoard.isBoardLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFE53935), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BOARD LOCKED",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "ParlayPro Pool · Price ${SquaresConfig.formatMoney(payouts.pricePerSquare.toDouble())}/sq · Total Pot ${SquaresConfig.formatMoney(payouts.totalPot)} | Operator fee 15% | Prize Pool ${SquaresConfig.formatMoney(payouts.prizePool)} · ${if (isHalves) "Halves (50% each)" else "Quarters (25% each)"}",
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdminUnlocked) {
                        val minutes = sessionRemainingSeconds / 60
                        val seconds = sessionRemainingSeconds % 60
                        val timeStr = String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvLimeAccent.copy(alpha = 0.15f))
                                .border(1.dp, TvLimeAccent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Admin Active ($timeStr)",
                                color = TvLimeAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TvFocusButton(
                            text = "Lock Admin",
                            icon = Icons.Default.Lock,
                            onClick = {
                                adminManager.lockSession()
                                isAdminUnlocked = false
                            }
                        )
                    } else {
                        TvFocusButton(
                            text = "Admin Zone",
                            icon = Icons.Default.AdminPanelSettings,
                            onClick = {
                                pinErrorText = null
                                pinFirstEntry = ""
                                pinSecondEntry = ""
                                pinCurrentEntry = ""
                                adminDialog = if (!adminManager.isPinSet()) {
                                    AdminDialogState.SetInitialPin
                                } else {
                                    AdminDialogState.EnterPinForAdminZone
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. MATCHUP SELECTOR & BOARD VERSION SELECTOR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Pool Matchup",
                    color = TvTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Board Version Switcher
                if (boardVersions.size > 1 || isAdminUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Boards: ",
                            color = TvTextSecondary,
                            fontSize = 12.sp
                        )
                        boardVersions.forEach { ver ->
                            val isCurrent = squaresBoard.boardVersion == ver
                            TvFocusButton(
                                text = "#$ver",
                                isPrimary = isCurrent,
                                onClick = { onSwitchBoardVersion(ver) },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        if (isAdminUnlocked) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TvFocusButton(
                                text = "+ New Board",
                                icon = Icons.Default.Add,
                                onClick = { onCreateNextBoardVersion() }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvDarkSurface)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "General Championship / Super Bowl Pool",
                        color = TvLimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(games) { g ->
                        val isSelected = selectedGame?.id == g.id
                        TvFocusCard(
                            onClick = { onSelectGame(g) },
                            modifier = Modifier.width(210.dp),
                            focusedBorderColor = TvOrangeFocus,
                            borderColor = if (isSelected) TvOrangeFocus else Color.Transparent
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = g.league,
                                        color = TvLimeAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "ACTIVE",
                                            color = TvOrangeFocus,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${g.awayTeam.displayName} @ ${g.homeTeam.displayName}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (g.state == "in") "${g.awayScore} - ${g.homeScore} (${g.statusDetail})" else g.statusDetail,
                                    color = if (g.state == "in") Color(0xFFFF5252) else TvTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (g.state == "in") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. POT BANNER & LIVE PAYOUT CALCULATOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2A344A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL POT",
                                color = TvTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = SquaresConfig.formatMoney(payouts.totalPot),
                                color = TvLimeAccent,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "100 squares × ${SquaresConfig.formatMoney(payouts.pricePerSquare.toDouble())} per square",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Pot ${SquaresConfig.formatMoney(payouts.totalPot)} | Operator fee 15% | Prize Pool ${SquaresConfig.formatMoney(payouts.prizePool)}",
                                color = TvOrangeFocus,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Payout breakdown boxes from prize pool
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isHalves) {
                                PayoutBadge(title = "HALFTIME (50%)", amountStr = SquaresConfig.formatMoney(payouts.q2Payout))
                                PayoutBadge(title = "FINAL (50%)", amountStr = SquaresConfig.formatMoney(payouts.finalPayout), isHighlight = true)
                            } else {
                                PayoutBadge(title = "1ST QUARTER (25%)", amountStr = SquaresConfig.formatMoney(payouts.q1Payout))
                                PayoutBadge(title = "HALFTIME (25%)", amountStr = SquaresConfig.formatMoney(payouts.q2Payout))
                                PayoutBadge(title = "3RD QUARTER (25%)", amountStr = SquaresConfig.formatMoney(payouts.q3Payout))
                                PayoutBadge(title = "FINAL (25%)", amountStr = SquaresConfig.formatMoney(payouts.finalPayout), isHighlight = true)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. LEGEND & STATUS STRIP (Open / Pending / Sold)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvDarkSurface.copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sold
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(TvOrangeFocus)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sold: $soldCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Pending
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFFB300))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pending: $pendingCount",
                            color = Color(0xFFFFE082),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Open / Available
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1E2838))
                                .border(1.dp, Color(0xFF475569), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open / Available: $availableCount",
                            color = TvLimeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Numbers locked indicator
                Text(
                    text = if (squaresBoard.isNumbersLocked) "Numbers Locked" else "Numbers Not Locked (?)",
                    color = if (squaresBoard.isNumbersLocked) TvLimeAccent else Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 5. BUYER ENTRY PANEL (Plain Buyer View)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2A344A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BUY SQUARES (Pending Payment)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your initials, select how many squares you want, and choose your payment method. Submitting reserves your squares as Pending until the admin confirms payment.",
                        color = TvTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (squaresBoard.isBoardLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFB71C1C).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "This board is currently LOCKED by the organizer. No new entries can be accepted.",
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (availableCount <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE65100).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Board Full / Pending Confirmation · All 100 squares are either sold or pending admin confirmation.",
                                color = Color(0xFFFFE0B2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Initials entry
                            Column(modifier = Modifier.width(180.dp)) {
                                Text(
                                    text = "Buyer Initials (2-3 letters)",
                                    color = TvTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = buyerInitials,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isLetter() }.uppercase().take(3)
                                        buyerInitials = filtered
                                    },
                                    singleLine = true,
                                    placeholder = { Text("ABC", color = Color(0xFF64748B), fontSize = 13.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TvOrangeFocus,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = TvDarkBackground,
                                        unfocusedContainerColor = TvDarkBackground
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Number of squares stepper
                            Column(modifier = Modifier.width(180.dp)) {
                                Text(
                                    text = "Quantity (max $availableCount)",
                                    color = TvTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TvDarkBackground)
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    TvFocusButton(
                                        text = "-",
                                        onClick = { if (buyerSquareCount > 1) buyerSquareCount-- },
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "$buyerSquareCount",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TvFocusButton(
                                        text = "+",
                                        onClick = { if (buyerSquareCount < availableCount) buyerSquareCount++ },
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // Cost preview
                            val totalDue = buyerSquareCount * squaresBoard.pricePerSquare
                            Column(modifier = Modifier.width(130.dp)) {
                                Text(
                                    text = "Amount Due",
                                    color = TvTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TvDarkBackground)
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$$totalDue",
                                        color = TvLimeAccent,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Payment method selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Payment Method",
                                    color = TvTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("CASH APP", "PAYPAL", "ZELLE", "CASH").forEach { method ->
                                        val isSel = buyerPaymentMethod == method
                                        TvFocusButton(
                                            text = method,
                                            isPrimary = isSel,
                                            onClick = { buyerPaymentMethod = method },
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Submit Button
                            val canSubmit = buyerInitials.length >= 2 && buyerSquareCount in 1..availableCount
                            Column(modifier = Modifier.width(180.dp)) {
                                Spacer(modifier = Modifier.height(18.dp))
                                TvFocusButton(
                                    text = "Pay & Reserve",
                                    icon = Icons.Default.QrCodeScanner,
                                    isPrimary = true,
                                    onClick = {
                                        if (canSubmit) {
                                            showBuyerPaymentModal = true
                                        } else {
                                            Toast.makeText(context, "Enter 2-3 letter initials", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 6. ADMIN ZONE (PIN PROTECTED CONTROLS)
        if (isAdminUnlocked) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141E2E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, TvOrangeFocus),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = TvOrangeFocus,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ADMIN ZONE (ParlayPro Control Panel)",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TvFocusButton(
                                    text = if (squaresBoard.isBoardLocked) "Unlock Board" else "Lock Board",
                                    icon = if (squaresBoard.isBoardLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    onClick = { onToggleLockBoard() }
                                )
                                TvFocusButton(
                                    text = if (squaresBoard.isNumbersLocked) "Numbers Locked" else "Lock Numbers",
                                    icon = Icons.Default.Shuffle,
                                    isPrimary = !squaresBoard.isNumbersLocked,
                                    onClick = { onLockNumbers() }
                                )
                                TvFocusButton(
                                    text = "Change PIN",
                                    icon = Icons.Default.Key,
                                    onClick = {
                                        pinErrorText = null
                                        pinCurrentEntry = ""
                                        pinFirstEntry = ""
                                        pinSecondEntry = ""
                                        adminDialog = AdminDialogState.ChangePinOld
                                    }
                                )
                                TvFocusButton(
                                    text = "Reset Board",
                                    icon = Icons.Default.RestartAlt,
                                    onClick = { adminDialog = AdminDialogState.ConfirmResetBoard }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // (A) BOARD SETUP: PRICE PER SQUARE & PAYOUT TYPE
                        Text(
                            text = "1. Board Setup & Price Per Square (Locked once sales/pending exist)",
                            color = TvTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isSetupLocked) {
                            Text(
                                text = "Locked ($soldCount sold, $pendingCount pending). Reset board to change price or board type.",
                                color = Color(0xFFFFB74D),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "Select square price. Total Pot, 15% Admin Fee, Prize Pool, and payouts update live below.",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Price buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            standardPrices.forEach { p ->
                                val isSel = squaresBoard.pricePerSquare == p
                                TvFocusButton(
                                    text = "$$p",
                                    isPrimary = isSel,
                                    onClick = { if (!isSetupLocked) onSelectPrice(p) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                            TvFocusButton(
                                text = "Custom",
                                isPrimary = squaresBoard.pricePerSquare !in standardPrices,
                                onClick = {
                                    if (!isSetupLocked) {
                                        customPriceInputText = "${squaresBoard.pricePerSquare}"
                                        adminDialog = AdminDialogState.CustomPriceInput
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Prize Pool & Admin Fee preview box next to price options
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            text = "Total Pot: ${SquaresConfig.formatMoney(payouts.totalPot)}",
                                            color = TvLimeAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Admin Fee (15%): ${SquaresConfig.formatMoney(payouts.adminFee)}",
                                            color = TvOrangeFocus,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Prize Pool (85%): ${SquaresConfig.formatMoney(payouts.prizePool)}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val payoutsLine = if (isHalves) {
                                    "Winner Payouts (Prize Pool): Halftime ${SquaresConfig.formatMoney(payouts.q2Payout)} (50%) · Final ${SquaresConfig.formatMoney(payouts.finalPayout)} (50%)"
                                } else {
                                    "Winner Payouts (Prize Pool): Q1 ${SquaresConfig.formatMoney(payouts.q1Payout)} · Halftime ${SquaresConfig.formatMoney(payouts.q2Payout)} · Q3 ${SquaresConfig.formatMoney(payouts.q3Payout)} · Final ${SquaresConfig.formatMoney(payouts.finalPayout)} (25% each)"
                                }
                                Text(
                                    text = payoutsLine,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payout Type Selector
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Payout Structure: ",
                                color = TvTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TvFocusButton(
                                text = "Quarters (25% Each)",
                                isPrimary = !isHalves,
                                onClick = { if (!isSetupLocked) onSelectPayoutType("QUARTERS") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TvFocusButton(
                                text = "Halves (50% / 50%)",
                                isPrimary = isHalves,
                                onClick = { if (!isSetupLocked) onSelectPayoutType("HALVES") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF2A344A))
                        Spacer(modifier = Modifier.height(12.dp))

                        // (B) MANAGE PENDING PAYMENTS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "2. Manage Pending Payments (${squaresBoard.pendingEntries.size} pending)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Confirm payment to assign random squares and mark Sold. Reject or Release All to free squares.",
                                    color = TvTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            if (squaresBoard.pendingEntries.isNotEmpty()) {
                                TvFocusButton(
                                    text = "Release All Pending",
                                    icon = Icons.Default.DeleteSweep,
                                    onClick = { adminDialog = AdminDialogState.ConfirmReleaseAllPending }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (squaresBoard.pendingEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TvDarkBackground)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pending payments waiting for confirmation.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                squaresBoard.pendingEntries.forEach { entry ->
                                    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TvDarkBackground)
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFB300)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = entry.initials,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "${entry.count} Square${if (entry.count > 1) "s" else ""} · $$${entry.totalAmount} Due",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Method: ${entry.paymentMethod} · Requested at $timeStr",
                                                    color = TvTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TvFocusButton(
                                                text = "Confirm Paid",
                                                icon = Icons.Default.CheckCircle,
                                                isPrimary = true,
                                                onClick = {
                                                    val assigned = onConfirmPendingPaid(entry.id)
                                                    adminToastMessage = "Confirmed ${entry.initials}! Assigned ${assigned.size} squares."
                                                    Toast.makeText(context, "Confirmed ${entry.initials} (${assigned.size} squares)", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            TvFocusButton(
                                                text = "Reject",
                                                icon = Icons.Default.Cancel,
                                                onClick = {
                                                    onRejectPendingEntry(entry.id)
                                                    Toast.makeText(context, "Rejected pending entry for ${entry.initials}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF2A344A))
                        Spacer(modifier = Modifier.height(12.dp))

                        // (C) SALES SUMMARY PER PAYMENT METHOD & FEE BREAKDOWN
                        Text(
                            text = "3. Sales Summary & Collection Breakdown",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 1: Board Financials (Squares Sold, Total Collected, Admin Fee 15%, Prize Pool)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SalesStatCard("SQUARES SOLD", "$soldCount / 100", "${squaresBoard.sales.size} sales", TvOrangeFocus, Modifier.weight(1f))
                            SalesStatCard("TOTAL COLLECTED", SquaresConfig.formatMoney(totalCollected.toDouble()), "100% of receipts", TvLimeAccent, Modifier.weight(1f))
                            SalesStatCard("ADMIN FEE (15%)", SquaresConfig.formatMoney(adminFeeCollected), "Operator earnings", Color(0xFFFFB300), Modifier.weight(1f))
                            SalesStatCard("PRIZE POOL", SquaresConfig.formatMoney(prizePoolCollected), "85% for winners", Color.White, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Collected per Payment Method
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SalesStatCard("CASH APP", SquaresConfig.formatMoney(cashAppSales.sumOf { it.totalAmount }.toDouble()), "${cashAppSales.size} sales", Color(0xFF00D632), Modifier.weight(1f))
                            SalesStatCard("PAYPAL", SquaresConfig.formatMoney(paypalSales.sumOf { it.totalAmount }.toDouble()), "${paypalSales.size} sales", Color(0xFF0079C1), Modifier.weight(1f))
                            SalesStatCard("ZELLE", SquaresConfig.formatMoney(zelleSales.sumOf { it.totalAmount }.toDouble()), "${zelleSales.size} sales", Color(0xFF7414CA), Modifier.weight(1f))
                            SalesStatCard("CASH", SquaresConfig.formatMoney(cashSales.sumOf { it.totalAmount }.toDouble()), "${cashSales.size} sales", Color(0xFFFFA000), Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 7. 10x10 BOARD GRID (D-PAD FRIENDLY)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2A344A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val awayName = selectedGame?.awayTeam?.displayName?.ifBlank { "AWAY" } ?: "AWAY"
                    val homeName = selectedGame?.homeTeam?.displayName?.ifBlank { "HOME" } ?: "HOME"

                    Text(
                        text = "10×10 SQUARES GRID",
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Columns: $awayName (Away)  ·  Rows: $homeName (Home)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B1320))
                            .padding(8.dp)
                    ) {
                        // Top Header Row: Away Team axis (0-9 or "?")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Empty corner
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "H \\ A", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            squaresBoard.awayNumbers.forEachIndexed { colIdx, num ->
                                val displayText = if (squaresBoard.isNumbersLocked) "$num" else "?"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .background(Color(0xFF162235)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayText,
                                        color = if (squaresBoard.isNumbersLocked) TvLimeAccent else Color(0xFFFFB74D),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // 10 Data Rows
                        for (r in 0..9) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Left Home Team Number
                                val homeDisplay = if (squaresBoard.isNumbersLocked) "${squaresBoard.homeNumbers.getOrElse(r) { r }}" else "?"
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(34.dp)
                                        .background(Color(0xFF162235)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = homeDisplay,
                                        color = if (squaresBoard.isNumbersLocked) TvLimeAccent else Color(0xFFFFB74D),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                // 10 Cell Columns
                                for (c in 0..9) {
                                    val cellIndex = r * 10 + c
                                    val cell = squaresBoard.cells.getOrNull(cellIndex)
                                    val isSold = !cell?.ownerName.isNullOrBlank()
                                    val owner = cell?.ownerName ?: ""

                                    SquareCellBox(
                                        cellIndex = cellIndex,
                                        row = r,
                                        col = c,
                                        owner = owner,
                                        isSold = isSold,
                                        isFocused = focusedCellIndex == cellIndex,
                                        onFocusChanged = { if (it) focusedCellIndex = cellIndex },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Cell inspector details
                    Spacer(modifier = Modifier.height(8.dp))
                    val inspectCell = focusedCellIndex?.let { squaresBoard.cells.getOrNull(it) }
                    val inspectRow = focusedCellIndex?.let { it / 10 } ?: 0
                    val inspectCol = focusedCellIndex?.let { it % 10 } ?: 0
                    val inspectHomeNum = if (squaresBoard.isNumbersLocked) "${squaresBoard.homeNumbers.getOrElse(inspectRow) { inspectRow }}" else "?"
                    val inspectAwayNum = if (squaresBoard.isNumbersLocked) "${squaresBoard.awayNumbers.getOrElse(inspectCol) { inspectCol }}" else "?"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (inspectCell != null && inspectCell.ownerName.isNotBlank()) {
                                "Selected: Square #${inspectCell.index + 1} ($homeName $inspectHomeNum, $awayName $inspectAwayNum) · Owner: ${inspectCell.ownerName}"
                            } else if (inspectCell != null) {
                                "Selected: Square #${inspectCell.index + 1} ($homeName $inspectHomeNum, $awayName $inspectAwayNum) · OPEN"
                            } else {
                                "Use remote D-pad to inspect squares on the board"
                            },
                            color = TvTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 8. WINNER TRACKER / QUARTERS TRACKER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TvDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2A344A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "WINNER TRACKER & QUARTER PAYOUTS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Payouts determined by the last digit of each team's score at the end of each period.",
                        color = TvTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isHalves) {
                        QuarterPayoutRow("Halftime", SquaresConfig.formatMoney(payouts.q2Payout), "50% of prize pool (score at end of 1st half)", squaresBoard)
                        Spacer(modifier = Modifier.height(6.dp))
                        QuarterPayoutRow("Final Score", SquaresConfig.formatMoney(payouts.finalPayout), "50% of prize pool (final score including overtimes)", squaresBoard)
                    } else {
                        QuarterPayoutRow("1st Quarter", SquaresConfig.formatMoney(payouts.q1Payout), "25% of prize pool (score at end of Q1)", squaresBoard)
                        Spacer(modifier = Modifier.height(6.dp))
                        QuarterPayoutRow("Halftime (Q2)", SquaresConfig.formatMoney(payouts.q2Payout), "25% of prize pool (score at halftime)", squaresBoard)
                        Spacer(modifier = Modifier.height(6.dp))
                        QuarterPayoutRow("3rd Quarter", SquaresConfig.formatMoney(payouts.q3Payout), "25% of prize pool (score at end of Q3)", squaresBoard)
                        Spacer(modifier = Modifier.height(6.dp))
                        QuarterPayoutRow("Final Score", SquaresConfig.formatMoney(payouts.finalPayout), "25% of prize pool (final score including overtimes)", squaresBoard)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 9. REFUND POLICY NOTE (MANDATORY REQUIREMENT)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TvLimeAccent,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Refund Policy: If this board is cancelled or refunded, buyers receive a FULL refund of the entire amount paid, including the 15% operator fee portion. All payments are fully refundable unless at least 75% of squares are sold.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // --- MODALS & DIALOGS ---

    // 1. BUYER PAYMENT MODAL (Shows QR code, amount due, and submits as PENDING)
    if (showBuyerPaymentModal) {
        val totalDue = buyerSquareCount * squaresBoard.pricePerSquare
        Dialog(onDismissRequest = { showBuyerPaymentModal = false }) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDarkSurface)
                    .border(2.dp, TvOrangeFocus, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Complete Payment for $buyerSquareCount Square${if (buyerSquareCount > 1) "s" else ""}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Initials: $buyerInitials · Total Due: $$totalDue via $buyerPaymentMethod",
                        color = TvLimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Payment details according to method
                    when (buyerPaymentMethod) {
                        "CASH APP" -> PaymentQrContent("Cash App", "$$totalDue", "Scan with Cash App camera to pay $$totalDue to the organizer.")
                        "PAYPAL" -> PaymentQrContent("PayPal", "$$totalDue", "Scan with PayPal app or send $$totalDue with note '$buyerInitials Squares'.")
                        "ZELLE" -> PaymentQrContent("Zelle", "$$totalDue", "Send $$totalDue via Zelle to the organizer with note '$buyerInitials Squares'.")
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TvDarkBackground)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = TvOrangeFocus,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Cash in Hand: Hand $$totalDue directly to the organizer.",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Note: Submitting puts your squares in PENDING status. The organizer confirms your payment in the Admin Zone before squares are assigned.",
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TvFocusButton(
                            text = "Cancel",
                            onClick = { showBuyerPaymentModal = false }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TvFocusButton(
                            text = "Confirm & Submit Pending",
                            isPrimary = true,
                            onClick = {
                                val success = onSubmitPendingEntry(buyerInitials, buyerSquareCount, buyerPaymentMethod)
                                showBuyerPaymentModal = false
                                if (success) {
                                    Toast.makeText(context, "Pending entry submitted for $buyerInitials! ($buyerSquareCount sq)", Toast.LENGTH_LONG).show()
                                    buyerInitials = ""
                                    buyerSquareCount = 1
                                } else {
                                    Toast.makeText(context, "Failed to submit. Board may be full or locked.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 2. ADMIN PIN DIALOGS (Salted SHA-256 + 5 wrong attempts -> 60s lockout)
    when (adminDialog) {
        AdminDialogState.None -> Unit

        AdminDialogState.SetInitialPin -> {
            AdminPinKeypadDialog(
                title = "Set Initial Admin PIN",
                subtitle = if (pinFirstEntry.isEmpty()) "Enter 4-digit PIN for organizer controls:" else "Confirm 4-digit PIN:",
                currentInput = pinCurrentEntry,
                errorText = pinErrorText,
                onDigit = { d -> if (pinCurrentEntry.length < 4) pinCurrentEntry += d },
                onBackspace = { if (pinCurrentEntry.isNotEmpty()) pinCurrentEntry = pinCurrentEntry.dropLast(1) },
                onSubmit = {
                    if (pinCurrentEntry.length == 4) {
                        if (pinFirstEntry.isEmpty()) {
                            pinFirstEntry = pinCurrentEntry
                            pinCurrentEntry = ""
                            pinErrorText = null
                        } else {
                            if (pinCurrentEntry == pinFirstEntry) {
                                adminManager.setPin(pinCurrentEntry)
                                adminManager.unlockSession()
                                isAdminUnlocked = true
                                adminDialog = AdminDialogState.None
                                Toast.makeText(context, "Admin PIN set & unlocked!", Toast.LENGTH_SHORT).show()
                            } else {
                                pinErrorText = "PINs do not match. Try again."
                                pinFirstEntry = ""
                                pinCurrentEntry = ""
                            }
                        }
                    }
                },
                onDismiss = { adminDialog = AdminDialogState.None }
            )
        }

        AdminDialogState.EnterPinForAdminZone -> {
            if (lockoutRemaining > 0) {
                AdminLockoutDialog(lockoutRemaining) { adminDialog = AdminDialogState.None }
            } else {
                AdminPinKeypadDialog(
                    title = "Organizer Admin Zone",
                    subtitle = "Enter 4-digit Admin PIN to unlock management:",
                    currentInput = pinCurrentEntry,
                    errorText = pinErrorText,
                    onDigit = { d -> if (pinCurrentEntry.length < 4) pinCurrentEntry += d },
                    onBackspace = { if (pinCurrentEntry.isNotEmpty()) pinCurrentEntry = pinCurrentEntry.dropLast(1) },
                    onSubmit = {
                        if (pinCurrentEntry.length == 4) {
                            if (adminManager.verifyPin(pinCurrentEntry)) {
                                adminManager.unlockSession()
                                isAdminUnlocked = true
                                adminDialog = AdminDialogState.None
                                Toast.makeText(context, "Admin Zone Unlocked (5 min session)", Toast.LENGTH_SHORT).show()
                            } else {
                                val remainingLockout = adminManager.getRemainingLockoutSeconds()
                                if (remainingLockout > 0) {
                                    lockoutRemaining = remainingLockout
                                } else {
                                    val left = 5 - adminManager.getFailedAttempts()
                                    pinErrorText = "Incorrect PIN. $left attempts remaining."
                                    pinCurrentEntry = ""
                                }
                            }
                        }
                    },
                    onDismiss = { adminDialog = AdminDialogState.None }
                )
            }
        }

        AdminDialogState.ChangePinOld -> {
            AdminPinKeypadDialog(
                title = "Change Admin PIN",
                subtitle = "Enter CURRENT 4-digit PIN:",
                currentInput = pinCurrentEntry,
                errorText = pinErrorText,
                onDigit = { d -> if (pinCurrentEntry.length < 4) pinCurrentEntry += d },
                onBackspace = { if (pinCurrentEntry.isNotEmpty()) pinCurrentEntry = pinCurrentEntry.dropLast(1) },
                onSubmit = {
                    if (pinCurrentEntry.length == 4) {
                        if (adminManager.verifyPin(pinCurrentEntry)) {
                            pinCurrentEntry = ""
                            pinErrorText = null
                            adminDialog = AdminDialogState.ChangePinNew
                        } else {
                            pinErrorText = "Incorrect PIN."
                            pinCurrentEntry = ""
                        }
                    }
                },
                onDismiss = { adminDialog = AdminDialogState.None }
            )
        }

        AdminDialogState.ChangePinNew -> {
            AdminPinKeypadDialog(
                title = "Change Admin PIN",
                subtitle = if (pinFirstEntry.isEmpty()) "Enter NEW 4-digit PIN:" else "Confirm NEW 4-digit PIN:",
                currentInput = pinCurrentEntry,
                errorText = pinErrorText,
                onDigit = { d -> if (pinCurrentEntry.length < 4) pinCurrentEntry += d },
                onBackspace = { if (pinCurrentEntry.isNotEmpty()) pinCurrentEntry = pinCurrentEntry.dropLast(1) },
                onSubmit = {
                    if (pinCurrentEntry.length == 4) {
                        if (pinFirstEntry.isEmpty()) {
                            pinFirstEntry = pinCurrentEntry
                            pinCurrentEntry = ""
                            pinErrorText = null
                        } else {
                            if (pinCurrentEntry == pinFirstEntry) {
                                adminManager.setPin(pinCurrentEntry)
                                adminDialog = AdminDialogState.None
                                Toast.makeText(context, "PIN successfully updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                pinErrorText = "PINs do not match. Try again."
                                pinFirstEntry = ""
                                pinCurrentEntry = ""
                            }
                        }
                    }
                },
                onDismiss = { adminDialog = AdminDialogState.None }
            )
        }

        AdminDialogState.ConfirmResetBoard -> {
            ConfirmDialog(
                title = "Reset Board #${squaresBoard.boardVersion}?",
                message = "This will erase all sales, pending entries, and owner initials on this board. Price and board type will be unlocked.",
                confirmText = "Reset Board",
                isDestructive = true,
                onConfirm = {
                    onResetBoard()
                    adminDialog = AdminDialogState.None
                    Toast.makeText(context, "Board reset successfully.", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { adminDialog = AdminDialogState.None }
            )
        }

        AdminDialogState.ConfirmReleaseAllPending -> {
            ConfirmDialog(
                title = "Release All Pending Entries?",
                message = "This will delete all ${squaresBoard.pendingEntries.size} pending buyer requests and free those squares back to Open.",
                confirmText = "Release All",
                isDestructive = true,
                onConfirm = {
                    onReleaseAllPending()
                    adminDialog = AdminDialogState.None
                    Toast.makeText(context, "All pending entries released.", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { adminDialog = AdminDialogState.None }
            )
        }

        AdminDialogState.CustomPriceInput -> {
            Dialog(onDismissRequest = { adminDialog = AdminDialogState.None }) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TvDarkSurface)
                        .border(2.dp, TvOrangeFocus, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Custom Price Per Square",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter custom dollar amount per square:",
                            color = TvTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = customPriceInputText,
                            onValueChange = { customPriceInputText = it.filter { ch -> ch.isDigit() }.take(4) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("e.g. 35", color = Color(0xFF64748B)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TvOrangeFocus,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = TvDarkBackground,
                                unfocusedContainerColor = TvDarkBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val parsedPrice = customPriceInputText.toIntOrNull()
                        if (parsedPrice != null && parsedPrice in 1..1000) {
                            val customPayouts = SquaresConfig.calculatePayouts(parsedPrice, squaresBoard.payoutType)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total Pot: ${SquaresConfig.formatMoney(customPayouts.totalPot)} | Admin Fee (15%): ${SquaresConfig.formatMoney(customPayouts.adminFee)} | Prize Pool: ${SquaresConfig.formatMoney(customPayouts.prizePool)}",
                                color = TvOrangeFocus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TvFocusButton(text = "Cancel", onClick = { adminDialog = AdminDialogState.None })
                            Spacer(modifier = Modifier.width(10.dp))
                            TvFocusButton(
                                text = "Set Price",
                                isPrimary = true,
                                onClick = {
                                    val price = customPriceInputText.toIntOrNull()
                                    if (price != null && price in 1..1000) {
                                        onSelectPrice(price)
                                        adminDialog = AdminDialogState.None
                                    } else {
                                        Toast.makeText(context, "Enter amount between $1 and $1000", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareCellBox(
    cellIndex: Int,
    row: Int,
    col: Int,
    owner: String,
    isSold: Boolean,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(focused) {
        onFocusChanged(focused)
    }

    val backgroundColor = when {
        isSold -> TvOrangeFocus.copy(alpha = 0.9f)
        else -> Color(0xFF131E2E)
    }

    val borderColor = when {
        focused -> TvLimeAccent
        isSold -> Color(0xFFE65100)
        else -> Color(0xFF223046)
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(if (focused) 2.dp else 1.dp, borderColor, RoundedCornerShape(3.dp))
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        if (isSold) {
            Text(
                text = owner,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                maxLines = 1
            )
        } else {
            Text(
                text = "${cellIndex + 1}",
                color = Color(0xFF334155),
                fontSize = 8.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PayoutBadge(title: String, amountStr: String, isHighlight: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlight) TvOrangeFocus.copy(alpha = 0.15f) else Color(0xFF162235))
            .border(1.dp, if (isHighlight) TvOrangeFocus else Color(0xFF2A344A), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isHighlight) TvOrangeFocus else TvTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amountStr,
                color = if (isHighlight) TvLimeAccent else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SalesStatCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TvDarkBackground)
            .border(1.dp, Color(0xFF223046), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, color = TvTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = subtitle, color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

@Composable
private fun QuarterPayoutRow(period: String, amount: String, description: String, board: SquaresBoard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = period, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = TvTextSecondary, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = amount, color = TvLimeAccent, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PaymentQrContent(methodName: String, amount: String, instructions: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TvDarkBackground)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(90.dp)
                    )
                    Text(
                        text = "$methodName Pay",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = instructions,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AdminPinKeypadDialog(
    title: String,
    subtitle: String,
    currentInput: String,
    errorText: String?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TvDarkSurface)
                .border(2.dp, TvOrangeFocus, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = TvTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))

                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in 0 until 4) {
                        val isFilled = i < currentInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) TvLimeAccent else Color(0xFF334155))
                                .border(1.dp, if (isFilled) TvLimeAccent else Color(0xFF64748B), CircleShape)
                        )
                    }
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText, color = Color(0xFFFF5252), fontSize = 11.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // On-screen TV Keypad (1-9, 0, Backspace, Submit)
                val keypad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("DEL", "0", "OK")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    keypad.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { key ->
                                TvFocusButton(
                                    text = key,
                                    isPrimary = key == "OK",
                                    onClick = {
                                        when (key) {
                                            "DEL" -> onBackspace()
                                            "OK" -> onSubmit()
                                            else -> onDigit(key)
                                        }
                                    },
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TvFocusButton(text = "Cancel", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun AdminLockoutDialog(lockoutRemaining: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TvDarkSurface)
                .border(2.dp, Color(0xFFE53935), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Security Lockout", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Too many failed PIN attempts. Admin access locked for $lockoutRemaining seconds.",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                TvFocusButton(text = "Close", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TvDarkSurface)
                .border(2.dp, if (isDestructive) Color(0xFFE53935) else TvOrangeFocus, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(text = title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = message, color = TvTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TvFocusButton(text = "Cancel", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(10.dp))
                    TvFocusButton(
                        text = confirmText,
                        isPrimary = true,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}
