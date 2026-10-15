package app.arro.ui.screens.kyc

import android.graphics.BitmapFactory
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.Address
import app.arro.core.Catalog
import app.arro.core.DocType
import app.arro.core.KycStatus
import app.arro.core.Text
import app.arro.core.Validation
import app.arro.platform.CameraPreview
import app.arro.platform.PhotoQuality
import app.arro.platform.hasCamera
import app.arro.platform.rememberCameraPermission
import app.arro.platform.rememberPhotoPicker
import app.arro.platform.takePhoto
import app.arro.ui.BannerKind
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.account
import app.arro.ui.back
import app.arro.ui.backend
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.launchCall
import app.arro.ui.nowTicker
import app.arro.ui.rememberCall
import app.arro.ui.resetTo
import app.arro.ui.components.ArroButton
import app.arro.ui.components.DarkSystemBars
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.BarCaption
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.FieldHint
import app.arro.ui.components.FieldLabel
import app.arro.ui.components.Flag
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Halo
import app.arro.ui.components.Ic
import app.arro.ui.components.IconBtn
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.KV
import app.arro.ui.components.LinkText
import app.arro.ui.components.ListRow
import app.arro.ui.components.MaxContentWidth
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.Radio
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SelectField
import app.arro.ui.components.StepBars
import app.arro.ui.components.StepState
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.Timeline
import app.arro.ui.components.TimelineItem
import app.arro.ui.components.TitleSize
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.VSpace
import app.arro.ui.components.haloBrush
import app.arro.ui.components.tap
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private fun docFile(dir: File, n: Int = 0) = File(File(dir, "kyc").apply { mkdirs() }, "document_$n.jpg")

// ------------------------------------------------------------------ 2.01 Intro

@Composable
fun KycIntroScreen() {
    val nav = LocalNav.current
    val a = account() ?: return
    val p = a.profile
    val step = when {
        p.dob.isBlank() -> 1
        p.address == null -> 2
        else -> 3
    }
    if (a.kyc.status == KycStatus.IN_REVIEW || a.kyc.status == KycStatus.APPROVED) {
        LaunchedEffect(Unit) { nav.resetTo(Routes.KYC_STATUS) }
        return
    }
    ArroScreen(
        background = C.Tint,
        topBar = {
            TopBar(onBack = { if (!nav.popBackStack()) nav.resetTo(Routes.HOME) }, right = {
                ArroButton("Do this later", { nav.resetTo(Routes.HOME) }, style = Btn.Text, size = BtnSize.S, fill = false)
            })
        },
        footer = {
            ArroButton(if (step == 1) "Start verification" else "Continue verification", {
                nav.go(when (step) { 1 -> Routes.KYC_PERSONAL; 2 -> Routes.KYC_ADDRESS; else -> Routes.KYC_DOCUMENT })
            })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Ic(Icons.Lock, size = 14.dp, tint = C.Slate); Cap(" Encrypted and used only to verify you", size = 12f)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.IdCard, 168.dp, description = "An ID card with a verified badge") }
        ScreenTitle("Let's confirm\nit's you", "The law requires us to verify your identity before you send money abroad. It takes about 3 minutes.")
        Group {
            val items = listOf(
                "Personal details" to "Name, date of birth, nationality",
                "Home address" to "Find it by postcode",
                "Photo of your ID" to "Passport, driving licence or ID card",
                "A quick selfie" to "Matched once against your ID photo",
            )
            items.forEachIndexed { i, (t, s) ->
                if (i > 0) GroupDivider()
                ListRow(t, subtitle = s, flat = true, leading = { NumberTile(i + 1, done = i + 1 < step, current = i + 1 == step) })
            }
        }
    }
}

