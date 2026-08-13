package tech.salev.optimum.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class TargetSlot(
    val targetDateStr: String,
    val startStr: String,
    val endStr: String
)

object TimeSlotCalculator {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    fun getMostRecentlyCompletedSlot(
        currentDateStr: String,
        intervalMinutes: Int,
        currentTime: LocalTime = LocalTime.now()
    ): TargetSlot {
        val minutesSinceMidnight = currentTime.hour * 60 + currentTime.minute

        // Round down to the nearest slot boundary
        val currentSlotStartMinutes = (minutesSinceMidnight / intervalMinutes) * intervalMinutes
        // The slot the user just completed = the one before the current slot
        val completedSlotStartMinutes = currentSlotStartMinutes - intervalMinutes

        var targetDate = LocalDate.parse(currentDateStr)
        var adjustedStartMinutes = completedSlotStartMinutes

        // Handle midnight crossing (e.g. first slot of the day)
        if (adjustedStartMinutes < 0) {
            adjustedStartMinutes += 24 * 60
            targetDate = targetDate.minusDays(1)
        }

        val adjustedEndMinutes = adjustedStartMinutes + intervalMinutes

        val startT = LocalTime.of(adjustedStartMinutes / 60, adjustedStartMinutes % 60)
        val endT = LocalTime.of((adjustedEndMinutes / 60) % 24, adjustedEndMinutes % 60)

        return TargetSlot(
            targetDateStr = targetDate.toString(),
            startStr = startT.format(fmt),
            endStr = endT.format(fmt)
        )
    }
}
