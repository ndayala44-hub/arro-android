package app.arro.ui.screens.payments

import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.ArroException
import app.arro.core.CardBrand
import app.arro.core.Catalog
import app.arro.core.MethodKind
import app.arro.core.MethodStatus
import app.arro.core.PaymentMethod
import app.arro.core.Text
import app.arro.core.Validation
import app.arro.platform.CameraPreview
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
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.CheckRow
import app.arro.ui.components.Chevron
import app.arro.ui.components.CodeInput
import app.arro.ui.components.ConfirmDialog
import app.arro.ui.components.FieldHint
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Group
import app.arro.ui.components.GroupingTransformation
import app.arro.ui.components.Halo
import app.arro.ui.components.HDivider
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SearchField
import app.arro.ui.components.SectionHeader
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.SpinnerRing
import app.arro.ui.components.StepState
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.Timeline
import app.arro.ui.components.TimelineItem
import app.arro.ui.components.TitleSize
import app.arro.ui.components.Toggle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.screens.kyc.CameraScreen
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Small card thumbnail (48 x 32) in the issuer's colours. */
@Composable
fun CardThumb(m: PaymentMethod, modifier: Modifier = Modifier) {
    val colors = when {
        m.kind == MethodKind.BANK -> listOf(C.Forest, C.Ink)
        m.brand == CardBrand.MASTERCARD -> listOf(Color(0xFF2C6FB7), Color(0xFF164E8A))
        else -> listOf(Color(0xFFFF6B57), Color(0xFFE24A3B))
    }
    Box(modifier.size(48.dp, 32.dp).background(Brush.linearGradient(colors), RoundedCornerShape(6.dp))) {
        Txt("•• ${m.last4}", T.Pill.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp), Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp), color = C.White)
    }
}

// ------------------------------------------------------------------ 7.01 Payment methods

@Composable
fun PaymentMethodsScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    var options by remember { mutableStateOf<PaymentMethod?>(null) }
    var confirmRemove by remember { mutableStateOf<PaymentMethod?>(null) }
    val cards = a.paymentMethods.filter { it.kind == MethodKind.CARD }
    val banks = a.paymentMethods.filter { it.kind == MethodKind.BANK }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        overlay = {
            val m = options
            Sheet(m != null, { options = null }) {
                if (m != null) {
                    SheetHeader(m.label, { options = null }, if (m.kind == MethodKind.CARD) "${m.issuer} · expires ${m.expiry}" else "${m.issuer} · Pay by bank")
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (m.status == MethodStatus.PENDING) ArroButton("Finish adding this card", { options = null; c.session.pendingCardId = m.id; nav.go(Routes.PAY_3DS) })
                        else if (!m.isDefault) ArroButton("Make default", { b.setDefaultMethod(m.id); options = null; banner.show("${m.shortLabel} is now your default", kind = BannerKind.Success) })
                        ArroButton(if (m.kind == MethodKind.CARD) "Remove card" else "Disconnect bank", { confirmRemove = m; options = null }, style = Btn.DangerText)
                    }
                }
            }
            val r = confirmRemove
            ConfirmDialog(r != null, if (r?.kind == MethodKind.BANK) "Disconnect ${r.issuer}?" else "Remove ${r?.label ?: ""}?",
                "You can add it again at any time. Transfers already paid aren't affected.", if (r?.kind == MethodKind.BANK) "Disconnect" else "Remove",
                { r?.let { b.removeMethod(it.id); banner.show("Removed", it.label, BannerKind.Info) }; confirmRemove = null }, { confirmRemove = null }, danger = true)
        },
    ) {
        ScreenTitle("Payment methods", "How you pay for transfers and top-ups.")
        Column {
            SectionHeader("Cards")
            if (cards.isNotEmpty()) Group {
                cards.forEachIndexed { i, m ->
                    if (i > 0) GroupDivider()
                    ListRow(
                        "${m.brand.label} ${if (m.credit) "credit" else "debit"}", flat = true,
                        subtitle = when {
                            m.status == MethodStatus.PENDING -> "${m.issuer} · waiting for your bank's approval"
                            m.credit -> "${m.issuer} · 1.75% card fee"
                            else -> "${m.issuer} · expires ${m.expiry}"
                        },
                        leading = { CardThumb(m) },
                        trailing = { RoundIcon(Icons.More, { options = m }, style = IconBtn.Soft, size = 36.dp, iconSize = 16.dp, description = "Card options") },
                        onClick = { options = m },
                        content = {
                            if (m.isDefault && m.status == MethodStatus.ACTIVE) Pill("Default", PillKind.Lime, modifier = Modifier.padding(top = 4.dp))
                            if (m.status == MethodStatus.PENDING) Pill("Not finished", PillKind.Warn, modifier = Modifier.padding(top = 4.dp))
                        },
                    )
                }
            }
            ListRow("Add a debit or credit card", Modifier.padding(top = 10.dp), dashed = true, minHeight = 54.dp,
                leading = { Tile(Icons.Plus, size = 36.dp, iconSize = 16.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.PAY_ADD_CARD) })
        }
        Column {
            SectionHeader("Bank accounts")
            if (banks.isNotEmpty()) Group {
                banks.forEachIndexed { i, m ->
                    if (i > 0) GroupDivider()
                    ListRow("${m.issuer} current •• ${m.last4}", flat = true, subtitle = "Pay by bank · connected ${Text.dayMonth(m.addedAt)}",
                        leading = { Tile(Icons.Bank) }, trailing = { Pill("Active", PillKind.Ok, dot = true) }, onClick = { options = m })
                }
            }
            ListRow("Link a bank account", Modifier.padding(top = 10.dp), dashed = true, minHeight = 54.dp,
                leading = { Tile(Icons.Plus, size = 36.dp, iconSize = 16.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.PAY_BANKS) })
        }
        Note("Your full card number is stored by our certified payment partner, never on this phone.", NoteKind.Lime, Icons.ShieldCheck)
    }
}

