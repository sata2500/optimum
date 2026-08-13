package tech.salev.optimum.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.salev.optimum.data.repository.SettingsRepository
import tech.salev.optimum.data.repository.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val optimumRepository: tech.salev.optimum.data.repository.OptimumRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.intervalMinutes,
        settingsRepository.dayStartTime,
        settingsRepository.dayEndTime,
        settingsRepository.isNotificationsEnabled,
        settingsRepository.isLongRingtoneEnabled
    ) { interval, start, end, notif, longRing ->
        SettingsUiState(
            intervalMinutes = interval,
            dayStartTime = start,
            dayEndTime = end,
            isNotificationsEnabled = notif,
            isLongRingtoneEnabled = longRing
        )
    }.combine(settingsRepository.customRingtone) { state, ringtone ->
        state.copy(customRingtone = ringtone)
    }.combine(settingsRepository.themeMode) { state, theme ->
        state.copy(themeMode = theme)
    }.combine(settingsRepository.isOnboardingCompleted) { state, onboarding ->
        state.copy(isOnboardingCompleted = onboarding)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetInterval -> setInterval(event.minutes)
            is SettingsEvent.SetStartEndTime -> setStartEndTime(event.start, event.end)
            is SettingsEvent.SetNotificationsEnabled -> setNotificationsEnabled(event.enabled)
            is SettingsEvent.SetLongRingtoneEnabled -> setLongRingtoneEnabled(event.enabled)
            is SettingsEvent.SetCustomRingtone -> setCustomRingtone(event.uri)
            is SettingsEvent.SetThemeMode -> setThemeMode(event.mode)
            is SettingsEvent.CompleteOnboarding -> completeOnboarding()
        }
    }

    private fun setInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setIntervalMinutes(minutes)
            optimumRepository.normalizeTimeSlotLogs(minutes)
            tech.salev.optimum.service.ReminderScheduler.schedulePeriodicReminder(context, minutes)
        }
    }

    private fun setStartEndTime(startTime: String, endTime: String) {
        viewModelScope.launch { settingsRepository.setStartEndTime(startTime, endTime) }
    }

    private fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    private fun setLongRingtoneEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLongRingtoneEnabled(enabled) }
    }

    private fun setCustomRingtone(uri: String?) {
        viewModelScope.launch { settingsRepository.setCustomRingtone(uri) }
    }

    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    private fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted() }
    }

    suspend fun getLastNotificationId(): Int = settingsRepository.getLastNotificationId()
}
