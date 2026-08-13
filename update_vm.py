import os
import re

file_path = "app/src/main/java/tech/salev/optimum/ui/viewmodel/OptimumViewModel.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports
if "tech.salev.optimum.ui.model.MergedTimeBlock" not in content:
    import_index = content.find("import ")
    imports_to_add = """import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.ui.model.MultiDayRow
import tech.salev.optimum.ui.model.MultiDayCell
"""
    content = content[:import_index] + imports_to_add + content[import_index:]

# Add filter states and computed blocks after allEvaluations
all_evaluations_regex = r"(val allEvaluations: StateFlow<List<DailyEvaluation>> = repository\.getAllEvaluations\(\)\n\s*\.stateIn\(viewModelScope, SharingStarted\.WhileSubscribed\(5000\), emptyList\(\)\))"
match = re.search(all_evaluations_regex, content)
if match:
    insert_pos = match.end()
    
    new_code = """

    private val _selectedFilterCategoryId = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFilterCategoryId: StateFlow<Set<Long>> = _selectedFilterCategoryId.asStateFlow()

    private val _selectedFilterActivityId = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFilterActivityId: StateFlow<Set<Long>> = _selectedFilterActivityId.asStateFlow()

    fun setCategoryFilter(ids: Set<Long>) { _selectedFilterCategoryId.value = ids }
    fun setActivityFilter(ids: Set<Long>) { _selectedFilterActivityId.value = ids }
    
    val timeSlots: StateFlow<List<Triple<String, String, LocalTime>>> = combine(
        intervalMinutes, dayStartTime, dayEndTime
    ) { interval, startStr, endStr ->
        val list = mutableListOf<Triple<String, String, LocalTime>>()
        var curr = try { LocalTime.parse(startStr) } catch (e: Exception) { LocalTime.of(6, 0) }
        val endLimit = try { LocalTime.parse(endStr) } catch (e: Exception) { LocalTime.of(23, 30) }
        val uiInterval = maxOf(15, interval)

        while (curr.isBefore(endLimit) || curr == endLimit) {
            val sStr = curr.format(DateTimeFormatter.ofPattern("HH:mm"))
            val nextTime = curr.plusMinutes(uiInterval.toLong())
            val eStr = nextTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            list.add(Triple(sStr, eStr, curr))
            if (nextTime.hour == 0 && nextTime.minute == 0) break
            if (nextTime.isAfter(endLimit)) break
            curr = nextTime
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyMergedBlocks: StateFlow<List<MergedTimeBlock>> = combine(
        timeSlots, currentLogs, categories, activities, currentDate, selectedFilterCategoryId, selectedFilterActivityId
    ) { slots, logs, cats, acts, dateStr, catFilter, actFilter ->
        val blocks = mutableListOf<MergedTimeBlock>()
        var currentBlock: MergedTimeBlock? = null
        val logMap = logs.associateBy { it.startTime }
        val catMap = cats.associateBy { it.id }
        val actMap = acts.associateBy { it.id }
        val currentDateParsed = try { LocalDate.parse(dateStr) } catch (e: Exception) { LocalDate.now() }
        val now = LocalTime.now()
        val today = LocalDate.now()

        for ((start, end, slotTime) in slots) {
            val log = logMap[start]
            val isFilteredOut = (catFilter.isNotEmpty() && !catFilter.contains(log?.categoryId)) || 
                                (actFilter.isNotEmpty() && !actFilter.contains(log?.activityId))
            val displayLog = if (isFilteredOut) null else log
            
            val category = displayLog?.let { l -> catMap[l.categoryId] }
            val activity = displayLog?.let { l -> actMap[l.activityId] }
            
            val isPast = currentDateParsed.isBefore(today) || (currentDateParsed == today && slotTime.isBefore(now))
            val isFuture = currentDateParsed.isAfter(today) || (currentDateParsed == today && slotTime.isAfter(now))
            
            val isCurrentSlot = currentDateParsed == today && (now.isAfter(slotTime) || now == slotTime) &&
                                (end == "00:00" || now.isBefore(try { LocalTime.parse(end) } catch (e: Exception) { LocalTime.MAX }))
            
            val canMerge = currentBlock != null && displayLog != null && currentBlock.log != null && 
                           currentBlock.log!!.activityId == displayLog.activityId

            if (canMerge) {
                currentBlock!!.endTime = end
                currentBlock!!.slotCount += 1
            } else {
                if (currentBlock != null) blocks.add(currentBlock!!)
                currentBlock = MergedTimeBlock(
                    startTime = start,
                    endTime = end,
                    log = displayLog,
                    category = category,
                    activity = activity,
                    isPastEmpty = isPast && displayLog == null,
                    isFuture = isFuture,
                    isCurrentSlot = isCurrentSlot,
                    slotCount = 1,
                    startSlotStr = start,
                    isFilteredOut = isFilteredOut
                )
            }
        }
        if (currentBlock != null) blocks.add(currentBlock!!)
        blocks
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val multiDayRows: StateFlow<List<MultiDayRow>> = combine(
        timeSlots, multiDayLogs, categories, activities, currentDate, daysToView, selectedFilterCategoryId, selectedFilterActivityId
    ) { slots, logs, cats, acts, dateStr, days, catFilter, actFilter ->
        val endLocalDate = try { LocalDate.parse(dateStr) } catch (e: Exception) { LocalDate.now() }
        val startLocalDate = endLocalDate.minusDays(days.toLong() - 1)
        val datesToDisplay = mutableListOf<LocalDate>()
        var d = startLocalDate
        while (!d.isAfter(endLocalDate)) {
            datesToDisplay.add(d)
            d = d.plusDays(1)
        }

        val logsMap = logs.groupBy { it.date }.mapValues { (_, dayLogs) -> dayLogs.associateBy { it.startTime } }
        val catMap = cats.associateBy { it.id }
        val actMap = acts.associateBy { it.id }
        val today = LocalDate.now()
        val now = LocalTime.now()

        slots.map { (start, end, slotTime) ->
            val cells = datesToDisplay.map { date ->
                val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val log = logsMap[dStr]?.get(start)
                val isFilteredOut = (catFilter.isNotEmpty() && !catFilter.contains(log?.categoryId)) || 
                                    (actFilter.isNotEmpty() && !actFilter.contains(log?.activityId))
                val displayLog = if (isFilteredOut) null else log
                val category = displayLog?.let { l -> catMap[l.categoryId] }
                val activity = displayLog?.let { l -> actMap[l.activityId] }
                val isPast = date.isBefore(today) || (date == today && slotTime.isBefore(now))
                
                MultiDayCell(
                    dateStr = dStr,
                    log = displayLog,
                    category = category,
                    activity = activity,
                    isPastEmpty = isPast && log == null,
                    isFuture = date.isAfter(today) || (date == today && slotTime.isAfter(now)),
                    isFilteredOut = isFilteredOut
                )
            }
            MultiDayRow(startStr = start, endStr = end, cells = cells)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
"""
    content = content[:insert_pos] + new_code + content[insert_pos:]

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
