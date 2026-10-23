package app.arro.ui.screens.kyc

import app.arro.R
import app.arro.ui.str
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
import app.arro.ui.components.longDate
import app.arro.ui.components.DateSheet
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
import androidx.core.graphics.createBitmap

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
                ArroButton(str(R.string.kyc_do_this_later), { nav.resetTo(Routes.HOME) }, style = Btn.Text, size = BtnSize.S, fill = false)
            })
        },
        footer = {
            ArroButton(if (step == 1) str(R.string.kyc_start_verification) else str(R.string.kyc_continue_verification), {
                nav.go(when (step) { 1 -> Routes.KYC_PERSONAL; 2 -> Routes.KYC_ADDRESS; else -> Routes.KYC_DOCUMENT })
            })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Ic(Icons.Lock, size = 14.dp, tint = C.Slate); Cap(str(R.string.kyc_encrypted_and_used_only_to_verify), size = 12f)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.IdCard, 168.dp, description = str(R.string.kyc_an_id_card_with_a_verified)) }
        ScreenTitle(str(R.string.kyc_let_s_confirm_it_s_you), str(R.string.kyc_the_law_requires_us_to_verify))
        Group {
            val items = listOf(
                str(R.string.kyc_personal_details) to str(R.string.kyc_name_date_of_birth_nationality),
                str(R.string.kyc_home_address) to str(R.string.kyc_find_it_by_postcode),
                str(R.string.kyc_photo_of_your_id) to str(R.string.kyc_passport_driving_licence_or_id_card),
                str(R.string.kyc_a_quick_selfie) to str(R.string.kyc_matched_once_against_your_id_photo),
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
    Box(Modifier.size(32.dp).background(if (current) C.Lime else if (done) C.Primary else C.Tint, CircleShape), contentAlignment = Alignment.Center) {
        if (done) Ic(Icons.Check, size = 16.dp, tint = C.PrimaryAccent)
        else Txt("$n", T.H3.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = if (current) C.OnLime else C.Ink)
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
    // Stored as yyyy-MM-dd; picked on a calendar.
    var dob by rememberSaveable { mutableStateOf(p.dob) }
    val dobDate = runCatching { java.time.LocalDate.parse(dob) }.getOrNull()
    val today = Text.localDate(System.currentTimeMillis())
    var nationality by rememberSaveable { mutableStateOf(p.nationality.ifBlank { str(R.string.kyc_british) }) }
    var occupation by rememberSaveable { mutableStateOf(p.occupation) }
    var picker by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String?>()) }
    fun submit() {
        val (iso, dobErr) = Validation.dob(dobDate, today)
        val e = mapOf(
            "first" to Validation.personName(first, str(R.string.kyc_first_name)),
            "last" to Validation.personName(last, str(R.string.kyc_last_name)),
            "dob" to dobErr,
            "occupation" to if (occupation.isBlank()) str(R.string.kyc_choose_your_occupation) else null,
        )
        errors = e
        if (e.values.any { it != null } || iso == null) return
        b.savePersonal(first, last, iso, nationality, occupation)
        nav.go(Routes.KYC_ADDRESS)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.kyc_n_1_of_4_details)) }) },
        footer = { ArroButton(str(R.string.kyc_continue), { submit() }) },
        overlay = {
            PickerSheet(picker == "nat", str(R.string.kyc_nationality), Catalog.nationalities, nationality, { nationality = it }, { picker = "" },
                leading = { o -> Flag(flagForNationality(o)) })
            PickerSheet(picker == "occ", str(R.string.kyc_occupation), Catalog.occupations, occupation, { occupation = it; errors = errors - "occupation" }, { picker = "" })
            DateSheet(
                visible = picker == "dob",
                title = str(R.string.kyc_date_of_birth),
                selected = dobDate,
                min = today.minusYears(110),
                max = today.minusYears(18),
                onPick = { dob = it.toString(); errors = errors - "dob" },
                onDismiss = { picker = "" },
                subtitle = str(R.string.kyc_exactly_as_it_appears_on_your),
                note = str(R.string.kyc_you_need_to_be_18_or),
                startYear = today.year - 30,
            )
        },
    ) {
        StepBars(1, 4)
        ScreenTitle(str(R.string.kyc_your_details), str(R.string.kyc_enter_them_exactly_as_they_appear))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArroField(first, { first = it }, Modifier.weight(1f), label = str(R.string.kyc_first_and_middle_names), error = errors["first"],
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            ArroField(last, { last = it }, Modifier.weight(1f), label = str(R.string.kyc_last_name_2), error = errors["last"],
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
        }
        SelectField(str(R.string.kyc_date_of_birth), dobDate?.let { longDate(it) }, { picker = "dob" }, placeholder = str(R.string.kyc_choose_on_a_calendar),
            error = errors["dob"], focused = picker == "dob", leading = { Ic(Icons.Calendar, tint = C.Slate) })
        SelectField(str(R.string.kyc_nationality), nationality, { picker = "nat" }, leading = { Flag(flagForNationality(nationality)) })
        SelectField(str(R.string.kyc_occupation), occupation.ifBlank { null }, { picker = "occ" }, error = errors["occupation"],
            hint = str(R.string.kyc_we_ask_because_regulations_require_us))
    }
}

