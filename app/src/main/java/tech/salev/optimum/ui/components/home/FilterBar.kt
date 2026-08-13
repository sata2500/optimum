package tech.salev.optimum.ui.components.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.util.ColorUtils

/**
 * Expandable filter panel with category and activity chip rows.
 *
 * The panel stays collapsed by default. When the user taps the header it
 * expands to reveal two [LazyRow]s — one for categories, one for the
 * activities belonging to the selected categories.
 *
 * Selections are multi-select: tapping a chip toggles membership in the
 * filter set. "Tümü" resets everything.
 */
@Composable
fun FilterBar(
    categories: ImmutableList<Category>,
    activities: ImmutableList<ActivityItem>,
    selectedCategoryIds: Set<Long>,
    selectedActivityIds: Set<Long>,
    onCategoryFilterChanged: (Set<Long>) -> Unit,
    onActivityFilterChanged: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Collapse / expand header ──────────────────────────────────────
        Surface(
            onClick = { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Filtreleri Gizle" else "Filtreleri Göster",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Filtreler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Expanded content ──────────────────────────────────────────────
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChipRow(
                    categories = categories,
                    selectedIds = selectedCategoryIds,
                    onSelectionChanged = { newSet ->
                        onCategoryFilterChanged(newSet)
                        onActivityFilterChanged(emptySet()) // reset activity filter on category change
                    }
                )

                val visibleActivities = remember(selectedCategoryIds, activities) {
                    if (selectedCategoryIds.isEmpty()) activities
                    else activities.filter { selectedCategoryIds.contains(it.categoryId) }
                }

                if (visibleActivities.isNotEmpty()) {
                    ActivityChipRow(
                        activities = visibleActivities,
                        categories = categories,
                        selectedIds = selectedActivityIds,
                        onSelectionChanged = onActivityFilterChanged
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryChipRow(
    categories: ImmutableList<Category>,
    selectedIds: Set<Long>,
    onSelectionChanged: (Set<Long>) -> Unit
) {
    Text(
        "Kategoriler",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChipItem(
                label = "Tümü",
                isSelected = selectedIds.isEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                onClick = { onSelectionChanged(emptySet()) }
            )
        }
        items(categories, key = { it.id }) { category ->
            val color = ColorUtils.parse(category.colorHex)
            FilterChipItem(
                label = category.name,
                isSelected = selectedIds.contains(category.id),
                color = color,
                dotColor = color,
                onClick = {
                    val newSet = if (selectedIds.contains(category.id))
                        selectedIds - category.id else selectedIds + category.id
                    onSelectionChanged(newSet)
                }
            )
        }
    }
}

@Composable
private fun ActivityChipRow(
    activities: List<ActivityItem>,
    categories: ImmutableList<Category>,
    selectedIds: Set<Long>,
    onSelectionChanged: (Set<Long>) -> Unit
) {
    Text(
        "Aktiviteler",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChipItem(
                label = "Tümü",
                isSelected = selectedIds.isEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                onClick = { onSelectionChanged(emptySet()) }
            )
        }
        items(activities, key = { it.id }) { activity ->
            val category = remember(activity.categoryId, categories) {
                categories.find { it.id == activity.categoryId }
            }
            val color = ColorUtils.parse(category?.colorHex)
            val displayCode = activity.getDisplayCode(category?.code ?: "")

            Surface(
                selected = selectedIds.contains(activity.id),
                onClick = {
                    val newSet = if (selectedIds.contains(activity.id))
                        selectedIds - activity.id else selectedIds + activity.id
                    onSelectionChanged(newSet)
                },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedIds.contains(activity.id)) color.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (selectedIds.contains(activity.id))
                             BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                ) {
                    Surface(shape = CircleShape, color = color, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = activity.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIds.contains(activity.id)) color
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    color: Color,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
