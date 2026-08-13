package tech.salev.optimum.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.repository.SettingsRepository

/**
 * Consolidated state snapshot for the daily time-slot grid screen.
 *
 * Wrapping all grid inputs in a single @Immutable data class lets Compose
 * skip recomposition when the reference hasn't changed, and avoids the
 * error-prone 7-parameter combine() anti-pattern in the ViewModel.
 */
@Immutable
data class HomeUiState(
    val currentDate: String = java.time.LocalDate.now()
        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
    val daysToView: Int = 1,
    val intervalMinutes: Int = SettingsRepository.DEFAULT_INTERVAL,
    val dayStartTime: String = SettingsRepository.DEFAULT_START_TIME,
    val dayEndTime: String = SettingsRepository.DEFAULT_END_TIME,
    val categories: ImmutableList<Category> = persistentListOf(),
    val activities: ImmutableList<ActivityItem> = persistentListOf(),
    val selectedFilterCategoryIds: ImmutableList<Long> = persistentListOf(),
    val selectedFilterActivityIds: ImmutableList<Long> = persistentListOf(),
)
