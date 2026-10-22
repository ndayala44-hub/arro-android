package app.arro.ui.screens.rates

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.ArroBackend
import app.arro.core.Catalog
import app.arro.core.Currency
import app.arro.core.Insights
import app.arro.core.Money
import app.arro.core.PayoutType
import app.arro.core.Rates
import app.arro.core.Text
import app.arro.ui.BannerKind
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.appState
import app.arro.ui.back
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.launchCall
import app.arro.ui.nowTicker
import app.arro.ui.rememberCall
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.BarCaption
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.Chevron
import app.arro.ui.components.Chip
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.Flag
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.HDivider
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.LinkText
import app.arro.ui.components.ListRow
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.Segmented
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Toggle
import app.arro.ui.components.TopBar
import app.arro.ui.components.TrailingValue
import app.arro.ui.components.Txt
import app.arro.ui.components.selectionRing
import app.arro.ui.screens.send.startSend
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlin.math.abs
import kotlin.math.roundToInt

private fun arrow(v: Double) = if (v >= 0) "▲" else "▼"
private fun changeColor(v: Double) = if (v >= 0) C.Ok else C.Err

/** Customer-rate history for GBP → [cur]: mid-market history with our margin taken out. */
private fun customerHistory(rates: app.arro.core.RatesSnapshot, cur: Currency, points: Int, now: Long, hourly: Boolean): List<Double> =
    Rates.history(rates, cur, points, now, hourly).map { it * (1 - Rates.TRANSFER_MARGIN) }

@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(56.dp, 24.dp)) {
        if (values.size < 2) return@Canvas
        val lo = values.min(); val hi = values.max(); val span = (hi - lo).takeIf { it > 0 } ?: 1.0
        val pad = 3.dp.toPx()
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * size.width / (values.size - 1)
            val y = pad + ((hi - v) / span * (size.height - 2 * pad)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ------------------------------------------------------------------ 9.01 Exchange rates

@Composable
fun RatesScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val now = nowTicker(60_000)
    var alertsOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val active = a.rateAlerts.filter { it.triggeredAt == 0L && it.expiresAt > now }
    val fetched = state.rates.fetchedAt
    ArroScreen(
        topBar = {
            TopBar(onBack = { nav.back() }, right = {
                ArroButton(if (active.isEmpty()) "Alerts" else "Alerts · ${active.size}", { alertsOpen = true }, style = Btn.Lime, size = BtnSize.S, icon = Icons.Bell, fill = false)
            })
        },
        gap = 14.dp,
        overlay = {
            Sheet(alertsOpen, { alertsOpen = false }) {
                SheetHeader("Rate alerts", { alertsOpen = false }, "We'll tell you when £1 buys more. Alerts last 30 days.")
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (a.rateAlerts.isEmpty()) Cap("No alerts yet. Choose a currency to set your first one.")
                    a.rateAlerts.sortedByDescending { it.createdAt }.forEach { al ->
                        val cur = Currency.of(al.to)
                        val reached = al.triggeredAt > 0
                        val expired = !reached && al.expiresAt <= now
                        ListRow("GBP → ${al.to} at ${Money.rate(al.target, Money.rateDecimals(al.target))}",
                            subtitle = when {
                                reached -> "Reached ${Text.relativeDay(al.triggeredAt, now)}"
                                expired -> "Expired"
                                else -> "Now ${Money.rate(b.customerRate(cur), Money.rateDecimals(al.target))} · ends ${Text.dayMonth(al.expiresAt)}" +
                                    (a.recipient(al.autoSendRecipientId)?.let { " · auto-sends to ${it.firstName}" } ?: "")
                            },
                            leading = { Flag(cur.flag, 32.dp) },
                            trailing = { RoundIcon(Icons.Trash, { b.deleteRateAlert(al.id); banner.show("Alert deleted", kind = BannerKind.Info) }, style = IconBtn.Soft, size = 36.dp, iconSize = 16.dp, description = "Delete alert") },
                            onClick = { alertsOpen = false; nav.go(Routes.rateAlert(al.to)) })
                    }
                    ArroButton("New alert", { alertsOpen = false; nav.go(Routes.rateAlert(active.firstOrNull()?.to?.let { t -> Rates.payoutCurrencies.firstOrNull { it.code != t }?.code } ?: "RWF")) }, icon = Icons.Plus)
                }
            }
        },
    ) {
        ScreenTitle("Exchange rates", "What £1 buys right now, with our margin included." + if (fetched > 0) " Updated ${Text.time(fetched)}." else "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("From GBP", true, { banner.show("Rates are shown from GBP", "Transfers are paid in pounds. More source currencies are coming.") }, flag = "gb", icon = Icons.ChevronDown)
            Cap("24-hour change", Modifier.weight(1f))
            LinkText(if (call.busy.value) "Updating…" else "Refresh", {
                scope.launchCall(call) {
                    val ok = b.refreshRates()
                    banner.show(if (ok) "Rates updated" else "Couldn't reach the rates service", if (ok) "Live mid-market rates, with our margin included." else "Showing the last rates we had.", if (ok) BannerKind.Success else BannerKind.Error)
                }
            }, color = C.Ink, strong = true)
        }
        Group {
            Rates.payoutCurrencies.forEachIndexed { i, cur ->
                if (i > 0) GroupDivider()
                val rate = b.customerRate(cur)
                val ch = Rates.change24h(state.rates, cur, now)
                val spark = remember(cur, now / 3_600_000L, state.rates.fetchedAt) { customerHistory(state.rates, cur, 8, now, hourly = true) }
                ListRow(cur.code, flat = true, minHeight = 58.dp, subtitle = cur.displayName,
                    leading = { Flag(cur.flag, 32.dp) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Sparkline(spark, changeColor(ch))
                            Box(Modifier.width(84.dp), contentAlignment = Alignment.CenterEnd) {
                                TrailingValue(Money.rate(rate, Money.rateDecimals(rate)), "${arrow(ch)} ${Money.percent(abs(ch))}", changeColor(ch))
                            }
                        }
                    },
                    onClick = { nav.go(Routes.rate(cur.code)) })
            }
        }
        LinkText("How we price, and how we compare", { nav.go(Routes.rateCompare()) }, color = C.Ink, strong = true, underline = true)
    }
}

