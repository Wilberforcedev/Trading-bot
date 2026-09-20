package com.example.tradingbot.model

enum class SignalType {
    CALL,
    PUT
}

enum class TradeStatus {
    ACTIVE,
    WON,
    LOST,
    DRAW
}

enum class StrategyType(val displayName: String, val description: String) {
    SMA_CROSSOVER(
        "SMA Crossover (3/8)",
        "Fast SMA(3) cross over Slow SMA(8) combined with 3-candle price momentum confirmation."
    ),
    PRICE_ACTION_TREND(
        "Price Action Trend (8/50)",
        "Consecutive 3-candle momentum aligned with Long SMA(50) and Short SMA(8) trend direction."
    ),
    ML_ENSEMBLE(
        "ML Multi-Indicator Ensemble",
        "Probabilistic classifier combining Awesome Oscillator, Parabolic SAR, CCI, and MACD signals."
    )
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)

data class Trade(
    val id: String,
    val symbol: String,
    val type: SignalType,
    val stake: Double,
    val entryPrice: Double,
    val closePrice: Double? = null,
    val openTime: Long,
    val closeTime: Long,
    val status: TradeStatus,
    val profitLoss: Double = 0.0,
    val martingaleStep: Int = 1,
    val strategy: StrategyType = StrategyType.SMA_CROSSOVER
)

data class BotConfig(
    val initialDeposit: Double = 1200.0,
    val baseStake: Double = 1.0,
    val martingaleMultiplier: Double = 2.0,
    val maxMartingaleSteps: Int = 7,
    val payoutPercentage: Double = 0.92, // 92% payout standard
    val expirationSeconds: Int = 60,
    val cooldownSeconds: Int = 55,
    val selectedSymbol: String = "EUR/USD",
    val strategy: StrategyType = StrategyType.SMA_CROSSOVER,
    val fastSmaPeriod: Int = 3,
    val slowSmaPeriod: Int = 8,
    val mlThreshold: Float = 0.60f,
    val stopLoss: Double = 500.0,
    val takeProfit: Double = 1000.0
)

data class IndicatorValues(
    val fastSma: Double? = null,
    val slowSma: Double? = null,
    val awesomeOscillator: Double? = null,
    val cci: Double? = null,
    val macd: Double? = null,
    val macdSignal: Double? = null,
    val psar: Double? = null,
    val mlCallProbability: Float = 0.50f,
    val mlPutProbability: Float = 0.50f,
    val activeSignal: SignalType? = null
)

data class BacktestSummary(
    val fileName: String,
    val symbol: String,
    val totalOrders: Int,
    val wins: Int,
    val draws: Int,
    val loses: Int,
    val winRate: Double,
    val netProfit: Double,
    val simulatedBalance: Double
)
