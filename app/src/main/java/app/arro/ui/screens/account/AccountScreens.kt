package app.arro.ui.screens.account

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import app.arro.core.Address
import app.arro.core.Appearance
import app.arro.core.Catalog
import app.arro.core.Currency
import app.arro.core.KycLevel
import app.arro.core.KycStatus
import app.arro.core.MethodStatus
import app.arro.core.Money
import app.arro.core.Seed
import app.arro.core.Text
import app.arro.core.Validation
import app.arro.platform.Biometrics
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
import app.arro.ui.resetTo
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroCard
import app.arro.ui.components.ArroField
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.CheckRow
import app.arro.ui.components.Chevron
import app.arro.ui.components.Chip
import app.arro.ui.components.CodeInput
import app.arro.ui.components.ConfirmDialog
import app.arro.ui.components.DialPrefix
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
import app.arro.ui.components.Keypad
import app.arro.ui.components.LinkText
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.PasscodeDots
import app.arro.ui.components.PasswordField
import app.arro.ui.components.PasswordStrength
import app.arro.ui.components.PickerSheet
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.ProgressBar
import app.arro.ui.components.Radio
import app.arro.ui.components.RoundIcon
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.Segmented
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Tab
import app.arro.ui.components.TabScreen
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.TitleSize
import app.arro.ui.components.Toggle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.screens.auth.BIO_NAME
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun SettingsRow(title: String, icon: Int, value: String? = null, tile: TileStyle = TileStyle.Tint, titleColor: androidx.compose.ui.graphics.Color = C.Ink, onClick: () -> Unit) {
    ListRow(title, flat = true, minHeight = 54.dp, titleStyle = T.RowTitle.copy(color = titleColor),
        leading = { Tile(icon, tile, 36.dp, iconSize = 18.dp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (value != null) Cap(value)
                if (tile != TileStyle.Err) Chevron()
            }
        }, onClick = onClick)
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, on: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    ListRow(title, flat = true, minHeight = 58.dp, subtitle = subtitle, trailing = { Toggle(on, onChange, enabled = enabled) },
        onClick = if (enabled) ({ onChange(!on) }) else null)
}

// ------------------------------------------------------------------ 11.01 Account

