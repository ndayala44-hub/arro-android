package app.arro.ui.screens.track

import app.arro.R
import app.arro.ui.str
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
            ScreenTitle(str(R.string.track_transfer_not_found), str(R.string.track_it_may_have_been_removed_from))
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
                Cap(str(R.string.track_to_x, t.recipientName))
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
    RoundIcon(Icons.Help, { nav.go(Routes.transferHelp(t.id)) }, style = IconBtn.Soft, description = str(R.string.track_help_with_this_transfer))
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
    var reason by remember { mutableStateOf(str(R.string.track_wrong_amount)) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val total = (t.deliveredAt - t.createdAt).coerceAtLeast(1)
    val fraction = ((now - t.createdAt).toFloat() / total).coerceIn(0.05f, 0.97f)
    val stage = when {
        now < t.paidAt -> str(R.string.track_we_re_taking_your_payment_from, t.fundingLabel)
        now < t.convertedAt -> str(R.string.track_converting_your_pounds_to_x_at, t.to)
        now < t.handedAt -> str(R.string.track_handing_your_money_to_x, t.providerName)
        else -> when (t.payoutType) {
            PayoutType.BANK -> str(R.string.track_x_has_the_money_and_will, t.providerName, t.firstName)
            PayoutType.CASH -> str(R.string.track_getting_the_cash_ready_at_the, t.firstName)
            else -> str(R.string.track_x_has_the_money_and_is, t.providerName, t.firstName)
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { HelpButton(t) }) },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArroButton(str(R.string.track_share_tracking), {
                    Share.text(context, str(R.string.track_i_ve_sent_x_to_x, Money.format(t.receiveMinor, t.toCur), t.recipientName, Text.by(t.deliveredAt, now), t.reference))
                }, Modifier.weight(1f), style = Btn.Line, icon = Icons.Share, padding = 12.dp)
                ArroButton(str(R.string.track_details), { nav.go(Routes.detail(t.id)) }, Modifier.weight(1f), style = Btn.Line)
            }
            if (b.canCancel(t)) ArroButton(str(R.string.track_request_cancellation), { cancelOpen = true }, style = Btn.DangerText)
        },
        overlay = {
            Sheet(cancelOpen, { cancelOpen = false }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Illustration(Art.Warning, 64.dp)
                    Txt(str(R.string.track_cancel_this_transfer), T.H2.copy(fontSize = 22.sp, lineHeight = 26.sp))
                }
                Txt(str(R.string.track_we_ll_ask_x_to_stop, t.providerName, t.firstName, if (t.payoutType == PayoutType.BANK) str(R.string.track_account) else str(R.string.track_wallet)),
                    T.Cap.copy(fontSize = 14.sp), Modifier.padding(top = 12.dp), color = C.Slate)
                Panel(Modifier.padding(top = 16.dp), gap = 10.dp) {
                    KV(str(R.string.track_you_d_get_back), Money.format(t.totalMinor, t.fromCur), strong = true)
                    KV(str(R.string.track_refunded_to), if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_your_gbp_balance) else t.fundingLabel)
                    KV(str(R.string.track_includes_the_fee), Money.format(t.chargesMinor, t.fromCur))
                }
                FieldLabel(str(R.string.track_why_are_you_cancelling), Modifier.padding(top = 18.dp))
                ReasonChips(listOf(str(R.string.track_wrong_amount), str(R.string.track_wrong_recipient), str(R.string.track_sent_by_mistake), str(R.string.track_other)), reason) { reason = it }
                Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroButton(str(R.string.track_yes_cancel_transfer), {
                        scope.launchCall(call) {
                            b.cancelTransfer(t.id, reason)
                            cancelOpen = false
                            banner.show(str(R.string.track_transfer_cancelled), str(R.string.track_your_refund_is_on_its_way), BannerKind.Success)
                        }
                    }, style = Btn.Danger, loading = call.busy.value)
                    ArroButton(str(R.string.track_keep_transfer), { cancelOpen = false }, style = Btn.Line)
                    FieldHint(call.error.value)
                }
            }
        },
    ) {
        TrackHeader(t, { Pill(str(R.string.track_in_progress), PillKind.Info, dot = true) })
        Panel(kind = PanelKind.Lime) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt(str(R.string.track_arriving_x, Text.by(t.deliveredAt, now)), T.H3)
                Txt(Text.remaining(t.deliveredAt, now), T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Forest)
            }
            ProgressBar(fraction, Modifier.padding(top = 12.dp), track = C.White)
            Cap(stage, Modifier.padding(top = 10.dp), color = C.Forest)
        }
        Timeline(listOf(
            TimelineItem(str(R.string.track_transfer_created), str(R.string.track_x_plus_x_fee, Money.format(t.sendMinor, t.fromCur), Money.format(t.chargesMinor, t.fromCur)), Text.time(t.createdAt), StepState.Done),
            TimelineItem(str(R.string.track_payment_received), t.fundingLabel, if (now >= t.paidAt) Text.time(t.paidAt) else "", if (now >= t.paidAt) StepState.Done else StepState.Now, if (now >= t.paidAt) null else Icons.Refresh),
            TimelineItem(if (t.payoutType == PayoutType.ARRO) str(R.string.track_sent_to_x_s_arro_account, t.firstName) else str(R.string.track_converted_to_x, t.to), if (t.payoutType == PayoutType.ARRO) null else Money.rateLine(t.fromCur, t.toCur, t.rate),
                if (now >= t.convertedAt) Text.time(t.convertedAt) else "", when { now >= t.convertedAt -> StepState.Done; now >= t.paidAt -> StepState.Now; else -> StepState.Todo }),
            TimelineItem(str(R.string.track_with_x, t.providerName), when (t.payoutType) {
                PayoutType.BANK -> str(R.string.track_banks_usually_credit_within_1_business)
                PayoutType.CASH -> str(R.string.track_usually_ready_within_the_hour)
                else -> str(R.string.track_usually_credited_within_10_minutes)
            }, if (now >= t.handedAt) Text.time(t.handedAt) else "", when { now >= t.handedAt -> StepState.Now; else -> StepState.Todo }, if (now >= t.handedAt) Icons.Bolt else null),
            TimelineItem(if (t.payoutType == PayoutType.CASH) str(R.string.track_ready_for_x_to_collect, t.firstName) else str(R.string.track_delivered_to_x, t.firstName), str(R.string.track_we_ll_notify_you_both_by), "", StepState.Todo),
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
    val relations = listOf(str(R.string.track_x_my_x, pronoun(t), relationOf(c, t).lowercase()).takeIf { relationOf(c, t).isNotBlank() }, str(R.string.track_family_member), str(R.string.track_friend), str(R.string.track_business_contact), str(R.string.track_myself_another_account)).filterNotNull().distinct()
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill(str(R.string.track_on_hold), PillKind.Warn, dot = true) }) },
        footer = {
            ArroButton(str(R.string.track_submit_and_continue), {
                scope.launchCall(call) {
                    b.answerHold(t.id, relation ?: "", source ?: "")
                    banner.show(str(R.string.track_thanks_your_transfer_is_moving_again), str(R.string.track_we_ll_let_you_know_when), BannerKind.Success)
                }
            }, loading = call.busy.value, enabled = relation != null && source != null)
        },
        overlay = {
            PickerSheet(picker, str(R.string.track_how_do_you_know_x, t.firstName), relations, relation, { relation = it }, { picker = false })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Illustration(Art.Clock, 76.dp, Modifier.offset(y = (-6).dp))
            Column(Modifier.weight(1f)) {
                ScreenTitle(str(R.string.track_one_quick_question), size = TitleSize.S)
                Cap(str(R.string.track_your_x_to_x_is_paused, Money.format(t.sendMinor, t.fromCur), t.firstName),
                    Modifier.padding(top = 6.dp), size = 14f)
            }
        }
        SelectField(str(R.string.track_how_do_you_know_x, t.firstName), relation, { picker = true }, focused = relation == null)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(str(R.string.track_where_does_this_money_come_from))
            ReasonChips(Catalog.fundSources, source) { source = it }
        }
        Note(str(R.string.track_we_ll_hold_your_rate_of, Money.rate(t.rate), Text.time(t.holdUntil), if (Text.isSameDay(t.holdUntil, System.currentTimeMillis())) str(R.string.track_today) else str(R.string.track_tomorrow), Money.format(t.totalMinor, t.fromCur)),
            NoteKind.Warn, Icons.Lock)
        Timeline(listOf(
            TimelineItem(str(R.string.track_payment_received), null, Text.time(t.paidAt), StepState.Done),
            TimelineItem(str(R.string.track_waiting_for_your_answer), null, Text.time(t.holdAt), StepState.Hold),
        ), Modifier.padding(top = 4.dp))
        FieldHint(call.error.value)
    }
}

