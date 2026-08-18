package tech.salev.optimum.ui.components.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PieChartData(
    val id: Long = 0L,
    val color: Color,
    val value: Float,
    val label: String,
    val subLabel: String = ""
)

/**
 * Modern Solid Pie Chart (Pasta Grafik) with smooth animations,
 * slice separation borders, and interactive tap selection.
 */
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    animationDuration: Int = 850,
    selectedIndex: Int? = null,
    onSliceClick: ((Int, PieChartData) -> Unit)? = null
) {
    val totalValue = data.sumOf { it.value.toDouble() }.toFloat()
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // Animation progress (0f to 1f)
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    var internalSelectedIndex by remember { mutableStateOf<Int?>(selectedIndex) }
    
    LaunchedEffect(selectedIndex) {
        internalSelectedIndex = selectedIndex
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(data, totalValue) {
                    if (onSliceClick != null && totalValue > 0f) {
                        detectTapGestures { offset ->
                            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            val radius = size.toPx() / 2f
                            
                            if (distance <= radius && distance >= 10f) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                // Align with chart starting at -90 degrees (top)
                                val chartAngle = (angle + 90f) % 360f
                                
                                var currentAngle = 0f
                                for ((index, item) in data.withIndex()) {
                                    val sweep = (item.value / totalValue) * 360f
                                    if (chartAngle in currentAngle..(currentAngle + sweep)) {
                                        internalSelectedIndex = if (internalSelectedIndex == index) null else index
                                        onSliceClick(index, item)
                                        break
                                    }
                                    currentAngle += sweep
                                }
                            } else if (distance < 10f) {
                                // Tapped center -> clear selection
                                internalSelectedIndex = null
                            }
                        }
                    }
                }
        ) {
            val canvasRadius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            if (totalValue <= 0f || data.isEmpty()) {
                // Empty state placeholder
                drawCircle(
                    color = surfaceColor.copy(alpha = 0.4f),
                    radius = canvasRadius,
                    center = center,
                    style = Fill
                )
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    radius = canvasRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                return@Canvas
            }

            var startAngle = -90f

            for ((index, item) in data.withIndex()) {
                val itemSweep = (item.value / totalValue) * 360f
                val sweepAngle = itemSweep * progress.value

                val isSelected = internalSelectedIndex == index
                val explodeDistance = if (isSelected) 8.dp.toPx() else 0f
                
                val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                val sliceCenter = if (isSelected) {
                    Offset(
                        x = center.x + (cos(midAngleRad) * explodeDistance).toFloat(),
                        y = center.y + (sin(midAngleRad) * explodeDistance).toFloat()
                    )
                } else {
                    center
                }

                val chartSize = Size(canvasRadius * 2, canvasRadius * 2)
                val topLeft = Offset(
                    sliceCenter.x - canvasRadius,
                    sliceCenter.y - canvasRadius
                )

                // 1. Draw solid filled pie slice
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = topLeft,
                    size = chartSize,
                    style = Fill
                )

                // 2. Draw clean separating stroke
                if (data.size > 1) {
                    drawArc(
                        color = surfaceColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = topLeft,
                        size = chartSize,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                startAngle += itemSweep
            }
        }
    }
}
