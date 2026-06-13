package com.spendless.app.core.data.database.entities

/**
 * Represents the verification state of a transaction.
 */
enum class TransactionStatus {
    PENDING_REVIEW,
    APPROVED,
    SELF_TRANSFER,
    IGNORED,
    LENT,
    BORROWED
}
