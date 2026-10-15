package app.arro.ui.screens.track

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.ArroBackend
import app.arro.core.Catalog
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
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.Btn
import app.arro.ui.components.Cap
import app.arro.ui.components.Chip
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.Halo
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.ProgressBar
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SelectField
import app.arro.ui.components.Sheet
import app.arro.ui.components.StepState
import app.arro.ui.components.Timeline
import app.arro.ui.components.TimelineItem
import app.arro.ui.components.TitleSize
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.screens.send.startSend
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** One screen for every state of a transfer: in progress, on hold, delivered, failed, cancelled and refunded. */
@Composable
fun TrackScreen(id: String) {
    val nav = LocalNav.current
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(id)
    if (t == null) {
        ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }) {
            ScreenTitle("Transfer not found", "It may have been removed from this device.")
        }
        return
    }
    when (t.status) {
        TransferStatus.PROCESSING, TransferStatus.IN_PROGRESS -> InProgress(t)
        TransferStatus.ON_HOLD -> OnHold(t)
        TransferStatus.DELIVERED -> Delivered(t)
        TransferStatus.FAILED -> Failed(t)
        TransferStatus.CANCELLED -> Cancelled(t)
        TransferStatus.REFUNDED -> Refunded(t)
    }
}

@Composable
private fun TrackHeader(t: Transfer, pill: @Composable () -> Unit, struck: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(Text.initials(t.recipientName), t.recipientColor, 64.dp, flag = if (t.to == "GBP") "gb" else t.toCur.flag, modifier = Modifier.alpha(if (struck) 0.7f else 1f))
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Cap("To ${t.recipientName}")
                pill()
            }
            Txt(Money.format(t.receiveMinor, t.toCur), T.Big28.copy(textDecoration = if (struck) TextDecoration.LineThrough else null),
                Modifier.padding(top = 2.dp), color = if (struck) C.Slate else C.Ink, maxLines = 1)
        }
    }
}

@Composable
private fun HelpButton(t: Transfer) {
    val nav = LocalNav.current
    RoundIcon(Icons.Help, { nav.go(Routes.transferHelp(t.id)) }, style = IconBtn.Soft, description = "Help with this transfer")
}

// ------------------------------------------------------------------ 5.01 In progress (+ 5.05 cancel sheet)

