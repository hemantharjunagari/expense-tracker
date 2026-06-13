package com.spendless.app.lend.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.data.database.dao.ContactAggregate
import com.spendless.app.core.data.database.dao.LendBorrowSummary
import com.spendless.app.core.data.database.entities.LendBorrowRecord
import com.spendless.app.core.data.database.entities.LendBorrowType
import com.spendless.app.lend.repository.LendBorrowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LendBorrowDashboardState(
    val isLoading: Boolean = true,
    val summary: LendBorrowSummary = LendBorrowSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    val overdueRecords: List<LendBorrowRecord> = emptyList(),
    val upcomingRecords: List<LendBorrowRecord> = emptyList(),
    val topContacts: List<ContactAggregate> = emptyList(),
    val totalRecovered: Double = 0.0,
    val recoveryRate: Float = 0f,
    val activeCount: Int = 0
)

@HiltViewModel
class LendBorrowDashboardViewModel @Inject constructor(
    private val repository: LendBorrowRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LendBorrowDashboardState())
    val state: StateFlow<LendBorrowDashboardState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            combine(
                repository.getSummary(),
                repository.getUpcomingAndOverdue(),
                repository.getTopContacts(),
                repository.getTotalRecovered(),
                repository.getActiveRecordCount()
            ) { summary, upcomingOverdue, contacts, recovered, activeCount ->
                val overdue = upcomingOverdue.filter {
                    it.status.name.contains("OVERDUE") || it.status.name == "DUE_TODAY"
                }
                val upcoming = upcomingOverdue.filter {
                    it.status.name == "DUE_SOON" || it.status.name == "ACTIVE"
                }

                val recoveryRate = if (summary.totalLent > 0)
                    ((recovered / summary.totalLent) * 100f).toFloat().coerceIn(0f, 100f)
                else 0f

                LendBorrowDashboardState(
                    isLoading = false,
                    summary = summary,
                    overdueRecords = overdue,
                    upcomingRecords = upcoming.take(5),
                    topContacts = contacts,
                    totalRecovered = recovered,
                    recoveryRate = recoveryRate,
                    activeCount = activeCount
                )
            }.collect { _state.value = it }
        }
    }

    fun syncContacts(context: android.content.Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.syncSystemContacts(context)
            _state.update { it.copy(isLoading = false) }
        }
    }
}
