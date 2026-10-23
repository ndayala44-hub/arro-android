package app.arro.ui.screens.recipients

import app.arro.R
import app.arro.ui.str
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.ArroException
import app.arro.core.Catalog
import app.arro.core.Insights
import app.arro.core.Money
import app.arro.core.PayoutMethod
import app.arro.core.PayoutType
import app.arro.core.Recipient
import app.arro.core.Text
import app.arro.core.TransferStatus
import app.arro.core.Validation
import app.arro.platform.rememberContactPicker
import app.arro.ui.BannerKind
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.appState
import app.arro.ui.back
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.tab
import app.arro.ui.launchCall
import app.arro.ui.nowTicker
import app.arro.ui.rememberCall
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.CheckRow
import app.arro.ui.components.Chevron
import app.arro.ui.components.Chip
import app.arro.ui.components.ConfirmDialog
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.LinkText
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SearchField
import app.arro.ui.components.SectionHeader
import app.arro.ui.components.Segmented
import app.arro.ui.components.SelectField
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Tab
import app.arro.ui.components.TabScreen
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.Toggle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.selectionRing
import app.arro.ui.screens.send.deliveryIcon
import app.arro.ui.screens.send.startSend
import app.arro.ui.screens.home.statusLabel
import app.arro.ui.screens.system.EmptyState
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf

private fun flagOf(country: String) = Catalog.countryOrNull(country)?.currency?.flag ?: "gb"

// ------------------------------------------------------------------ 6.01 Recipients (+ 12.03 empty)

