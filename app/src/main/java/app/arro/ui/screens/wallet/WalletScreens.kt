package app.arro.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.Account
import app.arro.core.Currency
import app.arro.core.FeedItem
import app.arro.core.LedgerType
import app.arro.core.MethodKind
import app.arro.core.Money
import app.arro.core.Text
import app.arro.core.Wallet
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
import app.arro.ui.rememberCall
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.BigAmount
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.Chevron
import app.arro.ui.components.Chip
import app.arro.ui.components.ConfirmDialog
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.Flag
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.HDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.QuickAction
import app.arro.ui.components.Radio
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SectionHeader
import app.arro.ui.components.Segmented
import app.arro.ui.components.SelectField
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.cardBrush
import app.arro.ui.components.cardInk
import app.arro.ui.screens.send.startSend
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ------------------------------------------------------------------ shared pieces

/** Keeps typed amounts tidy: digits and one decimal point, at most [decimals] places and 7 whole digits. */
fun cleanAmount(input: String, decimals: Int): String {
    val s = input.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = s.indexOf('.')
    val whole = (if (dot >= 0) s.substring(0, dot) else s).trimStart('0').take(7).ifEmpty { if (s.isNotEmpty()) "0" else "" }
    if (dot < 0 || decimals == 0) return whole
    val frac = s.substring(dot + 1).filter { it.isDigit() }.take(decimals)
    return whole.ifEmpty { "0" } + "." + frac
}

/** Shows a typed amount as "£1,250.5" with the missing decimals in grey; the raw text stays digits and a dot. */
class AmountTransformation(private val cur: Currency, private val suffixSize: Float) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dot = raw.indexOf('.')
        val whole = if (dot >= 0) raw.substring(0, dot) else raw
        val o2t = IntArray(raw.length + 1)
        val out = AnnotatedString.Builder()
        out.append(cur.symbol ?: "")
        if (whole.isEmpty()) {
            o2t[0] = out.length
            out.withStyle(SpanStyle(color = C.Placeholder)) { append("0") }
        } else {
            whole.forEachIndexed { i, ch ->
                if (i > 0 && (whole.length - i) % 3 == 0) out.append(',')
                o2t[i] = out.length
                out.append(ch)
            }
            o2t[whole.length] = out.length
        }
        var typedDecimals = 0
        if (dot >= 0) {
            o2t[dot] = out.length
            out.append('.')
            for (k in dot + 1 until raw.length) { o2t[k] = out.length; out.append(raw[k]); typedDecimals++ }
            o2t[raw.length] = out.length
        }
        if (cur.decimals > 0) out.withStyle(SpanStyle(color = C.Placeholder)) {
            append((if (dot < 0) "." else "") + "0".repeat((cur.decimals - typedDecimals).coerceAtLeast(0)))
        }
        if (cur.symbol == null) out.withStyle(SpanStyle(color = C.Slate, fontSize = suffixSize.sp)) { append(" " + cur.code) }
        val transformed = out.toAnnotatedString()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = o2t[offset.coerceIn(0, raw.length)].coerceIn(0, transformed.length)
            override fun transformedToOriginal(offset: Int): Int {
                var best = 0
                for (i in 0..raw.length) if (o2t[i] <= offset) best = i
                return best
            }
        }
        return TransformedText(transformed, mapping)
    }
}

/** A large editable amount using the system number keyboard. */
@Composable
fun AmountInput(
    text: String,
    onChange: (String) -> Unit,
    cur: Currency,
    modifier: Modifier = Modifier,
    size: Float = 56f,
    align: TextAlign = TextAlign.Center,
    autoFocus: Boolean = false,
) {
    val style = TextStyle(fontFamily = app.arro.ui.theme.Outfit, fontSize = size.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em,
        color = C.Ink, textAlign = align, fontFeatureSettings = "tnum", lineHeight = (size * 1.15f).sp)
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = text,
        onValueChange = { onChange(cleanAmount(it, cur.decimals)) },
        modifier = modifier.fillMaxWidth().focusRequester(focus),
        textStyle = style,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        visualTransformation = remember(cur, size) { AmountTransformation(cur, size * 0.45f) },
        cursorBrush = SolidColor(C.LimeDeep),
    )
}

