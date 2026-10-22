package app.arro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private enum class DateMode { Days, Months, Years }

private fun monthName(month: Int, short: Boolean = false): String =
    java.time.Month.of(month).getDisplayName(if (short) java.time.format.TextStyle.SHORT else java.time.format.TextStyle.FULL, Locale.UK)

private fun YearMonth.clamp(min: LocalDate, max: LocalDate): YearMonth {
    val lo = YearMonth.from(min)
    val hi = YearMonth.from(max)
    return if (this < lo) lo else if (this > hi) hi else this
}

/** "3 July 1991". */
fun longDate(date: LocalDate): String = "${date.dayOfMonth} ${monthName(date.monthValue)} ${date.year}"

/** Grid height shared by the day, month and year views so the sheet doesn't jump between them. */
private val GridHeight = 306.dp

/**
 * Calendar in a bottom sheet for picking a date between [min] and [max].
 *
 * With a date already chosen it opens on that month. With nothing chosen it opens on the year list
 * (scrolled to [startYear]), because a birthday is usually decades back: pick the year, then the month, then the day.
 */
@Composable
fun BoxScope.DateSheet(
    visible: Boolean,
    title: String,
    selected: LocalDate?,
    min: LocalDate,
    max: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    note: String? = null,
    startYear: Int = max.year,
) {
    var mode by remember(visible) { mutableStateOf(if (selected == null) DateMode.Years else DateMode.Days) }
    var view by remember(visible) { mutableStateOf(YearMonth.from(selected ?: LocalDate.of(startYear, 1, 1)).clamp(min, max)) }
    var pending by remember(visible) { mutableStateOf(selected) }
    Sheet(visible, onDismiss) {
        SheetHeader(title, onDismiss, subtitle)
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderChip(monthName(view.monthValue), mode == DateMode.Months, "Choose month") {
                mode = if (mode == DateMode.Months) DateMode.Days else DateMode.Months
            }
            HeaderChip(view.year.toString(), mode == DateMode.Years, "Choose year") {
                mode = if (mode == DateMode.Years) DateMode.Days else DateMode.Years
            }
            Spring()
            if (mode == DateMode.Days) {
                val canBack = view.minusMonths(1) >= YearMonth.from(min)
                val canNext = view.plusMonths(1) <= YearMonth.from(max)
                RoundIcon(Icons.ChevronRight, { if (canBack) view = view.minusMonths(1) }, Modifier.rotate(180f).alpha(if (canBack) 1f else 0.35f),
                    style = IconBtn.Line, size = 40.dp, iconSize = 18.dp, description = "Previous month")
                RoundIcon(Icons.ChevronRight, { if (canNext) view = view.plusMonths(1) }, Modifier.alpha(if (canNext) 1f else 0.35f),
                    style = IconBtn.Line, size = 40.dp, iconSize = 18.dp, description = "Next month")
            }
        }
        Box(Modifier.fillMaxWidth().height(GridHeight).padding(top = 12.dp)) {
            when (mode) {
                DateMode.Days -> DayGrid(view, pending, min, max) { pending = it }
                DateMode.Months -> MonthGrid(view, min, max) { m -> view = view.withMonth(m); mode = DateMode.Days }
                DateMode.Years -> YearGrid(view.year, min.year, max.year) { y ->
                    view = YearMonth.of(y, view.monthValue).clamp(min, max)
                    mode = DateMode.Months
                }
            }
        }
        if (note != null) Cap(note, Modifier.padding(top = 4.dp), size = 12.5f)
        ArroButton(
            pending?.let { "Use ${longDate(it)}" } ?: when (mode) {
                DateMode.Years -> "Choose the year"
                DateMode.Months -> "Choose the month"
                DateMode.Days -> "Choose the day"
            },
            { pending?.let { onPick(it); onDismiss() } },
            Modifier.padding(top = 14.dp),
            enabled = pending != null,
        )
    }
}

@Composable
private fun HeaderChip(text: String, active: Boolean, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) C.Primary else C.White, RoundedCornerShape(999.dp))
            .border(1.5.dp, if (active) C.Primary else C.Line, RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Txt(text, T.Chip.copy(fontWeight = FontWeight.SemiBold), color = if (active) C.OnPrimary else C.Ink)
        Ic(Icons.ChevronDown, Modifier.rotate(if (active) 180f else 0f), size = 16.dp, tint = if (active) C.PrimaryAccent else C.Slate)
    }
}

@Composable
private fun DayGrid(view: YearMonth, pending: LocalDate?, min: LocalDate, max: LocalDate, onDay: (LocalDate) -> Unit) {
    val lead = view.atDay(1).dayOfWeek.value - 1 // weeks start on Monday
    val days = view.lengthOfMonth()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                Cap(d.take(1), Modifier.weight(1f), align = TextAlign.Center, size = 12f)
            }
        }
        // Always six rows, so the sheet keeps its height from month to month.
        repeat(6) { r ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                repeat(7) { c ->
                    val n = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                        if (n in 1..days) {
                            val date = view.atDay(n)
                            val allowed = !date.isBefore(min) && !date.isAfter(max)
                            val on = date == pending
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (on) C.Primary else Color.Transparent, CircleShape)
                                    .then(if (allowed) Modifier.clickable(role = Role.Button) { onDay(date) } else Modifier)
                                    .semantics { contentDescription = longDate(date) + if (allowed) "" else ", not available" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Txt(n.toString(), T.Body.copy(fontFeatureSettings = "tnum", fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                                    color = when { on -> C.OnPrimary; allowed -> C.Ink; else -> C.Control })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(view: YearMonth, min: LocalDate, max: LocalDate, onMonth: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..12).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    val ym = YearMonth.of(view.year, m)
                    val allowed = ym >= YearMonth.from(min) && ym <= YearMonth.from(max)
                    GridCell(monthName(m, short = true), m == view.monthValue, allowed, Modifier.weight(1f)) { onMonth(m) }
                }
            }
        }
    }
}

@Composable
private fun YearGrid(selectedYear: Int, minYear: Int, maxYear: Int, onYear: (Int) -> Unit) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val rows = (maxYear downTo minYear).toList().chunked(4)
    val selectedRow = rows.indexOfFirst { selectedYear in it }.coerceAtLeast(0)
    LaunchedEffect(Unit) {
        // Show the selected year with a couple of rows above it for context.
        val rowPx = with(density) { 54.dp.roundToPx() }
        scroll.scrollTo(((selectedRow - 2).coerceAtLeast(0)) * rowPx)
    }
    Column(Modifier.fillMaxWidth().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { y -> GridCell(y.toString(), y == selectedYear, true, Modifier.weight(1f)) { onYear(y) } }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GridCell(text: String, on: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) C.Primary else C.Field, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, T.Body14.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = when { on -> C.OnPrimary; enabled -> C.Ink; else -> C.Control })
    }
}