// ------------------------------------------------------------------ 9.02 Rate detail

private enum class Range(val label: String, val points: Int, val hourly: Boolean, val stride: Int) {
    D1("1D", 25, true, 1), W1("1W", 169, true, 4), M1("1M", 31, false, 1), M6("6M", 183, false, 3), Y1("1Y", 366, false, 6)
}

@Composable
fun RateDetailScreen(code: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val cur = Currency.of(code)
    val now = nowTicker(60_000)
    var range by rememberSaveable { mutableStateOf(Range.M1) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val rate = b.customerRate(cur)
    val mid = b.midRate(cur)
    val series = remember(cur, range, now / 600_000L, state.rates.fetchedAt) {
        customerHistory(state.rates, cur, range.points, now, range.hourly).filterIndexed { i, _ -> i % range.stride == 0 || i == range.points - 1 }
    }
    val day = remember(cur, now / 600_000L, state.rates.fetchedAt) { customerHistory(state.rates, cur, 25, now, true) }
    val month = remember(cur, now / 600_000L, state.rates.fetchedAt) { customerHistory(state.rates, cur, 31, now, false) }
    val dayChange = Rates.change24h(state.rates, cur, now)
    val dayAbs = rate - rate / (1 + dayChange / 100)
    val monthChange = Rates.change(month)
    val decimals = Money.rateDecimals(rate)
    val country = Catalog.countryForCurrency(cur)
    val sample = 25_000L
    val sends = Money.convert(sample, Currency.GBP, cur, rate)
    val stepMs = if (range.hourly) 3_600_000L * range.stride else 86_400_000L * range.stride
    val sel = selected?.coerceIn(0, series.lastIndex)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "GBP → ${cur.code}", right = { RoundIcon(Icons.Bell, { nav.go(Routes.rateAlert(cur.code)) }, style = IconBtn.Soft, description = "Set rate alert") }) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton("Set alert", { nav.go(Routes.rateAlert(cur.code)) }, Modifier.weight(1f), style = Btn.Line, fill = true)
                ArroButton("Send at this rate", { if (country != null) startSend(c, nav, banner, country = country.code) }, Modifier.weight(1.4f), fill = true, enabled = country != null)
            }
        },
    ) {
        Column {
            Cap(if (sel != null) "On ${if (range.hourly) Text.dayMonth(now - (series.lastIndex - sel) * stepMs) + ", " + Text.time(now - (series.lastIndex - sel) * stepMs) else Text.date(now - (series.lastIndex - sel) * stepMs)}, 1 British pound bought"
                else "1 British pound buys")
            val shown = if (sel != null) series[sel] else rate
            val whole = Money.rate(shown, decimals)
            val dot = whole.indexOf('.')
            Txt(buildAnnotatedString {
                append(if (dot > 0) whole.substring(0, dot) else whole)
                withStyle(SpanStyle(fontSize = 24.sp, color = C.Slate)) { append((if (dot > 0) whole.substring(dot) else "") + " " + cur.code) }
            }, T.Amount, Modifier.padding(top = 6.dp))
            Txt(buildAnnotatedString {
                withStyle(SpanStyle(color = changeColor(dayChange))) { append("${arrow(dayChange)} ${Money.rate(abs(dayAbs), decimals)} (${Money.percent(abs(dayChange))}) today") }
                withStyle(SpanStyle(color = C.Slate)) { append(" · ") }
                withStyle(SpanStyle(color = changeColor(monthChange))) { append("${arrow(monthChange)} ${Money.percent(abs(monthChange))} this month") }
            }, T.Cap.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 8.dp))
        }
        Segmented(Range.entries.map { it.label }, range.ordinal, { range = Range.entries[it]; selected = null })
        RateChart(series, sel, { selected = it }, decimals,
            startLabel = if (range == Range.D1) "24 hours ago" else Text.dayMonth(now - (range.points - 1) * (if (range.hourly) 3_600_000L else 86_400_000L)))
        Panel(gap = 10.dp) {
            KV("Mid-market rate", Money.rate(mid, decimals))
            KV("Our margin", Money.percent(Rates.TRANSFER_MARGIN * 100, 2))
            KV("${Money.format(sample, Currency.GBP, false)} sends", Money.format(sends, cur), strong = true)
        }
        val lockNow = a.offerLock(cur.code, now)
        val offerNow = a.liveOffer(now)?.takeIf { it.to == cur.code }
        ListRow(
            when {
                lockNow != null -> "Rate locked until ${Text.time(lockNow.expiresAt)}"
                offerNow != null -> "Rate offer: ${Money.rate(offerNow.rate, decimals)} ${cur.code}"
                else -> "Lock a rate"
            },
            subtitle = when {
                lockNow != null -> "${Money.rateLine(Currency.GBP, cur, lockNow.rate)} for your next transfer"
                offerNow != null -> "Lock it for ${offerNow.lockMinutes} minutes before ${Text.time(offerNow.expiresAt)}"
                else -> "Hold a better rate for ${ArroBackend.OFFER_LOCK_MINUTES} minutes while you send"
            },
            minHeight = 56.dp,
            leading = { app.arro.ui.components.Tile(Icons.Lock, app.arro.ui.components.TileStyle.Lime, 36.dp, iconSize = 16.dp) }, trailing = { Chevron() },
            onClick = { nav.go(Routes.lockRate(cur.code)) },
        )
        if (a.rateAlerts.any { it.to == cur.code && it.triggeredAt == 0L }) {
            val al = a.rateAlerts.first { it.to == cur.code && it.triggeredAt == 0L }
            ListRow("Alert at ${Money.rate(al.target, decimals)}", subtitle = "We'll tell you when £1 buys this much", minHeight = 56.dp,
                leading = { app.arro.ui.components.Tile(Icons.Bell, app.arro.ui.components.TileStyle.Lime, 36.dp, iconSize = 16.dp) }, trailing = { Chevron() },
                onClick = { nav.go(Routes.rateAlert(cur.code)) })
        }
    }
}

