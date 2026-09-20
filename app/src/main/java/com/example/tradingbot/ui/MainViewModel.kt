package com.example.tradingbot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tradingbot.data.TradingRepository
import com.example.tradingbot.engine.BacktestEngine
import com.example.tradingbot.engine.LiveTradingEngine
import com.example.tradingbot.model.BacktestSummary
import com.example.tradingbot.model.BotConfig
import com.example.tradingbot.model.MarketSymbol
import com.example.tradingbot.model.MarketSymbols
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.StrategyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TradingRepository(application)
    private val liveEngine = LiveTradingEngine(application, repository, viewModelScope)

    // State flows from engine and repository
    val candles = liveEngine.candles
    val currentPrice = liveEngine.currentPrice
    val activeTrades = liveEngine.activeTrades
    val isBotRunning = liveEngine.isBotRunning
    val currentStake = liveEngine.currentStake
    val martingaleStep = liveEngine.martingaleStep
    val indicatorValues = liveEngine.indicatorValues
    val botLogs = liveEngine.botLogs
    val simulationSpeed = liveEngine.simulationSpeed

    val balance = repository.balance
    val trades = repository.trades
    val botConfig = repository.botConfig

    // Backtest states
    private val _availableDatasets = MutableStateFlow<List<String>>(emptyList())
    val availableDatasets: StateFlow<List<String>> = _availableDatasets.asStateFlow()

    private val _selectedDataset = MutableStateFlow("")
    val selectedDataset: StateFlow<String> = _selectedDataset.asStateFlow()

    private val _backtestResult = MutableStateFlow<BacktestSummary?>(null)
    val backtestResult: StateFlow<BacktestSummary?> = _backtestResult.asStateFlow()

    private val _isBacktesting = MutableStateFlow(false)
    val isBacktesting: StateFlow<Boolean> = _isBacktesting.asStateFlow()

    private val _batchBacktestResults = MutableStateFlow<List<BacktestSummary>>(emptyList())
    val batchBacktestResults: StateFlow<List<BacktestSummary>> = _batchBacktestResults.asStateFlow()

    init {
        loadAvailableDatasets()
    }

    private fun loadAvailableDatasets() {
        viewModelScope.launch(Dispatchers.IO) {
            val datasets = BacktestEngine.getAvailableDatasets(getApplication())
            _availableDatasets.value = datasets
            if (datasets.isNotEmpty()) {
                _selectedDataset.value = datasets.first()
            }
        }
    }

    fun setSelectedDataset(fileName: String) {
        _selectedDataset.value = fileName
    }

    fun startBot() = liveEngine.startBot()
    fun stopBot() = liveEngine.stopBot()
    fun setSimulationSpeed(speed: Int) = liveEngine.setSimulationSpeed(speed)
    fun setSymbol(symbol: MarketSymbol) = liveEngine.setSymbol(symbol)
    fun executeManualTrade(signal: SignalType) = liveEngine.executeManualTrade(signal)
    fun resetMartingale() = liveEngine.resetMartingale()

    fun updateConfig(config: BotConfig) {
        repository.updateConfig(config)
    }

    fun clearTradeHistory() {
        repository.clearHistory()
    }

    fun resetBalance(amount: Double) {
        repository.resetBalance(amount)
        liveEngine.resetMartingale()
    }

    fun runBacktest(
        fileName: String,
        strategy: StrategyType,
        fastPeriod: Int,
        slowPeriod: Int,
        expiration: Int,
        useMartingale: Boolean
    ) {
        _isBacktesting.value = true
        _backtestResult.value = null
        viewModelScope.launch(Dispatchers.Default) {
            val candles = BacktestEngine.loadCandlesFromAsset(getApplication(), fileName)
            val config = repository.botConfig.value
            val result = BacktestEngine.runBacktest(
                fileName = fileName,
                candles = candles,
                strategy = strategy,
                fastPeriod = fastPeriod,
                slowPeriod = slowPeriod,
                expirationCandles = expiration,
                payoutRate = config.payoutPercentage,
                baseStake = config.baseStake,
                useMartingale = useMartingale,
                martingaleMultiplier = config.martingaleMultiplier,
                initialDeposit = config.initialDeposit
            )
            _backtestResult.value = result
            _isBacktesting.value = false
        }
    }

    fun runBatchBacktest(
        strategy: StrategyType,
        fastPeriod: Int,
        slowPeriod: Int,
        expiration: Int,
        useMartingale: Boolean
    ) {
        _isBacktesting.value = true
        _batchBacktestResults.value = emptyList()
        viewModelScope.launch(Dispatchers.Default) {
            val datasets = _availableDatasets.value
            val results = mutableListOf<BacktestSummary>()
            val config = repository.botConfig.value

            for (fileName in datasets) {
                val candles = BacktestEngine.loadCandlesFromAsset(getApplication(), fileName)
                if (candles.size >= 40) {
                    val res = BacktestEngine.runBacktest(
                        fileName = fileName,
                        candles = candles,
                        strategy = strategy,
                        fastPeriod = fastPeriod,
                        slowPeriod = slowPeriod,
                        expirationCandles = expiration,
                        payoutRate = config.payoutPercentage,
                        baseStake = config.baseStake,
                        useMartingale = useMartingale,
                        martingaleMultiplier = config.martingaleMultiplier,
                        initialDeposit = config.initialDeposit
                    )
                    results.add(res)
                }
            }
            _batchBacktestResults.value = results
            _isBacktesting.value = false
        }
    }
}
