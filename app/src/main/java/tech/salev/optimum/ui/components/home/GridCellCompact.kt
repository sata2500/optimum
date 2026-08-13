package tech.salev.optimum.ui.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.TimeSlotLog
import tech.salev.optimum.util.ColorUtils

/**
 * A compact cell representing a single time slot in the grid view.
 * Displays the activity code if logged, or a warning icon if past and empty.
 */
@Composable
fun GridCellCompact(
    log: TimeSlotLog?,
    category: Category?,
    activity: ActivityItem?,
    isPastEmpty: Boolean,
    isFuture: Boolean,
    isFilteredOut: Boolean,
    onClick: () -> Unit
) {
    val catColor = remember(category) {
        ColorUtils.parse(category?.colorHex) ?: Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(enabled = !isFilteredOut) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isFilteredOut) Color.Transparent
        else if (log != null) catColor.copy(alpha = 0.2f)
        else if (isPastEmpty) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = if (isFilteredOut) null
        else if (isPastEmpty) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        else if (log != null) BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
        else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (log != null && activity != null) {
                Text(
                    text = activity.getDisplayCode(category?.code ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            } else if (isPastEmpty) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Eksik Kayıt",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