// ------------------------------------------------------------------ 7.02 Add card

@Composable
fun AddCardScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val state by appState()
    val a = state.account ?: return
    var number by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var cvv by rememberSaveable { mutableStateOf("") }
    var billingHome by rememberSaveable { mutableStateOf(true) }
    var billingPostcode by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    LaunchedEffect(c.session.scannedCard) {
        c.session.scannedCard?.let { (n, e) -> number = n; if (e.isNotEmpty()) expiry = e; c.session.scannedCard = null }
    }
    val brand = Validation.brand(number)
    val brandLabel = if (brand == CardBrand.UNKNOWN) "" else "${brand.label} ${if (app.arro.core.Seed.isCreditBin(number.filter { it.isDigit() })) "credit" else "debit"}"
    fun submit() = scope.launchCall(call, onError = { e ->
        if (e.code in setOf("number", "expiry", "cvv")) { errors = mapOf(e.code to (e.message ?: "")); true } else false
    }) {
        if (!billingHome && !Validation.isUkPostcode(billingPostcode)) throw ArroException("Enter the postcode your card statements go to.")
        val m = b.addCard(number, expiry, cvv, a.displayName, false)
        c.session.pendingCardId = m.id
        nav.go(Routes.PAY_3DS)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { ArroButton("Scan card", { nav.go(Routes.PAY_SCAN) }, style = Btn.Lime, size = BtnSize.S, icon = Icons.Scan, fill = false) }) },
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Ic(Icons.Lock, size = 14.dp, tint = C.Slate); Cap(" Encrypted and processed by a PCI DSS certified partner", size = 12f)
            }
            ArroButton("Add card", { submit() }, loading = call.busy.value, enabled = number.isNotBlank() && expiry.length == 4 && cvv.length >= 3)
        },
    ) {
        ScreenTitle("Card in your name")
        val digits = number.filter { it.isDigit() }
        CardPreview((digits + "•".repeat((16 - digits.length).coerceAtLeast(0))).chunked(4).joinToString(" "), expiry, a.displayName.uppercase(), brandLabel)
        ArroField(number, { v -> number = v.filter { it.isDigit() }.take(19); errors = errors - "number" }, label = "Card number",
            placeholder = "1234 5678 9012 3456", keyboard = KeyboardType.Number, error = errors["number"],
            textStyle = T.Input.copy(fontFeatureSettings = "tnum", letterSpacing = 0.6.sp),
            visualTransformation = GroupingTransformation(if (brand == CardBrand.AMEX) listOf(4, 6, 5) else listOf(4, 4, 4, 4, 3)),
            trailing = { if (brandLabel.isNotEmpty()) Pill(brandLabel, if (brand == CardBrand.AMEX) PillKind.Warn else PillKind.Ok) })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArroField(expiry, { v -> expiry = v.filter { it.isDigit() }.take(4); errors = errors - "expiry" }, Modifier.weight(1f), label = "Expiry date",
                placeholder = "MM / YY", keyboard = KeyboardType.Number, error = errors["expiry"],
                visualTransformation = GroupingTransformation(listOf(2, 2), '/'), textStyle = T.Input.copy(fontFeatureSettings = "tnum"))
            ArroField(cvv, { v -> cvv = v.filter { it.isDigit() }.take(4); errors = errors - "cvv" }, Modifier.weight(1f), label = "Security code",
                placeholder = "123", keyboard = KeyboardType.NumberPassword, password = true, error = errors["cvv"])
        }
        CheckRow(billingHome, { billingHome = it }, "Billing address is my home address",
            sub = if (billingHome) a.profile.address?.oneLine else null)
        if (!billingHome) ArroField(billingPostcode, { billingPostcode = it.uppercase() }, label = "Billing postcode", placeholder = "E8 2PL")
        FieldHint(call.error.value)
        Note("Test cards: any valid Visa or Mastercard number works. A number ending 0002 is declined by the bank.", NoteKind.Info, Icons.Info)
    }
}

