package com.spendless.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.spendless.app.core.data.datastore.PreferencesDataStore
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.worker.SmsHistoryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "system",
    val themeStyle: String = "standard",
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val appName: String = "SpendLess",
    val activeIcon: String = "default",
    val smsAutoImportEnabled: Boolean = false,
    val isRescanning: Boolean = false,
    val rescanProgress: Int = 0,
    val currentBudget: Double = 0.0,
    val resetDay: Int = 1
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val budgetDao: BudgetDao,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesDataStore.themeMode,
                preferencesDataStore.themeStyle,
                preferencesDataStore.isBiometricEnabled,
                preferencesDataStore.notificationsEnabled,
                preferencesDataStore.appName,
                preferencesDataStore.activeIcon,
                preferencesDataStore.smsAutoImportEnabled,
                budgetDao.getActiveBudget()
            ) { array ->
                val theme = array[0] as String
                val style = array[1] as String
                val biometric = array[2] as Boolean
                val notifications = array[3] as Boolean
                val name = array[4] as String
                val icon = array[5] as String
                val smsAutoImport = array[6] as Boolean
                val activeBudget = array[7] as? com.spendless.app.core.data.database.entities.Budget
                SettingsUiState(
                    themeMode = theme,
                    themeStyle = style,
                    biometricEnabled = biometric,
                    notificationsEnabled = notifications,
                    appName = name,
                    activeIcon = icon,
                    smsAutoImportEnabled = smsAutoImport,
                    currentBudget = activeBudget?.totalBudget ?: 0.0,
                    resetDay = activeBudget?.resetDay ?: 1
                )
            }.collect { state ->
                _uiState.update {
                    it.copy(
                        themeMode = state.themeMode,
                        themeStyle = state.themeStyle,
                        biometricEnabled = state.biometricEnabled,
                        notificationsEnabled = state.notificationsEnabled,
                        appName = state.appName,
                        activeIcon = state.activeIcon,
                        smsAutoImportEnabled = state.smsAutoImportEnabled,
                        currentBudget = state.currentBudget,
                        resetDay = state.resetDay
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesDataStore.setThemeMode(mode)
        }
    }

    fun setThemeStyle(style: String) {
        viewModelScope.launch {
            preferencesDataStore.setThemeStyle(style)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setBiometricEnabled(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun setAppName(name: String) {
        viewModelScope.launch {
            preferencesDataStore.setAppName(name.ifBlank { "SpendLess" })
        }
    }

    fun setActiveIcon(iconAlias: String) {
        viewModelScope.launch {
            preferencesDataStore.setActiveIcon(iconAlias)
        }
    }

    fun setSmsAutoImportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setSmsAutoImportEnabled(enabled)
        }
    }

    fun startRescan(startDateMs: Long = 0L) {
        val request = OneTimeWorkRequestBuilder<SmsHistoryWorker>()
            .setInputData(workDataOf(SmsHistoryWorker.KEY_START_DATE to startDateMs))
            .setConstraints(Constraints.NONE)
            .build()

        workManager.enqueueUniqueWork(
            SmsHistoryWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )

        _uiState.update { it.copy(isRescanning = true) }

        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(SmsHistoryWorker.WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    val progress = info.progress.getInt(SmsHistoryWorker.KEY_PROGRESS, 0)
                    val isDone = info.state in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED)
                    _uiState.update {
                        it.copy(
                            isRescanning = !isDone,
                            rescanProgress = if (isDone) 0 else progress
                        )
                    }
                }
        }
    }
}
