package tech.salev.optimum.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.settings.*
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.ui.viewmodel.ProfileViewModel
import tech.salev.optimum.ui.viewmodel.SettingsEvent
import tech.salev.optimum.ui.viewmodel.SettingsViewModel

private const val WEB_DASHBOARD_URL = "https://optimum-gilt-five.vercel.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OptimumViewModel,
    settingsViewModel: SettingsViewModel,
    evaluationViewModel: tech.salev.optimum.ui.viewmodel.EvaluationViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val allEvaluations by evaluationViewModel.allEvaluations.collectAsStateWithLifecycle()

    val userProfile by profileViewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by profileViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by profileViewModel.syncStateMessage.collectAsStateWithLifecycle()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.data != null) {
            profileViewModel.handleLegacySignInResult(result.data)
        } else {
            profileViewModel.cancelSignIn()
        }
    }

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
                            text = "Hesap, Bulut ve Tercihler",
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

            // ══════════════════════════════════════════════════════════════════
            // ── 1. PROFIL & BULUT SENKRONIZASYONU KARTI ──
            // ══════════════════════════════════════════════════════════════════
            if (!userProfile.isLoggedIn) {
                // Unauthenticated State Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "Google ile Giriş Yap & Buluta Bağlan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Zaman çizelgenizi ve verilerinizi güvenle yedekleyin. Bilgisayarınız üzerinden de tüm analiz ve çizelgelerinize erişebilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                profileViewModel.signInWithGoogle(context) { fallbackIntent ->
                                    googleSignInLauncher.launch(fallbackIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Google ile Giriş Yap",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Web Access Info
                        WebAccessBox(context = context)
                    }
                }
            } else {
                // Logged In User Profile & Cloud Sync Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!userProfile.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = "Profil Fotoğrafı",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userProfile.displayName.take(1).uppercase().ifBlank { "U" },
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = userProfile.displayName.ifBlank { "Optimum Kullanıcısı" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Google Hesabı Bağlı ✓",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Auto-Sync Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Otomatik Senkronizasyon",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Kayıt eklendiğinde arka planda buluta eşitler",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = uiState.isAutoSyncEnabled,
                                onCheckedChange = { settingsViewModel.onEvent(SettingsEvent.SetAutoSyncEnabled(it)) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Cloud Sync Status & Manual Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Son Senkronizasyon:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = userProfile.lastSyncTime ?: "Henüz yapılmadı",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { profileViewModel.syncCloudData() },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eşitleniyor...", fontSize = 14.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Şimdi Senkronize Et", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Web Access Card for logged in user
                        WebAccessBox(context = context)

                        // Sign Out Button
                        OutlinedButton(
                            onClick = { profileViewModel.signOut() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Hesaptan Çıkış Yap", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Sync message banner (soft and elegant)
            AnimatedVisibility(visible = syncMessage != null) {
                syncMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // ── 2. BILDIRIM AYARLARI ──
            // ══════════════════════════════════════════════════════════════════
            NotificationCard(
                isNotificationsEnabled = uiState.isNotificationsEnabled,
                isLongRingtoneEnabled = uiState.isLongRingtoneEnabled,
                isSilentNotificationEnabled = uiState.isSilentNotificationEnabled,
                silentAlertColor = uiState.silentAlertColor,
                customRingtoneUri = uiState.customRingtone,
                onNotificationsToggled = { settingsViewModel.onEvent(SettingsEvent.SetNotificationsEnabled(it)) },
                onLongRingtoneToggled = { settingsViewModel.onEvent(SettingsEvent.SetLongRingtoneEnabled(it)) },
                onSilentNotificationToggled = { settingsViewModel.onEvent(SettingsEvent.SetSilentNotificationEnabled(it)) },
                onSilentAlertColorChanged = { settingsViewModel.onEvent(SettingsEvent.SetSilentAlertColor(it)) },
                onCustomRingtoneSelected = { settingsViewModel.onEvent(SettingsEvent.SetCustomRingtone(it)) },
                onTestNotification = {
                    coroutineScope.launch {
                        tech.salev.optimum.service.NotificationHelper.showReminderNotification(context, uiState.intervalMinutes)
                        Toast.makeText(context, "Test bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // ══════════════════════════════════════════════════════════════════
            // ── 3. ZAMAN ARALIĞI VE PERİYOT ──
            // ══════════════════════════════════════════════════════════════════
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

            // ══════════════════════════════════════════════════════════════════
            // ── 4. TEMA ──
            // ══════════════════════════════════════════════════════════════════
            ThemeCard(
                themeMode = uiState.themeMode,
                onThemeSelected = { settingsViewModel.onEvent(SettingsEvent.SetThemeMode(it)) }
            )

            // ══════════════════════════════════════════════════════════════════
            // ── 5. VERI YEDEKLEME VE IÇE AKTARMA ──
            // ══════════════════════════════════════════════════════════════════
            DataExportCard(
                onExportLogsCsv = {
                    coroutineScope.launch {
                        val allLogs = viewModel.getAllLogs()
                        tech.salev.optimum.util.DataExporter.exportLogsAsCsv(context, allLogs, categories, activities)
                    }
                },
                onExportEvaluationsCsv = {
                    coroutineScope.launch {
                        tech.salev.optimum.util.DataExporter.exportEvaluationsAsCsv(context, allEvaluations)
                    }
                },
                onExportFullJson = {
                    coroutineScope.launch {
                        val allLogs = viewModel.getAllLogs()
                        tech.salev.optimum.util.DataExporter.exportFullBackupAsJson(
                            context,
                            allLogs,
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

/**
 * Web dashboard access card showing the URL and easy open/copy action.
 */
@Composable
private fun WebAccessBox(context: Context) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Optimum Web & Bilgisayar Erişimi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Çizelgenize ve detaylı analizlerinize bilgisayarınız üzerinden de erişebilirsiniz:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_DASHBOARD_URL))
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tarayıcı açılamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = WEB_DASHBOARD_URL,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Optimum Web URL", WEB_DASHBOARD_URL)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Bağlantı kopyalandı!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Kopyala",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