@Composable
private fun NumberTile(n: Int, done: Boolean, current: Boolean) {
    Box(Modifier.size(32.dp).background(if (current) C.Lime else if (done) C.Ink else C.Tint, CircleShape), contentAlignment = Alignment.Center) {
        if (done) Ic(Icons.Check, size = 16.dp, tint = C.Lime) else Txt("$n", T.H3.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}

// ------------------------------------------------------------------ 2.02 Personal details

@Composable
fun KycPersonalScreen() {
    val nav = LocalNav.current
    val b = backend()
    val a = account() ?: return
    val p = a.profile
    var first by rememberSaveable { mutableStateOf(p.firstNames) }
    var last by rememberSaveable { mutableStateOf(p.lastName) }
    val dobParts = p.dob.split("-")
    var day by rememberSaveable { mutableStateOf(dobParts.getOrNull(2) ?: "") }
    var month by rememberSaveable { mutableStateOf(dobParts.getOrNull(1) ?: "") }
    var year by rememberSaveable { mutableStateOf(dobParts.getOrNull(0) ?: "") }
    var nationality by rememberSaveable { mutableStateOf(p.nationality.ifBlank { "British" }) }
    var occupation by rememberSaveable { mutableStateOf(p.occupation) }
    var picker by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String?>()) }
    fun submit() {
        val (iso, dobErr) = Validation.dob(day, month, year, Text.localDate(System.currentTimeMillis()))
        val e = mapOf(
            "first" to Validation.personName(first, "first name"),
            "last" to Validation.personName(last, "last name"),
            "dob" to dobErr,
            "occupation" to if (occupation.isBlank()) "Choose your occupation." else null,
        )
        errors = e
        if (e.values.any { it != null } || iso == null) return
        b.savePersonal(first, last, iso, nationality, occupation)
        nav.go(Routes.KYC_ADDRESS)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("1 of 4 · Details") }) },
        footer = { ArroButton("Continue", { submit() }) },
        overlay = {
            PickerSheet(picker == "nat", "Nationality", Catalog.nationalities, nationality, { nationality = it }, { picker = "" },
                leading = { o -> Flag(flagForNationality(o)) })
            PickerSheet(picker == "occ", "Occupation", Catalog.occupations, occupation, { occupation = it; errors = errors - "occupation" }, { picker = "" })
        },
    ) {
        StepBars(1, 4)
        ScreenTitle("Your details", "Enter them exactly as they appear on your ID.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArroField(first, { first = it }, Modifier.weight(1f), label = "First and middle names", error = errors["first"],
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            ArroField(last, { last = it }, Modifier.weight(1f), label = "Last name", error = errors["last"],
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Date of birth")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val centered = T.Input.copy(textAlign = TextAlign.Center, fontFeatureSettings = "tnum")
                ArroField(day, { day = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), placeholder = "DD", keyboard = KeyboardType.Number, textStyle = centered)
                ArroField(month, { month = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), placeholder = "MM", keyboard = KeyboardType.Number, textStyle = centered)
                ArroField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.weight(1.5f), placeholder = "YYYY", keyboard = KeyboardType.Number, textStyle = centered)
            }
            FieldHint(errors["dob"])
        }
        SelectField("Nationality", nationality, { picker = "nat" }, leading = { Flag(flagForNationality(nationality)) })
        SelectField("Occupation", occupation.ifBlank { null }, { picker = "occ" }, error = errors["occupation"],
            hint = "We ask because regulations require us to know how you earn the money you send.")
    }
}

private fun flagForNationality(n: String) = when (n) {
    "British" -> "gb"; "Rwandan" -> "rw"; "Kenyan" -> "ke"; "Ugandan" -> "ug"; "Nigerian" -> "ng"; "Ghanaian" -> "gh"
    "Indian" -> "in"; "Filipino" -> "ph"; "Irish", "French" -> "eu"; else -> ""
}

// ------------------------------------------------------------------ 2.03 Address

