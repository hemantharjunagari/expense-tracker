package com.spendless.app.lend.repository

import com.spendless.app.core.data.database.dao.LendBorrowDao
import com.spendless.app.core.data.database.dao.LendBorrowSummary
import com.spendless.app.core.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LendBorrowRepository @Inject constructor(
    private val dao: LendBorrowDao
) {
    // ── Records ────────────────────────────────────────────────────────────────

    fun getAllRecordsPaged() = dao.getAllRecordsPaged()
    fun searchRecords(q: String) = dao.searchRecords(q)
    fun getRecord(id: Long) = dao.getRecordById(id)
    fun getRecordsByType(type: LendBorrowType) = dao.getRecordsByType(type)
    fun getUpcomingAndOverdue() = dao.getUpcomingAndOverdueRecords()
    fun getRecordsForContact(phone: String) = dao.getRecordsForContact(phone)

    suspend fun createRecord(record: LendBorrowRecord): Long {
        val id = dao.insertRecord(record)
        refreshContactAggregate(record.contactPhone)
        return id
    }

    suspend fun addPartialPayment(recordId: Long, amount: Double, notes: String?) {
        val record = dao.getRecordByIdSync(recordId) ?: return
        val payment = LendBorrowPayment(recordId = recordId, amount = amount, notes = notes)
        dao.insertPayment(payment)

        val totalPaid = dao.getTotalPaidForRecord(recordId) ?: 0.0
        val outstanding = (record.amount - totalPaid).coerceAtLeast(0.0)
        val updatedRecord = record.copy(
            totalPaid = totalPaid,
            outstanding = outstanding,
            updatedAt = System.currentTimeMillis()
        ).let { it.copy(status = it.computedStatus()) }

        dao.updateRecord(updatedRecord)
        refreshContactAggregate(record.contactPhone)
    }

    suspend fun markAsPaid(recordId: Long) {
        val record = dao.getRecordByIdSync(recordId) ?: return
        val remaining = record.outstanding
        if (remaining > 0) {
            val payment = LendBorrowPayment(recordId = recordId, amount = remaining)
            dao.insertPayment(payment)
        }
        dao.updateRecord(
            record.copy(
                totalPaid = record.amount,
                outstanding = 0.0,
                status = LendBorrowStatus.COMPLETED,
                updatedAt = System.currentTimeMillis()
            )
        )
        refreshContactAggregate(record.contactPhone)
    }

    suspend fun extendDueDate(recordId: Long, newDueDate: Long) {
        val record = dao.getRecordByIdSync(recordId) ?: return
        val updated = record.copy(dueDate = newDueDate, updatedAt = System.currentTimeMillis())
            .let { it.copy(status = it.computedStatus()) }
        dao.updateRecord(updated)
    }

    suspend fun deleteRecord(recordId: Long) {
        val record = dao.getRecordByIdSync(recordId) ?: return
        dao.softDeleteRecord(recordId)
        refreshContactAggregate(record.contactPhone)
    }

    suspend fun checkDuplicate(phone: String): LendBorrowRecord? =
        dao.getActiveRecordForContact(phone)

    // ── Payments ────────────────────────────────────────────────────────────────

    fun getPaymentsForRecord(recordId: Long) = dao.getPaymentsForRecord(recordId)

    // ── Contacts ────────────────────────────────────────────────────────────────

    fun getAllContacts() = dao.getAllContacts()
    fun searchContacts(q: String) = dao.searchContacts(q)

    // ── Analytics ────────────────────────────────────────────────────────────────

    fun getSummary(): Flow<LendBorrowSummary> {
        val now = System.currentTimeMillis()
        return dao.getSummary(now, now + 7L * 86400_000)
    }

    fun getTopContacts(limit: Int = 5) = dao.getTopContacts(limit)
    fun getTotalRecovered() = dao.getTotalRecovered()
    fun getActiveRecordCount() = dao.getActiveRecordCount()

    // ── Internal ────────────────────────────────────────────────────────────────

    private suspend fun refreshContactAggregate(phone: String) {
        val records = dao.getRecordsForContactSync(phone)
        if (records.isEmpty()) return

        val totalLent = records.filter { it.type == LendBorrowType.LENT }.sumOf { it.amount }
        val totalBorrowed = records.filter { it.type == LendBorrowType.BORROWED }.sumOf { it.amount }
        val outstandingReceivable = records.filter { it.type == LendBorrowType.LENT && it.status != LendBorrowStatus.COMPLETED }.sumOf { it.outstanding }
        val outstandingPayable = records.filter { it.type == LendBorrowType.BORROWED && it.status != LendBorrowStatus.COMPLETED }.sumOf { it.outstanding }
        val overdueReceivable = records.filter { it.type == LendBorrowType.LENT && it.status == LendBorrowStatus.OVERDUE }.sumOf { it.outstanding }
        val overduePayable = records.filter { it.type == LendBorrowType.BORROWED && it.status == LendBorrowStatus.OVERDUE }.sumOf { it.outstanding }

        val existing = dao.getContactByPhone(phone)
        val name = existing?.name ?: records.firstOrNull()?.contactName ?: "Unknown"
        val photoUri = existing?.photoUri ?: records.firstOrNull()?.contactPhotoUri

        dao.upsertContact(
            LendBorrowContact(
                phone = phone,
                name = name,
                photoUri = photoUri,
                lookupKey = existing?.lookupKey,
                totalLent = totalLent,
                totalBorrowed = totalBorrowed,
                outstandingReceivable = outstandingReceivable,
                outstandingPayable = outstandingPayable,
                overdueReceivable = overdueReceivable,
                overduePayable = overduePayable,
                lastActivityDate = System.currentTimeMillis()
            )
        )
    }

    suspend fun upsertContact(contact: LendBorrowContact) {
        dao.upsertContact(contact)
    }

    suspend fun getContactByPhone(phone: String): LendBorrowContact? =
        dao.getContactByPhone(phone)

    suspend fun findMatchingRecord(phone: String, amount: Double, givenDate: Long): LendBorrowRecord? =
        dao.findMatchingRecordSync(phone, amount, givenDate)

    suspend fun syncSystemContacts(context: android.content.Context) {
        val systemContacts = mutableListOf<LendBorrowContact>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                android.provider.ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            ),
            null,
            null,
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            val lookupIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val num = it.getString(numIdx) ?: continue
                val photo = it.getString(photoIdx)
                val lookup = it.getString(lookupIdx)
                val cleanPhone = num.replace(Regex("[\\s\\-\\(\\)]"), "")
                if (cleanPhone.isNotEmpty()) {
                    systemContacts.add(
                        LendBorrowContact(
                            phone = cleanPhone,
                            name = name,
                            photoUri = photo,
                            lookupKey = lookup
                        )
                    )
                }
            }
        }
        
        systemContacts.distinctBy { it.phone }.forEach { contact ->
            val existing = dao.getContactByPhone(contact.phone)
            if (existing == null) {
                dao.upsertContact(contact)
            } else {
                dao.upsertContact(
                    existing.copy(
                        name = contact.name,
                        photoUri = contact.photoUri,
                        lookupKey = contact.lookupKey
                    )
                )
            }
        }
    }
}
