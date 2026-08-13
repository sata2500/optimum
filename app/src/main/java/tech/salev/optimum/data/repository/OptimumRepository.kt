package tech.salev.optimum.data.repository

import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction layer for data operations.
 * Decouples the ViewModel from the concrete Room/DataStore implementation,
 * making unit testing with mock repositories straightforward.
 */
interface OptimumRepository {

    val allCategories: Flow<List<Category>>
    val allCategoriesWithActivities: Flow<List<tech.salev.optimum.data.model.CategoryWithActivities>>
    val allActivities: Flow<List<ActivityItem>>

    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    fun getActivitiesForCategory(categoryId: Long): Flow<List<ActivityItem>>
    suspend fun getMaxActivityNumberForCategory(categoryId: Long): Int
    suspend fun insertActivity(activity: ActivityItem): Long
    suspend fun updateActivity(activity: ActivityItem)
    suspend fun deleteActivity(activity: ActivityItem)

    fun getLogsForDate(date: String): Flow<List<TimeSlotLog>>
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<TimeSlotLog>>
    suspend fun hasOverlappingLogs(date: String, startTime: String, endTime: String, excludeId: Long = -1): Boolean
    suspend fun deleteOverlappingLogs(date: String, startTime: String, endTime: String, excludeId: Long = -1)
    suspend fun insertOrUpdateLog(log: TimeSlotLog): Long
    suspend fun deleteLogByDateAndStartTime(date: String, startTime: String)
    suspend fun normalizeTimeSlotLogs(newInterval: Int)

    fun getEvaluationForDate(date: String): Flow<DailyEvaluation?>
    fun getAllEvaluations(): Flow<List<DailyEvaluation>>
    suspend fun saveEvaluation(evaluation: DailyEvaluation)
    suspend fun deleteEvaluation(evaluation: DailyEvaluation)

    suspend fun restoreFullBackup(
        categories: List<Category>,
        activities: List<ActivityItem>,
        logs: List<TimeSlotLog>,
        evaluations: List<DailyEvaluation>
    )
}