@Composable
fun KycAddressScreen() {
    val nav = LocalNav.current
    val b = backend()
    val a = account() ?: return
    val existing = a.profile.address
    var postcode by rememberSaveable { mutableStateOf(existing?.postcode ?: "") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var line1 by rememberSaveable { mutableStateOf(existing?.line1 ?: "") }
    var city by rememberSaveable { mutableStateOf(existing?.city ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val results = remember(postcode) { Catalog.addressesFor(postcode) }
    fun submit() {
        val addr = if (manual) {
            error = when {
                line1.isBlank() -> "Enter the first line of your address."
                city.isBlank() -> "Enter your town or city."
                else -> Validation.postcode(postcode)
            }
            if (error != null) return
            Address(line1.trim(), city.trim(), Validation.normalisePostcode(postcode))
        } else {
            if (results.isEmpty()) { error = Validation.postcode(postcode) ?: "We couldn't find that postcode. Enter your address manually."; return }
            results[selected.coerceIn(0, results.lastIndex)]
        }
        b.saveAddress(addr)
        nav.go(Routes.KYC_DOCUMENT)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("2 of 4 · Address") }) },
        footer = {
            Cap("We may confirm your address with a credit reference agency. It won't affect your credit score.", size = 12f)
            ArroButton("Continue", { submit() })
        },
    ) {
        StepBars(2, 4)
        ScreenTitle("Home address", "Where you live now. We'll send nothing by post.")
        ArroField(postcode, { postcode = it.uppercase().take(8); selected = 0; error = null }, label = "Postcode", placeholder = "E8 2PL",
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
            prefix = { Ic(Icons.Search, tint = C.Slate) }, error = if (!manual) error else null)
        if (!manual) {
            if (results.isNotEmpty()) {
                Cap("${results.size} addresses found")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEachIndexed { i, ad ->
                        ListRow(ad.line1, subtitle = "${ad.city} ${ad.postcode}", selected = i == selected, minHeight = 56.dp,
                            leading = { Radio(i == selected) }, onClick = { selected = i })
                    }
                }
            } else if (postcode.length >= 5) {
                Cap("No addresses found for that postcode yet.")
            }
            LinkText("Enter address manually", { manual = true; error = null }, color = C.Ink, strong = true)
        } else {
            ArroField(line1, { line1 = it }, label = "Address line 1", placeholder = "Flat 3, 41 Kestrel Road",
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            ArroField(city, { city = it }, label = "Town or city", placeholder = "London",
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            FieldHint(error)
            LinkText("Find it by postcode instead", { manual = false; error = null }, color = C.Ink, strong = true)
        }
    }
}

// ------------------------------------------------------------------ 2.04 Choose document

@Composable
fun KycDocumentScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val context = LocalContext.current
    val draft = c.session.kyc
    var picker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val upload = rememberPhotoPicker({ docFile(context.filesDir) }) { f ->
        scope.launch {
            val q = PhotoQuality.of(f)
            draft.photoPath = f.absolutePath
            draft.quality = q.score
            draft.qualityNotes = qualityNotes(q)
            nav.go(Routes.KYC_CHECK)
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("3 of 4 · ID") }) },
        footer = {
            ArroButton("Take photo", { b.chooseDocument(draft.docType, draft.docCountry); nav.go(Routes.KYC_CAPTURE) })
            ArroButton("Upload a photo instead", { b.chooseDocument(draft.docType, draft.docCountry); upload() }, style = Btn.Text)
        },
        overlay = {
            PickerSheet(picker, "Issuing country", Catalog.issuingCountries, draft.docCountry, { draft.docCountry = it }, { picker = false })
        },
    ) {
        StepBars(3, 4)
        ScreenTitle("Choose your ID", "It must be in date and show your photo.")
        SelectField("Issuing country", draft.docCountry, { picker = true }, leading = { Flag(countryFlag(draft.docCountry)) })
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Catalog.docTypes.forEach { (type, labels) ->
                val on = draft.docType == type
                ListRow(labels.first, subtitle = labels.second, selected = on, onClick = { draft.docType = type },
                    leading = { Tile(if (type == DocType.PASSPORT) Icons.Passport else Icons.IdCardPerson, if (on) TileStyle.Lime else TileStyle.Tint) },
                    trailing = { Radio(on) })
            }
        }
        Note("Find a well-lit spot and lay your ID on a dark, plain surface.", NoteKind.Lime, Icons.Camera)
    }
}

private fun countryFlag(name: String) = when (name) {
    "United Kingdom" -> "gb"; "Rwanda" -> "rw"; "Kenya" -> "ke"; "Uganda" -> "ug"; "Nigeria" -> "ng"; "Ghana" -> "gh"
    "India" -> "in"; "Philippines" -> "ph"; "Ireland" -> "eu"; else -> ""
}

private fun qualityNotes(q: PhotoQuality) = listOf(
    "All four corners are visible" to true,
    (if (q.sharp) "Text and photo are in focus" else "The photo looks blurry") to q.sharp,
    (if (q.noGlare && q.wellLit) "No glare or shadows" else if (!q.noGlare) "There's glare on the photo" else "The photo is too dark") to (q.noGlare && q.wellLit),
)

// ------------------------------------------------------------------ 2.05 Document capture

