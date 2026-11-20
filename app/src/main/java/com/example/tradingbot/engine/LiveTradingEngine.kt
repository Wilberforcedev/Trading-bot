package com.example.tradingbot.engine

import android.content.Context
import com.example.tradingbot.data.TradingRepository
import com.example.tradingbot.indicators.TechnicalIndicators
import com.example.tradingbot.model.BotConfig
import com.example.tradingbot.model.Candle
import com.example.tradingbot.model.IndicatorValues
import com.example.tradingbot.model.MarketSymbol
import com.example.tradingbot.model.MarketSymbols
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.Trade
import com.example.tradingbot.model.TradeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

class LiveTradingEngine(
    private val context: Context,
    private val repository: TradingRepository,
    private val scope: CoroutineScope
) {
    private var tickerJob: Job? = null
    private var botRunnerJob: Job? = null

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _currentPrice = MutableStateFlow(1.0850)
    val currentPrice: StateFlow<Double> = _currentPrice.asStateFlow()

    private val _activeTrades = MutableStateFlow<List<Trade>>(emptyList())
    val activeTrades: StateFlow<List<Trade>> = _activeTrades.asStateFlow()

    private val _isBotRunning = MutableStateFlow(false)
    val isBotRunning: StateFlow<Boolean> = _isBotRunning.asStateFlow()

    private val _currentStake = MutableStateFlow(1.0)
    val currentStake: StateFlow<Double> = _currentStake.asStateFlow()

    private val _martingaleStep = MutableStateFlow(1)
    val martingaleStep: StateFlow<Int> = _martingaleStep.asStateFlow()

    private val _indicatorValues = MutableStateFlow(IndicatorValues())
    val indicatorValues: StateFlow<IndicatorValues> = _indicatorValues.asStateFlow()

    private val _botLogs = MutableStateFlow<List<String>>(emptyList())
    val botLogs: StateFlow<List<String>> = _botLogs.asStateFlow()

    private val _simulationSpeed = MutableStateFlow(1) // 1x, 2x, 5x
    val simulationSpeed: StateFlow<Int> = _simulationSpeed.asStateFlow()

    private var lastActionTime = 0L
    private var activeSymbol: MarketSymbol = MarketSymbols.ALL.first()
    private var candleTickCounter = 0
    private var currentCandleOpen = 1.0850
    private var currentCandleHigh = 1.0850
    private var currentCandleLow = 1.0850

    init {
        val config = repository.botConfig.value
        _currentStake.value = config.baseStake
        setSymbol(MarketSymbols.find(config.selectedSymbol))
        startTicker()
    }

    fun setSimulationSpeed(speed: Int) {
        _simulationSpeed.value = speed.coerceIn(1, 5)
    }

    fun setSymbol(symbol: MarketSymbol) {
        if (symbol.code == activeSymbol.code) return
        if (_activeTrades.value.isNotEmpty()) {
            addLog("Wait for open trade(s) to settle before switching markets.")
            return
        }
        activeSymbol = symbol
        // Try loading historical candles for this symbol from assets
        val assetName = when {
            symbol.code.startsWith("AUD/CAD") -> "AUDCADOTC_2024_2_2_18.csv"
            symbol.code.startsWith("AUD/CHF") -> "AUDCHFOTC_2024_2_2_18.csv"
            symbol.code.startsWith("EUR/GBP") -> "EURGBPOTC_2024_2_5_12.csv"
            symbol.code.startsWith("EUR/JPY") -> "EURJPYOTC_2024_2_5_12.csv"
            symbol.code.startsWith("EUR/NZD") -> "EURNZDOTC_2024_2_5_12.csv"
            symbol.code.startsWith("CAD/CHF") -> "CADCHFOTC_2024_2_6_19.csv"
            else -> null
        }

        val loaded = if (assetName != null) {
            BacktestEngine.loadCandlesFromAsset(context, assetName).takeLast(60)
        } else emptyList()

        if (loaded.isNotEmpty()) {
            _candles.value = loaded
            _currentPrice.value = loaded.last().close
            currentCandleOpen = loaded.last().close
            currentCandleHigh = loaded.last().close
            currentCandleLow = loaded.last().close
        } else {
            // Generate realistic initial 50 candles
            val list = mutableListOf<Candle>()
            var p = symbol.basePrice
            val now = System.currentTimeMillis()
            for (i in 50 downTo 1) {
                val o = p
                val delta = (Random.nextDouble() - 0.495) * symbol.volatility * 4
                val c = o + delta
                val h = maxOf(o, c) + Random.nextDouble() * symbol.volatility * 2
                val l = minOf(o, c) - Random.nextDouble() * symbol.volatility * 2
                list.add(Candle(now - i * 60000L, o, h, l, c))
                p = c
            }
            _candles.value = list
            _currentPrice.value = p
            currentCandleOpen = p
            currentCandleHigh = p
            currentCandleLow = p
        }

        addLog("Asset switched to ${symbol.code} (${if (symbol.isOtc) "OTC" else "Market"} - ${(symbol.payoutRate * 100).toInt()}% payout)")
        recomputeIndicators()
    }

    fun startBot() {
        if (_isBotRunning.value) return
        _isBotRunning.value = true
        addLog("Trading bot started. Strategy: ${repository.botConfig.value.strategy.displayName}")
        startBotRunner()
    }

    fun stopBot() {
        _isBotRunning.value = false
        botRunnerJob?.cancel()
        botRunnerJob = null
        addLog("Trading bot paused.")
    }

    fun resetMartingale() {
        val config = repository.botConfig.value
        _martingaleStep.value = 1
        _currentStake.value = config.baseStake
        addLog("Martingale reset to Step 1 ($${String.format("%.2f", config.baseStake)})")
    }

    private fun addLog(message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$timeStr] $message"
        val current = _botLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 100) current.removeAt(current.lastIndex)
        _botLogs.value = current
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val delayMs = 1000L / _simulationSpeed.value
                delay(delayMs)

                // Generate new tick
                val symbol = activeSymbol
                val price = _currentPrice.value
                val wave = sin(System.currentTimeMillis() / 8000.0) * symbol.volatility * 0.5
                val noise = (Random.nextDouble() - 0.5) * symbol.volatility
                val newPrice = maxOf(0.0001, price + wave + noise)
                _currentPrice.value = newPrice

                // Update current forming candle
                currentCandleHigh = maxOf(currentCandleHigh, newPrice)
                currentCandleLow = minOf(currentCandleLow, newPrice)
                candleTickCounter++

                // Every 60 ticks (or 1 minute in real-time), finalize candle
                if (candleTickCounter >= 30) {
                    candleTickCounter = 0
                    val finalized = Candle(
                        timestamp = System.currentTimeMillis(),
                        open = currentCandleOpen,
                        high = currentCandleHigh,
                        low = currentCandleLow,
                        close = newPrice
                    )
                    val updated = _candles.value.toMutableList()
                    updated.add(finalized)
                    if (updated.size > 80) updated.removeAt(0)
                    _candles.value = updated

                    currentCandleOpen = newPrice
                    currentCandleHigh = newPrice
                    currentCandleLow = newPrice

                    recomputeIndicators()
                }

                // Check active trades for expiration
                checkActiveTrades(newPrice)
            }
        }
    }

    private fun startBotRunner() {
        botRunnerJob?.cancel()
        botRunnerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isBotRunning.value) {
                delay(2000L / _simulationSpeed.value)

                val config = repository.botConfig.value
                val now = System.currentTimeMillis()

                // Check cooldown and MAX_ACTIONS limit (1 active action)
                if (_activeTrades.value.isEmpty() && (now - lastActionTime) >= (config.cooldownSeconds * 1000L / _simulationSpeed.value)) {
                    val signal = TechnicalIndicators.evaluateSignal(
                        strategy = config.strategy,
                        candles = _candles.value,
                        fastPeriod = config.fastSmaPeriod,
                        slowPeriod = config.slowSmaPeriod,
                        mlThreshold = config.mlThreshold
                    )

                    if (signal != null) {
                        lastActionTime = now
                        executeTrade(signal, isAutomated = true)
                    }
                }
            }
        }
    }

    fun executeManualTrade(signal: SignalType) {
        executeTrade(signal, isAutomated = false)
    }

    private fun executeTrade(signal: SignalType, isAutomated: Boolean) {
        val config = repository.botConfig.value
        val stake = _currentStake.value
        val balance = repository.balance.value

        if (balance < stake) {
            addLog("Insufficient balance ($${String.format("%.2f", balance)}) for stake $${String.format("%.2f", stake)}!")
            if (isAutomated) stopBot()
            return
        }

        val entryPrice = _currentPrice.value
        val now = System.currentTimeMillis()
        val durationMs = (config.expirationSeconds * 1000L) / _simulationSpeed.value
        val closeTime = now + durationMs

        val trade = Trade(
            id = UUID.randomUUID().toString(),
            symbol = activeSymbol.code,
            type = signal,
            stake = stake,
            entryPrice = entryPrice,
            openTime = now,
            closeTime = closeTime,
            status = TradeStatus.ACTIVE,
            martingaleStep = _martingaleStep.value,
            strategy = config.strategy
        )

        _activeTrades.value = _activeTrades.value + trade
        addLog(
            "${if (isAutomated) "BOT" else "MANUAL"} ${signal.name} placed on ${activeSymbol.code} @ ${String.format("%.5f", entryPrice)} | Stake: $${String.format("%.2f", stake)} (Step ${_martingaleStep.value})"
        )
    }

    private fun checkActiveTrades(currentPrice: Double) {
        val now = System.currentTimeMillis()
        val active = _activeTrades.value
        if (active.isEmpty()) return

        val completed = mutableListOf<Trade>()
        val remaining = mutableListOf<Trade>()

        for (trade in active) {
            if (now >= trade.closeTime) {
                val isWin = when (trade.type) {
                    SignalType.CALL -> currentPrice > trade.entryPrice
                    SignalType.PUT -> currentPrice < trade.entryPrice
                }
                val isDraw = currentPrice == trade.entryPrice

                val status: TradeStatus
                val pnl: Double

                if (isWin) {
                    status = TradeStatus.WON
                    pnl = trade.stake * MarketSymbols.find(trade.symbol).payoutRate
                } else if (isDraw) {
                    status = TradeStatus.DRAW
                    pnl = 0.0
                } else {
                    status = TradeStatus.LOST
                    pnl = -trade.stake
                }

                val finalizedTrade = trade.copy(
                    closePrice = currentPrice,
                    status = status,
                    profitLoss = pnl
                )
                completed.add(finalizedTrade)

                // Apply Martingale money management matching po_bot.py:
                val config = repository.botConfig.value
                if (status == TradeStatus.WON) {
                    addLog("Trade WON: ${trade.type.name} +$${String.format("%.2f", pnl)} | Martingale RESET to Step 1")
                    _martingaleStep.value = 1
                    _currentStake.value = config.baseStake
                } else if (status == TradeStatus.LOST) {
                    val nextStep = _martingaleStep.value + 1
                    val nextStake = _currentStake.value * config.martingaleMultiplier
                    if (nextStep <= config.maxMartingaleSteps) {
                        addLog("Trade LOST: ${trade.type.name} -$${String.format("%.2f", trade.stake)} | Martingale STEP $nextStep -> Next Stake: $${String.format("%.2f", nextStake)}")
                        _martingaleStep.value = nextStep
                        _currentStake.value = nextStake
                    } else {
                        addLog("Trade LOST: Max Martingale steps reached! Resetting to Step 1 ($${String.format("%.2f", config.baseStake)})")
                        _martingaleStep.value = 1
                        _currentStake.value = config.baseStake
                    }
                } else {
                    addLog("Trade DRAW: ${trade.type.name} (Stake returned)")
                }

                repository.recordTrade(finalizedTrade)
            } else {
                remaining.add(trade)
            }
        }

        _activeTrades.value = remaining
    }

    private fun recomputeIndicators() {
        val config = repository.botConfig.value
        _indicatorValues.value = TechnicalIndicators.computeIndicators(
            candles = _candles.value,
            fastPeriod = config.fastSmaPeriod,
            slowPeriod = config.slowSmaPeriod,
            mlThreshold = config.mlThreshold
        )
    }
}
