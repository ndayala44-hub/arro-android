package app.arro.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.arro.core.ArroException
import app.arro.core.BackendEvent
import app.arro.core.Currency
import app.arro.core.Money
import app.arro.core.PayoutType
import app.arro.core.Seed
import app.arro.core.Text
import app.arro.core.Validation
import app.arro.platform.Biometrics
import app.arro.ui.BannerKind
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.appState
import app.arro.ui.back
import app.arro.ui.backend
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.launchCall
import app.arro.ui.rememberCall
import app.arro.ui.resetTo
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.BarCaption
import app.arro.ui.components.BrandLockup
import app.arro.ui.components.BrandTile
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.CarouselDots
import app.arro.ui.components.CheckRow
import app.arro.ui.components.CodeInput
import app.arro.ui.components.DialPrefix
import app.arro.ui.components.FieldHint
import app.arro.ui.components.Flag
import app.arro.ui.components.HDivider
import app.arro.ui.components.Halo
import app.arro.ui.components.Ic
import app.arro.ui.components.Icons
import app.arro.ui.components.Illustration
import app.arro.ui.components.Keypad
import app.arro.ui.components.LinkText
import app.arro.ui.components.MaxContentWidth
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.PasscodeDots
import app.arro.ui.components.PasswordField
import app.arro.ui.components.PasswordStrength
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.ProgressBar
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.SelectField
import app.arro.ui.components.StepBars
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.TitleSize
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.components.VSpace
import app.arro.ui.components.Wordmark
import app.arro.ui.components.haloBrush
import app.arro.ui.components.tap
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val BIO_NAME = "fingerprint or face"

// ------------------------------------------------------------------ 1.01 Splash

@Composable
fun SplashScreen() {
    val nav = LocalNav.current
    val b = backend()
    val progress = remember { Animatable(0.15f) }
    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(1300, easing = LinearEasing)) }
        launch { b.refreshRates() }
        delay(1400)
        val s = b.state.value
        nav.resetTo(
            when {
                !s.onboardingSeen -> Routes.WELCOME
                s.account == null -> Routes.WELCOME
                !s.signedIn -> Routes.LOGIN
                else -> Routes.HOME
            },
        )
    }
    app.arro.ui.components.DarkSystemBars()
    Box(Modifier.fillMaxSize().background(C.Night), contentAlignment = Alignment.Center) {
        Box(Modifier.offset(y = (-40).dp).size(520.dp).background(haloBrush(alpha = 0.18f, stop = 0.62f), CircleShape))
        Column(Modifier.offset(y = (-60).dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BrandTile(104.dp)
            Wordmark(52f, C.OnNight)
            Cap("Money home, in minutes.", color = C.DarkCap, size = 15f)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProgressBar(progress.value, Modifier.width(120.dp), track = C.OnNight.copy(alpha = 0.14f), fill = C.Lime, height = 4.dp)
            Cap("Securing your connection", color = C.DarkCap, size = 12f)
        }
    }
}

