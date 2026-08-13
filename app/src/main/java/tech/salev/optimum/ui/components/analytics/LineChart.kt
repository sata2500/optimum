package tech.salev.optimum.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import tech.salev.optimum.ui.model.DailyActivityData

@Composable
fun LineChart(
    data: List<DailyActivityData>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxPct = 100f
        
        val pointSpacing = if (data.size > 1) width / (data.size - 1) else width
        
        val path = Path()
        
        data.forEachIndexed { index, item ->
            val x = index * pointSpacing
            // y goes from top (0) to bottom (height), so invert the percentage
            val y = height - (item.productivityPct / maxPct) * height
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Draw grid lines (0%, 50%, 100%)
        val gridLineColor = backgroundColor.copy(alpha = 0.5f)
        drawLine(gridLineColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
        drawLine(gridLineColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1f)
        drawLine(gridLineColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

        clipRect {
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Draw points
            data.forEachIndexed { index, item ->
                val x = index * pointSpacing
                val y = height - (item.productivityPct / maxPct) * height
                drawCircle(
                    color = lineColor,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
