package app.arro.ui.screens.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.Account
import app.arro.core.Agent
import app.arro.core.Catalog
import app.arro.core.CaseStatus
import app.arro.core.ChatMessage
import app.arro.core.HelpArticle
import app.arro.core.Money
import app.arro.core.PayoutType
import app.arro.core.Text
import app.arro.core.Transfer
import app.arro.core.TransferStatus
import app.arro.platform.Documents
import app.arro.platform.Share
import app.arro.platform.rememberPhotoPicker
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
import app.arro.ui.components.Avatar
import app.arro.ui.components.BarCaption
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.CheckRow
import app.arro.ui.components.Chevron
import app.arro.ui.components.Chip
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.HDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.Radio
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SearchField
import app.arro.ui.components.Segmented
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.StepState
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.Timeline
import app.arro.ui.components.TimelineItem
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.screens.home.statusLabel
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Opens the support chat, optionally about one transfer. */
fun openChat(c: app.arro.AppContainer, nav: androidx.navigation.NavHostController, transferId: String?) {
    c.session.chatAbout = transferId ?: c.session.chatAbout
    c.backend.startChatAbout(transferId)
    nav.go(Routes.CHAT)
}

private fun categoryArticles(label: String): List<HelpArticle> =
    Catalog.articles.filter { it.category.startsWith(label.substringBefore(" ")) || it.category == label }

private fun categoryIcon(key: String): Int = when (key) {
    "send" -> Icons.ArrowUpRight
    "percent" -> Icons.Percent
    "id-card" -> Icons.IdCard
    "credit-card" -> Icons.CreditCard
    "shield" -> Icons.Shield
    else -> Icons.Refresh
}

// ------------------------------------------------------------------ 10.01 Help centre

@Composable
fun HelpScreen() {
    val nav = LocalNav.current
    val c = container()
    val state by appState()
    val a = state.account
    val now = nowTicker(30_000)
    var query by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val latest = a?.transfers?.maxByOrNull { it.createdAt }
    val q = query.trim().lowercase()
    val results = if (q.isEmpty()) emptyList() else Catalog.articles.filter { art ->
        (art.title + " " + art.intro + " " + art.bullets.joinToString(" ") + " " + art.facts.joinToString(" ") { it.first + " " + it.second }).lowercase()
            .let { text -> q.split(" ").filter { it.isNotBlank() }.all { it in text } }
    }
    ArroScreen(
        topBar = {
            TopBar(onBack = { nav.back() }, right = {
                if (a != null) ArroButton("Chat", { openChat(c, nav, null) }, style = Btn.Lime, size = BtnSize.S, icon = Icons.Chat, fill = false)
            })
        },
        overlay = {
            val cat = category
            Sheet(cat != null, { category = null }) {
                if (cat != null) {
                    SheetHeader(cat, { category = null })
                    Group(Modifier.padding(top = 14.dp)) {
                        categoryArticles(cat).forEachIndexed { i, art ->
                            if (i > 0) GroupDivider()
                            ListRow(art.title, flat = true, minHeight = 52.dp, subtitle = "${art.minutes} min read", trailing = { Chevron() },
                                onClick = { category = null; nav.go(Routes.article(art.id)) })
                        }
                    }
                }
            }
        },
    ) {
        ScreenTitle("How can we help?")
        SearchField(query, { query = it }, "Search, e.g. \"cancel a transfer\"")
        if (q.isNotEmpty()) {
            if (results.isEmpty()) {
                Note("No articles match \"$query\". Amina can help with anything else.", NoteKind.Info, Icons.Search,
                    action = if (a != null) "Chat with us" else null, onAction = { openChat(c, nav, null) })
            } else Group {
                results.forEachIndexed { i, art ->
                    if (i > 0) GroupDivider()
                    ListRow(art.title, flat = true, minHeight = 52.dp, subtitle = art.category, trailing = { Chevron() }, onClick = { nav.go(Routes.article(art.id)) })
                }
            }
            return@ArroScreen
        }
        if (latest != null) {
            Panel(kind = PanelKind.Dark, padding = 14.dp, onClick = { nav.go(Routes.transferHelp(latest.id)) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(Text.initials(latest.recipientName), latest.recipientColor)
                    Column(Modifier.weight(1f)) {
                        Cap("Your latest transfer", color = C.DarkCap, size = 12f)
                        Txt("${Money.format(latest.receiveMinor, latest.toCur)} to ${latest.firstName}", T.Body.copy(fontWeight = FontWeight.SemiBold), color = C.White, maxLines = 1)
                    }
                    Pill("Get help", PillKind.OnDark)
                }
            }
        }
        val cases = a?.cases.orEmpty()
        if (cases.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Over("Your cases")
            Group {
                cases.take(3).forEachIndexed { i, cs ->
                    if (i > 0) GroupDivider()
                    val st = cs.status(now)
                    ListRow(cs.topic, flat = true, minHeight = 56.dp, subtitle = "Case ${cs.number} · opened ${Text.relativeDay(cs.createdAt, now)}",
                        trailing = { Pill(st.label, caseKind(st), dot = true) }, onClick = { nav.go(Routes.case(cs.id)) })
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Catalog.helpCategories.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { i, (label, icon) ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).border(1.5.dp, C.Line, RoundedCornerShape(18.dp))
                                .clickable {
                                    val arts = categoryArticles(label)
                                    if (arts.size == 1) nav.go(Routes.article(arts.first().id)) else category = label
                                }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Tile(categoryIcon(icon), if (i == 0 && label == "Sending") TileStyle.Lime else TileStyle.Tint, 36.dp, iconSize = 18.dp)
                            Txt(label, T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink, maxLines = 1)
                        }
                    }
                }
            }
        }
        Column {
            Over("Popular questions", Modifier.padding(bottom = 4.dp))
            Catalog.articles.take(3).forEachIndexed { i, art ->
                if (i > 0) HDivider()
                ListRow(art.title, flat = true, minHeight = 48.dp, trailing = { Chevron() }, onClick = { nav.go(Routes.article(art.id)) })
            }
        }
        Panel(gap = 6.dp) {
            Txt("Still need help?", T.H3)
            Cap("Chat with us any time of day. Most questions are answered in a few minutes.")
            if (a != null) ArroButton("Chat with Amina", { openChat(c, nav, null) }, Modifier.padding(top = 6.dp), size = BtnSize.M, icon = Icons.Chat)
        }
    }
}