@Composable
private fun CardPreview(shown: String, expiry: String, holder: String, brandLabel: String) {
    Box(
        Modifier.fillMaxWidth().height(196.dp)
            .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = C.Ink.copy(alpha = 0.25f), spotColor = C.Ink.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(C.Ink2, C.Ink, Color(0xFF021E1B)))),
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(40.dp, (-60).dp).size(220.dp).background(Brush.radialGradient(listOf(C.Lime.copy(alpha = 0.22f), C.Lime.copy(alpha = 0f))), CircleShape))
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp, 30.dp).background(Brush.linearGradient(listOf(Color(0xFFE9D9A8), Color(0xFFBFA761))), RoundedCornerShape(6.dp)))
                Txt(brandLabel.ifEmpty { "Debit or credit card" }, T.Cap.copy(fontWeight = FontWeight.SemiBold), color = C.Lime)
            }
            Spacer(Modifier.weight(1f))
            Txt(shown, T.Big22.copy(fontSize = 21.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp), color = C.White, maxLines = 1)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Txt(holder, T.Cap.copy(letterSpacing = 1.sp), color = C.DarkCap, maxLines = 1)
                Txt(if (expiry.length >= 2) expiry.take(2) + "/" + expiry.drop(2) else "MM/YY", T.Cap.copy(fontFeatureSettings = "tnum"), color = C.DarkCap)
            }
        }
    }
}