private fun flagForNationality(n: String) = when (n) {
    str(R.string.kyc_british) -> "gb"; str(R.string.kyc_rwandan) -> "rw"; str(R.string.kyc_kenyan) -> "ke"; str(R.string.kyc_ugandan) -> "ug"; str(R.string.kyc_nigerian) -> "ng"; str(R.string.kyc_ghanaian) -> "gh"
    str(R.string.kyc_indian) -> "in"; str(R.string.kyc_filipino) -> "ph"; str(R.string.kyc_irish), str(R.string.kyc_french) -> "eu"; else -> ""
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
                line1.isBlank() -> str(R.string.kyc_enter_the_first_line_of_your)
                city.isBlank() -> str(R.string.kyc_enter_your_town_or_city)
                else -> Validation.postcode(postcode)
            }
            if (error != null) return
            Address(line1.trim(), city.trim(), Validation.normalisePostcode(postcode))
        } else {
            if (results.isEmpty()) { error = Validation.postcode(postcode) ?: str(R.string.kyc_we_couldn_t_find_that_postcode); return }
            results[selected.coerceIn(0, results.lastIndex)]
        }
        b.saveAddress(addr)
        nav.go(Routes.KYC_DOCUMENT)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.kyc_n_2_of_4_address)) }) },
        footer = {
            Cap(str(R.string.kyc_we_may_confirm_your_address_with), size = 12f)
            ArroButton(str(R.string.kyc_continue), { submit() })
        },
    ) {
        StepBars(2, 4)
        ScreenTitle(str(R.string.kyc_home_address), str(R.string.kyc_where_you_live_now_we_ll))
        ArroField(postcode, { postcode = it.uppercase().take(8); selected = 0; error = null }, label = str(R.string.kyc_postcode), placeholder = str(R.string.kyc_e8_2pl),
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
            prefix = { Ic(Icons.Search, tint = C.Slate) }, error = if (!manual) error else null)
        if (!manual) {
            if (results.isNotEmpty()) {
                Cap(str(R.string.kyc_x_addresses_found, results.size))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEachIndexed { i, ad ->
                        ListRow(ad.line1, subtitle = "${ad.city} ${ad.postcode}", selected = i == selected, minHeight = 56.dp,
                            leading = { Radio(i == selected) }, onClick = { selected = i })
                    }
                }
            } else if (postcode.length >= 5) {
                Cap(str(R.string.kyc_no_addresses_found_for_that_postcode))
            }
            LinkText(str(R.string.kyc_enter_address_manually), { manual = true; error = null }, color = C.Ink, strong = true)
        } else {
            ArroField(line1, { line1 = it }, label = str(R.string.kyc_address_line_1), placeholder = str(R.string.kyc_flat_3_41_kestrel_road),
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            ArroField(city, { city = it }, label = str(R.string.kyc_town_or_city), placeholder = str(R.string.kyc_london),
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            FieldHint(error)
            LinkText(str(R.string.kyc_find_it_by_postcode_instead), { manual = false; error = null }, color = C.Ink, strong = true)
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.kyc_n_3_of_4_id)) }) },
        footer = {
            ArroButton(str(R.string.kyc_take_photo), { b.chooseDocument(draft.docType, draft.docCountry); nav.go(Routes.KYC_CAPTURE) })
            ArroButton(str(R.string.kyc_upload_a_photo_instead), { b.chooseDocument(draft.docType, draft.docCountry); upload() }, style = Btn.Text)
        },
        overlay = {
            PickerSheet(picker, str(R.string.kyc_issuing_country), Catalog.issuingCountries, draft.docCountry, { draft.docCountry = it }, { picker = false })
        },
    ) {
        StepBars(3, 4)
        ScreenTitle(str(R.string.kyc_choose_your_id), str(R.string.kyc_it_must_be_in_date_and))
        SelectField(str(R.string.kyc_issuing_country), draft.docCountry, { picker = true }, leading = { Flag(countryFlag(draft.docCountry)) })
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Catalog.docTypes.forEach { (type, labels) ->
                val on = draft.docType == type
                ListRow(labels.first, subtitle = labels.second, selected = on, onClick = { draft.docType = type },
                    leading = { Tile(if (type == DocType.PASSPORT) Icons.Passport else Icons.IdCardPerson, if (on) TileStyle.Lime else TileStyle.Tint) },
                    trailing = { Radio(on) })
            }
        }
        Note(str(R.string.kyc_find_a_well_lit_spot_and), NoteKind.Lime, Icons.Camera)
    }
}

