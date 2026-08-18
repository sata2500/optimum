package tech.salev.optimum.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.analytics.HeatmapCalendar
import tech.salev.optimum.ui.components.analytics.LineChart
import tech.salev.optimum.ui.components.analytics.PieChart
import tech.salev.optimum.ui.components.analytics.PieChartData
import tech.salev.optimum.ui.model.AnalyticsRange
import tech.salev.optimum.ui.viewmodel.AnalyticsViewModel
import tech.salev.optimum.ui.viewmodel.OptimumViewModel

enum class AnalyticsTab(val label: String) {
    CATEGORIES("Kategoriler"),
    ACTIVITIES("Aktiviteler"),
    EVALUATION("Değerlendirme")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsEvaluationScreen(
    optimumViewModel: OptimumViewModel,
    analyticsViewModel: AnalyticsViewModel = hiltViewModel(),
    evaluationViewModel: tech.salev.optimum.ui.viewmodel.EvaluationViewModel = hiltViewModel()
) {
    val uiState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val currentDateStr by optimumViewModel.currentDate.collectAsStateWithLifecycle()

    val currentEvaluation by evaluationViewModel.currentEvaluation.collectAsStateWithLifecycle()
    val allEvaluations by evaluationViewModel.allEvaluations.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(AnalyticsTab.CATEGORIES) }

    var rating by remember(currentEvaluation) { mutableIntStateOf(currentEvaluation?.rating ?: 0) }
    var mood by remember(currentEvaluation) { mutableIntStateOf(currentEvaluation?.mood ?: 0) }
    var journalNote by remember(currentEvaluation) { mutableStateOf(currentEvaluation?.journalNote ?: "") }

    var editingTargetDate by remember { mutableStateOf<String?>(null) }
    var evaluationToDelete by remember { mutableStateOf<tech.salev.optimum.data.model.DailyEvaluation?>(null) }

