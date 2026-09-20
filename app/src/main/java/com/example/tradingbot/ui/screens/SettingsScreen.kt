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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.tradingbot.model.StrategyType
import com.example.tradingbot.ui.MainViewModel
import com.example.tradingbot.ui.theme.AccentGold
import com.example.tradingbot.ui.theme.BorderStroke
import com.example.tradingbot.ui.theme.PrimaryBlue
import com.example.tradingbot.ui.theme.ProfitGreen
import com.example.tradingbot.ui.theme.SurfaceDark
import com.example.tradingbot.ui.theme.SurfaceElevated
import com.example.tradingbot.ui.theme.SurfaceVariantDark
import com.example.tradingbot.ui.theme.TextMuted
import com.example.tradingbot.ui.theme.TextPrimary
import com.example.tradingbot.ui.theme.TextSecondary

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val botConfig by viewModel.botConfig.collectAsState()

    var baseStake by remember(botConfig) { mutableDoubleStateOf(botConfig.baseStake) }
    var martingaleMultiplier by remember(botConfig) { mutableDoubleStateOf(botConfig.martingaleMultiplier) }
    var maxSteps by remember(botConfig) { mutableIntStateOf(botConfig.maxMartingaleSteps) }
    var expirationSeconds by remember(botConfig) { mutableIntStateOf(botConfig.expirationSeconds) }
    var strategy by remember(botConfig) { mutableStateOf(botConfig.strategy) }
    var fastSma by remember(botConfig) { mutableIntStateOf(botConfig.fastSmaPeriod) }
    var slowSma by remember(botConfig) { mutableIntStateOf(botConfig.slowSmaPeriod) }
    var mlThreshold by remember(botConfig) { mutableFloatStateOf(botConfig.mlThreshold) }
    var stopLoss by remember(botConfig) { mutableDoubleStateOf(botConfig.stopLoss) }
    var takeProfit by remember(botConfig) { mutableDoubleStateOf(botConfig.takeProfit) }

    var saveFeedback by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Money Management & Martingale Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MONEY MANAGEMENT & MARTINGALE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Base Stake selector
                    Text("Base Trade Stake: $${String.format("%.2f", baseStake)}", fontSize = 12.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0, 2.0, 5.0, 10.0, 25.0).forEach { s ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (baseStake == s) PrimaryBlue else SurfaceVariantDark)
                                    .clickable { baseStake = s }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "$${s.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (baseStake == s) Color.White else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Martingale Multiplier
                    Text("Martingale Multiplier: ${String.format("%.1f", martingaleMultiplier)}x", fontSize = 12.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.5, 2.0, 2.2, 2.5).forEach { m ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (martingaleMultiplier == m) AccentGold else SurfaceVariantDark)
                                    .clickable { martingaleMultiplier = m }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${m}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (martingaleMultiplier == m) Color.Black else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Max Martingale Steps
                    Text("Max Consecutive Steps: $maxSteps steps", fontSize = 12.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 5, 7, 10).forEach { st ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (maxSteps == st) PrimaryBlue else SurfaceVariantDark)
                                    .clickable { maxSteps = st }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "$st",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (maxSteps == st) Color.White else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Martingale Stack Preview
                    val stack = remember(baseStake, martingaleMultiplier, maxSteps) {
                        val list = mutableListOf<Double>()
                        var cur = baseStake
                        for (i in 1..maxSteps) {
                            list.add(cur)
                            cur *= martingaleMultiplier
                        }
                        list
                    }

                    Text("Progression Stack: " + stack.joinToString(", ") { "$${String.format("%.1f", it)}" }, fontSize = 10.sp, color = AccentGold)
                }
            }
        }

        item {
            // Strategy Selection & Parameter Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ALGORITHMIC TRADING STRATEGY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StrategyType.entries.forEach { st ->
                        val isSelected = st == strategy
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else SurfaceVariantDark)
                                .border(1.dp, if (isSelected) PrimaryBlue else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { strategy = st }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = st.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryBlue else TextPrimary
                                )
                                Text(
                                    text = st.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Expiration Duration
                    Text("Trade Expiration: ${expirationSeconds}s", fontSize = 12.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 60, 120, 300).forEach { exp ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (expirationSeconds == exp) PrimaryBlue else SurfaceVariantDark)
                                    .clickable { expirationSeconds = exp }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${exp}s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (expirationSeconds == exp) Color.White else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ML Threshold slider
                    Text("ML Confidence Threshold: ${(mlThreshold * 100).toInt()}%", fontSize = 12.sp, color = TextPrimary)
                    Slider(
                        value = mlThreshold,
                        onValueChange = { mlThreshold = it },
                        valueRange = 0.50f..0.80f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Config Button
                    Button(
                        onClick = {
                            val updated = botConfig.copy(
                                baseStake = baseStake,
                                martingaleMultiplier = martingaleMultiplier,
                                maxMartingaleSteps = maxSteps,
                                expirationSeconds = expirationSeconds,
                                strategy = strategy,
                                fastSmaPeriod = fastSma,
                                slowSmaPeriod = slowSma,
                                mlThreshold = mlThreshold,
                                stopLoss = stopLoss,
                                takeProfit = takeProfit
                            )
                            viewModel.updateConfig(updated)
                            saveFeedback = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_settings_button")
                    ) {
                        Text(
                            text = if (saveFeedback) "✓ SETTINGS SAVED" else "SAVE BOT CONFIGURATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            // Strategy Guide & Architecture Knowledge Base
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STRATEGY LOGIC (PYTHON PORT)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• SMA Crossover Strategy (po_bot_indicators.py):\n" +
                                "Fast SMA(3) cross over Slow SMA(8). When fast crosses below slow and 3 consecutive candle closes are decreasing, PUT is triggered. When fast crosses above slow and 3 consecutive candle closes are increasing, CALL is triggered.\n\n" +
                                "• Price Action Momentum (po_bot.py):\n" +
                                "Monitors consecutive higher/lower closes. On 3 consecutive drops (C < C-1 < C-2), PUT is executed. On 3 consecutive rises (C > C-1 > C-2), CALL is executed with strict cooldown rules.\n\n" +
                                "• Machine Learning Classifier (po_bot_ml.py):\n" +
                                "Calculates Awesome Oscillator, Parabolic SAR reversal state, CCI(20), and MACD signal crossover. Evaluates probability scores for CALL vs PUT; triggers when probability exceeds threshold (default 60%).\n\n" +
                                "• Money Management (Martingale):\n" +
                                "On win: resets to base stake. On loss: multiplies previous stake by coefficient (default 2.0x) to recover loss on next winning cycle.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