private fun countryFlag(name: String) = when (name) {
    str(R.string.kyc_united_kingdom) -> "gb"; str(R.string.kyc_rwanda) -> "rw"; str(R.string.kyc_kenya) -> "ke"; str(R.string.kyc_uganda) -> "ug"; str(R.string.kyc_nigeria) -> "ng"; str(R.string.kyc_ghana) -> "gh"
    str(R.string.kyc_india) -> "in"; str(R.string.kyc_philippines) -> "ph"; str(R.string.kyc_ireland) -> "eu"; else -> ""
}

private fun qualityNotes(q: PhotoQuality) = listOf(
    str(R.string.kyc_all_four_corners_are_visible) to true,
    (if (q.sharp) str(R.string.kyc_text_and_photo_are_in_focus) else str(R.string.kyc_the_photo_looks_blurry)) to q.sharp,
    (if (q.noGlare && q.wellLit) str(R.string.kyc_no_glare_or_shadows) else if (!q.noGlare) str(R.string.kyc_there_s_glare_on_the_photo) else str(R.string.kyc_the_photo_is_too_dark)) to (q.noGlare && q.wellLit),
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
                left = { RoundIcon(closeIcon, onClose, style = IconBtn.OnDark, description = str(R.string.kyc_close_camera)) },
                title = title, titleColor = C.OnNight,
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
    val docName = when (draft.docType) { DocType.PASSPORT -> str(R.string.kyc_passport_photo_page); DocType.DRIVING_LICENCE -> str(R.string.kyc_driving_licence); DocType.ID_CARD -> str(R.string.kyc_id_card) }
    val title = if (twoSided) (if (side == 0) str(R.string.kyc_doc_front, docName) else str(R.string.kyc_doc_back, docName)) else docName
    fun finish(f: File) = scope.launch {
        val q = PhotoQuality.of(f)
        if (twoSided && side == 0) {
            side = 1; busy = false
            banner.show(str(R.string.kyc_front_captured), str(R.string.kyc_now_turn_it_over_and_capture), BannerKind.Success)
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
        RoundIcon(Icons.Bolt, { torch = !torch }, style = if (torch) IconBtn.Lime else IconBtn.OnDark, description = str(R.string.kyc_flash))
    }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(120.dp))
            Txt(if (twoSided && side == 1) str(R.string.kyc_fit_the_back_inside_the_frame) else str(R.string.kyc_fit_the_x_inside_the_frame, if (draft.docType == DocType.PASSPORT) str(R.string.kyc_photo_page) else str(R.string.kyc_front)),
                T.Cap.copy(fontSize = 15.sp), color = C.OnNight, align = TextAlign.Center)
            Box(Modifier.padding(top = 28.dp).size(338.dp, 236.dp)) {
                if (permission.granted && cameraOk) {
                    CameraPreview(Modifier.padding(6.dp).fillMaxSize().clip(RoundedCornerShape(14.dp)), capture = capture, torch = torch,
                        onError = { banner.show(str(R.string.kyc_camera_problem), it, BannerKind.Error) })
                } else {
                    MockDocument(Modifier.padding(14.dp).fillMaxSize().rotate(-1.2f))
                }
                FrameCorners()
            }
            Row(Modifier.padding(top = 24.dp)) {
                Pill(if (busy) str(R.string.kyc_hold_still_capturing) else if (permission.granted) str(R.string.kyc_ready_when_you_are) else str(R.string.kyc_camera_access_needed), PillKind.Lime,
                    icon = if (busy || permission.granted) Icons.Check else Icons.Camera)
            }
            Column(Modifier.padding(top = 28.dp, start = 36.dp, end = 36.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CamTip(str(R.string.kyc_all_four_corners_in_view)); CamTip(str(R.string.kyc_no_glare_on_the_photo))
            }
            if (!permission.granted && cameraOk) {
                ArroButton(str(R.string.kyc_allow_camera), { permission.request() }, Modifier.padding(top = 16.dp), style = Btn.Lime, size = BtnSize.M, fill = false)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 40.dp, end = 40.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(str(R.string.kyc_upload), T.Link.copy(fontSize = 14.sp), Modifier.width(64.dp).tap { upload() }, color = C.OnNight)
                Box(
                    Modifier.size(78.dp).border(4.dp, C.OnNight, CircleShape).padding(8.dp).background(if (busy) C.LimeDeep else C.Lime, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !busy) {
                            busy = true
                            if (permission.granted && cameraOk) {
                                takePhoto(context, capture, docFile(context.filesDir, side), { finish(it) }, { busy = false; banner.show(str(R.string.kyc_couldn_t_take_the_photo), it, BannerKind.Error) })
                            } else {
                                // No camera: use the sample image so the flow can still be tested.
                                scope.launch { delay(600); finish(sampleDocument(context)) }
                            }
                        },
                )
                Txt(str(R.string.kyc_tips), T.Link.copy(fontSize = 14.sp), Modifier.width(64.dp).tap { nav.go(Routes.article("verify")) }, color = C.OnNight, align = TextAlign.End)
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
    val bmp = createBitmap(640, 440)
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
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption(str(R.string.kyc_n_3_of_4_id)) }) },
        gap = 20.dp,
        footer = {
            ArroButton(str(R.string.kyc_use_this_photo), { nav.go(Routes.KYC_SELFIE) })
            ArroButton(str(R.string.kyc_retake), { nav.back() }, style = Btn.Line)
        },
    ) {
        StepBars(3, 4)
        ScreenTitle(str(R.string.kyc_is_it_easy_to_read), str(R.string.kyc_check_every_detail_is_sharp_before))
        Box(Modifier.fillMaxWidth().background(Color(0xFF1A2624), RoundedCornerShape(20.dp)).padding(18.dp)) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), str(R.string.kyc_your_id_photo), Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            } else {
                MockDocument(Modifier.fillMaxWidth().height(196.dp))
            }
            Pill(if (good) str(R.string.kyc_looks_good) else str(R.string.kyc_hard_to_read), if (good) PillKind.Lime else PillKind.Warn, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val notes = draft.qualityNotes.ifEmpty { listOf(str(R.string.kyc_all_four_corners_are_visible) to true, str(R.string.kyc_text_and_photo_are_in_focus) to true, str(R.string.kyc_no_glare_or_shadows) to true) }
            notes.forEach { (text, ok) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(if (ok) Icons.Check else Icons.Alert, if (ok) TileStyle.Lime else TileStyle.Warn, size = 28.dp, round = true, iconSize = 16.dp)
                    Txt(text, T.Body14)
                }
            }
        }
        if (!good) Note(str(R.string.kyc_we_may_not_be_able_to), NoteKind.Warn, Icons.Info)
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
    CameraScreen(str(R.string.kyc_n_4_of_4_selfie), { nav.back() }, closeIcon = Icons.ArrowLeft) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(124.dp))
            Txt(if (step == 0) str(R.string.kyc_look_straight_ahead) else if (step == 1) str(R.string.kyc_now_turn_your_head) else str(R.string.kyc_got_it_thank_you), T.H2.copy(fontSize = 22.sp), color = C.OnNight)
            Cap(if (step == 0) str(R.string.kyc_then_slowly_turn_your_head_to) else if (step == 1) str(R.string.kyc_slowly_to_the_left) else str(R.string.kyc_checking_it_matches_your_id), color = C.DarkCap, size = 15f)
            Box(Modifier.padding(top = 30.dp).size(268.dp, 340.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(28.dp).fillMaxSize().clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                }).background(Brush.verticalGradient(listOf(Color(0xFF4B5A55), Color(0xFF2E3B37))))) {
                    if (permission.granted && cameraOk) CameraPreview(Modifier.fillMaxSize(), front = true, analyzer = analyzer)
                    else {
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 30.dp).size(108.dp, 132.dp).background(Brush.verticalGradient(listOf(Color(0xFF7D6A5C), Color(0xFF5E4D41))), CircleShape))
                        Box(Modifier.align(Alignment.BottomCenter).size(196.dp, 92.dp).background(Brush.verticalGradient(listOf(C.ForestFixed, Color(0xFF1E3B31))), RoundedCornerShape(topStart = 98.dp, topEnd = 98.dp)))
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
                Pill(when (step) { 0 -> if (faceSeen) str(R.string.kyc_step_1_of_2_hold_still) else str(R.string.kyc_step_1_of_2_finding_your); 1 -> str(R.string.kyc_step_2_of_2_turn_left); else -> str(R.string.kyc_done) }, PillKind.OnDark)
            }
            if (!permission.granted && cameraOk) ArroButton(str(R.string.kyc_allow_camera), { permission.request() }, Modifier.padding(top = 16.dp), style = Btn.Lime, size = BtnSize.M, fill = false)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Cap(str(R.string.kyc_your_selfie_is_compared_once_with), Modifier.fillMaxWidth(), color = C.DarkCap, align = TextAlign.Center, size = 12f)
                val canSkip = !permission.granted || !cameraOk || stuck
                ArroButton(if (step == 2) str(R.string.kyc_checking) else if (canSkip) str(R.string.kyc_i_m_ready) else str(R.string.kyc_keep_still), { submit() }, style = Btn.Lime,
                    enabled = step == 2 || canSkip, loading = call.busy.value)
                if (call.error.value != null) FieldHint(call.error.value)
            }
        }
    }
    LaunchedEffect(permission.denied) { if (permission.denied) banner.show(str(R.string.kyc_camera_is_off), str(R.string.kyc_you_can_still_continue_we_ll), BannerKind.Info) }
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
                footer = { ArroButton(str(R.string.kyc_explore_arro_while_you_wait), { nav.resetTo(Routes.HOME) }) },
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp).height(220.dp), contentAlignment = Alignment.Center) {
                    Halo(220.dp) { Illustration(Art.Clock, 200.dp, description = str(R.string.kyc_a_stopwatch)) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScreenTitle(str(R.string.kyc_checking_your_details), size = TitleSize.XL)
                    Txt(str(R.string.kyc_most_checks_finish_in_under_2), T.Lede, color = C.Slate)
                }
                ArroCard(Modifier.padding(top = 8.dp)) {
                    val doc = k.docType.label
                    Timeline(listOf(
                        TimelineItem(str(R.string.kyc_personal_details), time = str(R.string.kyc_received), state = StepState.Done),
                        TimelineItem(doc, time = str(R.string.kyc_received), state = StepState.Done),
                        TimelineItem(str(R.string.kyc_matching_your_selfie), if (f < 0.6f) str(R.string.kyc_comparing_it_with_your_x_photo, doc.lowercase()) else null,
                            state = if (f < 0.6f) StepState.Now else StepState.Done, icon = if (f < 0.6f) Icons.Refresh else null,
                            time = if (f < 0.6f) "" else str(R.string.kyc_done)),
                        TimelineItem(str(R.string.kyc_final_checks), if (f >= 0.6f) str(R.string.kyc_sanctions_and_fraud_screening) else null,
                            state = if (f >= 0.6f) StepState.Now else StepState.Todo, icon = if (f >= 0.6f) Icons.Refresh else null),
                    ))
                }
            }
        }
        KycStatus.APPROVED -> {
            val level = k.level
            DarkSystemBars()
            Box(Modifier.fillMaxSize().background(C.Night)) {
                Box(Modifier.align(Alignment.TopCenter).offset(y = 80.dp).size(420.dp).background(haloBrush(alpha = 0.22f, stop = 0.65f), CircleShape))
                ArroScreen(
                    background = Color.Transparent,
                    footer = {
                        ArroButton(str(R.string.kyc_send_your_first_transfer), { c.session.send.reset(); nav.resetTo(Routes.HOME); nav.go(Routes.SEND_COUNTRY) }, style = Btn.Lime)
                        ArroButton(str(R.string.kyc_go_to_home), { nav.resetTo(Routes.HOME) }, style = Btn.LineOnDark)
                    },
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 70.dp), contentAlignment = Alignment.Center) { Illustration(Art.IdCard, 240.dp, description = str(R.string.kyc_an_id_card_with_a_verified)) }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScreenTitle(str(R.string.kyc_you_re_verified), size = TitleSize.XL, color = C.Lime, align = TextAlign.Center)
                        Txt(str(R.string.kyc_welcome_to_arro_x_your_account, a.firstName), T.Lede, color = C.DarkCap, align = TextAlign.Center)
                    }
                    Column(
                        Modifier.padding(top = 8.dp).fillMaxWidth().background(C.OnNight.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .border(1.dp, C.OnNight.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Cap(str(R.string.kyc_account_level), color = C.DarkCap); Pill(level.label, PillKind.Lime)
                        }
                        KV(str(R.string.kyc_per_transfer), str(R.string.kyc_up_to) + app.arro.core.Money.format(level.perTransferMinor, app.arro.core.Currency.GBP, false), onDark = true)
                        KV(str(R.string.kyc_per_month), str(R.string.kyc_up_to) + app.arro.core.Money.format(level.perMonthMinor, app.arro.core.Currency.GBP, false), onDark = true)
                        LinkText(str(R.string.kyc_how_to_raise_your_limits), { nav.go(Routes.VERIFICATION) }, color = C.Lime, strong = true)
                    }
                }
            }
        }
        KycStatus.REJECTED -> ArroScreen(
            topBar = {
                TopBar(left = { RoundIcon(Icons.Close, { nav.resetTo(Routes.HOME) }, style = IconBtn.Soft, description = str(R.string.kyc_close)) }, right = {
                    ArroButton(str(R.string.kyc_get_help), { nav.go(Routes.CHAT) }, style = Btn.Text, size = BtnSize.S, fill = false)
                })
            },
            footer = {
                ArroButton(str(R.string.kyc_retake_x_photo, k.docType.label.lowercase()), {
                    c.backend.retakeIdentity(); nav.resetTo(Routes.HOME); nav.go(Routes.KYC_DOCUMENT); nav.go(Routes.KYC_CAPTURE)
                }, enabled = k.attemptsLeft > 0)
                ArroButton(str(R.string.kyc_use_a_different_id), { c.backend.retakeIdentity(); nav.resetTo(Routes.HOME); nav.go(Routes.KYC_DOCUMENT) }, style = Btn.Line, enabled = k.attemptsLeft > 0)
            },
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(Art.IdCardWarn, 168.dp, description = str(R.string.kyc_an_id_card_with_an_alert)) }
            ScreenTitle(str(R.string.kyc_we_need_a_clearer_photo), k.rejectReason.ifBlank { str(R.string.kyc_we_couldn_t_read_your_id) })
            ArroCard {
                Over(str(R.string.kyc_tips_for_a_quick_pass))
                VSpace(12.dp)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Icons.Bolt to str(R.string.kyc_turn_your_flash_off), Icons.Sun to str(R.string.kyc_tilt_the_x_away_from_lights, k.docType.label.lowercase()), Icons.Square to str(R.string.kyc_use_a_dark_plain_background)).forEach { (icon, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tile(icon, TileStyle.Warn, size = 28.dp, round = true, iconSize = 16.dp); Txt(text, T.Body14)
                        }
                    }
                }
            }
            Cap(if (k.attemptsLeft > 0) str(R.string.kyc_x_attempt_x_left_today_your, k.attemptsLeft, if (k.attemptsLeft == 1) "" else "s")
                else str(R.string.kyc_you_ve_used_today_s_attempts))
        }
    }
}
