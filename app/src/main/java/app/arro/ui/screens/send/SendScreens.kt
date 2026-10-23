package app.arro.ui.screens.send

import app.arro.R
import app.arro.ui.str
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.ArroBackend
import app.arro.core.Catalog
import app.arro.core.Country
import app.arro.core.LockKind
import app.arro.core.Loyalty
import app.arro.core.Currency
import app.arro.core.MethodKind
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
import app.arro.ui.screens.rates.offerError
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.BackButton
import app.arro.ui.components.BarCaption
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.Chevron
import app.arro.ui.components.Chip
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FiveGrid
import app.arro.ui.components.Flag
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.Keypad
import app.arro.ui.components.LinkText
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.PersonChip
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.Radio
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SearchField
import app.arro.ui.components.SectionHeader
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.VSpace
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.launch

fun deliveryIcon(type: PayoutType): Int = when (type) {
    PayoutType.MOBILE_MONEY -> Icons.Smartphone
    PayoutType.BANK -> Icons.Bank
    PayoutType.CASH -> Icons.Cash
    PayoutType.EWALLET -> Icons.Wallet
    PayoutType.UPI -> Icons.Bolt
    PayoutType.ARRO -> Icons.ArrowUpRight
}

fun deliveryTile(type: PayoutType): TileStyle = when (type) {
    PayoutType.MOBILE_MONEY -> TileStyle.Lime
    PayoutType.CASH -> TileStyle.Warn
    PayoutType.EWALLET, PayoutType.UPI -> TileStyle.Info
    PayoutType.ARRO -> TileStyle.Dark
    PayoutType.BANK -> TileStyle.Tint
}

// ------------------------------------------------------------------ 4.01 Send hub

@Composable
fun SendHubScreen() {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    var query by rememberSaveable { mutableStateOf("") }
    val now = nowTicker(60_000)
    val q = query.trim().lowercase()
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Help, { nav.go(Routes.HELP) }, style = IconBtn.Soft, description = str(R.string.send_help)) }) },
        gap = 20.dp,
    ) {
        ScreenTitle(str(R.string.send_send_money))
        SearchField(query, { query = it }, str(R.string.send_name_phone_number_or_country))
        if (q.isNotEmpty()) {
            val people = a.recipients.filter { r -> r.name.lowercase().contains(q) || r.methods.any { it.account.filter(Char::isDigit).contains(q.filter(Char::isDigit).ifEmpty { "~" }) } }
            val places = Catalog.countries.filter { it.name.lowercase().contains(q) || it.currency.code.lowercase().contains(q) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                people.forEach { r ->
                    ListRow(r.name, subtitle = r.defaultMethod?.let { "${it.providerName} · ${it.masked}" }, onClick = { startSend(c, nav, banner, recipient = r) },
                        leading = { Avatar(r.initials, r.color, flag = Catalog.countryOrNull(r.country)?.currency?.flag) }, trailing = { Chevron() })
                }
                places.forEach { ct ->
                    ListRow(ct.name, subtitle = "${ct.currency.code} · ${ct.methodsSummary}", onClick = { startSend(c, nav, banner, country = ct.code) },
                        leading = { Flag(ct.currency.flag, 32.dp) }, trailing = { Chevron() })
                }
                if (people.isEmpty() && places.isEmpty()) Cap(str(R.string.send_no_people_or_countries_match_x, query))
            }
        } else {
            Column {
                SectionHeader(str(R.string.send_how_do_you_want_to_send))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListRow(str(R.string.send_send_abroad), subtitle = str(R.string.send_mobile_money_bank_deposit_or_cash), leading = { Tile(Icons.Globe, TileStyle.Dark) },
                        trailing = { Chevron() }, onClick = { startSend(c, nav, banner) })
                    ListRow(str(R.string.send_to_an_arro_user), subtitle = str(R.string.send_instant_and_free_by_username_or), leading = { Tile(Icons.Users, TileStyle.Lime) },
                        trailing = { Chevron() }, onClick = { nav.go(Routes.SEND_ARRO) })
                    ListRow(str(R.string.send_cash_pickup), subtitle = str(R.string.send_collected_at_a_partner_agent_with), leading = { Tile(Icons.Cash, TileStyle.Warn) },
                        trailing = { Chevron() }, onClick = {
                            startSend(c, nav, banner)
                            c.session.send.payoutType = PayoutType.CASH
                        })
                    ListRow(str(R.string.send_scan_a_qr_code), subtitle = str(R.string.send_pay_someone_s_arro_code), leading = { Tile(Icons.Scan, TileStyle.Info) },
                        trailing = { Chevron() }, onClick = { nav.go(Routes.SCAN_QR) })
                }
            }
            Column {
                SectionHeader(str(R.string.send_recent_recipients), str(R.string.send_see_all), { nav.go(Routes.RECIPIENTS) })
                val recents = app.arro.core.Insights.recentRecipients(a).take(4)
                FiveGrid(listOf<@Composable () -> Unit>({ PersonChip(str(R.string.send_add), "", 0, null, { c.session.recipient.reset(forSend = false); nav.go(Routes.RCP_ADD) }, add = true) }) +
                    recents.map<app.arro.core.Recipient, @Composable () -> Unit> { r -> { PersonChip(r.firstName, r.initials, r.color, Catalog.countryOrNull(r.country)?.currency?.flag ?: "gb", { startSend(c, nav, banner, recipient = r) }) } })
            }
            val rate = Rates.customer(state.rates, Currency.GBP, Currency.RWF)
            val ch = Rates.change24h(state.rates, Currency.RWF, now)
            Note(str(R.string.send_gbp_rwf_is_x_x_today, if (ch >= 0) str(R.string.send_up) else str(R.string.send_down), Money.percent(kotlin.math.abs(ch)), Money.rate(rate)), NoteKind.Lime, Icons.Trending,
                Modifier.padding(top = 8.dp))
        }
    }
}

