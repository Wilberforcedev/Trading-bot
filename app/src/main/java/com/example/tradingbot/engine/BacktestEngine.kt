package com.example.tradingbot.engine

import android.content.Context
import com.example.tradingbot.indicators.TechnicalIndicators
import com.example.tradingbot.model.BacktestSummary
import com.example.tradingbot.model.Candle
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.StrategyType
import java.io.BufferedReader
import java.io.InputStreamReader

object BacktestEngine {

    /**
     * Lists all CSV files in assets/data_1m
     */
    fun getAvailableDatasets(context: Context): List<String> {
        return try {
            val files = context.assets.list("data_1m") ?: emptyArray()
            files.filter { it.endsWith(".csv") }.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses candles from a given asset CSV file.
     */
    fun loadCandlesFromAsset(context: Context, fileName: String): List<Candle> {
        val candles = mutableListOf<Candle>()
        try {
            val inputStream = context.assets.open("data_1m/$fileName")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var headerLine = reader.readLine() ?: return emptyList()

            val headers = headerLine.split(",").map { it.trim().lowercase() }
            var tsIdx = headers.indexOf("timestamp")
            var openIdx = headers.indexOf("open")
            var highIdx = headers.indexOf("high")
            var lowIdx = headers.indexOf("low")
            var closeIdx = headers.indexOf("close")

            // Default index fallback if header isn't standard
            if (tsIdx == -1) tsIdx = 1
            if (openIdx == -1) openIdx = 2
            if (closeIdx == -1) closeIdx = 3
            if (highIdx == -1) highIdx = 4
            if (lowIdx == -1) lowIdx = 5

            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    val parts = line.split(",").map { it.trim() }
                    try {
                        val tsStr = parts.getOrNull(tsIdx) ?: ""
                        val ts = (tsStr.toDoubleOrNull() ?: 0.0).toLong()

                        val open = parts.getOrNull(openIdx)?.toDoubleOrNull() ?: 0.0
                        val high = parts.getOrNull(highIdx)?.toDoubleOrNull() ?: open
                        val low = parts.getOrNull(lowIdx)?.toDoubleOrNull() ?: open
                        val close = parts.getOrNull(closeIdx)?.toDoubleOrNull() ?: open

                        if (ts > 0 && open > 0.0) {
                            candles.add(
                                Candle(
                                    timestamp = ts,
                                    open = open,
                                    high = maxOf(high, open, close),
                                    low = minOf(low, open, close),
                                    close = close
                                )
                            )
                        }
                    } catch (_: Exception) {
                    }
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return candles.sortedBy { it.timestamp }
    }

    /**
     * Runs backtest against loaded candles with chosen strategy and parameters.
     */
    fun runBacktest(
        fileName: String,
        candles: List<Candle>,
        strategy: StrategyType = StrategyType.SMA_CROSSOVER,
        fastPeriod: Int = 3,
        slowPeriod: Int = 8,
        expirationCandles: Int = 1,
        payoutRate: Double = 0.92,
        baseStake: Double = 10.0,
        useMartingale: Boolean = true,
        martingaleMultiplier: Double = 2.0,
        initialDeposit: Double = 1000.0
    ): BacktestSummary {
        if (candles.size < 40) {
            return BacktestSummary(
                fileName = fileName,
                symbol = extractSymbol(fileName),
                totalOrders = 0,
                wins = 0,
                draws = 0,
                loses = 0,
                winRate = 0.0,
                netProfit = 0.0,
                simulatedBalance = initialDeposit
            )
        }

        var orders = 0
        var wins = 0
        var draws = 0
        var loses = 0

        var currentStake = baseStake
        var balance = initialDeposit

        val fastSmaList = TechnicalIndicators.calculateSMA(candles, fastPeriod)
        val slowSmaList = TechnicalIndicators.calculateSMA(candles, slowPeriod)

        // Iterate through candles up to len - expirationCandles
        for (i in 35 until (candles.size - expirationCandles)) {
            val subCandles = candles.subList(0, i + 1)
            var signal: SignalType? = null

            when (strategy) {
                StrategyType.SMA_CROSSOVER -> {
                    val prevFast = fastSmaList[i - 1]
                    val prevSlow = slowSmaList[i - 1]
                    val currFast = fastSmaList[i]
                    val currSlow = slowSmaList[i]

                    if (prevFast != null && prevSlow != null && currFast != null && currSlow != null) {
                        val c0 = candles[i].close
                        val c1 = candles[i - 1].close
                        val c2 = candles[i - 2].close

                        // Bearish crossover: prevFast > prevSlow && currFast < currSlow AND 3 decreasing candles
                        if (prevFast > prevSlow && currFast < currSlow && c0 < c1 && c1 < c2) {
                            signal = SignalType.PUT
                        }
                        // Bullish crossover: prevFast < prevSlow && currFast > currSlow AND 3 increasing candles
                        else if (prevFast < prevSlow && currFast > currSlow && c0 > c1 && c1 > c2) {
                            signal = SignalType.CALL
                        }
                    }
                }

                StrategyType.PRICE_ACTION_TREND -> {
                    signal = TechnicalIndicators.evaluateSignal(
                        strategy = StrategyType.PRICE_ACTION_TREND,
                        candles = subCandles,
                        fastPeriod = fastPeriod,
                        slowPeriod = slowPeriod
                    )
                }

                StrategyType.ML_ENSEMBLE -> {
                    signal = TechnicalIndicators.evaluateSignal(
                        strategy = StrategyType.ML_ENSEMBLE,
                        candles = subCandles,
                        fastPeriod = fastPeriod,
                        slowPeriod = slowPeriod,
                        mlThreshold = 0.60f
                    )
                }
            }

            if (signal != null) {
                orders++
                val entryPrice = candles[i].close
                val expiryPrice = candles[i + expirationCandles].close

                val isWin = when (signal) {
                    SignalType.CALL -> expiryPrice > entryPrice
                    SignalType.PUT -> expiryPrice < entryPrice
                }
                val isDraw = expiryPrice == entryPrice

                if (isWin) {
                    wins++
                    val profit = currentStake * payoutRate
                    balance += profit
                    if (useMartingale) {
                        currentStake = baseStake // Reset to base stake on win
                    }
                } else if (isDraw) {
                    draws++
                    // Stake returned, no profit/loss
                } else {
                    loses++
                    balance -= currentStake
                    if (useMartingale) {
                        currentStake *= martingaleMultiplier // Step up on loss
                        if (currentStake > balance * 0.5) {
                            currentStake = baseStake // Reset if stake exceeds 50% bankroll
                        }
                    }
                }
            }
        }

        val winRate = if (orders > 0) (wins.toDouble() / orders.toDouble()) * 100.0 else 0.0
        val netProfit = balance - initialDeposit

        return BacktestSummary(
            fileName = fileName,
            symbol = extractSymbol(fileName),
            totalOrders = orders,
            wins = wins,
            draws = draws,
            loses = loses,
            winRate = winRate,
            netProfit = netProfit,
            simulatedBalance = balance
        )
    }

    private fun extractSymbol(fileName: String): String {
        val base = fileName.substringBefore("_")
        return when {
            base.endsWith("OTC") -> base.removeSuffix("OTC") + " (OTC)"
            else -> base
        }
    }
}