@Composable
fun AccountScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    var logoutConfirm by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0" }
    TabScreen(
        Tab.Account,
        overlay = {
            AppearanceSheet(appearanceOpen, a.settings.appearance, { choice -> b.updateSettings { it.copy(appearance = choice) } }) { appearanceOpen = false }
            ConfirmDialog(logoutConfirm, "Log out of Arro?", "You'll need your email and password to log in again on this phone.", "Log out", {
                logoutConfirm = false
                b.logout()
                nav.resetTo(Routes.LOGIN)
            }, { logoutConfirm = false })
        },
    ) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 14.dp)) {
            RoundIcon(Icons.Help, { nav.go(Routes.HELP) }, Modifier.align(Alignment.TopEnd).padding(end = 20.dp), style = IconBtn.Soft, description = "Help")
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(88.dp)) {
                    Avatar(a.initials, 0, 84.dp, modifier = Modifier.align(Alignment.Center))
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(28.dp).background(C.Lime, CircleShape).border(3.dp, C.White, CircleShape)
                            .then(Modifier.padding(0.dp)),
                        contentAlignment = Alignment.Center,
                    ) { RoundIcon(Icons.Pencil, { nav.go(Routes.PERSONAL) }, style = IconBtn.Lime, size = 26.dp, iconSize = 13.dp, description = "Edit personal details") }
                }
                Txt(a.displayName, T.Title.copy(fontSize = 22.sp), Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cap("@" + a.username.ifBlank { a.profile.firstNames.lowercase().take(4) + a.profile.lastName.lowercase().take(1) })
                    when (a.kyc.status) {
                        KycStatus.APPROVED -> Pill("Verified", PillKind.Ok, dot = true)
                        KycStatus.IN_REVIEW -> Pill("Checking ID", PillKind.Info, dot = true)
                        else -> Pill("Not verified", PillKind.Warn, dot = true)
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Panel(kind = PanelKind.Dark, padding = 14.dp, onClick = { nav.go(Routes.REFER) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(C.Lime.copy(alpha = 0.16f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Ic(Icons.Gift, tint = C.Lime) }
                    Column(Modifier.weight(1f)) {
                        Txt("Invite friends", T.H3, color = C.OnNight)
                        Cap("Get £10 for every friend who sends", color = C.DarkCap, size = 12f)
                    }
                    Ic(Icons.ChevronRight, tint = C.Lime)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Over("Account")
                Group {
                    SettingsRow("Personal details", Icons.User) { nav.go(Routes.PERSONAL) }
                    GroupDivider()
                    SettingsRow("Verification and limits", Icons.IdCard, if (a.verified) a.kyc.level.label else "Not verified") { nav.go(Routes.VERIFICATION) }
                    GroupDivider()
                    SettingsRow("Payment methods", Icons.CreditCard, a.paymentMethods.count { it.status == MethodStatus.ACTIVE }.takeIf { it > 0 }?.toString()) { nav.go(Routes.PAY_METHODS) }
                    GroupDivider()
                    SettingsRow("Wallet and balances", Icons.WalletSimple, "${a.wallets.size}") { nav.go(Routes.WALLET) }
                    GroupDivider()
                    SettingsRow("Statements", Icons.Statement) { nav.go(Routes.STATEMENTS) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Over("Security and preferences")
                Group {
                    SettingsRow("Security", Icons.ShieldCheck, if (a.frozen) "Frozen" else null) { nav.go(Routes.SECURITY) }
                    GroupDivider()
                    SettingsRow("Notifications", Icons.Bell) { nav.go(Routes.NOTIFICATION_SETTINGS) }
                    GroupDivider()
                    SettingsRow("Appearance", Icons.Sun, appearanceLabel(a.settings.appearance)) { appearanceOpen = true }
                    GroupDivider()
                    SettingsRow("Language and region", Icons.Globe, a.settings.language) { nav.go(Routes.LANGUAGE) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Over("Help and account")
                Group {
                    SettingsRow("Help centre and chat", Icons.Chat) { nav.go(Routes.HELP) }
                    GroupDivider()
                    SettingsRow("Account management", Icons.Filters) { nav.go(Routes.MANAGE) }
                    GroupDivider()
                    SettingsRow("Demo controls", Icons.Bolt, "Test build", TileStyle.Lime) { nav.go(Routes.DEMO) }
                    GroupDivider()
                    SettingsRow("Log out", Icons.LogOut, tile = TileStyle.Err, titleColor = C.Err) { logoutConfirm = true }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Cap("Arro $version · ", size = 12f)
                    LinkText("Terms", { nav.go(Routes.legal("terms")) }, color = C.Ink)
                    Cap(" · ", size = 12f)
                    LinkText("Privacy", { nav.go(Routes.legal("privacy")) }, color = C.Ink)
                }
            }
        }
    }
}

private fun appearanceLabel(a: Appearance) = when (a) { Appearance.LIGHT -> "Light"; Appearance.DARK -> "Dark"; Appearance.SYSTEM -> "Same as phone" }

/** Light, dark or follow the phone. The change applies straight away, so the sheet itself switches theme. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.AppearanceSheet(visible: Boolean, current: Appearance, onChoose: (Appearance) -> Unit, onDismiss: () -> Unit) {
    val lightBg = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val darkBg = androidx.compose.ui.graphics.Color(0xFF0A1412)
    Sheet(visible, onDismiss) {
        SheetHeader("Appearance", onDismiss, "Choose how Arro looks on this phone.")
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple(Appearance.LIGHT, "Light", "Bright screens, the classic Arro look"),
                Triple(Appearance.DARK, "Dark", "Easier on the eyes at night"),
                Triple(Appearance.SYSTEM, "Same as phone", "Switches with your phone's dark mode"),
            ).forEach { (option, title, sub) ->
                ListRow(title, subtitle = sub, selected = option == current, minHeight = 64.dp,
                    leading = {
                        val fill = when (option) {
                            Appearance.LIGHT -> Brush.linearGradient(listOf(lightBg, lightBg))
                            Appearance.DARK -> Brush.linearGradient(listOf(darkBg, darkBg))
                            Appearance.SYSTEM -> Brush.horizontalGradient(0f to lightBg, 0.5f to lightBg, 0.5f to darkBg, 1f to darkBg)
                        }
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(fill).border(1.5.dp, C.Line, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(12.dp).background(C.Lime, CircleShape)) }
                    },
                    trailing = { Radio(option == current) },
                    onClick = { onChoose(option) })
            }
        }
        ArroButton("Done", onDismiss, Modifier.padding(top = 16.dp), style = Btn.Line)
    }
}

// ------------------------------------------------------------------ 11.02 Personal details

@Composable
fun PersonalScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    var sheet by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    // email
    var email by rememberSaveable { mutableStateOf("") }
    // phone
    var phone by rememberSaveable { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    // address
    var postcode by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val results = remember(postcode) { Catalog.addressesFor(postcode) }
    fun close() { sheet = null; call.error.value = null }
    val lock: @Composable () -> Unit = { Ic(Icons.Lock, size = 16.dp, tint = C.Slate) }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 14.dp,
        overlay = {
            Sheet(sheet == "email", { close() }) {
                SheetHeader("Change email", { close() }, "Receipts, statements and password resets go here.")
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArroField(email, { email = it.trim() }, label = "New email", placeholder = "name@example.com", keyboard = KeyboardType.Email, error = call.error.value)
                    ArroButton("Save and verify", {
                        scope.launchCall(call) {
                            b.updateEmail(email); close(); b.sendEmailVerification()
                        }
                    }, loading = call.busy.value, enabled = email.contains("@"))
                }
            }
            Sheet(sheet == "phone", { close() }) {
                SheetHeader("Change mobile number", { close() }, if (!codeSent) "We'll text a code to the new number to check it's yours." else "Enter the 6-digit code we sent to ${Text.formatPhone(Validation.normaliseUkMobile(phone))}.")
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!codeSent) {
                        ArroField(phone, { phone = it.filter { ch -> ch.isDigit() }.take(11) }, label = "New UK mobile", placeholder = "7700 900123", keyboard = KeyboardType.Phone,
                            prefix = { DialPrefix("+44", "gb") }, error = call.error.value)
                        ArroButton("Send code", { scope.launchCall(call) { b.sendPhoneChangeCode(phone); codeSent = true; code = "" } }, loading = call.busy.value, enabled = phone.length >= 10)
                    } else {
                        CodeInput(code, { code = it }, error = call.error.value != null, onComplete = { entered ->
                            scope.launchCall(call) { b.confirmPhoneChange(entered); close(); codeSent = false; phone = ""; banner.show("Mobile number updated", kind = BannerKind.Success) }
                        })
                        FieldHint(call.error.value)
                        LinkText("Use a different number", { codeSent = false; call.error.value = null }, color = C.Ink, strong = true)
                    }
                }
            }
            Sheet(sheet == "address", { close() }, fullHeight = true) {
                SheetHeader("Change home address", { close() }, "Moving? We may ask for proof of address if your limits are Enhanced.")
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroField(postcode, { postcode = it.uppercase().take(8); selected = 0 }, label = "Postcode", placeholder = "E8 2PL",
                        capitalization = KeyboardCapitalization.Characters, prefix = { Ic(Icons.Search, tint = C.Slate) })
                    if (results.isEmpty() && postcode.length >= 5) Cap("No addresses found for that postcode.")
                    results.forEachIndexed { i, ad ->
                        ListRow(ad.line1, subtitle = "${ad.city} ${ad.postcode}", selected = i == selected, minHeight = 52.dp, leading = { Radio(i == selected) }, onClick = { selected = i })
                    }
                    FieldHint(call.error.value)
                    ArroButton("Save address", {
                        scope.launchCall(call) {
                            b.updateAddress(results[selected.coerceIn(0, results.lastIndex)]); close()
                            banner.show("Address updated", kind = BannerKind.Success)
                        }
                    }, loading = call.busy.value, enabled = results.isNotEmpty())
                }
            }
        },
    ) {
        ScreenTitle("Personal details")
        ArroField(a.displayName.takeIf { a.profile.displayName.isNotBlank() } ?: "Not provided yet", {}, label = "Legal name", readOnly = true, trailing = lock)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArroField(a.profile.dob.takeIf { it.isNotBlank() }?.let { Text.isoToDisplay(it) } ?: "—", {}, Modifier.weight(1f), label = "Date of birth", readOnly = true, trailing = lock)
            ArroField(a.profile.nationality.ifBlank { "—" }, {}, Modifier.weight(1f), label = "Nationality", readOnly = true, trailing = lock)
        }
        ArroField(a.email, {}, label = "Email", readOnly = true, trailing = {
            if (a.emailVerified) Pill("Verified", PillKind.Ok)
            else LinkText("Verify", { b.sendEmailVerification() }, color = C.Ink, strong = true)
        }, labelTrailing = { LinkText("Change", { email = a.email; sheet = "email" }, color = C.Ink, strong = true) })
        ArroField(Text.formatPhone(a.phone), {}, label = "Mobile number", readOnly = true,
            trailing = { LinkText("Change", { phone = ""; codeSent = false; sheet = "phone" }, color = C.Ink, strong = true) })
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Home address")
            Row(
                Modifier.fillMaxWidth().border(1.5.dp, C.Line, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val ad = a.profile.address
                Txt(if (ad != null) "${ad.line1}\n${ad.city} ${ad.postcode}" else "Not provided yet", T.Input.copy(lineHeight = 22.sp), Modifier.weight(1f))
                LinkText("Change", { postcode = ad?.postcode ?: ""; selected = 0; sheet = "address" }, color = C.Ink, strong = true)
            }
        }
        Note("Locked details come from your ID. To change them, chat with us and upload a new document.", NoteKind.Info, Icons.Info,
            action = "Chat with us", onAction = { app.arro.ui.screens.support.openChat(c, nav, null) })
    }
}

// ------------------------------------------------------------------ 11.03 Security

@Composable
fun SecurityScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val now = nowTicker(60_000)
    val bioAvailable = remember { Biometrics.available(context) }
    var freezeConfirm by remember { mutableStateOf(false) }
    var signOut by remember { mutableStateOf<String?>(null) }
    var passcodeSheet by remember { mutableStateOf(false) }
    var passwordSheet by remember { mutableStateOf(false) }
    val protections = listOf(a.biometricEnabled && bioAvailable, a.security.confirmNewDevices, a.security.passcodeOverThreshold, a.emailVerified)
    val on = protections.count { it }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 14.dp,
        overlay = {
            ConfirmDialog(freezeConfirm, if (a.frozen) "Unfreeze your account?" else "Freeze your account?",
                if (a.frozen) "You'll be able to send money and pay with your cards again." else "Sending money is blocked straight away. Money already on its way still arrives. You can unfreeze at any time.",
                if (a.frozen) "Unfreeze" else "Freeze now", {
                    b.setFrozen(!a.frozen); freezeConfirm = false
                    banner.show(if (a.frozen) "Account unfrozen" else "Account frozen", kind = if (a.frozen) BannerKind.Success else BannerKind.Info)
                }, { freezeConfirm = false }, danger = !a.frozen)
            val dev = a.devices.firstOrNull { it.id == signOut }
            ConfirmDialog(dev != null, "Sign out ${dev?.name ?: ""}?", "Anyone using it will need your password and an SMS code to sign in again.", "Sign out", {
                dev?.let { b.signOutDevice(it.id); banner.show("Signed out", it.name, BannerKind.Success) }; signOut = null
            }, { signOut = null }, danger = true)
            ChangePasscodeSheet(passcodeSheet) { passcodeSheet = false }
            ChangePasswordSheet(passwordSheet) { passwordSheet = false }
        },
    ) {
        ScreenTitle("Security")
        if (a.frozen) {
            Panel(kind = PanelKind.Grey, padding = 14.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tile(Icons.Lock, TileStyle.Err, 52.dp, iconSize = 24.dp)
                    Column(Modifier.weight(1f)) {
                        Txt("Your account is frozen", T.H3)
                        Cap("Sending is blocked until you unfreeze it.")
                    }
                }
            }
        } else Panel(kind = if (on == protections.size) PanelKind.Lime else PanelKind.Grey, padding = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Illustration(Art.Shield, 52.dp)
                Column {
                    Txt(if (on == protections.size) "Your account is well protected" else "$on of ${protections.size} protections are on", T.H3)
                    Cap(if (on == protections.size) "All ${protections.size} recommended protections are on." else "Turn on the rest below to keep your money safe.", color = C.Forest)
                }
            }
        }
        Group {
            ToggleRow(if (bioAvailable) "Fingerprint or face unlock" else "Fingerprint or face unlock",
                if (bioAvailable) "Unlock and approve transfers" else "Set up a fingerprint or face in Android settings first", a.biometricEnabled && bioAvailable, enabled = bioAvailable) { wantOn ->
                if (!wantOn) { b.setBiometric(false); return@ToggleRow }
                val activity = context as? FragmentActivity ?: return@ToggleRow
                Biometrics.prompt(activity, "Turn on $BIO_NAME unlock", "Confirm it's you", negative = "Cancel",
                    onSuccess = { b.setBiometric(true); banner.show("Biometric unlock is on", kind = BannerKind.Success) },
                    onError = { banner.show("Couldn't turn it on", it, BannerKind.Error) })
            }
            GroupDivider()
            ToggleRow("Confirm new devices by SMS", "A code to ${Text.maskPhoneShort(a.phone)}", a.security.confirmNewDevices) { v -> b.updateSecurity { it.copy(confirmNewDevices = v) } }
            GroupDivider()
            ToggleRow("Passcode for transfers over £1,000", "Even when biometric unlock is on", a.security.passcodeOverThreshold) { v -> b.updateSecurity { it.copy(passcodeOverThreshold = v) } }
            GroupDivider()
            ListRow("Change passcode", flat = true, minHeight = 52.dp,
                subtitle = a.security.passcodeChangedAt.takeIf { it > 0 }?.let { "Changed ${Text.relativeDay(it, now)}" }, trailing = { Chevron() }, onClick = { passcodeSheet = true })
            GroupDivider()
            ListRow("Change password", flat = true, minHeight = 52.dp,
                subtitle = a.security.passwordChangedAt.takeIf { it > 0 }?.let { "Changed ${Text.relativeDay(it, now)}" }, trailing = { Chevron() }, onClick = { passwordSheet = true })
        }
        Over("Signed-in devices")
        Group {
            a.devices.sortedByDescending { it.current }.forEachIndexed { i, d ->
                if (i > 0) GroupDivider()
                ListRow(d.name, flat = true, minHeight = 56.dp, subtitle = "${d.location} · ${if (d.current) "this device" else Text.relativeDay(d.lastActive, now)}",
                    leading = { Tile(if (d.laptop) Icons.Laptop else Icons.Smartphone, size = 36.dp, iconSize = 18.dp) },
                    trailing = { if (!d.current) ArroButton("Sign out", { signOut = d.id }, style = Btn.Line, size = BtnSize.S) })
            }
        }
        ArroButton(if (a.frozen) "Unfreeze my account" else "Freeze my account now", { freezeConfirm = true },
            style = if (a.frozen) Btn.Dark else Btn.Danger, icon = Icons.Lock)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ChangePasscodeSheet(visible: Boolean, onDone: () -> Unit) {
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    var step by remember(visible) { mutableIntStateOf(0) }
    var current by remember(visible) { mutableStateOf("") }
    var first by remember(visible) { mutableStateOf("") }
    var entry by remember(visible) { mutableStateOf("") }
    var error by remember(visible) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    fun complete(code: String) {
        when (step) {
            0 -> scope.launchCall(call) {
                if (b.checkPasscode(code)) { current = code; step = 1; entry = ""; error = null } else { error = "That's not your current passcode."; entry = "" }
            }
            1 -> if (Validation.passcodeWeak(code)) { error = "Avoid repeated or simple sequences like 123456."; entry = "" } else { first = code; step = 2; entry = ""; error = null }
            else -> if (code != first) { error = "Those didn't match. Enter your new passcode again."; entry = ""; step = 1 } else scope.launchCall(call, onError = { e -> error = e.message; entry = ""; step = 0; true }) {
                b.changePasscode(current, code)
                banner.show("Passcode changed", kind = BannerKind.Success)
                onDone()
            }
        }
    }
    Sheet(visible, onDone, fullHeight = true) {
        SheetHeader(when (step) { 0 -> "Enter your current passcode"; 1 -> "Choose a new passcode"; else -> "Confirm your new passcode" }, onDone,
            "Step ${step + 1} of 3")
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PasscodeDots(entry.length, error = error != null)
            Txt(error ?: " ", T.Cap, color = C.Err, align = TextAlign.Center)
            Keypad(onKey = { k -> if (entry.length < 6 && !call.busy.value) { entry += k; error = null; if (entry.length == 6) complete(entry) } },
                onDelete = { entry = entry.dropLast(1) }, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ChangePasswordSheet(visible: Boolean, onDone: () -> Unit) {
    val b = container().backend
    val banner = LocalBanner.current
    var current by remember(visible) { mutableStateOf("") }
    var next by remember(visible) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    Sheet(visible, onDone, fullHeight = true) {
        SheetHeader("Change password", onDone, "You'll stay logged in on this phone.")
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PasswordField(current, { current = it }, "Current password")
            PasswordField(next, { next = it }, "New password")
            PasswordStrength(next)
            FieldHint(call.error.value)
            ArroButton("Change password", {
                scope.launchCall(call) { b.changePassword(current, next); banner.show("Password changed", kind = BannerKind.Success); onDone() }
            }, loading = call.busy.value, enabled = current.isNotEmpty() && Validation.password(next).required)
        }
    }
}

// ------------------------------------------------------------------ 11.04 Verification and limits

@Composable
fun VerificationScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val k = a.kyc
    var docsSheet by remember { mutableStateOf(false) }
    var addressDoc by rememberSaveable { mutableStateOf<String?>(null) }
    var incomeDoc by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf("address") }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val picker = rememberPhotoPicker({ File(context.filesDir, "enhanced-$picking-${System.currentTimeMillis()}.jpg") }) { f ->
        if (picking == "address") addressDoc = f.name else incomeDoc = f.name
    }
    val usage = b.usage(a)
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        footer = {
            when {
                k.status != KycStatus.APPROVED -> ArroButton(if (k.status == KycStatus.IN_REVIEW) "See my check" else "Verify my identity", { nav.go(if (k.status == KycStatus.IN_REVIEW) Routes.KYC_STATUS else Routes.KYC) })
                k.level == KycLevel.STANDARD && !k.enhancedRequested -> ArroButton("Raise my limits", { docsSheet = true })
                k.enhancedRequested -> ArroButton("We're reviewing your documents", {}, enabled = false)
                else -> {}
            }
        },
        overlay = {
            Sheet(docsSheet, { docsSheet = false }, fullHeight = true) {
                SheetHeader("Raise your limits", { docsSheet = false }, "Upload two documents. We usually review them within a working day.")
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListRow("Proof of address", subtitle = addressDoc?.let { "Added · tap to replace" } ?: "Bank statement or utility bill from the last 3 months",
                        leading = { Tile(if (addressDoc != null) Icons.Check else Icons.Document, if (addressDoc != null) TileStyle.Ok else TileStyle.Tint) },
                        trailing = { Chevron() }, onClick = { picking = "address"; picker() })
                    ListRow("Proof of income", subtitle = incomeDoc?.let { "Added · tap to replace" } ?: "A recent payslip, tax return or business accounts",
                        leading = { Tile(if (incomeDoc != null) Icons.Check else Icons.CashSimple, if (incomeDoc != null) TileStyle.Ok else TileStyle.Tint) },
                        trailing = { Chevron() }, onClick = { picking = "income"; picker() })
                    Note("Photos or screenshots are fine. Make sure your name and the date are visible.", NoteKind.Info, Icons.Info)
                    FieldHint(call.error.value)
                    ArroButton("Submit documents", {
                        scope.launchCall(call) {
                            b.requestEnhanced(); docsSheet = false
                            banner.show("Documents sent", "We'll let you know when your limits go up.", BannerKind.Success)
                        }
                    }, loading = call.busy.value, enabled = addressDoc != null && incomeDoc != null)
                }
            }
        },
    ) {
        ScreenTitle("Verification\nand limits")
        ArroCard(padding = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Illustration(if (k.status == KycStatus.REJECTED) Art.IdCardWarn else Art.IdCard, 60.dp)
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(if (a.verified) k.level.label else "Not verified", T.H3)
                        when (k.status) {
                            KycStatus.APPROVED -> Pill("Verified", PillKind.Ok, dot = true)
                            KycStatus.IN_REVIEW -> Pill("Checking", PillKind.Info, dot = true)
                            KycStatus.REJECTED -> Pill("Action needed", PillKind.Warn, dot = true)
                            KycStatus.NOT_STARTED -> Pill("To do", PillKind.Neutral)
                        }
                    }
                    Cap(when (k.status) {
                        KycStatus.APPROVED -> "${k.docType.label} checked ${Text.date(k.decidedAt.takeIf { it > 0 } ?: k.submittedAt.takeIf { it > 0 } ?: a.createdAt)}" +
                            (if (k.docExpiry.isNotBlank()) " · expires ${k.docExpiry}" else "")
                        KycStatus.IN_REVIEW -> "We're checking your ${k.docType.label.lowercase()}. Usually a few minutes."
                        KycStatus.REJECTED -> k.rejectReason.ifBlank { "We need a clearer photo of your ID." }
                        KycStatus.NOT_STARTED -> "Verify your identity to start sending. It takes about 3 minutes."
                    }, Modifier.padding(top = 2.dp))
                }
            }
        }
        if (a.verified) {
            val level = k.level
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LimitLine("Per transfer", null, level.perTransferMinor)
                LimitLine("This month", usage.monthMinor, level.perMonthMinor)
                LimitLine("This year", usage.yearMinor, level.perYearMinor)
            }
            if (level == KycLevel.STANDARD) Panel(gap = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Txt("Enhanced", T.H3, Modifier.weight(1f))
                    Pill(if (k.enhancedRequested) "In review" else "Next level", if (k.enhancedRequested) PillKind.Info else PillKind.Neutral, dot = k.enhancedRequested)
                }
                Cap("Send up to ${Money.format(KycLevel.ENHANCED.perTransferMinor, Currency.GBP, false)} per transfer and ${Money.format(KycLevel.ENHANCED.perMonthMinor, Currency.GBP, false)} a month.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Ic(Icons.Document, size = 16.dp); Txt("Proof of address from the last 3 months", T.Body14) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Ic(Icons.CashSimple, size = 16.dp); Txt("Proof of income, like a payslip", T.Body14) }
            } else Note("You're on our highest level. Need more? Chat with us about a business account.", NoteKind.Lime, Icons.ShieldCheck)
        } else {
            Note("Until you're verified you can explore the app, add recipients and check rates, but not send money.", NoteKind.Info, Icons.Info)
        }
    }
}

@Composable
private fun LimitLine(label: String, used: Long?, limit: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, T.Body14.copy(fontWeight = FontWeight.Medium), Modifier.weight(1f))
            if (used == null) Txt(Money.format(limit, Currency.GBP, false), T.Num.copy(fontSize = 14.sp))
            else Txt(androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold)); append(Money.format(used, Currency.GBP, false)); pop()
                append(" of ${Money.format(limit, Currency.GBP, false)}")
            }, T.Body14.copy(fontFeatureSettings = "tnum"))
        }
        if (used != null) {
            val f = (used.toFloat() / limit).coerceIn(0f, 1f)
            ProgressBar(if (used > 0) f.coerceAtLeast(0.02f) else 0f, fill = if (f > 0.9f) C.WarnFill else C.Ink)
        }
    }
}

// ------------------------------------------------------------------ 11.05 Notifications

@Composable
fun NotificationSettingsScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val s = a.settings
    var permitted by remember { mutableStateOf(c.notifier.permitted()) }
    var timePicker by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> permitted = ok || c.notifier.permitted() }
    val hours = (0..23).map { "%02d:00".format(it) }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 14.dp,
        overlay = {
            val tp = timePicker
            PickerSheet(tp != null, if (tp == "from") "Quiet from" else "Quiet until", hours, if (tp == "from") s.quietFrom else s.quietTo, { h ->
                b.updateSettings { if (tp == "from") it.copy(quietFrom = h) else it.copy(quietTo = h) }
            }, { timePicker = null })
        },
    ) {
        ScreenTitle("Notifications", "Choose what we tell you about, and how.")
        if (!permitted) Note("Notifications for Arro are turned off on this phone, so you won't hear when money arrives.", NoteKind.Warn, Icons.Bell, action = "Turn on", onAction = {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        })
        Over("Transfers")
        Group {
            ToggleRow("Transfer updates", "Sent, delivered or delayed · push and email", s.transferUpdates) { v -> b.updateSettings { it.copy(transferUpdates = v) } }
            GroupDivider()
            ToggleRow("Text recipients on arrival", "Free SMS to the person you paid", s.smsRecipients) { v -> b.updateSettings { it.copy(smsRecipients = v) } }
            GroupDivider()
            ToggleRow("Rate alerts", "When a rate you're watching hits", s.rateAlerts) { v -> b.updateSettings { it.copy(rateAlerts = v) } }
        }
        Over("Account")
        Group {
            ListRow("Security alerts", flat = true, minHeight = 58.dp, subtitle = "Always on to keep you safe", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Ic(Icons.Lock, size = 14.dp, tint = C.Slate); Cap("On") }
            })
            GroupDivider()
            ToggleRow("Monthly statements", "By email, on the 1st", s.statements) { v -> b.updateSettings { it.copy(statements = v) } }
            GroupDivider()
            ToggleRow("News and offers", "New countries and promotions", s.news) { v -> b.updateSettings { it.copy(news = v) } }
        }
        Over("Quiet hours")
        Group {
            ToggleRow("${s.quietFrom} to ${s.quietTo}", "Security alerts still come through", s.quietHours) { v -> b.updateSettings { it.copy(quietHours = v) } }
            if (s.quietHours) {
                GroupDivider()
                ListRow("Starts", flat = true, minHeight = 52.dp, trailing = { Row(verticalAlignment = Alignment.CenterVertically) { Cap(s.quietFrom); Chevron() } }, onClick = { timePicker = "from" })
                GroupDivider()
                ListRow("Ends", flat = true, minHeight = 52.dp, trailing = { Row(verticalAlignment = Alignment.CenterVertically) { Cap(s.quietTo); Chevron() } }, onClick = { timePicker = "to" })
            }
        }
    }
}

// ------------------------------------------------------------------ 11.06 Language and region

@Composable
fun LanguageScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val s = a.settings
    val languages = listOf("English (UK)", "Kinyarwanda", "Français", "Kiswahili", "Português")
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }, gap = 14.dp) {
        ScreenTitle("Language\nand region")
        Over("App language")
        Group {
            languages.forEachIndexed { i, lang ->
                if (i > 0) GroupDivider()
                val available = i == 0
                ListRow(lang, flat = true, minHeight = 52.dp, subtitle = if (available) null else "Coming soon",
                    trailing = { Radio(s.language == lang) },
                    onClick = {
                        if (available) b.updateSettings { it.copy(language = lang) }
                        else banner.show("$lang is coming soon", "We'll let you know when Arro speaks $lang. Recipients already get SMS in their language.")
                    })
            }
        }
        Over("Region and display")
        Group {
            ListRow("Country of residence", flat = true, minHeight = 52.dp, trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Flag("gb", 18.dp); Cap("United Kingdom") }
            })
            GroupDivider()
            ListRow("Number format", flat = true, minHeight = 52.dp, trailing = { Cap("1,234.56") })
            GroupDivider()
            ListRow("Date format", flat = true, minHeight = 52.dp, trailing = { Cap(Text.numericDate(b.now)) })
        }
    }
}

// ------------------------------------------------------------------ 11.07 Invite friends

@Composable
fun ReferScreen() {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    val code = a.referral.code.ifBlank { (a.profile.firstNames.uppercase().take(4).ifBlank { "ARRO" }) + "-" + a.id.take(4).uppercase() }
    val link = "https://arro.app/i/$code"
    ArroScreen(
        background = C.Night,
        topBar = { TopBar(onBack = { nav.back() }) },
        footer = {
            ArroButton("Share invite link", {
                Share.text(context, "I use Arro to send money home. Your first transfer has no Arro fee: $link (code $code)", "Try Arro")
            }, style = Btn.Lime, icon = Icons.Share)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                LinkText("Terms apply", { nav.go(Routes.legal("referral")) }, color = C.DarkCap, underline = true)
                Cap(" · Rewards are paid into your GBP balance", color = C.DarkCap, size = 12f)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(320.dp).offset(y = 30.dp).background(Brush.radialGradient(listOf(C.Lime.copy(alpha = 0.18f), C.Lime.copy(alpha = 0f))), CircleShape))
            Illustration(Art.Gift, 200.dp, description = "A gift box")
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle("Invite friends,\nget £10", size = TitleSize.XL, color = C.Lime, align = TextAlign.Center)
            Txt("Friends' first transfer has no Arro fee. You get £10 when they send £100 or more.", T.Lede, color = C.DarkCap, align = TextAlign.Center)
        }
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth().dashedBorder().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Cap("Your invite code", color = C.DarkCap, size = 12f)
                Txt(code, T.Big22.copy(letterSpacing = 1.3.sp), Modifier.padding(top = 2.dp), color = C.OnNight)
            }
            ArroButton("Copy", { Share.copy(context, "Arro invite code", code); banner.show("Invite code copied", kind = BannerKind.Success) },
                style = Btn.LineOnDark, size = BtnSize.S, icon = Icons.Copy)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Cap("${a.referral.friendsJoined} ${if (a.referral.friendsJoined == 1) "friend" else "friends"} joined", color = C.DarkCap)
            Txt("${Money.format(a.referral.earnedMinor, Currency.GBP)} earned", T.Body14.copy(fontWeight = FontWeight.SemiBold), color = C.Lime)
        }
    }
}

/** The dashed lime outline around the invite code. */
private fun Modifier.dashedBorder(color: androidx.compose.ui.graphics.Color = C.Lime.copy(alpha = 0.5f)): Modifier = this.drawBehind {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
    drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = stroke)
}

// ------------------------------------------------------------------ 11.08 Account management

@Composable
fun ManageScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val context = LocalContext.current
    val state by appState()
    val a = state.account ?: return
    var privacy by remember { mutableStateOf(false) }
    var pause by remember { mutableStateOf(false) }
    var close by remember { mutableStateOf(false) }
    var understood by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }) },
        gap = 14.dp,
        overlay = {
            Sheet(privacy, { privacy = false }) {
                SheetHeader("Privacy choices", { privacy = false }, "Change these whenever you like. They never affect your transfers.")
                Group(Modifier.padding(top = 14.dp)) {
                    ToggleRow("Help improve Arro", "Anonymous usage analytics", a.settings.analytics) { v -> b.updateSettings { it.copy(analytics = v) } }
                    GroupDivider()
                    ToggleRow("Marketing", "Offers by email and push, never shared", a.settings.marketing) { v -> b.updateSettings { it.copy(marketing = v) } }
                }
            }
            ConfirmDialog(pause, "Pause your account?", "You'll be logged out and nobody can send money or log in until you log in again with your password.", "Pause account", {
                pause = false; b.pauseAccount()
            }, { pause = false })
            Sheet(close, { close = false; understood = false }) {
                SheetHeader("Close your Arro account", { close = false; understood = false }, "This can't be undone.")
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val balances = a.wallets.filter { it.balanceMinor != 0L }
                    if (balances.isNotEmpty()) Note("You still have ${balances.joinToString { Money.format(it.balanceMinor, it.cur) }}. Send or convert it first.", NoteKind.Warn, Icons.Warning,
                        action = "Go to wallet", onAction = { close = false; nav.go(Routes.WALLET) })
                    Cap("We'll delete your recipients, cards and settings. Transfer records are kept for 5 years, as the law requires.")
                    CheckRow(understood, { understood = it }, "I understand my account will be closed for good")
                    FieldHint(call.error.value)
                    ArroButton("Close my account", { scope.launchCall(call) { b.closeAccount() } }, style = Btn.Danger, loading = call.busy.value, enabled = understood)
                }
            }
        },
    ) {
        ScreenTitle("Account\nmanagement")
        Over("Your data")
        Group {
            ListRow("Download my data", flat = true, subtitle = "Everything we hold about you, as a JSON file", leading = { Tile(Icons.Download, size = 36.dp, iconSize = 18.dp) }, trailing = { Chevron() },
                onClick = {
                    scope.launchCall(call) {
                        val f = withContext(Dispatchers.IO) { Share.cacheFile(context, "arro-my-data.json").apply { writeText(b.exportData()) } }
                        banner.show("Your data is ready", Share.saveToDownloads(context, f, "application/json"), BannerKind.Success)
                    }
                })
            GroupDivider()
            ListRow("Privacy choices", flat = true, subtitle = "Analytics and marketing consent", leading = { Tile(Icons.Eye, size = 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { privacy = true })
        }
        Over("Your account")
        Group {
            ListRow("Close a currency account", flat = true, subtitle = a.wallets.filter { !it.main }.joinToString(" or ") { it.currency }.ifBlank { "You only have your main GBP account" },
                leading = { Tile(Icons.WalletSimple, size = 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.WALLET) })
            GroupDivider()
            ListRow("Pause my account", flat = true, subtitle = "Block sending and logins until you return", leading = { Tile(Icons.Pause, TileStyle.Warn, 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { pause = true })
            GroupDivider()
            ListRow("Close my account", flat = true, titleStyle = T.RowTitle.copy(color = C.Err), subtitle = "Permanent. You'll need a zero balance first.",
                leading = { Tile(Icons.Trash, TileStyle.Err, 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { call.error.value = null; close = true })
        }
        Note("Even after closing, the law requires us to keep transfer records for 5 years. We never use them for marketing.", NoteKind.Info, Icons.Info)
        Over("Legal")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Terms of use", false, { nav.go(Routes.legal("terms")) })
            Chip("Privacy policy", false, { nav.go(Routes.legal("privacy")) })
            Chip("Complaints", false, { nav.go(Routes.legal("complaints")) })
        }
    }
}

// ------------------------------------------------------------------ Demo controls (test builds)

@Composable
fun DemoScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account
    val demo = state.demo
    var outagePicker by remember { mutableStateOf(false) }
    var erase by remember { mutableStateOf(false) }
    val providers = Catalog.providers.filter { it.id != "arro" }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Demo controls") },
        gap = 14.dp,
        overlay = {
            PickerSheet(outagePicker, "Make a provider unavailable", listOf("None") + providers.map { it.name }, providers.firstOrNull { it.id == demo.outageProviderId }?.name ?: "None", { name ->
                val id = providers.firstOrNull { it.name == name }?.id ?: ""
                b.setDemo { it.copy(outageProviderId = id) }
                banner.show(if (id.isEmpty()) "All providers available" else "$name is now offline", if (id.isEmpty()) "" else "Sending with it shows the unavailable screen.")
            }, { outagePicker = false }, subtitles = providers.associate { it.name to Catalog.country(it.country).name })
            ConfirmDialog(erase, "Erase everything?", "This deletes the account, transfers and settings stored on this phone and starts again from the welcome screen.", "Erase", {
                erase = false; b.resetEverything(); nav.resetTo(Routes.WELCOME)
            }, { erase = false }, danger = true)
        },
    ) {
        ScreenTitle("Try every path", "These controls exist only in this test build. No real money moves; the backend runs on this phone.")
        Group {
            ToggleRow("Fast mode", "Transfers, checks and refunds finish in about a minute instead of real time", demo.fast) { v -> b.setDemo { it.copy(fast = v) } }
            GroupDivider()
            ListRow("Provider outage", flat = true, minHeight = 58.dp, subtitle = providers.firstOrNull { it.id == demo.outageProviderId }?.name ?: "None", trailing = { Chevron() }, onClick = { outagePicker = true })
            GroupDivider()
            ToggleRow("Ask for a retake on the next ID check", "Tests the rejected-photo path", demo.rejectNextKyc) { v -> b.setDemo { it.copy(rejectNextKyc = v) } }
        }
        Group {
            ListRow("Finish everything that's waiting", flat = true, minHeight = 56.dp, subtitle = "Deliver transfers, refunds and ID checks now",
                leading = { Tile(Icons.Bolt, TileStyle.Lime, 36.dp, iconSize = 18.dp) }, enabled = a != null,
                onClick = { b.fastForward(); banner.show("Done", "Everything waiting has moved on.", BannerKind.Success) })
            GroupDivider()
            ListRow("Lift the 24-hour sending pause", flat = true, minHeight = 56.dp, subtitle = if ((a?.sendPauseUntil ?: 0) > b.now) "Paused until ${Text.time(a!!.sendPauseUntil)}" else "Not paused",
                leading = { Tile(Icons.Clock, size = 36.dp, iconSize = 18.dp) }, enabled = a != null,
                onClick = { b.clearSendPause(); banner.show("Sending pause lifted", kind = BannerKind.Success) })
            GroupDivider()
            ListRow("Load the sample account", flat = true, minHeight = 56.dp, subtitle = "Eric Nshuti, with a year of history",
                leading = { Tile(Icons.User, size = 36.dp, iconSize = 18.dp) },
                onClick = { b.loadDemoAccount(); banner.show("Sample account loaded", "Passcode ${Seed.DEMO_PASSCODE}", BannerKind.Success); nav.resetTo(Routes.HOME) })
            GroupDivider()
            ListRow("Erase everything on this phone", flat = true, minHeight = 56.dp, titleStyle = T.RowTitle.copy(color = C.Err),
                leading = { Tile(Icons.Trash, TileStyle.Err, 36.dp, iconSize = 18.dp) }, onClick = { erase = true })
        }
        Panel(gap = 8.dp) {
            Txt("Test triggers", T.H3)
            listOf(
                "Sample login" to "${Seed.DEMO_EMAIL} · ${Seed.DEMO_PASSWORD}",
                "Passcode" to Seed.DEMO_PASSCODE,
                "Delivery fails, then refunds" to "Payout number or account ending 000",
                "Name check: not found" to "Number ending 404",
                "Name check: mismatch" to "Number ending 999",
                "Security hold" to "Any transfer over £1,000",
                "Card declined by the bank" to "Card number ending 0002",
                "Credit card (1.75% fee)" to "Mastercard starting 51 to 53",
            ).forEach { (k, v) ->
                Column {
                    Txt(k, T.Body14.copy(fontWeight = FontWeight.Medium))
                    Cap(v, size = 12.5f)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Legal documents

@Composable
fun LegalScreen(doc: String) {
    val nav = LocalNav.current
    val (title, sections) = when (doc) {
        "privacy" -> "Privacy policy" to listOf(
            "What we collect" to "Your name, contact details, identity documents, the people you send to and the transfers you make. We need these to provide the service and to meet anti-money-laundering law.",
            "How we use it" to "To move your money, keep your account secure, prevent fraud and meet our legal duties. With your consent, we also use anonymous analytics to improve the app.",
            "Who we share it with" to "Payment partners, payout providers in the recipient's country, identity verification services and regulators when the law requires. We never sell your data.",
            "How long we keep it" to "Transfer records for 5 years after your account closes, as the law requires. Everything else is deleted when you close your account.",
            "Your rights" to "You can download your data, correct it, or ask us to delete it, from Account management or by chatting with us.",
        )
        "complaints" -> "Complaints" to listOf(
            "Tell us" to "Chat with us in the app or email complaints@[DOMAIN]. We'll acknowledge your complaint within 1 working day.",
            "Our response" to "We aim to resolve complaints within 15 business days. In exceptional cases it can take up to 35 business days, and we'll tell you why.",
            "If you're not happy" to "You can refer your complaint to the Financial Ombudsman Service, free of charge, within 6 months of our final response.",
        )
        "referral" -> "Invite rewards" to listOf(
            "Who can take part" to "Verified Arro customers in the UK. Your friend must be new to Arro and use your invite link or code when they sign up.",
            "The reward" to "Your friend's first transfer has no Arro fee, up to £1,000. You get £10 in your GBP balance once their first transfer of £100 or more is delivered.",
            "Limits" to "Up to 20 rewards a year. We may withhold rewards if we see misuse.",
        )
        else -> "Terms of use" to listOf(
            "About us" to "Arro Ltd is authorised by [REGULATOR] as [LICENCE TYPE], reference [LICENCE NO.]. Registered office: [ADDRESS].",
            "Your account" to "You must be 18 or over and live in the UK. Keep your password and passcode secret; we will never ask for them.",
            "Sending money" to "We show the fee, exchange rate and amount your recipient gets before you pay. Your rate is locked for 30 minutes.",
            "Cancellations and refunds" to "You can cancel a transfer that hasn't been delivered and get a full refund, fee included.",
            "Safeguarding" to "Money you hold with Arro is safeguarded in a separate account at [BANK], so it's protected if anything happens to us.",
        )
    }
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }, gap = 14.dp) {
        ScreenTitle(title, "Last updated 1 Oct 2026 · Draft text for testing")
        sections.forEach { (h, body) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(h, T.H3)
                Txt(body, T.Body.copy(lineHeight = 23.sp), color = C.Ink)
            }
        }
        Note("This is placeholder legal text for a test build, not a real agreement.", NoteKind.Warn, Icons.Info)
    }
}