// ------------------------------------------------------------------ 4.02 Destination (and 12.09 no results)

@Composable
fun SendCountryScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val d = c.session.send
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val q = query.trim().lowercase()
    fun choose(ct: Country) {
        val keepCash = d.payoutType == PayoutType.CASH && ct.option(PayoutType.CASH) != null
        d.reset(ct.code)
        if (keepCash) d.payoutType = PayoutType.CASH
        nav.go(Routes.SEND_AMOUNT)
    }
    val recentCodes = a.transfers.sortedByDescending { it.createdAt }.map { it.country }.filter { it != "GB" }.distinct().take(3)
    val match: (Country) -> Boolean = { ct -> q.isEmpty() || ct.name.lowercase().contains(q) || ct.currency.code.lowercase().contains(q) || ct.currency.displayName.lowercase().contains(q) }
    val recent = recentCodes.mapNotNull { Catalog.countryOrNull(it) }.filter(match)
    val others = Catalog.countries.filter { it.code !in recentCodes }.filter(match)
    val unsupported = Catalog.unsupported.firstOrNull { q.length >= 3 && it.lowercase().startsWith(q) }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.send_step_1_of_5)) }) },
    ) {
        ScreenTitle(str(R.string.send_where_are_you_sending))
        SearchField(query, { query = it }, str(R.string.send_country_or_currency), focusRequester = focus)
        if (recent.isEmpty() && others.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Illustration(Art.Globe, 120.dp)
                val name = unsupported ?: query.trim().replaceFirstChar { it.uppercase() }
                Txt(str(R.string.send_we_don_t_send_to_x, name), T.H2.copy(fontSize = 20.sp), align = TextAlign.Center)
                Cap(str(R.string.send_we_add_new_countries_regularly_tell), Modifier.width(290.dp), align = TextAlign.Center, size = 14f)
                val asked = name in state.notifyCountries
                ArroButton(if (asked) str(R.string.send_we_ll_let_you_know) else str(R.string.send_notify_me), {
                    b.notifyWhenAvailable(name)
                    banner.show(str(R.string.send_we_ll_let_you_know), str(R.string.send_you_ll_get_a_notification_when, name), BannerKind.Success)
                }, Modifier.padding(top = 8.dp), style = Btn.Line, size = BtnSize.M, icon = Icons.Bell, enabled = !asked)
            }
            Over(str(R.string.send_popular_destinations))
            Group {
                listOf("IN", "RW").map { Catalog.country(it) }.forEachIndexed { i, ct ->
                    if (i > 0) GroupDivider()
                    CountryRow(ct) { choose(ct) }
                }
            }
        } else {
            if (recent.isNotEmpty()) {
                Over(str(R.string.send_your_recent_destinations))
                Group { recent.forEachIndexed { i, ct -> if (i > 0) GroupDivider(); CountryRow(ct) { choose(ct) } } }
            }
            if (others.isNotEmpty()) {
                Over(if (recent.isEmpty()) str(R.string.send_destinations) else str(R.string.send_all_destinations))
                Group { others.forEachIndexed { i, ct -> if (i > 0) GroupDivider(); CountryRow(ct) { choose(ct) } } }
            }
        }
    }
}

