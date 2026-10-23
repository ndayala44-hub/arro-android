package app.arro.ui.screens.send

import app.arro.R
import app.arro.ui.str
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.arro.core.ArroBackend
import app.arro.core.ArroException
import app.arro.core.ArroUser
import app.arro.core.Catalog
import app.arro.core.Currency
import app.arro.core.LockKind
import app.arro.core.Loyalty
import app.arro.core.Money
import app.arro.core.PayoutType
import app.arro.core.Text
import app.arro.core.TransferDraft
import app.arro.core.TransferStatus
import app.arro.platform.Biometrics
import app.arro.platform.CameraPreview
import app.arro.platform.Share
import app.arro.platform.hasCamera
import app.arro.platform.rememberCameraPermission
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
import app.arro.ui.resetTo
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.BarCaption
import app.arro.ui.components.Btn
import app.arro.ui.components.Cap
import app.arro.ui.components.Chevron
import app.arro.ui.components.FieldHint
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Halo
import app.arro.ui.components.HDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.Keypad
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PasscodeDots
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SelectField
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.SpinnerRing
import app.arro.ui.components.StepState
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.Timeline
import app.arro.ui.components.TimelineItem
import app.arro.ui.components.TitleSize
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.haloBrush
import app.arro.ui.screens.auth.BIO_NAME
import app.arro.ui.screens.kyc.CameraScreen
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ 4.08 Review and 4.09 Authorise