@Composable
private fun RateChart(series: List<Double>, selected: Int?, onSelect: (Int?) -> Unit, decimals: Int, startLabel: String) {
    if (series.size < 2) return
    val hi = series.max(); val lo = series.min()
    val hiIdx = series.indexOf(hi)
    Column {
        Cap("High ${Money.rate(hi, decimals)}", size = 11f)
        BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 4.dp)) {
            val n = series.size
            fun indexAt(x: Float, width: Float) = ((x / width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
            Canvas(
                Modifier.fillMaxWidth().height(142.dp)
                    .pointerInput(series) {
                        detectDragGestures(onDragStart = { o -> onSelect(indexAt(o.x, size.width.toFloat())) }, onDragEnd = { onSelect(null) }, onDragCancel = { onSelect(null) }) { change, _ ->
                            onSelect(indexAt(change.position.x, size.width.toFloat()))
                        }
                    }
                    .pointerInput(series) { detectTapGestures(onPress = { o -> onSelect(indexAt(o.x, size.width.toFloat())); tryAwaitRelease(); onSelect(null) }) },
            ) {
                val span = (hi - lo).takeIf { it > 0 } ?: 1.0
                val top = 8.dp.toPx(); val bottom = size.height - 8.dp.toPx()
                fun x(i: Int) = i * (size.width - 8.dp.toPx()) / (n - 1)
                fun y(v: Double) = (top + (hi - v) / span * (bottom - top)).toFloat()
                val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                drawLine(C.Line, Offset(0f, top), Offset(size.width, top), 1.dp.toPx(), pathEffect = dash)
                drawLine(C.Line, Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx(), pathEffect = dash)
                val line = Path()
                series.forEachIndexed { i, v -> if (i == 0) line.moveTo(x(i), y(v)) else line.lineTo(x(i), y(v)) }
                val fill = Path().apply { addPath(line); lineTo(x(n - 1), size.height); lineTo(0f, size.height); close() }
                drawPath(fill, Brush.verticalGradient(listOf(C.Lime.copy(alpha = 0.55f), C.Lime.copy(alpha = 0f))))
                drawPath(line, C.Ink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val dotIdx = selected ?: (n - 1)
                if (selected != null) drawLine(C.Ink.copy(alpha = 0.35f), Offset(x(dotIdx), 0f), Offset(x(dotIdx), size.height), 1.dp.toPx())
                drawCircle(C.Lime, 6.dp.toPx(), Offset(x(dotIdx), y(series[dotIdx])))
                drawCircle(C.Ink, 6.dp.toPx(), Offset(x(dotIdx), y(series[dotIdx])), style = Stroke(2.5.dp.toPx()))
                if (selected == null && hiIdx != n - 1) drawCircle(C.Ink, 3.dp.toPx(), Offset(x(hiIdx), y(hi)))
            }
        }
        Cap("Low ${Money.rate(lo, decimals)}", size = 11f)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Cap(startLabel, size = 11f); Cap("Now", size = 11f)
        }
    }
}

