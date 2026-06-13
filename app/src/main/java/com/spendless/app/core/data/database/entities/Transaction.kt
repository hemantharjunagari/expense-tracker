package com.spendless.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a financial transaction detected from an SMS message.
 */
enum class TransactionType {
    DEBIT, CREDIT, SELF_TRANSFER, LENT, BORROWED
}

enum class PaymentMethod {
    UPI, BANK_TRANSFER, CARD, CASH, WALLET, NET_BANKING, EMI, LOAN, OTHER
}

@Serializable
data class TransactionSplit(
    val categoryName: String,
    val amount: Double,
    val note: String? = null
)

/**
 * Represents a financial transaction detected from an SMS message or entered manually.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = BudgetCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("cycleId"),
        Index("timestamp"),
        Index("category"),
        Index("merchantNormalized"),
        Index("smsHash", unique = true),
        Index("type"),
        Index("status")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val merchantNormalized: String,
    val category: Category,
    val account: String = "Manual",
    val rawSmsBody: String = "",
    val smsHash: String? = null,
    val timestamp: Long,
    val cycleId: Long? = null,
    val isManuallyEdited: Boolean = false,
    val isManualEntry: Boolean = false,
    val userNote: String = "",
    val status: TransactionStatus = TransactionStatus.PENDING_REVIEW,
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val tags: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val isExcludedFromBudget: Boolean = false,
    val isExcludedFromAnalytics: Boolean = false,
    val splits: List<TransactionSplit> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