@Composable
fun SendReviewScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val online by c.connectivity.online.collectAsStateWithLifecycle()
    val a = state.account ?: return
    val d = c.session.send
    val q = d.quote ?: return
    val r = a.recipient(d.recipientId) ?: return
    val m = r.method(d.methodId) ?: r.defaultMethod ?: return
    val fundingId = d.fundingId ?: ArroBackend.BALANCE
    val fundingLabel = if (fundingId == ArroBackend.BALANCE) str(R.string.send_confirm_arro_gbp_balance) else a.paymentMethod(fundingId)?.label ?: ""
    val now = nowTicker()
    // The rate in this quote is held on the server; it can end, be used, or be replaced by a locked offer.
    val lock = a.rateLocks.firstOrNull { it.id == q.lockId }
    val expired = m.type != PayoutType.ARRO && (lock == null || !lock.activeAt(now))
    val earn = Loyalty.pointsToEarn(a, q)
    var reasonPicker by remember { mutableStateOf(false) }
    var approve by remember { mutableStateOf(false) }
    var usePasscode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val arrives = b.estimateArrival(m.type)
    val bioAllowed = a.biometricEnabled && Biometrics.available(context) && !(a.security.passcodeOverThreshold && q.sendMinor > 100_000)

    fun send() = scope.launchCall(call, onError = { e ->
        approve = false; code = ""
        when (e.code) {
            "expired" -> { d.quote = b.requote(q, r.country, fundingId).copy(fundingFeeMinor = b.fundingFee(q.sendMinor, fundingId)); banner.show(str(R.string.send_confirm_we_ve_refreshed_your_quote), e.message ?: str(R.string.send_confirm_check_it_and_send_again), BannerKind.Info); true }
            "insufficient" -> { banner.show(str(R.string.send_confirm_not_enough_in_your_balance), str(R.string.send_confirm_choose_another_way_to_pay), BannerKind.Error); nav.back(); true }
            "outage" -> { nav.go(Routes.SEND_UNAVAILABLE); true }
            else -> { banner.show(str(R.string.send_confirm_transfer_not_sent), e.message ?: "", BannerKind.Error); true }
        }
    }) {
        if (!online) throw ArroException(str(R.string.send_confirm_you_re_offline_connect_to_the))
        val tr = b.createTransfer(TransferDraft(r.id, m.id, q, fundingId, d.reason))
        d.transferId = tr.id
        approve = false
        nav.resetTo(Routes.HOME)
        nav.go(Routes.SEND_PROCESSING)
    }
    fun promptBio() {
        val activity = context as? FragmentActivity ?: return
        Biometrics.prompt(activity, str(R.string.send_confirm_approve_transfer), str(R.string.send_confirm_pay_x_to_x, Money.format(q.totalMinor, Currency.GBP), r.name),
            onSuccess = { send() }, onFallback = { usePasscode = true })
    }
    fun startApproval() {
        if (expired) return
        if (!online) { banner.show(str(R.string.send_confirm_you_re_offline), str(R.string.send_confirm_connect_to_the_internet_to_send), BannerKind.Error); return }
        usePasscode = !bioAllowed
        code = ""; codeError = null
        approve = true
        if (bioAllowed) promptBio()
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.send_confirm_review), right = { BarCaption(str(R.string.send_confirm_n_5_of_5)) }) },
        gap = 14.dp,
        footer = {
            if (expired) {
                ArroButton(str(R.string.send_confirm_get_a_new_quote), {
                    d.quote = b.requote(q, r.country, fundingId).copy(fundingFeeMinor = b.fundingFee(q.sendMinor, fundingId))
                })
            } else {
                ArroButton(str(R.string.send_confirm_send_x, Money.format(q.totalMinor, Currency.GBP)), { startApproval() }, loading = call.busy.value)
            }
        },
        overlay = {
            PickerSheet(reasonPicker, str(R.string.send_confirm_reason_for_sending), Catalog.reasons, d.reason, { d.reason = it }, { reasonPicker = false })
            Sheet(approve, { if (!call.busy.value) approve = false }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!usePasscode) {
                        Box(Modifier.padding(top = 6.dp).size(132.dp), contentAlignment = Alignment.Center) {
                            SpinnerRing(132.dp)
                            Illustration(Art.FaceScan, 104.dp, Modifier.clip(CircleShape))
                        }
                        Txt(str(R.string.send_confirm_approve_with_x, BIO_NAME), T.H2.copy(fontSize = 20.sp), Modifier.padding(top = 14.dp), align = TextAlign.Center)
                    } else {
                        Illustration(Art.Lock, 96.dp)
                        Txt(str(R.string.send_confirm_enter_your_passcode), T.H2.copy(fontSize = 20.sp), Modifier.padding(top = 10.dp), align = TextAlign.Center)
                    }
                    Txt(buildAnnotatedString {
                        append(str(R.string.send_confirm_pay))
                        withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Money.format(q.totalMinor, Currency.GBP)) }
                        append(str(R.string.send_confirm_from_x_to_x, fundingLabel, r.name))
                    }, T.Cap.copy(fontSize = 14.sp), Modifier.padding(top = 6.dp), color = C.Slate, align = TextAlign.Center)
                    if (!usePasscode) {
                        Txt(if (call.busy.value) str(R.string.send_confirm_sending) else str(R.string.send_confirm_waiting_for_you), T.Cap.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 18.dp), color = C.Ok)
                        Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArroButton(str(R.string.send_confirm_try_x_again, BIO_NAME), { promptBio() }, style = Btn.Dark, enabled = !call.busy.value)
                            ArroButton(str(R.string.send_confirm_use_passcode_instead), { usePasscode = true }, style = Btn.Line)
                            ArroButton(str(R.string.send_confirm_cancel), { approve = false }, style = Btn.Text)
                        }
                    } else {
                        PasscodeDots(code.length, error = codeError != null, modifier = Modifier.padding(top = 18.dp))
                        FieldHint(codeError)
                        Keypad({ k ->
                            if (code.length < 6 && !call.busy.value) {
                                code += k; codeError = null
                                if (code.length == 6) scope.launch {
                                    if (b.checkPasscode(code)) send() else { codeError = str(R.string.send_confirm_that_passcode_isn_t_right); code = "" }
                                }
                            }
                        }, { code = code.dropLast(1) }, Modifier.padding(top = 10.dp), keyHeight = 54.dp)
                        ArroButton(str(R.string.send_confirm_cancel), { approve = false }, style = Btn.Text, loading = call.busy.value)
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(r.initials, r.color, 64.dp, flag = if (m.type == PayoutType.ARRO) "gb" else Catalog.country(r.country).currency.flag)
            Txt(r.name, T.H3, Modifier.padding(top = 4.dp))
            Cap("${m.providerName} · ${m.full}")
            Txt(Money.format(q.receiveMinor, q.to), T.AmountMd, Modifier.padding(top = 8.dp))
            if (m.type != PayoutType.ARRO) {
                when {
                    expired && lock != null && lock.releasedAt > 0 -> Pill(str(R.string.send_confirm_rate_updated), PillKind.Info, icon = Icons.Refresh)
                    expired -> Pill(str(R.string.send_confirm_rate_lock_ended), PillKind.Warn, icon = Icons.Clock)
                    lock?.kind == LockKind.OFFER -> Pill(str(R.string.send_confirm_offer_rate_locked_for_x, Text.countdown(q.lockedUntil - now)), PillKind.Ok, icon = Icons.Lock)
                    else -> Pill(str(R.string.send_confirm_rate_locked_for_x, Text.countdown(q.lockedUntil - now)), PillKind.Neutral, icon = Icons.Lock)
                }
            }
        }
        Panel(gap = 10.dp) {
            KV(str(R.string.send_confirm_you_send), Money.format(q.sendMinor, Currency.GBP))
            KV(str(R.string.send_confirm_arro_fee), if (q.feeWaived || q.feeMinor == 0L) str(R.string.send_confirm_free) else Money.format(q.feeMinor, Currency.GBP))
            if (q.discountMinor > 0) KV(str(R.string.send_confirm_points_credit), "− " + Money.format(q.discountMinor, Currency.GBP), valueColor = C.Ok)
            if (q.fundingFeeMinor > 0) KV(str(R.string.send_confirm_credit_card_fee), Money.format(q.fundingFeeMinor, Currency.GBP))
            if (m.type != PayoutType.ARRO) KV(str(R.string.send_confirm_exchange_rate), Money.rateLine(Currency.GBP, q.to, q.rate))
            KV(str(R.string.send_confirm_arrives), when (m.type) {
                PayoutType.BANK -> str(R.string.send_confirm_n_1_business_day_x, Text.by(arrives, now))
                PayoutType.CASH -> str(R.string.send_confirm_within_the_hour_x, Text.by(arrives, now))
                PayoutType.ARRO -> str(R.string.send_confirm_instantly)
                else -> str(R.string.send_confirm_in_minutes_x, Text.by(arrives, now))
            })
            KV(str(R.string.send_confirm_paying_with), fundingLabel)
            HDivider()
            KV(str(R.string.send_confirm_total_to_pay), Money.format(q.totalMinor, Currency.GBP), strong = true)
            if (earn > 0) KV(str(R.string.send_confirm_arro_rewards), str(R.string.send_confirm_x_points_on_delivery, Money.points(earn)), valueColor = C.Ok)
        }
        SelectField(str(R.string.send_confirm_reason_for_sending), d.reason, { reasonPicker = true }, height = 50.dp)
        if (expired) Note(when {
            lock != null && lock.releasedAt > 0 -> str(R.string.send_confirm_the_rate_for_this_transfer_was)
            lock?.kind == LockKind.OFFER -> str(R.string.send_confirm_your_locked_offer_rate_ended_at, Text.time(lock.expiresAt))
            else -> str(R.string.send_confirm_your_30_minute_rate_lock_has)
        }, NoteKind.Warn, Icons.Clock)
        Cap(if (m.type == PayoutType.ARRO) str(R.string.send_confirm_money_sent_to_an_arro_user)
            else str(R.string.send_confirm_check_the_number_carefully_once_the, r.firstName, if (m.type == PayoutType.BANK) str(R.string.send_confirm_account) else str(R.string.send_confirm_wallet)), size = 12f)
    }
}

