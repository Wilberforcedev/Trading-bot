package com.example.tradingbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.Trade
import com.example.tradingbot.model.TradeStatus
import com.example.tradingbot.ui.MainViewModel
import com.example.tradingbot.ui.theme.AccentGold
import com.example.tradingbot.ui.theme.BorderStroke
import com.example.tradingbot.ui.theme.LossRed
import com.example.tradingbot.ui.theme.PrimaryBlue
import com.example.tradingbot.ui.theme.ProfitGreen
import com.example.tradingbot.ui.theme.SurfaceDark
import com.example.tradingbot.ui.theme.SurfaceElevated
import com.example.tradingbot.ui.theme.TextMuted
import com.example.tradingbot.ui.theme.TextPrimary
import com.example.tradingbot.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val trades by viewModel.trades.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val botConfig by viewModel.botConfig.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var showResetBalanceDialog by remember { mutableStateOf(false) }

    val totalTrades = trades.size
    val wins = trades.count { it.status == TradeStatus.WON }
    val draws = trades.count { it.status == TradeStatus.DRAW }
    val losses = trades.count { it.status == TradeStatus.LOST }
    val winRate = if (totalTrades > 0) (wins.toDouble() / totalTrades) * 100.0 else 0.0
    val totalProfitLoss = trades.sumOf { it.profitLoss }
    val totalGrossWins = trades.filter { it.status == TradeStatus.WON }.sumOf { it.profitLoss }
    val totalGrossLosses = trades.filter { it.status == TradeStatus.LOST }.sumOf { it.stake }
    val profitFactor = if (totalGrossLosses > 0) totalGrossWins / totalGrossLosses else if (totalGrossWins > 0) 99.0 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Performance Analytics Dashboard Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PERFORMANCE ANALYTICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        Row {
                            IconButton(
                                onClick = { showResetBalanceDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Balance",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear History",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NET PROFIT / LOSS", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = "${if (totalProfitLoss >= 0) "+" else ""}$${String.format("%,.2f", totalProfitLoss)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalProfitLoss >= 0) ProfitGreen else LossRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("WIN RATE", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = "${String.format("%.1f", winRate)}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (winRate >= 55.0) ProfitGreen else AccentGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mini Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Total Orders", value = "$totalTrades")
                        StatItem(label = "Wins", value = "$wins", color = ProfitGreen)
                        StatItem(label = "Draws", value = "$draws", color = TextSecondary)
                        StatItem(label = "Losses", value = "$losses", color = LossRed)
                        StatItem(label = "Profit Factor", value = String.format("%.2f", profitFactor))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CLOSED TRADES HISTORY (${trades.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                if (trades.isNotEmpty()) {
                    Text(
                        text = "Real-time SQLite Sync",
                        fontSize = 10.sp,
                        color = ProfitGreen
                    )
                }
            }
        }

        if (trades.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderStroke, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No trades executed yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start the automated bot or place manual trades on Terminal",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(trades) { trade ->
                TradeHistoryCard(trade = trade)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Trade History") },
            text = { Text("Are you sure you want to delete all saved trade records?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTradeHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showResetBalanceDialog = false },
            title = { Text("Reset Account Balance") },
            text = { Text("Reset virtual trading balance back to initial deposit ($${String.format("%.2f", botConfig.initialDeposit)})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetBalance(botConfig.initialDeposit)
                        showResetBalanceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetBalanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TradeHistoryCard(trade: Trade) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(trade.closeTime))
    val isWin = trade.status == TradeStatus.WON
    val isDraw = trade.status == TradeStatus.DRAW

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Direction Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (trade.type == SignalType.CALL) ProfitGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (trade.type == SignalType.CALL) "▲" else "▼",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trade.type == SignalType.CALL) ProfitGreen else LossRed
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trade.symbol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevated)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Step ${trade.martingaleStep}",
                                fontSize = 9.sp,
                                color = AccentGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Strike: ${String.format("%.5f", trade.entryPrice)} → Close: ${trade.closePrice?.let { String.format("%.5f", it) } ?: "—"}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Text(
                        text = "$dateStr • ${trade.strategy.name.replace("_", " ")}",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        isWin -> "+$${String.format("%.2f", trade.profitLoss)}"
                        isDraw -> "$0.00"
                        else -> "-$${String.format("%.2f", trade.stake)}"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isWin -> ProfitGreen
                        isDraw -> TextSecondary
                        else -> LossRed
                    }
                )

                Text(
                    text = trade.status.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isWin -> ProfitGreen
                        isDraw -> TextSecondary
                        else -> LossRed
                    }
                )

                Text(
                    text = "Stake: $${String.format("%.2f", trade.stake)}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
