package tech.salev.optimum.ui.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.salev.optimum.ui.model.MergedTimeBlock
import tech.salev.optimum.util.ColorUtils

/**
 * A row representing a merged time block in the daily table view.
 * Displays the start/end time, category, activity, and duration.
 */
@Composable
fun DailyTableRow(
    block: MergedTimeBlock,
    intervalMinutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val catColor = remember(block.category) {
        ColorUtils.parse(block.category?.colorHex) ?: Color(0xFF64748B)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (block.log != null) catColor.copy(alpha = 0.15f)
        else if (block.isPastEmpty) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (block.isCurrentSlot) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else if (block.isPastEmpty) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) 
                 else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saat Sütunu (1.2f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.2f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (block.isFuture) Color.LightGray else if (block.log != null) catColor else if (block.isPastEmpty) MaterialTheme.colorScheme.error else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = block.startTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (block.isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = block.endTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (block.isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Kategori Sütunu (1.5f)
            Text(
                text = block.category?.name ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = if (block.category != null) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Aktivite Sütunu (1.5f)
            Text(
                text = block.activity?.name ?: if (block.isPastEmpty) "Boş (Doldur)" else "-",
                style = MaterialTheme.typography.bodySmall,
                color = if (block.isPastEmpty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (block.isPastEmpty) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Süre Sütunu (1f)
            Text(
                text = "${block.slotCount * intervalMinutes} dk",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End
            )
        }
    }
}
