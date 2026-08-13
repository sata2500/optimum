package tech.salev.optimum.ui.viewmodel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.ui.model.MultiDayRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val categories: ImmutableList<Category> = persistentListOf(),
    val activities: ImmutableList<ActivityItem> = persistentListOf(),
    val currentDateStr: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val intervalMinutes: Int = 30,
    val unloggedPastSlots: ImmutableList<Pair<String, String>> = persistentListOf(),
    val daysToView: Int = 1,
    val errorMessage: String? = null,
    val dailyMergedBlocks: ImmutableList<MergedTimeBlock> = persistentListOf(),
    val multiDayRows: ImmutableList<MultiDayRow> = persistentListOf(),
    val selectedFilterCategoryId: Set<Long> = emptySet(),
    val selectedFilterActivityId: Set<Long> = emptySet()
)

sealed interface HomeEvent {
    data class SetDaysToView(val days: Int) : HomeEvent
    data class SetSelectedDate(val date: String) : HomeEvent
    data class SetCategoryFilter(val ids: Set<Long>) : HomeEvent
    data class SetActivityFilter(val ids: Set<Long>) : HomeEvent
    data class LogTimeSlot(
        val date: String,
        val startTime: String,
        val endTime: String,
        val categoryId: Long,
        val activityId: Long,
        val note: String,
        val logId: Long
    ) : HomeEvent
    data class LogMultipleSlots(val logs: List<TimeSlotLog>) : HomeEvent
    data class DeleteTimeLog(val date: String, val startTime: String) : HomeEvent
    data object ClearError : HomeEvent
}
