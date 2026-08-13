package tech.salev.optimum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeLogBottomSheet(
    startTime: String,
    endTime: String,
    date: String,
    existingLog: TimeSlotLog?,
    categories: List<Category>,
    activities: List<ActivityItem>,
    onDismiss: () -> Unit,
    onSaveLog: (date: String, startTime: String, endTime: String, categoryId: Long, activityId: Long, note: String) -> Unit,
    onDeleteLog: (() -> Unit)? = null,
    onAddCategory: ((name: String) -> Unit)? = null,
    onAddActivity: ((categoryId: Long, name: String) -> Unit)? = null
) {
    var selectedCategoryId by remember(existingLog, categories) {
        mutableLongStateOf(existingLog?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }

    val filteredActivities = remember(selectedCategoryId, activities) {
        activities.filter { it.categoryId == selectedCategoryId }
    }

    var selectedActivityId by remember(existingLog, filteredActivities) {
        mutableLongStateOf(
            existingLog?.activityId ?: filteredActivities.firstOrNull()?.id ?: 0L
        )
    }

    var noteText by remember(existingLog) {
        mutableStateOf(existingLog?.note ?: "")
    }

    var editedDate by remember(date) { mutableStateOf(date) }
    var editedStartTime by remember(startTime) { mutableStateOf(startTime) }
    var editedEndTime by remember(endTime) { mutableStateOf(endTime) }

    val context = LocalContext.current

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newActivityName by remember { mutableStateOf("") }
    var isSelectionExpanded by remember { mutableStateOf(true) }
    var isTimeEditExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Zaman Dilimi Kaydı ⏱️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isTimeEditExpanded = !isTimeEditExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Zaman Dilimini Düzenle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (!isTimeEditExpanded) {
                        Text(
                            text = "$editedDate | $editedStartTime - $editedEndTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val calendar = Calendar.getInstance()
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                editedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        val startTimePickerDialog = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                editedStartTime = String.format("%02d:%02d", hourOfDay, minute)
                            },
                            editedStartTime.split(":").getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY),
                            editedStartTime.split(":").getOrNull(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE),
                            true
                        )

                        val endTimePickerDialog = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                editedEndTime = String.format("%02d:%02d", hourOfDay, minute)
                            },
                            editedEndTime.split(":").getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY),
                            editedEndTime.split(":").getOrNull(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE),
                            true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { 
                                    val parts = editedDate.split("-")
                                    if (parts.size == 3) {
                                        val y = parts[0].toIntOrNull()
                                        val m = parts[1].toIntOrNull()
                                        val d = parts[2].toIntOrNull()
                                        if (y != null && m != null && d != null) {
                                            datePickerDialog.updateDate(y, m - 1, d)
                                        }
                                    }
                                    datePickerDialog.show() 
                                }
                            ) {
                                Text(text = editedDate, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { 
                                    val parts = editedStartTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull()
                                    val m = parts.getOrNull(1)?.toIntOrNull()
                                    if (h != null && m != null) {
                                        startTimePickerDialog.updateTime(h, m)
                                    }
                                    startTimePickerDialog.show() 
                                }
                            ) {
                                Text(text = editedStartTime, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            
                            Text("-", style = MaterialTheme.typography.bodyMedium)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { 
                                    val parts = editedEndTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull()
                                    val m = parts.getOrNull(1)?.toIntOrNull()
                                    if (h != null && m != null) {
                                        endTimePickerDialog.updateTime(h, m)
                                    }
                                    endTimePickerDialog.show() 
                                }
                            ) {
                                Text(text = editedEndTime, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                if (existingLog != null && onDeleteLog != null) {
                    IconButton(onClick = onDeleteLog) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (categories.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Henüz tanımlı bir kategori yok! Lütfen önce Kategoriler sekmesinden en az bir kategori ekleyin.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                if (!isSelectionExpanded) {
                    val selectedCategory = categories.find { it.id == selectedCategoryId }
                    val selectedActivity = activities.find { it.id == selectedActivityId }
                    Surface(
                        onClick = { isSelectionExpanded = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Seçiminiz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${selectedCategory?.name ?: "-"} > ${selectedActivity?.name ?: "-"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Genişlet", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(visible = isSelectionExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "1. Kategori Seçiniz",
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
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor
                            )
                        )
                    }
                    
                    item {
                        if (onAddCategory != null) {
                            FilterChip(
                                selected = false,
                                onClick = { showAddCategoryDialog = true },
                                label = { Text("+ Yeni Ekle", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }

                Text(
                    text = "2. Aktivite Seçiniz",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                if (filteredActivities.isEmpty() && onAddActivity == null) {
                    Text(
                        text = "Bu kategori altında tanımlı aktivite bulunamadı.",
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

                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { 
                                    selectedActivityId = activity.id 
                                    isSelectionExpanded = false
                                },
                                label = {
                                    Text(text = "[$displayCode] ${activity.name}")
                                }
                            )
                        }
                        
                        item {
                            if (onAddActivity != null) {
                                ElevatedFilterChip(
                                    selected = false,
                                    onClick = { showAddActivityDialog = true },
                                    label = { Text("+ Yeni Ekle", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.elevatedFilterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                        }
                    }
                    }
                } // End of AnimatedVisibility

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Not / Açıklama (İsteğe Bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (isSelectionExpanded) 1 else 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (selectedCategoryId != 0L && selectedActivityId != 0L) {
                            coroutineScope.launch {
                                sheetState.hide()
                                onSaveLog(editedDate, editedStartTime, editedEndTime, selectedCategoryId, selectedActivityId, noteText)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = selectedCategoryId != 0L && selectedActivityId != 0L,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false; newCategoryName = "" },
            title = { Text("Yeni Kategori") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Kategori Adı") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onAddCategory?.invoke(newCategoryName.trim())
                        showAddCategoryDialog = false
                        newCategoryName = ""
                    }
                }) { Text("Ekle") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false; newCategoryName = "" }) { Text("İptal") }
            }
        )
    }

    if (showAddActivityDialog) {
        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false; newActivityName = "" },
            title = { Text("Yeni Aktivite") },
            text = {
                OutlinedTextField(
                    value = newActivityName,
                    onValueChange = { newActivityName = it },
                    label = { Text("Aktivite Adı") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newActivityName.isNotBlank() && selectedCategoryId != 0L) {
                        onAddActivity?.invoke(selectedCategoryId, newActivityName.trim())
                        showAddActivityDialog = false
                        newActivityName = ""
                    }
                }) { Text("Ekle") }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false; newActivityName = "" }) { Text("İptal") }
            }
        )
    }
}
