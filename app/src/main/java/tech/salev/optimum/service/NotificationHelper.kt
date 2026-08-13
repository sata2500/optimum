package tech.salev.optimum.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import tech.salev.optimum.data.local.dataStore
import tech.salev.optimum.data.repository.SettingsRepository

object NotificationHelper {
    const val CHANNEL_ID = "optimum_reminders_channel_v2"
    const val CHANNEL_NAME = "Optimum Zaman Takip Hatırlatıcıları"
    const val DEFAULT_NOTIFICATION_ID = 1001

    const val ACTION_OPEN_LOG = "tech.salev.optimum.ACTION_OPEN_LOG"
    const val ACTION_SNOOZE = "tech.salev.optimum.ACTION_SNOOZE"

    /**
     * Returns the active channel ID based on the custom ringtone setting.
     * Must be called from a coroutine (suspend).
     */
    private suspend fun getChannelId(context: Context): String {
        val uriStr = context.dataStore.data
            .map { it[SettingsRepository.KEY_CUSTOM_RINGTONE] }
            .first()
        return if (uriStr == null) CHANNEL_ID else "${CHANNEL_ID}_custom_${uriStr.hashCode()}"
    }

    /**
     * Deletes all existing Optimum notification channels and recreates the active one.
     * Must be called from a coroutine (suspend). Performs I/O on Dispatchers.IO.
     */
    suspend fun recreateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            withContext(Dispatchers.IO) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notificationChannels.forEach {
                    if (it.id.startsWith(CHANNEL_ID)) {
                        notificationManager.deleteNotificationChannel(it.id)
                    }
                }
                createNotificationChannelInternal(context, notificationManager)
            }
        }
    }

    /**
     * Creates the notification channel if it does not already exist.
     * Must be called from a coroutine (suspend). Performs I/O on Dispatchers.IO.
     */
    suspend fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            withContext(Dispatchers.IO) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createNotificationChannelInternal(context, notificationManager)
            }
        }
    }

    private suspend fun createNotificationChannelInternal(
        context: Context,
        notificationManager: NotificationManager
    ) {
        val uriStr = context.dataStore.data
            .map { it[SettingsRepository.KEY_CUSTOM_RINGTONE] }
            .first()
        val activeChannelId = if (uriStr == null) CHANNEL_ID
                              else "${CHANNEL_ID}_custom_${uriStr.hashCode()}"

        val channel = NotificationChannel(
            activeChannelId,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Optimum geçmiş zaman dilimi aktivite kaydı hatırlatıcıları"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            if (uriStr != null) {
                val uri = android.net.Uri.parse(uriStr)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(uri, audioAttributes)
            }
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Shows the periodic reminder notification.
     * Must be called from a coroutine (suspend). All DataStore reads are done on Dispatchers.IO.
     */
    suspend fun showReminderNotification(context: Context, intervalMinutes: Int = 30) {
        withContext(Dispatchers.IO) {
            createNotificationChannel(context)

            val prefs = context.dataStore.data.first()
            val isLongRingtoneEnabled = prefs[SettingsRepository.KEY_LONG_RINGTONE] ?: false
            val activeChannelId = getChannelId(context)

            // Content intent → opens QuickLogActivity (Modal)
            val openIntent = Intent(
                context,
                tech.salev.optimum.QuickLogActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Snooze intent → postpones for 15 minutes
            val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("INTERVAL_MINUTES", intervalMinutes)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle("Optimum Zaman Kaydı ⏱️")
                .bigText(
                    "Son $intervalMinutes dakikanız nasıl geçti? " +
                    "Hemen kaydedin ve zaman çetelenizi güncel tutun."
                )

            val builder = NotificationCompat.Builder(context, activeChannelId)
                .setSmallIcon(tech.salev.optimum.R.drawable.ic_stat_optimum)
                .setColor(android.graphics.Color.parseColor("#D4AF37"))
                .setContentTitle("Optimum Zaman Kaydı ⏱️")
                .setContentText("Son $intervalMinutes dakikanız nasıl geçti? Zamanınızı kaydedin.")
                .setStyle(bigTextStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_add,
                    "Şimdi Kaydet",
                    openPendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_recent_history,
                    "15 Dk Ertele",
                    snoozePendingIntent
                )

            if (isLongRingtoneEnabled) {
                builder.setTimeoutAfter(60_000L) // 1 dakika
            }

            val notification = builder.build()

            if (isLongRingtoneEnabled) {
                notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Cancel the previous notification to force a fresh alert
            cancelLastNotification(context, notificationManager)

            // Generate a new unique ID and persist it
            val newId = System.currentTimeMillis().toInt()
            context.dataStore.edit { it[SettingsRepository.KEY_LAST_NOTIFICATION_ID] = newId }

            notificationManager.notify(newId, notification)
        }
    }

    /**
     * Cancels the last shown notification using the stored ID.
     * Must be called from a coroutine (suspend).
     */
    suspend fun cancelLastNotification(context: Context) {
        withContext(Dispatchers.IO) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            cancelLastNotification(context, notificationManager)
        }
    }

    private suspend fun cancelLastNotification(
        context: Context,
        notificationManager: NotificationManager
    ) {
        val lastId = context.dataStore.data
            .map { it[SettingsRepository.KEY_LAST_NOTIFICATION_ID] ?: DEFAULT_NOTIFICATION_ID }
            .first()
        notificationManager.cancel(lastId)
    }
}
