package tech.salev.optimum.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Theme preference enum stored as a String in DataStore. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        val KEY_DAY_START_TIME = stringPreferencesKey("day_start_time")
        val KEY_DAY_END_TIME = stringPreferencesKey("day_end_time")
        val KEY_CUSTOM_RINGTONE = stringPreferencesKey("custom_ringtone")
        val KEY_LAST_NOTIFICATION_ID = intPreferencesKey("last_notification_id")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_LONG_RINGTONE = booleanPreferencesKey("long_ringtone")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        const val DEFAULT_INTERVAL = 30
        const val DEFAULT_START_TIME = "06:00"
        const val DEFAULT_END_TIME = "23:30"
        private const val DEFAULT_NOTIFICATION_ID = 1001
    }

    val intervalMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_INTERVAL_MINUTES] ?: DEFAULT_INTERVAL
    }

    val dayStartTime: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_DAY_START_TIME] ?: DEFAULT_START_TIME
    }

    val dayEndTime: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_DAY_END_TIME] ?: DEFAULT_END_TIME
    }

    val customRingtone: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_RINGTONE]
    }

    val isNotificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val isLongRingtoneEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_LONG_RINGTONE] ?: false
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = true }
    }

    suspend fun setIntervalMinutes(minutes: Int) {
        dataStore.edit { it[KEY_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setDayStartTime(time: String) {
        dataStore.edit { it[KEY_DAY_START_TIME] = time }
    }

    suspend fun setDayEndTime(time: String) {
        dataStore.edit { it[KEY_DAY_END_TIME] = time }
    }

    suspend fun setStartEndTime(startTime: String, endTime: String) {
        dataStore.edit {
            it[KEY_DAY_START_TIME] = startTime
            it[KEY_DAY_END_TIME] = endTime
        }
    }

    suspend fun setCustomRingtone(uri: String?) {
        dataStore.edit {
            if (uri != null) it[KEY_CUSTOM_RINGTONE] = uri
            else it.remove(KEY_CUSTOM_RINGTONE)
        }
    }

    suspend fun saveLastNotificationId(id: Int) {
        dataStore.edit { it[KEY_LAST_NOTIFICATION_ID] = id }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLongRingtoneEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_LONG_RINGTONE] = enabled }
    }

    /**
     * Returns the last shown notification ID from DataStore.
     * Uses .first() for a clean single-value read.
     */
    suspend fun getLastNotificationId(): Int =
        dataStore.data.map { it[KEY_LAST_NOTIFICATION_ID] ?: DEFAULT_NOTIFICATION_ID }.first()

}
