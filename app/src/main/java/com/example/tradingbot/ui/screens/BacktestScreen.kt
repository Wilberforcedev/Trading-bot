package com.example.tradingbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.tradingbot.model.BacktestSummary
import com.example.tradingbot.model.StrategyType
import com.example.tradingbot.ui.MainViewModel
import com.example.tradingbot.ui.theme.AccentCyan
import com.example.tradingbot.ui.theme.AccentGold
import com.example.tradingbot.ui.theme.BorderStroke
import com.example.tradingbot.ui.theme.LossRed
import com.example.tradingbot.ui.theme.PrimaryBlue
import com.example.tradingbot.ui.theme.ProfitGreen
import com.example.tradingbot.ui.theme.SurfaceDark
import com.example.tradingbot.ui.theme.SurfaceElevated
import com.example.tradingbot.ui.theme.SurfaceVariantDark
import com.example.tradingbot.ui.theme.TextMuted
import com.example.tradingbot.ui.theme.TextPrimary
import com.example.tradingbot.ui.theme.TextSecondary

@Composable
fun BacktestScreen(viewModel: MainViewModel) {
    val datasets by viewModel.availableDatasets.collectAsState()
    val selectedDataset by viewModel.selectedDataset.collectAsState()
    val backtestResult by viewModel.backtestResult.collectAsState()
    val batchResults by viewModel.batchBacktestResults.collectAsState()
    val isBacktesting by viewModel.isBacktesting.collectAsState()
    val botConfig by viewModel.botConfig.collectAsState()

    var selectedStrategy by remember { mutableStateOf(botConfig.strategy) }
    var fastPeriod by remember { mutableIntStateOf(botConfig.fastSmaPeriod) }
    var slowPeriod by remember { mutableIntStateOf(botConfig.slowSmaPeriod) }
    var expirationCandles by remember { mutableIntStateOf(1) }
    var useMartingale by remember { mutableStateOf(true) }

    var datasetDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Historical Engine Config Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HISTORICAL DATA BACKTESTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Test strategies against ${datasets.size} real 1-minute historical datasets from Sharon Trading Bot repository.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dataset Selector Dropdown
                    Text("Select Historical Dataset:", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .border(1.dp, BorderStroke, RoundedCornerShape(8.dp))
                            .clickable { datasetDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDataset.ifEmpty { "Choose CSV File..." },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = datasetDropdownExpanded,
                            onDismissRequest = { datasetDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            datasets.forEach { fileName ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = fileName,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSelectedDataset(fileName)
                                        datasetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Strategy Selector Chips
                    Text("Strategy Engine:", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    StrategyType.entries.forEach { strat ->
                        val isSelected = strat == selectedStrategy
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else SurfaceVariantDark)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedStrategy = strat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = strat.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryBlue else TextPrimary
                                )
                                Text(
                                    text = strat.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Parameters Row: Fast SMA, Slow SMA, Expiration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fast SMA (${fastPeriod})", fontSize = 10.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(3, 5, 8).forEach { v ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (fastPeriod == v) AccentGold else SurfaceVariantDark)
                                            .clickable { fastPeriod = v }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "$v",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (fastPeriod == v) Color.Black else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Slow SMA (${slowPeriod})", fontSize = 10.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(8, 14, 50).forEach { v ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (slowPeriod == v) AccentCyan else SurfaceVariantDark)
                                            .clickable { slowPeriod = v }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "$v",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (slowPeriod == v) Color.Black else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Expiry (M1)", fontSize = 10.sp, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1, 2, 3).forEach { v ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (expirationCandles == v) PrimaryBlue else SurfaceVariantDark)
                                            .clickable { expirationCandles = v }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "${v}m",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (expirationCandles == v) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Martingale toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simulate Martingale (2.0x Multiplier)",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Switch(
                            checked = useMartingale,
                            onCheckedChange = { useMartingale = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ProfitGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedDataset.isNotEmpty()) {
                                    viewModel.runBacktest(
                                        fileName = selectedDataset,
                                        strategy = selectedStrategy,
                                        fastPeriod = fastPeriod,
                                        slowPeriod = slowPeriod,
                                        expiration = expirationCandles,
                                        useMartingale = useMartingale
                                    )
                                }
                            },
                            enabled = !isBacktesting && selectedDataset.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("run_backtest_button")
                        ) {
                            if (isBacktesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Dataset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.runBatchBacktest(
                                    strategy = selectedStrategy,
                                    fastPeriod = fastPeriod,
                                    slowPeriod = slowPeriod,
                                    expiration = expirationCandles,
                                    useMartingale = useMartingale
                                )
                            },
                            enabled = !isBacktesting && datasets.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("run_batch_backtest_button")
                        ) {
                            Text("Batch (All 24)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Single Backtest Result Display
        backtestResult?.let { result ->
            item {
                BacktestResultCard(result = result)
            }
        }

        // Batch Backtest Results Display
        if (batchResults.isNotEmpty()) {
            item {
                val totalOrders = batchResults.sumOf { it.totalOrders }
                val totalWins = batchResults.sumOf { it.wins }
                val totalLoses = batchResults.sumOf { it.loses }
                val totalDraws = batchResults.sumOf { it.draws }
                val overallWinRate = if (totalOrders > 0) (totalWins.toDouble() / totalOrders) * 100.0 else 0.0
                val totalProfit = batchResults.sumOf { it.netProfit }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BATCH BACKTEST SUMMARY (${batchResults.size} PAIRS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("OVERALL WIN RATE", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${String.format("%.2f", overallWinRate)}%",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (overallWinRate >= 55.0) ProfitGreen else AccentGold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SIMULATED PROFIT", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = "${if (totalProfit >= 0) "+" else ""}$${String.format("%,.2f", totalProfit)}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalProfit >= 0) ProfitGreen else LossRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total Orders: $totalOrders | Wins: $totalWins | Loses: $totalLoses | Draws: $totalDraws",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(batchResults) { summary ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = summary.symbol,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = summary.fileName,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format("%.1f", summary.winRate)}% (${summary.wins}W / ${summary.loses}L)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.winRate >= 55.0) ProfitGreen else TextPrimary
                            )
                            Text(
                                text = "${if (summary.netProfit >= 0) "+" else ""}$${String.format("%.2f", summary.netProfit)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (summary.netProfit >= 0) ProfitGreen else LossRed
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BacktestResultCard(result: BacktestSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (result.winRate >= 55.0) ProfitGreen.copy(alpha = 0.5f) else BorderStroke,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BACKTEST REPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${result.symbol} • ${result.fileName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (result.winRate >= 55.0) ProfitGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", result.winRate)}% WIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.winRate >= 55.0) ProfitGreen else LossRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar of Win Rate
            LinearProgressIndicator(
                progress = { (result.winRate / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (result.winRate >= 55.0) ProfitGreen else AccentGold,
                trackColor = SurfaceVariantDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Orders", value = "${result.totalOrders}")
                StatItem(label = "Wins", value = "${result.wins}", color = ProfitGreen)
                StatItem(label = "Draws", value = "${result.draws}", color = TextSecondary)
                StatItem(label = "Losses", value = "${result.loses}", color = LossRed)
                StatItem(
                    label = "Net Profit",
                    value = "${if (result.netProfit >= 0) "+" else ""}$${String.format("%.2f", result.netProfit)}",
                    color = if (result.netProfit >= 0) ProfitGreen else LossRed
                )
            }
        }
    }
}
