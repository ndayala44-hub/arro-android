package app.arro.ui.screens.wallet

import app.arro.R
import app.arro.ui.str
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.ArroBackend
import app.arro.core.Catalog
import app.arro.core.Currency
import app.arro.core.FeedItem
import app.arro.core.LedgerType
import app.arro.core.Money
import app.arro.core.PayoutType
import app.arro.core.Text
import app.arro.core.Transfer
import app.arro.core.TransferStatus
import app.arro.platform.Documents
import app.arro.platform.Share
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
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.BrandTile
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.Chip
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.HDivider
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.KV
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SearchField
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Tab
import app.arro.ui.components.TabScreen
import app.arro.ui.components.Toggle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.Wordmark
import app.arro.ui.screens.home.FeedRow
import app.arro.ui.screens.send.startSend
import app.arro.ui.screens.system.EmptyState
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ------------------------------------------------------------------ 8.05 Activity

private enum class Kind(val label: String) { All(str(R.string.activity_all)), Transfers(str(R.string.activity_transfers)), TopUps(str(R.string.activity_top_ups)), Conversions(str(R.string.activity_conversions)), Refunds(str(R.string.activity_refunds)) }
private enum class StatusFilter(val label: String) { Any(str(R.string.activity_any_status)), Progress(str(R.string.activity_in_progress)), Action(str(R.string.activity_needs_action)), Delivered(str(R.string.activity_delivered)), Returned(str(R.string.activity_refunded_or_cancelled)) }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val now = nowTicker(30_000)
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(Kind.All) }
    var status by rememberSaveable { mutableStateOf(StatusFilter.Any) }
    var curFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var filters by remember { mutableStateOf(false) }
    val feed = b.feed(a)
    val q = query.trim().lowercase()
    val list = feed.filter { item ->
        val kindOk = when (kind) {
            Kind.All -> true
            Kind.Transfers -> item is FeedItem.Sent
            Kind.TopUps -> item is FeedItem.Entry && (item.entry.type == LedgerType.TOP_UP || item.entry.type == LedgerType.REWARD)
            Kind.Conversions -> item is FeedItem.Entry && item.entry.type == LedgerType.CONVERSION
            Kind.Refunds -> item is FeedItem.Sent && item.transfer.status in setOf(TransferStatus.REFUNDED, TransferStatus.CANCELLED, TransferStatus.FAILED)
        }
        val statusOk = when (status) {
            StatusFilter.Any -> true
            StatusFilter.Progress -> item is FeedItem.Sent && item.transfer.status in setOf(TransferStatus.PROCESSING, TransferStatus.IN_PROGRESS)
            StatusFilter.Action -> item is FeedItem.Sent && item.transfer.status == TransferStatus.ON_HOLD
            StatusFilter.Delivered -> item is FeedItem.Sent && item.transfer.status == TransferStatus.DELIVERED
            StatusFilter.Returned -> item is FeedItem.Sent && item.transfer.status in setOf(TransferStatus.REFUNDED, TransferStatus.CANCELLED, TransferStatus.FAILED)
        }
        val curOk = curFilter == null || when (item) {
            is FeedItem.Sent -> item.transfer.from == curFilter || item.transfer.to == curFilter
            is FeedItem.Entry -> item.entry.currency == curFilter || item.entry.counterCurrency == curFilter
        }
        val text = when (item) {
            is FeedItem.Sent -> item.transfer.let { t -> "${t.recipientName} ${t.reference} ${t.providerName} ${Money.format(t.receiveMinor, t.toCur)} ${Money.format(t.totalMinor, t.fromCur)} ${t.to}" }
            is FeedItem.Entry -> item.entry.let { e -> "${e.title} ${e.subtitle} ${Money.format(e.amountMinor, Currency.of(e.currency))} ${e.currency}" }
        }.lowercase()
        kindOk && statusOk && curOk && (q.isEmpty() || text.contains(q))
    }
    val activeFilters = (if (status != StatusFilter.Any) 1 else 0) + (if (curFilter != null) 1 else 0)
    TabScreen(
        Tab.Activity,
        top = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle(str(R.string.activity_activity), modifier = Modifier.weight(1f))
                if (feed.isNotEmpty()) RoundIcon(Icons.Statement, { nav.go(Routes.STATEMENTS) }, style = IconBtn.Soft, description = str(R.string.activity_statements))
            }
        },
        overlay = {
            Sheet(filters, { filters = false }) {
                SheetHeader(str(R.string.activity_filter_activity), { filters = false })
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(str(R.string.activity_status))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusFilter.entries.forEach { s -> Chip(s.label, status == s, { status = s }) }
                    }
                    FieldLabel(str(R.string.activity_currency), Modifier.padding(top = 8.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(str(R.string.activity_any), curFilter == null, { curFilter = null })
                        (a.wallets.map { it.currency } + a.transfers.map { it.to }).distinct().forEach { code ->
                            Chip(code, curFilter == code, { curFilter = code }, flag = Currency.of(code).flag)
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArroButton(str(R.string.activity_clear), { status = StatusFilter.Any; curFilter = null }, Modifier.weight(1f), style = Btn.Line)
                        ArroButton(str(R.string.activity_show_x, list.size), { filters = false }, Modifier.weight(1f))
                    }
                }
            }
        },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (feed.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SearchField(query, { query = it }, str(R.string.activity_search_activity)) }
                Box {
                    RoundIcon(Icons.Filters, { filters = true }, style = IconBtn.Line, size = 50.dp, description = str(R.string.activity_filters), badge = activeFilters > 0)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (feed.isEmpty()) Kind.entries.take(4) else Kind.entries).forEach { k -> Chip(k.label, kind == k, { kind = k }) }
            }
            if (feed.isEmpty()) {
                EmptyState(Art.Plane, str(R.string.activity_nothing_here_yet), str(R.string.activity_every_transfer_top_up_and_refund), Modifier.padding(top = 30.dp)) {
                    ArroButton(str(R.string.activity_send_your_first_transfer), { startSend(c, nav, banner) })
                }
                return@Column
            }
            if (list.isEmpty()) {
                Note(if (q.isNotEmpty()) str(R.string.activity_nothing_matches_x, query) else str(R.string.activity_nothing_matches_these_filters), NoteKind.Info, Icons.Search,
                    action = str(R.string.activity_clear_filters), onAction = { query = ""; kind = Kind.All; status = StatusFilter.Any; curFilter = null })
            }
            list.groupBy { sectionOf(it.at, now) }.forEach { (label, items) ->
                Over(label, Modifier.padding(top = 6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { FeedRow(it, now, showFromAmount = true) }
                }
            }
        }
    }
}

