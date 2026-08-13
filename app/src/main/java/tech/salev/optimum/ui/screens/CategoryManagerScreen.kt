package tech.salev.optimum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.ui.components.ActivityDialog
import tech.salev.optimum.ui.components.CategoryDialog
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagerScreen(viewModel: OptimumViewModel, categoryManagerViewModel: tech.salev.optimum.ui.viewmodel.CategoryManagerViewModel) {
    val categoriesWithActivities by categoryManagerViewModel.categoriesWithActivities.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    var targetCategoryForActivity by remember { mutableStateOf<Category?>(null) }
    var editingActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var reorderingCategoryActivities by remember { mutableStateOf<Category?>(null) }

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
        val list = categoryList.value.toMutableList()
        val item = list.removeAt(from.index - 1) // -1 because of the Spacer item at index 0
        list.add(to.index - 1, item)
        categoryList.value = list
        categoryManagerViewModel.reorderCategories(list)
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
                        text = "Zamanınızı takip etmek için örneğin 'Eğitim', 'Market', 'İbadet' gibi kategoriler oluşturun.",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(categoryList.value, key = { it.id }) { category ->
                        ReorderableItem(state, key = category.id) { isDragging ->
                            val elevation = if (isDragging) 8.dp else 0.dp
                            val categoryActivities = categoriesWithActivities.find { it.category.id == category.id }?.activities ?: emptyList()

                            CategoryCardItem(
                                category = category,
                                activities = categoryActivities,
                                modifier = Modifier.longPressDraggableHandle(),
                                elevation = elevation,
                                onEditCategory = { editingCategory = category },
                                onDeleteCategory = { categoryManagerViewModel.deleteCategory(category) },
                                onAddActivity = { targetCategoryForActivity = category },
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
        tech.salev.optimum.ui.components.ReorderActivitiesDialog(
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )

                    Surface(
                        color = catColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = category.code,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                    }

                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onReorderActivities) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Aktiviteleri Sırala", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEditCategory) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteCategory) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Activities List
            Text(
                text = "Aktiviteler (${activities.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            if (activities.isEmpty()) {
                Text(
                    text = "Henüz tanımlı aktivite yok. Aşağıdaki butondan ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activities.forEach { activity ->
                        val displayCode = activity.getDisplayCode(category.code)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val actColor = activity.composeColor
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { onEditActivity(activity) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onDeleteActivity(activity) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddActivity,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aktivite Ekle")
            }
        }
    }
}
