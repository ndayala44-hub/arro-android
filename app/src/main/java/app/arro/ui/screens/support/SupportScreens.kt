package app.arro.ui.screens.support

import app.arro.R
import app.arro.ui.str
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
                if (a != null) ArroButton(str(R.string.support_chat), { openChat(c, nav, null) }, style = Btn.Lime, size = BtnSize.S, icon = Icons.Chat, fill = false)
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
                            ListRow(art.title, flat = true, minHeight = 52.dp, subtitle = str(R.string.support_x_min_read, art.minutes), trailing = { Chevron() },
                                onClick = { category = null; nav.go(Routes.article(art.id)) })
                        }
                    }
                }
            }
        },
    ) {
        ScreenTitle(str(R.string.support_how_can_we_help))
        SearchField(query, { query = it }, str(R.string.support_search_e_g_cancel_a_transfer))
        if (q.isNotEmpty()) {
            if (results.isEmpty()) {
                Note(str(R.string.support_no_articles_match_x_amina_can, query), NoteKind.Info, Icons.Search,
                    action = if (a != null) str(R.string.support_chat_with_us) else null, onAction = { openChat(c, nav, null) })
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
                        Cap(str(R.string.support_your_latest_transfer), color = C.DarkCap, size = 12f)
                        Txt(str(R.string.support_x_to_x, Money.format(latest.receiveMinor, latest.toCur), latest.firstName), T.Body.copy(fontWeight = FontWeight.SemiBold), color = C.OnNight, maxLines = 1)
                    }
                    Pill(str(R.string.support_get_help), PillKind.OnDark)
                }
            }
        }
        val cases = a?.cases.orEmpty()
        if (cases.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Over(str(R.string.support_your_cases))
            Group {
                cases.take(3).forEachIndexed { i, cs ->
                    if (i > 0) GroupDivider()
                    val st = cs.status(now)
                    ListRow(cs.topic, flat = true, minHeight = 56.dp, subtitle = str(R.string.support_case_x_opened_x, cs.number, Text.relativeDay(cs.createdAt, now)),
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
                            Tile(categoryIcon(icon), if (i == 0 && label == str(R.string.support_sending)) TileStyle.Lime else TileStyle.Tint, 36.dp, iconSize = 18.dp)
                            Txt(label, T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink, maxLines = 1)
                        }
                    }
                }
            }
        }
        Column {
            Over(str(R.string.support_popular_questions), Modifier.padding(bottom = 4.dp))
            Catalog.articles.take(3).forEachIndexed { i, art ->
                if (i > 0) HDivider()
                ListRow(art.title, flat = true, minHeight = 48.dp, trailing = { Chevron() }, onClick = { nav.go(Routes.article(art.id)) })
            }
        }
        Panel(gap = 6.dp) {
            Txt(str(R.string.support_still_need_help), T.H3)
            Cap(str(R.string.support_chat_with_us_any_time_of))
            if (a != null) ArroButton(str(R.string.support_chat_with_amina), { openChat(c, nav, null) }, Modifier.padding(top = 6.dp), size = BtnSize.M, icon = Icons.Chat)
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
    val art = Catalog.article(id) ?: run { NotFound(str(R.string.support_article_not_found)); return }
    var helpful by rememberSaveable { mutableStateOf<Boolean?>(null) }
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.support_help_x, art.category)) }) }, gap = 14.dp) {
        Txt(art.title, T.Title.copy(lineHeight = 31.sp))
        Cap(str(R.string.support_updated_x_x_min_read, art.updated, art.minutes))
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
            "arrive", "hold", "cancel", "refunds" -> str(R.string.support_see_my_transfers) to Routes.ACTIVITY
            "fees" -> str(R.string.support_compare_what_it_costs) to Routes.rateCompare()
            "verify" -> str(R.string.support_verification_and_limits) to Routes.VERIFICATION
            "cards" -> str(R.string.support_payment_methods) to Routes.PAY_METHODS
            "security" -> str(R.string.support_security_settings) to Routes.SECURITY
            else -> null
        }
        if (action != null && state.account != null) ArroButton(action.first, { nav.go(action.second) }, style = Btn.Line, size = BtnSize.M)
        ArroCard(Modifier.padding(top = 4.dp), padding = 14.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt(when (helpful) { true -> str(R.string.support_thanks_for_letting_us_know); false -> str(R.string.support_sorry_about_that); null -> str(R.string.support_was_this_helpful) }, T.Body14.copy(fontWeight = FontWeight.Medium), Modifier.weight(1f))
                if (helpful == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(str(R.string.support_yes), false, { helpful = true; banner.show(str(R.string.support_thanks_for_the_feedback), kind = BannerKind.Success) })
                    Chip(str(R.string.support_no), false, { helpful = false })
                }
            }
            if (helpful == false && state.account != null) {
                Cap(str(R.string.support_amina_can_answer_this_for_your), Modifier.padding(top = 8.dp))
                ArroButton(str(R.string.support_chat_with_us), { openChat(c, nav, null) }, Modifier.padding(top = 10.dp), size = BtnSize.M, icon = Icons.Chat)
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
    val t = a.transfer(id) ?: run { NotFound(str(R.string.support_transfer_not_found)); return }
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
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.support_transfer_help)) }) {
        ArroCard(padding = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(Text.initials(t.recipientName), t.recipientColor, flag = t.toCur.flag)
                Column(Modifier.weight(1f)) {
                    Txt(Money.format(t.receiveMinor, t.toCur), T.H3)
                    Cap(str(R.string.support_to_x_x, name, Text.relativeDay(t.createdAt, now).replaceFirstChar { it.lowercase() }), size = 12f)
                }
                Pill(label, kind, dot = true)
            }
        }
        val account = if (t.payoutType == PayoutType.MOBILE_MONEY || t.payoutType == PayoutType.EWALLET) Text.formatPhone(t.payoutAccount) else t.payoutAccount
        when (t.status) {
            TransferStatus.PROCESSING, TransferStatus.IN_PROGRESS -> Note(
                if (now < t.deliveredAt) str(R.string.support_on_track_and_expected_x_most, Text.by(t.deliveredAt, now))
                else str(R.string.support_this_is_taking_a_little_longer, t.providerName), NoteKind.Lime, Icons.Clock,
                action = str(R.string.support_track_it), onAction = { nav.go(Routes.track(t.id)) })
            TransferStatus.ON_HOLD -> Note(str(R.string.support_we_need_one_quick_answer_from), NoteKind.Warn, Icons.Alert, action = str(R.string.support_answer_now), onAction = { nav.go(Routes.track(t.id)) })
            TransferStatus.DELIVERED -> Note(str(R.string.support_x_confirmed_delivery_at_x_on, t.providerName, Text.time(t.deliveredAt), Text.dayMonth(t.deliveredAt), account), NoteKind.Lime, Icons.Check)
            else -> Note(if (t.refundedAt > 0) str(R.string.support_this_transfer_was_refunded_in_full, Text.dayMonth(t.refundedAt)) else str(R.string.support_your_refund_of_x_is_on, Money.format(t.totalMinor, t.fromCur), t.fundingLabel),
                NoteKind.Info, Icons.Refresh, action = str(R.string.support_see_refund), onAction = { nav.go(Routes.track(t.id)) })
        }
        ScreenTitle(str(R.string.support_what_s_wrong), size = app.arro.ui.components.TitleSize.S)
        val options = buildList {
            if (t.isOpen) add(Option(str(R.string.support_it_s_taking_longer_than_expected)) { nav.go(Routes.article("arrive")) })
            if (t.status == TransferStatus.DELIVERED || (t.isOpen && now > t.deliveredAt)) add(Option(str(R.string.support_x_hasn_t_received_it, name)) { c.session.disputeTopic = 0; nav.go(Routes.dispute(t.id)) })
            if (t.status == TransferStatus.DELIVERED) add(Option(str(R.string.support_x_got_less_than_i_expected, name)) { c.session.disputeTopic = 1; nav.go(Routes.dispute(t.id)) })
            add(Option(str(R.string.support_i_sent_it_to_the_wrong, if (t.payoutType == PayoutType.BANK) str(R.string.support_account) else str(R.string.support_number))) {
                openChat(c, nav, t.id)
                b.sendChat(str(R.string.support_i_sent_my_transfer_x_to, t.reference, if (t.payoutType == PayoutType.BANK) str(R.string.support_account) else str(R.string.support_number)))
            })
            if (t.isOpen) add(Option(str(R.string.support_i_want_to_cancel_it)) {
                if (b.canCancel(t)) { c.session.openCancel = true; nav.go(Routes.track(t.id)) }
                else banner.show(str(R.string.support_this_can_t_be_cancelled_now), str(R.string.support_it_s_already_with_x_chat, t.providerName), BannerKind.Error, str(R.string.support_chat)) { openChat(c, nav, t.id) }
            })
            if (t.refundPending) add(Option(str(R.string.support_my_refund_hasn_t_arrived)) { openChat(c, nav, t.id); b.sendChat(str(R.string.support_where_is_my_refund_for_x, t.reference)) })
            add(Option(str(R.string.support_i_was_charged_twice_or_the)) { c.session.disputeTopic = 2; nav.go(Routes.dispute(t.id)) })
            add(Option(str(R.string.support_i_didn_t_make_this_transfer)) { c.session.disputeTopic = 3; nav.go(Routes.dispute(t.id)) })
            add(Option(str(R.string.support_something_else)) { openChat(c, nav, t.id) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { o -> ListRow(o.text, minHeight = 56.dp, trailing = { Chevron() }, onClick = o.go) }
        }
        a.cases.firstOrNull { it.transferId == t.id }?.let { cs ->
            Note(str(R.string.support_you_have_a_case_open_about, cs.status(now).label.lowercase()), NoteKind.Info, Icons.Info, action = str(R.string.support_view_case), onAction = { nav.go(Routes.case(cs.id)) })
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
        if ((str(R.string.support_still_not) in lower || str(R.string.support_not_there) in lower) && latest != null && a.cases.none { it.transferId == latest.id && it.resolvedAt > b.now }) {
            scope.launch {
                runCatching { b.openCase(latest.id, str(R.string.support_money_not_received), str(R.string.support_raised_from_chat_x, m), str(R.string.support_in_the_app), emptyList()) }
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
                        Cap(if (typing) str(R.string.support_typing) else str(R.string.support_arro_support_online), size = 12f)
                    }
                    RoundIcon(Icons.Info, {
                        val cs = a.cases.firstOrNull()
                        if (cs != null) nav.go(Routes.case(cs.id)) else banner.show(str(R.string.support_no_open_cases), str(R.string.support_if_your_question_needs_more_digging))
                    }, style = IconBtn.Soft, description = str(R.string.support_case_details))
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
                        Pill(if (about != null) str(R.string.support_about_transfer_x, about.reference) else str(R.string.support_today), PillKind.Neutral)
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
                RoundIcon(Icons.Paperclip, { picker() }, style = IconBtn.Line, description = str(R.string.support_attach_a_photo))
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
                            if (text.isEmpty()) Txt(str(R.string.support_message), T.Body, color = C.Placeholder)
                            inner()
                        }
                    },
                )
                RoundIcon(Icons.Send, { send(text) }, style = if (text.isBlank()) IconBtn.Soft else IconBtn.Lime, description = str(R.string.support_send_message))
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
                .background(if (mine) C.Night else C.White, if (mine) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Txt(m.text, T.Body.copy(fontSize = 14.5.sp, lineHeight = 21.sp), color = if (mine) C.OnNight else C.Ink)
            Txt(Text.time(m.at) + if (mine) str(R.string.support_read) else "", T.Small.copy(fontSize = 11.sp), Modifier.padding(top = 4.dp).fillMaxWidth(),
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
                        Ic(Icons.Download, contentDescription = str(R.string.support_download))
                    }
                }
            } else {
                Row(
                    Modifier.background(C.Night, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Ic(Icons.Image, size = 16.dp, tint = C.Lime)
                    Txt(str(R.string.support_photo_sent), T.Cap, color = C.OnNight)
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
    str(R.string.support_recipient_hasn_t_received_the_money) to str(R.string.support_money_not_received),
    str(R.string.support_recipient_got_less_than_quoted) to str(R.string.support_less_than_quoted),
    str(R.string.support_i_was_charged_incorrectly) to str(R.string.support_incorrect_charge),
    str(R.string.support_i_didn_t_make_this_transfer) to str(R.string.support_transfer_i_didn_t_make),
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
    val t = a.transfer(id) ?: run { NotFound(str(R.string.support_transfer_not_found)); return }
    var topic by rememberSaveable { mutableIntStateOf(c.session.disputeTopic.coerceIn(0, disputeTopics.lastIndex)) }
    var details by rememberSaveable { mutableStateOf("") }
    var evidence by rememberSaveable { mutableStateOf(listOf<String>()) }
    var reply by rememberSaveable { mutableIntStateOf(0) }
    var freeze by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val picker = rememberPhotoPicker({ File(context.filesDir, "evidence-${System.currentTimeMillis()}.jpg") }) { f -> evidence = evidence + f.name }
    val replies = listOf(str(R.string.support_in_the_app), str(R.string.support_email), str(R.string.support_call_me))
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(t.reference) }) },
        gap = 14.dp,
        footer = {
            FieldHint(call.error.value)
            Cap(when (reply) {
                1 -> str(R.string.support_we_ll_email_x_within_1, a.email)
                2 -> str(R.string.support_we_ll_call_x_within_1, Text.formatPhone(a.phone))
                else -> str(R.string.support_we_ll_update_you_within_1)
            }, size = 12f)
            ArroButton(str(R.string.support_submit_report), {
                scope.launchCall(call) {
                    val cs = b.openCase(t.id, disputeTopics[topic].second, details, replies[reply], evidence)
                    if (topic == 3 && freeze) b.setFrozen(true)
                    banner.show(str(R.string.support_case_x_opened, cs.number), str(R.string.support_we_ll_update_you_within_1_2), BannerKind.Success)
                    nav.navigate(Routes.case(cs.id)) { popUpTo(Routes.DISPUTE) { inclusive = true } }
                }
            }, loading = call.busy.value, enabled = details.trim().length >= 10)
        },
    ) {
        ScreenTitle(str(R.string.support_report_a_problem))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            disputeTopics.forEachIndexed { i, (label, _) ->
                ListRow(label.replace("Recipient", t.firstName), minHeight = 46.dp, selected = topic == i, leading = { Radio(topic == i) }, onClick = { topic = i })
            }
        }
        if (topic == 3) {
            Note(str(R.string.support_if_someone_else_got_into_your), NoteKind.Warn, Icons.Shield)
            CheckRow(freeze, { freeze = it }, str(R.string.support_freeze_my_account_now), sub = str(R.string.support_you_can_unfreeze_it_in_security))
        }
        ArroField(details, { details = it.take(500) }, label = str(R.string.support_what_happened), placeholder = when (topic) {
            0 -> str(R.string.support_for_example_the_app_shows_delivered, t.firstName)
            1 -> str(R.string.support_tell_us_how_much_arrived_and)
            2 -> str(R.string.support_tell_us_what_you_were_charged)
            else -> str(R.string.support_tell_us_when_you_noticed_it)
        }, singleLine = false, height = 110.dp, capitalization = KeyboardCapitalization.Sentences, hint = "${details.length} / 500",
            imeAction = ImeAction.Default)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel(str(R.string.support_evidence_optional))
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
                        }, style = IconBtn.Plain, size = 28.dp, iconSize = 14.dp, description = str(R.string.support_remove))
                    }
                }
                if (evidence.size < 3) Chip(str(R.string.support_add), false, { picker() }, icon = Icons.Plus)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel(str(R.string.support_how_should_we_reply))
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
    val cs = a.cases.firstOrNull { it.id == id } ?: run { NotFound(str(R.string.support_case_not_found)); return }
    val t = a.transfer(cs.transferId)
    val now = nowTicker()
    val st = cs.status(now)
    var addOpen by remember { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf("") }
    val provider = t?.providerName ?: str(R.string.support_the_provider)
    fun stateOf(at: Long, next: Long) = when { now >= next -> StepState.Done; now >= at -> StepState.Now; else -> StepState.Todo }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill(st.label, caseKind(st), dot = true) }) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton(str(R.string.support_add_details), { addOpen = true }, Modifier.weight(1f), style = Btn.Line, fill = true, enabled = st != CaseStatus.RESOLVED)
                ArroButton(str(R.string.support_open_chat), { openChat(c, nav, cs.transferId) }, Modifier.weight(1f), fill = true)
            }
        },
        overlay = {
            Sheet(addOpen, { addOpen = false }) {
                SheetHeader(str(R.string.support_add_details), { addOpen = false }, str(R.string.support_anything_that_helps_us_resolve_case, cs.number))
                ArroField(extra, { extra = it.take(500) }, Modifier.padding(top = 14.dp), placeholder = str(R.string.support_for_example_claudine_checked_again_this),
                    singleLine = false, height = 110.dp, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, hint = "${extra.length} / 500")
                ArroButton(str(R.string.support_add_to_case), {
                    b.addCaseDetails(cs.id, extra); extra = ""; addOpen = false
                    banner.show(str(R.string.support_added_to_your_case), kind = BannerKind.Success)
                }, Modifier.padding(top = 14.dp), enabled = extra.trim().length >= 3)
            }
        },
    ) {
        Column {
            Over(str(R.string.support_case_x, cs.number))
            ScreenTitle(cs.topic, if (t != null) str(R.string.support_transfer_x_x_to_x, t.reference, Money.format(t.receiveMinor, t.toCur), t.recipientName) else null, Modifier.padding(top = 6.dp))
        }
        Panel(kind = PanelKind.Lime) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Illustration(Art.Clock, 48.dp)
                Column(Modifier.weight(1f)) {
                    if (st == CaseStatus.RESOLVED) {
                        Txt(str(R.string.support_resolved_x, Text.relativeDay(cs.resolvedAt, now).replaceFirstChar { it.lowercase() }), T.H3)
                        Cap(when {
                            t == null -> str(R.string.support_we_ve_closed_this_case)
                            t.status == TransferStatus.DELIVERED -> str(R.string.support_x_confirmed_the_credit_to_x, provider, t.firstName, Text.time(t.deliveredAt))
                            t.refundPending || t.status == TransferStatus.REFUNDED -> str(R.string.support_your_full_refund_of_x_has, Money.format(t.totalMinor, t.fromCur))
                            else -> str(R.string.support_we_ve_updated_your_transfer_check)
                        }, color = C.Forest)
                    } else {
                        Txt(str(R.string.support_next_update_by_x_x, Text.dayMonth(cs.resolvedAt), Text.time(cs.resolvedAt)), T.H3)
                        Cap(if (now >= cs.investigatingAt) str(R.string.support_we_re_checking_with_x, provider) else str(R.string.support_a_specialist_is_picking_up_your), color = C.Forest)
                    }
                }
            }
        }
        Timeline(listOf(
            TimelineItem(str(R.string.support_report_received), if (cs.evidence.isEmpty()) str(R.string.support_no_attachments) else str(R.string.support_with_x_x, cs.evidence.size, if (cs.evidence.size == 1) str(R.string.support_screenshot) else str(R.string.support_screenshots)), Text.time(cs.createdAt), StepState.Done),
            TimelineItem(str(R.string.support_assigned_to_x, Agent.NAME), str(R.string.support_payments_specialist), if (now >= cs.assignedAt) Text.time(cs.assignedAt) else "", stateOf(cs.assignedAt, cs.investigatingAt),
                icon = if (now >= cs.assignedAt && now < cs.investigatingAt) Icons.Refresh else null),
            TimelineItem(str(R.string.support_investigating_with_x, provider), str(R.string.support_provider_reference_requested), if (now >= cs.investigatingAt) Text.time(cs.investigatingAt) else "", stateOf(cs.investigatingAt, cs.resolvedAt),
                icon = if (now >= cs.investigatingAt && now < cs.resolvedAt) Icons.Refresh else null),
            TimelineItem(str(R.string.support_resolution), if (st == CaseStatus.RESOLVED) str(R.string.support_case_closed) else str(R.string.support_credit_confirmed_or_a_full_refund), if (st == CaseStatus.RESOLVED) Text.time(cs.resolvedAt) else "",
                if (st == CaseStatus.RESOLVED) StepState.Done else StepState.Todo),
        ))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Over(str(R.string.support_what_you_told_us))
            Panel(gap = 8.dp) {
                Txt(cs.details, T.Body14)
                cs.extraDetails.forEach { d -> HDivider(); Txt(d, T.Body14) }
                Cap(when (cs.replyBy) { str(R.string.support_email) -> str(R.string.support_we_ll_reply_by_email_to, a.email); str(R.string.support_call_me) -> str(R.string.support_we_ll_call_you_on_x, Text.formatPhone(a.phone)); else -> str(R.string.support_we_ll_reply_here_in_the) }, size = 12f)
            }
        }
        if (t != null) ListRow(str(R.string.support_view_the_transfer), minHeight = 52.dp, trailing = { Chevron() }, onClick = { nav.go(if (t.isOpen || t.refundPending) Routes.track(t.id) else Routes.detail(t.id)) })
        Cap(str(R.string.support_if_you_re_unhappy_with_our), size = 12f)
    }
}

@Composable
private fun NotFound(title: String) {
    val nav = LocalNav.current
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }) {
        ScreenTitle(title, str(R.string.support_it_may_have_been_removed_when))
        ArroButton(str(R.string.support_go_to_help), { nav.go(Routes.HELP) }, style = Btn.Line)
    }
}