// ------------------------------------------------------------------ 7.03 Scan card

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScanCardScreen() {
    val nav = LocalNav.current
    val c = container()
    val context = LocalContext.current
    val permission = rememberCameraPermission()
    val cameraOk = hasCamera(context, front = false)
    var torch by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<String?>(null) }
    var foundExpiry by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (!permission.granted && cameraOk) permission.request() }
    LaunchedEffect(found, foundExpiry) {
        val n = found ?: return@LaunchedEffect
        delay(if (foundExpiry != null) 700 else 2500)
        c.session.scannedCard = n to (foundExpiry ?: "")
        nav.back()
    }
    val analyzer = remember {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val numberRe = Regex("(?:\\d[ -]?){15,16}")
        val expiryRe = Regex("\\b(0[1-9]|1[0-2])\\s?/\\s?(\\d{2})\\b")
        ImageAnalysis.Analyzer { proxy ->
            val media = proxy.image
            if (media == null) { proxy.close(); return@Analyzer }
            recognizer.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { text ->
                    val all = text.text
                    if (found == null) numberRe.findAll(all).map { m -> m.value.filter { it.isDigit() } }.firstOrNull { Validation.luhn(it) }?.let { found = it }
                    if (foundExpiry == null) expiryRe.find(all)?.let { foundExpiry = it.groupValues[1] + it.groupValues[2] }
                }
                .addOnCompleteListener { proxy.close() }
        }
    }
    CameraScreen("Scan your card", { nav.back() }, right = {
        RoundIcon(Icons.Bolt, { torch = !torch }, style = if (torch) IconBtn.Lime else IconBtn.OnDark, description = "Torch")
    }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(150.dp))
            Txt("Hold the front of your card inside the frame", T.Cap.copy(fontSize = 15.sp), color = C.White, align = TextAlign.Center)
            Box(Modifier.padding(top = 30.dp).size(338.dp, 220.dp)) {
                if (permission.granted && cameraOk) {
                    CameraPreview(Modifier.padding(6.dp).fillMaxSize().clip(RoundedCornerShape(16.dp)), analyzer = analyzer, torch = torch)
                } else {
                    Box(Modifier.padding(12.dp).fillMaxSize().rotate(1.5f).shadow(14.dp, RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF6B57), Color(0xFFE24A3B))), RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Box(Modifier.size(40.dp, 30.dp).background(Brush.linearGradient(listOf(Color(0xFFE9D9A8), Color(0xFFBFA761))), RoundedCornerShape(6.dp)))
                        Txt("•••• •••• •••• ••••", T.Big22.copy(letterSpacing = 2.5.sp, fontWeight = FontWeight.Medium), Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp), color = C.White)
                    }
                }
                FrameCornersCard()
            }
            Column(Modifier.padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val f = found
                if (f != null) {
                    Pill(Validation.formatCard(f), PillKind.Lime)
                    Cap(if (foundExpiry != null) "Card number and expiry found" else "Card number found. Checking expiry date", color = C.DarkCap)
                } else {
                    Cap(if (permission.granted) "Looking for the card number" else "Allow the camera to scan your card", color = C.DarkCap)
                    if (!permission.granted && cameraOk) ArroButton("Allow camera", { permission.request() }, style = Btn.Lime, size = BtnSize.M, fill = false)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Cap("The image stays on your phone. Only the card details are sent, encrypted.", Modifier.fillMaxWidth(), color = C.DarkCap, align = TextAlign.Center, size = 12f)
                ArroButton("Enter details manually", { nav.back() }, style = Btn.LineOnDark)
            }
        }
    }
}

