package com.quietbuddy.ui.history

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import com.quietbuddy.ui.character.moodLabelRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Dp
import com.quietbuddy.ui.SingleColumnMaxWidth
import com.quietbuddy.ui.TwoPaneMaxWidth
import com.quietbuddy.ui.isLargeScreen
import com.quietbuddy.ui.isTwoPane
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietbuddy.core.DaySummary
import com.quietbuddy.core.HourBar

import com.quietbuddy.core.Thresholds
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen() {
    val palette = LocalPalette.current
    val viewModel: HistoryViewModel = hiltViewModel()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val selected by viewModel.selectedDay.collectAsStateWithLifecycle()
    val hours by viewModel.hours.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val selectedSummary = days.firstOrNull { it.dateEpochDay == selected }
    val challengesThisWeek = days.sumOf { it.challengesWon }
    val selectedName = LocalDate.ofEpochDay(selected).dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault())

    val chartHeight = if (isLargeScreen()) 240.dp else 150.dp

    val header: @Composable () -> Unit = {
        Column {
            Text(settings.roomName, style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
            Text(stringResource(R.string.history_this_week), style = MaterialTheme.typography.headlineSmall)
        }
    }

    // Seven day buttons
    val dayPicker: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val isSelected = day.dateEpochDay == selected
                val isToday = index == days.lastIndex
                val weekday = LocalDate.ofEpochDay(day.dateEpochDay)
                    .dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                val shape = RoundedCornerShape(16.dp)
                val moodText = day.dominantMood?.let { stringResource(moodLabelRes(it)) }
                    ?: stringResource(R.string.history_no_data)
                val dayDescription = stringResource(
                    if (isSelected) R.string.history_day_desc_selected else R.string.history_day_desc,
                    weekday,
                    moodText,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(shape)
                        .background(if (isSelected) palette.accentSoft else palette.card)
                        .border(2.dp, if (isSelected) palette.accent else palette.line, shape)
                        .clickable { viewModel.selectDay(day.dateEpochDay) }
                        .semantics {
                            contentDescription = dayDescription
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CharacterView(day.dominantMood, Modifier.size(30.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isToday) stringResource(R.string.history_today) else weekday,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) palette.accent else palette.inkSoft,
                    )
                }
            }
        }
    }

    // Hourly chart card
    val chart: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(20.dp))
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.history_hour_by_hour, selectedName), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.history_average_db), style = MaterialTheme.typography.labelSmall)
            }
            if (hours.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(chartHeight), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_no_readings), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
                }
            } else {
                HourlyChart(hours, settings.baseThresholds, chartHeight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendItem(stringResource(R.string.legend_calm), QB.Calm)
                LegendItem(stringResource(R.string.legend_loud), QB.Loud)
                LegendItem(stringResource(R.string.legend_too_loud), QB.TooLoud)
            }
        }
    }

    // Stat cards
    val stats: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = stringResource(R.string.history_calm_time),
                value = formatCalm(selectedSummary?.calmMinutes ?: 0),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = stringResource(R.string.home_loudest_moment),
                value = selectedSummary?.loudestDb?.let { "${it.roundToInt()} dB" } ?: "-",
                subtitle = selectedSummary?.loudestAtMs?.let { formatTime(it) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Challenges card
    val challenges: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    stringResource(R.string.history_challenges_won, challengesThisWeek),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        val twoPane = isTwoPane(maxWidth, maxHeight)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (twoPane) TwoPaneMaxWidth else SingleColumnMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            header()
            if (twoPane) {
                // Landscape: the week and its chart on the left, the day's numbers on the right.
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        dayPicker()
                        chart()
                    }
                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        stats()
                        challenges()
                    }
                }
            } else {
                dayPicker()
                chart()
                stats()
                challenges()
            }
        }
    }
}

@Composable
private fun HourlyChart(bars: List<HourBar>, thresholds: Thresholds, height: Dp) {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.inkSoft)
    val peakStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = palette.ink)

    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
        val leftPad = 26.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val x0 = leftPad
        val x1 = size.width
        val y0 = topPad
        val y1 = size.height - bottomPad

        fun yFor(db: Float): Float = y1 - ((db.coerceIn(30f, 90f) - 30f) / 60f) * (y1 - y0)

        // Gridlines and y labels
        listOf(30, 50, 70, 90).forEach { db ->
            val y = yFor(db.toFloat())
            drawLine(palette.line, Offset(x0, y), Offset(x1, y), strokeWidth = 1.dp.toPx())
            val m = measurer.measure("$db", labelStyle)
            drawText(m, topLeft = Offset(leftPad - 6.dp.toPx() - m.size.width, y - m.size.height / 2f))
        }

        val minH = bars.minOf { it.hour }
        val maxH = bars.maxOf { it.hour }
        val slots = (maxH - minH + 1).coerceAtLeast(1)
        val slotW = (x1 - x0) / slots
        val barW = minOf(slotW * 0.6f, 14.dp.toPx())
        val radius = 4.dp.toPx()

        fun centerX(hour: Int): Float = x0 + (hour - minH + 0.5f) * slotW

        bars.forEach { b ->
            val cx = centerX(b.hour)
            val top = yFor(b.laeqDb)
            val color = when {
                b.laeqDb < thresholds.worriedFromDb -> QB.Calm
                b.laeqDb < thresholds.sadFromDb -> QB.Loud
                else -> QB.TooLoud
            }
            val r = minOf(radius, barW / 2f, (y1 - top) / 2f)
            drawPath(topRoundedBar(cx - barW / 2f, top, cx + barW / 2f, y1, r), color)
        }

        bars.maxByOrNull { it.laeqDb }?.let { peak ->
            val m = measurer.measure("${peak.laeqDb.roundToInt()}", peakStyle)
            val top = yFor(peak.laeqDb)
            drawText(
                m,
                topLeft = Offset(
                    centerX(peak.hour) - m.size.width / 2f,
                    (top - m.size.height - 2.dp.toPx()).coerceAtLeast(0f),
                ),
            )
        }

        for (h in minH..maxH) {
            if ((h - minH) % 3 != 0) continue
            val m = measurer.measure("$h", labelStyle)
            drawText(m, topLeft = Offset(centerX(h) - m.size.width / 2f, y1 + 4.dp.toPx()))
        }
    }
}

private fun topRoundedBar(left: Float, top: Float, right: Float, bottom: Float, r: Float): Path =
    Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(left, top, right, bottom),
                topLeft = CornerRadius(r),
                topRight = CornerRadius(r),
                bottomRight = CornerRadius.Zero,
                bottomLeft = CornerRadius.Zero,
            ),
        )
    }

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .background(palette.card, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.headlineSmall)
        subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
    }
}

private fun formatCalm(minutes: Int): String = "${minutes / 60} h ${minutes % 60} min"

private fun formatTime(ms: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