// ------------------------------------------------------------------ 9.03 Rate alert

@Composable
fun RateAlertScreen(code: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val cur = Currency.of(code)
    val now = b.customerRate(cur)
    val decimals = Money.rateDecimals(now)
    val existing = a.rateAlerts.firstOrNull { it.to == cur.code && it.triggeredAt == 0L }
    val lo = now * 0.985
    val hi = now * 1.0165
    val step = if (decimals == 4) 0.0001 else if (now > 1000) 0.5 else 0.01
    fun snap(v: Double) = Rates.round((v / step).roundToInt() * step)
    var target by rememberSaveable { mutableDoubleStateOf(existing?.target ?: snap(now * 1.0064)) }
    var preset by rememberSaveable { mutableIntStateOf(if (existing == null) 0 else 2) }
    var customText by rememberSaveable { mutableStateOf("") }
    var push by rememberSaveable { mutableStateOf(existing?.push ?: true) }
    val country = Catalog.countryForCurrency(cur)
    val autoRecipient = a.transfers.sortedByDescending { it.createdAt }.firstOrNull { it.to == cur.code }?.let { a.recipient(it.recipientId) }
        ?: a.recipients.firstOrNull { it.country == country?.code }
    val autoMinor = a.transfers.firstOrNull { it.recipientId == autoRecipient?.id }?.sendMinor ?: 25_000L
    var auto by rememberSaveable { mutableStateOf(existing?.autoSendRecipientId?.isNotBlank() == true) }
    val p05 = snap(now * 1.005)
    val p1 = snap(now * 1.01)
    val others = a.rateAlerts.filter { it.to != cur.code && it.triggeredAt == 0L }
    val below = target <= now
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Rate alert") },
        footer = {
            if (existing != null) ArroButton("Delete this alert", { b.deleteRateAlert(existing.id); banner.show("Alert deleted", kind = BannerKind.Info); nav.back() }, style = Btn.DangerText)
            ArroButton(if (existing != null) "Update alert" else "Create alert", {
                b.createRateAlert(cur, target, push, if (auto) autoRecipient?.id ?: "" else "", if (auto) autoMinor else 0)
                banner.show(if (below) "Your rate is already there" else "Alert set",
                    if (below) "£1 buys ${Money.rate(now, decimals)} ${cur.code} right now." else "We'll tell you when £1 buys ${Money.rate(target, decimals)} ${cur.code}.", BannerKind.Success)
                nav.back()
            }, enabled = target > 0)
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.Bell, 72.dp)
            Column {
                Txt("Tell me when £1 buys", T.H2)
                Cap("GBP → ${cur.code} · now ${Money.rate(now, decimals)}", Modifier.padding(top = 2.dp))
            }
        }
        ArroCard(Modifier.selectionRing(18.dp, 4.dp), padding = 18.dp, border = C.Ink) {
            FieldLabel("Target rate")
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Txt(Money.rate(target, decimals), T.Big36.copy(fontSize = 38.sp), Modifier.weight(1f))
                Txt(cur.code, T.H3, Modifier.padding(bottom = 6.dp))
            }
            RateSlider(((target - lo) / (hi - lo)).toFloat().coerceIn(0f, 1f), ((now - lo) / (hi - lo)).toFloat()) { f ->
                target = snap(lo + f * (hi - lo)); preset = 2; customText = ""
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap(Money.rate(lo, if (decimals == 4) 4 else 0), size = 12f)
                Cap("Now ${Money.rate(now, if (decimals == 4) 4 else 0)}", size = 12f)
                Cap(Money.rate(hi, if (decimals == 4) 4 else 0), size = 12f)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("+0.5% · ${Money.rate(p05, decimals)}", preset == 0, { preset = 0; target = p05 })
            Chip("+1% · ${Money.rate(p1, decimals)}", preset == 1, { preset = 1; target = p1 })
            Chip("Custom", preset == 2, { preset = 2 })
        }
        if (preset == 2) ArroField(customText, { v ->
            customText = v.filter { it.isDigit() || it == '.' }.take(12)
            customText.replace(",", "").toDoubleOrNull()?.let { if (it > 0) target = Rates.round(it) }
        }, label = "Exact target", placeholder = Money.rate(target, decimals).replace(",", ""), keyboard = KeyboardType.Decimal,
            hint = if (below) "That's at or below today's rate, so we'd tell you straight away." else null)
        else if (below) FieldHint(hint = "That's at or below today's rate, so we'd tell you straight away.")
        Group {
            ListRow("Notify me", flat = true, subtitle = "Push and email", trailing = { Toggle(push, { push = it }) })
            if (autoRecipient != null) {
                GroupDivider()
                ListRow("Send ${Money.format(autoMinor, Currency.GBP, false)} to ${autoRecipient.firstName} automatically", flat = true,
                    subtitle = "Paid from your GBP balance when the rate hits", trailing = { Toggle(auto, { auto = it }) })
            }
        }
        if (auto && (a.wallet(Currency.GBP)?.balanceMinor ?: 0) < autoMinor + 300) Cap("Top up your GBP balance before then, or the automatic send can't go.", color = C.Warn, size = 12f)
        Cap("Alerts last 30 days." + if (others.isNotEmpty()) " Your other ${if (others.size == 1) "alert" else "alerts"}: " +
            others.joinToString { "GBP → ${it.to} at ${Money.rate(it.target, Money.rateDecimals(it.target))}" } + "." else "", size = 12f)
        if (existing != null) Cap("Saving replaces your current ${cur.code} alert at ${Money.rate(existing.target, decimals)}.", size = 12f)
    }
}

