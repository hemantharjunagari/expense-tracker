package com.spendless.app.lend.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.data.database.entities.*
import com.spendless.app.lend.repository.LendBorrowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ContactSuggestion(
    val name: String,
    val phone: String,
    val photoUri: String? = null
)

data class AddLendBorrowState(
    val type: LendBorrowType = LendBorrowType.LENT,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactPhotoUri: String? = null,
    val amount: String = "",
    val givenDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val interestRate: String = "",
    val notes: String = "",
    val upiId: String = "",
    val tags: String = "",
    val contactSuggestions: List<ContactSuggestion> = emptyList(),
    val duplicateRecord: LendBorrowRecord? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddLendBorrowViewModel @Inject constructor(
    private val repository: LendBorrowRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AddLendBorrowState())
    val state: StateFlow<AddLendBorrowState> = _state.asStateFlow()

    init {
        val typeStr = savedStateHandle.get<String>("type") ?: LendBorrowType.LENT.name
        _state.update { it.copy(type = LendBorrowType.valueOf(typeStr)) }
    }

    fun setContactName(name: String) {
        _state.update { it.copy(contactName = name, duplicateRecord = null) }
        if (name.length >= 2) {
            searchAndroidContacts(name)
        }
    }

    fun setContactPhone(phone: String) {
        _state.update { it.copy(contactPhone = phone) }
    }

    fun selectContact(suggestion: ContactSuggestion) {
        _state.update {
            it.copy(
                contactName = suggestion.name,
                contactPhone = suggestion.phone,
                contactPhotoUri = suggestion.photoUri,
                contactSuggestions = emptyList()
            )
        }
        // Check for duplicates
        viewModelScope.launch {
            val dup = repository.checkDuplicate(suggestion.phone)
            _state.update { it.copy(duplicateRecord = dup) }
        }
    }

    fun setAmount(v: String) = _state.update { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setGivenDate(ms: Long) = _state.update { it.copy(givenDate = ms) }
    fun setDueDate(ms: Long?) = _state.update { it.copy(dueDate = ms) }
    fun setInterestRate(v: String) = _state.update { it.copy(interestRate = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setUpiId(v: String) = _state.update { it.copy(upiId = v) }
    fun setTags(v: String) = _state.update { it.copy(tags = v) }
    fun clearSuggestions() = _state.update { it.copy(contactSuggestions = emptyList()) }

    fun save() {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull()
        if (s.contactName.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter a contact name") }
            return
        }
        if (amount == null || amount <= 0) {
            _state.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val record = LendBorrowRecord(
                    contactName = s.contactName.trim(),
                    contactPhone = s.contactPhone.trim(),
                    contactPhotoUri = s.contactPhotoUri,
                    type = s.type,
                    amount = amount,
                    interestRate = s.interestRate.toDoubleOrNull() ?: 0.0,
                    givenDate = s.givenDate,
                    dueDate = s.dueDate,
                    outstanding = amount,
                    notes = s.notes.ifBlank { null },
                    upiId = s.upiId.ifBlank { null },
                    tags = s.tags.ifBlank { null }
                )
                repository.createRecord(record)

                // Upsert contact
                val existingContact = repository.getContactByPhone(s.contactPhone)
                repository.upsertContact(
                    LendBorrowContact(
                        phone = s.contactPhone,
                        name = s.contactName,
                        photoUri = s.contactPhotoUri,
                        totalLent = (existingContact?.totalLent ?: 0.0) + if (s.type == LendBorrowType.LENT) amount else 0.0,
                        totalBorrowed = (existingContact?.totalBorrowed ?: 0.0) + if (s.type == LendBorrowType.BORROWED) amount else 0.0,
                        outstandingReceivable = (existingContact?.outstandingReceivable ?: 0.0) + if (s.type == LendBorrowType.LENT) amount else 0.0,
                        outstandingPayable = (existingContact?.outstandingPayable ?: 0.0) + if (s.type == LendBorrowType.BORROWED) amount else 0.0
                    )
                )

                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    private fun searchAndroidContacts(query: String) {
        viewModelScope.launch {
            repository.searchContacts(query)
                .take(1)
                .collect { dbContacts ->
                    val suggestions = dbContacts.map {
                        ContactSuggestion(
                            name = it.name,
                            phone = it.phone,
                            photoUri = it.photoUri
                        )
                    }
                    _state.update { it.copy(contactSuggestions = suggestions) }
                }
        }
    }
}