@Composable
private fun FrameCornersCard() {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val w = 4.dp.toPx(); val r = 18.dp.toPx(); val ext = 12.dp.toPx(); val h = w / 2
        val s = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val sz = androidx.compose.ui.geometry.Size(r * 2, r * 2)
        drawArc(C.Lime, 180f, 90f, false, androidx.compose.ui.geometry.Offset(h, h), sz, style = s)
        drawArc(C.Lime, 270f, 90f, false, androidx.compose.ui.geometry.Offset(size.width - 2 * r - h, h), sz, style = s)
        drawArc(C.Lime, 90f, 90f, false, androidx.compose.ui.geometry.Offset(h, size.height - 2 * r - h), sz, style = s)
        drawArc(C.Lime, 0f, 90f, false, androidx.compose.ui.geometry.Offset(size.width - 2 * r - h, size.height - 2 * r - h), sz, style = s)
        listOf(
            Triple(h + r, h, true), Triple(size.width - h - r - ext, h, true),
            Triple(h + r, size.height - h, true), Triple(size.width - h - r - ext, size.height - h, true),
        ).forEach { (x, y, _) -> drawLine(C.Lime, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x + ext, y), w, androidx.compose.ui.graphics.StrokeCap.Round) }
        listOf(Pair(h, h + r), Pair(size.width - h, h + r), Pair(h, size.height - h - r - ext), Pair(size.width - h, size.height - h - r - ext)).forEach { (x, y) ->
            drawLine(C.Lime, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x, y + ext), w, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

// ------------------------------------------------------------------ 7.04 Choose your bank

@Composable
fun BankListScreen() {
    val nav = LocalNav.current
    val state by appState()
    val a = state.account ?: return
    var query by rememberSaveable { mutableStateOf("") }
    val used = a.paymentMethods.filter { it.kind == MethodKind.BANK }.map { it.bankId }.toSet()
    val q = query.trim().lowercase()
    val list = Catalog.ukBanks.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }, gap = 14.dp) {
        ScreenTitle("Choose your bank", "Connect with Open Banking. You approve in your bank's app, and we never see your login.")
        SearchField(query, { query = it }, "Search for your bank")
        Over(if (q.isEmpty()) "Most popular" else "Banks")
        if (list.isEmpty()) Cap("We don't support that bank for Open Banking yet. You can add a debit card instead.")
        Group {
            list.forEachIndexed { i, bank ->
                if (i > 0) GroupDivider()
                val mine = bank.id in used
                ListRow(bank.name, flat = true, minHeight = 56.dp, subtitle = if (mine) "Already connected" else if (bank.id == "monzo" && a.demo) "You use Monzo for top-ups" else null,
                    leading = {
                        Box(Modifier.size(36.dp).background(if (mine || (bank.id == "monzo" && a.demo)) C.Lime else C.Tint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Txt(bank.name.take(1).uppercase(), T.H3.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    },
                    trailing = { Chevron() }, onClick = { nav.go(Routes.payConnect(bank.id)) })
            }
        }
    }
}

// ------------------------------------------------------------------ 7.05 Connecting bank

@Composable
fun BankConnectScreen(bankId: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val state by appState()
    val a = state.account ?: return
    val bank = Catalog.ukBank(bankId) ?: return
    var step by rememberSaveable { mutableIntStateOf(0) }
    var methodId by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (methodId != null) return@LaunchedEffect
        delay(900); step = 1
        delay(1600); step = 2
        try {
            methodId = b.connectBank(bankId).id; step = 3
        } catch (e: ArroException) { error = e.message }
    }
    ArroScreen(
        background = C.Tint,
        topBar = { TopBar(onBack = { nav.back() }) },
        footer = {
            ArroButton("Continue", {
                methodId?.let { id -> nav.navigate(Routes.payAdded(id)) { popUpTo(Routes.PAY_BANKS) { inclusive = true } } }
            }, enabled = step == 3, loading = step in 1..2 && error == null)
            if (error != null) ArroButton("Try again", { nav.back() }, style = Btn.Line)
        },
    ) {
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) { Halo(200.dp) { Illustration(Art.Bank, 200.dp, description = "A glass bank building") } }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(if (step == 3) "Connected to ${bank.name}" else "Connecting to ${bank.name}")
            Txt(when (step) {
                0 -> "Opening a secure connection to ${bank.name}."
                1, 2 -> "You approved access in the ${bank.name} app. We're finishing up."
                else -> "Your ${bank.name} account is ready to pay for transfers."
            }, T.Lede, color = C.Slate)
        }
        ArroCard(Modifier.padding(top = 8.dp)) {
            Timeline(listOf(
                TimelineItem("Secure connection opened", "Regulated Open Banking provider", state = if (step >= 1) StepState.Done else StepState.Now, icon = if (step >= 1) null else Icons.Refresh),
                TimelineItem("Approved in ${bank.name}", "With your ${bank.name} PIN", state = when { step >= 2 -> StepState.Done; step == 1 -> StepState.Now; else -> StepState.Todo }, icon = if (step == 1) Icons.Refresh else null),
                TimelineItem("Confirming the account is yours", "Matching the name to ${a.displayName}", state = when { step >= 3 -> StepState.Done; step == 2 -> StepState.Now; else -> StepState.Todo }, icon = if (step == 2) Icons.Refresh else null),
            ))
        }
        Note("Arro sees your account name and number only. Disconnect anytime in Payment methods.", NoteKind.Lime, Icons.Eye)
        FieldHint(error)
    }
}

// ------------------------------------------------------------------ 7.06 Card authorisation (3-D Secure)

