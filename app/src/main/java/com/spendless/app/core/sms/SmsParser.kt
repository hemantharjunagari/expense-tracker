package com.spendless.app.core.sms

import android.util.Log
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.Transaction
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.database.entities.TransactionStatus
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Multi-bank SMS parser for Indian financial transaction messages.
 *
 * Supports:
 * - HDFC, SBI, ICICI, Axis, Kotak, Yes, IDFC, PNB, BOI, Canara, IndusInd, RBL
 * - UPI: PhonePe, GPay, Paytm, BHIM
 * - Credit cards: all major issuers
 * - Wallets: Amazon Pay, Mobikwik, Freecharge
 */
object SmsParser {

    private const val TAG = "SmsParser"

    data class ParseResult(
        val amount: Double,
        val type: TransactionType,
        val merchant: String,
        val account: String,
        val timestamp: Long,
        val rawBody: String,
        val smsHash: String,
        val isPersonNameFallback: Boolean = false
    )

    /**
     * Parse an SMS message and return a ParseResult if it's a financial transaction.
     * Returns null if the message is not a valid financial transaction.
     */
    fun parse(sender: String, body: String, smsTimestamp: Long = System.currentTimeMillis()): ParseResult? {
        return try {
            // Step 1: Filter non-financial messages
            if (!SmsFilter.isFinancialTransaction(sender, body)) {
                // Log.v(TAG, "Filtered out SMS from $sender (non-financial)")
                return null
            }

            // Step 2: Extract amount
            val amount = extractAmount(body) ?: run {
                // Log.v(TAG, "Could not extract amount from SMS: ${body.take(50)}")
                return null
            }

            // Reject unreasonably small or large amounts
            if (amount < 1.0 || amount > 50_000_000.0) return null

            // Step 3: Determine transaction type
            val typeHint = SmsFilter.detectTransactionType(body)
            val type = when (typeHint) {
                SmsFilter.TransactionTypeHint.CREDIT -> TransactionType.CREDIT
                else -> TransactionType.DEBIT
            }

            // Step 4: Extract merchant
            var isPersonName = false
            var merchant = extractMerchant(body).trim()
            val personMatch = Regex("""(?:\bto\b|\bsent to\b|\bpaid to\b|\btransferred to\b|\btransfer to\b)\s+([a-zA-Z\s]{3,30})""", RegexOption.IGNORE_CASE).find(body)
            if (personMatch != null) {
                val potentialName = personMatch.groupValues[1].trim()
                val excluded = listOf("account", "a/c", "card", "self", "bank", "wallet", "ref", "upi", "vpa", "no", "number", "your", "my", "our", "their", "mobile", "phone")
                val isExcluded = excluded.any { potentialName.lowercase().contains(it) }
                if (!isExcluded && potentialName.isNotEmpty()) {
                    val cleanedName = cleanMerchantName(potentialName)
                    val isKnownCorporate = com.spendless.app.core.categorization.MerchantDatabase.EXACT_MERCHANT_MAP.containsKey(cleanedName.lowercase())
                    if (!isKnownCorporate) {
                        merchant = cleanedName
                        isPersonName = true
                    }
                }
            }
            if (merchant.length < 2) {
                merchant = "Unknown"
            }

            // Step 5: Extract account
            val account = extractAccount(body)

            // Step 6: Hash for deduplication
            val hashInput = "${body.trim()}${smsTimestamp / 60000}" // Per-minute bucket
            val smsHash = sha256(hashInput)

            ParseResult(
                amount = amount,
                type = type,
                merchant = merchant,
                account = account,
                timestamp = smsTimestamp,
                rawBody = body,
                smsHash = smsHash,
                isPersonNameFallback = isPersonName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS: ${e.message}")
            null
        }
    }

    // ── Amount Extraction ──────────────────────────────────────────────────────

    private fun extractAmount(body: String): Double? {
        // Try primary pattern first
        val primaryMatch = BankPatterns.AMOUNT_PATTERN.find(body)
        if (primaryMatch != null) {
            return primaryMatch.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        // Try verb pattern
        val verbMatch = BankPatterns.AMOUNT_VERB_PATTERN.find(body)
        if (verbMatch != null) {
            return verbMatch.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        return null
    }

    // ── Merchant Extraction ────────────────────────────────────────────────────

    private fun extractMerchant(body: String): String {
        for (pattern in BankPatterns.MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                    .replace(Regex("""[^A-Za-z0-9\s\-&'.@]"""), "")
                    .trim()
                if (merchant.length >= 2 && merchant.length <= 50) {
                    return cleanMerchantName(merchant)
                }
            }
        }

        // Fallback: look for VPA (UPI address)
        val vpaMatch = Regex("""([a-zA-Z0-9._\-]+@[a-zA-Z0-9]+)""").find(body)
        if (vpaMatch != null) {
            return vpaMatch.groupValues[1].split("@").firstOrNull()
                ?.replace(Regex("""[._\-]"""), " ")
                ?.trim()
                ?.uppercase()
                ?: "UPI Transfer"
        }

        return "Unknown"
    }

    private fun cleanMerchantName(raw: String): String {
        return raw
            .replace(Regex("""\s+"""), " ")
            .trim()
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            .take(50)
    }

    /**
     * Normalize merchant for grouping (lowercase, remove special chars, trim)
     */
    fun normalizeMerchant(merchant: String): String {
        return merchant
            .lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .let { applyMerchantAlias(it) }
    }

    /**
     * Apply known merchant aliases for better grouping.
     * E.g., "swiggy order" → "swiggy"
     */
    private fun applyMerchantAlias(normalized: String): String {
        return when {
            normalized.startsWith("swiggy") -> "swiggy"
            normalized.startsWith("zomato") -> "zomato"
            normalized.startsWith("amazon") -> "amazon"
            normalized.startsWith("flipkart") -> "flipkart"
            normalized.startsWith("uber") -> "uber"
            normalized.startsWith("ola") -> "ola"
            normalized.startsWith("rapido") -> "rapido"
            normalized.startsWith("blinkit") || normalized.startsWith("grofers") -> "blinkit"
            normalized.startsWith("zepto") -> "zepto"
            normalized.startsWith("bigbasket") || normalized.startsWith("big basket") -> "bigbasket"
            normalized.startsWith("netflix") -> "netflix"
            normalized.startsWith("spotify") -> "spotify"
            normalized.startsWith("youtube") -> "youtube premium"
            normalized.startsWith("myntra") -> "myntra"
            normalized.startsWith("meesho") -> "meesho"
            normalized.startsWith("bookmyshow") || normalized.startsWith("book my show") -> "bookmyshow"
            normalized.startsWith("pvr") -> "pvr cinemas"
            normalized.startsWith("inox") -> "inox"
            normalized.startsWith("dominos") || normalized.startsWith("domino") -> "dominos"
            normalized.startsWith("mcd") || normalized.startsWith("mcdonalds") -> "mcdonalds"
            normalized.startsWith("kfc") -> "kfc"
            normalized.startsWith("dunzo") -> "dunzo"
            normalized.startsWith("dmart") || normalized.startsWith("d mart") -> "dmart"
            normalized.startsWith("reliance") -> "reliance"
            normalized.startsWith("nykaa") -> "nykaa"
            normalized.startsWith("1mg") -> "1mg"
            normalized.startsWith("pharmeasy") -> "pharmeasy"
            normalized.startsWith("apollo") -> "apollo pharmacy"
            normalized.startsWith("phonepe") -> "phonepe"
            normalized.startsWith("paytm") -> "paytm"
            normalized.startsWith("gpay") || normalized.startsWith("google pay") -> "google pay"
            else -> normalized
        }
    }

    // ── Account Extraction ─────────────────────────────────────────────────────

    private fun extractAccount(body: String): String {
        for (pattern in BankPatterns.ACCOUNT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val last4 = match.groupValues[1]
                return "**$last4"
            }
        }
        return "Unknown"
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert a ParseResult to a Transaction entity (without category — caller sets it).
     */
    fun toTransaction(result: ParseResult, category: Category): Transaction {
        val isSelf = isSelfTransferSuggestion(result.rawBody, result.merchant)
        val finalCategory = when {
            isSelf -> Category.SELF_TRANSFER
            result.isPersonNameFallback -> Category.UNCATEGORIZED
            else -> category
        }
        val initialStatus = when {
            isSelf -> TransactionStatus.SELF_TRANSFER
            else -> TransactionStatus.PENDING_REVIEW
        }
        return Transaction(
            amount = result.amount,
            type = result.type,
            merchant = result.merchant,
            merchantNormalized = normalizeMerchant(result.merchant),
            category = finalCategory,
            account = result.account,
            rawSmsBody = result.rawBody,
            smsHash = result.smsHash,
            timestamp = result.timestamp,
            status = initialStatus
        )
    }

    /**
     * Suggest if a transaction is a self-transfer based on SMS text heuristics.
     */
    fun isSelfTransferSuggestion(body: String, merchant: String): Boolean {
        val lowerBody = body.lowercase()
        val lowerMerchant = merchant.lowercase()
        
        val bankKeywords = listOf("hdfc", "sbi", "icici", "axis", "kotak", "yes bank", "idfc", "canara", "pnb", "boi", "indusind", "rbl", "federal")
        val containsMultipleBanks = bankKeywords.count { lowerBody.contains(it) } >= 2

        val selfTransferKeywords = listOf(
            "self transfer", "to self", "transfer to own", "own account",
            "wallet top-up", "wallet topup", "topup wallet", "top-up wallet", "paytm wallet", "phonepe wallet",
            "credit card bill", "credit card payment", "cc bill", "cc payment",
            "fixed deposit", "fd creation", "fd transfer", "mutual fund", "zerodha", "groww", "investment"
        )

        return containsMultipleBanks || 
               selfTransferKeywords.any { lowerBody.contains(it) } ||
               lowerMerchant.contains("self") || 
               lowerMerchant.contains("wallet top") ||
               lowerMerchant.contains("credit card bill")
    }
}
