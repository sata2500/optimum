package tech.salev.optimum.ui.components.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Horizontal slider that controls how many days are shown in the grid.
 *
 * Uses local [tempDays] state to avoid triggering a ViewModel update on
 * every drag frame — the ViewModel is only notified when the finger lifts.
 *
 * @param daysToView Current committed value from the ViewModel.
 * @param onDaysChanged Called once when the user finishes dragging.
 */
@Composable
fun DaysSlider(
    daysToView: Int,
    onDaysChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tempDays by remember(daysToView) { mutableFloatStateOf(daysToView.toFloat()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Slider(
            value = tempDays,
            onValueChange = { tempDays = it },
            onValueChangeFinished = { onDaysChanged(tempDays.roundToInt()) },
            valueRange = 1f..30f,
            steps = 28,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${tempDays.roundToInt()} Gün",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(48.dp)
        )
    }
}