/** Dark camera backdrop with soft radial light, used by every camera screen. */
@Composable
fun CameraScreen(
    title: String,
    onClose: () -> Unit,
    closeIcon: Int = Icons.Close,
    right: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    DarkSystemBars()
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF2A3A36), Color(0xFF0E1A18), C.Camera), radius = 1400f)).tap { },
    ) {
        Box(Modifier.align(Alignment.TopCenter).widthIn(max = MaxContentWidth).fillMaxSize()) {
            content()
            TopBar(
                left = { RoundIcon(closeIcon, onClose, style = IconBtn.OnDark, description = "Close camera") },
                title = title, titleColor = C.White,
                right = { if (right != null) right() else Spacer(Modifier.width(44.dp)) },
            )
        }
    }
}

@Composable
fun KycCaptureScreen() {
    val nav = LocalNav.current
    val c = container()
    val context = LocalContext.current
    val banner = LocalBanner.current
    val draft = c.session.kyc
    val permission = rememberCameraPermission()
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var torch by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var side by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val cameraOk = hasCamera(context, front = false)
    LaunchedEffect(Unit) { if (!permission.granted && cameraOk) permission.request() }
    val twoSided = draft.docType.sides == 2
    val docName = when (draft.docType) { DocType.PASSPORT -> "Passport photo page"; DocType.DRIVING_LICENCE -> "Driving licence"; DocType.ID_CARD -> "ID card" }
    val title = if (twoSided) "$docName · ${if (side == 0) "front" else "back"}" else docName
    fun finish(f: File) = scope.launch {
        val q = PhotoQuality.of(f)
        if (twoSided && side == 0) {
            side = 1; busy = false
            banner.show("Front captured", "Now turn it over and capture the back.", BannerKind.Success)
            return@launch
        }
        draft.photoPath = f.absolutePath
        draft.quality = q.score
        draft.qualityNotes = qualityNotes(q)
        busy = false
        nav.go(Routes.KYC_CHECK)
    }
    val upload = rememberPhotoPicker({ docFile(context.filesDir, side) }) { f -> busy = true; finish(f) }
    CameraScreen(title, { nav.back() }, right = {
        RoundIcon(Icons.Bolt, { torch = !torch }, style = if (torch) IconBtn.Lime else IconBtn.OnDark, description = "Flash")
    }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(120.dp))
            Txt(if (twoSided && side == 1) "Fit the back inside the frame" else "Fit the ${if (draft.docType == DocType.PASSPORT) "photo page" else "front"} inside the frame",
                T.Cap.copy(fontSize = 15.sp), color = C.White, align = TextAlign.Center)
            Box(Modifier.padding(top = 28.dp).size(338.dp, 236.dp)) {
                if (permission.granted && cameraOk) {
                    CameraPreview(Modifier.padding(6.dp).fillMaxSize().clip(RoundedCornerShape(14.dp)), capture = capture, torch = torch,
                        onError = { banner.show("Camera problem", it, BannerKind.Error) })
                } else {
                    MockDocument(Modifier.padding(14.dp).fillMaxSize().rotate(-1.2f))
                }
                FrameCorners()
            }
            Row(Modifier.padding(top = 24.dp)) {
                Pill(if (busy) "Hold still, capturing" else if (permission.granted) "Ready when you are" else "Camera access needed", PillKind.Lime,
                    icon = if (busy || permission.granted) Icons.Check else Icons.Camera)
            }
            Column(Modifier.padding(top = 28.dp, start = 36.dp, end = 36.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CamTip("All four corners in view"); CamTip("No glare on the photo")
            }
            if (!permission.granted && cameraOk) {
                ArroButton("Allow camera", { permission.request() }, Modifier.padding(top = 16.dp), style = Btn.Lime, size = BtnSize.M, fill = false)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 40.dp, end = 40.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Upload", T.Link.copy(fontSize = 14.sp), Modifier.width(64.dp).tap { upload() }, color = C.White)
                Box(
                    Modifier.size(78.dp).border(4.dp, C.White, CircleShape).padding(8.dp).background(if (busy) C.LimeDeep else C.Lime, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !busy) {
                            busy = true
                            if (permission.granted && cameraOk) {
                                takePhoto(context, capture, docFile(context.filesDir, side), { finish(it) }, { busy = false; banner.show("Couldn't take the photo", it, BannerKind.Error) })
                            } else {
                                // No camera: use the sample image so the flow can still be tested.
                                scope.launch { delay(600); finish(sampleDocument(context)) }
                            }
                        },
                )
                Txt("Tips", T.Link.copy(fontSize = 14.sp), Modifier.width(64.dp).tap { nav.go(Routes.article("verify")) }, color = C.White, align = TextAlign.End)
            }
        }
    }
}