@Composable
private fun CountryRow(ct: Country, onClick: () -> Unit) {
    ListRow(ct.name, subtitle = "${ct.currency.code} · ${ct.methodsSummary}", flat = true, onClick = onClick,
        leading = { Flag(ct.currency.flag, 32.dp) }, trailing = { Chevron() })
}

// ------------------------------------------------------------------ 4.03 Amount and quote (+ 4.04 fees sheet)

@Composable
fun SendAmountScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val d = c.session.send
    val country = Catalog.country(d.country)
    val recipient = a.recipient(d.recipientId)
    val arro = d.arroUser
    val to = if (arro) Currency.GBP else country.currency
    val amount = Money.parse(d.amountText, Currency.GBP) ?: 0L
    val fundingForQuote = d.fundingId ?: a.paymentMethods.firstOrNull { it.isDefault }?.id
    val now = nowTicker()
    // The rate is held on the server for this quote: a 30-minute hold, or a locked offer rate if one is running.
    val lock = if (arro) null else a.activeLock(to.code, now)
    val offer = if (arro || lock?.kind == LockKind.OFFER) null else a.liveOffer(now)?.takeIf { it.to == to.code }
    val quote = b.quote(to, amount, d.payoutType, d.country, fundingForQuote)
    var heldId by remember { mutableStateOf<String?>(null) }
    var feesOpen by remember { mutableStateOf(false) }
    var countryPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lockCall = rememberCall()
    LaunchedEffect(to, arro, lock?.id) {
        if (arro) return@LaunchedEffect
        if (lock == null) {
            val previous = heldId?.let { id -> b.account?.rateLocks?.firstOrNull { it.id == id } }
            b.holdRate(to)
            // A locked offer ending is announced app-wide; a quote hold running out is announced here, as before.
            if (previous != null && previous.to == to.code && previous.kind == LockKind.QUOTE && previous.expiredUnused(b.now)) {
                banner.show(str(R.string.send_your_rate_was_refreshed), str(R.string.send_here_s_a_new_30_minute), BannerKind.Info)
            }
        }
        heldId = b.account?.activeLock(to.code, b.now)?.id
    }
    // Rates saved from an earlier session may be old: fetch today's and re-price the hold once they arrive.
    LaunchedEffect(to, arro) {
        if (!arro && !b.ratesLive() && b.refreshRates()) b.refreshHold(to)
    }
    val limit = b.limitProblem(amount)
    val tooSmall = amount in 1..99
    val canGo = amount >= 100 && limit == null && (arro || lock != null)
    val earn = Loyalty.pointsToEarn(a, quote)
    fun press(k: String) {
        var s = d.amountText
        if (k == ".") { if (!s.contains('.')) s += "." }
        else {
            val parts = s.split('.')
            if (parts.size > 1 && parts[1].length >= 2) return
            if (parts.size == 1 && parts[0].length >= 6) return
            s = if (s == "0") k else s + k
        }
        d.amountText = s
    }
    val sendText = d.amountText.let { t ->
        val parts = t.split('.')
        val intPart = (parts[0].toLongOrNull() ?: 0L).let { java.text.NumberFormat.getIntegerInstance(java.util.Locale.UK).format(it) }
        if (parts.size > 1) intPart + "." + parts[1] else intPart
    }
    val title = if (arro) str(R.string.send_send_to_x, recipient?.firstName ?: str(R.string.send_arro_user)) else str(R.string.send_send_to_x, country.name)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = title, right = { BarCaption(str(R.string.send_n_2_of_5)) }) },
        bodyPadding = 20.dp,
        gap = 0.dp,
        scroll = false,
        footer = {
            ArroButton(str(R.string.send_continue), {
                d.quote = quote
                nav.go(if (d.recipientId != null && d.methodId != null) Routes.SEND_FUNDING else Routes.SEND_RECIPIENT)
            }, enabled = canGo)
        },
        overlay = {
            FeesSheet(feesOpen, { feesOpen = false }, quote, recipient?.firstName ?: str(R.string.send_they), lock) { feesOpen = false; nav.go(Routes.rateCompare(to.code)) }
            PickerSheet(countryPicker, str(R.string.send_where_are_you_sending_2), Catalog.countries.map { it.name }, country.name, { name ->
                val ct = Catalog.countries.first { it.name == name }
                val keep = d.amountText
                d.reset(ct.code); d.amountText = keep
            }, { countryPicker = false }, subtitles = Catalog.countries.associate { it.name to "${it.currency.code} · ${it.methodsSummary}" },
                leading = { name -> Flag(Catalog.countries.first { it.name == name }.currency.flag) })
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
            // amount card
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).border(1.5.dp, C.Line, RoundedCornerShape(22.dp))) {
                Column {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                        app.arro.ui.components.FieldLabel(str(R.string.send_you_send))
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Txt(sendText, T.Big36, maxLines = 1)
                                Box(Modifier.padding(start = 3.dp).size(2.dp, 30.dp).background(C.LimeDeep, RoundedCornerShape(1.dp)))
                            }
                            Chip("GBP", flag = "gb", icon = null, onClick = { banner.show(str(R.string.send_you_send_in_pounds), str(R.string.send_transfers_are_paid_in_gbp_from)) })
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.5.dp).background(C.Line))
                    Column(Modifier.fillMaxWidth().background(C.Tint).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                        app.arro.ui.components.FieldLabel(if (arro) str(R.string.send_x_receives, recipient?.firstName ?: str(R.string.send_they)) else str(R.string.send_they_receive))
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Txt(Money.number(quote.receiveMinor, to, forceDecimals = false), T.Big36, Modifier.weight(1f), maxLines = 1)
                            Chip(to.code, flag = to.flag, onClick = { if (!arro) countryPicker = true })
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.Center).size(36.dp).background(C.Primary, CircleShape).border(3.dp, C.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Ic(Icons.ArrowDown, size = 16.dp, tint = C.PrimaryAccent) }
            }
            // quote lines
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!arro) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ic(Icons.Lock, size = 16.dp, tint = if (lock?.kind == LockKind.OFFER) C.Ok else C.Slate)
                        Txt(when {
                            lock == null -> str(R.string.send_getting_today_s_rate)
                            lock.kind == LockKind.OFFER -> str(R.string.send_offer_rate_locked_x, Text.countdown(lock.expiresAt - now))
                            else -> str(R.string.send_rate_locked_x, Text.countdown(lock.expiresAt - now))
                        }, T.Body14, color = if (lock?.kind == LockKind.OFFER) C.Ok else C.Slate)
                    }
                    Txt(Money.rateLine(Currency.GBP, to, quote.rate), T.Body14.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
                }
                if (offer != null) {
                    // A running rate offer can be locked without leaving the transfer.
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Lime3)
                            .clickable(enabled = !lockCall.busy.value, role = androidx.compose.ui.semantics.Role.Button) {
                                scope.launchCall(lockCall, onError = { banner.offerError(it) }) {
                                    val l = b.lockOffer(offer.id)
                                    banner.show(str(R.string.send_rate_locked), str(R.string.send_x_until_x, Money.rateLine(Currency.GBP, to, l.rate), Text.time(l.expiresAt)), BannerKind.Success)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Ic(Icons.Bolt, size = 16.dp, tint = C.Forest)
                        Txt(str(R.string.send_offer_x_for_x_min_x, Money.rate(offer.rate, Money.rateDecimals(offer.rate)), offer.lockMinutes, Text.countdown(offer.expiresAt - now)),
                            T.Body14, Modifier.weight(1f), color = C.Forest, maxLines = 1)
                        Txt(if (lockCall.busy.value) str(R.string.send_locking) else str(R.string.send_lock), T.Link.copy(fontWeight = FontWeight.SemiBold), color = C.Ink)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt(str(R.string.send_arro_fee), T.Body14, color = C.Slate)
                    Txt(when {
                        quote.feeWaived -> str(R.string.send_free_on_your_first_transfer)
                        quote.discountMinor > 0 -> str(R.string.send_x_x_credit, Money.format(quote.feeMinor, Currency.GBP), Money.format(quote.discountMinor, Currency.GBP))
                        else -> Money.format(quote.feeMinor, Currency.GBP)
                    }, T.Body14.copy(fontFeatureSettings = "tnum"), color = if (quote.feeWaived) C.Ok else C.Ink)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt(str(R.string.send_total_to_pay), T.Body14, color = C.Slate)
                    Txt(Money.format(quote.totalMinor, Currency.GBP), T.Body14.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
                }
                if (limit != null) FieldHint(limit)
                else if (tooSmall) FieldHint(str(R.string.send_the_smallest_amount_you_can_send))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!arro) LinkText(str(R.string.send_see_the_full_price_breakdown), { feesOpen = true }, color = C.Ink, strong = true, underline = true)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    if (earn > 0 && amount >= 100) Txt(str(R.string.send_you_ll_earn_x_points, Money.points(earn)), T.Cap.copy(fontWeight = FontWeight.Medium), color = C.Ok)
                }
            }
            // delivery method
            if (!arro) {
                val option = country.option(d.payoutType) ?: country.options.first()
                ListRow(
                    option.type.label, Modifier.padding(top = 12.dp), subtitle = when (option.type) {
                        PayoutType.MOBILE_MONEY, PayoutType.EWALLET, PayoutType.UPI -> str(R.string.send_usually_arrives_in_minutes)
                        else -> option.speed
                    },
                    minHeight = 56.dp, titleStyle = T.RowTitle.copy(fontSize = 14.sp),
                    leading = { Tile(deliveryIcon(option.type), deliveryTile(option.type), size = 36.dp, iconSize = 16.dp) },
                    trailing = { Txt(str(R.string.send_change), T.Link.copy(fontWeight = FontWeight.SemiBold), color = C.Ink) },
                    onClick = { nav.go(Routes.SEND_DELIVERY) },
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Keypad({ press(it) }, { d.amountText = if (d.amountText.length > 1) d.amountText.dropLast(1) else "0" },
                Modifier.padding(top = 6.dp), decimal = true, keyHeight = 50.dp)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.FeesSheet(
    visible: Boolean, onDismiss: () -> Unit, q: app.arro.core.Quote, name: String, lock: app.arro.core.RateLock?, onCompare: () -> Unit,
) {
    Sheet(visible, onDismiss) {
        SheetHeader(str(R.string.send_price_breakdown), onDismiss, str(R.string.send_everything_you_pay_and_exactly_what))
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KV(str(R.string.send_you_send), Money.format(q.sendMinor, Currency.GBP))
            KV(str(R.string.send_arro_fee_x, q.payoutType.short), if (q.feeWaived) str(R.string.send_free) else "+ " + Money.format(q.feeMinor, Currency.GBP))
            if (q.discountMinor > 0) KV(str(R.string.send_points_credit), "− " + Money.format(q.discountMinor, Currency.GBP), valueColor = C.Ok)
            if (q.fundingFeeMinor > 0) KV(str(R.string.send_credit_card_fee), "+ " + Money.format(q.fundingFeeMinor, Currency.GBP))
            KV(str(R.string.send_total_you_pay), Money.format(q.totalMinor, Currency.GBP), strong = true)
        }
        Box(Modifier.padding(vertical = 16.dp).fillMaxWidth().height(1.dp).background(C.Line))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KV(str(R.string.send_mid_market_rate), Money.rateLine(q.from, q.to, q.mid))
            KV(str(R.string.send_your_rate), Money.rateLine(q.from, q.to, q.rate))
            KV(str(R.string.send_margin_in_our_rate), str(R.string.send_x_about_x, Money.percent(q.marginPercent, 2), Money.format(q.marginMinor, Currency.GBP)))
        }
        Panel(Modifier.padding(top = 16.dp), PanelKind.Lime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                app.arro.ui.components.FieldLabel(str(R.string.send_x_receives, name), color = C.Forest)
                Txt(Money.format(q.receiveMinor, q.to), T.Big22)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap(str(R.string.send_total_cost_of_this_transfer), color = C.Forest)
                Txt(Money.format(q.totalCostMinor, Currency.GBP), T.Num.copy(fontSize = 14.sp))
            }
        }
        Note(when {
            lock == null -> str(R.string.send_we_re_getting_today_s_rate)
            lock.kind == LockKind.OFFER -> str(R.string.send_your_locked_offer_rate_is_guaranteed, Text.time(lock.expiresAt))
            else -> str(R.string.send_your_rate_is_guaranteed_until_x, Text.time(lock.expiresAt))
        }, NoteKind.Info, Icons.Lock, Modifier.padding(top = 14.dp))
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArroButton(str(R.string.send_compare_prices), onCompare, Modifier.weight(1f), style = Btn.Line)
            ArroButton(str(R.string.send_got_it), onDismiss, Modifier.weight(1f))
        }
    }
}

