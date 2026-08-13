package tech.salev.optimum.ui.viewmodel

import tech.salev.optimum.data.repository.ThemeMode

data class SettingsUiState(
    val intervalMinutes: Int = 30,
    val dayStartTime: String = "08:00",
    val dayEndTime: String = "23:59",
    val isNotificationsEnabled: Boolean = true,
    val isLongRingtoneEnabled: Boolean = false,
    val customRingtone: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOnboardingCompleted: Boolean = true
)

sealed interface SettingsEvent {
    data class SetInterval(val minutes: Int) : SettingsEvent
    data class SetStartEndTime(val start: String, val end: String) : SettingsEvent
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsEvent
    data class SetLongRingtoneEnabled(val enabled: Boolean) : SettingsEvent
    data class SetCustomRingtone(val uri: String?) : SettingsEvent
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent
    object CompleteOnboarding : SettingsEvent
}