@Composable
fun RecipientsScreen() {
    val nav = LocalNav.current
    val c = container()
    val state by appState()
    val a = state.account ?: return
    var query by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf<String?>(null) }
    val contacts = rememberContactPicker { name, phone ->
        val ct = Catalog.countries.firstOrNull { phone.replace(" ", "").startsWith(it.dialCode) }?.code ?: "RW"
        c.session.recipient.reset(ct, Catalog.country(ct).options.first().type, forSend = false)
        c.session.recipient.name = Text.titleCase(name)
        c.session.recipient.number = Validation.nationalDigits(phone, Catalog.country(ct))
        nav.go(Routes.RCP_ADD)
    }
    val q = query.trim().lowercase()
    val list = a.recipients.filter { r ->
        (country == null || r.country == country) &&
            (q.isEmpty() || r.name.lowercase().contains(q) || r.methods.any { m -> m.account.contains(q) || m.providerName.lowercase().contains(q) })
    }
    TabScreen(Tab.Recipients, top = {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle(str(R.string.recipients_recipients), modifier = Modifier.weight(1f))
            RoundIcon(Icons.Plus, { c.session.recipient.reset(forSend = false); nav.go(Routes.RCP_ADD) }, description = str(R.string.recipients_add_recipient))
        }
    }) {
        if (a.recipients.isEmpty()) {
            EmptyState(Art.Globe, str(R.string.recipients_add_the_people_you_send_to), str(R.string.recipients_save_them_once_with_a_checked), Modifier.padding(top = 40.dp)) {
                ArroButton(str(R.string.recipients_add_a_recipient), { c.session.recipient.reset(forSend = false); nav.go(Routes.RCP_ADD) }, icon = Icons.Plus)
                ArroButton(str(R.string.recipients_import_from_contacts), { contacts() }, style = Btn.Text)
            }
            return@TabScreen
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SearchField(query, { query = it }, str(R.string.recipients_search_by_name_number_or_bank))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(str(R.string.recipients_all_x, a.recipients.size), country == null, { country = null })
                a.recipients.map { it.country }.distinct().forEach { code ->
                    Chip(Catalog.countryOrNull(code)?.name ?: str(R.string.recipients_arro_users), country == code, { country = if (country == code) null else code }, flag = flagOf(code))
                }
            }
            if (list.isEmpty()) Cap(str(R.string.recipients_no_recipients_match_x, query))
            list.groupBy { it.country }.forEach { (code, people) ->
                Over(Catalog.countryOrNull(code)?.name ?: str(R.string.recipients_arro_users))
                Group {
                    people.sortedWith(compareByDescending<Recipient> { it.favourite }.thenBy { it.name }).forEachIndexed { i, r ->
                        if (i > 0) GroupDivider()
                        val m = r.defaultMethod
                        ListRow(r.name, flat = true, subtitle = m?.let { "${it.providerName} · ${it.masked}" } ?: str(R.string.recipients_no_payout_method_yet),
                            leading = { Avatar(r.initials, r.color, flag = flagOf(r.country)) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (r.favourite) Ic(Icons.StarFilled, size = 14.dp, tint = C.WarnFill, contentDescription = str(R.string.recipients_favourite))
                                    Chevron()
                                }
                            },
                            onClick = { nav.go(Routes.recipient(r.id)) })
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 6.02 Recipient profile

@Composable
fun RecipientScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val r = a.recipient(id)
    if (r == null) { LaunchedEffect(Unit) { nav.back() }; return }
    val now = nowTicker(30_000)
    var methodSheet by remember { mutableStateOf<PayoutMethod?>(null) }
    val country = Catalog.countryOrNull(r.country)
    val transfers = a.transfers.filter { it.recipientId == r.id }.sortedByDescending { it.createdAt }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Edit, { nav.go(Routes.rcpEdit(r.id)) }, style = IconBtn.Soft, description = str(R.string.recipients_edit_recipient)) }) },
        overlay = {
            val m = methodSheet
            Sheet(m != null, { methodSheet = null }) {
                if (m != null) {
                    SheetHeader(m.providerName, { methodSheet = null }, m.full)
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (r.defaultMethodId != m.id) ArroButton(str(R.string.recipients_make_this_the_default), { b.updateRecipient(r.id) { it.copy(defaultMethodId = m.id) }; methodSheet = null })
                        ArroButton(str(R.string.recipients_send_with_x, m.providerName), {
                            methodSheet = null
                            startSend(c, nav, banner, recipient = r)
                            c.session.send.payoutType = m.type; c.session.send.methodId = m.id
                        }, style = Btn.Line)
                        if (r.methods.size > 1) ArroButton(str(R.string.recipients_remove), { b.removePayoutMethod(r.id, m.id); methodSheet = null }, style = Btn.DangerText)
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(r.initials, r.color, 84.dp, flag = flagOf(r.country))
            Txt(r.name, T.H2.copy(fontSize = 20.sp), Modifier.padding(top = 6.dp), align = TextAlign.Center)
            Cap(listOf(r.relationship, listOf(r.city, country?.name ?: "").filter { it.isNotBlank() }.joinToString(", ")).filter { it.isNotBlank() }.joinToString(" · "),
                align = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArroButton(str(R.string.recipients_send_money), { startSend(c, nav, banner, recipient = r) }, Modifier.weight(1f), icon = Icons.ArrowUpRight, enabled = r.methods.isNotEmpty())
            RoundIcon(if (r.favourite) Icons.StarFilled else Icons.Star, { b.toggleFavourite(r.id) }, style = IconBtn.Line, size = 56.dp,
                description = if (r.favourite) str(R.string.recipients_remove_from_favourites) else str(R.string.recipients_add_to_favourites))
        }
        Column {
            SectionHeader(str(R.string.recipients_payout_methods), if (r.country != "GB") str(R.string.recipients_add) else null, { nav.go(Routes.rcpBank(r.id)) })
            Group {
                r.methods.forEachIndexed { i, m ->
                    if (i > 0) GroupDivider()
                    val isDefault = m.id == r.defaultMethodId
                    ListRow(m.providerName, flat = true,
                        subtitle = when (m.type) {
                            PayoutType.BANK -> "${m.accountType.ifBlank { "Account" }} •• ${m.account.takeLast(4)}${if (m.nameChecked) " · name checked" else ""}"
                            else -> "${m.full}${if (m.nameChecked) " · name checked" else ""}"
                        },
                        leading = { Tile(deliveryIcon(m.type), if (isDefault) TileStyle.Lime else TileStyle.Tint) },
                        trailing = { if (isDefault && m.nameChecked) Ic(Icons.ShieldCheck, tint = C.Ok) else Chevron() },
                        onClick = { methodSheet = m },
                        content = { if (isDefault) Pill(str(R.string.recipients_default), PillKind.Neutral, modifier = Modifier.padding(top = 4.dp)) })
                }
                if (r.methods.isEmpty()) ListRow(str(R.string.recipients_add_a_way_to_receive_money), flat = true, onClick = { nav.go(Routes.rcpBank(r.id)) }, trailing = { Chevron() })
            }
        }
        Column {
            SectionHeader(str(R.string.recipients_transfers), null, null)
            Cap(str(R.string.recipients_x_sent_this_year, Money.format(Insights.sentThisYear(a, r.id, now), app.arro.core.Currency.GBP)), Modifier.padding(bottom = 10.dp))
            if (transfers.isEmpty()) Cap(str(R.string.recipients_no_transfers_yet))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transfers.take(8).forEach { t ->
                    val (label, _) = statusLabel(t)
                    val kind = when (t.status) {
                        TransferStatus.DELIVERED -> PillKind.Ok
                        TransferStatus.ON_HOLD -> PillKind.Warn
                        TransferStatus.FAILED -> PillKind.Err
                        TransferStatus.CANCELLED, TransferStatus.REFUNDED -> PillKind.Neutral
                        else -> PillKind.Info
                    }
                    ListRow(Money.format(t.receiveMinor, t.toCur), subtitle = "${Text.relativeDay(t.createdAt, now)} · ${Money.format(t.totalMinor, t.fromCur)}",
                        minHeight = 56.dp, trailing = { Pill(label, kind, dot = true) },
                        onClick = { nav.go(if (t.isOpen || t.refundPending) Routes.track(t.id) else Routes.detail(t.id)) })
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 6.03 Add recipient (+ 12.04 validation)

@Composable
fun AddRecipientScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val d = c.session.recipient
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    var picker by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    val country = Catalog.country(d.country)
    val types = country.options.map { it.type }
    if (d.type !in types) { d.type = types.first(); d.providerId = Catalog.providersFor(d.country, d.type).firstOrNull()?.id ?: "" }
    val providers = Catalog.providersFor(d.country, d.type)
    val provider = Catalog.provider(d.providerId)
    val bank = Catalog.bank(d.providerId)
    val hint = if (d.type == PayoutType.MOBILE_MONEY) Validation.networkHint(d.number, country, provider) else null
    fun validate(): Map<String, String> {
        val e = mutableMapOf<String, String>()
        when (d.type) {
            PayoutType.MOBILE_MONEY, PayoutType.EWALLET, PayoutType.CASH -> Validation.mobile(d.number, country)?.let { e["number"] = it }
            PayoutType.BANK -> Validation.bankAccount(d.number, bank)?.let { e["number"] = it }
            PayoutType.UPI -> Validation.upi(d.number)?.let { e["number"] = it }
            PayoutType.ARRO -> {}
        }
        if (d.type == PayoutType.BANK && bank == null) e["provider"] = str(R.string.recipients_choose_their_bank)
        Validation.fullName(d.name, provider?.name?.removeSuffix(" Money")?.let { if (it.contains("MoMo")) str(R.string.recipients_momo) else it } ?: "")?.let { e["name"] = it }
        if (d.relationship.isBlank()) e["relationship"] = str(R.string.recipients_choose_how_you_know_them)
        return e
    }
    fun submit() {
        errors = validate()
        if (errors.isNotEmpty()) return
        val account = when (d.type) {
            PayoutType.MOBILE_MONEY, PayoutType.EWALLET, PayoutType.CASH -> country.dialCode + Validation.nationalDigits(d.number, country)
            PayoutType.BANK -> d.number.filter { it.isDigit() }
            else -> d.number.trim()
        }
        scope.launchCall(call, onError = { e -> if (e.code == "not_found") { errors = errors + ("number" to (e.message ?: "")); true } else false }) {
            val check = when (d.type) {
                PayoutType.CASH, PayoutType.UPI -> app.arro.core.NameCheck(d.name.trim().uppercase(), true, if (d.type == PayoutType.CASH) str(R.string.recipients_arro_cash_agents) else "UPI")
                else -> b.checkName(d.country, d.type, d.providerId, account, d.name)
            }
            d.check = check
            d.number = account
            nav.go(Routes.RCP_CHECK)
        }
    }
    val numberLabel = when (d.type) {
        PayoutType.MOBILE_MONEY -> str(R.string.recipients_mobile_money_number)
        PayoutType.EWALLET -> str(R.string.recipients_x_number, provider?.name ?: str(R.string.recipients_wallet))
        PayoutType.CASH -> str(R.string.recipients_their_mobile_number_for_the_pickup)
        PayoutType.BANK -> if (bank?.note?.isNotBlank() == true) str(R.string.recipients_account_number_x, bank.note) else str(R.string.recipients_account_number)
        PayoutType.UPI -> str(R.string.recipients_upi_id)
        PayoutType.ARRO -> str(R.string.recipients_username)
    }
    val nameLabel = when (d.type) {
        PayoutType.CASH -> str(R.string.recipients_full_name_as_on_their_id)
        PayoutType.BANK -> str(R.string.recipients_full_name_as_on_their_account)
        else -> str(R.string.recipients_full_name_as_registered_with_x, provider?.name?.split(" ")?.first() ?: str(R.string.recipients_the_provider))
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.recipients_add_recipient)) },
        gap = 14.dp,
        footer = { ArroButton(str(R.string.recipients_check_details), { submit() }, loading = call.busy.value) },
        overlay = {
            PickerSheet(picker == "country", str(R.string.recipients_country), Catalog.countries.map { it.name }, country.name, { name ->
                val ct = Catalog.countries.first { it.name == name }
                d.country = ct.code; d.type = ct.options.first().type
                d.providerId = Catalog.providersFor(ct.code, d.type).firstOrNull()?.id ?: ""; d.number = ""; errors = emptyMap()
            }, { picker = "" }, leading = { name -> app.arro.ui.components.Flag(Catalog.countries.first { it.name == name }.currency.flag) })
            PickerSheet(picker == "bank", str(R.string.recipients_bank), providers.map { it.name }, provider?.name, { name -> d.providerId = providers.first { it.name == name }.id; errors = errors - "provider" },
                { picker = "" }, leading = { Tile(Icons.Bank, size = 28.dp, iconSize = 16.dp) })
            PickerSheet(picker == "rel", str(R.string.recipients_relationship), Catalog.relationships, d.relationship, { d.relationship = it; errors = errors - "relationship" }, { picker = "" })
        },
    ) {
        if (errors.isNotEmpty()) {
            Note(str(R.string.recipients_x_x_to_fix_before_we, errors.size, if (errors.size == 1) str(R.string.recipients_thing) else str(R.string.recipients_things)), NoteKind.Err, Icons.Alert)
        }
        SelectField(str(R.string.recipients_country), country.name, { picker = "country" }, leading = { app.arro.ui.components.Flag(country.currency.flag) })
        if (types.size > 1) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(str(R.string.recipients_how_will_they_receive_money))
            Segmented(types.map { when (it) { PayoutType.MOBILE_MONEY -> str(R.string.recipients_mobile_money); PayoutType.BANK -> str(R.string.recipients_bank); PayoutType.CASH -> str(R.string.recipients_cash); PayoutType.EWALLET -> "E-wallet"; PayoutType.UPI -> "UPI"; PayoutType.ARRO -> str(R.string.recipients_arro) } },
                types.indexOf(d.type), { i ->
                    d.type = types[i]; d.providerId = Catalog.providersFor(d.country, d.type).firstOrNull()?.id ?: ""; d.number = ""; errors = emptyMap()
                })
        }
        when (d.type) {
            PayoutType.MOBILE_MONEY, PayoutType.EWALLET -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(str(R.string.recipients_provider), color = if (errors.containsKey("provider")) C.Err else C.Slate)
                providers.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { p ->
                            val on = p.id == d.providerId
                            ListRow(p.name, Modifier.weight(1f), selected = on, minHeight = 52.dp, titleStyle = T.RowTitle.copy(fontSize = 14.sp),
                                leading = { Tile(deliveryIcon(d.type), if (on) TileStyle.Lime else TileStyle.Tint, size = 30.dp, iconSize = 16.dp) },
                                onClick = { d.providerId = p.id })
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
            PayoutType.BANK -> SelectField(str(R.string.recipients_bank), provider?.name, { picker = "bank" }, placeholder = str(R.string.recipients_choose_their_bank_2), error = errors["provider"],
                leading = { Tile(Icons.Bank, size = 28.dp, iconSize = 16.dp) })
            else -> {}
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(numberLabel, color = if (errors.containsKey("number")) C.Err else C.Slate)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val phoneLike = d.type == PayoutType.MOBILE_MONEY || d.type == PayoutType.EWALLET || d.type == PayoutType.CASH
                if (phoneLike) Box(
                    Modifier.width(84.dp).size(84.dp, 54.dp).background(C.Field, RoundedCornerShape(14.dp)).border(1.5.dp, C.Line, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Txt(country.dialCode, T.Input) }
                ArroField(
                    d.number, { d.number = it; errors = errors - "number" }, Modifier.weight(1f),
                    placeholder = when (d.type) { PayoutType.BANK -> "0123456789"; PayoutType.UPI -> "name@okbank"; else -> "788 214 412" },
                    keyboard = if (d.type == PayoutType.UPI) KeyboardType.Email else KeyboardType.Number,
                    textStyle = T.Input.copy(fontFeatureSettings = "tnum"),
                    error = if (errors.containsKey("number")) "" else null,
                )
            }
            FieldHint(errors["number"], hint = hint)
        }
        ArroField(d.name, { d.name = it; errors = errors - "name" }, label = nameLabel, placeholder = str(R.string.recipients_first_and_last_name),
            capitalization = KeyboardCapitalization.Words, error = errors["name"])
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectField(str(R.string.recipients_relationship), d.relationship.ifBlank { null }, { picker = "rel" }, Modifier.weight(1f), error = errors["relationship"])
            ArroField(d.city, { d.city = it }, Modifier.weight(1f), label = str(R.string.recipients_city_optional), placeholder = country.city, capitalization = KeyboardCapitalization.Words)
        }
        FieldHint(call.error.value)
    }
}