/** The bank details someone needs to pay into a balance. */
fun accountDetails(a: Account, w: Wallet): List<Pair<String, String>> = when (w.currency) {
    "EUR" -> listOf("Name" to a.displayName, "IBAN" to w.iban, "BIC" to "ARROGB2L")
    "USD" -> listOf("Name" to a.displayName, "Routing number (ACH)" to w.sortCode, "Account number" to w.accountNumber, "Account type" to "Checking")
    else -> listOf("Name" to a.displayName, "Sort code" to w.sortCode, "Account number" to w.accountNumber.chunked(4).joinToString(" "))
}

@Composable
private fun CurrencyBalanceRow(w: Wallet, onClick: () -> Unit) {
    val c = container()
    val cur = w.cur
    ListRow(cur.displayName, minHeight = 76.dp, subtitle = "≈ ${Money.format(c.backend.inGbp(w.balanceMinor, cur), Currency.GBP)}",
        leading = { Flag(cur.flag, 36.dp) },
        trailing = { Txt(Money.format(w.balanceMinor, cur), T.Num.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)) },
        onClick = onClick)
}

// ------------------------------------------------------------------ 8.01 Wallet

@Composable
fun WalletScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    var openSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val main = a.wallets.firstOrNull { it.main } ?: a.wallets.firstOrNull()
    val others = a.wallets.filter { it !== main }
    val available = Currency.wallets.filter { cur -> a.wallets.none { it.currency == cur.code } }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Statement, { nav.go(Routes.STATEMENTS) }, style = IconBtn.Soft, description = "Statements") }) },
        gap = 20.dp,
        overlay = {
            Sheet(openSheet, { openSheet = false }) {
                SheetHeader("Open a balance", { openSheet = false }, "Hold money in another currency, then convert or send it when the rate suits you.")
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (available.isEmpty()) Cap("You hold every currency Arro offers for balances. More are coming soon.")
                    available.forEach { cur ->
                        ListRow(cur.displayName, subtitle = when (cur) {
                            Currency.EUR -> "Your own IBAN for euro payments"
                            Currency.USD -> "US account details for USD payments"
                            else -> cur.code
                        }, leading = { Flag(cur.flag, 36.dp) }, trailing = { Chevron() }, onClick = {
                            scope.launchCall(call) {
                                b.openCurrency(cur)
                                openSheet = false
                                banner.show("${cur.displayName} balance opened", "Add money or convert into it whenever you're ready.", BannerKind.Success)
                                nav.go(Routes.walletAccount(cur.code))
                            }
                        })
                    }
                    FieldHint(call.error.value)
                    Note("Balances in RWF, KES and other payout currencies are coming later this year.", NoteKind.Info, Icons.Info)
                }
            }
        },
    ) {
        Column {
            ScreenTitle("Wallet")
            BigAmount(b.totalInGbp(a), Currency.GBP, Modifier.padding(top = 14.dp))
            Cap("Total in GBP across ${a.wallets.size} ${if (a.wallets.size == 1) "currency" else "currencies"}, at today's rates", Modifier.padding(top = 6.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Add", Icons.Plus, { nav.go(Routes.addMoney(main?.currency ?: "GBP")) }, Modifier.weight(1f))
            QuickAction("Convert", Icons.Swap, { nav.go(Routes.convert(main?.currency ?: "GBP")) }, Modifier.weight(1f))
            QuickAction("Send", Icons.ArrowUpRight, { startSend(c, nav, banner) }, Modifier.weight(1f))
            QuickAction("Statements", Icons.Download, { nav.go(Routes.STATEMENTS) }, Modifier.weight(1f), lime = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (main != null) {
                Row(
                    Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(22.dp)).background(cardBrush(main.currency))
                        .clickable(role = Role.Button) { nav.go(Routes.walletAccount(main.currency)) }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Flag(main.cur.flag, 36.dp)
                    Column(Modifier.weight(1f)) {
                        Txt(main.cur.displayName, T.H3)
                        Cap("Account •• ${main.last4}${if (main.main) " · main" else ""}", color = cardInk(main.currency))
                    }
                    Txt(Money.format(main.balanceMinor, main.cur), T.Num.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
            }
            others.forEach { w -> CurrencyBalanceRow(w) { nav.go(Routes.walletAccount(w.currency)) } }
            ListRow("Open another currency", dashed = true, minHeight = 60.dp, subtitle = "Hold money before you send it",
                leading = { Tile(Icons.Plus, round = true) }, onClick = { call.error.value = null; openSheet = true })
        }
    }
}

// ------------------------------------------------------------------ 8.02 Currency account

@Composable
fun WalletAccountScreen(code: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val cur = Currency.of(code)
    val w = a.wallet(cur)
    var more by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    if (w == null) {
        ArroScreen(topBar = { TopBar(onBack = { nav.back() }, title = "${cur.code} account") }) {
            Note("You don't have a ${cur.displayName} balance yet.", NoteKind.Info, Icons.Info)
            if (cur in Currency.wallets) ArroButton("Open a ${cur.code} balance", {
                scope.launchCall(call) { b.openCurrency(cur); banner.show("${cur.displayName} balance opened", kind = BannerKind.Success) }
            }, loading = call.busy.value)
            FieldHint(call.error.value)
        }
        return
    }
    val details = accountDetails(a, w)
    val now = b.now
    val items = b.feedFor(a, cur).take(5)
    ArroScreen(
        topBar = {
            TopBar(onBack = { nav.back() }, title = "${cur.code} account", right = {
                if (!w.main) RoundIcon(Icons.More, { more = true }, style = IconBtn.Soft, description = "More options") else androidx.compose.foundation.layout.Spacer(Modifier.width(44.dp))
            })
        },
        overlay = {
            Sheet(more, { more = false }) {
                SheetHeader("${cur.displayName} balance", { more = false }, "Opened ${Text.date(w.openedAt.takeIf { it > 0 } ?: a.createdAt)}")
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroButton("Download statement", { more = false; c.session.walletCurrency = cur; nav.go(Routes.STATEMENTS) }, style = Btn.Line)
                    ArroButton("Close this balance", { more = false; confirmClose = true }, style = Btn.DangerText)
                }
            }
            ConfirmDialog(confirmClose, "Close your ${cur.code} balance?", "Your ${cur.code} account details stop working straight away. You can open a new balance later.",
                "Close balance", {
                    scope.launchCall(call, onError = { e -> banner.show("Can't close yet", e.message ?: "", BannerKind.Error); confirmClose = false; true }) {
                        b.closeCurrency(cur)
                        confirmClose = false
                        banner.show("${cur.code} balance closed", kind = BannerKind.Info)
                        nav.back()
                    }
                }, { confirmClose = false }, danger = true, loading = call.busy.value)
        },
    ) {
        Column(
            Modifier.fillMaxWidth().height(148.dp).clip(RoundedCornerShape(22.dp)).background(cardBrush(cur.code)).padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Flag(cur.flag, 24.dp)
                Txt(cur.displayName, T.H3, Modifier.padding(start = 8.dp).weight(1f))
                if (w.main) Box(Modifier.background(C.Ink.copy(alpha = 0.08f), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Txt("Main", T.Pill) }
            }
            Column {
                Cap("Balance", color = cardInk(cur.code))
                Txt(Money.format(w.balanceMinor, cur), T.AmountMd)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArroButton("Add", { nav.go(Routes.addMoney(cur.code)) }, Modifier.weight(1f), size = BtnSize.M, fill = true)
            ArroButton("Convert", { nav.go(Routes.convert(cur.code)) }, Modifier.weight(1f), style = Btn.Line, size = BtnSize.M, fill = true)
            ArroButton("Statement", { c.session.walletCurrency = cur; nav.go(Routes.STATEMENTS) }, Modifier.weight(1f), style = Btn.Line, size = BtnSize.M, fill = true)
        }
        ArroCard(padding = 16.dp) {
            Txt(when (cur) {
                Currency.EUR -> "Receive euros by SEPA transfer"
                Currency.USD -> "Receive dollars from US accounts"
                else -> "Receive pounds from any UK bank"
            }, T.H3)
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.forEach { (k, v) -> KV(k, v) }
            }
            ArroButton("Copy details", {
                Share.copy(context, "${cur.code} account details", details.joinToString("\n") { "${it.first}: ${it.second}" })
                banner.show("Account details copied", kind = BannerKind.Success)
            }, Modifier.padding(top = 12.dp).height(40.dp), style = Btn.Lime, size = BtnSize.S, icon = Icons.Copy, fill = true)
        }
        Column {
            SectionHeader("${cur.code} activity", if (items.isNotEmpty()) "See all" else null, { c.session.walletCurrency = cur; nav.go(Routes.ACTIVITY) })
            if (items.isEmpty()) Cap("Nothing in ${cur.code} yet. Money you add, convert or send from this balance shows up here.")
            items.forEachIndexed { i, item ->
                if (i > 0) HDivider()
                CompactFeedRow(item, cur, now)
            }
        }
    }
}

/** A flat activity row for one currency: "To Wanjiru Kamau · 12 Oct · M-Pesa · −£151.49". */
@Composable
fun CompactFeedRow(item: FeedItem, cur: Currency, now: Long) {
    val nav = LocalNav.current
    when (item) {
        is FeedItem.Sent -> {
            val t = item.transfer
            ListRow("To ${t.recipientName}", flat = true, subtitle = "${Text.when_(t.createdAt, now)} · ${t.providerName}",
                leading = { Avatar(t.recipientName.let { Text.initials(it) }, t.recipientColor, 36.dp) },
                trailing = { Txt("−" + Money.format(t.totalMinor, t.fromCur), T.RowTitle.copy(fontFeatureSettings = "tnum")) },
                onClick = { nav.go(if (t.isOpen || t.refundPending) Routes.track(t.id) else Routes.detail(t.id)) })
        }
        is FeedItem.Entry -> {
            val e = item.entry
            val mine = if (e.currency == cur.code) e.amountMinor else e.counterMinor
            ListRow(e.title, flat = true, subtitle = "${Text.when_(e.createdAt, now)} · ${if (e.type == LedgerType.CONVERSION) "Conversion" else e.subtitle}",
                leading = { Tile(if (e.type == LedgerType.CONVERSION) Icons.Swap else if (e.type == LedgerType.REWARD) Icons.Gift else Icons.Plus,
                    if (e.type == LedgerType.CONVERSION) TileStyle.Info else TileStyle.Lime, 36.dp, round = true, iconSize = 18.dp) },
                trailing = { Txt(Money.signed(mine, cur), T.RowTitle.copy(fontFeatureSettings = "tnum"), color = if (mine > 0) C.Ok else C.Ink) })
        }
    }
}

// ------------------------------------------------------------------ 8.03 Add money

@Composable
fun AddMoneyScreen(code: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    var cur by rememberSaveable { mutableStateOf(Currency.of(code).takeIf { a.wallet(it) != null } ?: Currency.GBP) }
    var text by rememberSaveable { mutableStateOf("500") }
    val methods = a.activeMethods.sortedBy { if (it.kind == MethodKind.BANK) 0 else 1 }
    var source by rememberSaveable { mutableStateOf(methods.firstOrNull { it.isDefault }?.id ?: methods.firstOrNull()?.id ?: MANUAL) }
    var curPicker by remember { mutableStateOf(false) }
    var manualSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val w = a.wallet(cur)
    val minor = Money.parse(text, cur) ?: 0L
    val presets = listOf(100L, 250L, 500L, 1000L).map { it * cur.factor }
    val problem = when {
        minor in 1..99 -> "Add at least ${Money.format(100, cur)}."
        minor > 1_000_000 -> "You can add up to ${Money.format(1_000_000, cur, false)} at a time."
        else -> null
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Add money") },
        gap = 20.dp,
        footer = {
            FieldHint(call.error.value)
            if (source == MANUAL) ArroButton("Show my account details", { manualSheet = true })
            else ArroButton("Add ${Money.format(minor, cur)}", {
                scope.launchCall(call) {
                    b.topUp(cur, minor, source)
                    banner.show("${Money.format(minor, cur)} added", "It's in your ${cur.code} balance now.", BannerKind.Success)
                    nav.back()
                }
            }, enabled = minor >= 100 && problem == null, loading = call.busy.value)
        },
        overlay = {
            PickerSheet(curPicker, "Add money to", a.wallets.map { "${it.currency} account" }, "${cur.code} account", { pick ->
                cur = Currency.of(pick.take(3)); text = cleanAmount(text, cur.decimals)
            }, { curPicker = false }, subtitles = a.wallets.associate { "${it.currency} account" to "Balance ${Money.format(it.balanceMinor, it.cur)}" },
                leading = { pick -> Flag(Currency.of(pick.take(3)).flag) })
            Sheet(manualSheet, { manualSheet = false }) {
                SheetHeader("Pay in by bank transfer", { manualSheet = false }, "Send ${cur.code} from any account in your name. It usually arrives within 2 hours.")
                val det = if (w != null) accountDetails(a, w) else emptyList()
                Panel(Modifier.padding(top = 16.dp), gap = 10.dp) { det.forEach { (k, v) -> KV(k, v) } }
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroButton("Copy details", {
                        Share.copy(context, "${cur.code} account details", det.joinToString("\n") { "${it.first}: ${it.second}" })
                        banner.show("Account details copied", kind = BannerKind.Success); manualSheet = false
                    }, icon = Icons.Copy)
                    ArroButton("Share details", { Share.text(context, det.joinToString("\n") { "${it.first}: ${it.second}" }, "My Arro ${cur.code} account") }, style = Btn.Line)
                }
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).border(1.5.dp, C.Line, RoundedCornerShape(999.dp)).clickable { curPicker = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Flag(cur.flag, 18.dp); Txt("To ${cur.code} account", T.Chip); Ic(Icons.ChevronDown, size = 16.dp, tint = C.Slate)
            }
            AmountInput(text, { text = it }, cur)
            Cap(problem ?: "Balance after: ${Money.format((w?.balanceMinor ?: 0) + minor, cur)}", color = if (problem != null) C.Err else C.Slate)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            presets.forEach { p -> Chip(Money.format(p, cur, false), minor == p, { text = Money.number(p, cur, false).replace(",", "") }) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("Pay from")
            methods.forEach { m ->
                ListRow(m.label, selected = source == m.id, subtitle = if (m.kind == MethodKind.BANK) "Pay by bank · instant · free" else if (m.credit) "Instant · 1.75% card fee waived for top-ups" else "Instant · free",
                    leading = { Tile(if (m.kind == MethodKind.BANK) Icons.Bank else Icons.CreditCard, if (m.kind == MethodKind.BANK) TileStyle.Tint else TileStyle.Info) },
                    trailing = { Radio(source == m.id) }, onClick = { source = m.id })
            }
            if (methods.isEmpty()) ListRow("Add a card or bank", dashed = true, subtitle = "Pay in instantly from your own account",
                leading = { Tile(Icons.Plus, round = true) }, trailing = { Chevron() }, onClick = { nav.go(Routes.PAY_METHODS) })
            ListRow("Manual bank transfer", selected = source == MANUAL, subtitle = "Use your ${if (cur == Currency.EUR) "IBAN" else "sort code and account number"}",
                leading = { Tile(Icons.Statement) }, trailing = { Radio(source == MANUAL) }, onClick = { source = MANUAL })
        }
    }
}

private const val MANUAL = "manual"

// ------------------------------------------------------------------ 8.04 Convert

@Composable
fun ConvertScreen(fromCode: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val held = a.wallets.map { it.cur }
    var from by rememberSaveable { mutableStateOf(Currency.of(fromCode).takeIf { it in held } ?: Currency.GBP) }
    var to by rememberSaveable { mutableStateOf(held.firstOrNull { it != from } ?: Currency.EUR) }
    var text by rememberSaveable { mutableStateOf("100") }
    var picker by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    if (held.size < 2) {
        ArroScreen(topBar = { TopBar(onBack = { nav.back() }, title = "Convert") }) {
            Box(Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) { Illustration(Art.Globe, 180.dp) }
            ScreenTitle("Open a second balance", "To convert, you need a balance in another currency. Euro and US dollar balances are free to open.", align = TextAlign.Center)
            ArroButton("Open a balance", { nav.go(Routes.WALLET) })
        }
        return
    }
    val wf = a.wallet(from)
    val wt = a.wallet(to)
    val minor = Money.parse(text, from) ?: 0L
    val rate = b.conversionRate(from, to)
    val mid = b.midRate(to, from)
    val out = Money.convert(minor, from, to, rate)
    val short = wf != null && minor > wf.balanceMinor
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Convert") },
        footer = {
            FieldHint(call.error.value)
            ArroButton("Convert ${Money.format(minor, from)}", {
                scope.launchCall(call) {
                    b.convert(from, to, minor)
                    banner.show("Converted", "${Money.format(minor, from)} is now ${Money.format(out, to)} in your ${to.code} balance.", BannerKind.Success)
                    nav.back()
                }
            }, enabled = minor > 0 && !short && from != to, loading = call.busy.value)
        },
        overlay = {
            val p = picker
            PickerSheet(p != null, if (p == "from") "Convert from" else "Convert to", held.filter { if (p == "from") true else it != from }.map { it.code },
                if (p == "from") from.code else to.code, { code ->
                    val cur = Currency.of(code)
                    if (p == "from") { if (cur == to) to = from; from = cur; text = cleanAmount(text, cur.decimals) } else to = cur
                }, { picker = null }, subtitles = a.wallets.associate { it.currency to "${it.cur.displayName} · ${Money.format(it.balanceMinor, it.cur)}" },
                leading = { code -> Flag(Currency.of(code).flag) })
        },
    ) {
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArroCard(padding = 16.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FieldLabel("From"); Cap("Balance ${Money.format(wf?.balanceMinor ?: 0, from)}", color = if (short) C.Err else C.Slate)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { AmountInput(text, { text = it }, from, size = 34f, align = TextAlign.Start, autoFocus = true) }
                        CurrencyChip(from) { picker = "from" }
                    }
                }
                ArroCard(padding = 16.dp, border = C.Mist, background = C.Tint) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FieldLabel("To"); Cap("Balance ${Money.format(wt?.balanceMinor ?: 0, to)}")
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(Money.number(out, to), T.AmountMd, Modifier.weight(1f), maxLines = 1)
                        CurrencyChip(to) { picker = "to" }
                    }
                }
            }
            Box(
                Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(C.Ink, CircleShape).border(4.dp, C.White, CircleShape)
                    .clickable(role = Role.Button) { val f = from; from = to; to = f; text = cleanAmount(text, from.decimals) },
                contentAlignment = Alignment.Center,
            ) { Ic(Icons.SwapVertical, size = 16.dp, tint = C.Lime, contentDescription = "Swap currencies") }
        }
        if (short) FieldHint("You have ${Money.format(wf?.balanceMinor ?: 0, from)} in ${from.code}. Add money or enter a smaller amount.")
        Panel(gap = 10.dp) {
            KV("Our rate", Money.rateLine(from, to, rate))
            KV("Mid-market rate", Money.rateLine(from, to, mid))
            KV("Fee", "None · 0.27% in rate", valueColor = C.Ok)
            KV("New ${to.code} balance", Money.format((wt?.balanceMinor ?: 0) + out, to), strong = true)
        }
        if (from == Currency.GBP) ListRow("Convert automatically at a better rate", minHeight = 56.dp, titleStyle = T.RowTitle.copy(fontSize = 14.sp),
            subtitle = "Set a target and we'll tell you when it's reached",
            leading = { Tile(Icons.Bell, TileStyle.Lime, 36.dp, iconSize = 16.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.rateAlert(to.code)) })
    }
}

