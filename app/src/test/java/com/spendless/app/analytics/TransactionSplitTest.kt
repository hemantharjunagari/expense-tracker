package com.spendless.app.analytics

import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.dao.getTotalIncomeInCycleSyncFlow
import com.spendless.app.core.data.database.dao.getTotalSpentInCycleSyncFlow
import com.spendless.app.core.data.database.entities.*
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionSplitTest {

    private val transactionDao = mockk<TransactionDao>(relaxed = true)

    @Test
    fun `getTotalSpentInCycleSyncFlow sums split amounts for debit transactions`() {
        val splits = listOf(
            TransactionSplit(categoryName = Category.FOOD_DINING.name, amount = 300.0),
            TransactionSplit(categoryName = Category.GROCERIES.name, amount = 200.0),
            TransactionSplit(categoryName = Category.INVESTMENTS.name, amount = 500.0) // investments budget tracking is false
        )
        val transaction = Transaction(
            id = 1,
            amount = 1000.0,
            type = TransactionType.DEBIT,
            merchant = "Supermarket",
            merchantNormalized = "SUPERMARKET",
            category = Category.MISCELLANEOUS,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.APPROVED,
            splits = splits
        )

        // Only FOOD_DINING and GROCERIES split amounts should be counted (300 + 200 = 500)
        // INVESTMENTS is budget tracking disabled
        val spent = transactionDao.getTotalSpentInCycleSyncFlow(listOf(transaction))
        assertEquals(500.0, spent, 0.01)
    }

    @Test
    fun `getTotalSpentInCycleSyncFlow uses full amount when no splits present`() {
        val transaction1 = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.DEBIT,
            merchant = "Cafe",
            merchantNormalized = "CAFE",
            category = Category.FOOD_DINING,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.APPROVED
        )
        val transaction2 = Transaction(
            id = 2,
            amount = 200.0,
            type = TransactionType.DEBIT,
            merchant = "Broker",
            merchantNormalized = "BROKER",
            category = Category.INVESTMENTS, // budget tracking false
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.APPROVED
        )

        val spent = transactionDao.getTotalSpentInCycleSyncFlow(listOf(transaction1, transaction2))
        assertEquals(100.0, spent, 0.01)
    }

    @Test
    fun `getTotalSpentInCycleSyncFlow ignores non-approved or budget-excluded transactions`() {
        val transaction1 = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.DEBIT,
            merchant = "Cafe",
            merchantNormalized = "CAFE",
            category = Category.FOOD_DINING,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.PENDING_REVIEW
        )
        val transaction2 = Transaction(
            id = 2,
            amount = 200.0,
            type = TransactionType.DEBIT,
            merchant = "Rent",
            merchantNormalized = "RENT",
            category = Category.RENT,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.APPROVED,
            isExcludedFromBudget = true
        )

        val spent = transactionDao.getTotalSpentInCycleSyncFlow(listOf(transaction1, transaction2))
        assertEquals(0.0, spent, 0.01)
    }

    @Test
    fun `getTotalIncomeInCycleSyncFlow sums split amounts for credit transactions`() {
        val splits = listOf(
            TransactionSplit(categoryName = Category.INCOME.name, amount = 800.0),
            TransactionSplit(categoryName = Category.MISCELLANEOUS.name, amount = 200.0)
        )
        val transaction = Transaction(
            id = 1,
            amount = 1000.0,
            type = TransactionType.CREDIT,
            merchant = "Salary",
            merchantNormalized = "SALARY",
            category = Category.INCOME,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.APPROVED,
            splits = splits
        )

        val income = transactionDao.getTotalIncomeInCycleSyncFlow(listOf(transaction))
        assertEquals(1000.0, income, 0.01)
    }

    @Test
    fun `getTotalIncomeInCycleSyncFlow uses full amount when no splits present`() {
        val transaction = Transaction(
            id = 1,
            amount = 1200.0,
            type = TransactionType.CREDIT,
            merchant = "Gift",
            merchantNormalized = "GIFT",
            category = Category.INCOME,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.APPROVED
        )

        val income = transactionDao.getTotalIncomeInCycleSyncFlow(listOf(transaction))
        assertEquals(1200.0, income, 0.01)
    }
}
