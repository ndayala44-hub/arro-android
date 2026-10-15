package app.arro.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.arro.core.Account
import app.arro.core.Currency
import app.arro.core.FeedItem
import app.arro.core.Insights
import app.arro.core.KycStatus
import app.arro.core.LedgerType
import app.arro.core.MethodStatus
import app.arro.core.Money
import app.arro.core.NoticeKind
import app.arro.core.Rates
import app.arro.core.Text
import app.arro.core.Transfer
import app.arro.core.TransferStatus
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.appState
import app.arro.ui.back
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.nowTicker
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.BalanceCard
import app.arro.ui.components.BigAmount
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.CheckBox
import app.arro.ui.components.Chevron
import app.arro.ui.components.FiveGrid
import app.arro.ui.components.Flag
import app.arro.ui.components.FlagPair
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.ListRow
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.PersonChip
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.ProgressBar
import app.arro.ui.components.QuickActions
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SectionHeader
import app.arro.ui.components.Segmented
import app.arro.ui.components.Tab
import app.arro.ui.components.TabScreen
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.TopBar
import app.arro.ui.components.TrailingValue
import app.arro.ui.components.Txt
import app.arro.ui.components.tap
import app.arro.ui.screens.send.startSend
import app.arro.ui.screens.system.HomeSkeleton
import app.arro.ui.screens.system.OfflineState
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ 3.01 Home and 3.02 first run

@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val online by c.connectivity.online.collectAsStateWithLifecycle()
    var loaded by rememberSaveable { mutableStateOf(false) }
    var showCached by remember { mutableStateOf(false) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    val now = nowTicker(30_000)
    LaunchedEffect(Unit) { if (!loaded) { delay(650); loaded = true } }

    // Ask for notification permission once, so transfer updates can reach the user.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(loaded) {
        if (loaded && !c.session.askedNotifications && Build.VERSION.SDK_INT >= 33 && !c.notifier.permitted()) {
            c.session.askedNotifications = true
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val firstRun = a.transfers.isEmpty() && !a.setupDismissed
    TabScreen(Tab.Home) {
        when {
            !loaded -> HomeSkeleton()
            !online && !showCached -> OfflineState(Money.format(b.totalInGbp(a), Currency.GBP), { c.connectivity.recheck() }, { showCached = true })
            else -> {
                HomeHeader(a, firstRun)
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Cap("Total balance")
                        if (!firstRun) Box(Modifier.clip(CircleShape).clickable { hidden = !hidden }.padding(2.dp)) {
                            Ic(Icons.Eye, size = 16.dp, tint = C.Slate, contentDescription = if (hidden) "Show balance" else "Hide balance")
                        }
                    }
                    if (hidden) Txt("£••••", T.Amount, Modifier.padding(top = 6.dp))
                    else BigAmount(b.totalInGbp(a), Currency.GBP, Modifier.padding(top = 6.dp))
                    if (!firstRun) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("${a.wallets.size} currenc${if (a.wallets.size == 1) "y" else "ies"}", PillKind.Neutral)
                        when (a.kyc.status) {
                            KycStatus.APPROVED -> Pill("Verified", PillKind.Ok, dot = true)
                            KycStatus.IN_REVIEW -> Pill("Checking your ID", PillKind.Info, dot = true, modifier = Modifier.tap { nav.go(Routes.KYC_STATUS) })
                            else -> Pill("Verify your identity", PillKind.Warn, dot = true, modifier = Modifier.tap { nav.go(Routes.KYC) })
                        }
                    }
                }
                QuickActions(
                    listOf(
                        Triple("Send", Icons.ArrowUpRight) { startSend(c, nav, banner) },
                        Triple("Add money", Icons.Plus) { nav.go(Routes.addMoney()) },
                        Triple("Convert", Icons.Swap) { nav.go(Routes.convert()) },
                        Triple("Rates", Icons.Trending) { nav.go(Routes.RATES) },
                    ),
                    Modifier.padding(start = 20.dp, end = 20.dp, top = if (firstRun) 22.dp else 24.dp),
                )
                if (firstRun) FirstRunBody(a) else DashboardBody(a, hidden, now)
            }
        }
    }
}

