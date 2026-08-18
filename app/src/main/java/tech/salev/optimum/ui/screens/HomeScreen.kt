package tech.salev.optimum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.CatchUpDialog
import tech.salev.optimum.ui.components.TimeLogBottomSheet
import tech.salev.optimum.ui.components.home.*
import tech.salev.optimum.ui.model.ActiveSlotInfo
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import java.time.LocalDate

/**
 * Home screen — orchestrates the daily/multi-day time-slot grid.
 *
 * Supports deliberate 1-second pull-down-and-hold gesture to reveal the hidden filter drawer.
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

    var isFilterVisible by rememberSaveable { mutableStateOf(false) }
    var activeSlotForLogging by remember { mutableStateOf<ActiveSlotInfo?>(null) }
    var showCatchUpDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope2 = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current

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

    val hasActiveFilters = selectedFilterCategoryId.isNotEmpty() || selectedFilterActivityId.isNotEmpty()

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Zaman Çizelgesi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Günlük aktivite takibi ⏱️",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                .pointerInput(isFilterVisible, categories.isNotEmpty()) {
                    if (isFilterVisible || categories.isEmpty()) return@pointerInput
                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isDown = true
                            var holdJob: kotlinx.coroutines.Job? = null

                            try {
                                while (isDown) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        isDown = false
                                        holdJob?.cancel()
                                        break
                                    }

                                    val dragY = change.position.y - down.position.y
                                    val dragX = kotlin.math.abs(change.position.x - down.position.x)

                                    // User pulls down significantly and vertically (not horizontal swipe)
                                    if (dragY > 60f && dragY > dragX * 1.2f) {
                                        if (holdJob == null && !isFilterVisible) {
                                            holdJob = this@coroutineScope.launch {
                                                delay(1000L) // 1 second hold requirement
                                                isFilterVisible = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    } else if (dragY < 15f) {
                                        // Released tension or scrolled up
                                        holdJob?.cancel()
                                        holdJob = null
                                    }
                                }
                            } finally {
                                holdJob?.cancel()
                            }
                        }
                    }
                }
        ) {
            // Preset Range Selector (Günlük, 1 Hafta, 1 Ay, Özel)
            DaysSlider(
                daysToView = daysToView,
                onDaysChanged = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetDaysToView(it)) }
            )

            // Date Navigation
            DateNavigationBar(
                currentDate = currentDateStr,
                onDateSelected = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetSelectedDate(it)) }
            )

            // Collapsible Filter Drawer (Revealed via pull-down or filter button)
            FilterBar(
                isVisible = isFilterVisible,
                onClose = { isFilterVisible = false },
                categories = categories,
                activities = activities,
                selectedCategoryIds = selectedFilterCategoryId,
                selectedActivityIds = selectedFilterActivityId,
                onCategoryFilterChanged = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetCategoryFilter(it)) },
                onActivityFilterChanged = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetActivityFilter(it)) }
            )

            // Active Filter Indicator Pill (Visible when filters are active but panel is closed)
            if (hasActiveFilters && !isFilterVisible) {
                Surface(
                    onClick = { isFilterVisible = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            val totalFilterCount = selectedFilterCategoryId.size + selectedFilterActivityId.size
                            Text(
                                text = "Aktif Filtre ($totalFilterCount seçim) • Düzenlemek için dokunun",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetCategoryFilter(emptySet()))
                                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.SetActivityFilter(emptySet()))
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Filtreleri Temizle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

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
                                coroutineScope2.launch {
                                    snackbarHostState.showSnackbar("Bu zaman dilimi henüz tamamlanmadı.")
                                }
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
                                coroutineScope2.launch {
                                    snackbarHostState.showSnackbar("Bu zaman dilimi henüz tamamlanmadı.")
                                }
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
                val logId = slot.log?.id ?: 0L
                homeViewModel.onEvent(
                    tech.salev.optimum.ui.viewmodel.HomeEvent.LogTimeSlot(
                        date = editedDate,
                        startTime = editedStart,
                        endTime = editedEnd,
                        categoryId = categoryId,
                        activityId = activityId,
                        note = note,
                        logId = logId
                    )
                )
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                activeSlotForLogging = null
            },
            onDeleteLog = {
                val deleteDate = slot.log?.date ?: slot.date
                val deleteStart = slot.log?.startTime ?: slot.start
                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.DeleteTimeLog(deleteDate, deleteStart))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                activeSlotForLogging = null
            },
            onAddCategory = { name ->
                categoryManagerViewModel.addCategory(
                    name,
                    name.take(1).uppercase(),
                    "#3F51B5",
                    true
                )
            },
            onAddActivity = { catId, name ->
                categoryManagerViewModel.addActivity(catId, name)
            }
        )
    }

    if (showCatchUpDialog) {
        CatchUpDialog(
            unloggedSlots = unloggedPastSlots,
            currentDate = currentDateStr,
            categories = categories,
            activities = activities,
            onDismiss = { showCatchUpDialog = false },
            onSaveBatch = { batchLogs ->
                batchLogs.forEach { log ->
                    homeViewModel.onEvent(
                        tech.salev.optimum.ui.viewmodel.HomeEvent.LogTimeSlot(
                            date = log.date,
                            startTime = log.startTime,
                            endTime = log.endTime,
                            categoryId = log.categoryId,
                            activityId = log.activityId,
                            note = log.note,
                            logId = 0L
                        )
                    )
                }
                showCatchUpDialog = false
                coroutineScope2.launch {
                    snackbarHostState.showSnackbar("${batchLogs.size} zaman dilimi kaydedildi.")
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.ClearError) },
            title = { Text("Hata") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.ClearError) }) {
                    Text("Tamam")
                }
            }
        )
    }
}
