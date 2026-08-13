package tech.salev.optimum.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity(tableName = "time_slot_logs")
data class TimeSlotLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val startTime: String, // HH:mm (e.g. "08:00")
    val endTime: String, // HH:mm (e.g. "08:30")
    val categoryId: Long,
    val activityId: Long,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