@Composable
private fun HomeHeader(a: Account, firstRun: Boolean) {
    val nav = LocalNav.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).tap { nav.go(Routes.ACCOUNT) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(a.initials, 0)
            Column {
                Cap(if (firstRun) "Welcome to Arro" else Text.greeting(System.currentTimeMillis()), size = 12f)
                Txt(a.displayName, T.H3, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!firstRun) RoundIcon(Icons.Search, { nav.go(Routes.HELP) }, style = IconBtn.Soft, description = "Search and help")
            RoundIcon(Icons.Bell, { nav.go(Routes.NOTIFICATIONS) }, style = IconBtn.Soft, badge = a.unreadCount > 0,
                description = if (a.unreadCount > 0) "Notifications, ${a.unreadCount} unread" else "Notifications")
        }
    }
}

@Composable
private fun DashboardBody(a: Account, hidden: Boolean, now: Long) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    // balance cards
    Row(Modifier.padding(top = 24.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        a.wallets.forEach { w ->
            BalanceCard(w.cur, if (hidden) 0 else w.balanceMinor, detail = if (w.main) "•• ${w.last4}" else null) { nav.go(Routes.walletAccount(w.currency)) }
        }
        Box(
            Modifier.width(120.dp).height(120.dp).clip(RoundedCornerShape(22.dp)).border(1.5.dp, C.Line, RoundedCornerShape(22.dp))
                .clickable(role = Role.Button) { nav.go(Routes.WALLET) }.padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(Icons.Plus, TileStyle.Tint, size = 36.dp, round = true)
                Txt("Open another currency", T.Cap.copy(fontWeight = FontWeight.Medium), color = C.Ink)
            }
        }
    }
    // send again
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
        SectionHeader("Send again", "See all", { nav.go(Routes.RECIPIENTS) })
        val recents = Insights.recentRecipients(a).take(4)
        FiveGrid(listOf<@Composable () -> Unit>({ PersonChip("Add", "", 0, null, { c.session.recipient.reset(forSend = false); nav.go(Routes.RCP_ADD) }, add = true) }) +
            recents.map<app.arro.core.Recipient, @Composable () -> Unit> { r -> { PersonChip(r.firstName, r.initials, r.color, app.arro.core.Catalog.countryOrNull(r.country)?.currency?.flag ?: "gb", { startSend(c, nav, banner, recipient = r) }) } })
    }
    // this month
    val months = Insights.months(a, now)
    val thisMonth = months.last()
    val previous = months[months.size - 2]
    val people = a.transfers.filter { it.createdAt >= thisMonth.start && it.status != TransferStatus.CANCELLED }.map { it.recipientId }.distinct().size
    Panel(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp), PanelKind.Dark, gap = 12.dp, onClick = { nav.go(Routes.INSIGHTS) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Over("${Text.monthName(now)} so far", color = C.Lime)
            Ic(Icons.ChevronRight, tint = C.OnNight)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Txt(Money.format(thisMonth.totalMinor, Currency.GBP), T.Big28, color = C.OnNight)
                Cap("sent to $people ${if (people == 1) "person" else "people"}", Modifier.padding(top = 2.dp), color = C.DarkCap)
                val diff = thisMonth.totalMinor - previous.totalMinor
                Pill(
                    when {
                        previous.totalMinor == 0L -> "First month with Arro"
                        diff < 0 -> "${Money.format(-diff, Currency.GBP, false)} less than ${Text.monthName(previous.start)}"
                        diff > 0 -> "${Money.format(diff, Currency.GBP, false)} more than ${Text.monthName(previous.start)}"
                        else -> "Same as ${Text.monthName(previous.start)}"
                    },
                    PillKind.Lime, modifier = Modifier.padding(top = 12.dp),
                )
            }
            MonthBars(months.map { it.totalMinor }, months.first().label, months.last().label)
        }
    }
    // rates
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
        SectionHeader("Today's rates", "All rates", { nav.go(Routes.RATES) })
        Group {
            listOf(Currency.RWF, Currency.KES, Currency.NGN).forEachIndexed { i, cur ->
                if (i > 0) GroupDivider()
                val rate = Rates.customer(state.rates, Currency.GBP, cur)
                val ch = Rates.change24h(state.rates, cur, now)
                ListRow("GBP → ${cur.code}", flat = true, minHeight = 56.dp, onClick = { nav.go(Routes.rate(cur.code)) },
                    leading = { FlagPair("gb", cur.flag) },
                    trailing = { TrailingValue(Money.rate(rate), "${if (ch >= 0) "▲" else "▼"} ${Money.percent(kotlin.math.abs(ch))}", if (ch >= 0) C.Ok else C.Err) })
            }
        }
    }
    // recent
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
        SectionHeader("Recent transfers", "See all", { nav.go(Routes.ACTIVITY) })
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            b.feed(a).take(4).forEach { item -> FeedRow(item, now) }
        }
    }
}

