package tech.salev.optimum.ui.components.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 1000
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    
    // Animation progress (0f to 1f)
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration)
        )
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val barCount = data.size
        val barSpacing = 16.dp.toPx()
        
        // Total available width minus spacing
        val totalSpacing = barSpacing * (barCount + 1)
        val barWidth = (width - totalSpacing) / barCount
        
        // Reserve space at bottom for labels
        val bottomLabelHeight = 32.dp.toPx()
        val topValueHeight = 24.dp.toPx()
        
        val maxBarHeight = height - bottomLabelHeight - topValueHeight
        
        var currentX = barSpacing

        for (item in data) {
            val barHeight = if (maxValue == 0f) 0f else (item.value / maxValue) * maxBarHeight
            val animatedBarHeight = barHeight * progress.value
            
            val barTopY = height - bottomLabelHeight - animatedBarHeight
            
            // Draw Bar
            drawRoundRect(
                color = item.color,
                topLeft = Offset(currentX, barTopY),
                size = Size(barWidth, animatedBarHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            
            // Draw Top Value Text
            val valueText = item.value.toInt().toString()
            val valueLayoutResult = textMeasurer.measure(
                text = valueText,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            )
            drawText(
                textLayoutResult = valueLayoutResult,
                topLeft = Offset(
                    x = currentX + (barWidth - valueLayoutResult.size.width) / 2,
                    y = barTopY - valueLayoutResult.size.height - 4.dp.toPx()
                )
            )
            
            // Draw Bottom Label
            val labelLayoutResult = textMeasurer.measure(
                text = item.label,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            )
            
            drawText(
                textLayoutResult = labelLayoutResult,
                topLeft = Offset(
                    x = currentX + (barWidth - labelLayoutResult.size.width) / 2,
                    y = height - bottomLabelHeight + 8.dp.toPx()
                )
            )
            
            currentX += barWidth + barSpacing
        }
    }
}