/** "Today", "Yesterday", "October", or "September 2025" for older years. */
private fun sectionOf(at: Long, now: Long): String = when {
    Text.isSameDay(at, now) -> str(R.string.activity_today)
    Text.isYesterday(at, now) -> str(R.string.activity_yesterday)
    Text.localDate(at).year == Text.localDate(now).year -> Text.monthName(at)
    else -> Text.monthYear(at)
}

// ------------------------------------------------------------------ 8.06 Transfer details

@Composable
fun TransferDetailScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(id) ?: run { MissingTransfer(); return }
    val (label, color) = app.arro.ui.screens.home.statusLabel(t)
    val recipient = a.recipient(t.recipientId)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Share, { nav.go(Routes.receipt(t.id)) }, description = str(R.string.activity_share_receipt)) }) },
        gap = 14.dp,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(Text.initials(t.recipientName), t.recipientColor, 64.dp, flag = t.toCur.flag)
            Txt(t.recipientName, T.H3, Modifier.padding(top = 4.dp))
            Txt(Money.format(t.receiveMinor, t.toCur), T.AmountMd, Modifier.padding(top = 4.dp))
            Txt(buildAnnotatedString {
                append(Text.dateTime(t.createdAt) + " · ")
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) {
                    append(if (t.status == TransferStatus.DELIVERED && t.payoutType != PayoutType.CASH) str(R.string.activity_delivered_in_x, Text.duration(t.deliveredAt - t.createdAt)) else label)
                }
            }, T.Cap, color = C.Slate, align = TextAlign.Center)
        }
        if (t.isOpen || t.refundPending) Note(
            if (t.status == TransferStatus.ON_HOLD) str(R.string.activity_this_transfer_needs_a_quick_answer) else if (t.refundPending) str(R.string.activity_your_refund_is_on_its_way) else str(R.string.activity_this_transfer_is_on_its_way),
            if (t.status == TransferStatus.ON_HOLD) NoteKind.Warn else NoteKind.Info, Icons.Clock, action = str(R.string.activity_track), onAction = { nav.go(Routes.track(t.id)) })
        Panel(gap = 10.dp) {
            KV(str(R.string.activity_you_sent), Money.format(t.sendMinor, t.fromCur))
            KV(str(R.string.activity_arro_fee), if (t.feeMinor == 0L) str(R.string.activity_free_first_transfer) else Money.format(t.feeMinor, t.fromCur), valueColor = if (t.feeMinor == 0L) C.Ok else C.Ink)
            if (t.pointsDiscountMinor > 0) KV(str(R.string.activity_points_credit), "− " + Money.format(t.pointsDiscountMinor, t.fromCur), valueColor = C.Ok)
            if (t.fundingFeeMinor > 0) KV(str(R.string.activity_card_fee), Money.format(t.fundingFeeMinor, t.fromCur))
            KV(str(R.string.activity_exchange_rate), Money.rateLine(t.fromCur, t.toCur, t.rate))
            if (a.rateLocks.any { it.id == t.rateLockId && it.kind == app.arro.core.LockKind.OFFER }) KV(str(R.string.activity_rate), str(R.string.activity_locked_offer_rate), valueColor = C.Ok)
            KV(str(R.string.activity_total_paid), Money.format(t.totalMinor, t.fromCur), strong = true)
            val earned = a.points.firstOrNull { it.ref == app.arro.core.Loyalty.earnedRef(t.id) }?.points
            val toCome = if (t.isOpen) app.arro.core.Loyalty.preview(t.feeMinor, t.pointsDiscountMinor) else 0
            if (earned != null) KV(str(R.string.activity_arro_rewards), str(R.string.activity_x_points, Money.points(earned)), valueColor = C.Ok)
            else if (toCome > 0) KV(str(R.string.activity_arro_rewards), str(R.string.activity_x_points_on_delivery, Money.points(toCome)))
        }
        Panel(gap = 10.dp) {
            KV(str(R.string.activity_to), t.recipientName, valueColor = C.Ok)
            KV(t.providerName, if (t.payoutType == PayoutType.MOBILE_MONEY || t.payoutType == PayoutType.EWALLET) Text.formatPhone(t.payoutAccount) else t.payoutAccount)
            KV(str(R.string.activity_paid_from), if (t.fundingId == ArroBackend.BALANCE) str(R.string.activity_arro_gbp_balance) else t.fundingLabel)
            if (t.reason.isNotBlank()) KV(str(R.string.activity_reason), t.reason)
            KV(str(R.string.activity_reference), t.reference, trailing = {
                RoundIcon(Icons.Copy, { Share.copy(context, str(R.string.activity_reference), t.reference); banner.show(str(R.string.activity_reference_copied), kind = BannerKind.Success) },
                    style = IconBtn.Plain, size = 28.dp, iconSize = 14.dp, description = str(R.string.activity_copy_reference))
            })
            if (t.providerRef.isNotBlank()) KV(str(R.string.activity_provider_id), t.providerRef)
            if (t.refundReference.isNotBlank()) KV(str(R.string.activity_refund_reference), t.refundReference)
        }
        if (t.status == TransferStatus.DELIVERED && recipient != null) Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Txt(str(R.string.activity_repeat_every_month), T.Body14.copy(fontWeight = FontWeight.Medium))
                Cap(str(R.string.activity_we_ll_remind_you_on_the, Text.localDate(t.createdAt).dayOfMonth, app.arro.ui.screens.send.ordinal(Text.localDate(t.createdAt).dayOfMonth)), size = 12.5f)
            }
            Toggle(t.repeatMonthly, { on -> b.setRepeatMonthly(t.id, on); banner.show(if (on) str(R.string.activity_monthly_reminder_set) else str(R.string.activity_monthly_reminder_off), kind = BannerKind.Info) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArroButton(str(R.string.activity_send_again), { sendAgain(c, nav, banner, t) }, Modifier.weight(1f), size = BtnSize.M, fill = true)
            ArroButton(str(R.string.activity_receipt), { nav.go(Routes.receipt(t.id)) }, Modifier.weight(1f), style = Btn.Line, size = BtnSize.M, fill = true)
        }
        ArroButton(str(R.string.activity_report_a_problem_with_this_transfer), { nav.go(Routes.dispute(t.id)) }, style = Btn.Text, size = BtnSize.S, icon = Icons.Help)
    }
}

