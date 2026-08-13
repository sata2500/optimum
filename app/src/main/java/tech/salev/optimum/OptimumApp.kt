package tech.salev.optimum

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.salev.optimum.service.NotificationHelper
import tech.salev.optimum.service.ReminderScheduler

@HiltAndroidApp
class OptimumApp : Application() {

    // Application-level scope that lives as long as the process.
    // Used only for one-time initialization tasks (channel creation).
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Create the notification channel (requires DataStore, hence suspend)
            NotificationHelper.createNotificationChannel(this@OptimumApp)
        }
        // Schedule the periodic reminder (non-suspend, AlarmManager)
        ReminderScheduler.schedulePeriodicReminder(this, 30)
    }
}