// ------------------------------------------------------------------ 4.10 Processing

@Composable
fun SendProcessingScreen() {
    val nav = LocalNav.current
    val c = container()
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(c.session.send.transferId) ?: return
    val now = nowTicker(400)
    LaunchedEffect(t.status, now >= t.convertedAt + 900) {
        when {
            t.status == TransferStatus.ON_HOLD -> nav.go(Routes.track(t.id))
            t.status == TransferStatus.DELIVERED || now >= t.convertedAt + 900 -> { delay(400); nav.go(Routes.SEND_SUCCESS) }
        }
    }
    ArroScreen(
        background = C.Tint,
        footer = { ArroButton(str(R.string.send_confirm_continue_in_the_background), { nav.resetTo(Routes.HOME) }, style = Btn.Line) },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 70.dp).height(240.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.Plane, 200.dp, description = str(R.string.send_confirm_a_paper_plane)) }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(str(R.string.send_confirm_sending_your_money), size = TitleSize.XL, align = TextAlign.Center)
            Txt(str(R.string.send_confirm_x_to_x_this_only_takes, Money.format(t.sendMinor, t.fromCur), t.recipientName), T.Lede, color = C.Slate, align = TextAlign.Center)
        }
        ArroCard(Modifier.padding(top = 18.dp)) {
            Timeline(listOf(
                TimelineItem(str(R.string.send_confirm_payment_approved), str(R.string.send_confirm_x_from_x, Money.format(t.totalMinor, t.fromCur), t.fundingLabel), if (now >= t.paidAt) Text.time(t.paidAt) else "",
                    if (now >= t.paidAt) StepState.Done else StepState.Now, if (now >= t.paidAt) null else Icons.Refresh),
                TimelineItem(if (t.payoutType == PayoutType.ARRO) str(R.string.send_confirm_checking_x_s_account, t.firstName) else str(R.string.send_confirm_converted_to_x, t.to),
                    if (t.payoutType == PayoutType.ARRO) null else str(R.string.send_confirm_at_x_as_quoted, Money.rate(t.rate)), if (now >= t.convertedAt) Text.time(t.convertedAt) else "",
                    when { now >= t.convertedAt -> StepState.Done; now >= t.paidAt -> StepState.Now; else -> StepState.Todo }, if (now in t.paidAt until t.convertedAt) Icons.Refresh else null),
                TimelineItem(str(R.string.send_confirm_handing_over_to_x, t.providerName), null, "",
                    if (now >= t.convertedAt) StepState.Now else StepState.Todo, Icons.Refresh),
            ))
        }
    }
}

