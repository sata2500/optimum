package tech.salev.optimum.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun HeatmapCalendar(
    data: List<Pair<String, Int>>, // Pair of Date String and Minutes
    modifier: Modifier = Modifier,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    baseColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    // Find the max value to calculate alpha intensity
    val maxMinutes = data.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Let's assume a 7-day week (rows) and calculate columns
        val rows = 4 // e.g. 4 weeks shown
        val cols = 7 // 7 days a week
        
        val cellPadding = 4f
        val cellWidth = (width - (cols - 1) * cellPadding) / cols
        val cellHeight = (height - (rows - 1) * cellPadding) / rows
        
        val cellSize = minOf(cellWidth, cellHeight)
        
        // Center the grid
        val startX = (width - (cols * cellSize + (cols - 1) * cellPadding)) / 2
        val startY = (height - (rows * cellSize + (rows - 1) * cellPadding)) / 2

        var dataIndex = 0
        // Draw from left to right (columns), top to bottom (rows)
        for (col in 0 until cols) {
            for (row in 0 until rows) {
                val x = startX + col * (cellSize + cellPadding)
                val y = startY + row * (cellSize + cellPadding)
                
                // Get data value if available
                val minutes = if (dataIndex < data.size) data[dataIndex].second else 0
                dataIndex++
                
                // Calculate color intensity
                val color = if (minutes == 0) {
                    emptyColor
                } else {
                    val intensity = (minutes.toFloat() / maxMinutes).coerceIn(0.2f, 1.0f)
                    baseColor.copy(alpha = intensity)
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}