// ------------------------------------------------------------------ 1.02 to 1.04 Welcome

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen() {
    val nav = LocalNav.current
    val b = backend()
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { b.markOnboardingSeen() }
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
    val page = pager.currentPage
    ArroScreen(
        background = C.Tint,
        scroll = false,
        bodyPadding = 0.dp,
        gap = 0.dp,
        topBar = {
            TopBar(
                left = {
                    if (page == 0) BrandLockup(30.dp, 24f)
                    else app.arro.ui.components.BackButton({ scope.launch { pager.animateScrollToPage(page - 1) } })
                },
                right = {
                    if (page < 2) ArroButton("Skip", { scope.launch { pager.animateScrollToPage(2) } }, style = Btn.Text, size = BtnSize.S, fill = false)
                },
            )
        },
        footer = {
            if (page < 2) {
                ArroButton("Next", { scope.launch { pager.animateScrollToPage(page + 1) } })
            } else {
                ArroButton("Create account", { nav.go(Routes.PHONE) })
                ArroButton("I already have an account", { nav.go(Routes.LOGIN) }, style = Btn.Line)
                val terms = buildAnnotatedString {
                    append("By continuing you agree to our ")
                    withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append("Terms") }
                    append(" and ")
                    withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
                    append(".")
                }
                Txt(terms, T.Cap.copy(fontSize = 12.sp), Modifier.fillMaxWidth().tap { nav.go(Routes.legal("terms")) }, color = C.Slate, align = TextAlign.Center)
            }
        },
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { p ->
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (p) {
                        0 -> Halo(220.dp) { Illustration(Art.Globe, 240.dp, description = "A globe with a coin orbiting it") }
                        1 -> WelcomeQuote()
                        else -> Halo(220.dp) { Illustration(Art.Shield, 240.dp, description = "A glass shield with a tick") }
                    }
                }
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CarouselDots(p, 3)
                    val (title, lede) = when (p) {
                        0 -> "Send home\nin minutes" to "Mobile money, bank deposit or cash pickup, to the people who count on you in Rwanda, Kenya, Nigeria and beyond."
                        1 -> "See exactly\nwhat arrives" to "Your rate, every fee and the exact amount they receive, shown before you pay. No surprises at the other end."
                        else -> "Protected at\nevery step" to "Encrypted payments, recipients checked by name, and real people on hand if anything needs fixing."
                    }
                    ScreenTitle(title, size = TitleSize.XL)
                    Txt(lede, T.Lede, color = C.Slate)
                }
            }
        }
    }
}

@Composable
private fun WelcomeQuote() {
    val b = backend()
    val q = remember { b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Halo(220.dp) { Illustration(Art.Welcome, 240.dp, description = "A stack of glass coins") }
        Column(
            Modifier.offset(y = (-18).dp).width(268.dp)
                .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = C.Ink.copy(alpha = 0.12f), spotColor = C.Ink.copy(alpha = 0.12f))
                .background(C.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap("You send"); Txt(Money.format(q.sendMinor, Currency.GBP), T.Num)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap("Fee"); Txt(Money.format(1_99, Currency.GBP), T.Num.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal))
            }
            HDivider(Modifier.padding(vertical = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Cap("Claudine gets"); Txt(Money.format(q.receiveMinor, Currency.RWF), T.Num, color = C.Ok)
            }
        }
    }
}

// ------------------------------------------------------------------ 1.05 Phone

@Composable
fun PhoneScreen() {
    val nav = LocalNav.current
    val b = backend()
    val state by appState()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    var phone by rememberSaveable { mutableStateOf(b.registration.value.phone.removePrefix("+44")) }
    var error by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf(false) }
    fun submit() {
        error = Validation.ukMobile(phone)
        if (error != null) return
        scope.launchCall(call) {
            b.sendSignupCode("GB", phone)
            nav.go(Routes.OTP)
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Step 1 of 4") }) },
        gap = 24.dp,
        footer = {
            ArroButton("Send code", { submit() }, loading = call.busy.value)
            LoginPrompt("Already with Arro?", "Log in") { nav.go(Routes.LOGIN) }
        },
        overlay = {
            PickerSheet(picker, "Country of residence", listOf("United Kingdom"), "United Kingdom", {}, { picker = false },
                subtitles = mapOf("United Kingdom" to "Arro is available to UK residents"), leading = { Flag("gb") })
        },
    ) {
        StepBars(1, 4)
        ScreenTitle("What's your\nmobile number?", "We'll text you a 6-digit code to confirm it's really you.")
        SelectField("Country of residence", "United Kingdom", { picker = true }, leading = { Flag("gb") })
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            app.arro.ui.components.FieldLabel("Mobile number")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectField(null, "+44", { picker = true }, Modifier.width(104.dp), leading = { Flag("gb", 18.dp) })
                ArroField(
                    phone, { v -> phone = v.filter { it.isDigit() || it == ' ' }; error = null }, Modifier.weight(1f),
                    placeholder = "7700 900412", keyboard = KeyboardType.Phone, imeAction = ImeAction.Done,
                    error = null, onDone = { submit() },
                )
            }
            FieldHint(error ?: call.error.value)
        }
        Note("We only use your number for security codes and transfer updates. We never sell it.", NoteKind.Lime, Icons.Lock)
        if (state.account != null) {
            Note("Creating a new account replaces the Arro account saved on this phone.", NoteKind.Warn, Icons.Info)
        }
    }
}

