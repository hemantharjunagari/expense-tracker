package com.spendless.app.core.data.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.spendless.app.core.data.database.entities.*
import kotlinx.coroutines.flow.Flow

data class LendBorrowSummary(
    val totalLent: Double,
    val totalBorrowed: Double,
    val totalOutstandingReceivable: Double,
    val totalOutstandingPayable: Double,
    val totalOverdueReceivable: Double,
    val totalOverduePayable: Double,
    val totalUpcomingDue: Double      // due within next 7 days
)

data class ContactAggregate(
    val phone: String,
    val name: String,
    val recordCount: Int,
    val totalLent: Double,
    val totalBorrowed: Double,
    val outstanding: Double
)

@Dao
interface LendBorrowDao {

    // ── Records ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LendBorrowRecord): Long

    @Update
    suspend fun updateRecord(record: LendBorrowRecord)

    @Query("SELECT * FROM lend_borrow_records WHERE id = :id AND isDeleted = 0")
    fun getRecordById(id: Long): Flow<LendBorrowRecord?>

    @Query("SELECT * FROM lend_borrow_records WHERE id = :id AND isDeleted = 0")
    suspend fun getRecordByIdSync(id: Long): LendBorrowRecord?

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0
        ORDER BY 
            CASE status 
                WHEN 'OVERDUE' THEN 0 
                WHEN 'DUE_TODAY' THEN 1 
                WHEN 'DUE_SOON' THEN 2 
                WHEN 'PARTIALLY_PAID' THEN 3 
                WHEN 'ACTIVE' THEN 4 
                ELSE 5 
            END, dueDate ASC
    """)
    fun getAllRecordsPaged(): PagingSource<Int, LendBorrowRecord>

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0 AND type = :type
        ORDER BY updatedAt DESC
    """)
    fun getRecordsByType(type: LendBorrowType): Flow<List<LendBorrowRecord>>

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0 AND contactPhone = :phone
        ORDER BY createdAt DESC
    """)
    fun getRecordsForContact(phone: String): Flow<List<LendBorrowRecord>>

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0 AND contactPhone = :phone
    """)
    suspend fun getRecordsForContactSync(phone: String): List<LendBorrowRecord>

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0 AND status IN ('OVERDUE', 'DUE_TODAY', 'DUE_SOON')
        ORDER BY dueDate ASC
    """)
    fun getUpcomingAndOverdueRecords(): Flow<List<LendBorrowRecord>>

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0 AND status = 'OVERDUE'
        ORDER BY dueDate ASC
    """)
    suspend fun getAllOverdueSync(): List<LendBorrowRecord>

    @Query("""
        SELECT * FROM lend_borrow_records
        WHERE isDeleted = 0 AND contactPhone = :phone 
          AND status NOT IN ('COMPLETED') 
        LIMIT 1
    """)
    suspend fun getActiveRecordForContact(phone: String): LendBorrowRecord?

    @Query("""
        SELECT * FROM lend_borrow_records 
        WHERE isDeleted = 0 
          AND contactPhone = :phone 
          AND amount = :amount 
          AND givenDate = :givenDate
        LIMIT 1
    """)
    suspend fun findMatchingRecordSync(phone: String, amount: Double, givenDate: Long): LendBorrowRecord?

    @Query("""
        SELECT * FROM lend_borrow_records
        WHERE isDeleted = 0 
          AND dueDate IS NOT NULL 
          AND dueDate BETWEEN :startMs AND :endMs
          AND status NOT IN ('COMPLETED')
    """)
    suspend fun getRecordsDueInRange(startMs: Long, endMs: Long): List<LendBorrowRecord>

    @Query("""
        SELECT * FROM lend_borrow_records
        WHERE isDeleted = 0 
          AND (contactName LIKE '%' || :query || '%' OR contactPhone LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchRecords(query: String): PagingSource<Int, LendBorrowRecord>

    @Query("UPDATE lend_borrow_records SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteRecord(id: Long, now: Long = System.currentTimeMillis())

    // ── Payments ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LendBorrowPayment): Long

    @Query("SELECT * FROM lend_borrow_payments WHERE recordId = :recordId ORDER BY paymentDate DESC")
    fun getPaymentsForRecord(recordId: Long): Flow<List<LendBorrowPayment>>

    @Query("SELECT SUM(amount) FROM lend_borrow_payments WHERE recordId = :recordId")
    suspend fun getTotalPaidForRecord(recordId: Long): Double?

    // ── Contacts ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: LendBorrowContact)

    @Query("SELECT * FROM lend_borrow_contacts ORDER BY lastActivityDate DESC")
    fun getAllContacts(): Flow<List<LendBorrowContact>>

    @Query("SELECT * FROM lend_borrow_contacts WHERE phone = :phone")
    suspend fun getContactByPhone(phone: String): LendBorrowContact?

    @Query("""
        SELECT * FROM lend_borrow_contacts 
        WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'
        ORDER BY lastActivityDate DESC
    """)
    fun searchContacts(query: String): Flow<List<LendBorrowContact>>

    // ── Aggregates ─────────────────────────────────────────────────────────────

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'LENT' THEN amount ELSE 0 END), 0) as totalLent,
            COALESCE(SUM(CASE WHEN type = 'BORROWED' THEN amount ELSE 0 END), 0) as totalBorrowed,
            COALESCE(SUM(CASE WHEN type = 'LENT' AND status != 'COMPLETED' THEN outstanding ELSE 0 END), 0) as totalOutstandingReceivable,
            COALESCE(SUM(CASE WHEN type = 'BORROWED' AND status != 'COMPLETED' THEN outstanding ELSE 0 END), 0) as totalOutstandingPayable,
            COALESCE(SUM(CASE WHEN type = 'LENT' AND status = 'OVERDUE' THEN outstanding ELSE 0 END), 0) as totalOverdueReceivable,
            COALESCE(SUM(CASE WHEN type = 'BORROWED' AND status = 'OVERDUE' THEN outstanding ELSE 0 END), 0) as totalOverduePayable,
            COALESCE(SUM(CASE WHEN status NOT IN ('COMPLETED') AND dueDate BETWEEN :nowMs AND :sevenDaysMs THEN outstanding ELSE 0 END), 0) as totalUpcomingDue
        FROM lend_borrow_records
        WHERE isDeleted = 0
    """)
    fun getSummary(nowMs: Long, sevenDaysMs: Long): Flow<LendBorrowSummary>

    @Query("""
        SELECT contactPhone as phone, contactName as name, 
               COUNT(*) as recordCount,
               SUM(CASE WHEN type = 'LENT' THEN amount ELSE 0 END) as totalLent,
               SUM(CASE WHEN type = 'BORROWED' THEN amount ELSE 0 END) as totalBorrowed,
               SUM(CASE WHEN type = 'LENT' THEN outstanding ELSE -outstanding END) as outstanding
        FROM lend_borrow_records
        WHERE isDeleted = 0 AND status != 'COMPLETED'
        GROUP BY contactPhone
        ORDER BY ABS(SUM(CASE WHEN type = 'LENT' THEN outstanding ELSE -outstanding END)) DESC
        LIMIT :limit
    """)
    fun getTopContacts(limit: Int = 5): Flow<List<ContactAggregate>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM lend_borrow_records
        WHERE isDeleted = 0 AND type = 'LENT' AND status = 'COMPLETED'
    """)
    fun getTotalRecovered(): Flow<Double>

    @Query("SELECT COUNT(*) FROM lend_borrow_records WHERE isDeleted = 0 AND status NOT IN ('COMPLETED')")
    fun getActiveRecordCount(): Flow<Int>
}
