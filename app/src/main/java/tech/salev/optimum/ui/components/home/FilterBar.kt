package tech.salev.optimum.ui.components.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Filter panel revealed via pull-down gesture on the timeline.
 * Hidden by default to keep the main view clean and distraction-free.
 */
@Composable
fun FilterBar(
    isVisible: Boolean,
    onClose: () -> Unit,
    categories: ImmutableList<Category>,
    activities: ImmutableList<ActivityItem>,
    selectedCategoryIds: Set<Long>,
    selectedActivityIds: Set<Long>,
    onCategoryFilterChanged: (Set<Long>) -> Unit,
    onActivityFilterChanged: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val hasActiveFilters = selectedCategoryIds.isNotEmpty() || selectedActivityIds.isNotEmpty()

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with title, clear button, and close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Filtreler",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasActiveFilters) {
                            TextButton(
                                onClick = {
                                    onCategoryFilterChanged(emptySet())
                                    onActivityFilterChanged(emptySet())
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Temizle",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Filtreleri Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Category Filter Row
                CategoryChipRow(
                    categories = categories,
                    selectedIds = selectedCategoryIds,
                    onSelectionChanged = { newSet ->
                        onCategoryFilterChanged(newSet)
                        onActivityFilterChanged(emptySet()) // reset activity filter on category change
                    }
                )

                // Activity Filter Row
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
        text = "Kategoriler",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
        text = "Aktiviteler",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                shape = RoundedCornerShape(14.dp),
                color = if (selectedIds.contains(activity.id)) color.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = if (selectedIds.contains(activity.id))
                             BorderStroke(1.dp, color.copy(alpha = 0.6f)) else null,
                modifier = Modifier.height(30.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, end = 10.dp)
                ) {
                    Surface(shape = CircleShape, color = color, modifier = Modifier.size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = activity.name,
                        style = MaterialTheme.typography.labelSmall,
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
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.6f)) else null,
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
