package tech.salev.optimum.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import tech.salev.optimum.ui.model.ActiveSlotInfo
import tech.salev.optimum.ui.model.MergedTimeBlock
import java.time.LocalDate

@Composable
fun DailyGridView(
    currentDateStr: String,
    currentDateParsed: LocalDate,
    intervalMinutes: Int,
    dailyMergedBlocks: ImmutableList<MergedTimeBlock>,
    onSlotClick: (ActiveSlotInfo) -> Unit
) {
    val isToday = currentDateParsed == LocalDate.now()
    val listState = rememberLazyListState()

    val currentSlotIndex = remember(dailyMergedBlocks) {
        dailyMergedBlocks.indexOfFirst { it.isCurrentSlot }.takeIf { it >= 0 }
            ?: dailyMergedBlocks.indexOfLast { it.isPastEmpty }.takeIf { it >= 0 }
    }

    LaunchedEffect(currentDateStr, currentSlotIndex) {
        if (isToday && currentSlotIndex != null && currentSlotIndex > 0) {
            listState.animateScrollToItem(maxOf(0, currentSlotIndex - 2))
        }
    }

    // Table header with vertical dividers
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Saat", modifier = Modifier.weight(1.0f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        androidx.compose.material3.VerticalDivider(
            modifier = Modifier.padding(horizontal = 6.dp).height(14.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        Text("Kategori", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        androidx.compose.material3.VerticalDivider(
            modifier = Modifier.padding(horizontal = 6.dp).height(14.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        Text("Aktiviteler", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        androidx.compose.material3.VerticalDivider(
            modifier = Modifier.padding(horizontal = 6.dp).height(14.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        Text("Süre", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(dailyMergedBlocks, key = { it.startTime }, contentType = { it.log != null }) { block ->
            if (block.isCurrentSlot && isToday) {
                NowIndicator(modifier = Modifier.padding(horizontal = 16.dp))
            }
            DailyTableRow(
                block = block,
                intervalMinutes = intervalMinutes,
                modifier = Modifier.animateItem(),
                onClick = {
                    onSlotClick(
                        ActiveSlotInfo(
                            date = currentDateStr,
                            start = block.startSlotStr,
                            end = block.endTime,
                            log = block.log,
                            isFuture = block.isFuture
                        )
                    )
                }
            )
        }
        item { Spacer(Modifier.height(64.dp)) }
    }
}