@Composable
private fun InProgress(t: Transfer) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val now = nowTicker()
    // Opened straight from "I want to cancel it" in transfer help.
    var cancelOpen by remember { mutableStateOf(c.session.openCancel.also { c.session.openCancel = false }) }
    var reason by remember { mutableStateOf("Wrong amount") }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val total = (t.deliveredAt - t.createdAt).coerceAtLeast(1)
    val fraction = ((now - t.createdAt).toFloat() / total).coerceIn(0.05f, 0.97f)
    val stage = when {
        now < t.paidAt -> "We're taking your payment from ${t.fundingLabel}."
        now < t.convertedAt -> "Converting your pounds to ${t.to} at the rate you locked."
        now < t.handedAt -> "Handing your money to ${t.providerName}."
        else -> when (t.payoutType) {
            PayoutType.BANK -> "${t.providerName} has the money and will credit ${t.firstName}'s account."
            PayoutType.CASH -> "Getting the cash ready at the agent for ${t.firstName}."
            else -> "${t.providerName} has the money and is crediting ${t.firstName}'s wallet."
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { HelpButton(t) }) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton("Share tracking", {
                    Share.text(context, "I've sent ${Money.format(t.receiveMinor, t.toCur)} to ${t.recipientName} with Arro. It should arrive ${Text.by(t.deliveredAt, now)}. Reference ${t.reference}.")
                }, Modifier.weight(1f), style = Btn.Line, icon = Icons.Share, padding = 12.dp)
                ArroButton("Details", { nav.go(Routes.detail(t.id)) }, Modifier.weight(1f), style = Btn.Line)
            }
            if (b.canCancel(t)) ArroButton("Request cancellation", { cancelOpen = true }, style = Btn.DangerText)
        },
        overlay = {
            Sheet(cancelOpen, { cancelOpen = false }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Illustration(Art.Warning, 64.dp)
                    Txt("Cancel this transfer?", T.H2.copy(fontSize = 22.sp, lineHeight = 26.sp))
                }
                Txt("We'll ask ${t.providerName} to stop it. If the money has already reached ${t.firstName}'s ${if (t.payoutType == PayoutType.BANK) "account" else "wallet"}, it can't be cancelled.",
                    T.Cap.copy(fontSize = 14.sp), Modifier.padding(top = 12.dp), color = C.Slate)
                Panel(Modifier.padding(top = 16.dp), gap = 10.dp) {
                    KV("You'd get back", Money.format(t.totalMinor, t.fromCur), strong = true)
                    KV("Refunded to", if (t.fundingId == ArroBackend.BALANCE) "Your GBP balance" else t.fundingLabel)
                    KV("Includes the fee", Money.format(t.feeMinor + t.fundingFeeMinor, t.fromCur))
                }
                FieldLabel("Why are you cancelling?", Modifier.padding(top = 18.dp))
                ReasonChips(listOf("Wrong amount", "Wrong recipient", "Sent by mistake", "Other"), reason) { reason = it }
                Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroButton("Yes, cancel transfer", {
                        scope.launchCall(call) {
                            b.cancelTransfer(t.id, reason)
                            cancelOpen = false
                            banner.show("Transfer cancelled", "Your refund is on its way.", BannerKind.Success)
                        }
                    }, style = Btn.Danger, loading = call.busy.value)
                    ArroButton("Keep transfer", { cancelOpen = false }, style = Btn.Line)
                    FieldHint(call.error.value)
                }
            }
        },
    ) {
        TrackHeader(t, { Pill("In progress", PillKind.Info, dot = true) })
        Panel(kind = PanelKind.Lime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt("Arriving ${Text.by(t.deliveredAt, now)}", T.H3)
                Txt(Text.remaining(t.deliveredAt, now), T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Forest)
            }
            ProgressBar(fraction, Modifier.padding(top = 12.dp), track = C.White)
            Cap(stage, Modifier.padding(top = 10.dp), color = C.Forest)
        }
        Timeline(listOf(
            TimelineItem("Transfer created", "${Money.format(t.sendMinor, t.fromCur)} plus ${Money.format(t.feeMinor + t.fundingFeeMinor, t.fromCur)} fee", Text.time(t.createdAt), StepState.Done),
            TimelineItem("Payment received", t.fundingLabel, if (now >= t.paidAt) Text.time(t.paidAt) else "", if (now >= t.paidAt) StepState.Done else StepState.Now, if (now >= t.paidAt) null else Icons.Refresh),
            TimelineItem(if (t.payoutType == PayoutType.ARRO) "Sent to ${t.firstName}'s Arro account" else "Converted to ${t.to}", if (t.payoutType == PayoutType.ARRO) null else Money.rateLine(t.fromCur, t.toCur, t.rate),
                if (now >= t.convertedAt) Text.time(t.convertedAt) else "", when { now >= t.convertedAt -> StepState.Done; now >= t.paidAt -> StepState.Now; else -> StepState.Todo }),
            TimelineItem("With ${t.providerName}", when (t.payoutType) {
                PayoutType.BANK -> "Banks usually credit within 1 business day"
                PayoutType.CASH -> "Usually ready within the hour"
                else -> "Usually credited within 10 minutes"
            }, if (now >= t.handedAt) Text.time(t.handedAt) else "", when { now >= t.handedAt -> StepState.Now; else -> StepState.Todo }, if (now >= t.handedAt) Icons.Bolt else null),
            TimelineItem(if (t.payoutType == PayoutType.CASH) "Ready for ${t.firstName} to collect" else "Delivered to ${t.firstName}", "We'll notify you both by SMS", "", StepState.Todo),
        ))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReasonChips(options: List<String>, selected: String?, onPick: (String) -> Unit) {
    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> Chip(o, o == selected, { onPick(o) }) }
    }
}

// ------------------------------------------------------------------ 5.03 On hold, action needed

