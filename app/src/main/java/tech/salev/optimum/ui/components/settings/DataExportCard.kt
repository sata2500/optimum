package tech.salev.optimum.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Card component for data export actions.
 */
@Composable
fun DataExportCard(
    onExportLogsCsv: () -> Unit,
    onExportEvaluationsCsv: () -> Unit,
    onExportFullJson: () -> Unit,
    onImportFullJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📦 Veri Yönetimi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Verilerinizi CSV veya JSON formatında dışa aktarın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedButton(
                onClick = onExportLogsCsv,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kayıtları CSV Olarak Dışa Aktar")
            }

            OutlinedButton(
                onClick = onExportEvaluationsCsv,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Değerlendirmeleri CSV Olarak Dışa Aktar")
            }

            OutlinedButton(
                onClick = onExportFullJson,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tam Yedek JSON Olarak Dışa Aktar")
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onImportFullJson,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tam Yedek JSON İçe Aktar")
            }
        }
    }
}