// ------------------------------------------------------------------ 6.04 Confirm account name

@Composable
fun NameCheckScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val d = c.session.recipient
    val check = d.check ?: return
    val country = Catalog.country(d.country)
    val first = Text.titleCase(check.registeredName).split(" ").first()
    fun save() {
        val method = b.newPayoutMethod(d.type, d.providerId.ifBlank { "cash_${d.country.lowercase()}" }, d.number, check.registeredName,
            if (d.type == PayoutType.BANK) d.accountType else "", checked = d.type != PayoutType.CASH)
        val r = b.addRecipient(if (check.matches) d.name else check.registeredName, d.country, d.relationship, d.city, method)
        banner.show(str(R.string.recipients_x_saved, r.firstName), str(R.string.recipients_you_can_send_to_x_any, r.firstName), BannerKind.Success)
        if (d.forSend) {
            val s = c.session.send
            s.recipientId = r.id; s.methodId = method.id; s.payoutType = method.type
            nav.popBackStack(Routes.SEND_RECIPIENT, inclusive = false)
            nav.go(Routes.SEND_FUNDING)
        } else {
            nav.popBackStack(Routes.RCP_ADD, inclusive = true)
            nav.go(Routes.recipient(r.id))
        }
    }
    ArroScreen(
        background = C.Tint,
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 20.dp,
        footer = {
            ArroButton(str(R.string.recipients_yes_save_x, first), { save() })
            ArroButton(str(R.string.recipients_no_change_details), { nav.back() }, style = Btn.Line)
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.Shield, 128.dp, description = str(R.string.recipients_a_glass_shield_with_a_tick)) }
        ScreenTitle(str(R.string.recipients_is_this_the_right_person), when (d.type) {
            PayoutType.CASH -> str(R.string.recipients_the_agent_will_check_this_name, first)
            PayoutType.BANK -> str(R.string.recipients_we_checked_the_account_with_x, check.providerName)
            else -> str(R.string.recipients_we_checked_the_number_with_x, check.providerName)
        }, align = TextAlign.Center)
        ArroCard(
            Modifier.selectionRing(20.dp, 4.dp, C.Lime.copy(alpha = 0.35f)),
            border = if (check.matches) androidx.compose.ui.graphics.Color(0xFF9FD97A) else C.WarnFill,
        ) {
            Over(when (d.type) { PayoutType.BANK -> str(R.string.recipients_registered_name_on_this_account); PayoutType.CASH -> str(R.string.recipients_name_for_pickup); else -> str(R.string.recipients_registered_name_on_this_wallet) })
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt(check.registeredName, T.Big22.copy(letterSpacing = 0.4.sp), Modifier.weight(1f))
                Tile(if (check.matches) Icons.Check else Icons.Alert, if (check.matches) TileStyle.Lime else TileStyle.Warn, size = 32.dp, round = true, iconSize = 16.dp)
            }
            Box(Modifier.padding(vertical = 12.dp).fillMaxWidth().size(1.dp).background(C.Line))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KV(if (d.type == PayoutType.BANK) str(R.string.recipients_account) else str(R.string.recipients_number), if (d.type == PayoutType.BANK || d.type == PayoutType.UPI) d.number else Text.formatPhone(d.number))
                KV(str(R.string.recipients_provider), "${check.providerName}, ${country.name}")
            }
        }
        if (check.matches) Note(str(R.string.recipients_matches_the_name_you_entered), NoteKind.Ok, Icons.Check)
        else Note(str(R.string.recipients_this_doesn_t_match_the_name, d.name.trim()), NoteKind.Warn, Icons.Alert)
    }
}

