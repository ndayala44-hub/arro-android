package app.arro.ui.screens.rates

import app.arro.R
import app.arro.ui.str
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

/** "Rates by Exchange Rate API", linked, as the free rates endpoint's terms ask on every page that shows its rates. */
@Composable
fun RatesAttribution(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LinkText(str(R.string.rates_attribution), { app.arro.platform.Share.openUrl(context, str(R.string.rates_attribution_url)) }, modifier, color = C.Slate, underline = true)
}

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
                ArroButton(if (active.isEmpty()) str(R.string.rates_alerts) else str(R.string.rates_alerts_x, active.size), { alertsOpen = true }, style = Btn.Lime, size = BtnSize.S, icon = Icons.Bell, fill = false)
            })
        },
        gap = 14.dp,
        overlay = {
            Sheet(alertsOpen, { alertsOpen = false }) {
                SheetHeader(str(R.string.rates_rate_alerts), { alertsOpen = false }, str(R.string.rates_we_ll_tell_you_when_1))
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (a.rateAlerts.isEmpty()) Cap(str(R.string.rates_no_alerts_yet_choose_a_currency))
                    a.rateAlerts.sortedByDescending { it.createdAt }.forEach { al ->
                        val cur = Currency.of(al.to)
                        val reached = al.triggeredAt > 0
                        val expired = !reached && al.expiresAt <= now
                        ListRow(str(R.string.rates_gbp_x_at_x, al.to, Money.rate(al.target, Money.rateDecimals(al.target))),
                            subtitle = when {
                                reached -> str(R.string.rates_reached_x, Text.relativeDay(al.triggeredAt, now))
                                expired -> str(R.string.rates_expired)
                                else -> str(R.string.rates_now_x_ends_x, Money.rate(b.customerRate(cur), Money.rateDecimals(al.target)), Text.dayMonth(al.expiresAt)) +
                                    (a.recipient(al.autoSendRecipientId)?.let { str(R.string.rates_auto_sends_to_x, it.firstName) } ?: "")
                            },
                            leading = { Flag(cur.flag, 32.dp) },
                            trailing = { RoundIcon(Icons.Trash, { b.deleteRateAlert(al.id); banner.show(str(R.string.rates_alert_deleted), kind = BannerKind.Info) }, style = IconBtn.Soft, size = 36.dp, iconSize = 16.dp, description = str(R.string.rates_delete_alert)) },
                            onClick = { alertsOpen = false; nav.go(Routes.rateAlert(al.to)) })
                    }
                    ArroButton(str(R.string.rates_new_alert), { alertsOpen = false; nav.go(Routes.rateAlert(active.firstOrNull()?.to?.let { t -> Rates.payoutCurrencies.firstOrNull { it.code != t }?.code } ?: "RWF")) }, icon = Icons.Plus)
                }
            }
        },
    ) {
        ScreenTitle(str(R.string.rates_exchange_rates), str(R.string.rates_what_1_buys_right_now_with) + if (fetched > 0) str(R.string.rates_updated_x, Text.time(fetched)) else "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(str(R.string.rates_from_gbp), true, { banner.show(str(R.string.rates_rates_are_shown_from_gbp), str(R.string.rates_transfers_are_paid_in_pounds_more)) }, flag = "gb", icon = Icons.ChevronDown)
            Cap(str(R.string.rates_n_24_hour_change), Modifier.weight(1f))
            LinkText(if (call.busy.value) str(R.string.rates_updating) else str(R.string.rates_refresh), {
                scope.launchCall(call) {
                    val ok = b.refreshRates()
                    banner.show(if (ok) str(R.string.rates_rates_updated) else str(R.string.rates_couldn_t_reach_the_rates_service), if (ok) str(R.string.rates_live_mid_market_rates_with_our) else str(R.string.rates_showing_the_last_rates_we_had), if (ok) BannerKind.Success else BannerKind.Error)
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
        LinkText(str(R.string.rates_how_we_price_and_how_we), { nav.go(Routes.rateCompare()) }, color = C.Ink, strong = true, underline = true)
        // Attribution required by the free ExchangeRate-API endpoint that supplies the live rates.
        RatesAttribution()
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
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.rates_gbp_x, cur.code), right = { RoundIcon(Icons.Bell, { nav.go(Routes.rateAlert(cur.code)) }, style = IconBtn.Soft, description = str(R.string.rates_set_rate_alert)) }) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton(str(R.string.rates_set_alert), { nav.go(Routes.rateAlert(cur.code)) }, Modifier.weight(1f), style = Btn.Line, fill = true)
                ArroButton(str(R.string.rates_send_at_this_rate), { if (country != null) startSend(c, nav, banner, country = country.code) }, Modifier.weight(1.4f), fill = true, enabled = country != null)
            }
        },
    ) {
        Column {
            Cap(if (sel != null) str(R.string.rates_on_x_1_british_pound_bought, if (range.hourly) Text.dayMonth(now - (series.lastIndex - sel) * stepMs) + ", " + Text.time(now - (series.lastIndex - sel) * stepMs) else Text.date(now - (series.lastIndex - sel) * stepMs))
                else str(R.string.rates_n_1_british_pound_buys))
            val shown = if (sel != null) series[sel] else rate
            val whole = Money.rate(shown, decimals)
            val dot = whole.indexOf('.')
            Txt(buildAnnotatedString {
                append(if (dot > 0) whole.substring(0, dot) else whole)
                withStyle(SpanStyle(fontSize = 24.sp, color = C.Slate)) { append((if (dot > 0) whole.substring(dot) else "") + " " + cur.code) }
            }, T.Amount, Modifier.padding(top = 6.dp))
            Txt(buildAnnotatedString {
                withStyle(SpanStyle(color = changeColor(dayChange))) { append(str(R.string.rates_x_x_x_today, arrow(dayChange), Money.rate(abs(dayAbs), decimals), Money.percent(abs(dayChange)))) }
                withStyle(SpanStyle(color = C.Slate)) { append(" · ") }
                withStyle(SpanStyle(color = changeColor(monthChange))) { append(str(R.string.rates_x_x_this_month, arrow(monthChange), Money.percent(abs(monthChange)))) }
            }, T.Cap.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 8.dp))
        }
        Segmented(Range.entries.map { it.label }, range.ordinal, { range = Range.entries[it]; selected = null })
        RateChart(series, sel, { selected = it }, decimals,
            startLabel = if (range == Range.D1) str(R.string.rates_n_24_hours_ago) else Text.dayMonth(now - (range.points - 1) * (if (range.hourly) 3_600_000L else 86_400_000L)))
        Panel(gap = 10.dp) {
            KV(str(R.string.rates_mid_market_rate), Money.rate(mid, decimals))
            KV(str(R.string.rates_our_margin), Money.percent(Rates.TRANSFER_MARGIN * 100, 2))
            KV(str(R.string.rates_x_sends, Money.format(sample, Currency.GBP, false)), Money.format(sends, cur), strong = true)
        }
        val lockNow = a.offerLock(cur.code, now)
        val offerNow = a.liveOffer(now)?.takeIf { it.to == cur.code }
        ListRow(
            when {
                lockNow != null -> str(R.string.rates_rate_locked_until_x, Text.time(lockNow.expiresAt))
                offerNow != null -> str(R.string.rates_rate_offer_x_x, Money.rate(offerNow.rate, decimals), cur.code)
                else -> str(R.string.rates_lock_a_rate)
            },
            subtitle = when {
                lockNow != null -> str(R.string.rates_x_for_your_next_transfer, Money.rateLine(Currency.GBP, cur, lockNow.rate))
                offerNow != null -> str(R.string.rates_lock_it_for_x_minutes_before, offerNow.lockMinutes, Text.time(offerNow.expiresAt))
                else -> str(R.string.rates_hold_a_better_rate_for_x, ArroBackend.OFFER_LOCK_MINUTES)
            },
            minHeight = 56.dp,
            leading = { app.arro.ui.components.Tile(Icons.Lock, app.arro.ui.components.TileStyle.Lime, 36.dp, iconSize = 16.dp) }, trailing = { Chevron() },
            onClick = { nav.go(Routes.lockRate(cur.code)) },
        )
        if (a.rateAlerts.any { it.to == cur.code && it.triggeredAt == 0L }) {
            val al = a.rateAlerts.first { it.to == cur.code && it.triggeredAt == 0L }
            ListRow(str(R.string.rates_alert_at_x, Money.rate(al.target, decimals)), subtitle = str(R.string.rates_we_ll_tell_you_when_1_2), minHeight = 56.dp,
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
        Cap(str(R.string.rates_high_x, Money.rate(hi, decimals)), size = 11f)
        Box(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 4.dp)) {
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
        Cap(str(R.string.rates_low_x, Money.rate(lo, decimals)), size = 11f)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Cap(startLabel, size = 11f); Cap(str(R.string.rates_now), size = 11f)
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
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.rates_rate_alert)) },
        footer = {
            if (existing != null) ArroButton(str(R.string.rates_delete_this_alert), { b.deleteRateAlert(existing.id); banner.show(str(R.string.rates_alert_deleted), kind = BannerKind.Info); nav.back() }, style = Btn.DangerText)
            ArroButton(if (existing != null) str(R.string.rates_update_alert) else str(R.string.rates_create_alert), {
                b.createRateAlert(cur, target, push, if (auto) autoRecipient?.id ?: "" else "", if (auto) autoMinor else 0)
                banner.show(if (below) str(R.string.rates_your_rate_is_already_there) else str(R.string.rates_alert_set),
                    if (below) str(R.string.rates_n_1_buys_x_x_right_now, Money.rate(now, decimals), cur.code) else str(R.string.rates_we_ll_tell_you_when_1_3, Money.rate(target, decimals), cur.code), BannerKind.Success)
                nav.back()
            }, enabled = target > 0)
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.Bell, 72.dp)
            Column {
                Txt(str(R.string.rates_tell_me_when_1_buys), T.H2)
                Cap(str(R.string.rates_gbp_x_now_x, cur.code, Money.rate(now, decimals)), Modifier.padding(top = 2.dp))
            }
        }
        ArroCard(Modifier.selectionRing(18.dp, 4.dp), padding = 18.dp, border = C.Ink) {
            FieldLabel(str(R.string.rates_target_rate))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Txt(Money.rate(target, decimals), T.Big36.copy(fontSize = 38.sp), Modifier.weight(1f))
                Txt(cur.code, T.H3, Modifier.padding(bottom = 6.dp))
            }
            RateSlider(((target - lo) / (hi - lo)).toFloat().coerceIn(0f, 1f), ((now - lo) / (hi - lo)).toFloat()) { f ->
                target = snap(lo + f * (hi - lo)); preset = 2; customText = ""
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap(Money.rate(lo, if (decimals == 4) 4 else 0), size = 12f)
                Cap(str(R.string.rates_now_x, Money.rate(now, if (decimals == 4) 4 else 0)), size = 12f)
                Cap(Money.rate(hi, if (decimals == 4) 4 else 0), size = 12f)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("+0.5% · ${Money.rate(p05, decimals)}", preset == 0, { preset = 0; target = p05 })
            Chip("+1% · ${Money.rate(p1, decimals)}", preset == 1, { preset = 1; target = p1 })
            Chip(str(R.string.rates_custom), preset == 2, { preset = 2 })
        }
        if (preset == 2) ArroField(customText, { v ->
            customText = v.filter { it.isDigit() || it == '.' }.take(12)
            customText.replace(",", "").toDoubleOrNull()?.let { if (it > 0) target = Rates.round(it) }
        }, label = str(R.string.rates_exact_target), placeholder = Money.rate(target, decimals).replace(",", ""), keyboard = KeyboardType.Decimal,
            hint = if (below) str(R.string.rates_that_s_at_or_below_today) else null)
        else if (below) FieldHint(hint = str(R.string.rates_that_s_at_or_below_today))
        Group {
            ListRow(str(R.string.rates_notify_me), flat = true, subtitle = str(R.string.rates_push_and_email), trailing = { Toggle(push, { push = it }) })
            if (autoRecipient != null) {
                GroupDivider()
                ListRow(str(R.string.rates_send_x_to_x_automatically, Money.format(autoMinor, Currency.GBP, false), autoRecipient.firstName), flat = true,
                    subtitle = str(R.string.rates_paid_from_your_gbp_balance_when), trailing = { Toggle(auto, { auto = it }) })
            }
        }
        if (auto && (a.wallet(Currency.GBP)?.balanceMinor ?: 0) < autoMinor + 300) Cap(str(R.string.rates_top_up_your_gbp_balance_before), color = C.Warn, size = 12f)
        Cap(str(R.string.rates_alerts_last_30_days) + if (others.isNotEmpty()) str(R.string.rates_your_other_x, if (others.size == 1) str(R.string.rates_alert) else str(R.string.rates_alerts_2)) +
            others.joinToString { str(R.string.rates_gbp_x_at_x, it.to, Money.rate(it.target, Money.rateDecimals(it.target))) } + "." else "", size = 12f)
        if (existing != null) Cap(str(R.string.rates_saving_replaces_your_current_x_alert, cur.code, Money.rate(existing.target, decimals)), size = 12f)
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
    val name = a.recipient(draft.recipientId)?.takeIf { it.country == country.code }?.firstName ?: str(R.string.rates_your_recipient)
    val mid = q.mid
    val others = listOf(Other(str(R.string.rates_typical_high_street_bank), 1500, 0.030, C.Err), Other(str(R.string.rates_typical_transfer_app), 399, 0.0143, C.Warn))
    val decimals = Money.rateDecimals(mid)
    fun receive(margin: Double) = Money.convert(sendMinor, Currency.GBP, cur, Rates.round(mid * (1 - margin)))
    fun cost(fee: Long, margin: Double) = fee + Math.round(sendMinor * margin)
    val arroCost = q.feeMinor + q.marginMinor
    val gains = others.map { q.receiveMinor - receive(it.margin) }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.rates_x_to_x_x, Money.format(sendMinor, Currency.GBP, false), country.name, type.label.lowercase())) }) },
    ) {
        ScreenTitle(str(R.string.rates_what_it_really_costs), str(R.string.rates_the_fee_plus_the_margin_hidden))
        ArroCard(padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt(str(R.string.rates_arro), T.H3, Modifier.weight(1f)); Pill(str(R.string.rates_total_cost_x, Money.format(arroCost, Currency.GBP)), PillKind.Lime)
            }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (q.feeMinor > 0) Box(Modifier.weight(q.feeMinor.toFloat()).height(12.dp).background(C.Ink))
                if (q.marginMinor > 0) Box(Modifier.weight(q.marginMinor.toFloat()).height(12.dp).background(C.LimeDeep))
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Legend(C.Ink, if (q.feeWaived) str(R.string.rates_fee_0_00_first_transfer) else str(R.string.rates_fee_x, Money.format(q.feeMinor, Currency.GBP)))
                Legend(C.LimeDeep, str(R.string.rates_rate_margin_x, Money.format(q.marginMinor, Currency.GBP)))
            }
            HDivider(Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Cap(str(R.string.rates_x_receives, name), Modifier.weight(1f)); Txt(Money.format(q.receiveMinor, cur), T.Num.copy(fontSize = 18.sp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Over(str(R.string.rates_same_transfer_elsewhere))
            Group {
                others.forEachIndexed { i, o ->
                    if (i > 0) GroupDivider()
                    ListRow(o.label, flat = true, subtitle = str(R.string.rates_fee_x_rate_x, Money.format(o.feeMinor, Currency.GBP), Money.rate(Rates.round(mid * (1 - o.margin)), decimals)),
                        verticalAlignment = Alignment.Top,
                        trailing = { TrailingValue(Money.format(receive(o.margin), cur), str(R.string.rates_costs_x, Money.format(cost(o.feeMinor, o.margin), Currency.GBP)), o.color) })
                }
            }
        }
        Panel(kind = PanelKind.Lime, gap = 4.dp) {
            Txt(str(R.string.rates_x_gets_more_with_arro, name), T.Label, color = C.Forest)
            Txt(str(R.string.rates_x_to_x, Money.format(gains.min(), cur, false), Money.format(gains.max(), cur, false)), T.Num.copy(fontSize = 18.sp))
        }
        Cap(str(R.string.rates_other_providers_prices_are_typical_published) +
            str(R.string.rates_mid_market_rate_x_at_x, Money.rate(mid, decimals), Text.time(state.rates.fetchedAt.takeIf { it > 0 } ?: b.now)), size = 11.5f)
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