    // Selected slice states for interactive highlighting
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }
    var selectedActSliceIndex by remember { mutableStateOf<Int?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Analiz & İstatistik 📊",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zaman Dağılımı ve Değerlendirmeler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1. TIME RANGE SELECTOR ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsRange.entries.forEach { range ->
                        val isSelected = uiState.selectedRange == range
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                analyticsViewModel.selectedRange.value = range
                                selectedCategoryIndex = null
                                selectedActSliceIndex = null
                            },
                            label = {
                                Text(
                                    text = range.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // ── 2. SUMMARY METRIC CARDS ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Toplam Süre",
                            value = formatDuration(uiState.totalMinutes),
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        MetricCard(
                            title = "Üretkenlik",
                            value = "%${uiState.productivityPct}",
                            modifier = Modifier.weight(1f),
                            containerColor = if (uiState.productivityPct >= 50) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = if (uiState.selectedRange == AnalyticsRange.TODAY) "Puan Ortalaması" else "Aktif Gün / Seri",
                            value = if (uiState.selectedRange == AnalyticsRange.TODAY) {
                                if (uiState.avgRating > 0f) "★ %.1f".format(uiState.avgRating) else "Henüz Yok"
                            } else {
                                "${uiState.activeDays}g / 🔥${uiState.streak}"
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        MetricCard(
                            title = "Günlük Ortalama",
                            value = formatDuration(uiState.dailyAvgMinutes),
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // ── 3. PRODUCTIVITY LINE CHART & HEATMAP (For Multi-Day Views) ──
            if (uiState.dailyData.size > 1 && uiState.selectedRange != AnalyticsRange.TODAY) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📈 Üretkenlik Trendi (%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(14.dp))
                            LineChart(
                                data = uiState.dailyData,
                                modifier = Modifier.fillMaxWidth().height(140.dp)
                            )
                        }
                    }
                }
            }

            if (uiState.heatmapData.isNotEmpty() && (uiState.selectedRange == AnalyticsRange.MONTH || uiState.selectedRange == AnalyticsRange.ALL)) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🗓️ Aktivite Yoğunluğu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            HeatmapCalendar(
                                data = uiState.heatmapData,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // ── 4. SEGMENTED TAB SELECTOR (Kategori | Aktivite | Değerlendirme) ──
            // ══════════════════════════════════════════════════════════════════
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnalyticsTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedTab = tab }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (tab) {
                                        AnalyticsTab.CATEGORIES -> Icons.Default.PieChart
                                        AnalyticsTab.ACTIVITIES -> Icons.Default.Tune
                                        AnalyticsTab.EVALUATION -> Icons.Default.RateReview
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // ── TAB CONTENT ──
            // ══════════════════════════════════════════════════════════════════
            when (selectedTab) {
                AnalyticsTab.CATEGORIES -> {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Kategori Dağılımı",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                if (uiState.categoryPieChartData.isNotEmpty()) {
                                    // Solid Filled Pie Chart
                                    PieChart(
                                        data = uiState.categoryPieChartData,
                                        size = 190.dp,
                                        selectedIndex = selectedCategoryIndex,
                                        onSliceClick = { index, item ->
                                            selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                                            analyticsViewModel.setFilterCategoryId(if (selectedCategoryIndex != null) item.id else null)
                                        }
                                    )

                                    Spacer(Modifier.height(18.dp))

                                    // Compact Smart Breakdown List with Top N + Expand
                                    BreakdownList(
                                        items = uiState.categoryPieChartData,
                                        totalMinutes = uiState.totalMinutes,
                                        selectedIndex = selectedCategoryIndex,
                                        onItemClick = { index, item ->
                                            selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                                            analyticsViewModel.setFilterCategoryId(if (selectedCategoryIndex != null) item.id else null)
                                        }
                                    )
                                } else {
                                    EmptyDataBox(message = "Bu zaman aralığında kaydedilmiş kategori verisi bulunamadı.")
                                }
                            }
                        }
                    }
                }

                AnalyticsTab.ACTIVITIES -> {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val activeCategories = uiState.categories.filter { (uiState.categoryMinutes[it.id] ?: 0) > 0 }
                                val currentCategoryName = uiState.categories.find { it.id == uiState.filterCategoryId }?.name

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (currentCategoryName != null) "$currentCategoryName Aktiviteleri" else "Aktivite Dağılımı",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // Category Filter Chips
                                if (activeCategories.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = uiState.filterCategoryId == null,
                                                onClick = {
                                                    analyticsViewModel.setFilterCategoryId(null)
                                                    selectedActSliceIndex = null
                                                },
                                                label = { Text("Tümü") },
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                        }
                                        items(activeCategories) { cat ->
                                            val isSelected = uiState.filterCategoryId == cat.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    analyticsViewModel.setFilterCategoryId(if (isSelected) null else cat.id)
                                                    selectedActSliceIndex = null
                                                },
                                                label = { Text(cat.name) },
                                                shape = RoundedCornerShape(14.dp),
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(cat.composeColor)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }

                                val displayedActivityData = if (uiState.filterCategoryId != null) {
                                    uiState.activityPieChartData
                                } else {
                                    uiState.allActivityPieChartData
                                }

                                if (displayedActivityData.isNotEmpty()) {
                                    val activeTotalMins = displayedActivityData.sumOf { it.value.toDouble() }.toInt()
                                    PieChart(
                                        data = displayedActivityData,
                                        size = 190.dp,
                                        selectedIndex = selectedActSliceIndex,
                                        onSliceClick = { index, _ ->
                                            selectedActSliceIndex = if (selectedActSliceIndex == index) null else index
                                        }
                                    )

                                    Spacer(Modifier.height(18.dp))

                                    // Compact Smart Breakdown List with Top N + Expand
                                    BreakdownList(
                                        items = displayedActivityData,
                                        totalMinutes = if (activeTotalMins > 0) activeTotalMins else uiState.totalMinutes,
                                        selectedIndex = selectedActSliceIndex,
                                        onItemClick = { index, _ ->
                                            selectedActSliceIndex = if (selectedActSliceIndex == index) null else index
                                        }
                                    )
                                } else {
                                    EmptyDataBox(message = "Bu aralıkta kayıtlı aktivite bulunamadı.")
                                }
                            }
                        }
                    }
                }

                AnalyticsTab.EVALUATION -> {
                    item {
                        val evalDateText = editingTargetDate ?: currentDateStr
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (editingTargetDate != null) "Değerlendirmeyi Düzenle" else "Günün Değerlendirmesi",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tarih: $evalDateText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (editingTargetDate != null) {
                                        TextButton(
                                            onClick = {
                                                editingTargetDate = null
                                                rating = currentEvaluation?.rating ?: 0
                                                mood = currentEvaluation?.mood ?: 0
                                                journalNote = currentEvaluation?.journalNote ?: ""
                                            }
                                        ) {
                                            Text("Bugüne Dön", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Star Rating (1 to 5)
                                Text(
                                    text = "Gününe Kaç Puan Verirsin?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                                            contentDescription = "Star $i",
                                            tint = if (i <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clickable { rating = i }
                                                .padding(4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                // Mood Selector
                                Text(
                                    text = "Nasıl Hissediyorsun?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val moods = listOf(
                                        Pair(1, "😔"), Pair(2, "😐"), Pair(3, "😊"), Pair(4, "🤩")
                                    )
                                    moods.forEach { (mVal, emoji) ->
                                        val isSelected = mood == mVal
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .clickable { mood = mVal },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 26.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                OutlinedTextField(
                                    value = journalNote,
                                    onValueChange = { journalNote = it },
                                    label = { Text("Günün Özeti / Notlar") },
                                    placeholder = { Text("Bugün neler yaptın? Öne çıkan hedeflerin ve düşüncelerin...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    minLines = 3,
                                    maxLines = 8
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        evaluationViewModel.saveEvaluation(rating, journalNote.trim(), evalDateText, mood)
                                        editingTargetDate = null
                                        scope.launch { snackbarHostState.showSnackbar("Değerlendirme Başarıyla Kaydedildi! ✓") }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (editingTargetDate != null) "Değerlendirmeyi Güncelle" else "Değerlendirmeyi Kaydet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Past Evaluations History
                    if (allEvaluations.isNotEmpty()) {
                        item {
                            var pastEvalExpanded by remember { mutableStateOf(false) }
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pastEvalExpanded = !pastEvalExpanded }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Geçmiş Değerlendirmeler (${allEvaluations.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (pastEvalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Genişlet"
                                        )
                                    }

                                    AnimatedVisibility(visible = pastEvalExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val sortedEvaluations = allEvaluations.sortedByDescending { it.date }
                                            sortedEvaluations.take(20).forEach { eval ->
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    tonalElevation = 1.dp
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            val moodEmoji = when (eval.mood) {
                                                                1 -> "😔" 2 -> "😐" 3 -> "😊" 4 -> "🤩" else -> ""
                                                            }
                                                            Text(
                                                                text = "${eval.date} $moodEmoji",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Row {
                                                                    for (i in 1..5) {
                                                                        Icon(
                                                                            imageVector = if (i <= eval.rating) Icons.Default.Star else Icons.Default.StarOutline,
                                                                            contentDescription = null,
                                                                            tint = if (i <= eval.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Spacer(Modifier.width(8.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        editingTargetDate = eval.date
                                                                        rating = eval.rating
                                                                        mood = eval.mood
                                                                        journalNote = eval.journalNote
                                                                    },
                                                                    modifier = Modifier.size(28.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Edit,
                                                                        contentDescription = "Düzenle",
                                                                        modifier = Modifier.size(16.dp),
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = { evaluationToDelete = eval },
                                                                    modifier = Modifier.size(28.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = "Sil",
                                                                        modifier = Modifier.size(16.dp),
                                                                        tint = MaterialTheme.colorScheme.error
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (eval.journalNote.isNotBlank()) {
                                                            Spacer(Modifier.height(6.dp))
                                                            Text(
                                                                text = eval.journalNote,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        // Delete Confirmation Dialog
        evaluationToDelete?.let { eval ->
            AlertDialog(
                onDismissRequest = { evaluationToDelete = null },
                title = { Text("Değerlendirmeyi Sil", fontWeight = FontWeight.Bold) },
                text = { Text("${eval.date} tarihine ait değerlendirmeyi silmek istediğinize emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            evaluationViewModel.deleteEvaluation(eval)
                            if (editingTargetDate == eval.date) {
                                editingTargetDate = null
                                rating = 0
                                mood = 0
                                journalNote = ""
                            }
                            evaluationToDelete = null
                            scope.launch { snackbarHostState.showSnackbar("Değerlendirme Silindi") }
                        }
                    ) {
                        Text("Sil", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { evaluationToDelete = null }) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}

/**
 * Smart, optimized breakdown list:
 * - Shows Top 5 items directly with % badge, colored indicator, and duration.
 * - Collapses the rest (>5) under a clean "Tümünü Göster (X adet) ▼" button.
 * - Adds a subtle inline progress indicator to visualize relative distribution.
 */
@Composable
private fun BreakdownList(
    items: List<PieChartData>,
    totalMinutes: Int,
    selectedIndex: Int?,
    onItemClick: (Int, PieChartData) -> Unit
) {
    var isExpanded by remember(items) { mutableStateOf(false) }
    val maxVisible = 5
    val visibleItems = if (isExpanded || items.size <= maxVisible) items else items.take(maxVisible)
    val hiddenCount = items.size - maxVisible

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visibleItems.forEachIndexed { index, pd ->
            val percentage = if (totalMinutes > 0) ((pd.value / totalMinutes) * 100).toInt() else 0
            val isSelected = selectedIndex == index
            val progressFraction = if (totalMinutes > 0) (pd.value / totalMinutes.toFloat()).coerceIn(0f, 1f) else 0f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onItemClick(index, pd) },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Percentage Badge
                        Surface(
                            color = pd.color.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "%$percentage",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = pd.color
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Color Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pd.color)
                        )
                        Spacer(Modifier.width(8.dp))

                        // Title
                        Text(
                            text = pd.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        // Formatted Duration
                        Text(
                            text = pd.subLabel.ifBlank { "${pd.value.toInt()} dk" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Proportional mini progress bar
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = pd.color.copy(alpha = 0.85f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Expand / Collapse Button if there are more items
        if (items.size > maxVisible) {
            val remainingMins = items.drop(maxVisible).sumOf { it.value.toDouble() }.toInt()
            val remainingPct = if (totalMinutes > 0) ((remainingMins.toFloat() / totalMinutes) * 100).toInt() else 0

            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isExpanded) "Daha Az Göster ▲" else "Diğer ($hiddenCount aktivite, %$remainingPct) Tümünü Göster ▼",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDataBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(minutes: Int): String {
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins > 0) "${hours}s ${mins}d" else "${hours}s"
    } else {
        "${minutes}d"
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
