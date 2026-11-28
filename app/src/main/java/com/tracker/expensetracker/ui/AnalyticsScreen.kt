package com.tracker.expensetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tracker.expensetracker.domain.model.AnalyticsRange
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CategorySpending
import com.tracker.expensetracker.domain.model.SpendingPoint
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    currency: AppCurrency,
    onRangeSelected: (AnalyticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val spending = state.spending
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range.label) }
                    )
                }
            }
        }
        if (spending == null) {
            item { Text("Loading spending…") }
        } else if (spending.categories.isEmpty()) {
            item { Text("No spending in this period", style = MaterialTheme.typography.bodyLarge) }
        } else {
            item {
                AnalyticsCard("Total spending") {
                    Text(currency.format(spending.total), style = MaterialTheme.typography.headlineMedium)
                    Text("${currency.format(spending.dailyAverage)} per day")
                }
            }
            item {
                AnalyticsCard("Monthly spending") { MonthlyBars(spending.months, currency) }
            }
            item {
                AnalyticsCard("Spending by category") { CategoryBreakdown(spending.categories, spending.total, currency) }
            }
            item {
                AnalyticsCard("Daily spending") { DailyTrend(spending.days, currency) }
            }
        }
    }
}

@Composable
private fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun MonthlyBars(points: List<SpendingPoint<YearMonth>>, currency: AppCurrency) {
    var selectedIndex by rememberSaveable(points.first().period.toString(), points.last().period.toString()) {
        mutableIntStateOf(points.lastIndex)
    }
    val max = points.maxOf { it.amount }
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.primaryContainer
    val track = MaterialTheme.colorScheme.surfaceVariant
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        points.forEachIndexed { index, point ->
            val selected = index == selectedIndex
            Column(
                Modifier.width(56.dp)
                    .selectable(selected, role = Role.RadioButton, onClick = { selectedIndex = index })
                    .semantics {
                        contentDescription = "${point.period.format(MONTH_DETAIL)}, ${currency.format(point.amount)}"
                    }
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.width(24.dp).height(96.dp).background(track, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(chartFraction(point.amount, max))
                            .background(if (selected) active else inactive, RoundedCornerShape(6.dp))
                    )
                }
                Text(point.period.format(MONTH_LABEL), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    val detail = points[selectedIndex.coerceIn(points.indices)]
    Text("${detail.period.format(MONTH_DETAIL)} · ${currency.format(detail.amount)}")
    Text("Totals include days within the selected range.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CategoryBreakdown(categories: List<CategorySpending>, total: BigDecimal, currency: AppCurrency) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = categories.firstOrNull { it.category.id == selectedId }
    val circleWeights = categories.map { chartFraction(it.amount, total) }
    val onArcSelected = { index: Int ->
        val id = categories[index].category.id
        selectedId = if (selectedId == id) null else id
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(176.dp)
                .semantics { contentDescription = "Category spending chart. Select a category below for details." }
                .pointerInput(categories, circleWeights, selectedId) {
                    detectTapGestures { point ->
                        val x = point.x - size.width / 2f
                        val y = point.y - size.height / 2f
                        val radius = minOf(size.width, size.height) / 2f
                        val distance = hypot(x, y)
                        if (distance in (radius * 0.68f)..radius) {
                            val portion = ((Math.toDegrees(atan2(y, x).toDouble()) + 450.0) % 360.0) / 360.0
                            var boundary = 0f
                            val index = circleWeights.indexOfFirst { weight ->
                                boundary += weight
                                portion < boundary
                            }
                            onArcSelected(if (index >= 0) index else categories.lastIndex)
                        }
                    }
                }
        ) {
            val stroke = size.minDimension * 0.16f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            categories.forEachIndexed { index, spending ->
                val sweep = if (index == categories.lastIndex) 270f - start else circleWeights[index] * 360f
                drawArc(
                    color = Color(spending.category.colorHex).copy(
                        alpha = if (selected == null || spending.category.id == selectedId) 1f else 0.30f
                    ),
                    startAngle = start,
                    sweepAngle = sweep.coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke)
                )
                start += sweep
            }
        }
        Text(
            currency.format(selected?.amount ?: total),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(104.dp),
            textAlign = TextAlign.Center
        )
    }

    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        categories.forEachIndexed { index, spending ->
            val color = Color(spending.category.colorHex)
            Row(
                Modifier.fillMaxWidth()
                    .selectable(
                        selected = spending.category.id == selectedId,
                        role = Role.RadioButton,
                        onClick = { onArcSelected(index) }
                    )
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Text(spending.category.displayName, modifier = Modifier.weight(1f))
                Text(currency.format(spending.amount))
            }
        }
    }
}

