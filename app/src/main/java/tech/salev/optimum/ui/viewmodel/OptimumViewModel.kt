package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.data.repository.OptimumRepository
import tech.salev.optimum.data.repository.SettingsRepository
import tech.salev.optimum.data.repository.ThemeMode
import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.ui.model.MultiDayRow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Root ViewModel shared across all screens via the NavGraph.
 *
 * Responsibilities:
 *  - Shared entity streams: [categories], [activities]
 *  - Home-screen grid state: date selection, filters, derived blocks
 *  - CRUD operations: category, activity, time-slot log, evaluation
 *  - Quick-log trigger (fired from notification action)
 *
 * Settings state lives in [SettingsViewModel] and is consumed only by
 * [SettingsScreen], keeping this class focused on data and grid logic.
 *
 * Builder logic lives in [ViewModelBuilders.kt] — pure Kotlin, unit-testable.
 */
@HiltViewModel
class OptimumViewModel @Inject constructor(
    private val repository: OptimumRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ── Error ────────────────────────────────────────────────────────────────

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }
    private fun handleError(t: Throwable) {
        _errorMessage.value = "Bir hata oluştu: ${t.localizedMessage}"
    }

    // ── Cross-cutting settings (needed by MainActivity / NavGraph) ───────────
    // Only values consumed outside SettingsScreen live here.

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val isOnboardingCompleted: StateFlow<Boolean> = settingsRepository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted() }
    }

    // ── Quick-log trigger (notification action → MainActivity) ───────────────

    val quickLogTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun triggerQuickLog() { quickLogTrigger.tryEmit(Unit) }

    // ── Navigation / filter state ────────────────────────────────────────────

    private val _currentDate = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    fun setSelectedDate(date: String) { _currentDate.value = date }

    // ── Settings streams (grid calculations depend on these) ─────────────────

    val intervalMinutes: StateFlow<Int> = settingsRepository.intervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_INTERVAL)

    val dayStartTime: StateFlow<String> = settingsRepository.dayStartTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_START_TIME)

    val dayEndTime: StateFlow<String> = settingsRepository.dayEndTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_END_TIME)

    // ── Entity streams ───────────────────────────────────────────────────────

    val categories: StateFlow<ImmutableList<Category>> = repository.allCategories
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val activities: StateFlow<ImmutableList<ActivityItem>> = repository.allActivities
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentLogs: StateFlow<ImmutableList<TimeSlotLog>> = currentDate
        .flatMapLatest { date -> repository.getLogsForDate(date) }
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    // ── Daily Evaluation ─────────────────────────────────────────────────────
    // Moved to EvaluationViewModel

    // ── Daily Evaluation ─────────────────────────────────────────────────────
    // Moved to EvaluationViewModel

    // ── Category & Activity CRUD (Forwarded for compatibility) ───────────────

    fun addCategory(name: String, abbreviation: String, color: String, isCore: Boolean = true) {
        viewModelScope.launch {
            runCatching {
                repository.insertCategory(Category(name = name, code = abbreviation, colorHex = color, isProductive = isCore))
            }.onFailure { handleError(it) }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            runCatching {
                repository.updateCategory(category)
            }.onFailure { handleError(it) }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            runCatching {
                repository.deleteCategory(category)
            }.onFailure { handleError(it) }
        }
    }

    fun addActivity(categoryId: Long, name: String, description: String = "", shortCode: String = "", colorHex: String = "#FFD700") {
        viewModelScope.launch {
            runCatching {
                val nextNumber = repository.getMaxActivityNumberForCategory(categoryId)
                val finalShortCode = if (shortCode.isNotBlank()) shortCode else name.trim().take(1).lowercase()
                repository.insertActivity(
                    ActivityItem(categoryId = categoryId, name = name,
                                 activityNumber = nextNumber, description = description,
                                 shortCode = finalShortCode, colorHex = colorHex)
                )
            }.onFailure { handleError(it) }
        }
    }

    fun updateActivity(activity: ActivityItem) {
        viewModelScope.launch {
            runCatching {
                repository.updateActivity(activity)
            }.onFailure { handleError(it) }
        }
    }

    fun deleteActivity(activity: ActivityItem) {
        viewModelScope.launch {
            runCatching {
                repository.deleteActivity(activity)
            }.onFailure { handleError(it) }
        }
    }

    fun restoreFullBackup(backupData: tech.salev.optimum.util.OptimumBackupData) {
        viewModelScope.launch {
            runCatching {
                repository.restoreFullBackup(
                    categories = backupData.categories,
                    activities = backupData.activities,
                    logs = backupData.timeLogs,
                    evaluations = backupData.evaluations
                )
            }.onFailure { handleError(it) }
        }
    }
}