@Composable
private fun CurrencyChip(cur: Currency, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(999.dp)).background(C.White, RoundedCornerShape(999.dp)).border(1.5.dp, C.Line, RoundedCornerShape(999.dp))
            .clickable(role = Role.DropdownList, onClick = onClick).padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Flag(cur.flag, 26.dp); Txt(cur.code, T.Chip.copy(fontWeight = FontWeight.SemiBold)); Ic(Icons.ChevronDown, size = 16.dp, tint = C.Slate)
    }
}

// ------------------------------------------------------------------ 8.08 Statements

@Composable
fun StatementsScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val today = Text.localDate(b.now)
    var cur by remember { mutableStateOf(c.session.walletCurrency.takeIf { a.wallet(it) != null } ?: Currency.GBP) }
    var period by rememberSaveable { mutableStateOf(1) }
    var customFrom by rememberSaveable { mutableStateOf(today.minusMonths(2).withDayOfMonth(1).toString()) }
    var customTo by rememberSaveable { mutableStateOf(today.toString()) }
    var pdf by rememberSaveable { mutableStateOf(true) }
    var accountPicker by remember { mutableStateOf(false) }
    var customSheet by remember { mutableStateOf(false) }
    var monthPicker by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val lastMonth = today.minusMonths(1)
    val (fromDate, toDate) = when (period) {
        0 -> today.withDayOfMonth(1) to today
        1 -> lastMonth.withDayOfMonth(1) to lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
        else -> LocalDate.parse(customFrom) to LocalDate.parse(customTo)
    }
    val fromMs = Text.startOfDay(fromDate)
    val toMs = Text.startOfDay(toDate.plusDays(1)) - 1
    fun rangeLabel(f: LocalDate, t: LocalDate) = "${f.dayOfMonth} ${f.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.UK)} to ${Text.date(Text.startOfDay(t))}"
    fun produce(cur: Currency, from: Long, to: Long, asPdf: Boolean, record: Boolean, share: Boolean) {
        scope.launchCall(call) {
            val lines = Documents.statementLines(b, a, cur, from, to)
            val file = withContext(Dispatchers.IO) {
                if (asPdf) Documents.statementPdf(context, a, cur, from, to, lines) else Documents.statementCsv(context, a, cur, from, to, lines)
            }
            val mime = if (asPdf) "application/pdf" else "text/csv"
            if (record) b.recordStatement(cur, Text.localDate(from).toString(), Text.localDate(to).toString(), if (asPdf) "PDF" else "CSV", lines.size)
            if (share) Share.file(context, file, mime, file.name)
            else banner.show("Statement ready", Share.saveToDownloads(context, file, mime) + if (lines.isEmpty()) ". No transactions in this period." else "", BannerKind.Success)
        }
    }
    val months = (0..12).map { today.minusMonths(it.toLong()).withDayOfMonth(1) }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        overlay = {
            PickerSheet(accountPicker, "Statement for", a.wallets.map { it.currency }, cur.code, { cur = Currency.of(it) }, { accountPicker = false },
                subtitles = a.wallets.associate { it.currency to "${it.cur.displayName} · •• ${it.last4}" }, leading = { code -> Flag(Currency.of(code).flag) })
            Sheet(customSheet, { customSheet = false }) {
                SheetHeader("Custom range", { customSheet = false }, "Choose the months to include. Statements cover up to 12 months.")
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectField("From the start of", Text.monthYear(Text.startOfDay(LocalDate.parse(customFrom))), { monthPicker = "from" })
                    SelectField("To the end of", Text.monthYear(Text.startOfDay(LocalDate.parse(customTo))), { monthPicker = "to" })
                    ArroButton("Use this range", { period = 2; customSheet = false })
                }
            }
            val mp = monthPicker
            PickerSheet(mp != null, if (mp == "from") "Start month" else "End month", months.map { Text.monthYear(Text.startOfDay(it)) },
                if (mp == "from") Text.monthYear(Text.startOfDay(LocalDate.parse(customFrom))) else Text.monthYear(Text.startOfDay(LocalDate.parse(customTo))), { label ->
                    val m = months.first { Text.monthYear(Text.startOfDay(it)) == label }
                    if (mp == "from") {
                        customFrom = m.toString()
                        if (LocalDate.parse(customTo) < m) customTo = m.withDayOfMonth(m.lengthOfMonth()).coerceAtMost(today).toString()
                    } else {
                        val end = m.withDayOfMonth(m.lengthOfMonth()).let { if (it > today) today else it }
                        customTo = end.toString()
                        if (LocalDate.parse(customFrom) > end) customFrom = m.toString()
                    }
                }, { monthPicker = null })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenTitle("Statements", "Proof of every transfer, for you, your bank or your accountant.", Modifier.weight(1f))
            Illustration(Art.Doc, 76.dp)
        }
        val w = a.wallet(cur)
        SelectField("Account", "${cur.code} account •• ${w?.last4 ?: ""}", { accountPicker = true }, leading = { Flag(cur.flag) })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Period")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("This month", period == 0, { period = 0 })
                Chip(Text.monthName(Text.startOfDay(lastMonth)), period == 1, { period = 1 })
                Chip("Custom range", period == 2, { customSheet = true })
            }
            Cap(rangeLabel(fromDate, toDate), size = 12.5f)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Format")
            Segmented(listOf("PDF", "CSV"), if (pdf) 0 else 1, { pdf = it == 0 })
        }
        ArroButton("Download statement", { produce(cur, fromMs, toMs, pdf, record = true, share = false) }, loading = call.busy.value, icon = Icons.Download)
        FieldHint(call.error.value)
        if (a.statements.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Over("Recent")
            Group {
                a.statements.take(6).forEachIndexed { i, s ->
                    if (i > 0) GroupDivider()
                    val f = LocalDate.parse(s.from)
                    val t = LocalDate.parse(s.to)
                    val wholeMonth = f.dayOfMonth == 1 && t == f.withDayOfMonth(f.lengthOfMonth())
                    val title = if (wholeMonth) Text.monthYear(Text.startOfDay(f)) else rangeLabel(f, t)
                    ListRow(title, flat = true, minHeight = 56.dp, subtitle = "${s.currency} · ${s.format} · ${s.count} ${if (s.count == 1) "transaction" else "transactions"}",
                        leading = { Tile(Icons.Document, TileStyle.Warn, 36.dp, iconSize = 18.dp) },
                        trailing = { Ic(Icons.Share, tint = C.Ink, contentDescription = "Share") },
                        onClick = { produce(Currency.of(s.currency), Text.startOfDay(f), Text.startOfDay(t.plusDays(1)) - 1, s.format == "PDF", record = false, share = true) })
                }
            }
        }
    }
}