// ------------------------------------------------------------------ 4.11 Sent

@Composable
fun SendSuccessScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val t = a.transfer(c.session.send.transferId) ?: return
    val now = nowTicker(10_000)
    app.arro.ui.components.DarkSystemBars()
    Box(Modifier.fillMaxSize().background(C.Night)) {
        Box(Modifier.align(Alignment.TopCenter).offset(y = 60.dp).size(440.dp).background(haloBrush(alpha = 0.2f, stop = 0.65f), CircleShape))
        ArroScreen(
            background = androidx.compose.ui.graphics.Color.Transparent,
            topBar = { TopBar(left = { Spacer(Modifier.width(44.dp)) }, right = { RoundIcon(Icons.Close, { nav.resetTo(Routes.HOME) }, style = IconBtn.OnDark, description = str(R.string.send_confirm_close)) }) },
            footer = {
                ArroButton(str(R.string.send_confirm_track_transfer), { nav.resetTo(Routes.HOME); nav.go(Routes.track(t.id)) }, style = Btn.Lime)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroButton(str(R.string.send_confirm_share_receipt), { nav.go(Routes.receipt(t.id)) }, Modifier.weight(1f), style = Btn.LineOnDark, icon = Icons.Share, padding = 12.dp)
                    ArroButton(if (t.repeatMonthly) str(R.string.send_confirm_repeats_monthly) else str(R.string.send_confirm_repeat_monthly), {
                        val on = !t.repeatMonthly
                        b.setRepeatMonthly(t.id, on)
                        banner.show(if (on) str(R.string.send_confirm_repeat_set_up) else str(R.string.send_confirm_repeat_cancelled),
                            if (on) str(R.string.send_confirm_we_ll_remind_you_to_send, t.firstName, Money.format(t.sendMinor, t.fromCur), Text.localDate(t.createdAt).dayOfMonth, ordinal(Text.localDate(t.createdAt).dayOfMonth)) else "",
                            BannerKind.Success)
                    }, Modifier.weight(1f), style = Btn.LineOnDark, icon = Icons.Refresh, padding = 12.dp)
                }
            },
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.Plane, 200.dp, description = str(R.string.send_confirm_a_paper_plane_taking_off)) }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                app.arro.ui.components.Over(str(R.string.send_confirm_money_sent), color = C.Lime)
                Txt(Money.format(t.receiveMinor, t.toCur), T.Amount.copy(fontSize = 46.sp), Modifier.padding(top = 4.dp), color = C.Lime, align = TextAlign.Center)
                Txt(buildAnnotatedString {
                    append(if (t.payoutType == PayoutType.ARRO) str(R.string.send_confirm_has_arrived_with) else str(R.string.send_confirm_is_on_its_way_to))
                    withStyle(SpanStyle(color = C.OnNight, fontWeight = FontWeight.SemiBold)) { append(t.recipientName) }
                }, T.Lede, Modifier.padding(top = 6.dp), color = C.DarkCap, align = TextAlign.Center)
            }
            Column(
                Modifier.padding(top = 10.dp).fillMaxWidth().background(C.OnNight.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .border(1.dp, C.OnNight.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkLine(str(R.string.send_confirm_expected), if (t.status == TransferStatus.DELIVERED) str(R.string.send_confirm_delivered_at_x, Text.time(t.deliveredAt)) else Text.by(t.deliveredAt, now))
                DarkLine(str(R.string.send_confirm_paid), "${Money.format(t.totalMinor, t.fromCur)} · ${t.fundingLabel.replace(" debit", "").replace(" credit", "")}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Cap(str(R.string.send_confirm_reference), color = C.DarkCap)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(t.reference, T.Num.copy(fontSize = 15.sp), color = C.OnNight)
                        RoundIcon(Icons.Copy, { Share.copy(context, str(R.string.send_confirm_arro_reference), t.reference); banner.show(str(R.string.send_confirm_reference_copied), t.reference, BannerKind.Success) },
                            style = IconBtn.OnDark, size = 32.dp, iconSize = 16.dp, description = str(R.string.send_confirm_copy_reference))
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkLine(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Cap(k, color = C.DarkCap)
        Txt(v, T.Num.copy(fontSize = 15.sp), color = C.OnNight, align = TextAlign.End)
    }
}

fun ordinal(n: Int): String = if (n in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

// ------------------------------------------------------------------ 12.06 Provider unavailable

@Composable
fun SendUnavailableScreen() {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val state by appState()
    val d = c.session.send
    val country = Catalog.country(d.country)
    val down = Catalog.provider(state.demo.outageProviderId)
    val backBy = remember { System.currentTimeMillis() + 75 * 60_000L }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.send_confirm_send_to_x, country.name)) }) },
        footer = {
            ArroButton(str(R.string.send_confirm_choose_another_way), { nav.popBackStack(); nav.go(Routes.SEND_DELIVERY) })
            ArroButton(str(R.string.send_confirm_notify_me_when_it_s_back), {
                banner.show(str(R.string.send_confirm_we_ll_let_you_know), str(R.string.send_confirm_you_ll_get_a_notification_when, down?.name ?: str(R.string.send_confirm_it)), BannerKind.Success)
            }, style = Btn.Line, icon = Icons.Bell)
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.Clock, 128.dp, description = str(R.string.send_confirm_a_stopwatch)) }
        ScreenTitle(str(R.string.send_confirm_x_is_briefly_offline, down?.name ?: str(R.string.send_confirm_this_provider)), str(R.string.send_confirm_planned_maintenance_until_about_x_your, Text.time(backBy)), align = TextAlign.Center)
        Group {
            val rows = mutableListOf<Triple<String, Int, Boolean>>()
            country.options.forEach { o ->
                val ps = Catalog.providersFor(country.code, o.type)
                if (o.type == PayoutType.MOBILE_MONEY || o.type == PayoutType.EWALLET) ps.forEach { p -> rows += Triple(p.name, deliveryIcon(o.type), p.id == down?.id) }
                else rows += Triple(o.type.label, deliveryIcon(o.type), ps.any { it.id == down?.id })
            }
            rows.forEachIndexed { i, (name, icon, isDown) ->
                if (i > 0) GroupDivider()
                ListRow(name, flat = true, minHeight = 56.dp,
                    leading = { Tile(icon, if (isDown) TileStyle.Warn else TileStyle.Tint, size = 36.dp, iconSize = 16.dp) },
                    trailing = { if (isDown) Pill(str(R.string.send_confirm_back_by_x, Text.time(backBy)), PillKind.Warn, dot = true) else Pill(str(R.string.send_confirm_working), PillKind.Ok, dot = true) })
            }
        }
    }
}