private fun caseKind(st: CaseStatus) = when (st) { CaseStatus.OPEN -> PillKind.Info; CaseStatus.IN_REVIEW -> PillKind.Warn; CaseStatus.RESOLVED -> PillKind.Ok }

// ------------------------------------------------------------------ 10.02 Article

@Composable
fun ArticleScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val state by appState()
    val art = Catalog.article(id) ?: run { NotFound("Article not found"); return }
    var helpful by rememberSaveable { mutableStateOf<Boolean?>(null) }
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Help · ${art.category}") }) }, gap = 14.dp) {
        Txt(art.title, T.Title.copy(lineHeight = 31.sp))
        Cap("Updated ${art.updated} · ${art.minutes} min read")
        Txt(art.intro, T.Body.copy(lineHeight = 24.sp))
        if (art.facts.isNotEmpty()) Group {
            art.facts.forEachIndexed { i, (k, v) ->
                if (i > 0) GroupDivider()
                val (icon, style) = when {
                    k.startsWith("Mobile") -> Icons.Smartphone to TileStyle.Lime
                    k.startsWith("Cash") -> Icons.CashSimple to TileStyle.Warn
                    k.contains("bank", true) || k.startsWith("Bank") -> Icons.Bank to TileStyle.Tint
                    k.contains("card", true) -> Icons.CreditCard to TileStyle.Info
                    k.contains("balance", true) -> Icons.Wallet to TileStyle.Lime
                    k.contains("rate", true) || k.contains("margin", true) -> Icons.Percent to TileStyle.Tint
                    k.contains("Passcode") || k.contains("device", true) -> Icons.Lock to TileStyle.Tint
                    else -> Icons.Info to TileStyle.Tint
                }
                ListRow(k, flat = true, minHeight = 52.dp, titleStyle = T.RowTitle.copy(fontSize = 14.sp),
                    leading = { Tile(icon, style, 32.dp, iconSize = 16.dp) },
                    trailing = { Txt(v, T.Body14.copy(fontWeight = FontWeight.SemiBold)) })
            }
        }
        if (art.sectionTitle.isNotBlank()) Txt(art.sectionTitle, T.H3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            art.bullets.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).background(C.Ink, CircleShape))
                    Txt(line, T.Body14.copy(lineHeight = 21.sp))
                }
            }
        }
        if (art.body.isNotBlank()) Txt(art.body, T.Body14)
        val action: Pair<String, String>? = when (art.id) {
            "arrive", "hold", "cancel", "refunds" -> "See my transfers" to Routes.ACTIVITY
            "fees" -> "Compare what it costs" to Routes.rateCompare()
            "verify" -> "Verification and limits" to Routes.VERIFICATION
            "cards" -> "Payment methods" to Routes.PAY_METHODS
            "security" -> "Security settings" to Routes.SECURITY
            else -> null
        }
        if (action != null && state.account != null) ArroButton(action.first, { nav.go(action.second) }, style = Btn.Line, size = BtnSize.M)
        ArroCard(Modifier.padding(top = 4.dp), padding = 14.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt(when (helpful) { true -> "Thanks for letting us know"; false -> "Sorry about that"; null -> "Was this helpful?" }, T.Body14.copy(fontWeight = FontWeight.Medium), Modifier.weight(1f))
                if (helpful == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Yes", false, { helpful = true; banner.show("Thanks for the feedback", kind = BannerKind.Success) })
                    Chip("No", false, { helpful = false })
                }
            }
            if (helpful == false && state.account != null) {
                Cap("Amina can answer this for your account in a few minutes.", Modifier.padding(top = 8.dp))
                ArroButton("Chat with us", { openChat(c, nav, null) }, Modifier.padding(top = 10.dp), size = BtnSize.M, icon = Icons.Chat)
            }
        }
    }
}

