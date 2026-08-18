package tech.salev.optimum.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.salev.optimum.data.repository.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        // Use goAsync() so the BroadcastReceiver doesn't get killed before
        // the coroutine finishes. The pending result keeps the process alive.
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                when (intent.action) {
                    "TRIGGER_REMINDER" -> {
                        if (settingsRepository.isNotificationsEnabled.first()) {
                            val intervalMinutes = settingsRepository.intervalMinutes.first()
                            
                            // AlarmManager'ın döngüsel çalışması için bir sonraki periyodu her şeyden ÖNCE kuruyoruz.
                            ReminderScheduler.schedulePeriodicReminder(context, intervalMinutes)
                            
                            val startStr = settingsRepository.dayStartTime.first()
                            val endStr = settingsRepository.dayEndTime.first()
                            
                            val start = tech.salev.optimum.util.TimeUtils.parseTime(startStr)
                            val end = tech.salev.optimum.util.TimeUtils.parseTime(endStr)
                            val now = java.time.LocalTime.now()
                            
                            if (tech.salev.optimum.util.TimeUtils.isTimeInRange(now, start, end)) {
                                NotificationHelper.cancelLastNotification(context)
                                NotificationHelper.showReminderNotification(context, intervalMinutes)
                            }
                        }
                    }
                    NotificationHelper.ACTION_SNOOZE -> {
                        val intervalMinutes = intent.getIntExtra("INTERVAL_MINUTES", 30)
                        if (settingsRepository.isNotificationsEnabled.first()) {
                            ReminderScheduler.snoozeReminder(context, 15, intervalMinutes)
                        }
                        NotificationHelper.cancelLastNotification(context)
                    }
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
