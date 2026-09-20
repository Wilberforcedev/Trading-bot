package com.example.tradingbot.indicators

import com.example.tradingbot.model.Candle
import com.example.tradingbot.model.IndicatorValues
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.StrategyType
import kotlin.math.abs

object TechnicalIndicators {

    /**
     * Calculates Simple Moving Average (SMA) for close prices.
     */
    fun calculateSMA(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.size < period) return List(candles.size) { null }
        val result = ArrayList<Double?>(candles.size)
        var runningSum = 0.0

        for (i in candles.indices) {
            runningSum += candles[i].close
            if (i >= period) {
                runningSum -= candles[i - period].close
            }
            if (i >= period - 1) {
                result.add(runningSum / period)
            } else {
                result.add(null)
            }
        }
        return result
    }

    /**
     * Calculates Exponential Moving Average (EMA).
     */
    fun calculateEMA(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.size < period) return List(candles.size) { null }
        val result = ArrayList<Double?>(candles.size)
        val multiplier = 2.0 / (period + 1)
        var ema: Double? = null

        var initialSum = 0.0
        for (i in candles.indices) {
            if (i < period - 1) {
                initialSum += candles[i].close
                result.add(null)
            } else if (i == period - 1) {
                initialSum += candles[i].close
                val initialSma = initialSum / period
                ema = initialSma
                result.add(initialSma)
            } else {
                ema = (candles[i].close - ema!!) * multiplier + ema
                result.add(ema)
            }
        }
        return result
    }

    /**
     * Awesome Oscillator (AO) = SMA(Median, 5) - SMA(Median, 34)
     */
    fun calculateAwesomeOscillator(candles: List<Candle>): List<Double?> {
        if (candles.size < 34) return List(candles.size) { null }
        val medians = candles.map { (it.high + it.low) / 2.0 }
        val result = ArrayList<Double?>(candles.size)

        for (i in candles.indices) {
            if (i >= 33) {
                val sma5 = medians.subList(i - 4, i + 1).average()
                val sma34 = medians.subList(i - 33, i + 1).average()
                result.add(sma5 - sma34)
            } else {
                result.add(null)
            }
        }
        return result
    }

    /**
     * Commodity Channel Index (CCI) with lookback period (default 20).
     */
    fun calculateCCI(candles: List<Candle>, period: Int = 20): List<Double?> {
        if (candles.size < period) return List(candles.size) { null }
        val typicalPrices = candles.map { (it.high + it.low + it.close) / 3.0 }
        val result = ArrayList<Double?>(candles.size)

        for (i in candles.indices) {
            if (i >= period - 1) {
                val tpWindow = typicalPrices.subList(i - period + 1, i + 1)
                val smaTp = tpWindow.average()
                val meanDeviation = tpWindow.map { abs(it - smaTp) }.average()
                if (meanDeviation == 0.0) {
                    result.add(0.0)
                } else {
                    result.add((typicalPrices[i] - smaTp) / (0.015 * meanDeviation))
                }
            } else {
                result.add(null)
            }
        }
        return result
    }

    /**
     * Moving Average Convergence Divergence (MACD).
     * Returns Triple(macdLine, signalLine, histogram).
     */
    fun calculateMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val fastEma = calculateEMA(candles, fastPeriod)
        val slowEma = calculateEMA(candles, slowPeriod)

        val macdLine = ArrayList<Double?>(candles.size)
        for (i in candles.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine.add(f - s)
            } else {
                macdLine.add(null)
            }
        }

        // Signal line is EMA of valid MACD values
        val signalLine = ArrayList<Double?>(candles.size)
        val histogram = ArrayList<Double?>(candles.size)
        val validMacdIndices = macdLine.mapIndexedNotNull { index, v -> if (v != null) Pair(index, v) else null }

        if (validMacdIndices.size >= signalPeriod) {
            val multiplier = 2.0 / (signalPeriod + 1)
            var currentSig: Double? = null
            var initSum = 0.0

            val lookupSignal = HashMap<Int, Double>()

            for (k in validMacdIndices.indices) {
                val (origIdx, macdVal) = validMacdIndices[k]
                if (k < signalPeriod - 1) {
                    initSum += macdVal
                } else if (k == signalPeriod - 1) {
                    initSum += macdVal
                    currentSig = initSum / signalPeriod
                    lookupSignal[origIdx] = currentSig
                } else {
                    currentSig = (macdVal - currentSig!!) * multiplier + currentSig
                    lookupSignal[origIdx] = currentSig
                }
            }

            for (i in candles.indices) {
                val sig = lookupSignal[i]
                val mac = macdLine[i]
                signalLine.add(sig)
                if (mac != null && sig != null) {
                    histogram.add(mac - sig)
                } else {
                    histogram.add(null)
                }
            }
        } else {
            repeat(candles.size) {
                signalLine.add(null)
                histogram.add(null)
            }
        }

        return Triple(macdLine, signalLine, histogram)
    }

    /**
     * Parabolic SAR calculation.
     */
    fun calculateParabolicSAR(candles: List<Candle>): Pair<List<Double?>, List<Boolean>> {
        val n = candles.size
        if (n < 2) return Pair(List(n) { null }, List(n) { false })

        val sarValues = ArrayList<Double?>(n)
        val reversals = ArrayList<Boolean>(n)

        var isRising = candles[1].close >= candles[0].close
        var ep = if (isRising) candles[0].high else candles[0].low
        var sar = if (isRising) candles[0].low else candles[0].high
        var af = 0.02
        val afStep = 0.02
        val afMax = 0.20

        sarValues.add(sar)
        reversals.add(false)

        for (i in 1 until n) {
            var isReversal = false
            val prevSar = sar

            if (isRising) {
                sar = prevSar + af * (ep - prevSar)
                if (i >= 2) sar = minOf(sar, candles[i - 1].low, candles[i - 2].low)
                else sar = minOf(sar, candles[i - 1].low)

                if (candles[i].low < sar) {
                    isRising = false
                    isReversal = true
                    sar = ep
                    ep = candles[i].low
                    af = 0.02
                } else {
                    if (candles[i].high > ep) {
                        ep = candles[i].high
                        af = minOf(af + afStep, afMax)
                    }
                }
            } else {
                sar = prevSar - af * (prevSar - ep)
                if (i >= 2) sar = maxOf(sar, candles[i - 1].high, candles[i - 2].high)
                else sar = maxOf(sar, candles[i - 1].high)

                if (candles[i].high > sar) {
                    isRising = true
                    isReversal = true
                    sar = ep
                    ep = candles[i].high
                    af = 0.02
                } else {
                    if (candles[i].low < ep) {
                        ep = candles[i].low
                        af = minOf(af + afStep, afMax)
                    }
                }
            }

            sarValues.add(sar)
            reversals.add(isReversal)
        }

        return Pair(sarValues, reversals)
    }

    /**
     * Compute current indicator snapshots and machine learning / strategy evaluation.
     */
    fun computeIndicators(
        candles: List<Candle>,
        fastPeriod: Int = 3,
        slowPeriod: Int = 8,
        mlThreshold: Float = 0.60f
    ): IndicatorValues {
        if (candles.size < 10) return IndicatorValues()

        val fastSmaList = calculateSMA(candles, fastPeriod)
        val slowSmaList = calculateSMA(candles, slowPeriod)
        val aoList = calculateAwesomeOscillator(candles)
        val cciList = calculateCCI(candles, 20)
        val (macdLine, macdSig, _) = calculateMACD(candles)
        val (sarList, reversals) = calculateParabolicSAR(candles)

        val lastIdx = candles.size - 1
        val fastSma = fastSmaList[lastIdx]
        val slowSma = slowSmaList[lastIdx]
        val ao = aoList[lastIdx]
        val cci = cciList[lastIdx]
        val macd = macdLine[lastIdx]
        val macdSignal = macdSig[lastIdx]
        val psar = sarList[lastIdx]

        // ML Feature extraction matching po_bot_ml.py:
        // row.append(1 if awesome_oscillator >= 0 else 0)
        // row.append(1 if psar is reversal else 0)
        // row.append(1 if cci <= 0 else 0)
        // row.append(1 if macd >= macdSignal else 0)
        var callScore = 0.0f
        var putScore = 0.0f

        if (ao != null) {
            if (ao >= 0) callScore += 0.25f else putScore += 0.25f
        }
        if (reversals.isNotEmpty()) {
            val rev = reversals[lastIdx]
            val lastClose = candles[lastIdx].close
            val prevClose = candles[lastIdx - 1].close
            if (rev && lastClose > prevClose) callScore += 0.25f
            else if (rev && lastClose < prevClose) putScore += 0.25f
            else {
                if (lastClose > prevClose) callScore += 0.15f else putScore += 0.15f
            }
        }
        if (cci != null) {
            if (cci > 0) callScore += 0.25f else putScore += 0.25f
        }
        if (macd != null && macdSignal != null) {
            if (macd >= macdSignal) callScore += 0.25f else putScore += 0.25f
        }

        val total = callScore + putScore
        val mlCallProb = if (total > 0f) callScore / total else 0.50f
        val mlPutProb = if (total > 0f) putScore / total else 0.50f

        return IndicatorValues(
            fastSma = fastSma,
            slowSma = slowSma,
            awesomeOscillator = ao,
            cci = cci,
            macd = macd,
            macdSignal = macdSignal,
            psar = psar,
            mlCallProbability = mlCallProb,
            mlPutProbability = mlPutProb
        )
    }

    /**
     * Evaluates trade signal according to selected strategy.
     */
    fun evaluateSignal(
        strategy: StrategyType,
        candles: List<Candle>,
        fastPeriod: Int = 3,
        slowPeriod: Int = 8,
        mlThreshold: Float = 0.60f
    ): SignalType? {
        if (candles.size < 10) return null
        val lastIdx = candles.size - 1

        return when (strategy) {
            StrategyType.SMA_CROSSOVER -> {
                // Matching po_bot_indicators.py and test_on_historical_data.py:
                val fastSma = calculateSMA(candles, fastPeriod)
                val slowSma = calculateSMA(candles, slowPeriod)

                val prevFast = fastSma.getOrNull(lastIdx - 1) ?: return null
                val prevSlow = slowSma.getOrNull(lastIdx - 1) ?: return null
                val currFast = fastSma.getOrNull(lastIdx) ?: return null
                val currSlow = slowSma.getOrNull(lastIdx) ?: return null

                val c0 = candles[lastIdx].close
                val c1 = candles[lastIdx - 1].close
                val c2 = candles[lastIdx - 2].close

                // Bearish crossover: fast was above slow, now below slow AND 3 decreasing candles
                if (prevFast > prevSlow && currFast < currSlow && c0 < c1 && c1 < c2) {
                    SignalType.PUT
                }
                // Bullish crossover: fast was below slow, now above slow AND 3 increasing candles
                else if (prevFast < prevSlow && currFast > currSlow && c0 > c1 && c1 > c2) {
                    SignalType.CALL
                } else null
            }

            StrategyType.PRICE_ACTION_TREND -> {
                // Matching po_bot.py price action check:
                // put: stack[-1] < stack[-1 - PERIOD] < stack[-1 - PERIOD * 2]
                // call: stack[-1] > stack[-1 - PERIOD] > stack[-1 - PERIOD * 2]
                val c0 = candles[lastIdx].close
                val c1 = candles[lastIdx - 1].close
                val c2 = candles[lastIdx - 2].close

                val fastSma = calculateSMA(candles, fastPeriod).getOrNull(lastIdx)
                val slowSma = calculateSMA(candles, slowPeriod).getOrNull(lastIdx)

                if (c0 < c1 && c1 < c2) {
                    if (fastSma != null && slowSma != null && fastSma <= slowSma) SignalType.PUT
                    else SignalType.PUT
                } else if (c0 > c1 && c1 > c2) {
                    if (fastSma != null && slowSma != null && fastSma >= slowSma) SignalType.CALL
                    else SignalType.CALL
                } else null
            }

            StrategyType.ML_ENSEMBLE -> {
                // Matching po_bot_ml.py:
                val indicators = computeIndicators(candles, fastPeriod, slowPeriod, mlThreshold)
                if (indicators.mlPutProbability >= mlThreshold) {
                    SignalType.PUT
                } else if (indicators.mlCallProbability >= mlThreshold) {
                    SignalType.CALL
                } else null
            }
        }
    }
}