// ------------------------------------------------------------------ 10.03 Transfer help

@Composable
fun TransferHelpScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(id) ?: run { NotFound("Transfer not found"); return }
    val now = nowTicker()
    val label = statusLabel(t).first
    val kind = when (t.status) {
        TransferStatus.DELIVERED -> PillKind.Ok
        TransferStatus.ON_HOLD -> PillKind.Warn
        TransferStatus.FAILED -> PillKind.Err
        TransferStatus.CANCELLED, TransferStatus.REFUNDED -> PillKind.Neutral
        else -> PillKind.Info
    }
    val name = t.firstName
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }, title = "Transfer help") }) {
        ArroCard(padding = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(Text.initials(t.recipientName), t.recipientColor, flag = t.toCur.flag)
                Column(Modifier.weight(1f)) {
                    Txt(Money.format(t.receiveMinor, t.toCur), T.H3)
                    Cap("To $name · ${Text.relativeDay(t.createdAt, now).replaceFirstChar { it.lowercase() }}", size = 12f)
                }
                Pill(label, kind, dot = true)
            }
        }
        val account = if (t.payoutType == PayoutType.MOBILE_MONEY || t.payoutType == PayoutType.EWALLET) Text.formatPhone(t.payoutAccount) else t.payoutAccount
        when (t.status) {
            TransferStatus.PROCESSING, TransferStatus.IN_PROGRESS -> Note(
                if (now < t.deliveredAt) "On track and expected ${Text.by(t.deliveredAt, now)}. Most questions are answered by the tracking screen before then."
                else "This is taking a little longer than usual. ${t.providerName} is still processing it.", NoteKind.Lime, Icons.Clock,
                action = "Track it", onAction = { nav.go(Routes.track(t.id)) })
            TransferStatus.ON_HOLD -> Note("We need one quick answer from you before this can continue.", NoteKind.Warn, Icons.Alert, action = "Answer now", onAction = { nav.go(Routes.track(t.id)) })
            TransferStatus.DELIVERED -> Note("${t.providerName} confirmed delivery at ${Text.time(t.deliveredAt)} on ${Text.dayMonth(t.deliveredAt)} to $account.", NoteKind.Lime, Icons.Check)
            else -> Note(if (t.refundedAt > 0) "This transfer was refunded in full on ${Text.dayMonth(t.refundedAt)}." else "Your refund of ${Money.format(t.totalMinor, t.fromCur)} is on its way to ${t.fundingLabel}.",
                NoteKind.Info, Icons.Refresh, action = "See refund", onAction = { nav.go(Routes.track(t.id)) })
        }
        ScreenTitle("What's wrong?", size = app.arro.ui.components.TitleSize.S)
        val options = buildList {
            if (t.isOpen) add(Option("It's taking longer than expected") { nav.go(Routes.article("arrive")) })
            if (t.status == TransferStatus.DELIVERED || (t.isOpen && now > t.deliveredAt)) add(Option("$name hasn't received it") { c.session.disputeTopic = 0; nav.go(Routes.dispute(t.id)) })
            if (t.status == TransferStatus.DELIVERED) add(Option("$name got less than I expected") { c.session.disputeTopic = 1; nav.go(Routes.dispute(t.id)) })
            add(Option("I sent it to the wrong ${if (t.payoutType == PayoutType.BANK) "account" else "number"}") {
                openChat(c, nav, t.id)
                b.sendChat("I sent my transfer ${t.reference} to the wrong ${if (t.payoutType == PayoutType.BANK) "account" else "number"}. Can you help?")
            })
            if (t.isOpen) add(Option("I want to cancel it") {
                if (b.canCancel(t)) { c.session.openCancel = true; nav.go(Routes.track(t.id)) }
                else banner.show("This can't be cancelled now", "It's already with ${t.providerName}. Chat with us and we'll see what we can do.", BannerKind.Error, "Chat") { openChat(c, nav, t.id) }
            })
            if (t.refundPending) add(Option("My refund hasn't arrived") { openChat(c, nav, t.id); b.sendChat("Where is my refund for ${t.reference}?") })
            add(Option("I was charged twice or the wrong amount") { c.session.disputeTopic = 2; nav.go(Routes.dispute(t.id)) })
            add(Option("I didn't make this transfer") { c.session.disputeTopic = 3; nav.go(Routes.dispute(t.id)) })
            add(Option("Something else") { openChat(c, nav, t.id) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { o -> ListRow(o.text, minHeight = 56.dp, trailing = { Chevron() }, onClick = o.go) }
        }
        a.cases.firstOrNull { it.transferId == t.id }?.let { cs ->
            Note("You have a case open about this transfer (${cs.status(now).label.lowercase()}).", NoteKind.Info, Icons.Info, action = "View case", onAction = { nav.go(Routes.case(cs.id)) })
        }
    }
}

