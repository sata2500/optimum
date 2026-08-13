package tech.salev.optimum.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Card component for setting daily tracking start and end times.
 */
@Composable
fun TimeRangeCard(
    dayStartTime: String,
    dayEndTime: String,
    onApply: (startTime: String, endTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Günlük Takip Saatleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tabloda gösterilecek ve bildirim gönderilecek başlangıç/bitiş saatlerini belirleyin (HH:mm).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            
            var startTimeText by remember(dayStartTime) { mutableStateOf(dayStartTime) }
            var endTimeText by remember(dayEndTime) { mutableStateOf(dayEndTime) }
            var isError by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startTimeText,
                    onValueChange = { startTimeText = it; isError = false },
                    label = { Text("Başlangıç") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = isError
                )
                OutlinedTextField(
                    value = endTimeText,
                    onValueChange = { endTimeText = it; isError = false },
                    label = { Text("Bitiş") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = isError
                )
            }
            
            if (isError) {
                Text(
                    text = "Geçersiz saat formatı (Örn: 08:30)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Button(
                onClick = {
                    if (startTimeText.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]\$")) &&
                        endTimeText.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]\$"))) {
                        onApply(startTimeText, endTimeText)
                        isError = false
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kaydet")
            }
        }
    }
}
