package tech.salev.optimum.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.settings.*
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.ui.viewmodel.SettingsEvent
import tech.salev.optimum.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OptimumViewModel,
    settingsViewModel: SettingsViewModel,
    evaluationViewModel: tech.salev.optimum.ui.viewmodel.EvaluationViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val currentLogs by viewModel.currentLogs.collectAsStateWithLifecycle()
    val allEvaluations by evaluationViewModel.allEvaluations.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val backupData = tech.salev.optimum.util.DataImporter.parseJsonBackup(context, uri)
                    viewModel.restoreFullBackup(backupData)
                    Toast.makeText(context, "Veriler başarıyla içe aktarıldı!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ayarlar ⚙️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bildirim ve Zamanlama Tercihleri",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            NotificationCard(
                isNotificationsEnabled = uiState.isNotificationsEnabled,
                isLongRingtoneEnabled = uiState.isLongRingtoneEnabled,
                customRingtoneUri = uiState.customRingtone,
                onNotificationsToggled = { settingsViewModel.onEvent(SettingsEvent.SetNotificationsEnabled(it)) },
                onLongRingtoneToggled = { settingsViewModel.onEvent(SettingsEvent.SetLongRingtoneEnabled(it)) },
                onCustomRingtoneSelected = { settingsViewModel.onEvent(SettingsEvent.SetCustomRingtone(it)) },
                onTestNotification = {
                    coroutineScope.launch {
                        tech.salev.optimum.service.NotificationHelper.showReminderNotification(context, uiState.intervalMinutes)
                        Toast.makeText(context, "Test bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            IntervalCard(
                currentInterval = uiState.intervalMinutes,
                onApply = { minutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetInterval(minutes))
                    Toast.makeText(context, "Periyot $minutes dakika olarak güncellendi", Toast.LENGTH_SHORT).show()
                }
            )

            TimeRangeCard(
                dayStartTime = uiState.dayStartTime,
                dayEndTime = uiState.dayEndTime,
                onApply = { start, end ->
                    settingsViewModel.onEvent(SettingsEvent.SetStartEndTime(start, end))
                    Toast.makeText(context, "Saatler güncellendi", Toast.LENGTH_SHORT).show()
                }
            )

            ThemeCard(
                themeMode = uiState.themeMode,
                onThemeSelected = { settingsViewModel.onEvent(SettingsEvent.SetThemeMode(it)) }
            )

            DataExportCard(
                onExportLogsCsv = {
                    coroutineScope.launch {
                        tech.salev.optimum.util.DataExporter.exportLogsAsCsv(context, currentLogs, categories, activities)
                    }
                },
                onExportEvaluationsCsv = {
                    Toast.makeText(context, "Yakında eklenecek!", Toast.LENGTH_SHORT).show()
                },
                onExportFullJson = {
                    coroutineScope.launch {
                        tech.salev.optimum.util.DataExporter.exportFullBackupAsJson(
                            context,
                            currentLogs,
                            categories,
                            activities,
                            allEvaluations
                        )
                    }
                },
                onImportFullJson = {
                    importLauncher.launch("application/json")
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