// ------------------------------------------------------------------ To an Arro user

@Composable
fun ArroUserScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    var query by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<ArroUser?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val existing = a.recipients.filter { r -> r.methods.any { it.type == PayoutType.ARRO } }
    fun sendTo(u: ArroUser) {
        val r = b.addArroRecipient(u)
        startSend(c, nav, banner, recipient = r)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Scan, { nav.go(Routes.SCAN_QR) }, style = IconBtn.Soft, description = str(R.string.send_confirm_scan_a_qr_code)) }) },
        footer = {
            ArroButton(if (found != null) str(R.string.send_confirm_send_to_x, found!!.name.split(" ").first()) else str(R.string.send_confirm_find), {
                val f = found
                if (f != null) sendTo(f)
                else scope.launchCall(call) { found = b.findArroUser(query) }
            }, loading = call.busy.value, enabled = query.isNotBlank() || found != null)
        },
    ) {
        ScreenTitle(str(R.string.send_confirm_send_to_an_arro_user), str(R.string.send_confirm_instant_and_free_between_arro_accounts))
        ArroField(query, { query = it; found = null; call.error.value = null }, label = str(R.string.send_confirm_username_or_mobile_number), placeholder = str(R.string.send_confirm_aline_or_07700_900456),
            keyboard = KeyboardType.Email, error = call.error.value, onDone = { scope.launchCall(call) { found = b.findArroUser(query) } })
        found?.let { u ->
            ListRow(u.name, subtitle = "@${u.handle} · ${Text.maskPhone(u.phone)}", selected = true,
                leading = { Avatar(Text.initials(u.name), 3, flag = "gb") }, trailing = { Pill(str(R.string.send_confirm_on_arro), PillKind.Lime) }, onClick = { sendTo(u) })
        }
        if (existing.isNotEmpty()) {
            Over(str(R.string.send_confirm_your_arro_contacts))
            existing.forEach { r ->
                ListRow(r.name, subtitle = r.defaultMethod?.account, leading = { Avatar(r.initials, r.color, flag = "gb") }, trailing = { Chevron() },
                    onClick = { startSend(c, nav, banner, recipient = r) })
            }
        }
        Note(str(R.string.send_confirm_try_aline_amani_kofi_or_ngozi), NoteKind.Lime, Icons.Info)
    }
}