/** Small six-month bar chart from the home insights panel. */
@Composable
private fun MonthBars(values: List<Long>, firstLabel: String, lastLabel: String) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.size(132.dp, 78.dp)) {
        val bw = 16.dp.toPx(); val gap = 6.dp.toPx(); val maxH = 64.dp.toPx(); val base = 70.dp.toPx()
        values.forEachIndexed { i, v ->
            val h = (v.toFloat() / max * maxH).coerceAtLeast(4.dp.toPx())
            drawRoundRect(if (i == values.lastIndex) C.Lime else C.ForestFixed, Offset(2.dp.toPx() + i * (bw + gap), base - h), Size(bw, h), CornerRadius(4.dp.toPx()))
        }
        val paint = android.graphics.Paint().apply { color = C.DarkCap.toArgb(); textSize = 8.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
        drawContext.canvas.nativeCanvas.drawText(firstLabel, 2.dp.toPx() + bw / 2, size.height, paint)
        drawContext.canvas.nativeCanvas.drawText(lastLabel, 2.dp.toPx() + values.lastIndex * (bw + gap) + bw / 2, size.height, paint)
    }
}

/** A transfer or top-up row (.row with avatar or tile). */
@Composable
fun FeedRow(item: FeedItem, now: Long, showFromAmount: Boolean = false) {
    val nav = LocalNav.current
    when (item) {
        is FeedItem.Sent -> {
            val t = item.transfer
            val (label, color) = statusLabel(t)
            ListRow(
                t.recipientName,
                subtitle = if (showFromAmount) "${Text.when_(t.createdAt, now)} · −${Money.format(t.totalMinor, t.fromCur)}" else "${t.providerName} · ${Text.when_(t.createdAt, now)}",
                leading = { Avatar(app.arro.core.Text.initials(t.recipientName), t.recipientColor, flag = t.toCur.flag.takeIf { t.to != "GBP" } ?: "gb") },
                trailing = { TrailingValue(Money.format(t.receiveMinor, t.toCur), label, color) },
                onClick = { nav.go(if (t.isOpen || t.refundPending) Routes.track(t.id) else Routes.detail(t.id)) },
            )
        }
        is FeedItem.Entry -> {
            val e = item.entry
            val cur = Currency.of(e.currency)
            when (e.type) {
                LedgerType.CONVERSION -> ListRow(
                    e.title, subtitle = "${Text.when_(e.createdAt, now)} · ${Money.format(e.amountMinor, cur)}",
                    leading = { Tile(Icons.Swap, TileStyle.Info, size = 44.dp, round = true) },
                    trailing = { TrailingValue("+" + Money.format(e.counterMinor, Currency.of(e.counterCurrency)), "Converted", C.Ok) },
                    onClick = { nav.go(Routes.walletAccount(e.counterCurrency)) },
                )
                else -> ListRow(
                    e.title, subtitle = "${e.subtitle} · ${Text.when_(e.createdAt, now)}",
                    leading = { Tile(if (e.type == LedgerType.REWARD) Icons.Gift else Icons.Plus, TileStyle.Lime, size = 44.dp, round = true) },
                    trailing = { TrailingValue(Money.signed(e.amountMinor, cur), e.currency, valueColor = C.Ok) },
                    onClick = { nav.go(Routes.walletAccount(e.currency)) },
                )
            }
        }
    }
}

