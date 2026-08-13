package tech.salev.optimum.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tech.salev.optimum.ui.components.analytics.BarChart
import tech.salev.optimum.ui.components.analytics.BarChartData
import tech.salev.optimum.ui.components.analytics.HeatmapCalendar
import tech.salev.optimum.ui.components.analytics.LineChart
import tech.salev.optimum.ui.components.analytics.PieChart
import tech.salev.optimum.ui.components.analytics.PieChartData
import tech.salev.optimum.ui.model.AnalyticsRange
import tech.salev.optimum.ui.viewmodel.AnalyticsViewModel
import tech.salev.optimum.ui.viewmodel.OptimumViewModel
import tech.salev.optimum.util.ColorUtils

enum class ChartType { PIE, BAR }

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

    var rating by remember(currentEvaluation) { mutableIntStateOf(currentEvaluation?.rating ?: 0) }
    var mood by remember(currentEvaluation) { mutableIntStateOf(currentEvaluation?.mood ?: 0) }
    var journalNote by remember(currentEvaluation) { mutableStateOf(currentEvaluation?.journalNote ?: "") }

    var editingTargetDate by remember { mutableStateOf<String?>(null) }
    var evaluationToDelete by remember { mutableStateOf<tech.salev.optimum.data.model.DailyEvaluation?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Chart States
    var categoryChartType by remember { mutableStateOf(ChartType.PIE) }
    var activityChartType by remember { mutableStateOf(ChartType.BAR) }

    // Chart data is now computed in ViewModel and provided via uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gelişmiş Analitik 📊",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zamanını Keşfet",
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
            
            // ── RANGE SELECTOR ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsRange.entries.forEach { range ->
                        val labelText = if (range == AnalyticsRange.ALL) "Tümü" else range.label
                        FilterChip(
                            selected = uiState.selectedRange == range,
                            onClick = { analyticsViewModel.selectedRange.value = range },
                            label = { Text(labelText) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // ── INSIGHTS (AI) ──
            if (uiState.insights.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.insights) { insight ->
                            ElevatedCard(
                                modifier = Modifier.width(280.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(insight.emoji, fontSize = 32.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = insight.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = insight.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SUMMARY CARDS GRID ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            title = "Toplam Süre",
                            value = "${uiState.totalMinutes / 60}s ${uiState.totalMinutes % 60}d",
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            title = "Aktif Gün / Seri",
                            value = "${uiState.activeDays}g / 🔥${uiState.streak}",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        MetricCard(
                            title = "Günlük Ort.",
                            value = "${uiState.dailyAvgMinutes / 60}s ${uiState.dailyAvgMinutes % 60}d",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // ── PRODUCTIVITY LINE CHART ──
            if (uiState.dailyData.size > 1) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Üretkenlik Trendi (%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            LineChart(
                                data = uiState.dailyData,
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                            )
                        }
                    }
                }
            }
            
            // ── HEATMAP CALENDAR ──
            if (uiState.heatmapData.isNotEmpty() && uiState.selectedRange != AnalyticsRange.TODAY) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Aktivite Yoğunluğu (Son 28 Gün)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            HeatmapCalendar(
                                data = uiState.heatmapData,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                    }
                }
            }

            // ── CATEGORY DISTRIBUTION ──
            if (uiState.categoryPieChartData.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kategori Dağılımı",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                ChartToggle(categoryChartType) { categoryChartType = it }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            if (categoryChartType == ChartType.PIE) {
                                PieChart(
                                    data = uiState.categoryPieChartData,
                                    modifier = Modifier.size(160.dp),
                                    strokeWidth = 30f
                                )
                                Spacer(Modifier.height(16.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    uiState.categoryPieChartData.forEach { pd ->
                                        val percentage = ((pd.value / uiState.totalMinutes) * 100).toInt()
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(pd.color))
                                            Spacer(Modifier.width(8.dp))
                                            Text(text = pd.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            Text(
                                                text = "${pd.value.toInt()} dk (%$percentage)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                BarChart(data = uiState.categoryBarChartData, modifier = Modifier.fillMaxWidth().height(150.dp))
                            }
                        }
                    }
                }
            }

            // ── ACTIVITY DETAILS & FILTER ──
            if (uiState.activityMinutes.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aktivite Detayları",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                ChartToggle(activityChartType) { activityChartType = it }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // Collapsible Filter System
                            var isFilterExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isFilterExpanded = !isFilterExpanded }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filtre", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Filtrele", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(if (isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
                            }

                            AnimatedVisibility(visible = isFilterExpanded) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                                    var catExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = catExpanded,
                                        onExpandedChange = { catExpanded = !catExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = if (uiState.filterCategoryId == null) "Tüm Kategoriler" else uiState.categories.find { it.id == uiState.filterCategoryId }?.name ?: "Bilinmeyen",
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            label = { Text("Kategori Seçimi") }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = catExpanded,
                                            onDismissRequest = { catExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Tüm Kategoriler") },
                                                onClick = { 
                                                    analyticsViewModel.setFilterCategoryId(null)
                                                    catExpanded = false 
                                                }
                                            )
                                            uiState.categories.filter { (uiState.categoryMinutes[it.id] ?: 0) > 0 }.forEach { cat ->
                                                DropdownMenuItem(
                                                    text = { Text(cat.name) },
                                                    onClick = { 
                                                        analyticsViewModel.setFilterCategoryId(cat.id)
                                                        catExpanded = false 
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    
                                    val availableActivities = uiState.activities.filter { act -> 
                                        (uiState.filterCategoryId == null || act.categoryId == uiState.filterCategoryId) && 
                                        (uiState.activityMinutes[act.id] ?: 0) > 0 
                                    }
                                    
                                    if (availableActivities.isNotEmpty()) {
                                        Text("Aktiviteler", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                        Spacer(Modifier.height(4.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            item {
                                                FilterChip(
                                                    selected = uiState.filterActivityIds.isEmpty(),
                                                    onClick = { analyticsViewModel.clearActivityFilter() },
                                                    label = { Text("Tümü") }
                                                )
                                            }
                                            items(availableActivities) { act ->
                                                val isSelected = uiState.filterActivityIds.contains(act.id)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { analyticsViewModel.toggleActivityFilter(act.id) },
                                                    label = { Text(act.name) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            if (uiState.activityPieChartData.isNotEmpty()) {
                                if (activityChartType == ChartType.BAR) {
                                    BarChart(data = uiState.activityBarChartData, modifier = Modifier.fillMaxWidth().height(150.dp))
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        PieChart(data = uiState.activityPieChartData, modifier = Modifier.size(160.dp), strokeWidth = 30f)
                                        Spacer(Modifier.height(16.dp))
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            val totalActivityTime = uiState.activityPieChartData.sumOf { it.value.toDouble() }.toFloat()
                                            uiState.activityPieChartData.forEach { pd ->
                                                val percentage = if (totalActivityTime > 0) ((pd.value / totalActivityTime) * 100).toInt() else 0
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(pd.color))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(text = pd.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "${pd.value.toInt()} dk (%$percentage)",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                    Text("Seçilen filtrede veri bulunamadı.", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            } else if (uiState.totalMinutes == 0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Henüz hiç zaman kaydı yok.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // ── DAILY EVALUATION SECTION ──
            item {
                val evalDateText = editingTargetDate ?: currentDateStr
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingTargetDate != null) "Değerlendirmeyi Düzenle ($evalDateText)" else "Günün Değerlendirmesi ($evalDateText)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
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
                        Text("Gününe Kaç Puan Verirsin?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Star $i",
                                    tint = if (i <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { rating = i }
                                        .padding(4.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))

                        // Mood Selector
                        Text("Nasıl Hissediyorsun?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val moods = listOf(
                                Pair(1, "😔"), Pair(2, "😐"), Pair(3, "😊"), Pair(4, "🤩")
                            )
                            moods.forEach { (mVal, emoji) ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (mood == mVal) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { mood = mVal },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = journalNote,
                            onValueChange = { journalNote = it },
                            label = { Text("Günün Özeti / Notlar") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            maxLines = 8
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                evaluationViewModel.saveEvaluation(rating, journalNote.trim(), evalDateText, mood)
                                editingTargetDate = null
                                scope.launch { snackbarHostState.showSnackbar("Değerlendirme Kaydedildi!") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (editingTargetDate != null) "Değerlendirmeyi Güncelle" else "Kaydet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── PAST EVALUATIONS ──
            if (allEvaluations.isNotEmpty()) {
                item {
                    var pastEvalExpanded by remember { mutableStateOf(false) }
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                    text = "Geçmiş Değerlendirmeler",
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
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val sortedEvaluations = allEvaluations.sortedByDescending { it.date }
                                    sortedEvaluations.take(10).forEach { eval -> // Show top 10
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val moodEmoji = when(eval.mood) {
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
                                                            for(i in 1..5) {
                                                                Icon(
                                                                    imageVector = if (i <= eval.rating) Icons.Default.Star else Icons.Default.StarOutline,
                                                                    contentDescription = null,
                                                                    tint = if (i <= eval.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
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
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = eval.journalNote,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
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

@Composable
fun ChartToggle(currentType: ChartType, onTypeChanged: (ChartType) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (currentType == ChartType.PIE) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onTypeChanged(ChartType.PIE) },
            contentAlignment = Alignment.Center
        ) { 
            Text("🥧", fontSize = 14.sp, color = if (currentType == ChartType.PIE) Color.White else MaterialTheme.colorScheme.onSurface) 
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (currentType == ChartType.BAR) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onTypeChanged(ChartType.BAR) },
            contentAlignment = Alignment.Center
        ) { 
            Text("📊", fontSize = 14.sp, color = if (currentType == ChartType.BAR) Color.White else MaterialTheme.colorScheme.onSurface) 
        }
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