// ------------------------------------------------------------------ 6.05 Add bank payout (or mobile money)

@Composable
fun BankPayoutScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val r = a.recipient(id) ?: return
    val country = Catalog.country(r.country)
    val types = country.options.map { it.type }.filter { it == PayoutType.BANK || it == PayoutType.MOBILE_MONEY || it == PayoutType.EWALLET }
    var type by remember { mutableStateOf(if (PayoutType.BANK in types) PayoutType.BANK else types.first()) }
    val providers = Catalog.providersFor(r.country, type)
    var providerId by remember(type) { mutableStateOf(providers.firstOrNull { p -> r.methods.none { it.providerId == p.id } }?.id ?: providers.firstOrNull()?.id ?: "") }
    var number by remember(type) { mutableStateOf("") }
    var accountType by remember { mutableStateOf(str(R.string.recipients_savings)) }
    var reason by remember { mutableStateOf(str(R.string.recipients_family_support)) }
    var makeDefault by remember { mutableStateOf(true) }
    var picker by remember { mutableStateOf("") }
    var lookup by remember { mutableStateOf<app.arro.core.NameCheck?>(null) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val bank = Catalog.bank(providerId)
    val provider = Catalog.provider(providerId)
    val digits = number.filter { it.isDigit() }
    val formatError = if (type == PayoutType.BANK) Validation.bankAccount(number, bank) else Validation.mobile(number, country)
    val account = if (type == PayoutType.BANK) digits else country.dialCode + Validation.nationalDigits(number, country)
    LaunchedEffect(providerId, digits, type) {
        lookup = null; lookupError = null
        if (digits.isNotEmpty() && formatError == null) {
            delay(400)
            try { lookup = b.checkName(r.country, type, providerId, account, r.name) } catch (e: ArroException) { lookupError = e.message }
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = if (type == PayoutType.BANK) str(R.string.recipients_bank_details) else str(R.string.recipients_mobile_money)) },
        gap = 14.dp,
        footer = {
            ArroButton(if (type == PayoutType.BANK) str(R.string.recipients_save_bank_account) else str(R.string.recipients_save_number), {
                val l = lookup ?: return@ArroButton
                val m = b.newPayoutMethod(type, providerId, account, l.registeredName, if (type == PayoutType.BANK) accountType else "", reason)
                b.addPayoutMethod(r.id, m, makeDefault)
                banner.show(str(R.string.recipients_x_added, provider?.name ?: str(R.string.recipients_account)), str(R.string.recipients_you_can_now_send_to_x, r.firstName), BannerKind.Success)
                nav.back()
            }, enabled = lookup != null, loading = lookup == null && digits.isNotEmpty() && formatError == null && lookupError == null)
        },
        overlay = {
            PickerSheet(picker == "bank", str(R.string.recipients_bank), providers.map { it.name }, provider?.name, { name -> providerId = providers.first { it.name == name }.id }, { picker = "" },
                leading = { Tile(Icons.Bank, size = 28.dp, iconSize = 16.dp) })
            PickerSheet(picker == "reason", str(R.string.recipients_usual_reason_for_sending), Catalog.reasons, reason, { reason = it }, { picker = "" })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(r.initials, r.color, flag = country.currency.flag)
            Column { Txt(r.name, T.H3); Cap("${country.name} · ${country.currency.code}") }
        }
        if (types.size > 1) Segmented(types.map { if (it == PayoutType.BANK) str(R.string.recipients_bank) else it.label }, types.indexOf(type), { type = types[it] })
        if (type == PayoutType.BANK) {
            SelectField(str(R.string.recipients_bank), provider?.name, { picker = "bank" }, leading = { Tile(Icons.Bank, size = 28.dp, iconSize = 16.dp) })
        } else {
            SelectField(str(R.string.recipients_provider), provider?.name, { picker = "bank" }, leading = { Tile(Icons.Smartphone, size = 28.dp, iconSize = 16.dp) })
        }
        ArroField(number, { number = it }, label = if (type == PayoutType.BANK) (if (bank?.note?.isNotBlank() == true) str(R.string.recipients_account_number_x, bank.note) else str(R.string.recipients_account_number)) else str(R.string.recipients_mobile_money_number),
            keyboard = KeyboardType.Number, textStyle = T.Input.copy(fontFeatureSettings = "tnum", letterSpacing = 1.sp),
            prefix = if (type != PayoutType.BANK) ({ app.arro.ui.components.DialPrefix(country.dialCode) }) else null,
            error = if (digits.length >= (bank?.accountDigits?.first ?: country.mobileDigits) && formatError != null) formatError else lookupError,
            ok = if (lookup != null) str(R.string.recipients_account_found) else null)
        lookup?.let { l ->
            Row(
                Modifier.fillMaxWidth().background(C.Tint, RoundedCornerShape(20.dp)).border(1.5.dp, C.Mist, RoundedCornerShape(20.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Tile(if (l.matches) Icons.Check else Icons.Alert, if (l.matches) TileStyle.Lime else TileStyle.Warn, size = 32.dp, round = true, iconSize = 16.dp)
                Column(Modifier.weight(1f)) {
                    Cap(str(R.string.recipients_account_name_from_x, l.providerName), size = 12f)
                    Txt(l.registeredName, T.H3, Modifier.padding(top = 2.dp))
                    if (!l.matches) Cap(str(R.string.recipients_this_doesn_t_match_x_check, r.name), color = C.Warn, size = 12f)
                }
            }
        }
        if (type == PayoutType.BANK) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(str(R.string.recipients_account_type))
            Segmented(listOf(str(R.string.recipients_savings), str(R.string.recipients_current)), if (accountType == str(R.string.recipients_savings)) 0 else 1, { accountType = if (it == 0) str(R.string.recipients_savings) else str(R.string.recipients_current) })
        }
        SelectField(str(R.string.recipients_usual_reason_for_sending), reason, { picker = "reason" })
        CheckRow(makeDefault, { makeDefault = it }, str(R.string.recipients_make_this_x_s_default_payout, r.firstName))
    }
}

