package tech.salev.optimum.ui.model

import androidx.compose.runtime.Immutable
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog

@Immutable
data class MergedTimeBlock(
    val startTime: String,
    val endTime: String,
    val log: TimeSlotLog?,
    val category: Category?,
    val activity: ActivityItem?,
    val isPastEmpty: Boolean,
    val isFuture: Boolean,
    val isCurrentSlot: Boolean,
    val slotCount: Int,
    val startSlotStr: String,
    val isFilteredOut: Boolean = false
)

@Immutable
data class MultiDayRow(
    val startStr: String,
    val endStr: String,
    val cells: List<MultiDayCell>
)

@Immutable
data class MultiDayCell(
    val dateStr: String,
    val log: TimeSlotLog?,
    val category: Category?,
    val activity: ActivityItem?,
    val isPastEmpty: Boolean,
    val isFuture: Boolean,
    val isFilteredOut: Boolean
)

@Immutable
data class ActiveSlotInfo(
    val date: String,
    val start: String,
    val end: String,
    val log: TimeSlotLog?,
    val isFuture: Boolean = false
)
