package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
import tech.salev.optimum.ui.model.InsightEngine
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

    val selectedRange = MutableStateFlow(AnalyticsRange.WEEK)
    
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
    private val allLogs = repository.getLogsBetweenDates(
        LocalDate.now().minusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE),
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
    private val allCategories = repository.allCategories
    private val allActivities = repository.allActivities
    private val allEvaluations = repository.getAllEvaluations()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        selectedRange,
        combine(filterCategoryId, filterActivityIds) { cId, aIds -> cId to aIds },
        combine(allLogs, allCategories, allActivities) { logs, cats, acts -> Triple(logs, cats, acts) },
        combine(allEvaluations, intervalMinutes) { evals, interval -> evals to interval }
    ) { range, (filterCatId, filterActIds), (logs, categories, activities), (evaluations, interval) ->

        val today = LocalDate.now()
        val startDate = when (range) {
            AnalyticsRange.TODAY -> today
            AnalyticsRange.WEEK -> today.minusDays(6)
            AnalyticsRange.MONTH -> today.minusDays(29)
            AnalyticsRange.ALL -> today.minusDays(365)
        }
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val startStr = startDate.format(fmt)
        val todayStr = today.format(fmt)

        val filteredLogs = logs.filter { it.date >= startStr && it.date <= todayStr }
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

        // Streak calculation
        var streak = 0
        var checkDate = today
        val logDates = logs.map { it.date }.toSet()
        while (logDates.contains(checkDate.format(fmt))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        // Category minutes map
        val categoryMinutes = mutableMapOf<Long, Int>()
        filteredLogs.forEach { log ->
            categoryMinutes[log.categoryId] = (categoryMinutes[log.categoryId] ?: 0) + interval
        }

        // Activity minutes map
        val activityMinutes = mutableMapOf<Long, Int>()
        filteredLogs.forEach { log ->
            activityMinutes[log.activityId] = (activityMinutes[log.activityId] ?: 0) + interval
        }

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

        // Insights
        val insights = InsightEngine.generate(
            dailyData = dailyData,
            streak = streak,
            productivityPct = productivityPct,
            categories = categories.toImmutableList(),
            categoryMinutes = categoryMinutes,
            totalMinutes = totalMinutes,
            avgRating = avgRating
        )

        // ── Chart Data Preparations ──

        // Category PieChart Data
        val catPieData = categories.filter { (categoryMinutes[it.id] ?: 0) > 0 }.map { cat ->
            PieChartData(
                color = cat.composeColor,
                value = (categoryMinutes[cat.id] ?: 0).toFloat(),
                label = cat.name
            )
        }.sortedByDescending { it.value }.toImmutableList()

        val catBarData = catPieData.map {
            BarChartData(label = it.label.take(3), value = it.value, color = it.color)
        }.toImmutableList()

        // Activity Data Calculation
        val actsToProcess = activities.filter { act ->
            val matchesCategory = filterCatId == null || act.categoryId == filterCatId
            val matchesActivity = filterActIds.isEmpty() || filterActIds.contains(act.id)
            val hasMinutes = (activityMinutes[act.id] ?: 0) > 0
            matchesCategory && matchesActivity && hasMinutes
        }

        val actPieData = actsToProcess.map { act ->
            val value = (activityMinutes[act.id] ?: 0).toFloat()
            val label = if (act.shortCode.isNotBlank()) act.shortCode else act.name.take(3)
            PieChartData(color = act.composeColor, value = value, label = label)
        }.sortedByDescending { it.value }.toImmutableList()

        val actBarData = actPieData.map {
            BarChartData(label = it.label.take(3), value = it.value, color = it.color)
        }.toImmutableList()

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
            insights = insights,
            evaluations = filteredEvals.toImmutableList(),
            categoryPieChartData = catPieData,
            categoryBarChartData = catBarData,
            activityPieChartData = actPieData,
            activityBarChartData = actBarData,
            filterCategoryId = filterCatId,
            filterActivityIds = filterActIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())
}