// ------------------------------------------------------------------ 4.05 Delivery method

@Composable
fun SendDeliveryScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val state by appState()
    val a = state.account ?: return
    val d = c.session.send
    val country = Catalog.country(d.country)
    val amount = Money.parse(d.amountText, Currency.GBP) ?: 0L
    var selected by remember { mutableStateOf(country.option(d.payoutType)?.type ?: country.options.first().type) }
    val receive = b.quote(country.currency, amount, selected, d.country).receiveMinor
    val outage = state.demo.outageProviderId
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.send_sending_x, Money.format(amount, Currency.GBP))) }) },
        footer = {
            ArroButton(str(R.string.send_continue_with_x, selected.short), {
                val down = Catalog.providersFor(d.country, selected).let { ps -> ps.isNotEmpty() && ps.all { it.id == outage } }
                if (down) { nav.go(Routes.SEND_UNAVAILABLE); return@ArroButton }
                if (selected != d.payoutType) { d.recipientId = null; d.methodId = null }
                d.payoutType = selected
                val r = a.recipient(d.recipientId)
                if (r != null && r.methodFor(selected) != null) {
                    d.methodId = r.methodFor(selected)?.id
                    nav.back()
                } else nav.go(Routes.SEND_RECIPIENT)
            })
        },
    ) {
        ScreenTitle(str(R.string.send_how_should_it_arrive), str(R.string.send_same_rate_on_every_option_fees))
        country.options.forEach { o ->
            val on = o.type == selected
            val fee = if (b.quote(country.currency, amount, o.type, d.country).feeWaived) 0L else o.feeMinor
            val providers = Catalog.providersFor(d.country, o.type)
            val downNames = providers.filter { it.id == outage }.map { it.name }
            ListRow(
                o.type.label, selected = on, verticalAlignment = Alignment.Top, onClick = { selected = o.type },
                leading = { Tile(deliveryIcon(o.type), if (on) deliveryTile(o.type) else TileStyle.Tint) },
                trailing = { Radio(on, Modifier.padding(top = 2.dp)) },
                content = {
                    if (o.badge != null) Pill(o.badge, if (o.badge == str(R.string.send_fastest)) PillKind.Lime else PillKind.Neutral, modifier = Modifier.padding(top = 4.dp))
                    Txt(o.description, T.RowSub, Modifier.padding(top = 4.dp), color = C.Slate)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Ic(if (o.speed == str(R.string.send_minutes)) Icons.Bolt else Icons.Clock, size = 16.dp)
                            Txt(o.speed, T.Cap, color = C.Ink)
                        }
                        Txt(if (fee == 0L) str(R.string.send_no_fee) else str(R.string.send_fee_x, Money.format(fee, Currency.GBP)), T.Cap, color = C.Ink)
                        Txt(str(R.string.send_pay_x, Money.format(amount + fee, Currency.GBP)), T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink)
                    }
                    if (downNames.isNotEmpty()) Txt(str(R.string.send_x_is_briefly_offline, downNames.joinToString()), T.Small, Modifier.padding(top = 6.dp), color = C.Warn)
                },
            )
        }
        Panel(kind = PanelKind.Lime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                app.arro.ui.components.FieldLabel(str(R.string.send_they_receive_on_every_option), color = C.Forest)
                Txt(Money.format(receive, country.currency), T.Num.copy(fontSize = 16.sp))
            }
        }
    }
}