@Composable
private fun DailyTrend(points: List<SpendingPoint<LocalDate>>, currency: AppCurrency) {
    var selectedIndex by rememberSaveable(points.first().period.toString(), points.last().period.toString()) {
        mutableIntStateOf(points.lastIndex)
    }
    val selected = selectedIndex.coerceIn(points.indices)
    val max = points.maxOf { it.amount }
    val line = MaterialTheme.colorScheme.primary
    val guides = MaterialTheme.colorScheme.outlineVariant
    val fractions = points.map { chartFraction(it.amount, max) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Peak day", style = MaterialTheme.typography.bodySmall)
        Text(currency.format(max), style = MaterialTheme.typography.bodySmall)
    }
    Canvas(
        Modifier.fillMaxWidth().height(104.dp)
            .semantics { contentDescription = "Daily spending trend. Select a day below for details." }
            .pointerInput(points) {
                detectTapGestures { position ->
                    val inset = 5.dp.toPx()
                    val ratio = (position.x - inset) / (size.width - 2 * inset).coerceAtLeast(1f)
                    selectedIndex = (ratio * points.lastIndex).roundToInt().coerceIn(points.indices)
                }
            }
    ) {
        val padding = 5.dp.toPx()
        val usableWidth = (size.width - 2 * padding).coerceAtLeast(0f)
        val usableHeight = (size.height - 2 * padding).coerceAtLeast(0f)
        fun position(index: Int): Offset {
            val x = if (points.size == 1) size.width / 2 else padding + usableWidth * index / points.lastIndex
            return Offset(x, padding + usableHeight * (1f - fractions[index]))
        }
        listOf(0f, 0.5f, 1f).forEach { guide ->
            val y = padding + usableHeight * guide
            drawLine(guides, Offset(padding, y), Offset(size.width - padding, y))
        }
        val trend = Path()
        points.indices.forEach { index ->
            val point = position(index)
            if (index == 0) trend.moveTo(point.x, point.y) else trend.lineTo(point.x, point.y)
        }
        drawPath(trend, line, style = Stroke(2.dp.toPx()))
        drawCircle(line, radius = 4.dp.toPx(), center = position(selected))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(points.first().period.format(DAY_LABEL), style = MaterialTheme.typography.labelSmall)
        Text(points.last().period.format(DAY_LABEL), style = MaterialTheme.typography.labelSmall)
    }
    val point = points[selected]
    Text("${point.period.format(DAY_DETAIL)} · ${currency.format(point.amount)}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(enabled = selected > 0, onClick = { selectedIndex = selected - 1 }) { Text("Previous day") }
        TextButton(enabled = selected < points.lastIndex, onClick = { selectedIndex = selected + 1 }) { Text("Next day") }
    }
}

private fun chartFraction(value: BigDecimal, max: BigDecimal): Float =
    if (max.signum() == 0) 0f else value.divide(max, MathContext.DECIMAL64).toFloat().coerceIn(0f, 1f)

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMM")
private val MONTH_DETAIL = DateTimeFormatter.ofPattern("MMM yyyy")
private val DAY_LABEL = DateTimeFormatter.ofPattern("MMM d")
private val DAY_DETAIL = DateTimeFormatter.ofPattern("MMM d, yyyy")
