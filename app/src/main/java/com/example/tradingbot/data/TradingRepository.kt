package com.example.tradingbot.data

import android.content.Context
import android.content.SharedPreferences
import com.example.tradingbot.model.BotConfig
import com.example.tradingbot.model.StrategyType
import com.example.tradingbot.model.Trade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TradingRepository(context: Context) {

    private val dbHelper = TradingDatabaseHelper(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("trading_bot_prefs", Context.MODE_PRIVATE)

    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades: StateFlow<List<Trade>> = _trades.asStateFlow()

    private val _balance = MutableStateFlow(1200.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _botConfig = MutableStateFlow(loadConfig())
    val botConfig: StateFlow<BotConfig> = _botConfig.asStateFlow()

    init {
        loadTrades()
        loadBalance()
    }

    private fun loadTrades() {
        _trades.value = dbHelper.getAllTrades()
    }

    private fun loadBalance() {
        val savedBalance = prefs.getFloat("account_balance", 1200.0f).toDouble()
        _balance.value = savedBalance
    }

    fun updateBalance(newBalance: Double) {
        _balance.value = newBalance
        prefs.edit().putFloat("account_balance", newBalance.toFloat()).apply()
    }

    fun recordTrade(trade: Trade) {
        dbHelper.insertTrade(trade)
        _trades.value = dbHelper.getAllTrades()
        updateBalance(_balance.value + trade.profitLoss)
    }

    fun clearHistory() {
        dbHelper.clearAllTrades()
        _trades.value = emptyList()
    }

    fun resetBalance(amount: Double) {
        updateBalance(amount)
    }

    fun updateConfig(config: BotConfig) {
        _botConfig.value = config
        prefs.edit().apply {
            putFloat("baseStake", config.baseStake.toFloat())
            putFloat("martingaleMultiplier", config.martingaleMultiplier.toFloat())
            putInt("maxMartingaleSteps", config.maxMartingaleSteps)
            putFloat("payoutPercentage", config.payoutPercentage.toFloat())
            putInt("expirationSeconds", config.expirationSeconds)
            putString("selectedSymbol", config.selectedSymbol)
            putString("strategy", config.strategy.name)
            putInt("fastSmaPeriod", config.fastSmaPeriod)
            putInt("slowSmaPeriod", config.slowSmaPeriod)
            putFloat("mlThreshold", config.mlThreshold)
            putFloat("stopLoss", config.stopLoss.toFloat())
            putFloat("takeProfit", config.takeProfit.toFloat())
            apply()
        }
    }

    private fun loadConfig(): BotConfig {
        return BotConfig(
            initialDeposit = prefs.getFloat("initialDeposit", 1200.0f).toDouble(),
            baseStake = prefs.getFloat("baseStake", 1.0f).toDouble(),
            martingaleMultiplier = prefs.getFloat("martingaleMultiplier", 2.0f).toDouble(),
            maxMartingaleSteps = prefs.getInt("maxMartingaleSteps", 7),
            payoutPercentage = prefs.getFloat("payoutPercentage", 0.92f).toDouble(),
            expirationSeconds = prefs.getInt("expirationSeconds", 60),
            cooldownSeconds = prefs.getInt("cooldownSeconds", 55),
            selectedSymbol = prefs.getString("selectedSymbol", "EUR/USD") ?: "EUR/USD",
            strategy = try {
                StrategyType.valueOf(prefs.getString("strategy", StrategyType.SMA_CROSSOVER.name)!!)
            } catch (e: Exception) {
                StrategyType.SMA_CROSSOVER
            },
            fastSmaPeriod = prefs.getInt("fastSmaPeriod", 3),
            slowSmaPeriod = prefs.getInt("slowSmaPeriod", 8),
            mlThreshold = prefs.getFloat("mlThreshold", 0.60f),
            stopLoss = prefs.getFloat("stopLoss", 500.0f).toDouble(),
            takeProfit = prefs.getFloat("takeProfit", 1000.0f).toDouble()
        )
    }
}
