package com.spendless.app.core.data.database.converters

import androidx.room.TypeConverter
import com.spendless.app.core.data.database.entities.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TypeConverters {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // TransactionType
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        try { TransactionType.valueOf(value) } catch (e: Exception) { TransactionType.DEBIT }

    // PaymentMethod
    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        try { PaymentMethod.valueOf(value) } catch (e: Exception) { PaymentMethod.OTHER }

    // List<TransactionSplit>
    @TypeConverter
    fun fromSplitList(list: List<TransactionSplit>): String =
        json.encodeToString(list)

    @TypeConverter
    fun toSplitList(value: String): List<TransactionSplit> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }

    // TransactionStatus
    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus =
        try { TransactionStatus.valueOf(value) } catch (e: Exception) { TransactionStatus.PENDING_REVIEW }

    // Category
    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category =
        Category.fromName(value)

    // Map<String, Double> for category budgets
    @TypeConverter
    fun fromCategoryMap(map: Map<String, Double>): String =
        json.encodeToString(map)

    @TypeConverter
    fun toCategoryMap(value: String): Map<String, Double> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyMap() }

    // List<String>
    @TypeConverter
    fun fromStringList(list: List<String>): String =
        json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }
}
