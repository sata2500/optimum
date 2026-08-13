package tech.salev.optimum.data.local

import androidx.room.*
import tech.salev.optimum.data.model.ActivityItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_items ORDER BY displayOrder ASC, activityNumber ASC")
    fun getAllActivities(): Flow<List<ActivityItem>>

    @Query("SELECT * FROM activity_items WHERE categoryId = :categoryId ORDER BY displayOrder ASC, activityNumber ASC")
    fun getActivitiesByCategoryId(categoryId: Long): Flow<List<ActivityItem>>

    @Query("SELECT * FROM activity_items WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityItem?

    @Query("SELECT MAX(activityNumber) FROM activity_items WHERE categoryId = :categoryId")
    suspend fun getMaxActivityNumberForCategory(categoryId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityItem>)

    @Query("DELETE FROM activity_items")
    suspend fun clearAllActivities()

    @Update
    suspend fun updateActivity(activity: ActivityItem)

    @Delete
    suspend fun deleteActivity(activity: ActivityItem)
}