@Composable
private fun OnHold(t: Transfer) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    var relation by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val relations = listOf("${pronoun(t)} my ${relationOf(c, t).lowercase()}".takeIf { relationOf(c, t).isNotBlank() }, "Family member", "Friend", "Business contact", "Myself, another account").filterNotNull().distinct()
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill("On hold", PillKind.Warn, dot = true) }) },
        footer = {
            ArroButton("Submit and continue", {
                scope.launchCall(call) {
                    b.answerHold(t.id, relation ?: "", source ?: "")
                    banner.show("Thanks, your transfer is moving again", "We'll let you know when it arrives.", BannerKind.Success)
                }
            }, loading = call.busy.value, enabled = relation != null && source != null)
        },
        overlay = {
            PickerSheet(picker, "How do you know ${t.firstName}?", relations, relation, { relation = it }, { picker = false })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Illustration(Art.Clock, 76.dp, Modifier.offset(y = (-6).dp))
            Column(Modifier.weight(1f)) {
                ScreenTitle("One quick question", size = TitleSize.S)
                Cap("Your ${Money.format(t.sendMinor, t.fromCur)} to ${t.firstName} is paused until you answer. Regulations require it for some transfers.",
                    Modifier.padding(top = 6.dp), size = 14f)
            }
        }
        SelectField("How do you know ${t.firstName}?", relation, { picker = true }, focused = relation == null)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Where does this money come from?")
            ReasonChips(Catalog.fundSources, source) { source = it }
        }
        Note("We'll hold your rate of ${Money.rate(t.rate)} for 24 hours. If we don't hear from you by ${Text.time(t.holdUntil)} ${if (Text.isSameDay(t.holdUntil, System.currentTimeMillis())) "today" else "tomorrow"}, we'll refund ${Money.format(t.totalMinor, t.fromCur)} in full.",
            NoteKind.Warn, Icons.Lock)
        Timeline(listOf(
            TimelineItem("Payment received", null, Text.time(t.paidAt), StepState.Done),
            TimelineItem("Waiting for your answer", null, Text.time(t.holdAt), StepState.Hold),
        ), Modifier.padding(top = 4.dp))
        FieldHint(call.error.value)
    }
}

private fun relationOf(c: app.arro.AppContainer, t: Transfer): String = c.backend.state.value.account?.recipient(t.recipientId)?.relationship ?: ""
private fun pronoun(t: Transfer): String = "They're"

// ------------------------------------------------------------------ 5.02 Delivered

@Composable
private fun Delivered(t: Transfer) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account
    val minutes = Text.duration(t.deliveredAt - t.createdAt)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Share, { nav.go(Routes.receipt(t.id)) }, style = IconBtn.Soft, description = "Share receipt") }) },
        footer = {
            ArroButton("Send to ${t.firstName} again", {
                val r = a?.recipient(t.recipientId)
                if (r != null) { startSend(c, nav, banner, recipient = r); c.session.send.amountText = Money.number(t.sendMinor, t.fromCur, false).replace(",", "") }
                else banner.show("Recipient removed", "Add ${t.firstName} again to send.", BannerKind.Info)
            })
            ArroButton("View receipt", { nav.go(Routes.receipt(t.id)) }, style = Btn.Line)
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(112.dp)) {
                Avatar(Text.initials(t.recipientName), t.recipientColor, 84.dp, modifier = Modifier.offset(14.dp, 14.dp))
                Box(
                    Modifier.align(Alignment.BottomEnd).offset((-2).dp, (-6).dp).size(40.dp).background(C.Lime, CircleShape).border(4.dp, C.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Ic(Icons.CheckBold, size = 20.dp, tint = C.OnLime) }
            }
            Pill(if (t.payoutType == PayoutType.CASH) "Ready to collect" else "Delivered", PillKind.Ok, dot = true)
            Txt(Money.format(t.receiveMinor, t.toCur), T.AmountMd, Modifier.padding(top = 6.dp))
            Txt(
                when (t.payoutType) {
                    PayoutType.CASH -> "is ready for ${t.firstName} to collect with ID\nsince ${Text.time(t.deliveredAt)}, $minutes after you sent it"
                    PayoutType.BANK -> "reached ${t.firstName}'s ${t.providerName} account\nat ${Text.time(t.deliveredAt)}, $minutes after you sent it"
                    PayoutType.ARRO -> "arrived in ${t.firstName}'s Arro balance\nat ${Text.time(t.deliveredAt)}"
                    else -> "reached ${t.firstName}'s ${t.providerName} wallet\nat ${Text.time(t.deliveredAt)}, $minutes after you sent it"
                },
                T.Cap.copy(fontSize = 15.sp), color = C.Slate, align = TextAlign.Center,
            )
        }
        Panel(gap = 10.dp) {
            KV("You paid", Money.format(t.totalMinor, t.fromCur))
            if (t.payoutType != PayoutType.ARRO) KV("Exchange rate", Money.rateLine(t.fromCur, t.toCur, t.rate))
            KV("Delivered to", if (t.payoutType == PayoutType.MOBILE_MONEY || t.payoutType == PayoutType.EWALLET) Text.formatPhone(t.payoutAccount) else t.payoutAccount)
            KV("Reference", t.reference)
        }
        Timeline(listOf(
            TimelineItem("Sent and paid", time = Text.time(t.paidAt.coerceAtMost(t.deliveredAt)), state = StepState.Done),
            TimelineItem("Handed to ${t.providerName}", time = Text.time(t.handedAt.coerceAtMost(t.deliveredAt)), state = StepState.Done),
            TimelineItem(if (t.payoutType == PayoutType.CASH) "Ready to collect" else "Credited to ${t.firstName}", time = Text.time(t.deliveredAt), state = StepState.Done),
        ), Modifier.padding(horizontal = 4.dp), compact = true)
    }
}

