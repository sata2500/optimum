package tech.salev.optimum.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object ReminderScheduler {

    fun schedulePeriodicReminder(context: Context, intervalMinutes: Int = 30) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "TRIGGER_REMINDER"
            putExtra("INTERVAL_MINUTES", intervalMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = java.time.LocalDateTime.now()
        
        // Calculate the next exact boundary
        val remainder = now.minute % intervalMinutes
        val minutesToAdd = if (remainder == 0) intervalMinutes else (intervalMinutes - remainder)
        val nextBoundaryMinute = now.minute + minutesToAdd
        
        var nextTrigger = now.withMinute(nextBoundaryMinute % 60).withSecond(0).withNano(0)
        if (nextBoundaryMinute >= 60) {
            nextTrigger = nextTrigger.plusHours((nextBoundaryMinute / 60).toLong())
        }
            
        val triggerAtEpochMillis = nextTrigger.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intervalMillis = triggerAtEpochMillis - System.currentTimeMillis()
        val triggerAtMillis = SystemClock.elapsedRealtime() + intervalMillis

        try {
            val info = AlarmManager.AlarmClockInfo(triggerAtEpochMillis, pendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun snoozeReminder(context: Context, snoozeMinutes: Int = 15, originalIntervalMinutes: Int = 30) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "TRIGGER_REMINDER"
            putExtra("INTERVAL_MINUTES", originalIntervalMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100, // IMPORTANT: Use 100 to overwrite the already scheduled next regular alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeMillis = snoozeMinutes * 60 * 1000L
        val triggerAtMillis = SystemClock.elapsedRealtime() + snoozeMillis
        val triggerAtEpochMillis = System.currentTimeMillis() + snoozeMillis

        try {
            val info = AlarmManager.AlarmClockInfo(triggerAtEpochMillis, pendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationActionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