/** Opens the send flow with the same recipient, method and amount as [t]. */
fun sendAgain(c: app.arro.AppContainer, nav: androidx.navigation.NavHostController, banner: app.arro.ui.BannerState, t: Transfer) {
    val a = c.backend.state.value.account ?: return
    val r = a.recipient(t.recipientId)
    if (r == null) { startSend(c, nav, banner, country = t.country.takeIf { Catalog.countryOrNull(it) != null }); return }
    startSend(c, nav, banner, recipient = r)
    val d = c.session.send
    if (d.recipientId == r.id) {
        r.methods.firstOrNull { it.account == t.payoutAccount && it.type == t.payoutType }?.let { m -> d.methodId = m.id; d.payoutType = m.type }
        d.amountText = Money.number(t.sendMinor, t.fromCur, false).replace(",", "")
        d.reason = t.reason.ifBlank { d.reason }
    }
}

@Composable
private fun MissingTransfer() {
    val nav = LocalNav.current
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }) {
        EmptyState(Art.Doc, str(R.string.activity_transfer_not_found), str(R.string.activity_it_may_have_been_removed_when)) {
            ArroButton(str(R.string.activity_go_to_activity), { nav.go(Routes.ACTIVITY) })
        }
    }
}

// ------------------------------------------------------------------ 8.07 Receipt

