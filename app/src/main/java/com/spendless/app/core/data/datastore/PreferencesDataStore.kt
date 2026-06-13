package com.spendless.app.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "spendless_preferences"
)

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Onboarding
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_SMS_PERMISSION_GRANTED = booleanPreferencesKey("sms_permission_granted")
        val KEY_SMS_AUTO_IMPORT_ENABLED = booleanPreferencesKey("sms_auto_import_enabled")
        val KEY_HISTORICAL_SCAN_DONE = booleanPreferencesKey("historical_scan_done")

        // Theme
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "dark", "light", "system"
        val KEY_THEME_STYLE = stringPreferencesKey("theme_style") // "standard", "glass"

        // Budget
        val KEY_ACTIVE_BUDGET_ID = longPreferencesKey("active_budget_id")
        val KEY_DEFAULT_RESET_DAY = intPreferencesKey("default_reset_day")

        // App lock
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")

        // App name
        val KEY_APP_NAME = stringPreferencesKey("app_name")

        // App icon
        val KEY_ACTIVE_ICON = stringPreferencesKey("active_icon") // alias name

        // Widget last update
        val KEY_WIDGET_LAST_UPDATE = longPreferencesKey("widget_last_update")

        // Notifications
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_NOTIFIED_50 = booleanPreferencesKey("notified_50")
        val KEY_NOTIFIED_75 = booleanPreferencesKey("notified_75")
        val KEY_NOTIFIED_90 = booleanPreferencesKey("notified_90")
        val KEY_NOTIFIED_100 = booleanPreferencesKey("notified_100")
        val KEY_LAST_NOTIFICATION_CYCLE = longPreferencesKey("last_notification_cycle")
    }

    private fun <T> Flow<Preferences>.safeMap(default: T, transform: (Preferences) -> T): Flow<T> =
        catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs -> transform(prefs) }

    // ── Onboarding ─────────────────────────────────────────────────────────────

    val isOnboardingComplete: Flow<Boolean> =
        dataStore.data.safeMap(false) { it[KEY_ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    val isHistoricalScanDone: Flow<Boolean> =
        dataStore.data.safeMap(false) { it[KEY_HISTORICAL_SCAN_DONE] ?: false }

    suspend fun setHistoricalScanDone(done: Boolean) {
        dataStore.edit { it[KEY_HISTORICAL_SCAN_DONE] = done }
    }

    val smsAutoImportEnabled: Flow<Boolean> =
        dataStore.data.safeMap(false) { it[KEY_SMS_AUTO_IMPORT_ENABLED] ?: false }

    suspend fun setSmsAutoImportEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SMS_AUTO_IMPORT_ENABLED] = enabled }
    }

    // ── Theme ──────────────────────────────────────────────────────────────────

    val themeMode: Flow<String> =
        dataStore.data.safeMap("system") { it[KEY_THEME_MODE] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    val themeStyle: Flow<String> =
        dataStore.data.safeMap("standard") { it[KEY_THEME_STYLE] ?: "standard" }

    suspend fun setThemeStyle(style: String) {
        dataStore.edit { it[KEY_THEME_STYLE] = style }
    }

    // ── Budget ─────────────────────────────────────────────────────────────────

    val activeBudgetId: Flow<Long> =
        dataStore.data.safeMap(-1L) { it[KEY_ACTIVE_BUDGET_ID] ?: -1L }

    suspend fun setActiveBudgetId(id: Long) {
        dataStore.edit { it[KEY_ACTIVE_BUDGET_ID] = id }
    }

    val resetDay: Flow<Int> =
        dataStore.data.safeMap(1) { it[KEY_DEFAULT_RESET_DAY] ?: 1 }

    suspend fun setResetDay(day: Int) {
        dataStore.edit { it[KEY_DEFAULT_RESET_DAY] = day }
    }

    // ── App lock ───────────────────────────────────────────────────────────────

    val isBiometricEnabled: Flow<Boolean> =
        dataStore.data.safeMap(false) { it[KEY_BIOMETRIC_ENABLED] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    // ── App name & icon ────────────────────────────────────────────────────────

    val appName: Flow<String> =
        dataStore.data.safeMap("SpendLess") { it[KEY_APP_NAME] ?: "SpendLess" }

    suspend fun setAppName(name: String) {
        dataStore.edit { it[KEY_APP_NAME] = name }
    }

    val activeIcon: Flow<String> =
        dataStore.data.safeMap("default") { it[KEY_ACTIVE_ICON] ?: "default" }

    suspend fun setActiveIcon(iconAlias: String) {
        dataStore.edit { it[KEY_ACTIVE_ICON] = iconAlias }
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    val notificationsEnabled: Flow<Boolean> =
        dataStore.data.safeMap(true) { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun resetNotificationFlags(cycleId: Long) {
        dataStore.edit {
            it[KEY_NOTIFIED_50] = false
            it[KEY_NOTIFIED_75] = false
            it[KEY_NOTIFIED_90] = false
            it[KEY_NOTIFIED_100] = false
            it[KEY_LAST_NOTIFICATION_CYCLE] = cycleId
        }
    }

    suspend fun markNotificationSent(threshold: Int) {
        dataStore.edit {
            when (threshold) {
                50 -> it[KEY_NOTIFIED_50] = true
                75 -> it[KEY_NOTIFIED_75] = true
                90 -> it[KEY_NOTIFIED_90] = true
                100 -> it[KEY_NOTIFIED_100] = true
            }
        }
    }

    suspend fun getNotificationState(): NotificationState {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        return NotificationState(
            notified50 = prefs[KEY_NOTIFIED_50] ?: false,
            notified75 = prefs[KEY_NOTIFIED_75] ?: false,
            notified90 = prefs[KEY_NOTIFIED_90] ?: false,
            notified100 = prefs[KEY_NOTIFIED_100] ?: false,
            lastCycleId = prefs[KEY_LAST_NOTIFICATION_CYCLE] ?: -1L
        )
    }
}

data class NotificationState(
    val notified50: Boolean,
    val notified75: Boolean,
    val notified90: Boolean,
    val notified100: Boolean,
    val lastCycleId: Long
)
