package com.example.tradingbot.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tradingbot.model.MarketSymbol
import com.example.tradingbot.model.MarketSymbols
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.Trade
import com.example.tradingbot.ui.MainViewModel
import com.example.tradingbot.ui.components.TradingChart
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
fun TerminalScreen(viewModel: MainViewModel) {
    val balance by viewModel.balance.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val currentPrice by viewModel.currentPrice.collectAsState()
    val activeTrades by viewModel.activeTrades.collectAsState()
    val isBotRunning by viewModel.isBotRunning.collectAsState()
    val currentStake by viewModel.currentStake.collectAsState()
    val martingaleStep by viewModel.martingaleStep.collectAsState()
    val indicatorValues by viewModel.indicatorValues.collectAsState()
    val botLogs by viewModel.botLogs.collectAsState()
    val botConfig by viewModel.botConfig.collectAsState()
    val simulationSpeed by viewModel.simulationSpeed.collectAsState()

    val currentSymbol = MarketSymbols.find(botConfig.selectedSymbol)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Account Balance & Status Bar
            AccountHeaderCard(
                balance = balance,
                isBotRunning = isBotRunning,
                martingaleStep = martingaleStep,
                currentStake = currentStake
            )
        }

        item {
            // Market Asset Selector Row (Forex & OTC Stocks)
            AssetSelectorRow(
                selectedSymbol = currentSymbol,
                onSymbolSelected = {
                    viewModel.setSymbol(it)
                    viewModel.updateConfig(botConfig.copy(selectedSymbol = it.code))
                }
            )
        }

        item {
            // Candlestick Chart with SMA Overlays
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentSymbol.code,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ProfitGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${(currentSymbol.payoutRate * 100).toInt()}% PAYOUT",
                                        color = ProfitGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = String.format("%.5f", currentPrice),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Chart legend
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentGold)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "SMA(${botConfig.fastSmaPeriod})",
                                    fontSize = 11.sp,
                                    color = AccentGold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyan)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "SMA(${botConfig.slowSmaPeriod})",
                                    fontSize = 11.sp,
                                    color = AccentCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TradingChart(
                        candles = candles,
                        currentPrice = currentPrice,
                        fastSmaPeriod = botConfig.fastSmaPeriod,
                        slowSmaPeriod = botConfig.slowSmaPeriod
                    )
                }
            }
        }

        item {
            // Live Technical Indicators & ML Probabilities
            IndicatorDashboardCard(
                indicators = indicatorValues,
                strategy = botConfig.strategy
            )
        }

        if (activeTrades.isNotEmpty()) {
            item {
                ActiveTradesSection(
                    activeTrades = activeTrades,
                    currentPrice = currentPrice
                )
            }
        }

        item {
            // Execution Deck: CALL / PUT + Bot Start/Pause + Speed Controls
            ExecutionControlsDeck(
                isBotRunning = isBotRunning,
                currentStake = currentStake,
                martingaleStep = martingaleStep,
                simulationSpeed = simulationSpeed,
                onStartBot = { viewModel.startBot() },
                onStopBot = { viewModel.stopBot() },
                onCall = { viewModel.executeManualTrade(SignalType.CALL) },
                onPut = { viewModel.executeManualTrade(SignalType.PUT) },
                onSpeedChanged = { viewModel.setSimulationSpeed(it) },
                onResetMartingale = { viewModel.resetMartingale() }
            )
        }

        item {
            // Bot Terminal Console
            TerminalConsoleCard(logs = botLogs)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AccountHeaderCard(
    balance: Double,
    isBotRunning: Boolean,
    martingaleStep: Int,
    currentStake: Double
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIRTUAL BALANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$${String.format("%,.2f", balance)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                // Bot Running Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isBotRunning) ProfitGreen.copy(alpha = 0.2f) else SurfaceElevated)
                        .border(
                            1.dp,
                            if (isBotRunning) ProfitGreen else TextMuted,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isBotRunning) ProfitGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBotRunning) "BOT ACTIVE" else "BOT PAUSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBotRunning) ProfitGreen else TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Step $martingaleStep: $${String.format("%.2f", currentStake)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentGold
                )
            }
        }
    }
}

@Composable
fun AssetSelectorRow(
    selectedSymbol: MarketSymbol,
    onSymbolSelected: (MarketSymbol) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MarketSymbols.ALL.forEach { symbol ->
            val isSelected = symbol.code == selectedSymbol.code
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PrimaryBlue else SurfaceDark)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryBlue else BorderStroke,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSymbolSelected(symbol) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("asset_chip_${symbol.code.replace(" ", "_")}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = symbol.code,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(symbol.payoutRate * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else ProfitGreen
                    )
                }
            }
        }
    }
}