@Composable
fun ReceiptScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(id) ?: run { MissingTransfer(); return }
    val scope = rememberCoroutineScope()
    val save = rememberCall()
    val share = rememberCall()
    val (pillText, pillKind) = when (t.status) {
        TransferStatus.DELIVERED -> (if (t.payoutType == PayoutType.CASH) str(R.string.activity_ready_to_collect) else str(R.string.activity_delivered)) to PillKind.Ok
        TransferStatus.REFUNDED -> str(R.string.activity_refunded) to PillKind.Neutral
        TransferStatus.CANCELLED -> str(R.string.activity_cancelled) to PillKind.Neutral
        TransferStatus.FAILED -> str(R.string.activity_not_delivered) to PillKind.Err
        TransferStatus.ON_HOLD -> str(R.string.activity_on_hold) to PillKind.Warn
        else -> str(R.string.activity_in_progress) to PillKind.Info
    }
    val rows = buildList {
        add(str(R.string.activity_sender) to str(R.string.activity_x_x_uk, a.displayName, a.profile.address?.city ?: str(R.string.activity_london)))
        add(str(R.string.activity_recipient) to "${t.recipientName}, ${Catalog.countryOrNull(t.country)?.city ?: ""}, ${t.country}")
        add(str(R.string.activity_delivered_to) to "${t.providerName} ${if (t.payoutType == PayoutType.MOBILE_MONEY || t.payoutType == PayoutType.EWALLET) Text.formatPhone(t.payoutAccount) else t.payoutAccount}")
        add(str(R.string.activity_amount_sent) to Money.format(t.sendMinor, t.fromCur))
        add(str(R.string.activity_arro_fee) to Money.format(t.feeMinor, t.fromCur))
        if (t.pointsDiscountMinor > 0) add(str(R.string.activity_points_credit) to "− " + Money.format(t.pointsDiscountMinor, t.fromCur))
        if (t.fundingFeeMinor > 0) add(str(R.string.activity_card_fee) to Money.format(t.fundingFeeMinor, t.fromCur))
        add(str(R.string.activity_total_paid) to Money.format(t.totalMinor, t.fromCur))
        add(str(R.string.activity_exchange_rate) to Money.rateLine(t.fromCur, t.toCur, t.rate))
        add(str(R.string.activity_paid_with) to t.fundingLabel)
        add(str(R.string.activity_created) to "${Text.dateTime(t.createdAt)} ${Text.zoneName(t.createdAt)}")
        if (t.status == TransferStatus.DELIVERED) add(str(R.string.activity_delivered) to "${Text.dateTime(t.deliveredAt)} ${Text.zoneName(t.deliveredAt)}")
        add(str(R.string.activity_reference) to t.reference)
    }
    ArroScreen(
        background = if (C.dark) Color(0xFF050C0A) else Color(0xFFE9EFEB),
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.activity_receipt_pdf)) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton(str(R.string.activity_save_pdf), {
                    scope.launchCall(save) {
                        val f = withContext(Dispatchers.IO) { Documents.transferReceipt(context, a, t) }
                        banner.show(str(R.string.activity_receipt_saved), Share.saveToDownloads(context, f, "application/pdf"), BannerKind.Success)
                    }
                }, Modifier.weight(1f).background(C.White, RoundedCornerShape(999.dp)), style = Btn.Line, icon = Icons.Download, loading = save.busy.value, fill = true)
                ArroButton(str(R.string.activity_share), {
                    scope.launchCall(share) {
                        val f = withContext(Dispatchers.IO) { Documents.transferReceipt(context, a, t) }
                        Share.file(context, f, "application/pdf", str(R.string.activity_arro_receipt_x, t.reference))
                    }
                }, Modifier.weight(1f), icon = Icons.Share, loading = share.busy.value, fill = true)
            }
        },
    ) {
        Column(
            Modifier.padding(top = 4.dp).fillMaxWidth().shadow(18.dp, RoundedCornerShape(14.dp), ambientColor = C.Ink.copy(alpha = 0.12f), spotColor = C.Ink.copy(alpha = 0.12f))
                .background(C.White, RoundedCornerShape(14.dp)).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandTile(22.dp)
                Wordmark(18f, modifier = Modifier.padding(start = 6.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Pill(pillText, pillKind, dot = true)
            }
            Over(str(R.string.activity_transfer_receipt), Modifier.padding(top = 16.dp))
            Txt(Money.format(t.receiveMinor, t.toCur), T.Big22.copy(fontSize = 26.sp), Modifier.padding(top = 4.dp))
            Cap(if (t.status == TransferStatus.DELIVERED) str(R.string.activity_received_by_x, t.recipientName) else str(R.string.activity_for_x, t.recipientName), size = 12f)
            DashedLine(Modifier.padding(vertical = 14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                rows.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Txt(k, T.Small, color = C.Slate)
                        Txt(v, T.Small.copy(fontWeight = if (k == str(R.string.activity_total_paid)) FontWeight.Bold else FontWeight.Medium), Modifier.weight(1f), align = TextAlign.End)
                    }
                }
            }
            DashedLine(Modifier.padding(top = 14.dp, bottom = 10.dp))
            Txt(str(R.string.activity_arro_ltd_is_authorised_by_regulator),
                T.Small.copy(fontSize = 10.5.sp), color = C.Slate)
        }
    }
}

@Composable
fun DashedLine(modifier: Modifier = Modifier, color: Color = C.Mist) {
    Canvas(modifier.fillMaxWidth().height(1.5.dp)) {
        drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    }
}
