package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.data.repository.OptimumRepository
import tech.salev.optimum.data.repository.SettingsRepository
import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.ui.model.MultiDayRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OptimumRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ── Streams that were in OptimumViewModel ──────────────────────────────────────────────────────────────
    
    private val intervalMinutes = settingsRepository.intervalMinutes
    private val dayStartTime = settingsRepository.dayStartTime
    private val dayEndTime = settingsRepository.dayEndTime

    private val currentDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    private val daysToView = MutableStateFlow(1)
    private val selectedFilterCategoryId = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedFilterActivityId = MutableStateFlow<Set<Long>>(emptySet())
    private val errorMessage = MutableStateFlow<String?>(null)

    private val categories = repository.allCategories.map { it.toImmutableList() }
    private val activities = repository.allActivities.map { it.toImmutableList() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentLogs = currentDate
        .flatMapLatest { date -> repository.getLogsForDate(date) }
        .map { it.toImmutableList() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val multiDayLogs = combine(currentDate, daysToView) { date, days -> date to days }
        .flatMapLatest { (date, days) ->
            val end = LocalDate.parse(date)
            val start = end.minusDays(days.toLong() - 1)
            repository.getLogsBetweenDates(
                start.format(DateTimeFormatter.ISO_LOCAL_DATE),
                end.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        }
        .map { it.toImmutableList() }

    private val timeSlots = combine(intervalMinutes, dayStartTime, dayEndTime) { interval, start, end ->
        TimeSlotBuilder.build(interval, start, end)
    }

    private val dailyMergedBlocks = combine(
        combine(timeSlots, currentLogs, categories, activities) { s, l, c, a ->
            DailyBlockInputs(slots = s, logs = l, cats = c, acts = a, dateStr = "", catFilter = emptySet(), actFilter = emptySet())
        },
        combine(currentDate, selectedFilterCategoryId, selectedFilterActivityId) { d, cF, aF ->
            Triple(d, cF, aF)
        }
    ) { partialInput, filters ->
        val inputs = partialInput.copy(
            dateStr = filters.first,
            catFilter = filters.second,
            actFilter = filters.third
        )
        DailyMergedBlocksBuilder.build(inputs)
    }

    private val multiDayRows = combine(
        combine(timeSlots, multiDayLogs, categories, activities) { s, l, c, a ->
            MultiDayInputs(slots = s, logs = l, cats = c, acts = a, dateStr = "", days = 1, catFilter = emptySet(), actFilter = emptySet())
        },
        combine(currentDate, daysToView, selectedFilterCategoryId, selectedFilterActivityId) { d, days, cF, aF ->
            listOf(d, days, cF, aF) // a simple wrapper
        }
    ) { partialInput, filters ->
        val inputs = partialInput.copy(
            dateStr = filters[0] as String,
            days = filters[1] as Int,
            catFilter = filters[2] as Set<Long>,
            actFilter = filters[3] as Set<Long>
        )
        MultiDayRowsBuilder.build(inputs)
    }

    private val unloggedPastSlots = combine(
        currentLogs, intervalMinutes, dayStartTime, dayEndTime
    ) { logs, interval, start, end ->
        UnloggedSlotsBuilder.build(logs, interval, start, end)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(categories, activities, intervalMinutes) { cats, acts, interval -> Triple(cats, acts, interval) },
        combine(unloggedPastSlots, dailyMergedBlocks, multiDayRows) { unlogged, daily, multi -> Triple(unlogged, daily, multi) },
        combine(currentDate, daysToView, errorMessage) { date, days, err -> Triple(date, days, err) },
        combine(selectedFilterCategoryId, selectedFilterActivityId) { catF, actF -> Pair(catF, actF) }
    ) { f1, f2, f3, f4 ->
        HomeUiState(
            categories = f1.first,
            activities = f1.second,
            intervalMinutes = f1.third,
            unloggedPastSlots = f2.first,
            dailyMergedBlocks = f2.second,
            multiDayRows = f2.third,
            currentDateStr = f3.first,
            daysToView = f3.second,
            errorMessage = f3.third,
            selectedFilterCategoryId = f4.first,
            selectedFilterActivityId = f4.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SetDaysToView -> daysToView.value = event.days
            is HomeEvent.SetSelectedDate -> currentDate.value = event.date
            is HomeEvent.SetCategoryFilter -> selectedFilterCategoryId.value = event.ids
            is HomeEvent.SetActivityFilter -> selectedFilterActivityId.value = event.ids
            is HomeEvent.LogTimeSlot -> logTimeSlot(event)
            is HomeEvent.LogMultipleSlots -> logMultipleSlots(event.logs)
            is HomeEvent.DeleteTimeLog -> deleteTimeLog(event.date, event.startTime)
            HomeEvent.ClearError -> errorMessage.value = null
        }
    }

    private fun logTimeSlot(event: HomeEvent.LogTimeSlot) {
        viewModelScope.launch {
            runCatching {
                repository.deleteOverlappingLogs(event.date, event.startTime, event.endTime, event.logId)
                repository.insertOrUpdateLog(
                    TimeSlotLog(
                        id = event.logId, date = event.date, startTime = event.startTime,
                        endTime = event.endTime, categoryId = event.categoryId,
                        activityId = event.activityId, note = event.note
                    )
                )
            }.onFailure { handleError(it) }
        }
    }

    private fun logMultipleSlots(logs: List<TimeSlotLog>) {
        viewModelScope.launch {
            runCatching {
                logs.forEach { log ->
                    repository.deleteOverlappingLogs(log.date, log.startTime, log.endTime, log.id)
                    repository.insertOrUpdateLog(log)
                }
            }.onFailure { handleError(it) }
        }
    }

    private fun deleteTimeLog(date: String, startTime: String) {
        viewModelScope.launch {
            runCatching { repository.deleteLogByDateAndStartTime(date, startTime) }
                .onFailure { handleError(it) }
        }
    }

    private fun handleError(t: Throwable) {
        errorMessage.value = "Bir hata oluştu: ${t.localizedMessage}"
    }
}
