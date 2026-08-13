package tech.salev.optimum.ui.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.ui.components.analytics.BarChartData
import tech.salev.optimum.ui.components.analytics.PieChartData

enum class AnalyticsRange(val label: String) {
    TODAY("Bugün"),
    WEEK("7 Gün"),
    MONTH("30 Gün"),
    ALL("Tüm Zamanlar")
}

data class DailyActivityData(
    val date: String,          // YYYY-MM-DD
    val totalMinutes: Int,
    val productivityPct: Int   // 0-100
)

data class AnalyticsInsight(
    val emoji: String,
    val title: String,
    val description: String
)

data class AnalyticsUiState(
    val selectedRange: AnalyticsRange = AnalyticsRange.WEEK,
    val totalMinutes: Int = 0,
    val productivityPct: Int = 0,
    val activeDays: Int = 0,
    val dailyAvgMinutes: Int = 0,
    val avgRating: Float = 0f,
    val streak: Int = 0,
    val categories: ImmutableList<Category> = persistentListOf(),
    val activities: ImmutableList<ActivityItem> = persistentListOf(),
    val categoryMinutes: Map<Long, Int> = emptyMap(),
    val activityMinutes: Map<Long, Int> = emptyMap(),
    val dailyData: ImmutableList<DailyActivityData> = persistentListOf(),
    val heatmapData: ImmutableList<Pair<String, Int>> = persistentListOf(),
    val insights: ImmutableList<AnalyticsInsight> = persistentListOf(),
    val evaluations: ImmutableList<DailyEvaluation> = persistentListOf(),
    val categoryPieChartData: ImmutableList<PieChartData> = persistentListOf(),
    val categoryBarChartData: ImmutableList<BarChartData> = persistentListOf(),
    val activityPieChartData: ImmutableList<PieChartData> = persistentListOf(),
    val activityBarChartData: ImmutableList<BarChartData> = persistentListOf(),
    val filterCategoryId: Long? = null,
    val filterActivityIds: Set<Long> = emptySet()
)
