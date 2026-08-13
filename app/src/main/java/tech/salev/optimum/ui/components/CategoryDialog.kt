package tech.salev.optimum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDialog(
    initialCategory: Category? = null,
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, code: String, colorHex: String, isProductive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var code by remember { mutableStateOf(initialCategory?.code ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialCategory?.colorHex ?: CategoryColors.first()) }
    var isProductive by remember { mutableStateOf(initialCategory?.isProductive ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (initialCategory == null) "Yeni Kategori Ekle 🏷️" else "Kategoriyi Düzenle ✏️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it 
                        if (it.isNotBlank() && (code.isBlank() || code.length == 1)) {
                            code = it.take(1).uppercase()
                        }
                    },
                    label = { Text("Kategori Adı (Örn: Eğitim)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 3) code = it },
                    label = { Text("Kısa Kod (Örn: E)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Renk Seçiniz",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CategoryColors) { hex ->
                        val displayColor = tech.salev.optimum.util.ColorUtils.parse(hex)
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(displayColor)
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Verimli Zaman Katkısı",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isProductive) "Odaklanma / İş zekası" else "Mola / Dinlenme / Sosyal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isProductive,
                        onCheckedChange = { isProductive = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val finalCode = if (code.isNotBlank()) code.trim() else name.trim().take(1).uppercase()
                                onSaveCategory(name.trim(), finalCode, selectedColorHex, isProductive)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

