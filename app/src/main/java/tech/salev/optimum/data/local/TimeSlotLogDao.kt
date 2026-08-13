package tech.salev.optimum.data.local

import androidx.room.*
import tech.salev.optimum.data.model.TimeSlotLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotLogDao {
    @Query("SELECT * FROM time_slot_logs WHERE date = :date ORDER BY startTime ASC")
    fun getLogsForDate(date: String): Flow<List<TimeSlotLog>>

    @Query("SELECT * FROM time_slot_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<TimeSlotLog>>

    @Query("SELECT * FROM time_slot_logs WHERE date = :date AND startTime = :startTime LIMIT 1")
    suspend fun getLogByDateAndStartTime(date: String, startTime: String): TimeSlotLog?

    @Query("SELECT COUNT(*) FROM time_slot_logs WHERE date = :date AND startTime < :endTime AND endTime > :startTime AND id != :excludeId")
    suspend fun countOverlappingLogs(date: String, startTime: String, endTime: String, excludeId: Long = -1): Int

    @Query("DELETE FROM time_slot_logs WHERE date = :date AND startTime < :endTime AND endTime > :startTime AND id != :excludeId")
    suspend fun deleteOverlappingLogs(date: String, startTime: String, endTime: String, excludeId: Long = -1)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TimeSlotLog): Long

    @Update
    suspend fun updateLog(log: TimeSlotLog)

    @Delete
    suspend fun deleteLog(log: TimeSlotLog)

    @Query("DELETE FROM time_slot_logs WHERE date = :date AND startTime = :startTime")
    suspend fun deleteLogByDateAndStartTime(date: String, startTime: String)

    @Query("SELECT * FROM time_slot_logs")
    suspend fun getAllLogsSync(): List<TimeSlotLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<TimeSlotLog>)

    @Delete
    suspend fun deleteLogs(logs: List<TimeSlotLog>)

    @Query("DELETE FROM time_slot_logs")
    suspend fun clearAllLogs()
}