// ------------------------------------------------------------------ Scan a QR code

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScanQrScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val permission = rememberCameraPermission()
    val cameraOk = hasCamera(context, front = false)
    var showMine by remember { mutableStateOf(false) }
    var handled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (!permission.granted && cameraOk) permission.request() }
    fun handle(raw: String) {
        if (handled) return
        val handle = Regex("(?:arro://pay/|arro\\.app/pay/|^@)([A-Za-z0-9._-]+)").find(raw)?.groupValues?.get(1)
        if (handle == null) { banner.show(str(R.string.send_confirm_that_s_not_an_arro_code), str(R.string.send_confirm_scan_the_qr_code_in_someone), BannerKind.Error); return }
        handled = true
        scope.launch {
            runCatching { b.findArroUser(handle) }
                .onSuccess { u -> startSend(c, nav, banner, recipient = b.addArroRecipient(u)) }
                .onFailure { banner.show(str(R.string.send_confirm_code_not_recognised), it.message ?: "", BannerKind.Error); delay(1500); handled = false }
        }
    }
    val analyzer = remember {
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        ImageAnalysis.Analyzer { proxy ->
            val media = proxy.image
            if (media == null) { proxy.close(); return@Analyzer }
            scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let { handle(it) } }
                .addOnCompleteListener { proxy.close() }
        }
    }
    CameraScreen(str(R.string.send_confirm_scan_a_qr_code), { nav.back() }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(130.dp))
            Txt(str(R.string.send_confirm_point_at_someone_s_arro_code), T.Cap.copy(fontSize = 15.sp), color = C.OnNight)
            Box(Modifier.padding(top = 28.dp).size(260.dp).clip(RoundedCornerShape(22.dp)).border(3.dp, C.Lime, RoundedCornerShape(22.dp))) {
                if (permission.granted && cameraOk) CameraPreview(Modifier.fillMaxSize(), analyzer = analyzer)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ic(Icons.Scan, size = 64.dp, tint = C.DarkCap) }
            }
            if (!permission.granted && cameraOk) ArroButton(str(R.string.send_confirm_allow_camera), { permission.request() }, Modifier.padding(top = 16.dp), style = Btn.Lime,
                size = app.arro.ui.components.BtnSize.M, fill = false)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ArroButton(str(R.string.send_confirm_show_my_code), { showMine = true }, style = Btn.Lime, icon = Icons.Scan)
                ArroButton(str(R.string.send_confirm_enter_a_username_instead), { nav.go(Routes.SEND_ARRO) }, style = Btn.LineOnDark)
            }
        }
        Sheet(showMine, { showMine = false }) {
            SheetHeader(str(R.string.send_confirm_my_arro_code), { showMine = false }, str(R.string.send_confirm_anyone_with_arro_can_scan_this))
            val bmp = remember(a.username) { qrBitmap("arro://pay/${a.username}", 560) }
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(bmp.asImageBitmap(), str(R.string.send_confirm_qr_code_for_x, a.username), Modifier.size(240.dp))
                Txt("@${a.username}", T.H2)
                Cap(a.displayName)
            }
            ArroButton(str(R.string.send_confirm_share_my_code), { Share.text(context, str(R.string.send_confirm_pay_me_on_arro_arro_app, a.username)) }, Modifier.padding(top = 18.dp), style = Btn.Line, icon = Icons.Share)
        }
    }
}

/** Renders [text] as a QR code bitmap with ZXing. */
fun qrBitmap(text: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) 0xFF042D29.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