// ------------------------------------------------------------------ 10.04 Chat

@Composable
fun ChatScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    var text by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (a.chat.isEmpty()) b.startChatAbout(c.session.chatAbout) }
    val messages = a.chat
    val typing = messages.lastOrNull()?.fromAgent == false
    LaunchedEffect(messages.size, typing) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.size + (if (typing) 1 else 0)) }
    val about = a.transfer(c.session.chatAbout)
    val picker = rememberPhotoPicker({ File(context.filesDir, "chat-${System.currentTimeMillis()}.jpg") }) { f -> b.sendChat("", f.name) }
    fun send(message: String) {
        val m = message.trim()
        if (m.isEmpty()) return
        b.sendChat(m)
        text = ""
        val latest = a.transfers.maxByOrNull { it.createdAt }
        val lower = m.lowercase()
        if (("still not" in lower || "not there" in lower) && latest != null && a.cases.none { it.transferId == latest.id && it.resolvedAt > b.now }) {
            scope.launch {
                runCatching { b.openCase(latest.id, "Money not received", "Raised from chat: \"$m\"", "In the app", emptyList()) }
            }
        }
    }
    fun openAttachment(msg: ChatMessage) {
        val tr = a.transfers.filter { it.status == TransferStatus.DELIVERED }.maxByOrNull { it.deliveredAt } ?: return
        scope.launch {
            val f = withContext(Dispatchers.IO) { Documents.transferReceipt(context, a, tr) }
            Share.file(context, f, "application/pdf", msg.attachment)
        }
    }
    Box(Modifier.fillMaxSize().background(C.Tint)) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = app.arro.ui.components.MaxContentWidth).fillMaxSize().imePadding()) {
            // header
            Column(Modifier.fillMaxWidth().background(C.White)) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    app.arro.ui.components.BackButton({ nav.back() })
                    Box {
                        Avatar("AM", 6)
                        Box(Modifier.align(Alignment.BottomEnd).size(11.dp).background(Color(0xFF2DBE60), CircleShape).border(2.dp, C.White, CircleShape))
                    }
                    Column(Modifier.weight(1f)) {
                        Txt(Agent.NAME, T.H3)
                        Cap(if (typing) "typing…" else "Arro support · online", size = 12f)
                    }
                    RoundIcon(Icons.Info, {
                        val cs = a.cases.firstOrNull()
                        if (cs != null) nav.go(Routes.case(cs.id)) else banner.show("No open cases", "If your question needs more digging, Amina will open a case you can follow here.")
                    }, style = IconBtn.Soft, description = "Case details")
                }
                HDivider()
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), state = list,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Pill(if (about != null) "About transfer ${about.reference}" else "Today", PillKind.Neutral)
                    }
                }
                items(messages, key = { it.id }) { m -> Bubble(m) { openAttachment(m) } }
                if (typing) item { TypingDots() }
            }
            val quick = messages.lastOrNull()?.takeIf { it.fromAgent }?.quickReplies.orEmpty()
            if (quick.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { qr -> Chip(qr, false, { send(qr) }) }
            }
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(Icons.Paperclip, { picker() }, style = IconBtn.Line, description = "Attach a photo")
                BasicTextField(
                    text, { text = it.take(1000) },
                    Modifier.weight(1f),
                    textStyle = T.Body.copy(color = C.Ink),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(text) }),
                    singleLine = true,
                    cursorBrush = SolidColor(C.Ink),
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).background(C.White, RoundedCornerShape(24.dp)).border(1.5.dp, C.Line, RoundedCornerShape(24.dp))
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (text.isEmpty()) Txt("Message", T.Body, color = C.Placeholder)
                            inner()
                        }
                    },
                )
                RoundIcon(Icons.Send, { send(text) }, style = if (text.isBlank()) IconBtn.Soft else IconBtn.Lime, description = "Send message")
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, onAttachment: () -> Unit) {
    val mine = !m.fromAgent
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (m.text.isNotBlank()) Column(
            Modifier.widthIn(max = 300.dp)
                .then(if (mine) Modifier else Modifier.shadow(1.dp, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)))
                .background(if (mine) C.Ink else C.White, if (mine) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Txt(m.text, T.Body.copy(fontSize = 14.5.sp, lineHeight = 21.sp), color = if (mine) C.White else C.Ink)
            Txt(Text.time(m.at) + if (mine) " · Read" else "", T.Small.copy(fontSize = 11.sp), Modifier.padding(top = 4.dp).fillMaxWidth(),
                color = if (mine) C.DarkCap else C.Slate, align = if (mine) TextAlign.End else TextAlign.Start)
        }
        if (m.attachment.isNotBlank()) {
            if (m.fromAgent) {
                ArroCard(Modifier.widthIn(max = 300.dp).clickable(onClick = onAttachment), padding = 12.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Tile(Icons.Statement, TileStyle.Warn)
                        Column(Modifier.weight(1f)) {
                            Txt(m.attachment, T.RowTitle.copy(fontSize = 14.sp))
                            if (m.attachmentNote.isNotBlank()) Cap(m.attachmentNote, size = 12.5f)
                        }
                        Ic(Icons.Download, contentDescription = "Download")
                    }
                }
            } else {
                Row(
                    Modifier.background(C.Ink, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Ic(Icons.Image, size = 16.dp, tint = C.Lime)
                    Txt("Photo sent", T.Cap, color = C.White)
                }
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val tick = nowTicker(350)
    Row(
        Modifier.background(C.White, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { i ->
            val on = ((tick / 350) % 3).toInt() == i
            Box(Modifier.size(7.dp).background(if (on) C.Slate else C.Pending, CircleShape))
        }
    }
}

// ------------------------------------------------------------------ 10.05 Report a problem

private class Option(val text: String, val go: () -> Unit)

private val disputeTopics = listOf(
    "Recipient hasn't received the money" to "Money not received",
    "Recipient got less than quoted" to "Less than quoted",
    "I was charged incorrectly" to "Incorrect charge",
    "I didn't make this transfer" to "Transfer I didn't make",
)

@Composable
fun DisputeScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(id) ?: run { NotFound("Transfer not found"); return }
    var topic by rememberSaveable { mutableIntStateOf(c.session.disputeTopic.coerceIn(0, disputeTopics.lastIndex)) }
    var details by rememberSaveable { mutableStateOf("") }
    var evidence by rememberSaveable { mutableStateOf(listOf<String>()) }
    var reply by rememberSaveable { mutableIntStateOf(0) }
    var freeze by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val picker = rememberPhotoPicker({ File(context.filesDir, "evidence-${System.currentTimeMillis()}.jpg") }) { f -> evidence = evidence + f.name }
    val replies = listOf("In the app", "Email", "Call me")
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(t.reference) }) },
        gap = 14.dp,
        footer = {
            FieldHint(call.error.value)
            Cap(when (reply) {
                1 -> "We'll email ${a.email} within 1 working day. You can follow your case under Help."
                2 -> "We'll call ${Text.formatPhone(a.phone)} within 1 working day. You can follow your case under Help."
                else -> "We'll update you within 1 working day. You can follow your case under Help."
            }, size = 12f)
            ArroButton("Submit report", {
                scope.launchCall(call) {
                    val cs = b.openCase(t.id, disputeTopics[topic].second, details, replies[reply], evidence)
                    if (topic == 3 && freeze) b.setFrozen(true)
                    banner.show("Case ${cs.number} opened", "We'll update you within 1 working day.", BannerKind.Success)
                    nav.navigate(Routes.case(cs.id)) { popUpTo(Routes.DISPUTE) { inclusive = true } }
                }
            }, loading = call.busy.value, enabled = details.trim().length >= 10)
        },
    ) {
        ScreenTitle("Report a problem")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            disputeTopics.forEachIndexed { i, (label, _) ->
                ListRow(label.replace("Recipient", t.firstName), minHeight = 46.dp, selected = topic == i, leading = { Radio(topic == i) }, onClick = { topic = i })
            }
        }
        if (topic == 3) {
            Note("If someone else got into your account, we'll stop all payments while we look into it.", NoteKind.Warn, Icons.Shield)
            CheckRow(freeze, { freeze = it }, "Freeze my account now", sub = "You can unfreeze it in Security when it's safe")
        }
        ArroField(details, { details = it.take(500) }, label = "What happened?", placeholder = when (topic) {
            0 -> "For example: the app shows delivered, but ${t.firstName} has no credit from Arro."
            1 -> "Tell us how much arrived and how much was quoted."
            2 -> "Tell us what you were charged and what you expected."
            else -> "Tell us when you noticed it and anything else that looks wrong."
        }, singleLine = false, height = 110.dp, capitalization = KeyboardCapitalization.Sentences, hint = "${details.length} / 500",
            imeAction = ImeAction.Default)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Evidence (optional)")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                evidence.forEach { name ->
                    Row(
                        Modifier.height(36.dp).background(C.Tint, RoundedCornerShape(999.dp)).border(1.5.dp, C.Line, RoundedCornerShape(999.dp)).padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Ic(Icons.Image, size = 16.dp)
                        Txt(name.removePrefix("evidence-").take(16), T.Chip)
                        RoundIcon(Icons.Close, {
                            evidence = evidence - name
                            runCatching { File(context.filesDir, name).delete() }
                        }, style = IconBtn.Plain, size = 28.dp, iconSize = 14.dp, description = "Remove")
                    }
                }
                if (evidence.size < 3) Chip("Add", false, { picker() }, icon = Icons.Plus)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("How should we reply?")
            Segmented(replies, reply, { reply = it })
        }
    }
}