@Composable
private fun CamTip(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Ic(Icons.Check, size = 16.dp, tint = C.Lime); Cap(text, color = C.DarkCap)
    }
}

/** Lime corner brackets around the capture frame (.corner). */
@Composable
private fun FrameCorners(color: Color = C.Lime) {
    Canvas(Modifier.fillMaxSize()) {
        val w = 4.dp.toPx(); val len = 28.dp.toPx(); val r = 18.dp.toPx()
        val s = Stroke(w, cap = StrokeCap.Round)
        val half = w / 2
        // each corner: an arc plus two short lines
        drawArc(color, 180f, 90f, false, Offset(half, half), Size(r * 2, r * 2), style = s)
        drawArc(color, 270f, 90f, false, Offset(size.width - r * 2 - half, half), Size(r * 2, r * 2), style = s)
        drawArc(color, 90f, 90f, false, Offset(half, size.height - r * 2 - half), Size(r * 2, r * 2), style = s)
        drawArc(color, 0f, 90f, false, Offset(size.width - r * 2 - half, size.height - r * 2 - half), Size(r * 2, r * 2), style = s)
        val ext = len - r
        drawLine(color, Offset(half + r, half), Offset(half + r + ext, half), w, StrokeCap.Round)
        drawLine(color, Offset(half, half + r), Offset(half, half + r + ext), w, StrokeCap.Round)
        drawLine(color, Offset(size.width - half - r, half), Offset(size.width - half - r - ext, half), w, StrokeCap.Round)
        drawLine(color, Offset(size.width - half, half + r), Offset(size.width - half, half + r + ext), w, StrokeCap.Round)
        drawLine(color, Offset(half + r, size.height - half), Offset(half + r + ext, size.height - half), w, StrokeCap.Round)
        drawLine(color, Offset(half, size.height - half - r), Offset(half, size.height - half - r - ext), w, StrokeCap.Round)
        drawLine(color, Offset(size.width - half - r, size.height - half), Offset(size.width - half - r - ext, size.height - half), w, StrokeCap.Round)
        drawLine(color, Offset(size.width - half, size.height - half - r), Offset(size.width - half, size.height - half - r - ext), w, StrokeCap.Round)
    }
}

/** The illustrated passport page from the designs, used when there's no photo yet. */
@Composable
fun MockDocument(modifier: Modifier = Modifier) {
    Box(
        modifier.shadow(12.dp, RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF2EEE4), Color(0xFFE3DCCB))), RoundedCornerShape(12.dp)).padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(74.dp, 92.dp).clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(listOf(Color(0xFFC9C2B2), Color(0xFFA89F8C)))),
                contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.size(46.dp, 54.dp).background(Color(0xFF6E6656), RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp)))
            }
            Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.6f, 0.8f, 0.45f, 0.7f, 0.55f).forEachIndexed { i, f ->
                    Box(Modifier.fillMaxWidth(f).height(7.dp).background(if (i == 0) Color(0xFFB7AE9A) else Color(0xFFC5BCA8), RoundedCornerShape(3.dp)))
                }
            }
        }
        Txt("P<GBRNSHUTI<<ERIC<<<<<<<<<<<<<<\n5384019276GBR9107032M3105122<<<<",
            T.Cap.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, letterSpacing = 0.8.sp, lineHeight = 12.sp),
            Modifier.align(Alignment.BottomStart), color = Color(0xFF5B5446), maxLines = 2)
    }
}