/** Status text and colour for a transfer, as shown under amounts. */
fun statusLabel(t: Transfer): Pair<String, Color> = when (t.status) {
    TransferStatus.PROCESSING, TransferStatus.IN_PROGRESS -> "In progress" to C.Info
    TransferStatus.ON_HOLD -> "Action needed" to C.Warn
    TransferStatus.DELIVERED -> (if (t.payoutType == app.arro.core.PayoutType.CASH) "Ready to collect" else "Delivered") to C.Ok
    TransferStatus.FAILED -> "Failed" to C.Err
    TransferStatus.CANCELLED -> "Cancelled" to C.Slate
    TransferStatus.REFUNDED -> "Refunded" to C.Slate
}

@Composable
private fun FirstRunBody(a: Account) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val verified = a.kyc.status == KycStatus.APPROVED
    val hasMethod = a.paymentMethods.any { it.status == MethodStatus.ACTIVE }
    val done = listOf(true, verified, hasMethod, a.transfers.isNotEmpty())
    ArroCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Txt("Get set up", T.H2)
            Txt("${done.count { it }} of 4", T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink)
        }
        ProgressBar(done.count { it } / 4f, Modifier.padding(top = 14.dp), fill = C.LimeDeep)
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupItem("Create your account", true) {}
            SetupItem(if (a.kyc.status == KycStatus.IN_REVIEW) "Verify your identity · checking" else "Verify your identity", verified) {
                nav.go(if (a.kyc.status == KycStatus.NOT_STARTED || a.kyc.status == KycStatus.REJECTED) Routes.KYC else Routes.KYC_STATUS)
            }
            SetupItem("Add a card or bank", hasMethod) { nav.go(Routes.PAY_METHODS) }
            SetupItem("Send your first transfer", a.transfers.isNotEmpty()) { startSend(c, nav, banner) }
        }
    }
    if (a.firstTransferFree) Panel(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), PanelKind.Lime) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.Gift, 52.dp)
            Column(Modifier.weight(1f)) {
                Txt("Your first transfer has no Arro fee", T.H3)
                Cap("Applies to any amount up to £1,000.", Modifier.padding(top = 2.dp))
            }
        }
    }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
        Txt("Recent transfers", T.H2)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.Plane, 52.dp)
            Cap("No transfers yet. Your first one will show up here with live tracking.", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SetupItem(text: String, done: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = !done, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckBox(done)
        if (done) Txt(text, T.Cap.copy(textDecoration = TextDecoration.LineThrough), Modifier.weight(1f), color = C.Slate)
        else {
            Txt(text, T.RowTitle, Modifier.weight(1f))
            Chevron()
        }
    }
}

// ------------------------------------------------------------------ 3.03 Notifications

