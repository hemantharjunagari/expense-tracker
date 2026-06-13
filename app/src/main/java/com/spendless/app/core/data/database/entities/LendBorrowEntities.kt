package com.spendless.app.core.data.database.entities

import androidx.room.*
import java.time.Instant

enum class LendBorrowType { LENT, BORROWED }

enum class LendBorrowStatus {
    ACTIVE,
    DUE_SOON,       // within 3 days
    DUE_TODAY,
    OVERDUE,
    PARTIALLY_PAID,
    COMPLETED;

    val displayName: String get() = when (this) {
        ACTIVE        -> "Active"
        DUE_SOON      -> "Due Soon"
        DUE_TODAY     -> "Due Today"
        OVERDUE       -> "Overdue"
        PARTIALLY_PAID -> "Partially Paid"
        COMPLETED     -> "Completed"
    }

    val emoji: String get() = when (this) {
        ACTIVE        -> "🟡"
        DUE_SOON      -> "🟠"
        DUE_TODAY     -> "🔴"
        OVERDUE       -> "⛔"
        PARTIALLY_PAID -> "🔵"
        COMPLETED     -> "✅"
    }
}

@Entity(tableName = "lend_borrow_records")
data class LendBorrowRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Contact info
    val contactName: String,
    val contactPhone: String,
    val contactPhotoUri: String? = null,
    val contactLookupKey: String? = null, // Android Contacts lookup key

    // Transaction details
    val type: LendBorrowType,
    val amount: Double,
    val interestRate: Double = 0.0, // % per month, 0 = no interest

    // Dates
    val givenDate: Long,            // epoch ms
    val dueDate: Long?,             // null = no due date

    // Repayment tracking
    val totalPaid: Double = 0.0,
    val outstanding: Double = amount, // computed, stored for fast queries

    // Status (auto-updated by repo/worker)
    val status: LendBorrowStatus = LendBorrowStatus.ACTIVE,

    // Optional fields
    val upiId: String? = null,
    val notes: String? = null,
    val tags: String? = null,        // comma separated
    val documentUri: String? = null,

    // Lifecycle
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isOverdue: Boolean get() = dueDate != null &&
        dueDate < System.currentTimeMillis() && status != LendBorrowStatus.COMPLETED

    val daysUntilDue: Long? get() = dueDate?.let {
        val diff = it - System.currentTimeMillis()
        diff / (1000 * 60 * 60 * 24)
    }

    /** Compute the correct status based on dates and payments */
    fun computedStatus(): LendBorrowStatus = when {
        outstanding <= 0.0 -> LendBorrowStatus.COMPLETED
        totalPaid > 0.0 && outstanding > 0.0 -> {
            when {
                dueDate != null && dueDate < System.currentTimeMillis() -> LendBorrowStatus.OVERDUE
                dueDate != null && (dueDate - System.currentTimeMillis()) < 3L * 86400_000 -> LendBorrowStatus.DUE_SOON
                else -> LendBorrowStatus.PARTIALLY_PAID
            }
        }
        dueDate != null && dueDate < System.currentTimeMillis() -> LendBorrowStatus.OVERDUE
        dueDate != null && isDueToday() -> LendBorrowStatus.DUE_TODAY
        dueDate != null && (dueDate - System.currentTimeMillis()) <= 3L * 86400_000 -> LendBorrowStatus.DUE_SOON
        else -> LendBorrowStatus.ACTIVE
    }

    private fun isDueToday(): Boolean {
        if (dueDate == null) return false
        val dueCal = java.util.Calendar.getInstance().apply { timeInMillis = dueDate }
        val todayCal = java.util.Calendar.getInstance()
        return dueCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
               dueCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

@Entity(
    tableName = "lend_borrow_payments",
    foreignKeys = [ForeignKey(
        entity = LendBorrowRecord::class,
        parentColumns = ["id"],
        childColumns = ["recordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recordId")]
)
data class LendBorrowPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lend_borrow_contacts")
data class LendBorrowContact(
    @PrimaryKey
    val phone: String,
    val name: String,
    val photoUri: String? = null,
    val lookupKey: String? = null,

    // Aggregates (refreshed when records change)
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val outstandingReceivable: Double = 0.0,  // we lent, they owe us
    val outstandingPayable: Double = 0.0,     // we borrowed, we owe them
    val overdueReceivable: Double = 0.0,
    val overduePayable: Double = 0.0,
    val lastActivityDate: Long = System.currentTimeMillis()
)