/** Writes a plain sample image for devices without a camera, so the flow can be completed. */
private fun sampleDocument(context: android.content.Context): File {
    val f = docFile(context.filesDir, 9)
    val bmp = android.graphics.Bitmap.createBitmap(640, 440, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(0xFFE9E3D5.toInt())
    val paint = android.graphics.Paint().apply { color = 0xFF6E6656.toInt(); textSize = 26f; isAntiAlias = true; typeface = android.graphics.Typeface.MONOSPACE }
    canvas.drawRect(28f, 28f, 176f, 212f, paint.apply { color = 0xFFA89F8C.toInt() })
    paint.color = 0xFF5B5446.toInt()
    for (i in 0 until 5) canvas.drawRect(200f, 40f + i * 26f, 200f + 260f - i * 30f, 52f + i * 26f, paint)
    canvas.drawText("P<GBRNSHUTI<<ERIC<<<<<<<<<<<<<<", 28f, 380f, paint)
    canvas.drawText("5384019276GBR9107032M3105122<<<<", 28f, 414f, paint)
    f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
    return f
}

// ------------------------------------------------------------------ 2.06 Photo check

@Composable
fun KycCheckScreen() {
    val nav = LocalNav.current
    val c = container()
    val draft = c.session.kyc
    val bitmap = remember(draft.photoPath) { draft.photoPath?.let { p -> PhotoQuality.decodeSmall(File(p), 900) } }
    val good = draft.quality >= 0.67f
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("3 of 4 · ID") }) },
        gap = 20.dp,
        footer = {
            ArroButton("Use this photo", { nav.go(Routes.KYC_SELFIE) })
            ArroButton("Retake", { nav.back() }, style = Btn.Line)
        },
    ) {
        StepBars(3, 4)
        ScreenTitle("Is it easy to read?", "Check every detail is sharp before you send it.")
        Box(Modifier.fillMaxWidth().background(Color(0xFF1A2624), RoundedCornerShape(20.dp)).padding(18.dp)) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), "Your ID photo", Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            } else {
                MockDocument(Modifier.fillMaxWidth().height(196.dp))
            }
            Pill(if (good) "Looks good" else "Hard to read", if (good) PillKind.Lime else PillKind.Warn, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val notes = draft.qualityNotes.ifEmpty { listOf("All four corners are visible" to true, "Text and photo are in focus" to true, "No glare or shadows" to true) }
            notes.forEach { (text, ok) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(if (ok) Icons.Check else Icons.Alert, if (ok) TileStyle.Lime else TileStyle.Warn, size = 28.dp, round = true, iconSize = 16.dp)
                    Txt(text, T.Body14)
                }
            }
        }
        if (!good) Note("We may not be able to read this. A retake in better light usually helps.", NoteKind.Warn, Icons.Info)
    }
}

