package tech.salev.optimum.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.ui.components.CatchUpDialog
import tech.salev.optimum.ui.components.TimeLogBottomSheet
import tech.salev.optimum.ui.components.home.DateNavigationBar
import tech.salev.optimum.ui.components.home.DailyTableRow
import tech.salev.optimum.ui.components.home.DaysSlider
import tech.salev.optimum.ui.components.home.EmptyStateCard
import tech.salev.optimum.ui.components.home.FilterBar
import tech.salev.optimum.ui.components.home.GridCellCompact
import tech.salev.optimum.ui.components.home.NowIndicator
import tech.salev.optimum.ui.components.home.DailyGridView
import tech.salev.optimum.ui.components.home.MultiDayGridView
import tech.salev.optimum.ui.model.ActiveSlotInfo
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.util.TimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Home screen — orchestrates the daily/multi-day time-slot grid.
 *
 * All visual sub-sections live in dedicated composables under
 * `ui/components/home/`. This composable is responsible only for:
 * - Collecting ViewModel state
 * - Deciding which sub-view to display (empty / daily / multi-day)
 * - Wiring user actions back to the ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: tech.salev.optimum.ui.viewmodel.HomeViewModel,
    optimumViewModel: OptimumViewModel,
    categoryManagerViewModel: tech.salev.optimum.ui.viewmodel.CategoryManagerViewModel,
    onNavigateToCategories: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val categories = uiState.categories
    val activities = uiState.activities
    val currentDateStr = uiState.currentDateStr
    val intervalMinutes = uiState.intervalMinutes
    val unloggedPastSlots = uiState.unloggedPastSlots
    val daysToView = uiState.daysToView
    val errorMessage = uiState.errorMessage
    val dailyMergedBlocks = uiState.dailyMergedBlocks
    val multiDayRows = uiState.multiDayRows
    val selectedFilterCategoryId = uiState.selectedFilterCategoryId
    val selectedFilterActivityId = uiState.selectedFilterActivityId

    var activeSlotForLogging by remember { mutableStateOf<ActiveSlotInfo?>(null) }
    var showCatchUpDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Trigger quick-log from notification shortcut
    LaunchedEffect(Unit) {
        optimumViewModel.quickLogTrigger.collectLatest {
            val slot = unloggedPastSlots.lastOrNull()
            if (slot != null) {
                activeSlotForLogging = ActiveSlotInfo(currentDateStr, slot.first, slot.second, null)
            }
        }
    }

    val currentDateParsed = remember(currentDateStr) {
        runCatching { LocalDate.parse(currentDateStr) }.getOrDefault(LocalDate.now())
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Zaman Çizelgesi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (unloggedPastSlots.isNotEmpty() && categories.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showCatchUpDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Toplu Doldur (${unloggedPastSlots.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            DaysSlider(
                daysToView = daysToView,
                onDaysChanged = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetDaysToView(it)) }
            )

            DateNavigationBar(
                currentDate = currentDateStr,
                onDateSelected = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetSelectedDate(it)) }
            )

            FilterBar(
                categories = categories,
                activities = activities,
                selectedCategoryIds = selectedFilterCategoryId,
                selectedActivityIds = selectedFilterActivityId,
                onCategoryFilterChanged = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetCategoryFilter(it)) },
                onActivityFilterChanged = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetActivityFilter(it)) }
            )

            when {
                categories.isEmpty() || activities.isEmpty() -> {
                    EmptyStateCard(onNavigateToCategories = onNavigateToCategories)
                }

                daysToView == 1 -> {
                    DailyGridView(
                        currentDateStr = currentDateStr,
                        currentDateParsed = currentDateParsed,
                        intervalMinutes = intervalMinutes,
                        dailyMergedBlocks = dailyMergedBlocks,
                        onSlotClick = { slotInfo ->
                            if (!slotInfo.isFuture) {
                                activeSlotForLogging = slotInfo
                            } else {
                                Toast.makeText(context, "Bu zaman dilimi henüz tamamlanmadı.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                else -> {
                    MultiDayGridView(
                        multiDayRows = multiDayRows,
                        onSlotClick = { slotInfo ->
                            if (!slotInfo.isFuture) {
                                activeSlotForLogging = slotInfo
                            } else {
                                Toast.makeText(context, "Bu zaman dilimi henüz tamamlanmadı.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs & Sheets ──────────────────────────────────────────────────────

    activeSlotForLogging?.let { slot ->
        TimeLogBottomSheet(
            startTime = slot.start,
            endTime = slot.end,
            date = slot.date,
            existingLog = slot.log,
            categories = categories,
            activities = activities,
            onDismiss = { activeSlotForLogging = null },
            onSaveLog = { editedDate, editedStart, editedEnd, categoryId, activityId, note ->
                val startLocal = TimeUtils.parseTime(editedStart)
                val endLocal = TimeUtils.parseTime(editedEnd)
                val duration = java.time.Duration.between(startLocal, endLocal).toMinutes()

                if (duration > intervalMinutes) {
                    // Merge: fill all covered slots
                    val logsToInsert = buildList {
                        var curr = startLocal
                        while (curr.isBefore(endLocal)) {
                            val next = curr.plusMinutes(intervalMinutes.toLong())
                            add(TimeSlotLog(date = editedDate, startTime = TimeUtils.format(curr),
                                           endTime = TimeUtils.format(next), categoryId = categoryId,
                                           activityId = activityId, note = note))
                            curr = next
                        }
                    }
                    homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.LogMultipleSlots(logsToInsert))
                } else {
                    val logId = slot.log?.id ?: 0L
                    homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.LogTimeSlot(editedDate, editedStart, editedEnd, categoryId, activityId, note, logId))
                }
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                activeSlotForLogging = null
            },
            onDeleteLog = {
                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.DeleteTimeLog(slot.date, slot.start))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                activeSlotForLogging = null
            },
            onAddCategory = { name ->
                val color = String.format("#%06X", (0xFFFFFF and (Math.random() * 0xFFFFFF).toInt()))
                categoryManagerViewModel.addCategory(name, name.take(1).uppercase(), color, true)
            },
            onAddActivity = { catId, name -> categoryManagerViewModel.addActivity(catId, name) }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.ClearError) },
            title = { Text("Uyarı", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.ClearError) }) { Text("Tamam") } }
        )
    }

    if (showCatchUpDialog && unloggedPastSlots.isNotEmpty()) {
        CatchUpDialog(
            unloggedSlots = unloggedPastSlots,
            currentDate = currentDateStr,
            categories = categories,
            activities = activities,
            onDismiss = { showCatchUpDialog = false },
            onSaveBatch = { batchLogs ->
                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.LogMultipleSlots(batchLogs))
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                showCatchUpDialog = false
            }
        )
    }
}

