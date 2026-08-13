package tech.salev.optimum.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String, // Short code e.g. "EG", "MRK", "IBD", "ZMN"
    val colorHex: String, // Hex color code e.g. "#4CAF50"
    val iconName: String = "Category",
    val isProductive: Boolean = true, // Productive vs Distraction/Rest
    @androidx.room.ColumnInfo(defaultValue = "0")
    val displayOrder: Int = 0
) {
    val composeColor: androidx.compose.ui.graphics.Color
        get() = tech.salev.optimum.util.ColorUtils.parse(colorHex)
}
