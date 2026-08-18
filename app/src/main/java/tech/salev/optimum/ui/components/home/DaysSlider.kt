package tech.salev.optimum.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class TimelinePreset {
    DAILY,      // 1 Gün
    WEEKLY,     // 7 Gün
    MONTHLY,    // 30 Gün
    CUSTOM      // Özel (1-30 Gün)
}

/**
 * Modern Segmented Control for timeline range selection:
 * 1. Günlük (1 Gün)
 * 2. 1 Hafta (7 Gün)
 * 3. 1 Ay (30 Gün)
 * 4. Özel (Manuel gün girişi, max 30 gün)
 *
 * Guarantees zero line-wrapping on all screen sizes with responsive typography.
 */
@Composable
fun DaysSlider(
    daysToView: Int,
    onDaysChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDaysDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val currentPreset = when (daysToView) {
        1 -> TimelinePreset.DAILY
        7 -> TimelinePreset.WEEKLY
        30 -> TimelinePreset.MONTHLY
        else -> TimelinePreset.CUSTOM
    }

    val customLabel = if (currentPreset == TimelinePreset.CUSTOM) "Özel ($daysToView G)" else "Özel"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preset 1: Günlük
            SegmentPillItem(
                label = "Günlük",
                isSelected = currentPreset == TimelinePreset.DAILY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDaysChanged(1)
                },
                modifier = Modifier.weight(1f)
            )

            // Preset 2: 1 Hafta
            SegmentPillItem(
                label = "1 Hafta",
                isSelected = currentPreset == TimelinePreset.WEEKLY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDaysChanged(7)
                },
                modifier = Modifier.weight(1f)
            )

            // Preset 3: 1 Ay
            SegmentPillItem(
                label = "1 Ay",
                isSelected = currentPreset == TimelinePreset.MONTHLY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDaysChanged(30)
                },
                modifier = Modifier.weight(1f)
            )

            // Preset 4: Özel
            SegmentPillItem(
                label = customLabel,
                isSelected = currentPreset == TimelinePreset.CUSTOM,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCustomDaysDialog = true
                },
                modifier = Modifier.weight(1.05f)
            )
        }
    }

    if (showCustomDaysDialog) {
        CustomDaysDialog(
            initialDays = daysToView,
            onDismiss = { showCustomDaysDialog = false },
            onConfirm = { selectedDays ->
                onDaysChanged(selectedDays)
                showCustomDaysDialog = false
            }
        )
    }
}

@Composable
private fun SegmentPillItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "segmentBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "segmentText"
    )

    Surface(
        shape = RoundedCornerShape(9.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomDaysDialog(
    initialDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var tempDaysFloat by remember { mutableFloatStateOf(initialDays.coerceIn(1, 30).toFloat()) }
    var textInput by remember { mutableStateOf(initialDays.coerceIn(1, 30).toString()) }

    fun updateDays(newDays: Int) {
        val clamped = newDays.coerceIn(1, 30)
        tempDaysFloat = clamped.toFloat()
        textInput = clamped.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Özel Çizelge Aralığı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tabloda geriye dönük kaç gün görüntülemek istediğinizi belirleyin (1 - 30 gün). Sayıya dokunarak klavyeden yazabilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Large Days Number Display with Direct Keyboard Input + Steppers
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val current = textInput.toIntOrNull() ?: tempDaysFloat.roundToInt()
                                if (current > 1) updateDays(current - 1)
                            },
                            enabled = (textInput.toIntOrNull() ?: tempDaysFloat.roundToInt()) > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Azalt")
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp)
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    textInput = digits
                                    val num = digits.toIntOrNull()
                                    if (num != null) {
                                        val clamped = num.coerceIn(1, 30)
                                        tempDaysFloat = clamped.toFloat()
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(56.dp)
                            )
                            Text(
                                text = "GÜN (1-30)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val current = textInput.toIntOrNull() ?: tempDaysFloat.roundToInt()
                                if (current < 30) updateDays(current + 1)
                            },
                            enabled = (textInput.toIntOrNull() ?: tempDaysFloat.roundToInt()) < 30
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Artır")
                        }
                    }
                }

                // Interactive Slider
                Slider(
                    value = tempDaysFloat,
                    onValueChange = {
                        tempDaysFloat = it
                        textInput = it.roundToInt().toString()
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets row (Optimized for all screen sizes: 3G, 5G, 14G)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 14).forEach { quickDay ->
                        val isCurrent = (textInput.toIntOrNull() ?: tempDaysFloat.roundToInt()) == quickDay
                        SuggestionChip(
                            onClick = { updateDays(quickDay) },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$quickDay G",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDays = (textInput.toIntOrNull() ?: tempDaysFloat.roundToInt()).coerceIn(1, 30)
                    onConfirm(finalDays)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Uygula", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
