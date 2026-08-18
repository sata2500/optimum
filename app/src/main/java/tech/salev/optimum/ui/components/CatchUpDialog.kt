package tech.salev.optimum.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchUpDialog(
    unloggedSlots: List<Pair<String, String>>, // list of (startTime, endTime)
    currentDate: String,
    categories: List<Category>,
    activities: List<ActivityItem>,
    onDismiss: () -> Unit,
    onSaveBatch: (List<TimeSlotLog>) -> Unit
) {
    if (unloggedSlots.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentSlot = unloggedSlots.getOrNull(currentIndex) ?: return

    // Map storing user choices for each slot index: index -> (categoryId, activityId, note)
    val slotSelections = remember {
        mutableStateMapOf<Int, Triple<Long, Long, String>>()
    }

    // Last selected category & activity for auto-carry-over across slots
    var lastSelectedCategoryId by remember { mutableLongStateOf(categories.firstOrNull()?.id ?: 0L) }
    var lastSelectedActivityId by remember {
        mutableLongStateOf(activities.firstOrNull { it.categoryId == (categories.firstOrNull()?.id ?: 0L) }?.id ?: 0L)
    }

    val currentSelection = slotSelections[currentIndex]

    var selectedCategoryId by remember(currentIndex, currentSelection, categories) {
        mutableLongStateOf(
            currentSelection?.first
                ?: if (lastSelectedCategoryId != 0L) lastSelectedCategoryId
                else categories.firstOrNull()?.id ?: 0L
        )
    }

    val selectedCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }
    }

    val filteredActivities = remember(selectedCategoryId, activities) {
        activities.filter { it.categoryId == selectedCategoryId }
    }

    var selectedActivityId by remember(currentIndex, currentSelection, filteredActivities) {
        mutableLongStateOf(
            currentSelection?.second
                ?: if (filteredActivities.any { it.id == lastSelectedActivityId }) lastSelectedActivityId
                else filteredActivities.firstOrNull()?.id ?: 0L
        )
    }

    var noteText by remember(currentIndex, currentSelection) {
        mutableStateOf(currentSelection?.third ?: "")
    }

    // Automatically initialize slot selection if not yet recorded
    LaunchedEffect(currentIndex, selectedCategoryId, selectedActivityId, noteText) {
        if (selectedCategoryId != 0L && selectedActivityId != 0L) {
            slotSelections[currentIndex] = Triple(selectedCategoryId, selectedActivityId, noteText)
            lastSelectedCategoryId = selectedCategoryId
            lastSelectedActivityId = selectedActivityId
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Toplu Zaman Doldurma",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kaçırılan slotları hızlıca tamamlayın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${currentIndex + 1} / ${unloggedSlots.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / unloggedSlots.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                // Current Slot Time Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = (selectedCategory?.composeColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, (selectedCategory?.composeColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "İşlenen Zaman Dilimi:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${currentSlot.first} - ${currentSlot.second}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = selectedCategory?.composeColor ?: MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (categories.isEmpty()) {
                    Text(
                        text = "Henüz kategori bulunmuyor. Önce kategorilerinizi oluşturun.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // 1. Kategori Seçimi (Yatay Çipler)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. Kategori",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories, key = { it.id }) { category ->
                                val isSelected = category.id == selectedCategoryId
                                val catColor = category.composeColor

                                Surface(
                                    modifier = Modifier
                                        .clickable {
                                            selectedCategoryId = category.id
                                            lastSelectedCategoryId = category.id
                                            val newFiltered = activities.filter { it.categoryId == category.id }
                                            selectedActivityId = newFiltered.firstOrNull()?.id ?: 0L
                                            lastSelectedActivityId = selectedActivityId
                                            slotSelections[currentIndex] = Triple(category.id, selectedActivityId, noteText)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) catColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = category.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) Color.White else catColor
                                        )
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Alt Aktivite Seçimi (Dikey/Kompakt Liste)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "2. Aktivite",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (filteredActivities.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Bu kategoride henüz aktivite tanımlanmamış.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredActivities, key = { it.id }) { activity ->
                                    val isSelected = activity.id == selectedActivityId
                                    val actColor = activity.composeColor
                                    val displayCode = activity.getDisplayCode(selectedCategory?.code ?: "A")

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedActivityId = activity.id
                                                lastSelectedActivityId = activity.id
                                                slotSelections[currentIndex] = Triple(selectedCategoryId, activity.id, noteText)
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) actColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (isSelected) 1.8.dp else 1.dp,
                                            if (isSelected) actColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    color = actColor,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = displayCode,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Text(
                                                    text = activity.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Seçildi",
                                                    tint = actColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // İsteğe Bağlı Not
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            if (selectedCategoryId != 0L && selectedActivityId != 0L) {
                                slotSelections[currentIndex] = Triple(selectedCategoryId, selectedActivityId, it)
                            }
                        },
                        label = { Text("Not (İsteğe Bağlı)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Navigasyon ve Buton Kontrolleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Geri")
                    }

                    TextButton(
                        onClick = {
                            slotSelections.remove(currentIndex)
                            if (currentIndex < unloggedSlots.size - 1) {
                                currentIndex++
                            }
                        }
                    ) {
                        Text("Boş Geç")
                    }

                    if (currentIndex < unloggedSlots.size - 1) {
                        Button(
                            onClick = {
                                if (selectedCategoryId != 0L && selectedActivityId != 0L) {
                                    slotSelections[currentIndex] = Triple(selectedCategoryId, selectedActivityId, noteText)
                                    lastSelectedCategoryId = selectedCategoryId
                                    lastSelectedActivityId = selectedActivityId
                                }
                                currentIndex++
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sonraki")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedCategoryId != 0L && selectedActivityId != 0L) {
                                    slotSelections[currentIndex] = Triple(selectedCategoryId, selectedActivityId, noteText)
                                }

                                val resultLogs = slotSelections.mapNotNull { (idx, selection) ->
                                    val slot = unloggedSlots.getOrNull(idx) ?: return@mapNotNull null
                                    TimeSlotLog(
                                        date = currentDate,
                                        startTime = slot.first,
                                        endTime = slot.second,
                                        categoryId = selection.first,
                                        activityId = selection.second,
                                        note = selection.third
                                    )
                                }

                                onSaveBatch(resultLogs)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bitir", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Erken Kaydet (Tümünü bitirmeden kaydetmek isteyenler için)
                if (slotSelections.isNotEmpty() && currentIndex < unloggedSlots.size - 1) {
                    TextButton(
                        onClick = {
                            if (selectedCategoryId != 0L && selectedActivityId != 0L) {
                                slotSelections[currentIndex] = Triple(selectedCategoryId, selectedActivityId, noteText)
                            }
                            val resultLogs = slotSelections.mapNotNull { (idx, selection) ->
                                val slot = unloggedSlots.getOrNull(idx) ?: return@mapNotNull null
                                TimeSlotLog(
                                    date = currentDate,
                                    startTime = slot.first,
                                    endTime = slot.second,
                                    categoryId = selection.first,
                                    activityId = selection.second,
                                    note = selection.third
                                )
                            }
                            onSaveBatch(resultLogs)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Şu Ana Kadar Olanları Kaydet & Çık", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
