package tech.salev.optimum.data.repository

import tech.salev.optimum.data.local.ActivityDao
import tech.salev.optimum.data.local.CategoryDao
import tech.salev.optimum.data.local.DailyEvaluationDao
import tech.salev.optimum.data.local.TimeSlotLogDao
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

/**
 * Concrete implementation of [OptimumRepository].
 * All data access goes through Room DAOs.
 * Injected as a singleton by Hilt.
 */
@Singleton
class OptimumRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val activityDao: ActivityDao,
    private val timeSlotLogDao: TimeSlotLogDao,
    private val dailyEvaluationDao: DailyEvaluationDao,
    private val db: tech.salev.optimum.data.local.OptimumDatabase
) : OptimumRepository {

    override val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    override val allCategoriesWithActivities: Flow<List<tech.salev.optimum.data.model.CategoryWithActivities>> = categoryDao.getCategoriesWithActivities()
    override val allActivities: Flow<List<ActivityItem>> = activityDao.getAllActivities()

    override suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    override suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    override suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    override suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    override fun getActivitiesForCategory(categoryId: Long): Flow<List<ActivityItem>> =
        activityDao.getActivitiesByCategoryId(categoryId)

    override suspend fun getMaxActivityNumberForCategory(categoryId: Long): Int =
        (activityDao.getMaxActivityNumberForCategory(categoryId) ?: 0) + 1

    override suspend fun insertActivity(activity: ActivityItem): Long =
        activityDao.insertActivity(activity)

    override suspend fun updateActivity(activity: ActivityItem) =
        activityDao.updateActivity(activity)

    override suspend fun deleteActivity(activity: ActivityItem) =
        activityDao.deleteActivity(activity)

    override fun getLogsForDate(date: String): Flow<List<TimeSlotLog>> =
        timeSlotLogDao.getLogsForDate(date)

    override fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<TimeSlotLog>> =
        timeSlotLogDao.getLogsBetweenDates(startDate, endDate)

    override suspend fun hasOverlappingLogs(
        date: String, startTime: String, endTime: String, excludeId: Long
    ): Boolean = timeSlotLogDao.countOverlappingLogs(date, startTime, endTime, excludeId) > 0

    override suspend fun deleteOverlappingLogs(
        date: String, startTime: String, endTime: String, excludeId: Long
    ) = timeSlotLogDao.deleteOverlappingLogs(date, startTime, endTime, excludeId)

    override suspend fun insertOrUpdateLog(log: TimeSlotLog): Long =
        timeSlotLogDao.insertLog(log)

    override suspend fun deleteLogByDateAndStartTime(date: String, startTime: String) =
        timeSlotLogDao.deleteLogByDateAndStartTime(date, startTime)

    override suspend fun normalizeTimeSlotLogs(newInterval: Int) {
        val allLogs = timeSlotLogDao.getAllLogsSync()
        val logsToDelete = mutableListOf<TimeSlotLog>()
        val logsToInsert = mutableListOf<TimeSlotLog>()

        for (log in allLogs) {
            val startLocal = tech.salev.optimum.util.TimeUtils.parseTime(log.startTime)
            val endLocal = tech.salev.optimum.util.TimeUtils.parseTime(log.endTime)
            val duration = java.time.Duration.between(startLocal, endLocal).toMinutes()

            if (duration > newInterval) {
                logsToDelete.add(log)
                var curr = startLocal
                while (curr.isBefore(endLocal)) {
                    val next = curr.plusMinutes(newInterval.toLong())
                    if (next.isAfter(endLocal)) break // Güvenlik kontrolü
                    logsToInsert.add(
                        log.copy(
                            id = 0, // Auto-generate
                            startTime = tech.salev.optimum.util.TimeUtils.format(curr),
                            endTime = tech.salev.optimum.util.TimeUtils.format(next)
                        )
                    )
                    curr = next
                }
            }
        }

        if (logsToDelete.isNotEmpty()) {
            timeSlotLogDao.deleteLogs(logsToDelete)
        }
        if (logsToInsert.isNotEmpty()) {
            timeSlotLogDao.insertLogs(logsToInsert)
        }
    }

    override fun getEvaluationForDate(date: String): Flow<DailyEvaluation?> =
        dailyEvaluationDao.getEvaluationForDate(date)

    override fun getAllEvaluations(): Flow<List<DailyEvaluation>> =
        dailyEvaluationDao.getAllEvaluations()

    override suspend fun saveEvaluation(evaluation: DailyEvaluation) =
        dailyEvaluationDao.insertOrUpdateEvaluation(evaluation)

    override suspend fun deleteEvaluation(evaluation: DailyEvaluation) =
        dailyEvaluationDao.deleteEvaluation(evaluation)

    override suspend fun restoreFullBackup(
        categories: List<Category>,
        activities: List<ActivityItem>,
        logs: List<TimeSlotLog>,
        evaluations: List<DailyEvaluation>
    ) {
        db.withTransaction {
            // Tabloları temizle
            categoryDao.clearAllCategories()
            activityDao.clearAllActivities()
            timeSlotLogDao.clearAllLogs()
            dailyEvaluationDao.clearAllEvaluations()

            // Orijinal ID'leriyle ekle
            if (categories.isNotEmpty()) categoryDao.insertCategories(categories)
            if (activities.isNotEmpty()) activityDao.insertActivities(activities)
            if (logs.isNotEmpty()) timeSlotLogDao.insertLogs(logs)
            if (evaluations.isNotEmpty()) dailyEvaluationDao.insertEvaluations(evaluations)
        }
    }
}
