package com.example.tradingbot.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tradingbot.model.SignalType
import com.example.tradingbot.model.StrategyType
import com.example.tradingbot.model.Trade
import com.example.tradingbot.model.TradeStatus

class TradingDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "trading_bot.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_TRADES = "trades"
        private const val COL_ID = "id"
        private const val COL_SYMBOL = "symbol"
        private const val COL_TYPE = "type"
        private const val COL_STAKE = "stake"
        private const val COL_ENTRY_PRICE = "entry_price"
        private const val COL_CLOSE_PRICE = "close_price"
        private const val COL_OPEN_TIME = "open_time"
        private const val COL_CLOSE_TIME = "close_time"
        private const val COL_STATUS = "status"
        private const val COL_PROFIT_LOSS = "profit_loss"
        private const val COL_MARTINGALE_STEP = "martingale_step"
        private const val COL_STRATEGY = "strategy"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTrades = """
            CREATE TABLE $TABLE_TRADES (
                $COL_ID TEXT PRIMARY KEY,
                $COL_SYMBOL TEXT NOT NULL,
                $COL_TYPE TEXT NOT NULL,
                $COL_STAKE REAL NOT NULL,
                $COL_ENTRY_PRICE REAL NOT NULL,
                $COL_CLOSE_PRICE REAL,
                $COL_OPEN_TIME INTEGER NOT NULL,
                $COL_CLOSE_TIME INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_PROFIT_LOSS REAL NOT NULL,
                $COL_MARTINGALE_STEP INTEGER NOT NULL,
                $COL_STRATEGY TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTrades)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRADES")
        onCreate(db)
    }

    fun insertTrade(trade: Trade) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, trade.id)
            put(COL_SYMBOL, trade.symbol)
            put(COL_TYPE, trade.type.name)
            put(COL_STAKE, trade.stake)
            put(COL_ENTRY_PRICE, trade.entryPrice)
            put(COL_CLOSE_PRICE, trade.closePrice)
            put(COL_OPEN_TIME, trade.openTime)
            put(COL_CLOSE_TIME, trade.closeTime)
            put(COL_STATUS, trade.status.name)
            put(COL_PROFIT_LOSS, trade.profitLoss)
            put(COL_MARTINGALE_STEP, trade.martingaleStep)
            put(COL_STRATEGY, trade.strategy.name)
        }
        db.insertWithOnConflict(TABLE_TRADES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllTrades(): List<Trade> {
        val list = mutableListOf<Trade>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRADES ORDER BY $COL_CLOSE_TIME DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                val trade = Trade(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    symbol = it.getString(it.getColumnIndexOrThrow(COL_SYMBOL)),
                    type = SignalType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_TYPE))),
                    stake = it.getDouble(it.getColumnIndexOrThrow(COL_STAKE)),
                    entryPrice = it.getDouble(it.getColumnIndexOrThrow(COL_ENTRY_PRICE)),
                    closePrice = if (it.isNull(it.getColumnIndexOrThrow(COL_CLOSE_PRICE))) null else it.getDouble(it.getColumnIndexOrThrow(COL_CLOSE_PRICE)),
                    openTime = it.getLong(it.getColumnIndexOrThrow(COL_OPEN_TIME)),
                    closeTime = it.getLong(it.getColumnIndexOrThrow(COL_CLOSE_TIME)),
                    status = TradeStatus.valueOf(it.getString(it.getColumnIndexOrThrow(COL_STATUS))),
                    profitLoss = it.getDouble(it.getColumnIndexOrThrow(COL_PROFIT_LOSS)),
                    martingaleStep = it.getInt(it.getColumnIndexOrThrow(COL_MARTINGALE_STEP)),
                    strategy = try {
                        StrategyType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_STRATEGY)))
                    } catch (e: Exception) {
                        StrategyType.SMA_CROSSOVER
                    }
                )
                list.add(trade)
            }
        }
        return list
    }

    fun clearAllTrades() {
        val db = writableDatabase
        db.delete(TABLE_TRADES, null, null)
    }
}
