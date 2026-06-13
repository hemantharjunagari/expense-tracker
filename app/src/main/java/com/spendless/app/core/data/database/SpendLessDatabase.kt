package com.spendless.app.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.JournalMode
import androidx.room.TypeConverters
import com.spendless.app.core.data.database.converters.TypeConverters as AppTypeConverters
import com.spendless.app.core.data.database.dao.*
import com.spendless.app.core.data.database.entities.*

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        BudgetCycle::class,
        CategoryRule::class,
        MonthlySummary::class,
        // Lend & Borrow module (v2)
        LendBorrowRecord::class,
        LendBorrowPayment::class,
        LendBorrowContact::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(AppTypeConverters::class)
abstract class SpendLessDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun categoryDao(): CategoryDao
    abstract fun monthlySummaryDao(): MonthlySummaryDao
    abstract fun lendBorrowDao(): LendBorrowDao

    companion object {
        const val DATABASE_NAME = "spendless.db"

        @Volatile
        private var INSTANCE: SpendLessDatabase? = null

        fun getInstance(context: Context): SpendLessDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SpendLessDatabase::class.java,
                    DATABASE_NAME
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