// ------------------------------------------------------------------ 5.04 Failed

@Composable
private fun Failed(t: Transfer) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val now = nowTicker()
    val state by appState()
    val a = state.account
    val country = Catalog.countryOrNull(t.country)
    val f = ((now - t.refundStartedAt).toFloat() / (t.refundDueAt - t.refundStartedAt).coerceAtLeast(1)).coerceIn(0.1f, 0.95f)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill("Not delivered", PillKind.Err, dot = true) }) },
        footer = {
            ArroButton(if (t.payoutType == PayoutType.BANK) "Fix the account number and resend" else "Fix the number and resend", { nav.go(Routes.rcpEdit(t.recipientId)) })
            if (country?.option(PayoutType.BANK) != null && t.payoutType != PayoutType.BANK) ArroButton("Send by bank deposit instead", {
                val r = a?.recipient(t.recipientId)
                startSend(c, nav, banner, recipient = r)
                c.session.send.payoutType = PayoutType.BANK
                c.session.send.methodId = r?.methodFor(PayoutType.BANK)?.id
                if (c.session.send.methodId == null) c.session.send.recipientId = null
                c.session.send.amountText = Money.number(t.sendMinor, t.fromCur, false).replace(",", "")
            }, style = Btn.Line)
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Illustration(Art.Warning, 76.dp, Modifier.offset(y = (-6).dp))
            ScreenTitle("We couldn't deliver this transfer", modifier = Modifier.weight(1f), size = TitleSize.S)
        }
        ArroCard(border = if (C.dark) Color(0xFF5A2520) else Color(0xFFF6C9C3), background = if (C.dark) C.ErrBg else Color(0xFFFFF8F7)) {
            Over("What happened", color = C.Err)
            Txt(t.failReason, T.Body, Modifier.padding(top = 8.dp))
        }
        RefundCard(t, f, now)
        Timeline(listOf(
            TimelineItem("Payment received", time = Text.time(t.paidAt), state = StepState.Done),
            TimelineItem("Rejected by ${t.providerName}", time = Text.time(t.failAt), state = StepState.Bad, icon = Icons.Cross),
            TimelineItem("Refund started", time = Text.time(t.refundStartedAt), state = StepState.Now, icon = Icons.Refresh),
        ))
    }
}

@Composable
private fun RefundCard(t: Transfer, fraction: Float, now: Long) {
    ArroCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.Refund, 40.dp)
            Column(Modifier.weight(1f)) {
                Txt("Your money is safe", T.H3)
                Cap("${Money.format(t.totalMinor, t.fromCur)} is on its way back to ${if (t.fundingId == ArroBackend.BALANCE) "your GBP balance" else t.fundingLabel}")
            }
        }
        ProgressBar(fraction, Modifier.padding(top = 12.dp))
        Cap(if (t.fundingId == ArroBackend.BALANCE) "Balance refunds arrive straight away." else "Refunds usually reach your card in 3 to 5 working days. Expected ${Text.by(t.refundDueAt, now)}.",
            Modifier.padding(top = 8.dp), size = 12f)
    }
}

// ------------------------------------------------------------------ 5.06 Cancelled