// ------------------------------------------------------------------ 4.06 Choose recipient

@Composable
fun SendRecipientScreen() {
    val nav = LocalNav.current
    val c = container()
    val state by appState()
    val a = state.account ?: return
    val d = c.session.send
    val country = Catalog.country(d.country)
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val inCountry = a.recipients.filter { it.country == d.country && (q.isEmpty() || it.name.lowercase().contains(q)) }
    val able = inCountry.filter { it.methodFor(d.payoutType) != null }
    val other = inCountry.filter { it.methodFor(d.payoutType) == null && it.methods.isNotEmpty() }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.send_step_3_of_5)) }) },
    ) {
        ScreenTitle(str(R.string.send_who_s_it_for), str(R.string.send_showing_people_who_can_receive_x, d.payoutType.short, country.name))
        SearchField(query, { query = it }, str(R.string.send_search_recipients))
        ListRow(str(R.string.send_add_a_new_recipient), subtitle = when (d.payoutType) {
            PayoutType.BANK -> str(R.string.send_name_bank_and_account_number)
            PayoutType.CASH -> str(R.string.send_name_as_it_appears_on_their)
            else -> str(R.string.send_name_and_x_number, d.payoutType.short)
        }, dashed = true,
            leading = { Box(Modifier.size(44.dp).background(C.Lime, CircleShape), contentAlignment = Alignment.Center) { Ic(Icons.Plus, tint = C.OnLime) } },
            trailing = { Chevron() },
            onClick = { c.session.recipient.reset(d.country, d.payoutType, forSend = true); nav.go(Routes.RCP_ADD) })
        if (able.isNotEmpty()) {
            Over("${country.name} · ${d.payoutType.short}")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                able.forEach { r ->
                    val m = r.methodFor(d.payoutType)!!
                    val on = r.id == d.recipientId
                    ListRow(r.name, subtitle = "${m.providerName} · ${m.masked}", selected = on,
                        leading = { Avatar(r.initials, r.color, flag = country.currency.flag) }, trailing = { Radio(on) },
                        onClick = { d.recipientId = r.id; d.methodId = m.id; nav.go(Routes.SEND_FUNDING) })
                }
            }
        } else if (inCountry.isEmpty()) {
            Cap(if (q.isEmpty()) str(R.string.send_you_haven_t_added_anyone_in, country.name) else str(R.string.send_no_recipients_match_x, query))
        }
        if (other.isNotEmpty()) {
            Over(str(R.string.send_needs_a_different_method))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                other.forEach { r ->
                    val m = r.defaultMethod!!
                    ListRow(r.name, subtitle = str(R.string.send_x_switch_to_x, m.providerName, m.type.short),
                        leading = { Avatar(r.initials, r.color, flag = country.currency.flag) },
                        enabled = true,
                        onClick = {
                            d.payoutType = m.type; d.recipientId = r.id; d.methodId = m.id
                            nav.go(Routes.SEND_FUNDING)
                        })
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 4.07 Pay with (+ 12.05 insufficient funds)

@Composable
fun SendFundingScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val d = c.session.send
    val base = d.quote ?: return
    val gbp = a.wallet(Currency.GBP)?.balanceMinor ?: 0L
    val methods = a.activeMethods
    var selected by remember {
        mutableStateOf(d.fundingId ?: methods.firstOrNull { it.isDefault }?.id ?: if (gbp >= base.totalMinor) ArroBackend.BALANCE else methods.firstOrNull()?.id ?: ArroBackend.BALANCE)
    }
    val q = base.copy(fundingFeeMinor = b.fundingFee(base.sendMinor, selected))
    var short by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val topUpSource = methods.firstOrNull { it.kind == MethodKind.BANK } ?: methods.firstOrNull { it.kind == MethodKind.CARD && !it.credit }
    val card = methods.firstOrNull { it.kind == MethodKind.CARD && !it.credit } ?: methods.firstOrNull()
    val shortfall = q.totalMinor - gbp
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.send_step_4_of_5)) }) },
        gap = 14.dp,
        footer = {
            ArroButton(str(R.string.send_review_transfer), {
                if (selected == ArroBackend.BALANCE && gbp < q.totalMinor) { short = true; return@ArroButton }
                d.fundingId = selected
                d.quote = q
                nav.go(Routes.SEND_REVIEW)
            })
        },
        overlay = {
            Sheet(short, { short = false }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Illustration(Art.Wallet, 72.dp)
                    Column(Modifier.weight(1f)) {
                        Txt(str(R.string.send_not_enough_in_your_balance), T.H2.copy(fontSize = 21.sp))
                        Cap(str(R.string.send_you_re_x_short_for_this, Money.format(shortfall, Currency.GBP)), Modifier.padding(top = 4.dp))
                    }
                }
                Panel(Modifier.padding(top = 16.dp), gap = 10.dp) {
                    KV(str(R.string.send_total_to_pay), Money.format(q.totalMinor, Currency.GBP))
                    KV(str(R.string.send_gbp_balance), Money.format(gbp, Currency.GBP))
                    KV(str(R.string.send_shortfall), Money.format(shortfall, Currency.GBP), strong = true, valueColor = C.Err, keyColor = C.Err)
                }
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (topUpSource != null) ArroButton(str(R.string.send_add_x_from_x, Money.format(shortfall, Currency.GBP), topUpSource.issuer), {
                        scope.launchCall(call) {
                            b.topUp(Currency.GBP, shortfall, topUpSource.id)
                            short = false
                            d.fundingId = ArroBackend.BALANCE
                            d.quote = q
                            banner.show(str(R.string.send_money_added), str(R.string.send_x_is_in_your_gbp_balance, Money.format(shortfall, Currency.GBP)), BannerKind.Success)
                            nav.go(Routes.SEND_REVIEW)
                        }
                    }, loading = call.busy.value)
                    if (card != null) ArroButton(str(R.string.send_pay_with_x, card.label), {
                        selected = card.id; short = false
                        d.fundingId = card.id; d.quote = base.copy(fundingFeeMinor = b.fundingFee(base.sendMinor, card.id))
                        nav.go(Routes.SEND_REVIEW)
                    }, style = Btn.Line)
                    ArroButton(str(R.string.send_change_the_amount), { short = false; nav.popBackStack(Routes.SEND_AMOUNT, false) }, style = Btn.Text)
                    FieldHint(call.error.value)
                }
            }
        },
    ) {
        Column {
            ScreenTitle(str(R.string.send_how_will_you_pay))
            Row(Modifier.padding(top = 8.dp)) {
                Txt(str(R.string.send_total_to_pay_2), T.Lede, color = C.Slate)
                Txt(Money.format(q.totalMinor, Currency.GBP), T.Lede.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val balOn = selected == ArroBackend.BALANCE
            ListRow(str(R.string.send_arro_balance), subtitle = str(R.string.send_x_available_instant, Money.format(gbp, Currency.GBP)), selected = balOn,
                leading = { Tile(Icons.Wallet, TileStyle.Lime) },
                trailing = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeeLabel(0); Radio(balOn) } },
                onClick = { selected = ArroBackend.BALANCE })
            methods.forEach { m ->
                val on = selected == m.id
                val fee = b.fundingFee(base.sendMinor, m.id)
                ListRow(
                    m.label,
                    subtitle = if (m.kind == MethodKind.BANK) str(R.string.send_x_x_approve_in_your_bank, m.issuer, m.last4) else str(R.string.send_x_instant, m.issuer),
                    selected = on,
                    leading = { Tile(if (m.kind == MethodKind.BANK) Icons.Bank else Icons.CreditCard, if (m.credit) TileStyle.Warn else if (m.kind == MethodKind.BANK) TileStyle.Tint else TileStyle.Info) },
                    trailing = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeeLabel(fee); Radio(on) } },
                    onClick = { selected = m.id },
                )
            }
        }
        if (methods.any { it.credit }) Cap(str(R.string.send_credit_cards_add_a_1_75), size = 12f)
        ListRow(str(R.string.send_add_a_card_or_bank), dashed = true, minHeight = 54.dp,
            leading = { Tile(Icons.Plus, TileStyle.Soft, size = 36.dp, iconSize = 16.dp) }, trailing = { Chevron() },
            onClick = { c.session.cardReturnToSend = true; nav.go(Routes.PAY_METHODS) })
    }
}

@Composable
private fun FeeLabel(fee: Long) {
    Txt(if (fee == 0L) str(R.string.send_no_fee) else "+ ${Money.format(fee, Currency.GBP)}", T.Cap.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = if (fee == 0L) C.Ok else C.Warn)
}
