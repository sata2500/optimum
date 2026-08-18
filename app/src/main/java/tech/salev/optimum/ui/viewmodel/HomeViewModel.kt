package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.data.repository.OptimumRepository
import tech.salev.optimum.data.repository.SettingsRepository
import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.ui.model.MultiDayRow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OptimumRepository,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: tech.salev.optimum.data.repository.SyncRepository
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

    // ── combine() max 5 akış typed overload sınırı — ara sarmalayıcılar ile çözüm ──────────────────────────
    // Kotlin Flow combine() 6+ akış için vararg kullanır ve Array<Any!> döner (tip güvenli değil).
    // Çözüm: ara data class sarmalayıcılarla 2 aşamalı combine zinciri.
    // flowOn(Default): ağır hesaplamalar Main thread'i bloke etmez.
    // distinctUntilChanged(): aynı state tekrar emit edilmez.

    // Aşama 1a: temel slot/log verisi + filtre context'i
    private data class DailyInputBundle(
        val slots: ImmutableList<Triple<String, String, LocalTime>>,
        val logs: ImmutableList<TimeSlotLog>,
        val cats: ImmutableList<Category>,
        val acts: ImmutableList<ActivityItem>,
        val dateStr: String
    )

    private val dailyBundle = combine(timeSlots, currentLogs, categories, activities, currentDate) {
        slots, logs, cats, acts, dateStr -> DailyInputBundle(slots, logs, cats, acts, dateStr)
    }

    // Aşama 1b: bundle + filtreler → nihai hesaplama (2+1 = 3 akış, sınır içinde)
    private val dailyMergedBlocks = combine(
        dailyBundle, selectedFilterCategoryId, selectedFilterActivityId
    ) { bundle, catFilter, actFilter ->
        DailyMergedBlocksBuilder.build(
            DailyBlockInputs(
                slots = bundle.slots,
                logs = bundle.logs,
                cats = bundle.cats,
                acts = bundle.acts,
                dateStr = bundle.dateStr,
                catFilter = catFilter,
                actFilter = actFilter,
                nowTime = LocalTime.now()
            )
        )
    }.flowOn(Dispatchers.Default).distinctUntilChanged()

    // Aşama 2a: multi-day slot/log verisi + context
    private data class MultiDayInputBundle(
        val slots: ImmutableList<Triple<String, String, LocalTime>>,
        val logs: ImmutableList<TimeSlotLog>,
        val cats: ImmutableList<Category>,
        val acts: ImmutableList<ActivityItem>,
        val dateStr: String
    )

    private val multiDayBundle = combine(timeSlots, multiDayLogs, categories, activities, currentDate) {
        slots, logs, cats, acts, dateStr -> MultiDayInputBundle(slots, logs, cats, acts, dateStr)
    }

    // Aşama 2b: bundle + days + filtre → nihai hesaplama
    private data class MultiDayFilterState(val days: Int, val catFilter: Set<Long>, val actFilter: Set<Long>)

    private val multiDayFilterState = combine(
        daysToView, selectedFilterCategoryId, selectedFilterActivityId
    ) { days, catF, actF -> MultiDayFilterState(days, catF, actF) }

    private val multiDayRows = combine(multiDayBundle, multiDayFilterState) { bundle, filter ->
        MultiDayRowsBuilder.build(
            MultiDayInputs(
                slots = bundle.slots,
                logs = bundle.logs,
                cats = bundle.cats,
                acts = bundle.acts,
                dateStr = bundle.dateStr,
                days = filter.days,
                catFilter = filter.catFilter,
                actFilter = filter.actFilter
            )
        )
    }.flowOn(Dispatchers.Default).distinctUntilChanged()

    private val unloggedPastSlots = combine(
        currentLogs, intervalMinutes, dayStartTime, dayEndTime
    ) { logs, interval, start, end ->
        UnloggedSlotsBuilder.build(logs, interval, start, end)
    }

    // uiState: Ana state combine — 5 akış (sınır içinde), ikinci zincir kalan alanlar
    private data class UiStateCore(
        val categories: ImmutableList<Category>,
        val activities: ImmutableList<ActivityItem>,
        val intervalMinutes: Int,
        val unloggedPastSlots: ImmutableList<Pair<String, String>>,
        val dailyMergedBlocks: ImmutableList<MergedTimeBlock>
    )

    private val uiStateCore = combine(
        categories, activities, intervalMinutes, unloggedPastSlots, dailyMergedBlocks
    ) { cats, acts, interval, unlogged, daily ->
        UiStateCore(cats, acts, interval, unlogged, daily)
    }

    private data class UiStateExtra(
        val multiDayRows: ImmutableList<MultiDayRow>,
        val currentDateStr: String,
        val daysToView: Int,
        val errorMessage: String?,
        val selectedFilterCategoryId: Set<Long>
    )

    private val uiStateExtra = combine(
        multiDayRows, currentDate, daysToView, errorMessage, selectedFilterCategoryId
    ) { multi, date, days, err, catF ->
        UiStateExtra(multi, date, days, err, catF)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        uiStateCore, uiStateExtra, selectedFilterActivityId
    ) { core, extra, actF ->
        HomeUiState(
            categories = core.categories,
            activities = core.activities,
            intervalMinutes = core.intervalMinutes,
            unloggedPastSlots = core.unloggedPastSlots,
            dailyMergedBlocks = core.dailyMergedBlocks,
            multiDayRows = extra.multiDayRows,
            currentDateStr = extra.currentDateStr,
            daysToView = extra.daysToView,
            errorMessage = extra.errorMessage,
            selectedFilterCategoryId = extra.selectedFilterCategoryId,
            selectedFilterActivityId = actF
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
                syncRepository.triggerAutoSync()
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
                syncRepository.triggerAutoSync()
            }.onFailure { handleError(it) }
        }
    }

    private fun deleteTimeLog(date: String, startTime: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteLogByDateAndStartTime(date, startTime)
                syncRepository.triggerAutoSync()
            }.onFailure { handleError(it) }
        }
    }

    private fun handleError(t: Throwable) {
        errorMessage.value = "Bir hata oluştu: ${t.localizedMessage}"
    }
}