// ------------------------------------------------------------------ 2.07 Selfie with liveness

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun KycSelfieScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val context = LocalContext.current
    val banner = LocalBanner.current
    val draft = c.session.kyc
    val permission = rememberCameraPermission()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val cameraOk = hasCamera(context, front = true)
    var step by remember { mutableIntStateOf(0) } // 0 looking for face, 1 face found, 2 turned (done)
    var faceSeen by remember { mutableStateOf(false) }
    val started = remember { System.currentTimeMillis() }
    val now = nowTicker(500)
    LaunchedEffect(Unit) { if (!permission.granted && cameraOk) permission.request() }
    val analyzer = remember {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build(),
        )
        ImageAnalysis.Analyzer { proxy ->
            val media = proxy.image
            if (media == null) { proxy.close(); return@Analyzer }
            val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
            detector.process(input)
                .addOnSuccessListener { faces ->
                    val f = faces.firstOrNull()
                    if (f != null) {
                        faceSeen = true
                        val yaw = f.headEulerAngleY
                        if (step == 0 && abs(yaw) < 12f) step = 1
                        else if (step == 1 && abs(yaw) > 24f) step = 2
                    }
                }
                .addOnCompleteListener { proxy.close() }
        }
    }
    fun submit() = scope.launchCall(call) {
        draft.selfieDone = true
        b.submitIdentity(draft.quality.takeIf { draft.photoPath != null } ?: 0.9f)
        nav.resetTo(Routes.KYC_STATUS)
    }
    LaunchedEffect(step) { if (step == 2) { delay(500); submit() } }
    val stuck = now - started > 12_000 && step < 2
    CameraScreen("4 of 4 · Selfie", { nav.back() }, closeIcon = Icons.ArrowLeft) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(124.dp))
            Txt(if (step == 0) "Look straight ahead" else if (step == 1) "Now turn your head" else "Got it, thank you", T.H2.copy(fontSize = 22.sp), color = C.White)
            Cap(if (step == 0) "Then slowly turn your head to the left" else if (step == 1) "Slowly, to the left" else "Checking it matches your ID", color = C.DarkCap, size = 15f)
            Box(Modifier.padding(top = 30.dp).size(268.dp, 340.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(28.dp).fillMaxSize().clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                }).background(Brush.verticalGradient(listOf(Color(0xFF4B5A55), Color(0xFF2E3B37))))) {
                    if (permission.granted && cameraOk) CameraPreview(Modifier.fillMaxSize(), front = true, analyzer = analyzer)
                    else {
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 30.dp).size(108.dp, 132.dp).background(Brush.verticalGradient(listOf(Color(0xFF7D6A5C), Color(0xFF5E4D41))), CircleShape))
                        Box(Modifier.align(Alignment.BottomCenter).size(196.dp, 92.dp).background(Brush.verticalGradient(listOf(C.Forest, Color(0xFF1E3B31))), RoundedCornerShape(topStart = 98.dp, topEnd = 98.dp)))
                    }
                }
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 8.dp.toPx()
                    val rect = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx())
                    val tl = Offset(12.dp.toPx(), 12.dp.toPx())
                    drawOval(Color.White.copy(alpha = 0.18f), tl, rect, style = Stroke(sw))
                    val sweep = when (step) { 0 -> if (faceSeen) 70f else 20f; 1 -> 190f; else -> 360f }
                    drawArc(C.Lime, -90f, sweep, false, tl, rect, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
            Row(Modifier.padding(top = 24.dp)) {
                Pill(when (step) { 0 -> if (faceSeen) "Step 1 of 2 · hold still" else "Step 1 of 2 · finding your face"; 1 -> "Step 2 of 2 · turn left"; else -> "Done" }, PillKind.OnDark)
            }
            if (!permission.granted && cameraOk) ArroButton("Allow camera", { permission.request() }, Modifier.padding(top = 16.dp), style = Btn.Lime, size = BtnSize.M, fill = false)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Cap("Your selfie is compared once with your ID photo, then deleted after verification.", Modifier.fillMaxWidth(), color = C.DarkCap, align = TextAlign.Center, size = 12f)
                val canSkip = !permission.granted || !cameraOk || stuck
                ArroButton(if (step == 2) "Checking" else if (canSkip) "I'm ready" else "Keep still", { submit() }, style = Btn.Lime,
                    enabled = step == 2 || canSkip, loading = call.busy.value)
                if (call.error.value != null) FieldHint(call.error.value)
            }
        }
    }
    LaunchedEffect(permission.denied) { if (permission.denied) banner.show("Camera is off", "You can still continue; we'll check your selfie later.", BannerKind.Info) }
}

// ------------------------------------------------------------------ 2.08 to 2.10 Review, approved, rejected