@Composable
fun NotificationsScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val state by appState()
    val a = state.account ?: return
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val now = nowTicker(30_000)
    val items = a.notifications.filter {
        when (filter) {
            1 -> it.kind == NoticeKind.TRANSFER || it.kind == NoticeKind.RATE
            2 -> it.kind == NoticeKind.ACCOUNT || it.kind == NoticeKind.SECURITY || it.kind == NoticeKind.STATEMENT
            else -> true
        }
    }.sortedByDescending { it.createdAt }
    ArroScreen(
        topBar = {
            TopBar(onBack = { nav.back() }, right = {
                app.arro.ui.components.ArroButton("Mark all read", { b.markAllRead() }, style = Btn.Text, size = BtnSize.S, fill = false, enabled = a.unreadCount > 0)
            })
        },
    ) {
        ScreenTitle("Notifications")
        Segmented(listOf("All", "Transfers", "Account"), filter, { filter = it })
        if (items.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Illustration(Art.Bell, 128.dp)
                Txt("You're all caught up", T.H2)
                Cap("Transfer updates, rate alerts and security messages will appear here.")
            }
        }
        val (today, earlier) = items.partition { Text.isSameDay(it.createdAt, now) }
        listOf("Today" to today, "Earlier" to earlier).forEach { (label, list) ->
            if (list.isNotEmpty()) {
                Over(label, Modifier.padding(top = 4.dp))
                list.forEach { n ->
                    val (icon, style) = when (n.kind) {
                        NoticeKind.RATE -> Icons.Trending to TileStyle.Lime
                        NoticeKind.SECURITY -> Icons.Smartphone to TileStyle.Info
                        NoticeKind.STATEMENT -> Icons.Statement to TileStyle.Warn
                        NoticeKind.TRANSFER -> Icons.Check to TileStyle.Tint
                        NoticeKind.ACCOUNT -> Icons.ShieldCheck to TileStyle.Tint
                    }
                    Box(
                        Modifier.fillMaxWidth().then(
                            if (!n.read) Modifier.background(C.Tint, RoundedCornerShape(16.dp)).border(1.5.dp, C.Mist, RoundedCornerShape(16.dp)) else Modifier,
                        ),
                    ) {
                        ListRow(
                            n.title, subtitle = n.body, verticalAlignment = Alignment.Top, flat = !n.read,
                            leading = { Tile(icon, style) },
                            trailing = {
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Cap(Text.when_(n.createdAt, now), size = 12f)
                                    if (!n.read) Box(Modifier.size(8.dp).background(C.Badge, CircleShape))
                                }
                            },
                            onClick = {
                                b.markRead(n.id)
                                nav.go(
                                    when (n.kind) {
                                        NoticeKind.TRANSFER -> a.transfer(n.transferId)?.let { t -> if (t.isOpen || t.refundPending) Routes.track(t.id) else Routes.detail(t.id) } ?: Routes.ACTIVITY
                                        NoticeKind.RATE -> Routes.RATES
                                        NoticeKind.SECURITY -> Routes.SECURITY
                                        NoticeKind.STATEMENT -> Routes.STATEMENTS
                                        NoticeKind.ACCOUNT -> if (n.title.startsWith("Case")) Routes.HELP else Routes.VERIFICATION
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 3.04 Insights

@Composable
fun InsightsScreen() {
    val nav = LocalNav.current
    val state by appState()
    val a = state.account ?: return
    val now = nowTicker(60_000)
    var period by rememberSaveable { mutableIntStateOf(0) }
    val p = Insights.Period.entries[period]
    val s = Insights.summary(a, now, p)
    val bars: List<Pair<String, Long>> = when (p) {
        Insights.Period.MONTH -> Insights.months(a, now).map { it.label.take(1) to it.totalMinor }
        Insights.Period.QUARTER -> (3 downTo 0).map { back ->
            val start = Text.localDate(now).withDayOfMonth(1).minusMonths(((Text.localDate(now).monthValue - 1) % 3 + back * 3).toLong())
            val from = Text.startOfDay(start); val to = Text.startOfDay(start.plusMonths(3))
            "Q${(start.monthValue - 1) / 3 + 1}" to a.transfers.filter { it.createdAt in from until to && it.status != TransferStatus.CANCELLED && it.status != TransferStatus.REFUNDED }.sumOf { it.sendMinor }
        }
        Insights.Period.YEAR -> (2 downTo 0).map { back ->
            val y = Text.localDate(now).year - back
            val from = Text.startOfDay(java.time.LocalDate.of(y, 1, 1)); val to = Text.startOfDay(java.time.LocalDate.of(y + 1, 1, 1))
            "$y" to a.transfers.filter { it.createdAt in from until to && it.status != TransferStatus.CANCELLED && it.status != TransferStatus.REFUNDED }.sumOf { it.sendMinor }
        }
    }
    val periodWord = when (p) { Insights.Period.MONTH -> Text.monthName(now); Insights.Period.QUARTER -> "this quarter"; Insights.Period.YEAR -> "${Text.localDate(now).year}" }
    val prevWord = when (p) {
        Insights.Period.MONTH -> Text.monthName(Text.startOfDay(Text.localDate(now).withDayOfMonth(1).minusMonths(1)))
        Insights.Period.QUARTER -> "last quarter"
        Insights.Period.YEAR -> "${Text.localDate(now).year - 1}"
    }
    val diff = s.totalMinor - s.previousMinor
    val sentence = when {
        s.totalMinor == 0L -> "You haven't sent anything in ${if (p == Insights.Period.MONTH) periodWord else periodWord} yet."
        s.previousMinor == 0L -> "You've sent ${Money.format(s.totalMinor, Currency.GBP, false)} so far, with ${s.daysLeft} days to go."
        diff < 0 -> "You've sent ${Money.format(-diff, Currency.GBP, false)} less than in $prevWord, with ${s.daysLeft} days to go."
        diff > 0 -> "You've sent ${Money.format(diff, Currency.GBP, false)} more than in $prevWord, with ${s.daysLeft} days to go."
        else -> "You've sent the same as in $prevWord, with ${s.daysLeft} days to go."
    }
    TabScreen(Tab.Home, top = {
        TopBar(onBack = { nav.back() }, right = { app.arro.ui.components.BarCaption(s.periodLabel) })
    }) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenTitle("Insights")
            Segmented(listOf("Month", "Quarter", "Year"), period, { period = it })
            Panel(kind = PanelKind.Lime) {
                Txt(sentence, T.H2.copy(fontSize = 19.sp))
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        BigAmount(s.totalMinor, Currency.GBP, size = 34.sp)
                        Cap("sent in $periodWord", Modifier.padding(top = 4.dp))
                    }
                    InsightBars(bars)
                }
            }
            if (s.shares.isNotEmpty()) {
                Column {
                    SectionHeader("Where it went")
                    val max = s.shares.maxOf { it.totalMinor }.coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        s.shares.forEach { sh ->
                            val country = app.arro.core.Catalog.countryOrNull(sh.country)
                            Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.go(Routes.recipient(sh.recipientId)) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Flag(country?.currency?.flag ?: "gb", 18.dp)
                                        Txt("${country?.name ?: "UK"} · ${sh.name.split(" ").first()}", T.Body14.copy(fontWeight = FontWeight.Medium))
                                    }
                                    Txt(Money.format(sh.totalMinor, Currency.GBP), T.Num.copy(fontSize = 14.sp))
                                }
                                ProgressBar(sh.totalMinor.toFloat() / max)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArroCard(Modifier.weight(1f)) {
                    Cap("Fees paid")
                    Txt(Money.format(s.feesMinor, Currency.GBP), T.Big22.copy(fontSize = 20.sp), Modifier.padding(top = 4.dp))
                    Cap("across ${s.transfers} transfer${if (s.transfers == 1) "" else "s"}", Modifier.padding(top = 2.dp), size = 12f)
                }
                ArroCard(Modifier.weight(1f)) {
                    Cap("Fastest delivery")
                    val f = s.fastest
                    Txt(if (f != null) Text.duration(f.deliveredAt - f.createdAt) else "–", T.Big22.copy(fontSize = 20.sp), Modifier.padding(top = 4.dp))
                    Cap(if (f != null) "to ${f.firstName}, ${f.providerName}" else "No deliveries yet", Modifier.padding(top = 2.dp), size = 12f)
                }
            }
        }
    }
}

@Composable
private fun InsightBars(bars: List<Pair<String, Long>>) {
    val max = (bars.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.size(150.dp, 86.dp)) {
        val n = bars.size
        val gap = 7.dp.toPx()
        val bw = ((size.width - 4.dp.toPx() - gap * (n - 1)) / n).coerceAtMost(18.dp.toPx())
        val maxH = 64.dp.toPx(); val base = 70.dp.toPx()
        val paint = android.graphics.Paint().apply { textSize = 9.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
        bars.forEachIndexed { i, (label, v) ->
            val last = i == n - 1
            val h = (v.toFloat() / max * maxH).coerceAtLeast(4.dp.toPx())
            val x = 2.dp.toPx() + i * (bw + gap)
            drawRoundRect(if (last) C.Ink else if (C.dark) Color(0xFF2E5A2A) else Color(0xFFC6EBA6), Offset(x, base - h), Size(bw, h), CornerRadius(5.dp.toPx()))
            paint.color = (if (last) C.Ink else C.Forest).toArgb()
            paint.isFakeBoldText = last
            drawContext.canvas.nativeCanvas.drawText(label, x + bw / 2, size.height - 2.dp.toPx(), paint)
        }
    }
}
