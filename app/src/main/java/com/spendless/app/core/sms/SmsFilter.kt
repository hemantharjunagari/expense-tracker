package com.spendless.app.core.sms

/**
 * Filters SMS messages to determine if they are financial transactions.
 * Rejects OTPs, promotional messages, and non-bank senders.
 */
object SmsFilter {

    /**
     * Returns true if the SMS is a financial transaction message worth parsing.
     */
    fun isFinancialTransaction(sender: String, body: String): Boolean {
        val lowerBody = body.lowercase()
        val upperSender = sender.uppercase()

        // 1. Check for OTP keywords (highest priority reject)
        if (containsOtp(lowerBody)) return false

        // 2. Check for non-financial promotional patterns
        if (isPromotional(lowerBody)) return false

        // 3. Must contain at least one financial keyword
        if (!containsFinancialKeyword(lowerBody)) return false

        // 4. Must contain an amount pattern
        if (!containsAmount(lowerBody)) return false

        // 5. Sender check (trusted bank sender IDs)
        // Note: We don't REQUIRE a known sender because some banks use random numbers
        // but we do flag known non-bank senders
        if (isKnownNonFinancialSender(upperSender)) return false

        return true
    }

    private fun containsOtp(lowerBody: String): Boolean {
        return BankPatterns.OTP_KEYWORDS.any { keyword -> lowerBody.contains(keyword) }
    }

    private fun isPromotional(lowerBody: String): Boolean {
        // Promotional SMS usually lack specific amounts and contain marketing language
        val promoCount = BankPatterns.NON_FINANCIAL_KEYWORDS.count { lowerBody.contains(it) }
        val financialCount = BankPatterns.FINANCIAL_REQUIRED_KEYWORDS.count { lowerBody.contains(it) }
        // If more promo keywords than financial keywords, likely promotional
        return promoCount > 2 && financialCount == 0
    }

    private fun containsFinancialKeyword(lowerBody: String): Boolean {
        return BankPatterns.FINANCIAL_REQUIRED_KEYWORDS.any { lowerBody.contains(it) }
    }

    private fun containsAmount(body: String): Boolean {
        return BankPatterns.AMOUNT_PATTERN.containsMatchIn(body) ||
               BankPatterns.AMOUNT_VERB_PATTERN.containsMatchIn(body)
    }

    private fun isKnownNonFinancialSender(upperSender: String): Boolean {
        // Known non-financial short codes
        val nonFinancialPrefixes = setOf(
            "VM-JIOTN", "VM-AIRTEL", "JIO-", "AIRTEL",
            "VM-VODAF", "VM-BSNL", "AMAZON-", "FLIPKRT"
        )
        return nonFinancialPrefixes.any { upperSender.startsWith(it) }
    }

    /**
     * Detects transaction type from SMS body.
     */
    fun detectTransactionType(body: String): TransactionTypeHint {
        val lowerBody = body.lowercase()

        val debitScore = BankPatterns.DEBIT_KEYWORDS.count { lowerBody.contains(it) }
        val creditScore = BankPatterns.CREDIT_KEYWORDS.count { lowerBody.contains(it) }

        return when {
            debitScore > creditScore -> TransactionTypeHint.DEBIT
            creditScore > debitScore -> TransactionTypeHint.CREDIT
            // Tie-breaker: context clues
            lowerBody.contains("refund") || lowerBody.contains("cashback") -> TransactionTypeHint.CREDIT
            else -> TransactionTypeHint.DEBIT // Default to debit (more common)
        }
    }

    enum class TransactionTypeHint { DEBIT, CREDIT, UNKNOWN }
}
