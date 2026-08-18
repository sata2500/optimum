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
import androidx.compose.material3.VerticalDivider
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
 * Displays the start/end time, category, activity, and duration separated by vertical dividers.
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
        color = if (block.log != null) catColor.copy(alpha = 0.12f)
        else if (block.isPastEmpty) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = if (block.isCurrentSlot) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else if (block.log != null) BorderStroke(1.dp, catColor.copy(alpha = 0.3f))
        else if (block.isPastEmpty) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saat Sütunu (1.0f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.0f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (block.isFuture) Color.LightGray
                            else if (block.log != null) catColor
                            else if (block.isPastEmpty) MaterialTheme.colorScheme.error
                            else Color.Gray
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.Start) {
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

            // Dikey Ayırıcı 1
            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Kategori Sütunu (1.2f)
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.Center
            ) {
                if (block.category != null) {
                    Text(
                        text = block.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = catColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "[${block.category.code}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor.copy(alpha = 0.75f),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dikey Ayırıcı 2
            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Aktivite Sütunu (2.2f) - Geniş tutuldu
            Text(
                text = block.activity?.name ?: if (block.isPastEmpty) "Boş (Doldur)" else "-",
                style = MaterialTheme.typography.bodySmall,
                color = if (block.isPastEmpty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (block.isPastEmpty) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(2.2f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Dikey Ayırıcı 3
            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Süre Sütunu (0.7f)
            Text(
                text = "${block.slotCount * intervalMinutes} dk",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.End
            )
        }
    }
}
