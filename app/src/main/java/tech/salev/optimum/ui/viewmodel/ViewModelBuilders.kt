package tech.salev.optimum.ui.viewmodel

import kotlinx.collections.immutable.ImmutableList
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.ui.model.MultiDayCell
import tech.salev.optimum.ui.model.MultiDayRow
import tech.salev.optimum.util.TimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf

// ─────────────────────────────────────────────────────────────────────────────
// Data containers (internal — shared between ViewModel and builders)
// ─────────────────────────────────────────────────────────────────────────────

internal data class DailyBlockInputs(
    val slots: ImmutableList<Triple<String, String, LocalTime>>,
    val logs: ImmutableList<TimeSlotLog>,
    val cats: ImmutableList<Category>,
    val acts: ImmutableList<ActivityItem>,
    val dateStr: String,
    val catFilter: Set<Long>,
    val actFilter: Set<Long>,
    /** LocalTime.now() dışarıdan geçirilerek her slot için tekrarlanan sistem çağrısı önlenir */
    val nowTime: LocalTime = LocalTime.now()
)

internal data class MultiDayInputs(
    val slots: ImmutableList<Triple<String, String, LocalTime>>,
    val logs: ImmutableList<TimeSlotLog>,
    val cats: ImmutableList<Category>,
    val acts: ImmutableList<ActivityItem>,
    val dateStr: String,
    val days: Int,
    val catFilter: Set<Long>,
    val actFilter: Set<Long>
)

// ─────────────────────────────────────────────────────────────────────────────
// Pure builder objects — zero Android dependencies, trivially unit-testable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Builds the list of time-slot boundaries for a given day range and interval.
 */
internal object TimeSlotBuilder {
    fun build(
        interval: Int,
        startStr: String,
        endStr: String
    ): ImmutableList<Triple<String, String, LocalTime>> {
        val list = mutableListOf<Triple<String, String, LocalTime>>()
        var curr = TimeUtils.parseTime(startStr, LocalTime.of(6, 0))
        val endLimit = TimeUtils.parseTime(endStr, LocalTime.of(23, 30))

        while (curr.isBefore(endLimit) || curr == endLimit) {
            val next = curr.plusMinutes(interval.toLong())
            list.add(Triple(TimeUtils.format(curr), TimeUtils.format(next), curr))
            if (next.hour == 0 && next.minute == 0) break
            if (next.isAfter(endLimit)) break
            curr = next
        }
        return list.toImmutableList()
    }
}

/**
 * Merges time slots with their log records for the single-day table view.
 */
internal object DailyMergedBlocksBuilder {
    fun build(inp: DailyBlockInputs): ImmutableList<MergedTimeBlock> {
        val catMap = inp.cats.associateBy { it.id }
        val actMap = inp.acts.associateBy { it.id }
        val dateParsed = runCatching { LocalDate.parse(inp.dateStr) }.getOrDefault(LocalDate.now())
        val now = inp.nowTime // LocalTime.now() dışarıdan geçirildi — her slot için çağrı yok
        val today = LocalDate.now()

        return inp.slots.map { (start, end, slotTime) ->
            val log = inp.logs.firstOrNull { l ->
                TimeUtils.isTimeSlotOverlapping(start, end, l.startTime, l.endTime)
            }
            val filteredOut = isFilteredOut(log, inp.catFilter, inp.actFilter)
            val displayLog = if (filteredOut) null else log
            val isPast = dateParsed.isBefore(today) || (dateParsed == today && slotTime.isBefore(now))

            MergedTimeBlock(
                startTime = start,
                endTime = end,
                log = displayLog,
                category = displayLog?.let { catMap[it.categoryId] },
                activity = displayLog?.let { actMap[it.activityId] },
                isPastEmpty = isPast && displayLog == null,
                isFuture = dateParsed.isAfter(today) || (dateParsed == today && slotTime.isAfter(now)),
                isCurrentSlot = dateParsed == today &&
                        (now.isAfter(slotTime) || now == slotTime) &&
                        (end == "00:00" || now.isBefore(TimeUtils.parseTime(end, LocalTime.MAX))),
                slotCount = 1,
                startSlotStr = start,
                isFilteredOut = filteredOut
            )
        }.toImmutableList()
    }
}

/**
 * Builds the multi-column grid rows for the multi-day matrix view.
 */
internal object MultiDayRowsBuilder {
    fun build(inp: MultiDayInputs): ImmutableList<MultiDayRow> {
        val endDate = runCatching { LocalDate.parse(inp.dateStr) }.getOrDefault(LocalDate.now())
        val startDate = endDate.minusDays(inp.days.toLong() - 1)
        val dates = generateSequence(endDate) { d ->
            d.minusDays(1).takeIf { !it.isBefore(startDate) }
        }.toList()

        val logsByDate = inp.logs.groupBy { it.date }
        val catMap = inp.cats.associateBy { it.id }
        val actMap = inp.acts.associateBy { it.id }
        val today = LocalDate.now()
        val now = LocalTime.now()

        return inp.slots.map { (start, end, slotTime) ->
            val cells = dates.map { date ->
                val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayLogs = logsByDate[dStr] ?: emptyList()
                val log = dayLogs.firstOrNull { l ->
                    TimeUtils.isTimeSlotOverlapping(start, end, l.startTime, l.endTime)
                }
                val filteredOut = isFilteredOut(log, inp.catFilter, inp.actFilter)
                val displayLog = if (filteredOut) null else log
                val isPast = date.isBefore(today) || (date == today && slotTime.isBefore(now))

                MultiDayCell(
                    dateStr = dStr,
                    log = displayLog,
                    category = displayLog?.let { catMap[it.categoryId] },
                    activity = displayLog?.let { actMap[it.activityId] },
                    isPastEmpty = isPast && log == null,
                    isFuture = date.isAfter(today) || (date == today && slotTime.isAfter(now)),
                    isFilteredOut = filteredOut
                )
            }
            MultiDayRow(startStr = start, endStr = end, cells = cells)
        }.toImmutableList()
    }
}

/**
 * Finds all past time slots that have no log entry (CatchUp mode).
 */
internal object UnloggedSlotsBuilder {
    fun build(
        logs: ImmutableList<TimeSlotLog>,
        interval: Int,
        startStr: String,
        endStr: String
    ): ImmutableList<Pair<String, String>> {
        val slots = mutableListOf<Pair<String, String>>()
        val now = LocalTime.now()
        var curr = TimeUtils.parseTime(startStr, LocalTime.of(6, 0))
        val limit = TimeUtils.parseTime(endStr, LocalTime.of(23, 30))

        while (curr.isBefore(now) && curr.isBefore(limit)) {
            val sStr = TimeUtils.format(curr)
            val next = curr.plusMinutes(interval.toLong())
            val nextStr = TimeUtils.format(next)
            val isLogged = logs.any { l ->
                TimeUtils.isTimeSlotOverlapping(sStr, nextStr, l.startTime, l.endTime)
            }
            if (!isLogged) slots.add(sStr to nextStr)
            if (next.hour == 0 && next.minute == 0) break
            curr = next
        }
        return slots.toImmutableList()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared filter helper
// ─────────────────────────────────────────────────────────────────────────────

private fun isFilteredOut(
    log: TimeSlotLog?,
    catFilter: Set<Long>,
    actFilter: Set<Long>
): Boolean =
    (catFilter.isNotEmpty() && !catFilter.contains(log?.categoryId)) ||
    (actFilter.isNotEmpty() && !actFilter.contains(log?.activityId))