@Composable
private fun RateSlider(fraction: Float, nowFraction: Float, onChange: (Float) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 14.dp).height(28.dp)
        .pointerInput(Unit) { detectTapGestures { o -> onChange((o.x / size.width).coerceIn(0f, 1f)) } }
        .pointerInput(Unit) { detectDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) } },
    ) {
        val w = maxWidth
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp).background(C.Line, RoundedCornerShape(2.dp)))
        Box(Modifier.width(w * fraction).padding(top = 12.dp).height(4.dp).background(C.Ink, RoundedCornerShape(2.dp)))
        Box(Modifier.offset(x = w * nowFraction - 1.dp, y = 8.dp).size(2.dp, 12.dp).background(C.Slate.copy(alpha = 0.5f)))
        Box(Modifier.offset(x = w * fraction - 12.dp, y = 2.dp).size(24.dp).clip(CircleShape).background(C.Lime).border(3.dp, C.Ink, CircleShape))
    }
}

// ------------------------------------------------------------------ 9.04 What it really costs

@Composable
fun RateCompareScreen(code: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val state by appState()
    val a = state.account ?: return
    val draft = c.session.send
    val cur = Currency.of(code).takeIf { it in Rates.payoutCurrencies } ?: Currency.RWF
    val country = Catalog.countryForCurrency(cur) ?: Catalog.country("RW")
    val fromDraft = draft.quote?.takeIf { it.to == cur }
    val sendMinor = fromDraft?.sendMinor ?: (Money.parse(draft.amountText, Currency.GBP)?.takeIf { draft.country == country.code && it >= 100 } ?: 25_000L)
    val type = fromDraft?.payoutType ?: if (draft.country == country.code) draft.payoutType else country.options.first().type
    val q = b.quote(cur, sendMinor, type, country.code, null)
    val name = a.recipient(draft.recipientId)?.takeIf { it.country == country.code }?.firstName ?: "Your recipient"
    val mid = q.mid
    val others = listOf(Other("Typical high-street bank", 1500, 0.030, C.Err), Other("Typical transfer app", 399, 0.0143, C.Warn))
    val decimals = Money.rateDecimals(mid)
    fun receive(margin: Double) = Money.convert(sendMinor, Currency.GBP, cur, Rates.round(mid * (1 - margin)))
    fun cost(fee: Long, margin: Double) = fee + Math.round(sendMinor * margin)
    val arroCost = q.feeMinor + q.marginMinor
    val gains = others.map { q.receiveMinor - receive(it.margin) }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("${Money.format(sendMinor, Currency.GBP, false)} to ${country.name} · ${type.label.lowercase()}") }) },
    ) {
        ScreenTitle("What it really costs", "The fee plus the margin hidden in a rate. We show both.")
        ArroCard(padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt("Arro", T.H3, Modifier.weight(1f)); Pill("Total cost ${Money.format(arroCost, Currency.GBP)}", PillKind.Lime)
            }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (q.feeMinor > 0) Box(Modifier.weight(q.feeMinor.toFloat()).height(12.dp).background(C.Ink))
                if (q.marginMinor > 0) Box(Modifier.weight(q.marginMinor.toFloat()).height(12.dp).background(C.LimeDeep))
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Legend(C.Ink, if (q.feeWaived) "Fee £0.00 · first transfer" else "Fee ${Money.format(q.feeMinor, Currency.GBP)}")
                Legend(C.LimeDeep, "Rate margin ${Money.format(q.marginMinor, Currency.GBP)}")
            }
            HDivider(Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Cap("$name receives", Modifier.weight(1f)); Txt(Money.format(q.receiveMinor, cur), T.Num.copy(fontSize = 18.sp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Over("Same transfer elsewhere")
            Group {
                others.forEachIndexed { i, o ->
                    if (i > 0) GroupDivider()
                    ListRow(o.label, flat = true, subtitle = "Fee ${Money.format(o.feeMinor, Currency.GBP)} · rate ${Money.rate(Rates.round(mid * (1 - o.margin)), decimals)}",
                        verticalAlignment = Alignment.Top,
                        trailing = { TrailingValue(Money.format(receive(o.margin), cur), "costs ${Money.format(cost(o.feeMinor, o.margin), Currency.GBP)}", o.color) })
                }
            }
        }
        Panel(kind = PanelKind.Lime, gap = 4.dp) {
            Txt("$name gets more with Arro", T.Label, color = C.Forest)
            Txt("+ ${Money.format(gains.min(), cur, false)} to ${Money.format(gains.max(), cur, false)}", T.Num.copy(fontSize = 18.sp))
        }
        Cap("Other providers' prices are typical published fees and rate margins for UK banks and transfer apps, for illustration. " +
            "Mid-market rate ${Money.rate(mid, decimals)} at ${Text.time(state.rates.fetchedAt.takeIf { it > 0 } ?: b.now)}.", size = 11.5f)
    }
}

/** Typical competitor pricing, used for illustration only. */
private data class Other(val label: String, val feeMinor: Long, val margin: Double, val color: Color)

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Cap(text, size = 12f)
    }
}
