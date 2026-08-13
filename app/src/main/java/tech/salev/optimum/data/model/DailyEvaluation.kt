package tech.salev.optimum.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity(tableName = "daily_evaluations")
data class DailyEvaluation(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val rating: Int = 0, // 1 to 5 star rating
    val mood: Int = 0,   // 0=unset, 1=bad, 2=neutral, 3=good, 4=great
    val journalNote: String = "", // Self-reflection summary for the day
    val updatedTimestamp: Long = System.currentTimeMillis()
)