@Composable
fun KycStatusScreen() {
    val nav = LocalNav.current
    val c = container()
    val a = account() ?: return
    val now = nowTicker()
    val k = a.kyc
    when (k.status) {
        KycStatus.NOT_STARTED -> LaunchedEffect(Unit) { nav.resetTo(Routes.KYC) }
        KycStatus.IN_REVIEW -> {
            val total = (k.decideAt - k.submittedAt).coerceAtLeast(1)
            val f = ((now - k.submittedAt).toFloat() / total).coerceIn(0f, 1f)
            ArroScreen(
                background = C.Tint,
                footer = { ArroButton("Explore Arro while you wait", { nav.resetTo(Routes.HOME) }) },
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp).height(220.dp), contentAlignment = Alignment.Center) {
                    Halo(220.dp) { Illustration(Art.Clock, 200.dp, description = "A stopwatch") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScreenTitle("Checking your\ndetails", size = TitleSize.XL)
                    Txt("Most checks finish in under 2 minutes. Feel free to look around, we'll notify you when it's done.", T.Lede, color = C.Slate)
                }
                ArroCard(Modifier.padding(top = 8.dp)) {
                    val doc = k.docType.label
                    Timeline(listOf(
                        TimelineItem("Personal details", time = "Received", state = StepState.Done),
                        TimelineItem(doc, time = "Received", state = StepState.Done),
                        TimelineItem("Matching your selfie", if (f < 0.6f) "Comparing it with your ${doc.lowercase()} photo" else null,
                            state = if (f < 0.6f) StepState.Now else StepState.Done, icon = if (f < 0.6f) Icons.Refresh else null,
                            time = if (f < 0.6f) "" else "Done"),
                        TimelineItem("Final checks", if (f >= 0.6f) "Sanctions and fraud screening" else null,
                            state = if (f >= 0.6f) StepState.Now else StepState.Todo, icon = if (f >= 0.6f) Icons.Refresh else null),
                    ))
                }
            }
        }
        KycStatus.APPROVED -> {
            val level = k.level
            DarkSystemBars()
            Box(Modifier.fillMaxSize().background(C.Ink)) {
                Box(Modifier.align(Alignment.TopCenter).offset(y = 80.dp).size(420.dp).background(haloBrush(alpha = 0.22f, stop = 0.65f), CircleShape))
                ArroScreen(
                    background = Color.Transparent,
                    footer = {
                        ArroButton("Send your first transfer", { c.session.send.reset(); nav.resetTo(Routes.HOME); nav.go(Routes.SEND_COUNTRY) }, style = Btn.Lime)
                        ArroButton("Go to home", { nav.resetTo(Routes.HOME) }, style = Btn.LineOnDark)
                    },
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 70.dp), contentAlignment = Alignment.Center) { Illustration(Art.IdCard, 240.dp, description = "An ID card with a verified badge") }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScreenTitle("You're verified", size = TitleSize.XL, color = C.Lime, align = TextAlign.Center)
                        Txt("Welcome to Arro, ${a.firstName}. Your account is ready to send.", T.Lede, color = C.DarkCap, align = TextAlign.Center)
                    }
                    Column(
                        Modifier.padding(top = 8.dp).fillMaxWidth().background(C.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .border(1.dp, C.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Cap("Account level", color = C.DarkCap); Pill(level.label, PillKind.Lime)
                        }
                        KV("Per transfer", "up to " + app.arro.core.Money.format(level.perTransferMinor, app.arro.core.Currency.GBP, false), onDark = true)
                        KV("Per month", "up to " + app.arro.core.Money.format(level.perMonthMinor, app.arro.core.Currency.GBP, false), onDark = true)
                        LinkText("How to raise your limits", { nav.go(Routes.VERIFICATION) }, color = C.Lime, strong = true)
                    }
                }
            }
        }
        KycStatus.REJECTED -> ArroScreen(
            topBar = {
                TopBar(left = { RoundIcon(Icons.Close, { nav.resetTo(Routes.HOME) }, style = IconBtn.Soft, description = "Close") }, right = {
                    ArroButton("Get help", { nav.go(Routes.CHAT) }, style = Btn.Text, size = BtnSize.S, fill = false)
                })
            },
            footer = {
                ArroButton("Retake ${k.docType.label.lowercase()} photo", {
                    c.backend.retakeIdentity(); nav.resetTo(Routes.HOME); nav.go(Routes.KYC_DOCUMENT); nav.go(Routes.KYC_CAPTURE)
                }, enabled = k.attemptsLeft > 0)
                ArroButton("Use a different ID", { c.backend.retakeIdentity(); nav.resetTo(Routes.HOME); nav.go(Routes.KYC_DOCUMENT) }, style = Btn.Line, enabled = k.attemptsLeft > 0)
            },
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.IdCardWarn, 168.dp, description = "An ID card with an alert badge") }
            ScreenTitle("We need a\nclearer photo", k.rejectReason.ifBlank { "We couldn't read your ID. A retake usually fixes it." })
            ArroCard {
                Over("Tips for a quick pass")
                VSpace(12.dp)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Icons.Bolt to "Turn your flash off", Icons.Sun to "Tilt the ${k.docType.label.lowercase()} away from lights", Icons.Square to "Use a dark, plain background").forEach { (icon, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tile(icon, TileStyle.Warn, size = 28.dp, round = true, iconSize = 16.dp); Txt(text, T.Body14)
                        }
                    }
                }
            }
            Cap(if (k.attemptsLeft > 0) "${k.attemptsLeft} attempt${if (k.attemptsLeft == 1) "" else "s"} left today. Your details and selfie are saved."
                else "You've used today's attempts. Try again tomorrow, or chat with us.")
        }
    }
}