private fun relationOf(c: app.arro.AppContainer, t: Transfer): String = c.backend.state.value.account?.recipient(t.recipientId)?.relationship ?: ""
private fun pronoun(t: Transfer): String = str(R.string.track_they_re)

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
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Share, { nav.go(Routes.receipt(t.id)) }, style = IconBtn.Soft, description = str(R.string.track_share_receipt)) }) },
        footer = {
            ArroButton(str(R.string.track_send_to_x_again, t.firstName), {
                val r = a?.recipient(t.recipientId)
                if (r != null) { startSend(c, nav, banner, recipient = r); c.session.send.amountText = Money.number(t.sendMinor, t.fromCur, false).replace(",", "") }
                else banner.show(str(R.string.track_recipient_removed), str(R.string.track_add_x_again_to_send, t.firstName), BannerKind.Info)
            })
            ArroButton(str(R.string.track_view_receipt), { nav.go(Routes.receipt(t.id)) }, style = Btn.Line)
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
            Pill(if (t.payoutType == PayoutType.CASH) str(R.string.track_ready_to_collect) else str(R.string.track_delivered), PillKind.Ok, dot = true)
            Txt(Money.format(t.receiveMinor, t.toCur), T.AmountMd, Modifier.padding(top = 6.dp))
            Txt(
                when (t.payoutType) {
                    PayoutType.CASH -> str(R.string.track_is_ready_for_x_to_collect, t.firstName, Text.time(t.deliveredAt), minutes)
                    PayoutType.BANK -> str(R.string.track_reached_x_s_x_account_at, t.firstName, t.providerName, Text.time(t.deliveredAt), minutes)
                    PayoutType.ARRO -> str(R.string.track_arrived_in_x_s_arro_balance, t.firstName, Text.time(t.deliveredAt))
                    else -> str(R.string.track_reached_x_s_x_wallet_at, t.firstName, t.providerName, Text.time(t.deliveredAt), minutes)
                },
                T.Cap.copy(fontSize = 15.sp), color = C.Slate, align = TextAlign.Center,
            )
        }
        Panel(gap = 10.dp) {
            KV(str(R.string.track_you_paid), Money.format(t.totalMinor, t.fromCur))
            if (t.payoutType != PayoutType.ARRO) KV(str(R.string.track_exchange_rate), Money.rateLine(t.fromCur, t.toCur, t.rate))
            KV(str(R.string.track_delivered_to), if (t.payoutType == PayoutType.MOBILE_MONEY || t.payoutType == PayoutType.EWALLET) Text.formatPhone(t.payoutAccount) else t.payoutAccount)
            KV(str(R.string.track_reference), t.reference)
        }
        Timeline(listOf(
            TimelineItem(str(R.string.track_sent_and_paid), time = Text.time(t.paidAt.coerceAtMost(t.deliveredAt)), state = StepState.Done),
            TimelineItem(str(R.string.track_handed_to_x, t.providerName), time = Text.time(t.handedAt.coerceAtMost(t.deliveredAt)), state = StepState.Done),
            TimelineItem(if (t.payoutType == PayoutType.CASH) str(R.string.track_ready_to_collect) else str(R.string.track_credited_to_x, t.firstName), time = Text.time(t.deliveredAt), state = StepState.Done),
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
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill(str(R.string.track_not_delivered), PillKind.Err, dot = true) }) },
        footer = {
            ArroButton(if (t.payoutType == PayoutType.BANK) str(R.string.track_fix_the_account_number_and_resend) else str(R.string.track_fix_the_number_and_resend), { nav.go(Routes.rcpEdit(t.recipientId)) })
            if (country?.option(PayoutType.BANK) != null && t.payoutType != PayoutType.BANK) ArroButton(str(R.string.track_send_by_bank_deposit_instead), {
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
            ScreenTitle(str(R.string.track_we_couldn_t_deliver_this_transfer), modifier = Modifier.weight(1f), size = TitleSize.S)
        }
        ArroCard(border = if (C.dark) Color(0xFF5A2520) else Color(0xFFF6C9C3), background = if (C.dark) C.ErrBg else Color(0xFFFFF8F7)) {
            Over(str(R.string.track_what_happened), color = C.Err)
            Txt(t.failReason, T.Body, Modifier.padding(top = 8.dp))
        }
        RefundCard(t, f, now)
        Timeline(listOf(
            TimelineItem(str(R.string.track_payment_received), time = Text.time(t.paidAt), state = StepState.Done),
            TimelineItem(str(R.string.track_rejected_by_x, t.providerName), time = Text.time(t.failAt), state = StepState.Bad, icon = Icons.Cross),
            TimelineItem(str(R.string.track_refund_started), time = Text.time(t.refundStartedAt), state = StepState.Now, icon = Icons.Refresh),
        ))
    }
}