@Composable
fun IndicatorDashboardCard(
    indicators: com.example.tradingbot.model.IndicatorValues,
    strategy: com.example.tradingbot.model.StrategyType
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TECHNICAL SIGNALS (${strategy.displayName})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "M1 TIMEFRAME",
                    fontSize = 10.sp,
                    color = AccentCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "Fast SMA",
                    value = indicators.fastSma?.let { String.format("%.5f", it) } ?: "—",
                    color = AccentGold
                )
                MetricPill(
                    label = "Slow SMA",
                    value = indicators.slowSma?.let { String.format("%.5f", it) } ?: "—",
                    color = AccentCyan
                )
                MetricPill(
                    label = "Awesome Osc.",
                    value = indicators.awesomeOscillator?.let { String.format("%+.4f", it) } ?: "—",
                    color = if ((indicators.awesomeOscillator ?: 0.0) >= 0) ProfitGreen else LossRed
                )
                MetricPill(
                    label = "CCI(20)",
                    value = indicators.cci?.let { String.format("%.1f", it) } ?: "—",
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ML Probabilities progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CALL PROBABILITY: ${(indicators.mlCallProbability * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                    Text(
                        text = "PUT PROBABILITY: ${(indicators.mlPutProbability * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { indicators.mlCallProbability },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ProfitGreen,
                    trackColor = LossRed
                )
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun ActiveTradesSection(
    activeTrades: List<Trade>,
    currentPrice: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ACTIVE EXPIRING ORDERS (${activeTrades.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        activeTrades.forEach { trade ->
            val remainingMs = maxOf(0L, trade.closeTime - System.currentTimeMillis())
            val remainingSec = (remainingMs / 1000).toInt()
            val isInTheMoney = when (trade.type) {
                SignalType.CALL -> currentPrice > trade.entryPrice
                SignalType.PUT -> currentPrice < trade.entryPrice
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isInTheMoney) ProfitGreen.copy(alpha = 0.5f) else LossRed.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (trade.type == SignalType.CALL) ProfitGreen else LossRed)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = trade.type.name,
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${trade.symbol} • $${String.format("%.2f", trade.stake)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Strike: ${String.format("%.5f", trade.entryPrice)} | Cur: ${String.format("%.5f", currentPrice)}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${remainingSec}s",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGold
                        )
                        Text(
                            text = if (isInTheMoney) "IN THE MONEY" else "OUT OF MONEY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInTheMoney) ProfitGreen else LossRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionControlsDeck(
    isBotRunning: Boolean,
    currentStake: Double,
    martingaleStep: Int,
    simulationSpeed: Int,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onCall: () -> Unit,
    onPut: () -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onResetMartingale: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Speed & Reset Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "SIM SPEED:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf(1, 2, 5).forEach { speed ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (simulationSpeed == speed) PrimaryBlue else SurfaceVariantDark)
                                .clickable { onSpeedChanged(speed) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${speed}X",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (simulationSpeed == speed) Color.White else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                OutlinedButton(
                    onClick = onResetMartingale,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Martingale",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Stake", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main CALL and PUT Manual Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CALL BUTTON (Green)
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("call_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▲ HIGHER (CALL)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = "Stake $${String.format("%.2f", currentStake)}",
                            fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                }

                // PUT BUTTON (Red)
                Button(
                    onClick = onPut,
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("put_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▼ LOWER (PUT)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Stake $${String.format("%.2f", currentStake)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bot Automation Toggle
            Button(
                onClick = if (isBotRunning) onStopBot else onStartBot,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBotRunning) LossRed.copy(alpha = 0.85f) else PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("bot_toggle_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (isBotRunning) "Pause Bot" else "Start Bot",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBotRunning) "PAUSE AUTOMATED BOT" else "START AUTOMATED TRADING BOT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalConsoleCard(logs: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ProfitGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BOT TERMINAL CONSOLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "${logs.size} LOGS",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF03070E))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        val color = when {
                            log.contains("WON") -> ProfitGreen
                            log.contains("LOST") -> LossRed
                            log.contains("CALL") -> AccentCyan
                            log.contains("PUT") -> AccentGold
                            else -> Color(0xFF90A4AE)
                        }
                        Text(
                            text = log,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