// ------------------------------------------------------------------ 10.06 Case

@Composable
fun CaseScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val cs = a.cases.firstOrNull { it.id == id } ?: run { NotFound("Case not found"); return }
    val t = a.transfer(cs.transferId)
    val now = nowTicker()
    val st = cs.status(now)
    var addOpen by remember { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf("") }
    val provider = t?.providerName ?: "the provider"
    fun stateOf(at: Long, next: Long) = when { now >= next -> StepState.Done; now >= at -> StepState.Now; else -> StepState.Todo }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill(st.label, caseKind(st), dot = true) }) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton("Add details", { addOpen = true }, Modifier.weight(1f), style = Btn.Line, fill = true, enabled = st != CaseStatus.RESOLVED)
                ArroButton("Open chat", { openChat(c, nav, cs.transferId) }, Modifier.weight(1f), fill = true)
            }
        },
        overlay = {
            Sheet(addOpen, { addOpen = false }) {
                SheetHeader("Add details", { addOpen = false }, "Anything that helps us resolve case ${cs.number}.")
                ArroField(extra, { extra = it.take(500) }, Modifier.padding(top = 14.dp), placeholder = "For example: Claudine checked again this evening.",
                    singleLine = false, height = 110.dp, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, hint = "${extra.length} / 500")
                ArroButton("Add to case", {
                    b.addCaseDetails(cs.id, extra); extra = ""; addOpen = false
                    banner.show("Added to your case", kind = BannerKind.Success)
                }, Modifier.padding(top = 14.dp), enabled = extra.trim().length >= 3)
            }
        },
    ) {
        Column {
            Over("Case ${cs.number}")
            ScreenTitle(cs.topic, if (t != null) "Transfer ${t.reference} · ${Money.format(t.receiveMinor, t.toCur)} to ${t.recipientName}" else null, Modifier.padding(top = 6.dp))
        }
        Panel(kind = PanelKind.Lime) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Illustration(Art.Clock, 48.dp)
                Column(Modifier.weight(1f)) {
                    if (st == CaseStatus.RESOLVED) {
                        Txt("Resolved ${Text.relativeDay(cs.resolvedAt, now).replaceFirstChar { it.lowercase() }}", T.H3)
                        Cap(when {
                            t == null -> "We've closed this case."
                            t.status == TransferStatus.DELIVERED -> "$provider confirmed the credit to ${t.firstName} at ${Text.time(t.deliveredAt)}. Proof of delivery is in your chat."
                            t.refundPending || t.status == TransferStatus.REFUNDED -> "Your full refund of ${Money.format(t.totalMinor, t.fromCur)} has been issued."
                            else -> "We've updated your transfer. Check its status for details."
                        }, color = C.Forest)
                    } else {
                        Txt("Next update by ${Text.dayMonth(cs.resolvedAt)}, ${Text.time(cs.resolvedAt)}", T.H3)
                        Cap(if (now >= cs.investigatingAt) "We're checking with $provider." else "A specialist is picking up your case.", color = C.Forest)
                    }
                }
            }
        }
        Timeline(listOf(
            TimelineItem("Report received", if (cs.evidence.isEmpty()) "No attachments" else "With ${cs.evidence.size} ${if (cs.evidence.size == 1) "screenshot" else "screenshots"}", Text.time(cs.createdAt), StepState.Done),
            TimelineItem("Assigned to ${Agent.NAME}", "Payments specialist", if (now >= cs.assignedAt) Text.time(cs.assignedAt) else "", stateOf(cs.assignedAt, cs.investigatingAt),
                icon = if (now >= cs.assignedAt && now < cs.investigatingAt) Icons.Refresh else null),
            TimelineItem("Investigating with $provider", "Provider reference requested", if (now >= cs.investigatingAt) Text.time(cs.investigatingAt) else "", stateOf(cs.investigatingAt, cs.resolvedAt),
                icon = if (now >= cs.investigatingAt && now < cs.resolvedAt) Icons.Refresh else null),
            TimelineItem("Resolution", if (st == CaseStatus.RESOLVED) "Case closed" else "Credit confirmed, or a full refund", if (st == CaseStatus.RESOLVED) Text.time(cs.resolvedAt) else "",
                if (st == CaseStatus.RESOLVED) StepState.Done else StepState.Todo),
        ))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Over("What you told us")
            Panel(gap = 8.dp) {
                Txt(cs.details, T.Body14)
                cs.extraDetails.forEach { d -> HDivider(); Txt(d, T.Body14) }
                Cap(when (cs.replyBy) { "Email" -> "We'll reply by email to ${a.email}"; "Call me" -> "We'll call you on ${Text.formatPhone(a.phone)}"; else -> "We'll reply here in the app" }, size = 12f)
            }
        }
        if (t != null) ListRow("View the transfer", minHeight = 52.dp, trailing = { Chevron() }, onClick = { nav.go(if (t.isOpen || t.refundPending) Routes.track(t.id) else Routes.detail(t.id)) })
        Cap("If you're unhappy with our final response, you can refer your complaint to the Financial Ombudsman Service.", size = 12f)
    }
}

@Composable
private fun NotFound(title: String) {
    val nav = LocalNav.current
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }) {
        ScreenTitle(title, "It may have been removed when the demo data was reset.")
        ArroButton("Go to Help", { nav.go(Routes.HELP) }, style = Btn.Line)
    }
}