@Composable
private fun LoginPrompt(text: String, link: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Cap("$text ")
        LinkText(link, onClick, color = C.Ink, strong = true)
    }
}

// ------------------------------------------------------------------ 1.06 OTP

@Composable
fun OtpScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val reg by b.registration.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val banner = LocalBanner.current
    var code by rememberSaveable { mutableStateOf("") }
    var resendAt by remember { mutableStateOf(System.currentTimeMillis() + 60_000) }
    val now = app.arro.ui.nowTicker()
    fun verify(v: String = code) {
        if (v.length < 6) { call.error.value = "Enter all 6 digits."; return }
        scope.launchCall(call) {
            b.verifySignupCode(v)
            nav.go(Routes.ACCOUNT_SETUP)
        }
    }
    // Fill the code automatically when the "SMS" arrives, like Android's SMS autofill.
    LaunchedEffect(Unit) {
        c.banners.collect { e -> if (e is BackendEvent.Code && e.to == reg.phone) { delay(900); code = e.code; verify(e.code) } }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Step 2 of 4") }) },
        gap = 24.dp,
        footer = { ArroButton("Verify", { verify() }, loading = call.busy.value, enabled = code.length == 6) },
    ) {
        StepBars(2, 4)
        Illustration(Art.Phone, 128.dp, description = "A phone receiving a message")
        Column {
            ScreenTitle("Enter the code")
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(buildAnnotatedString {
                    append("Sent by SMS to ")
                    withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Text.formatPhone(reg.phone)) }
                    append(". ")
                }, T.Lede, color = C.Slate)
                LinkText("Edit", { nav.back() }, color = C.Ink, strong = true)
            }
        }
        CodeInput(code, { code = it; call.error.value = null }, error = call.error.value != null, onComplete = { verify(it) })
        FieldHint(call.error.value)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (now < resendAt) {
                Txt(buildAnnotatedString {
                    append("Resend code in ")
                    withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Text.countdown(resendAt - now)) }
                }, T.Cap, color = C.Slate)
            } else {
                LinkText("Resend code", {
                    scope.launch { runCatching { b.resendSignupCode() }; resendAt = System.currentTimeMillis() + 60_000 }
                }, color = C.Ink, strong = true)
            }
            LinkText("Get a call instead", {
                scope.launch {
                    runCatching { b.resendSignupCode() }
                    banner.show("Calling ${Text.formatPhone(reg.phone)}", "An automated call will read out your code.", BannerKind.Info)
                }
            }, color = C.Ink, strong = true)
        }
    }
}

// ------------------------------------------------------------------ 1.07 Email and password

@Composable
fun AccountSetupScreen() {
    val nav = LocalNav.current
    val b = backend()
    val reg by b.registration.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    var email by rememberSaveable { mutableStateOf(reg.email) }
    var password by rememberSaveable { mutableStateOf("") }
    var marketing by rememberSaveable { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    fun submit() {
        emailError = Validation.email(email)
        if (emailError != null) return
        if (!Validation.password(password).required) { call.error.value = "Your password needs at least 8 characters, upper and lower case letters and a number."; return }
        scope.launchCall(call) {
            b.setLogin(email, password, marketing)
            nav.go(Routes.PASSCODE)
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, right = { BarCaption("Step 3 of 4") }) },
        gap = 20.dp,
        footer = { ArroButton("Continue", { submit() }, loading = call.busy.value) },
    ) {
        StepBars(3, 4)
        ScreenTitle("Set up your login", "Your email is where receipts and statements go.")
        ArroField(email, { email = it.trim(); emailError = null }, label = "Email address", placeholder = "name@mail.com",
            keyboard = KeyboardType.Email, error = emailError)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PasswordField(password, { password = it; call.error.value = null }, "Password", onDone = { submit() })
            PasswordStrength(password)
        }
        FieldHint(call.error.value)
        CheckRow(marketing, { marketing = it }, "Email me rate alerts and product news. I can opt out anytime.")
    }
}