@Composable
fun CardApprovalScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val m = a.paymentMethod(c.session.pendingCardId)
    if (m == null) {
        // Removed (declined or cancelled): leave, unless we've already navigated away.
        LaunchedEffect(Unit) { if (nav.currentDestination?.route == Routes.PAY_3DS) nav.back() }
        return
    }
    val started = remember { System.currentTimeMillis() }
    val now = nowTicker()
    var smsMode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    fun approve() = scope.launchCall(call, onError = { e -> if (e.code == "declined") { banner.show("Card declined", e.message ?: "", BannerKind.Error); true } else false }) {
        val done = b.approveCard(m.id)
        nav.popBackStack(Routes.PAY_ADD_CARD, inclusive = true)
        nav.go(Routes.payAdded(done.id))
    }
    // The "bank app" asks for approval a few seconds in, like a push notification from the issuer.
    LaunchedEffect(Unit) {
        delay(3500)
        banner.show("${m.issuer}: is this you?", "Approve adding ${m.label} to Arro.", BannerKind.Info, "Approve") { approve() }
    }
    val remaining = (5 * 60_000L - (now - started)).coerceAtLeast(0)
    LaunchedEffect(remaining == 0L) { if (remaining == 0L) { banner.show("Approval timed out", "Add the card again when you're ready.", BannerKind.Error); b.cancelPendingMethod(m.id) } }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back(); b.cancelPendingMethod(m.id) }) },
        overlay = {
            Sheet(true, { nav.back(); b.cancelPendingMethod(m.id) }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Illustration(Art.Phone, 112.dp)
                    Txt(if (smsMode) "Enter the code from ${m.issuer}" else "Approve in your ${m.issuer} app", T.H2.copy(fontSize = 21.sp), Modifier.padding(top = 10.dp), align = TextAlign.Center)
                    Txt(if (smsMode) "We've asked ${m.issuer} to text a code to your mobile ending ${a.phone.takeLast(3)}."
                        else "${m.issuer} sent you a notification to confirm it's really you adding ${m.label}. No money will be taken.",
                        T.Cap.copy(fontSize = 14.sp), Modifier.padding(top = 6.dp), color = C.Slate, align = TextAlign.Center)
                    if (smsMode) {
                        CodeInput(code, { code = it }, Modifier.padding(top = 18.dp), onComplete = { approve() })
                    } else {
                        Row(
                            Modifier.padding(top = 18.dp).background(C.Tint, RoundedCornerShape(999.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SpinnerRing(20.dp, stroke = 3.dp, track = C.Mist, color = C.Ink)
                            Txt("Waiting for approval · ${Text.countdown(remaining)}", T.Body14.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                    FieldHint(call.error.value)
                    Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArroButton(if (smsMode) "Confirm code" else "I've approved it", { approve() }, loading = call.busy.value, enabled = !smsMode || code.length == 6)
                        ArroButton(if (smsMode) "Approve in the app instead" else "Get a code by SMS instead", { smsMode = !smsMode }, style = Btn.Line)
                        ArroButton("Cancel", { nav.back(); b.cancelPendingMethod(m.id) }, style = Btn.Text)
                    }
                }
            }
        },
    ) {
        ScreenTitle("Card in your name")
        CardPreview("•••• •••• •••• ${m.last4}", m.expiry.replace("/", ""), m.holder, m.label.substringBefore(" ••"))
    }
}

// ------------------------------------------------------------------ 7.07 Method added

@Composable
fun MethodAddedScreen(id: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val state by appState()
    val a = state.account ?: return
    val m = a.paymentMethod(id) ?: return
    val isCard = m.kind == MethodKind.CARD
    fun done() {
        if (c.session.cardReturnToSend) {
            c.session.cardReturnToSend = false
            c.session.send.fundingId = m.id
            if (!nav.popBackStack(Routes.SEND_FUNDING, inclusive = false)) nav.go(Routes.HOME)
        } else if (!nav.popBackStack(Routes.PAY_METHODS, inclusive = false)) {
            nav.popBackStack(Routes.HOME, inclusive = false)
        }
    }
    ArroScreen(
        background = C.Tint,
        footer = {
            if (c.session.cardReturnToSend) ArroButton("Back to my transfer", { done() })
            ArroButton("Done", {
                c.session.cardReturnToSend = false
                if (!nav.popBackStack(Routes.PAY_METHODS, inclusive = false)) nav.popBackStack(Routes.HOME, inclusive = false)
            }, style = if (c.session.cardReturnToSend) Btn.Text else Btn.Dark)
        },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 60.dp).height(260.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.Wallet, 200.dp, description = "A glass wallet with coins") }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(if (isCard) "Card added" else "Bank connected", size = TitleSize.XL, align = TextAlign.Center)
            Txt("${m.label} is ready to pay for transfers and top-ups.", T.Lede, color = C.Slate, align = TextAlign.Center)
        }
        ArroCard(Modifier.padding(top = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isCard) CardThumb(m) else Tile(Icons.Bank)
                Column(Modifier.weight(1f)) {
                    Txt(m.label, T.H3)
                    Cap(if (isCard) "${m.issuer} · expires ${m.expiry}" else "Pay by bank")
                }
                Pill("Verified", PillKind.Ok, dot = true)
            }
            HDivider(Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt("Use as my default", T.Body14)
                Toggle(m.isDefault, { on -> if (on) b.setDefaultMethod(m.id) else b.setDefaultMethod(a.paymentMethods.firstOrNull { it.id != m.id && it.status == MethodStatus.ACTIVE }?.id ?: m.id) })
            }
        }
    }
}

