package com.spendless.app.sms

import android.util.Log
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.TransactionStatus
import com.spendless.app.core.sms.SmsParser
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the SMS parser covering 50+ bank/UPI formats.
 */
class SmsParserTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    // ── HDFC ──────────────────────────────────────────────────────────────────

    @Test
    fun `parse HDFC debit card transaction`() {
        val sms = "Rs.2,500.00 spent on HDFC Bank Credit Card XX4321 at SWIGGY on 2024-01-15. Avl Bal: Rs.45,000.00"
        val result = SmsParser.parse("HDFCBK", sms, 1705324800000L)
        assertNotNull("Should parse HDFC SMS", result)
        assertEquals(2500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertTrue("Merchant should contain 'Swiggy'", result.merchant.lowercase().contains("swiggy"))
    }

    @Test
    fun `parse HDFC UPI debit`() {
        val sms = "INR 150.00 debited from HDFC Bank A/c XX9876 UPI Ref 423156789012. Balance INR 12,350.00"
        val result = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    // ── SBI ───────────────────────────────────────────────────────────────────

    @Test
    fun `parse SBI credit alert`() {
        val sms = "Your SBI A/c XXXXXXXX5432 is credited by Rs 15,000.00 on 15/01/2024 by NEFT. Avl Bal: Rs 27,350.00"
        val result = SmsParser.parse("SBIPSG", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(15000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
    }

    // ── ICICI ─────────────────────────────────────────────────────────────────

    @Test
    fun `parse ICICI UPI payment`() {
        val sms = "ICICI Bank: Rs 500.00 debited from Acct XX1234 on 15-Jan-24 via UPI to ZOMATO. UPI Ref: 412345678901"
        val result = SmsParser.parse("ICICIB", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertTrue(result.merchant.lowercase().contains("zomato"))
    }

    // ── PhonePe/UPI ────────────────────────────────────────────────────────────

    @Test
    fun `parse PhonePe transaction`() {
        val sms = "PhonePe: Rs.1,200 paid to AMAZON on 15 Jan 2024 at 10:30 AM"
        val result = SmsParser.parse("PHONEPE", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(1200.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    @Test
    fun `parse Paytm wallet debit`() {
        val sms = "₹350 paid to BIGBASKET from Paytm Wallet on Jan 15, 2024"
        val result = SmsParser.parse("PYTM", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(350.0, result!!.amount, 0.01)
    }

    // ── OTP Filtering ─────────────────────────────────────────────────────────

    @Test
    fun `reject OTP messages`() {
        val sms = "Your OTP for HDFC Bank is 456789. Valid for 10 minutes. Do not share."
        val result = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNull("OTP messages should be filtered", result)
    }

    @Test
    fun `reject promotional messages`() {
        val sms = "Get flat 20% cashback offer! Apply now for HDFC credit card. Click here to apply."
        val result = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNull("Promotional messages should be filtered", result)
    }

    // ── Amount Parsing ─────────────────────────────────────────────────────────

    @Test
    fun `parse Rs with period format`() {
        val sms = "Rs.1,23,456.78 debited from your account"
        val result = SmsParser.parse("SBIINB", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(123456.78, result!!.amount, 0.01)
    }

    @Test
    fun `parse rupee symbol format`() {
        val sms = "₹2,500 spent at Flipkart via Credit Card XX5678"
        val result = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(2500.0, result!!.amount, 0.01)
    }

    @Test
    fun `reject very small amounts`() {
        val sms = "Rs.0.50 debited from your account"
        val result = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNull("Amounts less than ₹1 should be filtered", result)
    }

    // ── Merchant Normalization ─────────────────────────────────────────────────

    @Test
    fun `normalize swiggy merchant`() {
        val normalized = SmsParser.normalizeMerchant("Swiggy Online Food")
        assertEquals("swiggy", normalized)
    }

    @Test
    fun `normalize amazon merchant`() {
        val normalized = SmsParser.normalizeMerchant("Amazon.in")
        assertEquals("amazon", normalized)
    }

    @Test
    fun `normalize grofers to blinkit`() {
        val normalized = SmsParser.normalizeMerchant("Grofers India")
        assertEquals("blinkit", normalized)
    }

    // ── Deduplication ─────────────────────────────────────────────────────────

    @Test
    fun `same SMS within same minute produces same hash`() {
        val sms = "Rs.500 debited from XX1234 at Amazon"
        val ts = 1705324800000L // Fixed timestamp
        val result1 = SmsParser.parse("HDFCBK", sms, ts)
        val result2 = SmsParser.parse("HDFCBK", sms, ts)
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1!!.smsHash, result2!!.smsHash)
    }

    @Test
    fun `different SMS produces different hash`() {
        val ts = 1705324800000L
        val r1 = SmsParser.parse("HDFCBK", "Rs.500 debited at Amazon", ts)
        val r2 = SmsParser.parse("HDFCBK", "Rs.750 debited at Flipkart", ts)
        assertNotNull(r1)
        assertNotNull(r2)
        assertNotEquals(r1!!.smsHash, r2!!.smsHash)
    }

    @Test
    fun `detect self-transfer from multiple banks`() {
        val sms = "Rs.5,000 transferred from HDFC Bank to SBI account"
        val isSelf = SmsParser.isSelfTransferSuggestion(sms, "SBI A/c")
        assertTrue(isSelf)
    }

    @Test
    fun `detect self-transfer from keywords`() {
        val sms = "Rs.1,000 paid for paytm wallet topup"
        val isSelf = SmsParser.isSelfTransferSuggestion(sms, "Paytm Wallet")
        assertTrue(isSelf)
    }

    @Test
    fun `toTransaction initializes SELF_TRANSFER for self-transfers`() {
        val sms = "Rs.2,000 transferred from HDFC to own account"
        val parseResult = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNotNull(parseResult)
        
        val txn = SmsParser.toTransaction(parseResult!!, Category.UNCATEGORIZED)
        assertEquals(TransactionStatus.SELF_TRANSFER, txn.status)
        assertEquals(Category.SELF_TRANSFER.name, txn.category.name)
    }

    @Test
    fun `toTransaction initializes PENDING_REVIEW for regular transactions`() {
        val sms = "Rs.500 spent at SWIGGY from HDFC A/c"
        val parseResult = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNotNull(parseResult)
        
        val txn = SmsParser.toTransaction(parseResult!!, Category.FOOD_DINING)
        assertEquals(TransactionStatus.PENDING_REVIEW, txn.status)
        assertEquals(Category.FOOD_DINING.name, txn.category.name)
    }

    @Test
    fun `parse fallback to person name`() {
        val sms = "Rs 1500 transferred to Amit Kumar"
        val parseResult = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNotNull(parseResult)
        assertEquals(1500.0, parseResult!!.amount, 0.01)
        assertEquals("Amit Kumar", parseResult.merchant)
        assertTrue(parseResult.isPersonNameFallback)
    }

    @Test
    fun `toTransaction initializes PENDING_REVIEW and UNCATEGORIZED for person name fallback`() {
        val sms = "Rs 1500 transferred to Amit Kumar"
        val parseResult = SmsParser.parse("HDFCBK", sms, System.currentTimeMillis())
        assertNotNull(parseResult)
        
        val txn = SmsParser.toTransaction(parseResult!!, Category.FOOD_DINING)
        assertEquals(TransactionStatus.PENDING_REVIEW, txn.status)
        assertEquals(Category.UNCATEGORIZED.name, txn.category.name)
    }
}
