package tech.salev.optimum.ui.components.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class PieChartData(
    val color: Color,
    val value: Float,
    val label: String
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 1000,
    strokeWidth: Float = 40f
) {
    val totalValue = data.sumOf { it.value.toDouble() }.toFloat()
    
    // Animation progress (0f to 1f)
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration)
        )
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            
            for (item in data) {
                // Determine the sweep angle based on the value's proportion to the total
                val sweepAngle = if (totalValue == 0f) 0f else (item.value / totalValue) * 360f
                // Apply animation progress
                val animatedSweepAngle = sweepAngle * progress.value
                
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = animatedSweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.dp.toPx())
                )
                startAngle += animatedSweepAngle
            }
        }
    }
}
