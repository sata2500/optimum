package tech.salev.optimum.ui.components.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Card component for notification settings and test trigger.
 */
@Composable
fun NotificationCard(
    isNotificationsEnabled: Boolean,
    isLongRingtoneEnabled: Boolean,
    isSilentNotificationEnabled: Boolean,
    silentAlertColor: String,
    customRingtoneUri: String?,
    onNotificationsToggled: (Boolean) -> Unit,
    onLongRingtoneToggled: (Boolean) -> Unit,
    onSilentNotificationToggled: (Boolean) -> Unit,
    onSilentAlertColorChanged: (String) -> Unit,
    onCustomRingtoneSelected: (String?) -> Unit,
    onTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ringtonePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: android.net.Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    android.net.Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            onCustomRingtoneSelected(uri?.toString())
        }
    }

    val alertColors = remember {
        listOf(
            "#D4AF37" to "Altın",
            "#E53935" to "Kırmızı",
            "#43A047" to "Yeşil",
            "#00ACC1" to "Turkuaz",
            "#8E24AA" to "Mor",
            "#FB8C00" to "Turuncu",
            "#3F51B5" to "İndigo",
            "#E91E63" to "Pembe"
        )
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Notifications Toggle
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Bildirimler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zaman takip hatırlatıcıları",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = onNotificationsToggled
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))

            // Silent Notification (Visual Color Flash Alert) Toggle
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sessiz Görsel Bildirim (Ekran Flaşı)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ses ve titreşim kapatılır; ekran uyanır ve seçilen renkte yanıp söner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = isSilentNotificationEnabled,
                        onCheckedChange = onSilentNotificationToggled,
                        enabled = isNotificationsEnabled
                    )
                }

                if (isSilentNotificationEnabled && isNotificationsEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Flaş & Ekran Uyarı Rengi:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Color Palette Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                alertColors.forEach { (hex, name) ->
                                    val parsedColor = try {
                                        Color(android.graphics.Color.parseColor(hex))
                                    } catch (e: Exception) {
                                        Color(0xFFD4AF37)
                                    }
                                    val isSelected = silentAlertColor.equals(hex, ignoreCase = true)

                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { onSilentAlertColorChanged(hex) },
                                        shape = CircleShape,
                                        color = parsedColor,
                                        border = BorderStroke(
                                            if (isSelected) 2.5.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                        )
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = name,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (isLongRingtoneEnabled) {
                                    "⚡ Telefon kapalı/kilitli olsa bile ekran uyanır ve 1 dakika boyunca ısrarla seçilen renkte yanıp söner."
                                } else {
                                    "⚡ Telefon kapalı/kilitli olsa bile ekran uyanır ve 10 kez seçilen renkte yanıp söner."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            // ── Android Özel İzin Kontrolleri (Görsel Flaş & Kilit Ekranı) ──
                            val notificationManager = remember(context) {
                                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            }
                            val hasFullScreenPermission = remember(context) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    notificationManager.canUseFullScreenIntent()
                                } else true
                            }
                            val hasOverlayPermission = remember(context) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Settings.canDrawOverlays(context)
                                } else true
                            }

                            if (!hasFullScreenPermission || !hasOverlayPermission) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ Görsel Flaş İçin Gerekli İzinler",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )

                                        if (!hasFullScreenPermission) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• Tam Ekran Bildirim İzni",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilledTonalButton(
                                                    onClick = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                            try {
                                                                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                                                    data = Uri.parse("package:${context.packageName}")
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                    data = Uri.parse("package:${context.packageName}")
                                                                }
                                                                context.startActivity(intent)
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("İzni Aç", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        if (!hasOverlayPermission) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• Diğer Uygulamaların Üzerinde Gösterme",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilledTonalButton(
                                                    onClick = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                            try {
                                                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                                                    data = Uri.parse("package:${context.packageName}")
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                    data = Uri.parse("package:${context.packageName}")
                                                                }
                                                                context.startActivity(intent)
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("İzni Aç", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Test Butonları ──
                            val scope = rememberCoroutineScope()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val testIntent = Intent(context, tech.salev.optimum.QuickLogActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            putExtra("IS_VISUAL_ALERT", true)
                                        }
                                        context.startActivity(testIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("⚡ Şimdi Test Et", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "5 saniye içinde ekranınızı kilitleyin...", Toast.LENGTH_LONG).show()
                                        scope.launch {
                                            delay(5000)
                                            tech.salev.optimum.service.NotificationHelper.showReminderNotification(context, 30)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🔒 5 Sn Sonra Test Et", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
            
            // Long Ringtone Toggle
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Uzun Bildirim Sesi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bildirimler 1 dakika boyunca ısrarla çalar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(
                    checked = isLongRingtoneEnabled,
                    onCheckedChange = onLongRingtoneToggled,
                    enabled = isNotificationsEnabled
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
            
            // Custom Ringtone Selector
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Özel Bildirim Sesi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (customRingtoneUri != null) "Özel ses seçildi" else "Varsayılan bildirim sesi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            val existingUri = customRingtoneUri?.let { android.net.Uri.parse(it) }
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    enabled = isNotificationsEnabled
                ) {
                    Text("Seç")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
            
            // Test Notification Trigger
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Bildirim Testi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Bildirim mekanizmasının cihazınızda sorunsuz çalıştığını doğrulamak için anlık test bildirimi gönderin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Test Bildirimi Gönder 🔔")
                }
            }
        }
    }
}
