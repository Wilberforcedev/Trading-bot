package com.example.tradingbot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tradingbot.indicators.TechnicalIndicators
import com.example.tradingbot.model.Candle
import com.example.tradingbot.ui.theme.AccentCyan
import com.example.tradingbot.ui.theme.AccentGold
import com.example.tradingbot.ui.theme.LossRed
import com.example.tradingbot.ui.theme.ProfitGreen
import com.example.tradingbot.ui.theme.SurfaceElevated
import com.example.tradingbot.ui.theme.TextMuted
import kotlin.math.max
import kotlin.math.min

@Composable
fun TradingChart(
    candles: List<Candle>,
    currentPrice: Double,
    fastSmaPeriod: Int = 3,
    slowSmaPeriod: Int = 8,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text("Waiting for market data...", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val displayCandles = remember(candles) { candles.takeLast(40) }
    val fastSmaList = remember(displayCandles, fastSmaPeriod) {
        TechnicalIndicators.calculateSMA(displayCandles, fastSmaPeriod)
    }
    val slowSmaList = remember(displayCandles, slowSmaPeriod) {
        TechnicalIndicators.calculateSMA(displayCandles, slowSmaPeriod)
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1424))
            .padding(vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val rightPadding = 56.dp.toPx()
            val chartWidth = width - rightPadding
            val numCandles = displayCandles.size
            val candleSlotWidth = chartWidth / numCandles.coerceAtLeast(1)
            val candleBodyWidth = (candleSlotWidth * 0.65f).coerceIn(2f, 16f)

            var minPrice = displayCandles.minOf { it.low }
            var maxPrice = displayCandles.maxOf { it.high }

            // Include SMA and current price in scale calculation
            fastSmaList.filterNotNull().forEach {
                minPrice = min(minPrice, it)
                maxPrice = max(maxPrice, it)
            }
            slowSmaList.filterNotNull().forEach {
                minPrice = min(minPrice, it)
                maxPrice = max(maxPrice, it)
            }
            minPrice = min(minPrice, currentPrice)
            maxPrice = max(maxPrice, currentPrice)

            val priceSpan = (maxPrice - minPrice).let { if (it == 0.0) 0.0001 else it }
            val paddingRatio = 0.08
            val paddedMin = minPrice - priceSpan * paddingRatio
            val paddedMax = maxPrice + priceSpan * paddingRatio
            val paddedSpan = paddedMax - paddedMin

            fun priceToY(price: Double): Float {
                return (height - ((price - paddedMin) / paddedSpan * height)).toFloat()
            }

            // Draw horizontal grid lines & price labels
            val gridSteps = 4
            for (step in 0..gridSteps) {
                val stepPrice = paddedMin + (paddedSpan / gridSteps) * step
                val y = priceToY(stepPrice)
                drawLine(
                    color = Color(0x1FFFFFFF),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                val label = String.format("%.4f", stepPrice)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(chartWidth + 4.dp.toPx(), y - 7.dp.toPx()),
                    style = TextStyle(color = Color(0xFF78909C), fontSize = 9.sp)
                )
            }

            // Draw Candlesticks
            displayCandles.forEachIndexed { index, candle ->
                val centerX = index * candleSlotWidth + candleSlotWidth / 2f
                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                val isBullish = candle.close >= candle.open
                val candleColor = if (isBullish) ProfitGreen else LossRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.5f
                )

                // Body
                val bodyTop = min(openY, closeY)
                val bodyHeight = max(abs(closeY - openY), 2f)
                drawRect(
                    color = candleColor,
                    topLeft = Offset(centerX - candleBodyWidth / 2f, bodyTop),
                    size = Size(candleBodyWidth, bodyHeight)
                )
            }

            // Draw Fast SMA line (Golden)
            val fastSmaPath = Path()
            var fastStarted = false
            fastSmaList.forEachIndexed { index, sma ->
                if (sma != null) {
                    val x = index * candleSlotWidth + candleSlotWidth / 2f
                    val y = priceToY(sma)
                    if (!fastStarted) {
                        fastSmaPath.moveTo(x, y)
                        fastStarted = true
                    } else {
                        fastSmaPath.lineTo(x, y)
                    }
                }
            }
            if (fastStarted) {
                drawPath(
                    path = fastSmaPath,
                    color = AccentGold,
                    style = Stroke(width = 2f)
                )
            }

            // Draw Slow SMA line (Cyan)
            val slowSmaPath = Path()
            var slowStarted = false
            slowSmaList.forEachIndexed { index, sma ->
                if (sma != null) {
                    val x = index * candleSlotWidth + candleSlotWidth / 2f
                    val y = priceToY(sma)
                    if (!slowStarted) {
                        slowSmaPath.moveTo(x, y)
                        slowStarted = true
                    } else {
                        slowSmaPath.lineTo(x, y)
                    }
                }
            }
            if (slowStarted) {
                drawPath(
                    path = slowSmaPath,
                    color = AccentCyan,
                    style = Stroke(width = 2f)
                )
            }

            // Draw Current Price dashed line and right indicator badge
            val currentY = priceToY(currentPrice)
            drawLine(
                color = Color(0xFF2979FF),
                start = Offset(0f, currentY),
                end = Offset(chartWidth, currentY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )

            // Current price badge
            val badgeHeight = 16.dp.toPx()
            val badgeWidth = rightPadding - 4.dp.toPx()
            drawRect(
                color = Color(0xFF2979FF),
                topLeft = Offset(chartWidth + 2.dp.toPx(), currentY - badgeHeight / 2f),
                size = Size(badgeWidth, badgeHeight)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = String.format("%.4f", currentPrice),
                topLeft = Offset(chartWidth + 4.dp.toPx(), currentY - 6.dp.toPx()),
                style = TextStyle(color = Color.White, fontSize = 9.sp)
            )
        }
    }
}
private fun abs(value: Float): Float = if (value < 0) -value else value
