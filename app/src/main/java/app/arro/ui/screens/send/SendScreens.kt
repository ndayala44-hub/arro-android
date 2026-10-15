package app.arro.ui.screens.send

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
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Help, { nav.go(Routes.HELP) }, style = IconBtn.Soft, description = "Help") }) },
        gap = 20.dp,
    ) {
        ScreenTitle("Send money")
        SearchField(query, { query = it }, "Name, phone number or country")
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
                if (people.isEmpty() && places.isEmpty()) Cap("No people or countries match \"$query\".")
            }
        } else {
            Column {
                SectionHeader("How do you want to send?")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListRow("Send abroad", subtitle = "Mobile money, bank deposit or cash pickup", leading = { Tile(Icons.Globe, TileStyle.Dark) },
                        trailing = { Chevron() }, onClick = { startSend(c, nav, banner) })
                    ListRow("To an Arro user", subtitle = "Instant and free, by @username or phone", leading = { Tile(Icons.Users, TileStyle.Lime) },
                        trailing = { Chevron() }, onClick = { nav.go(Routes.SEND_ARRO) })
                    ListRow("Cash pickup", subtitle = "Collected at a partner agent with ID", leading = { Tile(Icons.Cash, TileStyle.Warn) },
                        trailing = { Chevron() }, onClick = {
                            startSend(c, nav, banner)
                            c.session.send.payoutType = PayoutType.CASH
                        })
                    ListRow("Scan a QR code", subtitle = "Pay someone's Arro code", leading = { Tile(Icons.Scan, TileStyle.Info) },
                        trailing = { Chevron() }, onClick = { nav.go(Routes.SCAN_QR) })
                }
            }
            Column {
                SectionHeader("Recent recipients", "See all", { nav.go(Routes.RECIPIENTS) })
                val recents = app.arro.core.Insights.recentRecipients(a).take(4)
                FiveGrid(listOf<@Composable () -> Unit>({ PersonChip("Add", "", 0, null, { c.session.recipient.reset(forSend = false); nav.go(Routes.RCP_ADD) }, add = true) }) +
                    recents.map<app.arro.core.Recipient, @Composable () -> Unit> { r -> { PersonChip(r.firstName, r.initials, r.color, Catalog.countryOrNull(r.country)?.currency?.flag ?: "gb", { startSend(c, nav, banner, recipient = r) }) } })
            }
            val rate = Rates.customer(state.rates, Currency.GBP, Currency.RWF)
            val ch = Rates.change24h(state.rates, Currency.RWF, now)
            Note("GBP → RWF is ${if (ch >= 0) "up" else "down"} ${Money.percent(kotlin.math.abs(ch))} today. 1 GBP = ${Money.rate(rate)} RWF.", NoteKind.Lime, Icons.Trending,
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Step 1 of 5") }) },
    ) {
        ScreenTitle("Where are you\nsending?")
        SearchField(query, { query = it }, "Country or currency", focusRequester = focus)
        if (recent.isEmpty() && others.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Illustration(Art.Globe, 120.dp)
                val name = unsupported ?: query.trim().replaceFirstChar { it.uppercase() }
                Txt("We don't send to $name yet", T.H2.copy(fontSize = 20.sp), align = TextAlign.Center)
                Cap("We add new countries regularly. Tell us you're interested and we'll let you know.", Modifier.width(290.dp), align = TextAlign.Center, size = 14f)
                val asked = name in state.notifyCountries
                ArroButton(if (asked) "We'll let you know" else "Notify me", {
                    b.notifyWhenAvailable(name)
                    banner.show("We'll let you know", "You'll get a notification when Arro sends to $name.", BannerKind.Success)
                }, Modifier.padding(top = 8.dp), style = Btn.Line, size = BtnSize.M, icon = Icons.Bell, enabled = !asked)
            }
            Over("Popular destinations")
            Group {
                listOf("IN", "RW").map { Catalog.country(it) }.forEachIndexed { i, ct ->
                    if (i > 0) GroupDivider()
                    CountryRow(ct) { choose(ct) }
                }
            }
        } else {
            if (recent.isNotEmpty()) {
                Over("Your recent destinations")
                Group { recent.forEachIndexed { i, ct -> if (i > 0) GroupDivider(); CountryRow(ct) { choose(ct) } } }
            }
            if (others.isNotEmpty()) {
                Over(if (recent.isEmpty()) "Destinations" else "All destinations")
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
    val quote = b.quote(to, amount, d.payoutType, d.country, fundingForQuote)
    var lockUntil by remember { mutableLongStateOf(System.currentTimeMillis() + 30 * 60_000) }
    val now = nowTicker()
    var feesOpen by remember { mutableStateOf(false) }
    var countryPicker by remember { mutableStateOf(false) }
    LaunchedEffect(now >= lockUntil) {
        if (now >= lockUntil) {
            b.refreshRates()
            lockUntil = System.currentTimeMillis() + 30 * 60_000
            banner.show("Your rate was refreshed", "Here's a new 30-minute rate lock.", BannerKind.Info)
        }
    }
    val limit = b.limitProblem(amount)
    val tooSmall = amount in 1..99
    val canGo = amount >= 100 && limit == null
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
    val title = if (arro) "Send to ${recipient?.firstName ?: "Arro user"}" else "Send to ${country.name}"
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = title, right = { BarCaption("2 of 5") }) },
        bodyPadding = 20.dp,
        gap = 0.dp,
        scroll = false,
        footer = {
            ArroButton("Continue", {
                d.quote = quote.copy(lockedUntil = lockUntil)
                nav.go(if (d.recipientId != null && d.methodId != null) Routes.SEND_FUNDING else Routes.SEND_RECIPIENT)
            }, enabled = canGo)
        },
        overlay = {
            FeesSheet(feesOpen, { feesOpen = false }, quote, recipient?.firstName ?: "They", lockUntil) { feesOpen = false; nav.go(Routes.rateCompare(to.code)) }
            PickerSheet(countryPicker, "Where are you sending?", Catalog.countries.map { it.name }, country.name, { name ->
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
                        app.arro.ui.components.FieldLabel("You send")
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Txt(sendText, T.Big36, maxLines = 1)
                                Box(Modifier.padding(start = 3.dp).size(2.dp, 30.dp).background(C.LimeDeep, RoundedCornerShape(1.dp)))
                            }
                            Chip("GBP", flag = "gb", icon = null, onClick = { banner.show("You send in pounds", "Transfers are paid in GBP from your card, bank or balance.") })
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.5.dp).background(C.Line))
                    Column(Modifier.fillMaxWidth().background(C.Tint).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                        app.arro.ui.components.FieldLabel(if (arro) "${recipient?.firstName ?: "They"} receives" else "They receive")
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Txt(Money.number(quote.receiveMinor, to, forceDecimals = false), T.Big36, Modifier.weight(1f), maxLines = 1)
                            Chip(to.code, flag = to.flag, onClick = { if (!arro) countryPicker = true })
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.Center).size(36.dp).background(C.Ink, CircleShape).border(3.dp, C.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Ic(Icons.ArrowDown, size = 16.dp, tint = C.Lime) }
            }
            // quote lines
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!arro) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ic(Icons.Lock, size = 16.dp, tint = C.Slate)
                        Txt("Rate locked ${Text.countdown(lockUntil - now)}", T.Body14, color = C.Slate)
                    }
                    Txt(Money.rateLine(Currency.GBP, to, quote.rate), T.Body14.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt("Arro fee", T.Body14, color = C.Slate)
                    Txt(if (quote.feeWaived) "Free on your first transfer" else Money.format(quote.feeMinor, Currency.GBP), T.Body14.copy(fontFeatureSettings = "tnum"),
                        color = if (quote.feeWaived) C.Ok else C.Ink)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt("Total to pay", T.Body14, color = C.Slate)
                    Txt(Money.format(quote.totalMinor, Currency.GBP), T.Body14.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
                }
                if (limit != null) FieldHint(limit)
                else if (tooSmall) FieldHint("The smallest amount you can send is £1.00.")
                if (!arro) LinkText("See the full price breakdown", { feesOpen = true }, color = C.Ink, strong = true, underline = true)
            }
            // delivery method
            if (!arro) {
                val option = country.option(d.payoutType) ?: country.options.first()
                ListRow(
                    option.type.label, Modifier.padding(top = 12.dp), subtitle = when (option.type) {
                        PayoutType.MOBILE_MONEY, PayoutType.EWALLET, PayoutType.UPI -> "Usually arrives in minutes"
                        else -> option.speed
                    },
                    minHeight = 56.dp, titleStyle = T.RowTitle.copy(fontSize = 14.sp),
                    leading = { Tile(deliveryIcon(option.type), deliveryTile(option.type), size = 36.dp, iconSize = 16.dp) },
                    trailing = { Txt("Change", T.Link.copy(fontWeight = FontWeight.SemiBold), color = C.Ink) },
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
    visible: Boolean, onDismiss: () -> Unit, q: app.arro.core.Quote, name: String, lockUntil: Long, onCompare: () -> Unit,
) {
    Sheet(visible, onDismiss) {
        SheetHeader("Price breakdown", onDismiss, "Everything you pay, and exactly what arrives.")
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KV("You send", Money.format(q.sendMinor, Currency.GBP))
            KV("Arro fee · ${q.payoutType.short}", if (q.feeWaived) "Free" else "+ " + Money.format(q.feeMinor, Currency.GBP))
            if (q.fundingFeeMinor > 0) KV("Credit card fee", "+ " + Money.format(q.fundingFeeMinor, Currency.GBP))
            KV("Total you pay", Money.format(q.totalMinor, Currency.GBP), strong = true)
        }
        Box(Modifier.padding(vertical = 16.dp).fillMaxWidth().height(1.dp).background(C.Line))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KV("Mid-market rate", Money.rateLine(q.from, q.to, q.mid))
            KV("Your rate", Money.rateLine(q.from, q.to, q.rate))
            KV("Margin in our rate", "${Money.percent(q.marginPercent, 2)} · about ${Money.format(q.marginMinor, Currency.GBP)}")
        }
        Panel(Modifier.padding(top = 16.dp), PanelKind.Lime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                app.arro.ui.components.FieldLabel("$name receives", color = C.Forest)
                Txt(Money.format(q.receiveMinor, q.to), T.Big22)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap("Total cost of this transfer", color = C.Forest)
                Txt(Money.format(q.totalCostMinor, Currency.GBP), T.Num.copy(fontSize = 14.sp))
            }
        }
        Note("Your rate is guaranteed until ${Text.time(lockUntil)}. Nothing is deducted on arrival.", NoteKind.Info, Icons.Lock, Modifier.padding(top = 14.dp))
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArroButton("Compare prices", onCompare, Modifier.weight(1f), style = Btn.Line)
            ArroButton("Got it", onDismiss, Modifier.weight(1f))
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Sending ${Money.format(amount, Currency.GBP)}") }) },
        footer = {
            ArroButton("Continue with ${selected.short}", {
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
        ScreenTitle("How should it\narrive?", "Same rate on every option. Fees and speed differ.")
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
                    if (o.badge != null) Pill(o.badge, if (o.badge == "Fastest") PillKind.Lime else PillKind.Neutral, modifier = Modifier.padding(top = 4.dp))
                    Txt(o.description, T.RowSub, Modifier.padding(top = 4.dp), color = C.Slate)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Ic(if (o.speed == "Minutes") Icons.Bolt else Icons.Clock, size = 16.dp)
                            Txt(o.speed, T.Cap, color = C.Ink)
                        }
                        Txt(if (fee == 0L) "No fee" else "Fee ${Money.format(fee, Currency.GBP)}", T.Cap, color = C.Ink)
                        Txt("Pay ${Money.format(amount + fee, Currency.GBP)}", T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink)
                    }
                    if (downNames.isNotEmpty()) Txt("${downNames.joinToString()} is briefly offline", T.Small, Modifier.padding(top = 6.dp), color = C.Warn)
                },
            )
        }
        Panel(kind = PanelKind.Lime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                app.arro.ui.components.FieldLabel("They receive on every option", color = C.Forest)
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Step 3 of 5") }) },
    ) {
        ScreenTitle("Who's it for?", "Showing people who can receive ${d.payoutType.short} in ${country.name}.")
        SearchField(query, { query = it }, "Search recipients")
        ListRow("Add a new recipient", subtitle = when (d.payoutType) {
            PayoutType.BANK -> "Name, bank and account number"
            PayoutType.CASH -> "Name as it appears on their ID"
            else -> "Name and ${d.payoutType.short} number"
        }, dashed = true,
            leading = { Box(Modifier.size(44.dp).background(C.Lime, CircleShape), contentAlignment = Alignment.Center) { Ic(Icons.Plus) } },
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
            Cap(if (q.isEmpty()) "You haven't added anyone in ${country.name} yet." else "No recipients match \"$query\".")
        }
        if (other.isNotEmpty()) {
            Over("Needs a different method")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                other.forEach { r ->
                    val m = r.defaultMethod!!
                    ListRow(r.name, subtitle = "${m.providerName} · switch to ${m.type.short}",
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Step 4 of 5") }) },
        gap = 14.dp,
        footer = {
            ArroButton("Review transfer", {
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
                        Txt("Not enough in your balance", T.H2.copy(fontSize = 21.sp))
                        Cap("You're ${Money.format(shortfall, Currency.GBP)} short for this transfer.", Modifier.padding(top = 4.dp))
                    }
                }
                Panel(Modifier.padding(top = 16.dp), gap = 10.dp) {
                    KV("Total to pay", Money.format(q.totalMinor, Currency.GBP))
                    KV("GBP balance", Money.format(gbp, Currency.GBP))
                    KV("Shortfall", Money.format(shortfall, Currency.GBP), strong = true, valueColor = C.Err, keyColor = C.Err)
                }
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (topUpSource != null) ArroButton("Add ${Money.format(shortfall, Currency.GBP)} from ${topUpSource.issuer}", {
                        scope.launchCall(call) {
                            b.topUp(Currency.GBP, shortfall, topUpSource.id)
                            short = false
                            d.fundingId = ArroBackend.BALANCE
                            d.quote = q
                            banner.show("Money added", "${Money.format(shortfall, Currency.GBP)} is in your GBP balance.", BannerKind.Success)
                            nav.go(Routes.SEND_REVIEW)
                        }
                    }, loading = call.busy.value)
                    if (card != null) ArroButton("Pay with ${card.label}", {
                        selected = card.id; short = false
                        d.fundingId = card.id; d.quote = base.copy(fundingFeeMinor = b.fundingFee(base.sendMinor, card.id))
                        nav.go(Routes.SEND_REVIEW)
                    }, style = Btn.Line)
                    ArroButton("Change the amount", { short = false; nav.popBackStack(Routes.SEND_AMOUNT, false) }, style = Btn.Text)
                    FieldHint(call.error.value)
                }
            }
        },
    ) {
        Column {
            ScreenTitle("How will you pay?")
            Row(Modifier.padding(top = 8.dp)) {
                Txt("Total to pay ", T.Lede, color = C.Slate)
                Txt(Money.format(q.totalMinor, Currency.GBP), T.Lede.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val balOn = selected == ArroBackend.BALANCE
            ListRow("Arro balance", subtitle = "${Money.format(gbp, Currency.GBP)} available · instant", selected = balOn,
                leading = { Tile(Icons.Wallet, TileStyle.Lime) },
                trailing = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeeLabel(0); Radio(balOn) } },
                onClick = { selected = ArroBackend.BALANCE })
            methods.forEach { m ->
                val on = selected == m.id
                val fee = b.fundingFee(base.sendMinor, m.id)
                ListRow(
                    m.label,
                    subtitle = if (m.kind == MethodKind.BANK) "${m.issuer} •• ${m.last4} · approve in your bank app" else "${m.issuer} · instant",
                    selected = on,
                    leading = { Tile(if (m.kind == MethodKind.BANK) Icons.Bank else Icons.CreditCard, if (m.credit) TileStyle.Warn else if (m.kind == MethodKind.BANK) TileStyle.Tint else TileStyle.Info) },
                    trailing = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeeLabel(fee); Radio(on) } },
                    onClick = { selected = m.id },
                )
            }
        }
        if (methods.any { it.credit }) Cap("Credit cards add a 1.75% fee on the amount you send, and your card issuer may treat it as a cash advance.", size = 12f)
        ListRow("Add a card or bank", dashed = true, minHeight = 54.dp,
            leading = { Tile(Icons.Plus, TileStyle.Soft, size = 36.dp, iconSize = 16.dp) }, trailing = { Chevron() },
            onClick = { c.session.cardReturnToSend = true; nav.go(Routes.PAY_METHODS) })
    }
}

@Composable
private fun FeeLabel(fee: Long) {
    Txt(if (fee == 0L) "No fee" else "+ ${Money.format(fee, Currency.GBP)}", T.Cap.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = if (fee == 0L) C.Ok else C.Warn)
}