@Composable
private fun Cancelled(t: Transfer) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val now = nowTicker()
    val state by appState()
    val f = ((now - t.refundStartedAt).toFloat() / (t.refundDueAt - t.refundStartedAt).coerceAtLeast(1)).coerceIn(0.1f, 0.95f)
    val byYou = t.cancelReason != "No answer within 24 hours"
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill("Cancelled", PillKind.Neutral, dot = true) }) },
        gap = 20.dp,
        footer = {
            ArroButton("Start a new transfer", {
                val r = state.account?.recipient(t.recipientId)
                startSend(c, nav, banner, recipient = r, country = if (r == null) t.country else null)
            })
        },
    ) {
        TrackHeader(t, {}, struck = true)
        ScreenTitle("Transfer cancelled",
            if (byYou) "Cancelled at your request at ${Text.time(t.cancelledAt)}. Nothing was sent to ${t.firstName}."
            else "We didn't hear back within 24 hours, so we cancelled it. Nothing was sent to ${t.firstName}.")
        ArroCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Txt("Refund on its way", T.H3); Txt(Money.format(t.totalMinor, t.fromCur), T.Num)
            }
            ProgressBar(f, Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap("To ${if (t.fundingId == ArroBackend.BALANCE) "your GBP balance" else t.fundingLabel}")
                Txt(Text.by(t.refundDueAt, now), T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink)
            }
        }
        Timeline(listOf(
            TimelineItem("Transfer created", time = Text.time(t.createdAt), state = StepState.Done),
            TimelineItem(if (byYou) "Cancelled by you" else "Cancelled automatically", "Reason: ${t.cancelReason.lowercase()}", Text.time(t.cancelledAt), StepState.Done),
            TimelineItem(if (t.fundingId == ArroBackend.BALANCE) "Refund sent to your balance" else "Refund sent to your bank",
                if (t.fundingId == ArroBackend.BALANCE) "It's there now" else "Your bank shows it in 3 to 5 working days", Text.time(t.refundStartedAt), StepState.Now, Icons.Refresh),
            TimelineItem("Refund received", state = StepState.Todo),
        ))
    }
}

// ------------------------------------------------------------------ 5.07 Refunded

@Composable
private fun Refunded(t: Transfer) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    ArroScreen(
        background = C.Tint,
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill("Refunded", PillKind.Ok, dot = true) }) },
        footer = {
            ArroButton("Try sending again", {
                val r = a.recipient(t.recipientId)
                startSend(c, nav, banner, recipient = r, country = if (r == null) t.country else null)
            })
            ArroButton("Download refund receipt", {
                val f = Documents.refundReceipt(context, a, t)
                banner.show("Refund receipt ready", Share.saveToDownloads(context, f, "application/pdf"), BannerKind.Success)
            }, style = Btn.Line, icon = Icons.Download)
        },
    ) {
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.Refund, 200.dp, description = "A coin with arrows circling back") }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle("Refund complete", size = TitleSize.XL, align = TextAlign.Center)
            Txt(buildAnnotatedString {
                withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Money.format(t.totalMinor, t.fromCur)) }
                append(" is back on ${if (t.fundingId == ArroBackend.BALANCE) "your GBP balance" else "your " + t.fundingLabel}, including the ${Money.format(t.feeMinor + t.fundingFeeMinor, t.fromCur)} fee.")
            }, T.Lede, color = C.Slate, align = TextAlign.Center)
        }
        ArroCard(Modifier.padding(top = 8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KV("Original transfer", "To ${t.firstName} · ${Text.dayMonth(t.createdAt)}")
                KV("Refund reference", t.refundReference)
                KV("Refund issued", "${Text.dayMonth(t.refundStartedAt)}, ${Text.time(t.refundStartedAt)}")
                KV(if (t.fundingId == ArroBackend.BALANCE) "Back in your balance" else "Received by your bank", Text.dayMonth(t.refundedAt))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cap("Can't see it? Some banks take an extra day to show refunds. If it's not there by ${Text.dayMonth(t.refundedAt + 7 * 86_400_000L)}, ", Modifier.weight(1f), size = 12f)
        }
        app.arro.ui.components.LinkText("Contact us", { nav.go(Routes.transferHelp(t.id)) }, color = C.Ink, strong = true)
    }
}
