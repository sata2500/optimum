package tech.salev.optimum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
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

    // Attempt to inherit from previous slot if current is null
    val currentSelection = slotSelections[currentIndex] ?: slotSelections[currentIndex - 1]

    var selectedCategoryId by remember(currentIndex, currentSelection, categories) {
        mutableLongStateOf(currentSelection?.first ?: categories.firstOrNull()?.id ?: 0L)
    }

    val filteredActivities = remember(selectedCategoryId, activities) {
        activities.filter { it.categoryId == selectedCategoryId }
    }

    var selectedActivityId by remember(currentIndex, currentSelection, filteredActivities) {
        mutableLongStateOf(
            currentSelection?.second ?: filteredActivities.firstOrNull()?.id ?: 0L
        )
    }

    var noteText by remember(currentIndex, currentSelection) {
        mutableStateOf(currentSelection?.third ?: "")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header & Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Toplu Zaman Doldurma Modu",
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

                // Time Slot Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zaman Dilimi:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${currentSlot.first} - ${currentSlot.second}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                    // 1. Select Category
                    Text(
                        text = "1. Kategori",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            val isSelected = category.id == selectedCategoryId
                            val catColor = category.composeColor

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryId = category.id
                                    val newFiltered = activities.filter { it.categoryId == category.id }
                                    selectedActivityId = newFiltered.firstOrNull()?.id ?: 0L
                                    slotSelections[currentIndex] = Triple(category.id, selectedActivityId, noteText)
                                },
                                label = {
                                    Text(
                                        text = "${category.code} - ${category.name}",
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor
                                )
                            )
                        }
                    }

                    // 2. Select Activity
                    Text(
                        text = "2. Aktivite",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (filteredActivities.isEmpty()) {
                        Text(
                            text = "Bu kategoride aktivite yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        ) {
                            items(filteredActivities) { activity ->
                                val isSelected = activity.id == selectedActivityId
                                val selectedCat = categories.find { it.id == selectedCategoryId }
                                val displayCode = activity.getDisplayCode(selectedCat?.code ?: "A")

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedActivityId = activity.id
                                        slotSelections[currentIndex] = Triple(selectedCategoryId, activity.id, noteText)
                                    },
                                    label = { Text("[$displayCode] ${activity.name}") }
                                )
                            }
                        }
                    }
                }

                // Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                                }
                                currentIndex++
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sonraki")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
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
                        Text("Şu Ana Kadar Olanları Kaydet & Çık")
                    }
                }
            }
        }
    }
}