// ------------------------------------------------------------------ 1.08 Passcode

@Composable
fun PasscodeSetupScreen() {
    val nav = LocalNav.current
    val b = backend()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    var first by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun confirm() {
        if (code.length < 6) return
        val f = first
        if (f == null) {
            if (Validation.passcodeWeak(code)) { error = "That's easy to guess. Avoid repeated or sequential digits."; code = ""; return }
            first = code; code = ""; error = null
        } else if (f != code) {
            error = "Those passcodes don't match. Try again."; first = null; code = ""
        } else {
            scope.launchCall(call) {
                b.finishRegistration(code)
                nav.resetTo(Routes.BIOMETRIC)
            }
        }
    }
    ArroScreen(
        topBar = { TopBar(onBack = { if (first != null) { first = null; code = "" } else nav.back() }, right = { BarCaption("Step 4 of 4") }) },
        gap = 24.dp,
        scroll = false,
        footer = { ArroButton(if (first == null) "Continue" else "Confirm passcode", { confirm() }, enabled = code.length == 6, loading = call.busy.value) },
    ) {
        StepBars(4, 4)
        ScreenTitle(if (first == null) "Create a passcode" else "Confirm your passcode",
            if (first == null) "Six digits to open Arro and approve every transfer. Avoid birthdays and repeated numbers." else "Enter the same six digits again.")
        PasscodeDots(code.length, error = error != null, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        FieldHint(error ?: call.error.value)
        Spacer(Modifier.weight(1f))
        Keypad({ k -> if (code.length < 6) { code += k; error = null } }, { code = code.dropLast(1) })
    }
}

// ------------------------------------------------------------------ 1.09 Biometrics

@Composable
fun BiometricSetupScreen() {
    val nav = LocalNav.current
    val b = backend()
    val context = LocalContext.current
    val banner = LocalBanner.current
    fun next() = nav.resetTo(Routes.KYC)
    ArroScreen(
        background = C.Tint,
        scroll = false,
        bodyPadding = 0.dp,
        topBar = { TopBar(onBack = null) },
        footer = {
            ArroButton("Turn on $BIO_NAME unlock", {
                val activity = context as? FragmentActivity
                if (activity == null || !Biometrics.available(context)) {
                    banner.show("Not set up on this phone", "Add a fingerprint or face in your phone's settings, then turn this on in Security.", BannerKind.Error)
                    next()
                } else {
                    Biometrics.prompt(activity, "Turn on $BIO_NAME unlock", "Confirm it's you", negative = "Cancel",
                        onSuccess = { b.setBiometric(true); banner.show("Biometric unlock is on", kind = BannerKind.Success); next() },
                        onFallback = {}, onError = { banner.show("Couldn't turn it on", it, BannerKind.Error) })
                }
            }, icon = Icons.FaceId)
            ArroButton("Not now", { next() }, style = Btn.Text)
        },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.FaceScan, 240.dp, description = "Face being scanned") }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenTitle("Unlock with\na glance", size = TitleSize.XL)
            Txt("Use your $BIO_NAME to open Arro and approve transfers. Your biometric data never leaves this phone.", T.Lede, color = C.Slate)
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CheckLine("Faster than typing your passcode")
                CheckLine("Change it anytime in Security")
            }
        }
    }
}

