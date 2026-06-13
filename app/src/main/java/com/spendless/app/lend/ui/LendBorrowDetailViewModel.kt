package com.spendless.app.lend.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.data.database.entities.*
import com.spendless.app.lend.repository.LendBorrowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordDetailState(
    val record: LendBorrowRecord? = null,
    val payments: List<LendBorrowPayment> = emptyList(),
    val isLoading: Boolean = true,
    val showPaymentSheet: Boolean = false,
    val showExtendSheet: Boolean = false,
    val paymentInput: String = "",
    val paymentNotes: String = "",
    val extendDate: Long? = null,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class LendBorrowDetailViewModel @Inject constructor(
    private val repository: LendBorrowRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recordId: Long = checkNotNull(savedStateHandle["recordId"])
    private val _state = MutableStateFlow(RecordDetailState())
    val state: StateFlow<RecordDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getRecord(recordId),
                repository.getPaymentsForRecord(recordId)
            ) { record, payments ->
                _state.update {
                    it.copy(
                        record = record,
                        payments = payments,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun showPaymentSheet() = _state.update { it.copy(showPaymentSheet = true) }
    fun hidePaymentSheet() = _state.update { it.copy(showPaymentSheet = false, paymentInput = "", paymentNotes = "") }
    fun setPaymentAmount(v: String) = _state.update { it.copy(paymentInput = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setPaymentNotes(v: String) = _state.update { it.copy(paymentNotes = v) }

    fun showExtendSheet() = _state.update { it.copy(showExtendSheet = true) }
    fun hideExtendSheet() = _state.update { it.copy(showExtendSheet = false) }
    fun setExtendDate(ms: Long) = _state.update { it.copy(extendDate = ms) }

    fun addPartialPayment() {
        val amount = _state.value.paymentInput.toDoubleOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.addPartialPayment(recordId, amount, _state.value.paymentNotes.ifBlank { null })
            _state.update { it.copy(isSaving = false, showPaymentSheet = false, paymentInput = "", paymentNotes = "") }
        }
    }

    fun markAsPaid() {
        viewModelScope.launch {
            repository.markAsPaid(recordId)
        }
    }

    fun extendDueDate() {
        val date = _state.value.extendDate ?: return
        viewModelScope.launch {
            repository.extendDueDate(recordId, date)
            _state.update { it.copy(showExtendSheet = false) }
        }
    }

    fun deleteRecord() {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            _state.update { it.copy(isDeleted = true) }
        }
    }
}
