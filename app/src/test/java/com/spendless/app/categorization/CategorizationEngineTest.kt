package com.spendless.app.categorization

import com.spendless.app.core.categorization.CategorizationEngine
import com.spendless.app.core.categorization.MerchantDatabase
import com.spendless.app.core.data.database.dao.CategoryRuleDao
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the categorization engine.
 */
class CategorizationEngineTest {

    private lateinit var categorizationEngine: CategorizationEngine
    private lateinit var categoryRuleDao: CategoryRuleDao

    @Before
    fun setup() {
        categoryRuleDao = mockk(relaxed = true)
        coEvery { categoryRuleDao.getAllRulesSorted() } returns emptyList()
        categorizationEngine = CategorizationEngine(categoryRuleDao)
    }

    // ── Merchant exact matching ────────────────────────────────────────────────

    @Test
    fun `categorize swiggy as food dining`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "swiggy",
            rawSmsBody = "Rs.350 debited at Swiggy",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.FOOD_DINING, category)
    }

    @Test
    fun `categorize amazon as shopping`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "amazon",
            rawSmsBody = "Rs.1200 paid to Amazon",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.SHOPPING, category)
    }

    @Test
    fun `categorize netflix as subscriptions`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "netflix",
            rawSmsBody = "Rs.649 charged by Netflix",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.SUBSCRIPTIONS, category)
    }

    @Test
    fun `categorize blinkit as groceries`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "blinkit",
            rawSmsBody = "Rs.450 paid to Blinkit",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.GROCERIES, category)
    }

    @Test
    fun `categorize irctc as transportation`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "irctc",
            rawSmsBody = "Rs.1200 paid to IRCTC",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.TRANSPORTATION, category)
    }

    // ── Keyword fallback ───────────────────────────────────────────────────────

    @Test
    fun `categorize by food keyword in SMS`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "local dhaba",
            rawSmsBody = "Rs.250 paid at local restaurant",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.FOOD_DINING, category)
    }

    @Test
    fun `categorize salary credit as income`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "salary",
            rawSmsBody = "Rs.50000 credited as salary",
            transactionType = TransactionType.CREDIT
        )
        assertEquals(Category.INCOME, category)
    }

    // ── Default fallback ───────────────────────────────────────────────────────

    @Test
    fun `unknown merchant defaults to uncategorized for debit`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "xyzrandommerchant123",
            rawSmsBody = "Rs.100 debited",
            transactionType = TransactionType.DEBIT
        )
        assertEquals(Category.UNCATEGORIZED, category)
    }

    @Test
    fun `unknown credit defaults to income`() = runTest {
        val category = categorizationEngine.categorize(
            merchantNormalized = "unknown sender",
            rawSmsBody = "Rs.500 credited to your account",
            transactionType = TransactionType.CREDIT
        )
        assertEquals(Category.INCOME, category)
    }

    // ── Merchant database completeness ─────────────────────────────────────────

    @Test
    fun `merchant database has swiggy`() {
        assertTrue(MerchantDatabase.EXACT_MERCHANT_MAP.containsKey("swiggy"))
    }

    @Test
    fun `merchant database has all food delivery apps`() {
        val foodApps = listOf("swiggy", "zomato", "dominos", "pizza hut", "kfc")
        foodApps.forEach { merchant ->
            assertEquals(
                "Expected $merchant to be FOOD_DINING",
                Category.FOOD_DINING,
                MerchantDatabase.EXACT_MERCHANT_MAP[merchant]
            )
        }
    }

    @Test
    fun `merchant database has major OTT platforms`() {
        val ottPlatforms = listOf("netflix", "hotstar", "sonyliv", "zee5", "spotify")
        ottPlatforms.forEach { platform ->
            assertEquals(
                "Expected $platform to be SUBSCRIPTIONS",
                Category.SUBSCRIPTIONS,
                MerchantDatabase.EXACT_MERCHANT_MAP[platform]
            )
        }
    }

}
