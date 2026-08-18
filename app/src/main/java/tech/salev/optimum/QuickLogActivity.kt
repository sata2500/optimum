package tech.salev.optimum

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.TimeLogBottomSheet
import tech.salev.optimum.ui.theme.OptimumTheme
import tech.salev.optimum.ui.viewmodel.CategoryManagerViewModel
import tech.salev.optimum.ui.viewmodel.HomeEvent
import tech.salev.optimum.ui.viewmodel.HomeViewModel
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.ui.viewmodel.SettingsViewModel
import tech.salev.optimum.util.TimeSlotCalculator

@AndroidEntryPoint
class QuickLogActivity : ComponentActivity() {

    private val viewModel: OptimumViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val categoryManagerViewModel: CategoryManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen up and show over lock screen across all Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        setContent {
            OptimumTheme {
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val categories = uiState.categories
                val activities = uiState.activities
                val intervalMinutes = uiState.intervalMinutes

                val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                val isLongRingtone = settingsState.isLongRingtoneEnabled
                val alertColorHex = settingsState.silentAlertColor

                // Determine if visual alert / flash mode is triggered
                val isVisualAlertIntent = remember { intent.getBooleanExtra("IS_VISUAL_ALERT", false) }
                val isSilentMode = isVisualAlertIntent || settingsState.isSilentNotificationEnabled

                var showBottomSheet by remember {
                    mutableStateOf(!isVisualAlertIntent && !settingsState.isSilentNotificationEnabled)
                }

                val alertColor = remember(alertColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(alertColorHex))
                    } catch (e: Exception) {
                        Color(0xFFD4AF37)
                    }
                }

                // Handle screen brightness override during visual flash
                DisposableEffect(showBottomSheet, isSilentMode) {
                    if (isSilentMode && !showBottomSheet) {
                        try {
                            window.attributes = window.attributes.apply {
                                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    } else {
                        try {
                            window.attributes = window.attributes.apply {
                                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    onDispose {
                        try {
                            window.attributes = window.attributes.apply {
                                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }

                val currentDateStr = uiState.currentDateStr
                val targetSlot = remember(currentDateStr, intervalMinutes) {
                    TimeSlotCalculator.getMostRecentlyCompletedSlot(
                        currentDateStr = currentDateStr,
                        intervalMinutes = intervalMinutes
                    )
                }
                val startStr = targetSlot.startStr
                val endStr = targetSlot.endStr
                val targetDateStr = targetSlot.targetDateStr

                val currentLogs by viewModel.currentLogs.collectAsStateWithLifecycle()
                val existingLog = currentLogs.find { it.startTime == startStr }

                val coroutineScope = rememberCoroutineScope()

                fun dismissNotificationAndFinish() {
                    coroutineScope.launch {
                        val notificationId = settingsViewModel.getLastNotificationId()
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notificationId)
                        finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background) // Tema desteği — artık hardcoded değil
                ) {
                    if (isSilentMode && !showBottomSheet) {
                        // High-intensity full screen visual flash / pulse
                        VisualFlashScreen(
                            alertColor = alertColor,
                            isLongRingtone = isLongRingtone,
                            intervalMinutes = intervalMinutes,
                            timeRange = "$startStr - $endStr",
                            onAddActivity = {
                                showBottomSheet = true
                            },
                            onSnooze = {
                                tech.salev.optimum.service.ReminderScheduler.snoozeReminder(
                                    context = this@QuickLogActivity,
                                    snoozeMinutes = 15,
                                    originalIntervalMinutes = intervalMinutes
                                )
                                dismissNotificationAndFinish()
                            },
                            onDismiss = {
                                dismissNotificationAndFinish()
                            }
                        )
                    }

                    if (showBottomSheet && categories.isNotEmpty() && activities.isNotEmpty()) {
                        TimeLogBottomSheet(
                            startTime = startStr,
                            endTime = endStr,
                            date = targetDateStr,
                            existingLog = existingLog,
                            categories = categories,
                            activities = activities,
                            onDismiss = {
                                finish()
                            },
                            onSaveLog = { editedDate, editedStart, editedEnd, categoryId, activityId, note ->
                                val logId = existingLog?.id ?: 0L
                                homeViewModel.onEvent(
                                    HomeEvent.LogTimeSlot(
                                        editedDate,
                                        editedStart,
                                        editedEnd,
                                        categoryId,
                                        activityId,
                                        note,
                                        logId
                                    )
                                )
                                dismissNotificationAndFinish()
                            },
                            onDeleteLog = {
                                val deleteDate = existingLog?.date ?: targetDateStr
                                val deleteStart = existingLog?.startTime ?: startStr
                                homeViewModel.onEvent(
                                    HomeEvent.DeleteTimeLog(
                                        deleteDate,
                                        deleteStart
                                    )
                                )
                                dismissNotificationAndFinish()
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
                    } else if (showBottomSheet && (categories.isEmpty() || activities.isEmpty())) {
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

@Composable
private fun VisualFlashScreen(
    alertColor: Color,
    isLongRingtone: Boolean,
    intervalMinutes: Int,
    timeRange: String,
    onAddActivity: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var isFlashingActive by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isLongRingtone) {
        if (isLongRingtone) {
            // 60 saniye boyunca ısrarla flaş yap
            delay(60_000L)
            isFlashingActive = false
        } else {
            // 15 saniye boyunca aktif flaş
            delay(15_000L)
            isFlashingActive = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "FlashPulse")

    // Intense screen flash pulse (alpha transitioning rapidly)
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    // Pulsing outer aura scale
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    // Button pulse scale
    val btnScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )

    val currentAlpha = if (isFlashingActive) flashAlpha else 0.35f
    val currentScale = if (isFlashingActive) auraScale else 1.0f

    // Entire screen is interactive — tapping anywhere stops flash and opens log sheet!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        alertColor.copy(alpha = currentAlpha),
                        alertColor.copy(alpha = currentAlpha * 0.5f),
                        Color(0xFF07090E)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddActivity()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-intensity full screen flashing border frame with safe window insets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(8.dp)
                .border(
                    width = 4.dp,
                    color = alertColor.copy(alpha = if (isFlashingActive) currentAlpha else 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = alertColor.copy(alpha = 0.25f),
                border = BorderStroke(1.5.dp, alertColor.copy(alpha = 0.8f)),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isFlashingActive) "Sessiz Görsel Ekran Flaşı Aktif" else "Optimum Hatırlatıcısı",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Center Pulsing Visual Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    // Outer expanding glow wave
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(currentScale)
                            .border(
                                width = 4.dp,
                                color = alertColor.copy(alpha = currentAlpha * 0.9f),
                                shape = CircleShape
                            )
                    )

                    // Middle ambient aura
                    Box(
                        modifier = Modifier
                            .size(135.dp)
                            .clip(CircleShape)
                            .background(alertColor.copy(alpha = currentAlpha * 0.4f))
                    )

                    // Inner solid emblem
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = CircleShape,
                        color = alertColor,
                        shadowElevation = 12.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Zaman Kaydı Vakti! ⏱️",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, alertColor.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Son $intervalMinutes Dakika: $timeRange",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = alertColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "Aktivitenizi kaydetmek için ekrana veya aşağıdaki butona dokunun.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Bottom Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action Button (Large glowing button)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onAddActivity()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .scale(if (isFlashingActive) btnScale else 1.0f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = alertColor,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aktiviteyi Kaydet ⏱️",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("15 Dk Ertele", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kapat", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