@Composable
private fun RefundCard(t: Transfer, fraction: Float, now: Long) {
    ArroCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.Refund, 40.dp)
            Column(Modifier.weight(1f)) {
                Txt(str(R.string.track_your_money_is_safe), T.H3)
                Cap(str(R.string.track_x_is_on_its_way_back, Money.format(t.totalMinor, t.fromCur), if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_your_gbp_balance_2) else t.fundingLabel))
            }
        }
        ProgressBar(fraction, Modifier.padding(top = 12.dp))
        Cap(if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_balance_refunds_arrive_straight_away) else str(R.string.track_refunds_usually_reach_your_card_in, Text.by(t.refundDueAt, now)),
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
    val byYou = t.cancelReason != str(R.string.track_no_answer_within_24_hours)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill(str(R.string.track_cancelled), PillKind.Neutral, dot = true) }) },
        gap = 20.dp,
        footer = {
            ArroButton(str(R.string.track_start_a_new_transfer), {
                val r = state.account?.recipient(t.recipientId)
                startSend(c, nav, banner, recipient = r, country = if (r == null) t.country else null)
            })
        },
    ) {
        TrackHeader(t, {}, struck = true)
        ScreenTitle(str(R.string.track_transfer_cancelled),
            if (byYou) str(R.string.track_cancelled_at_your_request_at_x, Text.time(t.cancelledAt), t.firstName)
            else str(R.string.track_we_didn_t_hear_back_within, t.firstName))
        ArroCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Txt(str(R.string.track_refund_on_its_way), T.H3); Txt(Money.format(t.totalMinor, t.fromCur), T.Num)
            }
            ProgressBar(f, Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap(str(R.string.track_to_x, if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_your_gbp_balance_2) else t.fundingLabel))
                Txt(Text.by(t.refundDueAt, now), T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Ink)
            }
        }
        Timeline(listOf(
            TimelineItem(str(R.string.track_transfer_created), time = Text.time(t.createdAt), state = StepState.Done),
            TimelineItem(if (byYou) str(R.string.track_cancelled_by_you) else str(R.string.track_cancelled_automatically), str(R.string.track_reason_x, t.cancelReason.lowercase()), Text.time(t.cancelledAt), StepState.Done),
            TimelineItem(if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_refund_sent_to_your_balance) else str(R.string.track_refund_sent_to_your_bank),
                if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_it_s_there_now) else str(R.string.track_your_bank_shows_it_in_3), Text.time(t.refundStartedAt), StepState.Now, Icons.Refresh),
            TimelineItem(str(R.string.track_refund_received), state = StepState.Todo),
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
        topBar = { TopBar(onBack = { nav.back() }, right = { Pill(str(R.string.track_refunded), PillKind.Ok, dot = true) }) },
        footer = {
            ArroButton(str(R.string.track_try_sending_again), {
                val r = a.recipient(t.recipientId)
                startSend(c, nav, banner, recipient = r, country = if (r == null) t.country else null)
            })
            ArroButton(str(R.string.track_download_refund_receipt), {
                val f = Documents.refundReceipt(context, a, t)
                banner.show(str(R.string.track_refund_receipt_ready), Share.saveToDownloads(context, f, "application/pdf"), BannerKind.Success)
            }, style = Btn.Line, icon = Icons.Download)
        },
    ) {
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.Refund, 200.dp, description = str(R.string.track_a_coin_with_arrows_circling_back)) }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(str(R.string.track_refund_complete), size = TitleSize.XL, align = TextAlign.Center)
            Txt(buildAnnotatedString {
                withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Money.format(t.totalMinor, t.fromCur)) }
                append(str(R.string.track_is_back_on_x_including_the, if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_your_gbp_balance_2) else str(R.string.track_your) + t.fundingLabel, Money.format(t.chargesMinor, t.fromCur)))
            }, T.Lede, color = C.Slate, align = TextAlign.Center)
        }
        ArroCard(Modifier.padding(top = 8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KV(str(R.string.track_original_transfer), str(R.string.track_to_x_x, t.firstName, Text.dayMonth(t.createdAt)))
                KV(str(R.string.track_refund_reference), t.refundReference)
                KV(str(R.string.track_refund_issued), "${Text.dayMonth(t.refundStartedAt)}, ${Text.time(t.refundStartedAt)}")
                KV(if (t.fundingId == ArroBackend.BALANCE) str(R.string.track_back_in_your_balance) else str(R.string.track_received_by_your_bank), Text.dayMonth(t.refundedAt))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cap(str(R.string.track_can_t_see_it_some_banks, Text.dayMonth(t.refundedAt + 7 * 86_400_000L)), Modifier.weight(1f), size = 12f)
        }
        app.arro.ui.components.LinkText(str(R.string.track_contact_us), { nav.go(Routes.transferHelp(t.id)) }, color = C.Ink, strong = true)
    }
}