@Composable
fun CheckLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(Icons.Check, TileStyle.Lime, size = 32.dp, round = true, iconSize = 16.dp)
        Txt(text, T.Body14)
    }
}

// ------------------------------------------------------------------ 1.10 Log in

@Composable
fun LoginScreen() {
    val nav = LocalNav.current
    val b = backend()
    val state by appState()
    val context = LocalContext.current
    val banner = LocalBanner.current
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    var id by rememberSaveable { mutableStateOf(state.account?.email ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    fun submit() = scope.launchCall(call) {
        b.login(id, password)
        nav.resetTo(Routes.HOME)
    }
    val bioReady = state.account?.biometricEnabled == true
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 24.dp,
        footer = { LoginPrompt("New to Arro?", "Create an account") { nav.go(Routes.PHONE) } },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BrandLockup(26.dp, 22f) }
        Column(Modifier.padding(top = 8.dp)) {
            ScreenTitle("Welcome back", "Log in with the email or mobile number you signed up with.", size = TitleSize.XL)
        }
        ArroField(id, { id = it; call.error.value = null }, label = "Email or mobile number", placeholder = "name@mail.com or 07700 900412",
            keyboard = KeyboardType.Email)
        PasswordField(password, { password = it; call.error.value = null }, "Password", error = call.error.value, onDone = { submit() },
            labelTrailing = { LinkText("Forgot password?", { nav.go(Routes.FORGOT) }, color = C.Ink, strong = true) })
        ArroButton("Log in", { submit() }, Modifier.padding(top = 4.dp), loading = call.busy.value, enabled = id.isNotBlank() && password.isNotEmpty())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HDivider(Modifier.weight(1f)); Cap("or"); HDivider(Modifier.weight(1f))
        }
        ArroButton("Log in with $BIO_NAME", {
            val activity = context as? FragmentActivity
            when {
                !bioReady -> banner.show("Biometric unlock isn't on", "Log in with your password, then turn it on in Security.", BannerKind.Info)
                activity == null || !Biometrics.available(context) -> banner.show("Not available", "This phone has no fingerprint or face set up.", BannerKind.Error)
                else -> Biometrics.prompt(activity, "Log in to Arro", state.account?.email ?: "", negative = "Use password",
                    onSuccess = { runCatching { b.loginWithBiometrics(); nav.resetTo(Routes.HOME) }.onFailure { banner.show(it.message ?: "Try your password", kind = BannerKind.Error) } })
            }
        }, style = Btn.Line, icon = Icons.FaceId)
        Note("Exploring Arro? Use the sample account: ${Seed.DEMO_EMAIL}, password ${Seed.DEMO_PASSWORD}.", NoteKind.Lime, Icons.Info,
            action = "Fill in sample account", onAction = { id = Seed.DEMO_EMAIL; password = Seed.DEMO_PASSWORD })
    }
}

// ------------------------------------------------------------------ 1.11 Unlock and 12.08 Session ended (overlays)

