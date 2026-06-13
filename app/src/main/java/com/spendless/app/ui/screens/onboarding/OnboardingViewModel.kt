package com.spendless.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.entities.Budget
import com.spendless.app.core.data.datastore.PreferencesDataStore
import com.spendless.app.worker.SmsHistoryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val smsPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val budgetAmount: String = "10000",
    val resetDay: Int = 1,
    val importProgress: Int = 0,
    val importedCount: Int = 0,
    val totalSmsCount: Int = 0,
    val importComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val budgetDao: BudgetDao,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onSmsPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(smsPermissionGranted = granted) }
        viewModelScope.launch {
            preferencesDataStore.setSmsAutoImportEnabled(granted)
        }
    }

    fun setSmsAutoImportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setSmsAutoImportEnabled(enabled)
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun setBudgetAmount(amount: String) {
        _uiState.update { it.copy(budgetAmount = amount.filter { c -> c.isDigit() }) }
    }

    fun setResetDay(day: Int) {
        _uiState.update { it.copy(resetDay = day) }
        viewModelScope.launch {
            preferencesDataStore.setResetDay(day)
        }
    }

    suspend fun saveBudget() {
        val amount = _uiState.value.budgetAmount.toDoubleOrNull() ?: 10000.0
        val resetDay = _uiState.value.resetDay

        // Deactivate existing budgets
        budgetDao.deactivateAllBudgets()

        // Create new budget
        val budget = Budget(
            totalBudget = amount,
            resetDay = resetDay,
            isActive = true
        )
        val budgetId = budgetDao.insertBudget(budget)
        preferencesDataStore.setActiveBudgetId(budgetId)
    }

    fun startHistoricalImport(startDateMs: Long = 0L) {
        val request = OneTimeWorkRequestBuilder<SmsHistoryWorker>()
            .setInputData(workDataOf(SmsHistoryWorker.KEY_START_DATE to startDateMs))
            .setConstraints(Constraints.NONE)
            .build()

        workManager.enqueueUniqueWork(
            SmsHistoryWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )

        // Observe work progress
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(SmsHistoryWorker.WORK_NAME)
                .collect { workInfoList ->
                    val workInfo = workInfoList.firstOrNull() ?: return@collect
                    
                    val isFinished = workInfo.state == WorkInfo.State.SUCCEEDED
                    val sourceData = if (isFinished) workInfo.outputData else workInfo.progress

                    val progress = sourceData.getInt(SmsHistoryWorker.KEY_PROGRESS, 0)
                    val imported = sourceData.getInt(SmsHistoryWorker.KEY_IMPORTED, 0)
                    val total = sourceData.getInt(SmsHistoryWorker.KEY_TOTAL, 0)

                    _uiState.update {
                        it.copy(
                            importProgress = if (isFinished) 100 else progress,
                            importedCount = imported,
                            totalSmsCount = total,
                            importComplete = isFinished
                        )
                    }
                }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesDataStore.setOnboardingComplete(true)
        }
    }
}
