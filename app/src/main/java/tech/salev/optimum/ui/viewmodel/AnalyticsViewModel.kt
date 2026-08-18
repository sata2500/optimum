package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.data.repository.OptimumRepository
import tech.salev.optimum.data.repository.SettingsRepository
import tech.salev.optimum.ui.model.AnalyticsRange
import tech.salev.optimum.ui.model.AnalyticsUiState
import tech.salev.optimum.ui.model.DailyActivityData
import tech.salev.optimum.ui.components.analytics.BarChartData
import tech.salev.optimum.ui.components.analytics.PieChartData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: OptimumRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val selectedRange = MutableStateFlow(AnalyticsRange.TODAY)
    
    private val filterCategoryId = MutableStateFlow<Long?>(null)
    private val filterActivityIds = MutableStateFlow<Set<Long>>(emptySet())
    
    fun setFilterCategoryId(id: Long?) { filterCategoryId.value = id; filterActivityIds.value = emptySet() }
    fun toggleActivityFilter(id: Long) {
        val current = filterActivityIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        filterActivityIds.value = current
    }
    fun clearActivityFilter() { filterActivityIds.value = emptySet() }

    private val intervalMinutes = settingsRepository.intervalMinutes

    // ── Kritik Düzeltme: DB seviyesinde range filtreleme (365 gün RAM'de değil) ──
    // selectedRange değiştikçe flatMapLatest yalnızca ihtiyaç duyulan aralık için sorgu yapar.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val rangeFilteredLogs = selectedRange.flatMapLatest { range ->
        val today = LocalDate.now()
        val startDate = when (range) {
            AnalyticsRange.TODAY   -> today
            AnalyticsRange.WEEK   -> today.minusDays(6)
            AnalyticsRange.MONTH  -> today.minusDays(29)
            AnalyticsRange.ALL    -> today.minusDays(365)
        }
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        repository.getLogsBetweenDates(
            startDate.format(fmt),
            today.format(fmt)
        )
    }

    private val allCategories = repository.allCategories
    private val allActivities = repository.allActivities
    private val allEvaluations = repository.getAllEvaluations()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        selectedRange,
        combine(filterCategoryId, filterActivityIds) { cId, aIds -> cId to aIds },
        combine(rangeFilteredLogs, allCategories, allActivities) { logs, cats, acts -> Triple(logs, cats, acts) },
        combine(allEvaluations, intervalMinutes) { evals, interval -> evals to interval }
    ) { range, (filterCatId, filterActIds), (logs, categories, activities), (evaluations, interval) ->

        val today = LocalDate.now()
        val startDate = when (range) {
            AnalyticsRange.TODAY  -> today
            AnalyticsRange.WEEK  -> today.minusDays(6)
            AnalyticsRange.MONTH -> today.minusDays(29)
            AnalyticsRange.ALL   -> today.minusDays(365)
        }
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val startStr = startDate.format(fmt)
        val todayStr = today.format(fmt)

        // rangeFilteredLogs zaten DB seviyesinde filtrelenmiş; sadece evaluations için gerekli
        val filteredLogs = logs // Artık doğrudan kullanıyoruz — DB zaten filtreledi
        val filteredEvals = evaluations.filter { it.date >= startStr && it.date <= todayStr }

        // Total logged minutes
        val totalMinutes = filteredLogs.size * interval

        // Productive minutes
        val productiveCatIds = categories.filter { it.isProductive }.map { it.id }.toSet()
        val productiveMinutes = filteredLogs.filter { productiveCatIds.contains(it.categoryId) }.size * interval
        val productivityPct = if (totalMinutes > 0) ((productiveMinutes.toFloat() / totalMinutes) * 100).toInt() else 0

        // Active days
        val activeDays = filteredLogs.map { it.date }.toSet().size

        // Daily average hours
        val rangeDays = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
        val dailyAvgMinutes = if (rangeDays > 0) totalMinutes / rangeDays else 0

        // Avg rating
        val avgRating = if (filteredEvals.isNotEmpty()) filteredEvals.map { it.rating }.average().toFloat() else 0f

        // Streak calculation (filteredLogs'tan değil — streak geriye gittiği için allEvaluations logları kullanılır)
        // Ancak filteredLogs zaten yeterince geniş aralık içeriyor; streak için doğrudan kullanabiliriz
        var streak = 0
        var checkDate = today
        val logDates = filteredLogs.map { it.date }.toSet()
        while (logDates.contains(checkDate.format(fmt))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        // ── Category ve Activity dakika hesabı: groupBy + sumOf (idiomatik Kotlin) ──
        val categoryMinutes: Map<Long, Int> = filteredLogs
            .groupBy { it.categoryId }
            .mapValues { (_, logs) -> logs.size * interval }

        val activityMinutes: Map<Long, Int> = filteredLogs
            .groupBy { it.activityId }
            .mapValues { (_, logs) -> logs.size * interval }

        // Daily activity data for line chart (last rangeDays)
        val dailyData = (0 until minOf(rangeDays, 30)).map { daysBack ->
            val date = today.minusDays(daysBack.toLong()).format(fmt)
            val dayLogs = filteredLogs.filter { it.date == date }
            val dayTotal = dayLogs.size * interval
            val dayProductive = dayLogs.filter { productiveCatIds.contains(it.categoryId) }.size * interval
            val dayPct = if (dayTotal > 0) ((dayProductive.toFloat() / dayTotal) * 100).toInt() else 0
            DailyActivityData(date = date, totalMinutes = dayTotal, productivityPct = dayPct)
        }.reversed().toImmutableList()

        // Heatmap data for last 28 days
        val heatmapData = (0 until 28).map { daysBack ->
            val date = today.minusDays(daysBack.toLong()).format(fmt)
            val dayMinutes = filteredLogs.filter { it.date == date }.size * interval
            date to dayMinutes
        }.reversed().toImmutableList()

        // ── Chart Data Preparations ──

        // Category PieChart Data
        val catPieData = categories.filter { (categoryMinutes[it.id] ?: 0) > 0 }.map { cat ->
            val mins = categoryMinutes[cat.id] ?: 0
            val durStr = if (mins >= 60) "${mins / 60}s ${mins % 60}dk" else "${mins}dk"
            PieChartData(
                id = cat.id,
                color = cat.composeColor,
                value = mins.toFloat(),
                label = cat.name,
                subLabel = durStr
            )
        }.sortedByDescending { it.value }.toImmutableList()

        val catBarData = catPieData.map {
            BarChartData(label = it.label.take(4), value = it.value, color = it.color)
        }.toImmutableList()

        // Activity Data Calculation (Filtered by selected category / chips)
        val actsToProcess = activities.filter { act ->
            val matchesCategory = filterCatId == null || act.categoryId == filterCatId
            val matchesActivity = filterActIds.isEmpty() || filterActIds.contains(act.id)
            val hasMinutes = (activityMinutes[act.id] ?: 0) > 0
            matchesCategory && matchesActivity && hasMinutes
        }

        val actPieData = actsToProcess.map { act ->
            val value = (activityMinutes[act.id] ?: 0).toFloat()
            val mins = value.toInt()
            val durStr = if (mins >= 60) "${mins / 60}s ${mins % 60}dk" else "${mins}dk"
            PieChartData(
                id = act.id,
                color = act.composeColor,
                value = value,
                label = act.name,
                subLabel = durStr
            )
        }.sortedByDescending { it.value }.toImmutableList()

        val actBarData = actPieData.map {
            val shortLabel = if (it.label.length > 5) it.label.take(5) else it.label
            BarChartData(label = shortLabel, value = it.value, color = it.color)
        }.toImmutableList()

        // All Activities Pie Data (Across all categories)
        val allActPieData = activities.filter { (activityMinutes[it.id] ?: 0) > 0 }.map { act ->
            val value = (activityMinutes[act.id] ?: 0).toFloat()
            val mins = value.toInt()
            val durStr = if (mins >= 60) "${mins / 60}s ${mins % 60}dk" else "${mins}dk"
            PieChartData(
                id = act.id,
                color = act.composeColor,
                value = value,
                label = act.name,
                subLabel = durStr
            )
        }.sortedByDescending { it.value }.toImmutableList()

        AnalyticsUiState(
            selectedRange = range,
            totalMinutes = totalMinutes,
            productivityPct = productivityPct,
            activeDays = activeDays,
            dailyAvgMinutes = dailyAvgMinutes,
            avgRating = avgRating,
            streak = streak,
            categories = categories.toImmutableList(),
            activities = activities.toImmutableList(),
            categoryMinutes = categoryMinutes,
            activityMinutes = activityMinutes,
            dailyData = dailyData,
            heatmapData = heatmapData,
            evaluations = filteredEvals.toImmutableList(),
            categoryPieChartData = catPieData,
            categoryBarChartData = catBarData,
            activityPieChartData = actPieData,
            activityBarChartData = actBarData,
            allActivityPieChartData = allActPieData,
            filterCategoryId = filterCatId,
            filterActivityIds = filterActIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())
}

