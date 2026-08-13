package tech.salev.optimum.ui.components.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import tech.salev.optimum.ui.model.ActiveSlotInfo
import tech.salev.optimum.ui.model.MultiDayRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MultiDayGridView(
    multiDayRows: ImmutableList<MultiDayRow>,
    onSlotClick: (ActiveSlotInfo) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
        Column(modifier = Modifier.fillMaxHeight()) {

            // Date header row
            if (multiDayRows.isNotEmpty()) {
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(
                        modifier = Modifier.width(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Saat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    multiDayRows.first().cells.forEach { cell ->
                        val date = runCatching { LocalDate.parse(cell.dateStr) }.getOrDefault(LocalDate.now())
                        val isToday = date == LocalDate.now()
                        val dayName = when (date.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> "Pzt"
                            java.time.DayOfWeek.TUESDAY -> "Sal"
                            java.time.DayOfWeek.WEDNESDAY -> "Çar"
                            java.time.DayOfWeek.THURSDAY -> "Per"
                            java.time.DayOfWeek.FRIDAY -> "Cum"
                            java.time.DayOfWeek.SATURDAY -> "Cmt"
                            java.time.DayOfWeek.SUNDAY -> "Paz"
                            else -> ""
                        }
                        Column(
                            modifier = Modifier.width(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("dd/MM")),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(multiDayRows, key = { it.startStr }) { row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.startStr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(70.dp)
                        )
                        row.cells.forEach { cell ->
                            Box(modifier = Modifier.width(48.dp).padding(end = 4.dp)) {
                                GridCellCompact(
                                    log = cell.log,
                                    category = cell.category,
                                    activity = cell.activity,
                                    isPastEmpty = cell.isPastEmpty,
                                    isFuture = cell.isFuture,
                                    isFilteredOut = cell.isFilteredOut,
                                    onClick = {
                                        onSlotClick(
                                            ActiveSlotInfo(
                                                date = cell.dateStr,
                                                start = row.startStr,
                                                end = row.endStr,
                                                log = cell.log,
                                                isFuture = cell.isFuture
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(64.dp)) }
            }
        }
    }
}
