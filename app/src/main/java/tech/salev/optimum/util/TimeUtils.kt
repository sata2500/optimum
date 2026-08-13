package tech.salev.optimum.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Shared time-formatting and parsing helpers.
 *
 * Keeps `DateTimeFormatter` instances (which are thread-safe and expensive to
 * create) in one place, and provides null-safe wrappers used across
 * ViewModel logic and composable previews alike.
 */
object TimeUtils {

    val HHmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Parses `"HH:mm"` safely, returning [default] on any error. */
    fun parseTime(value: String, default: LocalTime = LocalTime.of(0, 0)): LocalTime =
        runCatching { LocalTime.parse(value, HHmm) }.getOrDefault(default)

    /**
     * Formats [time] as `"HH:mm"`.
     * Convenience wrapper to keep format strings out of call sites.
     */
    fun format(time: LocalTime): String = time.format(HHmm)
    
    /**
     * Checks if [current] time is within [start] and [end] inclusive.
     * Handles overnight ranges (e.g. 22:00 to 06:00).
     */
    fun isTimeInRange(current: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            !current.isBefore(start) && !current.isAfter(end)
        } else {
            !current.isBefore(start) || !current.isAfter(end)
        }
    }
}

