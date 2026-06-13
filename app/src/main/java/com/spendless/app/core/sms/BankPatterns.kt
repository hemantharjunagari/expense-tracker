package com.spendless.app.core.sms

/**
 * Collection of regex patterns for 50+ Indian bank/UPI/wallet SMS formats.
 * Each pattern is designed to be fast and non-backtracking.
 */
object BankPatterns {

    // ── Amount Patterns ────────────────────────────────────────────────────────

    /**
     * Matches: Rs.1,234.56 | Rs 1234 | INR 1,234.56 | ₹1234.56 | ₹ 1,234
     */
    val AMOUNT_PATTERN = Regex(
        """(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Alternative pattern: "debited by 1234" | "credited with 5000"
     */
    val AMOUNT_VERB_PATTERN = Regex(
        """(?:debited by|credited (?:by|with|for)|paid|spent|charged)\s+(?:Rs\.?\s*|INR\s*|₹\s*)?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // ── Transaction Type Indicators ────────────────────────────────────────────

    val DEBIT_KEYWORDS = setOf(
        "debited", "debit", "spent", "paid", "withdrawn", "withdrawal",
        "purchase", "payment", "charged", "deducted", "transferred out",
        "sent", "used at", "txn at", "transaction at", "transferred from your account"
    )

    val CREDIT_KEYWORDS = setOf(
        "credited", "credit", "received", "deposited", "refund",
        "cashback", "reversed", "added", "transferred to your",
        "money received", "amount received", "transferred to your account"
    )

    // ── Merchant Extraction Patterns ───────────────────────────────────────────

    /**
     * Common prepositions that introduce merchant names in SMS
     */
    val MERCHANT_PATTERNS = listOf(
        // "at MERCHANT" or "at MERCHANT on date"
        Regex("""(?:at|@)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\s+dated|\s+via|\s+using|\s+ref|\.|,|$)""", RegexOption.IGNORE_CASE),
        // "to MERCHANT"
        Regex("""(?:\bto\b)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\s+UPI|\s+ref|\s+via|\.|,|$)""", RegexOption.IGNORE_CASE),
        // "towards MERCHANT"
        Regex("""(?:towards)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\.|,|$)""", RegexOption.IGNORE_CASE),
        // "for MERCHANT"
        Regex("""(?:for)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on|\.|,|via|$)""", RegexOption.IGNORE_CASE),
        // UPI: "UPI-MERCHANT-..."
        Regex("""UPI[-/]([A-Za-z0-9\s]+?)[-/\s]""", RegexOption.IGNORE_CASE)
    )

    // ── Account/Card Patterns ──────────────────────────────────────────────────

    val ACCOUNT_PATTERNS = listOf(
        // "A/c XX1234" or "Acct ending 1234" or "AC 1234"
        Regex("""(?:A/?c|Acct?|Account|Card|card|A/C)\s*(?:no\.?\s*)?(?:XX+|X+|ending\s+)?(\d{4})""", RegexOption.IGNORE_CASE),
        // "card ending 1234"
        Regex("""(?:card ending|ending in|ending)\s+(\d{4})""", RegexOption.IGNORE_CASE),
        // "XXXX1234" format
        Regex("""X{2,}(\d{4})"""),
        // Credit card last 4
        Regex("""(?:credit card|debit card|CC|DC)\s*\*+(\d{4})""", RegexOption.IGNORE_CASE)
    )

    // ── Date/Time Patterns ─────────────────────────────────────────────────────

    val DATE_PATTERNS = listOf(
        // DD-MM-YYYY or DD/MM/YYYY
        Regex("""(\d{2})[/-](\d{2})[/-](\d{4})"""),
        // DD-MMM-YYYY (01-Jan-2024)
        Regex("""(\d{2})-([A-Za-z]{3})-(\d{4})"""),
        // YYYY-MM-DD
        Regex("""(\d{4})[/-](\d{2})[/-](\d{2})""")
    )

    // ── Bank SMS Sender IDs ────────────────────────────────────────────────────

    val BANK_SENDERS = setOf(
        // HDFC
        "HDFCBK", "HDFCBN", "HDFC", "VM-HDFCBK",
        // SBI
        "SBIPSG", "SBIINB", "SBICRD", "SBI",
        // ICICI
        "ICICIB", "ICICIT", "ICICIBANK",
        // Axis
        "AXISBK", "AXISNB", "AXIS",
        // Kotak
        "KOTAKB", "KOTAK",
        // Yes Bank
        "YESBKG", "YESBNK",
        // IDFC
        "IDFCBK", "IDFC",
        // Canara
        "CANBKG", "CANBNK",
        // PNB
        "PNBSMS", "PNB",
        // BOI
        "BOISBD",
        // IndusInd
        "INDBNK", "INDUSL",
        // RBL
        "RBLBNK",
        // Federal
        "FEDBKS",
        // Paytm Bank
        "PAYTMB", "PYTBNK",
        // UPI apps
        "PYTM", "GPAY", "PHONEPE", "BHIM", "MOBKWK",
        // Credit cards
        "AMEXIN", "SCCARD", "CITI",
        // Wallet
        "AMZPAY", "FKAXIS"
    )

    // ── OTP / Non-financial Filter Keywords ───────────────────────────────────

    val OTP_KEYWORDS = setOf(
        "otp", "one time password", "verification code", "login otp",
        "transaction otp", "secure code", "2fa", "auth code",
        "do not share", "not share this", "never share"
    )

    val NON_FINANCIAL_KEYWORDS = setOf(
        "dear customer", "your account", "click here", "visit us",
        "click to", "download", "install", "offer", "cashback offer",
        "limited time", "exciting offer", "get flat", "earn upto",
        "apply now", "pre-approved", "loan offer", "credit offer"
    )

    val FINANCIAL_REQUIRED_KEYWORDS = setOf(
        "debited", "credited", "rs.", "rs ", "inr", "₹",
        "spent", "paid", "withdrawn", "refund", "balance",
        "transaction", "payment", "purchase"
    )

    // ── UPI Reference Patterns ────────────────────────────────────────────────

    val UPI_REF_PATTERN = Regex(
        """(?:UPI\s*Ref\.?\s*(?:No\.?\s*)?|Ref\s*No\.?\s*|txnid\s*:?\s*|transaction\s*id\s*:?\s*)(\d{10,})""",
        RegexOption.IGNORE_CASE
    )

    // ── Balance Pattern ───────────────────────────────────────────────────────

    val BALANCE_PATTERN = Regex(
        """(?:avl|available|bal\.?|balance)\s*(?:is\s*)?(?:Rs\.?\s*|INR\s*|₹\s*)?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
}