// ------------------------------------------------------------------ 6.06 Edit recipient

@Composable
fun EditRecipientScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val r = a.recipient(id) ?: return
    val m = r.defaultMethod
    val country = Catalog.countryOrNull(r.country)
    var name by rememberSaveable { mutableStateOf(r.name) }
    var color by rememberSaveable { mutableIntStateOf(r.color) }
    var number by rememberSaveable {
        mutableStateOf(if (m != null && country != null && m.type != PayoutType.BANK && m.type != PayoutType.ARRO) Validation.nationalDigits(m.account, country) else m?.account ?: "")
    }
    var relationship by rememberSaveable { mutableStateOf(r.relationship) }
    var nickname by rememberSaveable { mutableStateOf(r.nickname) }
    var sms by rememberSaveable { mutableStateOf(r.smsOnArrival) }
    var picker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val editable = m != null && m.type != PayoutType.ARRO && country != null
    fun save() {
        val e = mutableMapOf<String, String>()
        Validation.fullName(name)?.let { e["name"] = it }
        if (editable) {
            (if (m.type == PayoutType.BANK) Validation.bankAccount(number, Catalog.bank(m.providerId)) else Validation.mobile(number, country))?.let { e["number"] = it }
        }
        errors = e
        if (e.isNotEmpty()) return
        scope.launchCall(call, onError = { ex -> if (ex.code == "not_found") { errors = mapOf("number" to (ex.message ?: "")); true } else false }) {
            var newMethod = m
            if (editable) {
                val account = if (m.type == PayoutType.BANK) number.filter { it.isDigit() } else country.dialCode + Validation.nationalDigits(number, country)
                if (account != m.account) {
                    val check = b.checkName(r.country, m.type, m.providerId, account, name)
                    newMethod = m.copy(account = account, accountName = check.registeredName, nameChecked = true, checkedAt = System.currentTimeMillis())
                    if (!check.matches) banner.show(str(R.string.recipients_name_doesn_t_match), str(R.string.recipients_x_has_this_number_registered_to, check.providerName, check.registeredName), BannerKind.Error)
                }
            }
            val updated = newMethod
            b.updateRecipient(r.id) { old ->
                old.copy(name = Text.titleCase(name), color = color, relationship = relationship, nickname = nickname.trim(), smsOnArrival = sms,
                    methods = old.methods.map { if (updated != null && it.id == updated.id) updated else it })
            }
            banner.show(str(R.string.recipients_changes_saved), kind = BannerKind.Success)
            nav.back()
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.recipients_edit_recipient)) },
        gap = 14.dp,
        footer = { ArroButton(str(R.string.recipients_save_changes), { save() }, loading = call.busy.value) },
        overlay = {
            PickerSheet(picker, str(R.string.recipients_relationship), Catalog.relationships, relationship, { relationship = it }, { picker = false })
            ConfirmDialog(confirmDelete, str(R.string.recipients_delete_x, r.firstName), str(R.string.recipients_you_won_t_be_able_to, r.firstName),
                str(R.string.recipients_delete_recipient), {
                    confirmDelete = false
                    b.deleteRecipient(r.id)
                    banner.show(str(R.string.recipients_x_deleted, r.firstName), kind = BannerKind.Info)
                    if (!nav.popBackStack(Routes.RECIPIENTS, inclusive = false)) nav.tab(Routes.RECIPIENTS)
                }, { confirmDelete = false }, danger = true)
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Avatar(Text.initials(name.ifBlank { r.name }), color, 64.dp, flag = flagOf(r.country))
            LinkText(str(R.string.recipients_change_colour), { color = color % 6 + 1 }, color = C.Ink, strong = true)
        }
        ArroField(name, { name = it; errors = errors - "name" }, label = str(R.string.recipients_full_name), capitalization = KeyboardCapitalization.Words, error = errors["name"])
        if (editable) {
            ArroField(number, { number = it; errors = errors - "number" },
                label = if (m.type == PayoutType.BANK) str(R.string.recipients_x_account_number, m.providerName) else str(R.string.recipients_x_number, m.providerName),
                keyboard = KeyboardType.Number, textStyle = T.Input.copy(fontFeatureSettings = "tnum"),
                prefix = if (m.type != PayoutType.BANK) ({ app.arro.ui.components.DialPrefix(country.dialCode) }) else null,
                error = errors["number"], hint = str(R.string.recipients_we_ll_check_the_registered_name))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectField(str(R.string.recipients_relationship), relationship.ifBlank { null }, { picker = true }, Modifier.weight(1f))
            ArroField(nickname, { nickname = it }, Modifier.weight(1f), label = str(R.string.recipients_nickname), placeholder = str(R.string.recipients_optional))
        }
        ListRow(str(R.string.recipients_text_x_when_money_arrives, r.firstName), subtitle = str(R.string.recipients_free_sms_in_x_or_english, if (r.country == "RW") str(R.string.recipients_kinyarwanda) else str(R.string.recipients_their_language)),
            trailing = { Toggle(sms, { sms = it }) }, onClick = { sms = !sms })
        Row(Modifier.padding(top = 4.dp)) {
            ArroButton(str(R.string.recipients_delete_recipient), { confirmDelete = true }, style = Btn.DangerText, size = BtnSize.M, fill = false, icon = Icons.Trash, padding = 0.dp)
        }
        FieldHint(call.error.value)
    }
}
