package tech.salev.optimum.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity(
    tableName = "activity_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ActivityItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val activityNumber: Int, // e.g. 1, 2, 10, 14
    val description: String = "",
    val colorHex: String = "#FFD700", // Default Gold
    @androidx.room.ColumnInfo(defaultValue = "")
    val shortCode: String = "",
    @androidx.room.ColumnInfo(defaultValue = "0")
    val displayOrder: Int = 0
) {
    fun getDisplayCode(categoryCode: String): String {
        val catCodeShort = categoryCode.firstOrNull()?.toString()?.uppercase() ?: "A"
        val actCodeShort = if (shortCode.isNotBlank()) shortCode else activityNumber.toString()
        return "$catCodeShort$actCodeShort"
    }

    val composeColor: androidx.compose.ui.graphics.Color
        get() = tech.salev.optimum.util.ColorUtils.parse(colorHex)
}
