package tech.salev.optimum.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog
import java.util.Calendar

enum class TimeLogStep {
    SELECT_CATEGORY,
    SELECT_ACTIVITY
}

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
    var selectedCategoryId by remember(existingLog) {
        mutableLongStateOf(existingLog?.categoryId ?: 0L)
    }

    val selectedCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }
    }

    val filteredActivities = remember(selectedCategoryId, activities) {
        activities.filter { it.categoryId == selectedCategoryId }
    }

    var selectedActivityId by remember(existingLog) {
        mutableLongStateOf(existingLog?.activityId ?: 0L)
    }

    val selectedActivity = remember(selectedActivityId, activities) {
        activities.find { it.id == selectedActivityId }
    }

    var isSelectionCollapsed by remember(existingLog) {
        mutableStateOf(existingLog != null && existingLog.categoryId != 0L && existingLog.activityId != 0L)
    }

    var noteText by remember(existingLog) {
        mutableStateOf(existingLog?.note ?: "")
    }

    var currentStep by remember(existingLog) {
        mutableStateOf(
            if (existingLog != null && existingLog.categoryId != 0L) TimeLogStep.SELECT_ACTIVITY else TimeLogStep.SELECT_CATEGORY
        )
    }

    var editedDate by remember(date, existingLog) { mutableStateOf(existingLog?.date ?: date) }
    var editedStartTime by remember(startTime, existingLog) { mutableStateOf(existingLog?.startTime ?: startTime) }
    var editedEndTime by remember(endTime, existingLog) { mutableStateOf(existingLog?.endTime ?: endTime) }

    val context = LocalContext.current

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newActivityName by remember { mutableStateOf("") }
    var isTimeEditExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Time Details and Delete Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            fontWeight = FontWeight.SemiBold,
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                Text(
                                    text = editedDate,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
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
                                Text(
                                    text = editedStartTime,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
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
                                Text(
                                    text = editedEndTime,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existingLog != null && onDeleteLog != null) {
                        IconButton(onClick = onDeleteLog) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.outline
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
                // Collapsed Summary Card or Full Drill-Down Selection Flow
                if (isSelectionCollapsed && selectedCategory != null && selectedActivity != null) {
                    val catColor = selectedCategory.composeColor
                    val actColor = selectedActivity.composeColor
                    val displayCode = selectedActivity.getDisplayCode(selectedCategory.code)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isSelectionCollapsed = false
                                currentStep = TimeLogStep.SELECT_ACTIVITY
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = catColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.2.dp, catColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Category Tag
                                Surface(
                                    color = catColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = selectedCategory.code,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = selectedCategory.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )

                                // Activity Tag
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
                                    text = selectedActivity.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = catColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Değiştir",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = catColor
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Full Selection Flow with Collapsing Action
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (selectedCategory != null && selectedActivity != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { isSelectionCollapsed = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Seçimi Daralt ▴", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        // Two-step Drill-Down Selection Flow (Matching Sketch 1 & 4)
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                if (targetState == TimeLogStep.SELECT_ACTIVITY) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width } + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }
                            },
                            label = "SelectionStep"
                        ) { step ->
                            when (step) {
                                TimeLogStep.SELECT_CATEGORY -> {
                                    // Step 1: List Categories in Order with Vivid Colors
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "1. Kategori Seçiniz",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (onAddCategory != null) {
                                                TextButton(onClick = { showAddCategoryDialog = true }) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Yeni Kategori")
                                                }
                                            }
                                        }

                                        // Category Table Header
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "KOD",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(50.dp)
                                                )
                                                Text(
                                                    text = "KATEGORİLER",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 180.dp, max = 450.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(categories, key = { it.id }) { category ->
                                                val isSelected = category.id == selectedCategoryId
                                                val catColor = category.composeColor

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCategoryId = category.id
                                                            val newFiltered = activities.filter { it.categoryId == category.id }
                                                            if (activities.find { it.id == selectedActivityId }?.categoryId != category.id) {
                                                                selectedActivityId = 0L
                                                            }
                                                            currentStep = TimeLogStep.SELECT_ACTIVITY
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) catColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(
                                                        1.2.dp,
                                                        if (isSelected) catColor else catColor.copy(alpha = 0.35f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            // Left accent color strip
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(4.dp)
                                                                    .height(20.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(catColor)
                                                            )

                                                            // Category Code badge
                                                            Surface(
                                                                color = catColor,
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.widthIn(min = 34.dp)
                                                            ) {
                                                                Text(
                                                                    text = category.code,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = Color.White,
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                )
                                                            }

                                                            Text(
                                                                text = category.name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }

                                                        Icon(
                                                            imageVector = Icons.Default.ChevronRight,
                                                            contentDescription = "Aktiviteleri Aç",
                                                            tint = catColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                TimeLogStep.SELECT_ACTIVITY -> {
                                    // Step 2: Show activities under selected Category (Sketch 4)
                                    val catColor = selectedCategory?.composeColor ?: MaterialTheme.colorScheme.primary

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Breadcrumb / Category Header Banner
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { currentStep = TimeLogStep.SELECT_CATEGORY },
                                            shape = RoundedCornerShape(10.dp),
                                            color = catColor.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Kategoriyi Değiştir",
                                                        tint = catColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "Kategori:",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                    Surface(
                                                        color = catColor,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = selectedCategory?.code ?: "-",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                    Text(
                                                        text = selectedCategory?.name ?: "-",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = catColor
                                                    )
                                                }

                                                Text(
                                                    text = "Değiştir ↺",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catColor
                                                )
                                            }
                                        }

                                        // Activities Table Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "2. Aktivite Seçiniz",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (onAddActivity != null && selectedCategoryId != 0L) {
                                                TextButton(onClick = { showAddActivityDialog = true }) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Yeni Aktivite")
                                                }
                                            }
                                        }

                                        if (filteredActivities.isEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "Bu kategori altında tanımlı aktivite bulunamadı.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                    if (onAddActivity != null) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Button(
                                                            onClick = { showAddActivityDialog = true },
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Aktivite Ekle")
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Table Header: AKTİVİTE KODU | AKTİVİTE ADI (Sketch 4)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "KOD",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.width(60.dp)
                                                    )
                                                    Text(
                                                        text = "AKTİVİTE ADI",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 180.dp, max = 450.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(filteredActivities, key = { it.id }) { activity ->
                                                    val isSelected = activity.id == selectedActivityId
                                                    val displayCode = activity.getDisplayCode(selectedCategory?.code ?: "A")
                                                    val actColor = activity.composeColor

                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedActivityId = activity.id
                                                                // Automatically collapse to give space to note/description!
                                                                isSelectionCollapsed = true
                                                            },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSelected) actColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(
                                                            if (isSelected) 2.dp else 1.dp,
                                                            if (isSelected) actColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Surface(
                                                                    color = actColor,
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    modifier = Modifier.widthIn(min = 40.dp)
                                                                ) {
                                                                    Text(
                                                                        text = displayCode,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White,
                                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                    )
                                                                }

                                                                Column {
                                                                    Text(
                                                                        text = activity.name,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    if (activity.description.isNotBlank()) {
                                                                        Text(
                                                                            text = activity.description,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.outline
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.CheckCircle,
                                                                    contentDescription = "Seçildi",
                                                                    tint = actColor,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Not / Açıklama (İsteğe Bağlı)") },
                    placeholder = { Text("Bu zaman diliminde neler yaptınız? Not veya detay ekleyin...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (isSelectionCollapsed) 3 else 1,
                    maxLines = 6,
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

            Spacer(modifier = Modifier.height(12.dp))
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
