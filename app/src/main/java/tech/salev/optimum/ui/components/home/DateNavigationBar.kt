package tech.salev.optimum.ui.components.home

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Date header bar with previous/next day chevrons and a tappable date label.
 *
 * Tapping the date label opens a system [DatePickerDialog] capped at today,
 * so users cannot accidentally navigate to future dates.
 *
 * @param currentDate Currently selected date (ISO-8601 string, e.g. "2026-08-11").
 * @param onDateSelected Called with the new ISO-8601 date string when the user
 *                       navigates or picks a date.
 */
@Composable
fun DateNavigationBar(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsed = runCatching { LocalDate.parse(currentDate) }.getOrDefault(LocalDate.now())
    val isToday = parsed == LocalDate.now()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onDateSelected(parsed.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Gün")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(parsed.year, parsed.monthValue - 1, parsed.dayOfMonth)
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onDateSelected(
                                LocalDate.of(year, month + 1, day)
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                        },
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH),
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    ).apply {
                        datePicker.maxDate = System.currentTimeMillis()
                    }.show()
                }
            ) {
                Text(
                    text = parsed.format(DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isToday) {
                    Text(
                        text = "Bugün",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = {
                    val next = parsed.plusDays(1)
                    if (!next.isAfter(LocalDate.now())) {
                        onDateSelected(next.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                },
                enabled = !isToday
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Gün")
            }
        }
    }
}
