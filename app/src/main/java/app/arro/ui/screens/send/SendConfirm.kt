package app.arro.ui.screens.send

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
    val fundingLabel = if (fundingId == ArroBackend.BALANCE) "Arro GBP balance" else a.paymentMethod(fundingId)?.label ?: ""
    val now = nowTicker()
    val expired = now >= q.lockedUntil
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
            "expired" -> { d.quote = b.requote(q, r.country, fundingId).copy(fundingFeeMinor = b.fundingFee(q.sendMinor, fundingId)); banner.show("Your rate lock ran out", "We've refreshed your quote. Check it and send again.", BannerKind.Info); true }
            "insufficient" -> { banner.show("Not enough in your balance", "Choose another way to pay.", BannerKind.Error); nav.back(); true }
            "outage" -> { nav.go(Routes.SEND_UNAVAILABLE); true }
            else -> { banner.show("Transfer not sent", e.message ?: "", BannerKind.Error); true }
        }
    }) {
        if (!online) throw ArroException("You're offline. Connect to the internet to send money.")
        val tr = b.createTransfer(TransferDraft(r.id, m.id, q, fundingId, d.reason))
        d.transferId = tr.id
        approve = false
        nav.resetTo(Routes.HOME)
        nav.go(Routes.SEND_PROCESSING)
    }
    fun promptBio() {
        val activity = context as? FragmentActivity ?: return
        Biometrics.prompt(activity, "Approve transfer", "Pay ${Money.format(q.totalMinor, Currency.GBP)} to ${r.name}",
            onSuccess = { send() }, onFallback = { usePasscode = true })
    }
    fun startApproval() {
        if (expired) return
        if (!online) { banner.show("You're offline", "Connect to the internet to send money.", BannerKind.Error); return }
        usePasscode = !bioAllowed
        code = ""; codeError = null
        approve = true
        if (bioAllowed) promptBio()
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Review", right = { BarCaption("5 of 5") }) },
        gap = 14.dp,
        footer = {
            if (expired) {
                ArroButton("Get a new quote", {
                    d.quote = b.requote(q, r.country, fundingId).copy(fundingFeeMinor = b.fundingFee(q.sendMinor, fundingId))
                })
            } else {
                ArroButton("Send ${Money.format(q.totalMinor, Currency.GBP)}", { startApproval() }, loading = call.busy.value)
            }
        },
        overlay = {
            PickerSheet(reasonPicker, "Reason for sending", Catalog.reasons, d.reason, { d.reason = it }, { reasonPicker = false })
            Sheet(approve, { if (!call.busy.value) approve = false }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!usePasscode) {
                        Box(Modifier.padding(top = 6.dp).size(132.dp), contentAlignment = Alignment.Center) {
                            SpinnerRing(132.dp)
                            Illustration(Art.FaceScan, 104.dp, Modifier.clip(CircleShape))
                        }
                        Txt("Approve with $BIO_NAME", T.H2.copy(fontSize = 20.sp), Modifier.padding(top = 14.dp), align = TextAlign.Center)
                    } else {
                        Illustration(Art.Lock, 96.dp)
                        Txt("Enter your passcode", T.H2.copy(fontSize = 20.sp), Modifier.padding(top = 10.dp), align = TextAlign.Center)
                    }
                    Txt(buildAnnotatedString {
                        append("Pay ")
                        withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Money.format(q.totalMinor, Currency.GBP)) }
                        append(" from $fundingLabel\nto ${r.name}")
                    }, T.Cap.copy(fontSize = 14.sp), Modifier.padding(top = 6.dp), color = C.Slate, align = TextAlign.Center)
                    if (!usePasscode) {
                        Txt(if (call.busy.value) "Sending" else "Waiting for you", T.Cap.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 18.dp), color = C.Ok)
                        Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArroButton("Try $BIO_NAME again", { promptBio() }, style = Btn.Dark, enabled = !call.busy.value)
                            ArroButton("Use passcode instead", { usePasscode = true }, style = Btn.Line)
                            ArroButton("Cancel", { approve = false }, style = Btn.Text)
                        }
                    } else {
                        PasscodeDots(code.length, error = codeError != null, modifier = Modifier.padding(top = 18.dp))
                        FieldHint(codeError)
                        Keypad({ k ->
                            if (code.length < 6 && !call.busy.value) {
                                code += k; codeError = null
                                if (code.length == 6) scope.launch {
                                    if (b.checkPasscode(code)) send() else { codeError = "That passcode isn't right."; code = "" }
                                }
                            }
                        }, { code = code.dropLast(1) }, Modifier.padding(top = 10.dp), keyHeight = 54.dp)
                        ArroButton("Cancel", { approve = false }, style = Btn.Text, loading = call.busy.value)
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
                if (expired) Pill("Rate lock ended", PillKind.Warn, icon = Icons.Clock)
                else Pill("Rate locked for ${Text.countdown(q.lockedUntil - now)}", PillKind.Neutral, icon = Icons.Lock)
            }
        }
        Panel(gap = 10.dp) {
            KV("You send", Money.format(q.sendMinor, Currency.GBP))
            KV("Arro fee", if (q.feeWaived || q.feeMinor == 0L) "Free" else Money.format(q.feeMinor, Currency.GBP))
            if (q.fundingFeeMinor > 0) KV("Credit card fee", Money.format(q.fundingFeeMinor, Currency.GBP))
            if (m.type != PayoutType.ARRO) KV("Exchange rate", Money.rateLine(Currency.GBP, q.to, q.rate))
            KV("Arrives", when (m.type) {
                PayoutType.BANK -> "1 business day, ${Text.by(arrives, now)}"
                PayoutType.CASH -> "Within the hour, ${Text.by(arrives, now)}"
                PayoutType.ARRO -> "Instantly"
                else -> "In minutes, ${Text.by(arrives, now)}"
            })
            KV("Paying with", fundingLabel)
            HDivider()
            KV("Total to pay", Money.format(q.totalMinor, Currency.GBP), strong = true)
        }
        SelectField("Reason for sending", d.reason, { reasonPicker = true }, height = 50.dp)
        if (expired) Note("Your 30-minute rate lock has ended. Get a new quote to carry on.", NoteKind.Warn, Icons.Clock)
        Cap(if (m.type == PayoutType.ARRO) "Money sent to an Arro user arrives straight away and can't be recalled."
            else "Check the number carefully. Once the money reaches ${r.firstName}'s ${if (m.type == PayoutType.BANK) "account" else "wallet"}, the transfer can't be reversed.", size = 12f)
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
        footer = { ArroButton("Continue in the background", { nav.resetTo(Routes.HOME) }, style = Btn.Line) },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 70.dp).height(240.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.Plane, 200.dp, description = "A paper plane") }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle("Sending your\nmoney", size = TitleSize.XL, align = TextAlign.Center)
            Txt("${Money.format(t.sendMinor, t.fromCur)} to ${t.recipientName}. This only takes a moment.", T.Lede, color = C.Slate, align = TextAlign.Center)
        }
        ArroCard(Modifier.padding(top = 18.dp)) {
            Timeline(listOf(
                TimelineItem("Payment approved", "${Money.format(t.totalMinor, t.fromCur)} from ${t.fundingLabel}", if (now >= t.paidAt) Text.time(t.paidAt) else "",
                    if (now >= t.paidAt) StepState.Done else StepState.Now, if (now >= t.paidAt) null else Icons.Refresh),
                TimelineItem(if (t.payoutType == PayoutType.ARRO) "Checking ${t.firstName}'s account" else "Converted to ${t.to}",
                    if (t.payoutType == PayoutType.ARRO) null else "At ${Money.rate(t.rate)}, as quoted", if (now >= t.convertedAt) Text.time(t.convertedAt) else "",
                    when { now >= t.convertedAt -> StepState.Done; now >= t.paidAt -> StepState.Now; else -> StepState.Todo }, if (now in t.paidAt until t.convertedAt) Icons.Refresh else null),
                TimelineItem("Handing over to ${t.providerName}", null, "",
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
    Box(Modifier.fillMaxSize().background(C.Ink)) {
        Box(Modifier.align(Alignment.TopCenter).offset(y = 60.dp).size(440.dp).background(haloBrush(alpha = 0.2f, stop = 0.65f), CircleShape))
        ArroScreen(
            background = androidx.compose.ui.graphics.Color.Transparent,
            topBar = { TopBar(left = { Spacer(Modifier.width(44.dp)) }, right = { RoundIcon(Icons.Close, { nav.resetTo(Routes.HOME) }, style = IconBtn.OnDark, description = "Close") }) },
            footer = {
                ArroButton("Track transfer", { nav.resetTo(Routes.HOME); nav.go(Routes.track(t.id)) }, style = Btn.Lime)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroButton("Share receipt", { nav.go(Routes.receipt(t.id)) }, Modifier.weight(1f), style = Btn.LineOnDark, icon = Icons.Share, padding = 12.dp)
                    ArroButton(if (t.repeatMonthly) "Repeats monthly" else "Repeat monthly", {
                        val on = !t.repeatMonthly
                        b.setRepeatMonthly(t.id, on)
                        banner.show(if (on) "Repeat set up" else "Repeat cancelled",
                            if (on) "We'll remind you to send ${t.firstName} ${Money.format(t.sendMinor, t.fromCur)} on the ${Text.localDate(t.createdAt).dayOfMonth}${ordinal(Text.localDate(t.createdAt).dayOfMonth)} of each month." else "",
                            BannerKind.Success)
                    }, Modifier.weight(1f), style = Btn.LineOnDark, icon = Icons.Refresh, padding = 12.dp)
                }
            },
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.Plane, 200.dp, description = "A paper plane taking off") }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                app.arro.ui.components.Over("Money sent", color = C.Lime)
                Txt(Money.format(t.receiveMinor, t.toCur), T.Amount.copy(fontSize = 46.sp), Modifier.padding(top = 4.dp), color = C.Lime, align = TextAlign.Center)
                Txt(buildAnnotatedString {
                    append(if (t.payoutType == PayoutType.ARRO) "has arrived with " else "is on its way to ")
                    withStyle(SpanStyle(color = C.White, fontWeight = FontWeight.SemiBold)) { append(t.recipientName) }
                }, T.Lede, Modifier.padding(top = 6.dp), color = C.DarkCap, align = TextAlign.Center)
            }
            Column(
                Modifier.padding(top = 10.dp).fillMaxWidth().background(C.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .border(1.dp, C.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkLine("Expected", if (t.status == TransferStatus.DELIVERED) "Delivered at ${Text.time(t.deliveredAt)}" else Text.by(t.deliveredAt, now))
                DarkLine("Paid", "${Money.format(t.totalMinor, t.fromCur)} · ${t.fundingLabel.replace(" debit", "").replace(" credit", "")}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Cap("Reference", color = C.DarkCap)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(t.reference, T.Num.copy(fontSize = 15.sp), color = C.White)
                        RoundIcon(Icons.Copy, { Share.copy(context, "Arro reference", t.reference); banner.show("Reference copied", t.reference, BannerKind.Success) },
                            style = IconBtn.OnDark, size = 32.dp, iconSize = 16.dp, description = "Copy reference")
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
        Txt(v, T.Num.copy(fontSize = 15.sp), color = C.White, align = TextAlign.End)
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Send to ${country.name}") }) },
        footer = {
            ArroButton("Choose another way", { nav.popBackStack(); nav.go(Routes.SEND_DELIVERY) })
            ArroButton("Notify me when it's back", {
                banner.show("We'll let you know", "You'll get a notification when ${down?.name ?: "it"} is working again.", BannerKind.Success)
            }, style = Btn.Line, icon = Icons.Bell)
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.Clock, 128.dp, description = "A stopwatch") }
        ScreenTitle("${down?.name ?: "This provider"} is\nbriefly offline", "Planned maintenance until about ${Text.time(backBy)}. Your money is safe, and other ways to send are working.", align = TextAlign.Center)
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
                    trailing = { if (isDown) Pill("Back by ${Text.time(backBy)}", PillKind.Warn, dot = true) else Pill("Working", PillKind.Ok, dot = true) })
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
        topBar = { TopBar(onBack = { nav.back() }, right = { RoundIcon(Icons.Scan, { nav.go(Routes.SCAN_QR) }, style = IconBtn.Soft, description = "Scan a QR code") }) },
        footer = {
            ArroButton(if (found != null) "Send to ${found!!.name.split(" ").first()}" else "Find", {
                val f = found
                if (f != null) sendTo(f)
                else scope.launchCall(call) { found = b.findArroUser(query) }
            }, loading = call.busy.value, enabled = query.isNotBlank() || found != null)
        },
    ) {
        ScreenTitle("Send to an\nArro user", "Instant and free between Arro accounts, in pounds.")
        ArroField(query, { query = it; found = null; call.error.value = null }, label = "Username or mobile number", placeholder = "@aline or 07700 900456",
            keyboard = KeyboardType.Email, error = call.error.value, onDone = { scope.launchCall(call) { found = b.findArroUser(query) } })
        found?.let { u ->
            ListRow(u.name, subtitle = "@${u.handle} · ${Text.maskPhone(u.phone)}", selected = true,
                leading = { Avatar(Text.initials(u.name), 3, flag = "gb") }, trailing = { Pill("On Arro", PillKind.Lime) }, onClick = { sendTo(u) })
        }
        if (existing.isNotEmpty()) {
            Over("Your Arro contacts")
            existing.forEach { r ->
                ListRow(r.name, subtitle = r.defaultMethod?.account, leading = { Avatar(r.initials, r.color, flag = "gb") }, trailing = { Chevron() },
                    onClick = { startSend(c, nav, banner, recipient = r) })
            }
        }
        Note("Try @aline, @amani, @kofi or @ngozi to see how it works.", NoteKind.Lime, Icons.Info)
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
        if (handle == null) { banner.show("That's not an Arro code", "Scan the QR code in someone's Arro app.", BannerKind.Error); return }
        handled = true
        scope.launch {
            runCatching { b.findArroUser(handle) }
                .onSuccess { u -> startSend(c, nav, banner, recipient = b.addArroRecipient(u)) }
                .onFailure { banner.show("Code not recognised", it.message ?: "", BannerKind.Error); delay(1500); handled = false }
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
    CameraScreen("Scan a QR code", { nav.back() }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(130.dp))
            Txt("Point at someone's Arro code", T.Cap.copy(fontSize = 15.sp), color = C.White)
            Box(Modifier.padding(top = 28.dp).size(260.dp).clip(RoundedCornerShape(22.dp)).border(3.dp, C.Lime, RoundedCornerShape(22.dp))) {
                if (permission.granted && cameraOk) CameraPreview(Modifier.fillMaxSize(), analyzer = analyzer)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ic(Icons.Scan, size = 64.dp, tint = C.DarkCap) }
            }
            if (!permission.granted && cameraOk) ArroButton("Allow camera", { permission.request() }, Modifier.padding(top = 16.dp), style = Btn.Lime,
                size = app.arro.ui.components.BtnSize.M, fill = false)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ArroButton("Show my code", { showMine = true }, style = Btn.Lime, icon = Icons.Scan)
                ArroButton("Enter a username instead", { nav.go(Routes.SEND_ARRO) }, style = Btn.LineOnDark)
            }
        }
        Sheet(showMine, { showMine = false }) {
            SheetHeader("My Arro code", { showMine = false }, "Anyone with Arro can scan this to pay you instantly.")
            val bmp = remember(a.username) { qrBitmap("arro://pay/${a.username}", 560) }
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(bmp.asImageBitmap(), "QR code for @${a.username}", Modifier.size(240.dp))
                Txt("@${a.username}", T.H2)
                Cap(a.displayName)
            }
            ArroButton("Share my code", { Share.text(context, "Pay me on Arro: arro.app/pay/${a.username}") }, Modifier.padding(top = 18.dp), style = Btn.Line, icon = Icons.Share)
        }
    }
}

/** Renders [text] as a QR code bitmap with ZXing. */
fun qrBitmap(text: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) 0xFF042D29.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
