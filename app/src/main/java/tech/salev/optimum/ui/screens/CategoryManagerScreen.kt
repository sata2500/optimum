package tech.salev.optimum.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.ui.components.ActivityDialog
import tech.salev.optimum.ui.components.CategoryDialog
import tech.salev.optimum.ui.components.ReorderActivitiesDialog
import tech.salev.optimum.ui.viewmodel.CategoryManagerViewModel
import tech.salev.optimum.ui.viewmodel.OptimumViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagerScreen(
    viewModel: OptimumViewModel,
    categoryManagerViewModel: CategoryManagerViewModel
) {
    val categoriesWithActivities by categoryManagerViewModel.categoriesWithActivities.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    var targetCategoryForActivity by remember { mutableStateOf<Category?>(null) }
    var editingActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var reorderingCategoryActivities by remember { mutableStateOf<Category?>(null) }

    // Track which categories are expanded in accordion
    var expandedCategoryIds by remember { mutableStateOf(setOf<Long>()) }

    // Reorderable State for Categories
    val categoryList = remember(categoriesWithActivities) { mutableStateOf(categoriesWithActivities.map { it.category }) }

    LaunchedEffect(categoriesWithActivities) {
        val mapped = categoriesWithActivities.map { it.category }
        if (categoryList.value != mapped) {
            categoryList.value = mapped
        }
    }

    val lazyListState = rememberLazyListState()
    val state = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Index offset accounts for headers (Spacer + Column header)
        val fromIdx = from.index - 2
        val toIdx = to.index - 2
        if (fromIdx >= 0 && toIdx >= 0 && fromIdx < categoryList.value.size && toIdx < categoryList.value.size) {
            val list = categoryList.value.toMutableList()
            val item = list.removeAt(fromIdx)
            list.add(toIdx, item)
            categoryList.value = list
            categoryManagerViewModel.reorderCategories(list)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kategori & Aktiviteler 🏷️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zaman takip çetelesi tanımlarınız",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Kategori Ekle") },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (categoriesWithActivities.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Henüz Kategori Tanımlanmadı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zamanınızı takip etmek için örneğin 'Eğitim', 'Çarşı', 'İbadet' gibi kategoriler oluşturun.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("İlk Kategorini Ekle")
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Table Header: KOD | KATEGORİLER
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KOD",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = "KATEGORİLER",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "İŞLEMLER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    items(categoryList.value, key = { it.id }) { category ->
                        ReorderableItem(state, key = category.id) { isDragging ->
                            val elevation = if (isDragging) 8.dp else 0.dp
                            val categoryActivities = categoriesWithActivities.find { it.category.id == category.id }?.activities ?: emptyList()
                            val isExpanded = expandedCategoryIds.contains(category.id)

                            CategoryCardItem(
                                category = category,
                                activities = categoryActivities,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedCategoryIds = if (isExpanded) {
                                        expandedCategoryIds - category.id
                                    } else {
                                        expandedCategoryIds + category.id
                                    }
                                },
                                modifier = Modifier.longPressDraggableHandle(),
                                elevation = elevation,
                                onEditCategory = { editingCategory = category },
                                onDeleteCategory = { categoryManagerViewModel.deleteCategory(category) },
                                onAddActivity = {
                                    targetCategoryForActivity = category
                                    // Also expand category when adding activity
                                    expandedCategoryIds = expandedCategoryIds + category.id
                                },
                                onEditActivity = { activity ->
                                    targetCategoryForActivity = category
                                    editingActivity = activity
                                },
                                onDeleteActivity = { activity ->
                                    categoryManagerViewModel.deleteActivity(activity)
                                },
                                onReorderActivities = {
                                    reorderingCategoryActivities = category
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddCategoryDialog) {
        CategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSaveCategory = { name, code, colorHex, isProductive ->
                categoryManagerViewModel.addCategory(name, code, colorHex, isProductive)
                showAddCategoryDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryDialog(
            initialCategory = category,
            onDismiss = { editingCategory = null },
            onSaveCategory = { name, code, colorHex, isProductive ->
                categoryManagerViewModel.updateCategory(
                    category.copy(
                        name = name,
                        code = code,
                        colorHex = colorHex,
                        isProductive = isProductive
                    )
                )
                editingCategory = null
            }
        )
    }

    targetCategoryForActivity?.let { category ->
        ActivityDialog(
            initialActivity = editingActivity,
            targetCategory = category,
            onDismiss = {
                targetCategoryForActivity = null
                editingActivity = null
            },
            onSaveActivity = { name, description, shortCode, colorHex ->
                if (editingActivity != null) {
                    categoryManagerViewModel.updateActivity(
                        editingActivity!!.copy(name = name, description = description, shortCode = shortCode, colorHex = colorHex)
                    )
                } else {
                    categoryManagerViewModel.addActivity(category.id, name, description, shortCode, colorHex)
                }
                targetCategoryForActivity = null
                editingActivity = null
            }
        )
    }

    reorderingCategoryActivities?.let { category ->
        val catActs = categoriesWithActivities.find { it.category.id == category.id }?.activities ?: emptyList()
        ReorderActivitiesDialog(
            category = category,
            activities = catActs,
            onDismiss = { reorderingCategoryActivities = null },
            onSave = { reorderedList ->
                categoryManagerViewModel.reorderActivities(reorderedList)
                reorderingCategoryActivities = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryCardItem(
    category: Category,
    activities: List<ActivityItem>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onAddActivity: () -> Unit,
    onEditActivity: (ActivityItem) -> Unit,
    onDeleteActivity: (ActivityItem) -> Unit,
    onReorderActivities: () -> Unit
) {
    val catColor = category.composeColor
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isExpanded) catColor else catColor.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Category Header Row - Clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Prominent Category Color Strip Indicator
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(catColor)
                    )

                    // Category Code Solid Badge
                    Surface(
                        color = catColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.widthIn(min = 38.dp)
                    ) {
                        Text(
                            text = category.code,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Category Name & Activity Count
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activities.size} aktivite",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Action buttons & Expand indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onAddActivity,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Aktivite Ekle",
                            tint = catColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEditCategory,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Kategoriyi Düzenle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteCategory,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Kategoriyi Sil",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Kapat" else "Aç",
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(arrowRotation),
                            tint = catColor
                        )
                    }
                }
            }

            // Accordion Content: Sub-activities (AnimatedVisibility)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(catColor.copy(alpha = 0.05f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = catColor.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktiviteler (${activities.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                        if (activities.size > 1) {
                            TextButton(
                                onClick = onReorderActivities,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sırala", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (activities.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Henüz aktivite eklenmedi. Aşağıdaki butondan ekleyebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        activities.forEach { activity ->
                            val displayCode = activity.getDisplayCode(category.code)
                            val actColor = activity.composeColor

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
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
                                        Column {
                                            Text(
                                                text = activity.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
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

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onEditActivity(activity) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Aktiviteyi Düzenle",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteActivity(activity) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Aktiviteyi Sil",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onAddActivity,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = catColor.copy(alpha = 0.15f),
                            contentColor = catColor
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aktivite Ekle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
