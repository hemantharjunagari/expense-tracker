package com.spendless.app.lend.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.data.database.entities.LendBorrowContact
import com.spendless.app.core.data.database.entities.LendBorrowRecord
import com.spendless.app.lend.repository.LendBorrowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactProfileState(
    val contact: LendBorrowContact? = null,
    val records: List<LendBorrowRecord> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ContactProfileViewModel @Inject constructor(
    private val repository: LendBorrowRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle["phone"])
    private val _state = MutableStateFlow(ContactProfileState())
    val state: StateFlow<ContactProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecordsForContact(phone).collectLatest { records ->
                val contact = repository.getContactByPhone(phone)
                _state.update {
                    it.copy(
                        contact = contact,
                        records = records,
                        isLoading = false
                    )
                }
            }
        }
    }
}