@Composable
fun UnlockScreen(expired: Boolean) {
    val c = container()
    val b = c.backend
    val state by appState()
    val context = LocalContext.current
    val banner = LocalBanner.current
    val scope = rememberCoroutineScope()
    val account = state.account ?: return
    var showPad by remember(expired) { mutableStateOf(!expired) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    val bio = account.biometricEnabled && Biometrics.available(context)
    fun unlocked() { c.session.sessionExpired = false; code = "" }
    fun promptBio() {
        val activity = context as? FragmentActivity ?: return
        Biometrics.prompt(activity, "Unlock Arro", "Welcome back, ${account.firstName}", onSuccess = { b.unlockWithBiometrics(); unlocked() }, onFallback = { showPad = true })
    }
    LaunchedEffect(showPad) { if (showPad && bio && !expired) promptBio() }
    BackHandler { (context as? Activity)?.moveTaskToBack(true) }
    fun submit(v: String) = scope.launch {
        try {
            b.unlockWithPasscode(v); unlocked()
        } catch (e: ArroException) {
            error = e.message; code = ""; shake++
            if (e.code == "locked_out") banner.show("Log in again", e.message ?: "", BannerKind.Error)
        }
    }
    app.arro.ui.components.DarkSystemBars()
    Box(Modifier.fillMaxSize().background(C.Night).tap { }) {
        if (!showPad) {
            // 12.08 Session ended
            Box(Modifier.align(Alignment.TopCenter).offset(y = 90.dp).size(420.dp).background(Brush.radialGradient(listOf(Color(0x2E6CC5F4), Color(0x006CC5F4))), CircleShape))
            Column(
                Modifier.align(Alignment.TopCenter).widthIn(max = MaxContentWidth).fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.statusBarsPadding().height(100.dp))
                Illustration(Art.Lock, 240.dp, description = "A glass padlock")
                Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScreenTitle("Session ended", size = TitleSize.XL, color = C.OnNight, align = TextAlign.Center)
                    Txt("For your security we locked Arro after 5 minutes without activity. Nothing was lost.", T.Lede, color = C.DarkCap, align = TextAlign.Center)
                }
                val open = account.transfers.firstOrNull { it.isOpen }
                Row(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp).fillMaxWidth()
                        .background(C.OnNight.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Ic(Icons.Check, tint = C.Lime)
                    Txt(if (open != null) "Your transfer to ${open.firstName} is still on its way." else "Your balances and transfers are safe.", T.Body14, color = C.OnNight)
                }
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (bio) ArroButton("Unlock with $BIO_NAME", { promptBio() }, style = Btn.Lime, icon = Icons.FaceId)
                    else ArroButton("Unlock with passcode", { showPad = true }, style = Btn.Lime, icon = Icons.Lock)
                    ArroButton("Log in with password", { c.session.sessionExpired = false; b.logout() }, style = Btn.LineOnDark)
                }
            }
        } else {
            // 1.11 Quick unlock
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = MaxContentWidth).fillMaxSize()) {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(64.dp).background(C.Lime, CircleShape), contentAlignment = Alignment.Center) { Txt(account.initials, T.H2.copy(fontSize = 22.sp), color = C.OnLime) }
                    Txt("Welcome back, ${account.firstName}", T.H2.copy(fontSize = 22.sp), color = C.OnNight)
                    Cap(error ?: "Enter your passcode", color = if (error != null) Color(0xFFFF9B8F) else C.DarkCap)
                    val dx = remember { Animatable(0f) }
                    LaunchedEffect(shake) { if (shake > 0) { for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) dx.animateTo(x, tween(45)) } }
                    PasscodeDots(code.length, dark = true, error = error != null, modifier = Modifier.padding(top = 18.dp).offset(x = dx.value.dp))
                }
                Spacer(Modifier.weight(1f))
                Keypad(
                    onKey = { k -> if (code.length < 6) { code += k; error = null; if (code.length == 6) submit(code) } },
                    onDelete = { code = code.dropLast(1) },
                    modifier = Modifier.padding(horizontal = 36.dp),
                    dark = true,
                    bottomLeft = if (bio) ({ Box(Modifier.size(58.dp).tap { promptBio() }, contentAlignment = Alignment.Center) { Ic(Icons.FaceId, size = 28.dp, tint = C.Lime, contentDescription = "Use $BIO_NAME") } }) else null,
                )
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    LinkText("Forgot passcode?", { c.session.sessionExpired = false; b.logout() }, color = C.OnNight, strong = true)
                    LinkText("Not ${account.firstName}? Switch", { c.session.sessionExpired = false; b.logout() }, color = C.DarkCap)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 1.12 to 1.14 Password reset

