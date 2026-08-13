package tech.salev.optimum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.CatchUpDialog
import tech.salev.optimum.ui.theme.OptimumTheme
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.ui.viewmodel.SettingsViewModel

@AndroidEntryPoint
class QuickLogActivity : ComponentActivity() {

    private val viewModel: OptimumViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val homeViewModel: tech.salev.optimum.ui.viewmodel.HomeViewModel by viewModels()
    private val categoryManagerViewModel: tech.salev.optimum.ui.viewmodel.CategoryManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            OptimumTheme {
                val uiState by homeViewModel.uiState.collectAsState()
                val categories = uiState.categories
                val activities = uiState.activities
                val unloggedSlots = uiState.unloggedPastSlots
                val intervalMinutes = uiState.intervalMinutes
                
                var showDialog by remember { mutableStateOf(true) }
                val coroutineScope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    if (showDialog && categories.isNotEmpty() && activities.isNotEmpty()) {
                        val currentDateStr = uiState.currentDateStr
                        val targetSlot = tech.salev.optimum.util.TimeSlotCalculator.getMostRecentlyCompletedSlot(
                            currentDateStr = currentDateStr,
                            intervalMinutes = intervalMinutes
                        )
                        val startStr = targetSlot.startStr
                        val endStr = targetSlot.endStr
                        val targetDateStr = targetSlot.targetDateStr
                                                
                        val currentLogs by viewModel.currentLogs.collectAsState()
                        val existingLog = currentLogs.find { it.startTime == startStr }

                        tech.salev.optimum.ui.components.TimeLogBottomSheet(
                            startTime = startStr,
                            endTime = endStr,
                            date = targetDateStr,
                            existingLog = existingLog,
                            categories = categories,
                            activities = activities,
                            onDismiss = {
                                showDialog = false
                                finish()
                            },
                            onSaveLog = { editedDate, editedStart, editedEnd, categoryId, activityId, note ->
                                val logId = existingLog?.id ?: 0L
                                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.LogTimeSlot(editedDate, editedStart, editedEnd, categoryId, activityId, note, logId))
                                
                                coroutineScope.launch {
                                    val notificationId = settingsViewModel.getLastNotificationId()
                                    val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                    notificationManager.cancel(notificationId)
                                    showDialog = false
                                    finish()
                                }
                            },
                            onDeleteLog = {
                                homeViewModel.onEvent(tech.salev.optimum.ui.viewmodel.HomeEvent.DeleteTimeLog(targetDateStr, startStr))
                                
                                coroutineScope.launch {
                                    val notificationId = settingsViewModel.getLastNotificationId()
                                    val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                    notificationManager.cancel(notificationId)
                                    showDialog = false
                                    finish()
                                }
                            },
                            onAddCategory = { name ->
                                categoryManagerViewModel.addCategory(name, name.take(1).uppercase(), "#3F51B5", true)
                            },
                            onAddActivity = { catId, name ->
                                categoryManagerViewModel.addActivity(catId, name)
                            }
                        )
                    } else if (showDialog && (categories.isEmpty() || activities.isEmpty())) {
                        AlertDialog(
                            onDismissRequest = { finish() },
                            title = { Text("Uyarı") },
                            text = { Text("Kategori veya aktivite bulunamadı. Lütfen önce uygulamadan kategori ve aktivite ekleyin.") },
                            confirmButton = {
                                TextButton(onClick = { finish() }) {
                                    Text("Tamam")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

