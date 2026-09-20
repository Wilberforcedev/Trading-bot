package com.example.tradingbot.model

data class MarketSymbol(
    val code: String,
    val name: String,
    val isOtc: Boolean,
    val payoutRate: Double = 0.92,
    val basePrice: Double = 1.0500,
    val volatility: Double = 0.00015
)

object MarketSymbols {
    val ALL = listOf(
        MarketSymbol("EUR/USD", "Euro / US Dollar", false, 0.92, 1.0850, 0.00012),
        MarketSymbol("AUD/CAD OTC", "AUD / CAD (OTC)", true, 0.92, 0.8920, 0.00015),
        MarketSymbol("AUD/CHF OTC", "AUD / CHF (OTC)", true, 0.92, 0.5840, 0.00014),
        MarketSymbol("AUD/JPY", "AUD / JPY", false, 0.91, 98.450, 0.025),
        MarketSymbol("AUD/NZD OTC", "AUD / NZD (OTC)", true, 0.92, 1.0760, 0.00013),
        MarketSymbol("CAD/CHF OTC", "CAD / CHF (OTC)", true, 0.92, 0.6540, 0.00014),
        MarketSymbol("EUR/GBP OTC", "EUR / GBP (OTC)", true, 0.92, 0.8560, 0.00011),
        MarketSymbol("EUR/JPY OTC", "EUR / JPY (OTC)", true, 0.92, 161.20, 0.035),
        MarketSymbol("EUR/NZD OTC", "EUR / NZD (OTC)", true, 0.92, 1.7750, 0.00022),
        MarketSymbol("EUR/RUB OTC", "EUR / RUB (OTC)", true, 0.92, 98.500, 0.040),
        MarketSymbol("AAPL OTC", "Apple Inc. (OTC)", true, 0.92, 185.50, 0.045),
        MarketSymbol("TSLA OTC", "Tesla Inc. (OTC)", true, 0.92, 215.30, 0.080),
        MarketSymbol("AMZN OTC", "Amazon.com (OTC)", true, 0.92, 178.20, 0.050),
        MarketSymbol("MSFT OTC", "Microsoft (OTC)", true, 0.92, 420.10, 0.060),
        MarketSymbol("BA OTC", "Boeing Company (OTC)", true, 0.92, 204.80, 0.065),
        MarketSymbol("MCD OTC", "McDonald's (OTC)", true, 0.92, 290.40, 0.035),
        MarketSymbol("NFLX OTC", "Netflix Inc. (OTC)", true, 0.92, 630.00, 0.090)
    )

    fun find(code: String): MarketSymbol {
        return ALL.find { it.code == code } ?: ALL.first()
    }
}