@Composable
fun ForgotScreen() {
    val nav = LocalNav.current
    val b = backend()
    val state by appState()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    var email by rememberSaveable { mutableStateOf(state.account?.email ?: "") }
    fun submit() = scope.launchCall(call) {
        b.requestPasswordReset(email)
        nav.go(Routes.RESET)
    }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 24.dp,
        footer = {
            ArroButton("Send reset code", { submit() }, loading = call.busy.value, enabled = email.isNotBlank())
            ArroButton("I no longer use this email", { nav.go(Routes.HELP) }, style = Btn.Text)
        },
    ) {
        Illustration(Art.Envelope, 128.dp, description = "An envelope")
        ScreenTitle("Reset your\npassword", "Enter the email on your account. We'll send a 6-digit code to set a new password.")
        ArroField(email, { email = it.trim(); call.error.value = null }, label = "Email address", keyboard = KeyboardType.Email,
            imeAction = ImeAction.Done, error = call.error.value, onDone = { submit() })
        Note("For your safety, new transfers are paused for 24 hours after a password reset. Transfers already on their way are not affected.", NoteKind.Info, Icons.Info)
    }
}

@Composable
fun ResetScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val verifyCall = rememberCall()
    var code by rememberSaveable { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    fun verify(v: String) = scope.launchCall(verifyCall) { b.verifyResetCode(v); verified = true }
    LaunchedEffect(Unit) {
        c.banners.collect { e -> if (e is BackendEvent.Code && e.purpose.contains("reset")) { delay(900); code = e.code; verify(e.code) } }
    }
    val match = confirm.isNotEmpty() && confirm == password
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 20.dp,
        footer = {
            ArroButton("Update password", {
                scope.launchCall(call) {
                    if (!match) throw ArroException("The passwords don't match.")
                    c.session.resetUntil = b.resetPassword(password)
                    nav.resetTo(Routes.RESET_DONE)
                }
            }, loading = call.busy.value, enabled = verified && Validation.password(password).required && match)
        },
    ) {
        Column {
            ScreenTitle("Choose a new\npassword")
            Txt(buildAnnotatedString {
                append("Enter the code we emailed to ")
                withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(Text.maskEmail(b.resetEmail)) }
                append(".")
            }, T.Lede, Modifier.padding(top = 8.dp), color = C.Slate)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            app.arro.ui.components.FieldLabel("Reset code")
            CodeInput(code, { code = it; verified = false; verifyCall.error.value = null }, error = verifyCall.error.value != null, onComplete = { verify(it) })
            FieldHint(verifyCall.error.value, if (verified) "Code confirmed" else null)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PasswordField(password, { password = it }, "New password", imeAction = ImeAction.Next)
            PasswordStrength(password, showRules = false)
        }
        PasswordField(confirm, { confirm = it }, "Confirm new password", ok = if (match) "Passwords match" else null,
            error = if (confirm.length >= password.length && confirm.isNotEmpty() && !match) "The passwords don't match." else null)
        FieldHint(call.error.value)
    }
}

@Composable
fun ResetDoneScreen() {
    val nav = LocalNav.current
    val c = container()
    val until = c.session.resetUntil.takeIf { it > 0 } ?: (System.currentTimeMillis() + 86_400_000L)
    ArroScreen(
        background = C.Tint,
        scroll = false,
        bodyPadding = 0.dp,
        footer = { ArroButton("Log in", { nav.resetTo(Routes.LOGIN) }) },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Halo(220.dp) { Illustration(Art.Shield, 240.dp, description = "A glass shield with a tick") }
        }
        Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenTitle("Password\nupdated", size = TitleSize.XL, align = TextAlign.Center)
            Txt("We've signed you out on your other devices. Log in with your new password to carry on.", T.Lede, color = C.Slate, align = TextAlign.Center)
            Note("New transfers are paused until ${Text.time(until)} ${if (Text.isSameDay(until, System.currentTimeMillis())) "today" else "tomorrow"} while we make sure this was you. Didn't change it?",
                NoteKind.Warn, Icons.Clock, Modifier.padding(top = 8.dp), action = "Contact us now", onAction = { nav.go(Routes.CHAT) })
        }
    }
}
