package app.arro.ui.screens.account

import app.arro.R
import app.arro.ui.str
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
            ConfirmDialog(logoutConfirm, str(R.string.account_log_out_of_arro), str(R.string.account_you_ll_need_your_email_and), str(R.string.account_log_out), {
                logoutConfirm = false
                b.logout()
                nav.resetTo(Routes.LOGIN)
            }, { logoutConfirm = false })
        },
    ) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 14.dp)) {
            RoundIcon(Icons.Help, { nav.go(Routes.HELP) }, Modifier.align(Alignment.TopEnd).padding(end = 20.dp), style = IconBtn.Soft, description = str(R.string.account_help))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(88.dp)) {
                    Avatar(a.initials, 0, 84.dp, modifier = Modifier.align(Alignment.Center))
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(28.dp).background(C.Lime, CircleShape).border(3.dp, C.White, CircleShape)
                            .then(Modifier.padding(0.dp)),
                        contentAlignment = Alignment.Center,
                    ) { RoundIcon(Icons.Pencil, { nav.go(Routes.PERSONAL) }, style = IconBtn.Lime, size = 26.dp, iconSize = 13.dp, description = str(R.string.account_edit_personal_details)) }
                }
                Txt(a.displayName, T.Title.copy(fontSize = 22.sp), Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cap("@" + a.username.ifBlank { a.profile.firstNames.lowercase().take(4) + a.profile.lastName.lowercase().take(1) })
                    when (a.kyc.status) {
                        KycStatus.APPROVED -> Pill(str(R.string.account_verified), PillKind.Ok, dot = true)
                        KycStatus.IN_REVIEW -> Pill(str(R.string.account_checking_id), PillKind.Info, dot = true)
                        else -> Pill(str(R.string.account_not_verified), PillKind.Warn, dot = true)
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Panel(kind = PanelKind.Dark, padding = 14.dp, onClick = { nav.go(Routes.REFER) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(C.Lime.copy(alpha = 0.16f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Ic(Icons.Gift, tint = C.Lime) }
                    Column(Modifier.weight(1f)) {
                        Txt(str(R.string.account_invite_friends), T.H3, color = C.OnNight)
                        Cap(str(R.string.account_get_10_for_every_friend_who), color = C.DarkCap, size = 12f)
                    }
                    Ic(Icons.ChevronRight, tint = C.Lime)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Over(str(R.string.account_account))
                Group {
                    SettingsRow(str(R.string.account_personal_details), Icons.User) { nav.go(Routes.PERSONAL) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_verification_and_limits), Icons.IdCard, if (a.verified) a.kyc.level.label else str(R.string.account_not_verified)) { nav.go(Routes.VERIFICATION) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_payment_methods), Icons.CreditCard, a.paymentMethods.count { it.status == MethodStatus.ACTIVE }.takeIf { it > 0 }?.toString()) { nav.go(Routes.PAY_METHODS) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_wallet_and_balances), Icons.WalletSimple, "${a.wallets.size}") { nav.go(Routes.WALLET) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_loyalty_and_rewards), Icons.Award, str(R.string.account_x_pts, Money.points(a.pointsBalance))) { nav.go(Routes.LOYALTY) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_statements), Icons.Statement) { nav.go(Routes.STATEMENTS) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Over(str(R.string.account_security_and_preferences))
                Group {
                    SettingsRow(str(R.string.account_security), Icons.ShieldCheck, if (a.frozen) str(R.string.account_frozen) else null) { nav.go(Routes.SECURITY) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_notifications), Icons.Bell) { nav.go(Routes.NOTIFICATION_SETTINGS) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_appearance), Icons.Sun, appearanceLabel(a.settings.appearance)) { appearanceOpen = true }
                    GroupDivider()
                    SettingsRow(str(R.string.account_language_and_region), Icons.Globe, a.settings.language) { nav.go(Routes.LANGUAGE) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Over(str(R.string.account_help_and_account))
                Group {
                    SettingsRow(str(R.string.account_help_centre_and_chat), Icons.Chat) { nav.go(Routes.HELP) }
                    GroupDivider()
                    SettingsRow(str(R.string.account_account_management), Icons.Filters) { nav.go(Routes.MANAGE) }
                    GroupDivider()
                    if (app.arro.core.DemoConfig.enabled) {
                        SettingsRow(str(R.string.account_demo_controls), Icons.Bolt, str(R.string.account_test_build), TileStyle.Lime) { nav.go(Routes.DEMO) }
                        GroupDivider()
                    }
                    SettingsRow(str(R.string.account_log_out), Icons.LogOut, tile = TileStyle.Err, titleColor = C.Err) { logoutConfirm = true }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Cap(str(R.string.account_arro_x, version), size = 12f)
                    LinkText(str(R.string.account_terms), { nav.go(Routes.legal("terms")) }, color = C.Ink)
                    Cap(" · ", size = 12f)
                    LinkText(str(R.string.account_privacy), { nav.go(Routes.legal("privacy")) }, color = C.Ink)
                }
            }
        }
    }
}

private fun appearanceLabel(a: Appearance) = when (a) { Appearance.LIGHT -> str(R.string.account_light); Appearance.DARK -> str(R.string.account_dark); Appearance.SYSTEM -> str(R.string.account_same_as_phone) }

/** Light, dark or follow the phone. The change applies straight away, so the sheet itself switches theme. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.AppearanceSheet(visible: Boolean, current: Appearance, onChoose: (Appearance) -> Unit, onDismiss: () -> Unit) {
    val lightBg = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val darkBg = androidx.compose.ui.graphics.Color(0xFF0A1412)
    Sheet(visible, onDismiss) {
        SheetHeader(str(R.string.account_appearance), onDismiss, str(R.string.account_choose_how_arro_looks_on_this))
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple(Appearance.LIGHT, str(R.string.account_light), str(R.string.account_bright_screens_the_classic_arro_look)),
                Triple(Appearance.DARK, str(R.string.account_dark), str(R.string.account_easier_on_the_eyes_at_night)),
                Triple(Appearance.SYSTEM, str(R.string.account_same_as_phone), str(R.string.account_switches_with_your_phone_s_dark)),
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
        ArroButton(str(R.string.account_done), onDismiss, Modifier.padding(top = 16.dp), style = Btn.Line)
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
                SheetHeader(str(R.string.account_change_email), { close() }, str(R.string.account_receipts_statements_and_password_resets_go))
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArroField(email, { email = it.trim() }, label = str(R.string.account_new_email), placeholder = "name@example.com", keyboard = KeyboardType.Email, error = call.error.value)
                    ArroButton(str(R.string.account_save_and_verify), {
                        scope.launchCall(call) {
                            b.updateEmail(email); close(); b.sendEmailVerification()
                        }
                    }, loading = call.busy.value, enabled = email.contains("@"))
                }
            }
            Sheet(sheet == "phone", { close() }) {
                SheetHeader(str(R.string.account_change_mobile_number), { close() }, if (!codeSent) str(R.string.account_we_ll_text_a_code_to) else str(R.string.account_enter_the_6_digit_code_we, Text.formatPhone(Validation.normaliseUkMobile(phone))))
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!codeSent) {
                        ArroField(phone, { phone = it.filter { ch -> ch.isDigit() }.take(11) }, label = str(R.string.account_new_uk_mobile), placeholder = "7700 900123", keyboard = KeyboardType.Phone,
                            prefix = { DialPrefix("+44", "gb") }, error = call.error.value)
                        ArroButton(str(R.string.account_send_code), { scope.launchCall(call) { b.sendPhoneChangeCode(phone); codeSent = true; code = "" } }, loading = call.busy.value, enabled = phone.length >= 10)
                    } else {
                        CodeInput(code, { code = it }, error = call.error.value != null, onComplete = { entered ->
                            scope.launchCall(call) { b.confirmPhoneChange(entered); close(); codeSent = false; phone = ""; banner.show(str(R.string.account_mobile_number_updated), kind = BannerKind.Success) }
                        })
                        FieldHint(call.error.value)
                        LinkText(str(R.string.account_use_a_different_number), { codeSent = false; call.error.value = null }, color = C.Ink, strong = true)
                    }
                }
            }
            Sheet(sheet == "address", { close() }, fullHeight = true) {
                SheetHeader(str(R.string.account_change_home_address), { close() }, str(R.string.account_moving_we_may_ask_for_proof))
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArroField(postcode, { postcode = it.uppercase().take(8); selected = 0 }, label = str(R.string.account_postcode), placeholder = str(R.string.account_e8_2pl),
                        capitalization = KeyboardCapitalization.Characters, prefix = { Ic(Icons.Search, tint = C.Slate) })
                    if (results.isEmpty() && postcode.length >= 5) Cap(str(R.string.account_no_addresses_found_for_that_postcode))
                    results.forEachIndexed { i, ad ->
                        ListRow(ad.line1, subtitle = "${ad.city} ${ad.postcode}", selected = i == selected, minHeight = 52.dp, leading = { Radio(i == selected) }, onClick = { selected = i })
                    }
                    FieldHint(call.error.value)
                    ArroButton(str(R.string.account_save_address), {
                        scope.launchCall(call) {
                            b.updateAddress(results[selected.coerceIn(0, results.lastIndex)]); close()
                            banner.show(str(R.string.account_address_updated), kind = BannerKind.Success)
                        }
                    }, loading = call.busy.value, enabled = results.isNotEmpty())
                }
            }
        },
    ) {
        ScreenTitle(str(R.string.account_personal_details))
        ArroField(a.displayName.takeIf { a.profile.displayName.isNotBlank() } ?: str(R.string.account_not_provided_yet), {}, label = str(R.string.account_legal_name), readOnly = true, trailing = lock)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArroField(a.profile.dob.takeIf { it.isNotBlank() }?.let { Text.isoToDisplay(it) } ?: "—", {}, Modifier.weight(1f), label = str(R.string.account_date_of_birth), readOnly = true, trailing = lock)
            ArroField(a.profile.nationality.ifBlank { "—" }, {}, Modifier.weight(1f), label = str(R.string.account_nationality), readOnly = true, trailing = lock)
        }
        ArroField(a.email, {}, label = str(R.string.account_email), readOnly = true, trailing = {
            if (a.emailVerified) Pill(str(R.string.account_verified), PillKind.Ok)
            else LinkText(str(R.string.account_verify), { b.sendEmailVerification() }, color = C.Ink, strong = true)
        }, labelTrailing = { LinkText(str(R.string.account_change), { email = a.email; sheet = "email" }, color = C.Ink, strong = true) })
        ArroField(Text.formatPhone(a.phone), {}, label = str(R.string.account_mobile_number), readOnly = true,
            trailing = { LinkText(str(R.string.account_change), { phone = ""; codeSent = false; sheet = "phone" }, color = C.Ink, strong = true) })
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(str(R.string.account_home_address))
            Row(
                Modifier.fillMaxWidth().border(1.5.dp, C.Line, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val ad = a.profile.address
                Txt(if (ad != null) "${ad.line1}\n${ad.city} ${ad.postcode}" else str(R.string.account_not_provided_yet), T.Input.copy(lineHeight = 22.sp), Modifier.weight(1f))
                LinkText(str(R.string.account_change), { postcode = ad?.postcode ?: ""; selected = 0; sheet = "address" }, color = C.Ink, strong = true)
            }
        }
        Note(str(R.string.account_locked_details_come_from_your_id), NoteKind.Info, Icons.Info,
            action = str(R.string.account_chat_with_us), onAction = { app.arro.ui.screens.support.openChat(c, nav, null) })
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
            ConfirmDialog(freezeConfirm, if (a.frozen) str(R.string.account_unfreeze_your_account) else str(R.string.account_freeze_your_account),
                if (a.frozen) str(R.string.account_you_ll_be_able_to_send) else str(R.string.account_sending_money_is_blocked_straight_away),
                if (a.frozen) str(R.string.account_unfreeze) else str(R.string.account_freeze_now), {
                    b.setFrozen(!a.frozen); freezeConfirm = false
                    banner.show(if (a.frozen) str(R.string.account_account_unfrozen) else str(R.string.account_account_frozen), kind = if (a.frozen) BannerKind.Success else BannerKind.Info)
                }, { freezeConfirm = false }, danger = !a.frozen)
            val dev = a.devices.firstOrNull { it.id == signOut }
            ConfirmDialog(dev != null, str(R.string.account_sign_out_x, dev?.name ?: ""), str(R.string.account_anyone_using_it_will_need_your), str(R.string.account_sign_out), {
                dev?.let { b.signOutDevice(it.id); banner.show(str(R.string.account_signed_out), it.name, BannerKind.Success) }; signOut = null
            }, { signOut = null }, danger = true)
            ChangePasscodeSheet(passcodeSheet) { passcodeSheet = false }
            ChangePasswordSheet(passwordSheet) { passwordSheet = false }
        },
    ) {
        ScreenTitle(str(R.string.account_security))
        if (a.frozen) {
            Panel(kind = PanelKind.Grey, padding = 14.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tile(Icons.Lock, TileStyle.Err, 52.dp, iconSize = 24.dp)
                    Column(Modifier.weight(1f)) {
                        Txt(str(R.string.account_your_account_is_frozen), T.H3)
                        Cap(str(R.string.account_sending_is_blocked_until_you_unfreeze))
                    }
                }
            }
        } else Panel(kind = if (on == protections.size) PanelKind.Lime else PanelKind.Grey, padding = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Illustration(Art.Shield, 52.dp)
                Column {
                    Txt(if (on == protections.size) str(R.string.account_your_account_is_well_protected) else str(R.string.account_x_of_x_protections_are_on, on, protections.size), T.H3)
                    Cap(if (on == protections.size) str(R.string.account_all_x_recommended_protections_are_on, protections.size) else str(R.string.account_turn_on_the_rest_below_to), color = C.Forest)
                }
            }
        }
        Group {
            ToggleRow(if (bioAvailable) str(R.string.account_fingerprint_or_face_unlock) else str(R.string.account_fingerprint_or_face_unlock),
                if (bioAvailable) str(R.string.account_unlock_and_approve_transfers) else str(R.string.account_set_up_a_fingerprint_or_face), a.biometricEnabled && bioAvailable, enabled = bioAvailable) { wantOn ->
                if (!wantOn) { b.setBiometric(false); return@ToggleRow }
                val activity = context as? FragmentActivity ?: return@ToggleRow
                Biometrics.prompt(activity, str(R.string.account_turn_on_x_unlock, BIO_NAME), str(R.string.account_confirm_it_s_you), negative = str(R.string.account_cancel),
                    onSuccess = { b.setBiometric(true); banner.show(str(R.string.account_biometric_unlock_is_on), kind = BannerKind.Success) },
                    onError = { banner.show(str(R.string.account_couldn_t_turn_it_on), it, BannerKind.Error) })
            }
            GroupDivider()
            ToggleRow(str(R.string.account_confirm_new_devices_by_sms), str(R.string.account_a_code_to_x, Text.maskPhoneShort(a.phone)), a.security.confirmNewDevices) { v -> b.updateSecurity { it.copy(confirmNewDevices = v) } }
            GroupDivider()
            ToggleRow(str(R.string.account_passcode_for_transfers_over_1_000), str(R.string.account_even_when_biometric_unlock_is_on), a.security.passcodeOverThreshold) { v -> b.updateSecurity { it.copy(passcodeOverThreshold = v) } }
            GroupDivider()
            ToggleRow(str(R.string.account_show_balance_on_home_screen_widgets), str(R.string.account_your_gbp_balance_and_recent_recipients), a.settings.widgetBalance) { v -> b.updateSettings { it.copy(widgetBalance = v) } }
            GroupDivider()
            ListRow(str(R.string.account_change_passcode), flat = true, minHeight = 52.dp,
                subtitle = a.security.passcodeChangedAt.takeIf { it > 0 }?.let { str(R.string.account_changed_x, Text.relativeDay(it, now)) }, trailing = { Chevron() }, onClick = { passcodeSheet = true })
            GroupDivider()
            ListRow(str(R.string.account_change_password), flat = true, minHeight = 52.dp,
                subtitle = a.security.passwordChangedAt.takeIf { it > 0 }?.let { str(R.string.account_changed_x, Text.relativeDay(it, now)) }, trailing = { Chevron() }, onClick = { passwordSheet = true })
        }
        Over(str(R.string.account_signed_in_devices))
        Group {
            a.devices.sortedByDescending { it.current }.forEachIndexed { i, d ->
                if (i > 0) GroupDivider()
                ListRow(d.name, flat = true, minHeight = 56.dp, subtitle = str(R.string.account_device_line, d.location, if (d.current) str(R.string.account_this_device) else Text.relativeDay(d.lastActive, now)),
                    leading = { Tile(if (d.laptop) Icons.Laptop else Icons.Smartphone, size = 36.dp, iconSize = 18.dp) },
                    trailing = { if (!d.current) ArroButton(str(R.string.account_sign_out), { signOut = d.id }, style = Btn.Line, size = BtnSize.S) })
            }
        }
        ArroButton(if (a.frozen) str(R.string.account_unfreeze_my_account) else str(R.string.account_freeze_my_account_now), { freezeConfirm = true },
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
                if (b.checkPasscode(code)) { current = code; step = 1; entry = ""; error = null } else { error = str(R.string.account_that_s_not_your_current_passcode); entry = "" }
            }
            1 -> if (Validation.passcodeWeak(code)) { error = str(R.string.account_avoid_repeated_or_simple_sequences_like); entry = "" } else { first = code; step = 2; entry = ""; error = null }
            else -> if (code != first) { error = str(R.string.account_those_didn_t_match_enter_your); entry = ""; step = 1 } else scope.launchCall(call, onError = { e -> error = e.message; entry = ""; step = 0; true }) {
                b.changePasscode(current, code)
                banner.show(str(R.string.account_passcode_changed), kind = BannerKind.Success)
                onDone()
            }
        }
    }
    Sheet(visible, onDone, fullHeight = true) {
        SheetHeader(when (step) { 0 -> str(R.string.account_enter_your_current_passcode); 1 -> str(R.string.account_choose_a_new_passcode); else -> str(R.string.account_confirm_your_new_passcode) }, onDone,
            str(R.string.account_step_x_of_3, step + 1))
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
        SheetHeader(str(R.string.account_change_password), onDone, str(R.string.account_you_ll_stay_logged_in_on))
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PasswordField(current, { current = it }, str(R.string.account_current_password))
            PasswordField(next, { next = it }, str(R.string.account_new_password))
            PasswordStrength(next)
            FieldHint(call.error.value)
            ArroButton(str(R.string.account_change_password), {
                scope.launchCall(call) { b.changePassword(current, next); banner.show(str(R.string.account_password_changed), kind = BannerKind.Success); onDone() }
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
                k.status != KycStatus.APPROVED -> ArroButton(if (k.status == KycStatus.IN_REVIEW) str(R.string.account_see_my_check) else str(R.string.account_verify_my_identity), { nav.go(if (k.status == KycStatus.IN_REVIEW) Routes.KYC_STATUS else Routes.KYC) })
                k.level == KycLevel.STANDARD && !k.enhancedRequested -> ArroButton(str(R.string.account_raise_my_limits), { docsSheet = true })
                k.enhancedRequested -> ArroButton(str(R.string.account_we_re_reviewing_your_documents), {}, enabled = false)
                else -> {}
            }
        },
        overlay = {
            Sheet(docsSheet, { docsSheet = false }, fullHeight = true) {
                SheetHeader(str(R.string.account_raise_your_limits), { docsSheet = false }, str(R.string.account_upload_two_documents_we_usually_review))
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListRow(str(R.string.account_proof_of_address), subtitle = addressDoc?.let { str(R.string.account_added_tap_to_replace) } ?: str(R.string.account_bank_statement_or_utility_bill_from),
                        leading = { Tile(if (addressDoc != null) Icons.Check else Icons.Document, if (addressDoc != null) TileStyle.Ok else TileStyle.Tint) },
                        trailing = { Chevron() }, onClick = { picking = "address"; picker() })
                    ListRow(str(R.string.account_proof_of_income), subtitle = incomeDoc?.let { str(R.string.account_added_tap_to_replace) } ?: str(R.string.account_a_recent_payslip_tax_return_or),
                        leading = { Tile(if (incomeDoc != null) Icons.Check else Icons.CashSimple, if (incomeDoc != null) TileStyle.Ok else TileStyle.Tint) },
                        trailing = { Chevron() }, onClick = { picking = "income"; picker() })
                    Note(str(R.string.account_photos_or_screenshots_are_fine_make), NoteKind.Info, Icons.Info)
                    FieldHint(call.error.value)
                    ArroButton(str(R.string.account_submit_documents), {
                        scope.launchCall(call) {
                            b.requestEnhanced(); docsSheet = false
                            banner.show(str(R.string.account_documents_sent), str(R.string.account_we_ll_let_you_know_when), BannerKind.Success)
                        }
                    }, loading = call.busy.value, enabled = addressDoc != null && incomeDoc != null)
                }
            }
        },
    ) {
        ScreenTitle(str(R.string.account_verification_and_limits_2))
        ArroCard(padding = 14.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Illustration(if (k.status == KycStatus.REJECTED) Art.IdCardWarn else Art.IdCard, 60.dp)
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(if (a.verified) k.level.label else str(R.string.account_not_verified), T.H3)
                        when (k.status) {
                            KycStatus.APPROVED -> Pill(str(R.string.account_verified), PillKind.Ok, dot = true)
                            KycStatus.IN_REVIEW -> Pill(str(R.string.account_checking), PillKind.Info, dot = true)
                            KycStatus.REJECTED -> Pill(str(R.string.account_action_needed), PillKind.Warn, dot = true)
                            KycStatus.NOT_STARTED -> Pill(str(R.string.account_to_do), PillKind.Neutral)
                        }
                    }
                    Cap(when (k.status) {
                        KycStatus.APPROVED -> str(R.string.account_x_checked_x, k.docType.label, Text.date(k.decidedAt.takeIf { it > 0 } ?: k.submittedAt.takeIf { it > 0 } ?: a.createdAt)) +
                            (if (k.docExpiry.isNotBlank()) str(R.string.account_expires_x, k.docExpiry) else "")
                        KycStatus.IN_REVIEW -> str(R.string.account_we_re_checking_your_x_usually, k.docType.label.lowercase())
                        KycStatus.REJECTED -> k.rejectReason.ifBlank { str(R.string.account_we_need_a_clearer_photo_of) }
                        KycStatus.NOT_STARTED -> str(R.string.account_verify_your_identity_to_start_sending)
                    }, Modifier.padding(top = 2.dp))
                }
            }
        }
        if (a.verified) {
            val level = k.level
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LimitLine(str(R.string.account_per_transfer), null, level.perTransferMinor)
                LimitLine(str(R.string.account_this_month), usage.monthMinor, level.perMonthMinor)
                LimitLine(str(R.string.account_this_year), usage.yearMinor, level.perYearMinor)
            }
            if (level == KycLevel.STANDARD) Panel(gap = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Txt(str(R.string.account_enhanced), T.H3, Modifier.weight(1f))
                    Pill(if (k.enhancedRequested) str(R.string.account_in_review) else str(R.string.account_next_level), if (k.enhancedRequested) PillKind.Info else PillKind.Neutral, dot = k.enhancedRequested)
                }
                Cap(str(R.string.account_send_up_to_x_per_transfer, Money.format(KycLevel.ENHANCED.perTransferMinor, Currency.GBP, false), Money.format(KycLevel.ENHANCED.perMonthMinor, Currency.GBP, false)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Ic(Icons.Document, size = 16.dp); Txt(str(R.string.account_proof_of_address_from_the_last), T.Body14) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Ic(Icons.CashSimple, size = 16.dp); Txt(str(R.string.account_proof_of_income_like_a_payslip), T.Body14) }
            } else Note(str(R.string.account_you_re_on_our_highest_level), NoteKind.Lime, Icons.ShieldCheck)
        } else {
            Note(str(R.string.account_until_you_re_verified_you_can), NoteKind.Info, Icons.Info)
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
                append(str(R.string.account_of_x, Money.format(limit, Currency.GBP, false)))
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
            PickerSheet(tp != null, if (tp == "from") str(R.string.account_quiet_from) else str(R.string.account_quiet_until), hours, if (tp == "from") s.quietFrom else s.quietTo, { h ->
                b.updateSettings { if (tp == "from") it.copy(quietFrom = h) else it.copy(quietTo = h) }
            }, { timePicker = null })
        },
    ) {
        ScreenTitle(str(R.string.account_notifications), str(R.string.account_choose_what_we_tell_you_about))
        if (!permitted) Note(str(R.string.account_notifications_for_arro_are_turned_off), NoteKind.Warn, Icons.Bell, action = str(R.string.account_turn_on), onAction = {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        })
        Over(str(R.string.account_transfers))
        Group {
            ToggleRow(str(R.string.account_transfer_updates), str(R.string.account_sent_delivered_or_delayed_push_and), s.transferUpdates) { v -> b.updateSettings { it.copy(transferUpdates = v) } }
            GroupDivider()
            ToggleRow(str(R.string.account_text_recipients_on_arrival), str(R.string.account_free_sms_to_the_person_you), s.smsRecipients) { v -> b.updateSettings { it.copy(smsRecipients = v) } }
            GroupDivider()
            ToggleRow(str(R.string.account_rate_alerts), str(R.string.account_when_a_rate_you_re_watching), s.rateAlerts) { v -> b.updateSettings { it.copy(rateAlerts = v) } }
        }
        Over(str(R.string.account_account))
        Group {
            ListRow(str(R.string.account_security_alerts), flat = true, minHeight = 58.dp, subtitle = str(R.string.account_always_on_to_keep_you_safe), trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Ic(Icons.Lock, size = 14.dp, tint = C.Slate); Cap(str(R.string.account_on)) }
            })
            GroupDivider()
            ToggleRow(str(R.string.account_monthly_statements), str(R.string.account_by_email_on_the_1st), s.statements) { v -> b.updateSettings { it.copy(statements = v) } }
            GroupDivider()
            ToggleRow(str(R.string.account_news_and_offers), str(R.string.account_new_countries_and_promotions), s.news) { v -> b.updateSettings { it.copy(news = v) } }
        }
        Over(str(R.string.account_quiet_hours))
        Group {
            ToggleRow(str(R.string.account_x_to_x, s.quietFrom, s.quietTo), str(R.string.account_security_alerts_still_come_through), s.quietHours) { v -> b.updateSettings { it.copy(quietHours = v) } }
            if (s.quietHours) {
                GroupDivider()
                ListRow(str(R.string.account_starts), flat = true, minHeight = 52.dp, trailing = { Row(verticalAlignment = Alignment.CenterVertically) { Cap(s.quietFrom); Chevron() } }, onClick = { timePicker = "from" })
                GroupDivider()
                ListRow(str(R.string.account_ends), flat = true, minHeight = 52.dp, trailing = { Row(verticalAlignment = Alignment.CenterVertically) { Cap(s.quietTo); Chevron() } }, onClick = { timePicker = "to" })
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
    val languages = listOf(str(R.string.account_english_uk), str(R.string.account_kinyarwanda), str(R.string.account_fran_ais), str(R.string.account_kiswahili), str(R.string.account_portugu_s))
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }, gap = 14.dp) {
        ScreenTitle(str(R.string.account_language_and_region_2))
        Over(str(R.string.account_app_language))
        Group {
            languages.forEachIndexed { i, lang ->
                if (i > 0) GroupDivider()
                val available = i == 0
                ListRow(lang, flat = true, minHeight = 52.dp, subtitle = if (available) null else str(R.string.account_coming_soon),
                    trailing = { Radio(s.language == lang) },
                    onClick = {
                        if (available) b.updateSettings { it.copy(language = lang) }
                        else banner.show(str(R.string.account_x_is_coming_soon, lang), str(R.string.account_we_ll_let_you_know_when_2, lang))
                    })
            }
        }
        Over(str(R.string.account_region_and_display))
        Group {
            ListRow(str(R.string.account_country_of_residence), flat = true, minHeight = 52.dp, trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Flag("gb", 18.dp); Cap(str(R.string.account_united_kingdom)) }
            })
            GroupDivider()
            ListRow(str(R.string.account_number_format), flat = true, minHeight = 52.dp, trailing = { Cap("1,234.56") })
            GroupDivider()
            ListRow(str(R.string.account_date_format), flat = true, minHeight = 52.dp, trailing = { Cap(Text.numericDate(b.now)) })
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
            ArroButton(str(R.string.account_share_invite_link), {
                Share.text(context, str(R.string.account_i_use_arro_to_send_money, link, code), str(R.string.account_try_arro))
            }, style = Btn.Lime, icon = Icons.Share)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                LinkText(str(R.string.account_terms_apply), { nav.go(Routes.legal("referral")) }, color = C.DarkCap, underline = true)
                Cap(str(R.string.account_rewards_are_paid_into_your_gbp), color = C.DarkCap, size = 12f)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(320.dp).offset(y = 30.dp).background(Brush.radialGradient(listOf(C.Lime.copy(alpha = 0.18f), C.Lime.copy(alpha = 0f))), CircleShape))
            Illustration(Art.Gift, 200.dp, description = str(R.string.account_a_gift_box))
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(str(R.string.account_invite_friends_get_10), size = TitleSize.XL, color = C.Lime, align = TextAlign.Center)
            Txt(str(R.string.account_friends_first_transfer_has_no_arro), T.Lede, color = C.DarkCap, align = TextAlign.Center)
        }
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth().dashedBorder().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Cap(str(R.string.account_your_invite_code), color = C.DarkCap, size = 12f)
                Txt(code, T.Big22.copy(letterSpacing = 1.3.sp), Modifier.padding(top = 2.dp), color = C.OnNight)
            }
            ArroButton(str(R.string.account_copy), { Share.copy(context, str(R.string.account_arro_invite_code), code); banner.show(str(R.string.account_invite_code_copied), kind = BannerKind.Success) },
                style = Btn.LineOnDark, size = BtnSize.S, icon = Icons.Copy)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Cap(if (a.referral.friendsJoined == 1) str(R.string.account_one_friend_joined) else str(R.string.account_x_friends_joined, a.referral.friendsJoined), color = C.DarkCap)
            Txt(str(R.string.account_x_earned, Money.format(a.referral.earnedMinor, Currency.GBP)), T.Body14.copy(fontWeight = FontWeight.SemiBold), color = C.Lime)
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
                SheetHeader(str(R.string.account_privacy_choices), { privacy = false }, str(R.string.account_change_these_whenever_you_like_they))
                Group(Modifier.padding(top = 14.dp)) {
                    ToggleRow(str(R.string.account_help_improve_arro), str(R.string.account_anonymous_usage_analytics), a.settings.analytics) { v -> b.updateSettings { it.copy(analytics = v) } }
                    GroupDivider()
                    ToggleRow(str(R.string.account_marketing), str(R.string.account_offers_by_email_and_push_never), a.settings.marketing) { v -> b.updateSettings { it.copy(marketing = v) } }
                }
            }
            ConfirmDialog(pause, str(R.string.account_pause_your_account), str(R.string.account_you_ll_be_logged_out_and), str(R.string.account_pause_account), {
                pause = false; b.pauseAccount()
            }, { pause = false })
            Sheet(close, { close = false; understood = false }) {
                SheetHeader(str(R.string.account_close_your_arro_account), { close = false; understood = false }, str(R.string.account_this_can_t_be_undone))
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val balances = a.wallets.filter { it.balanceMinor != 0L }
                    if (balances.isNotEmpty()) Note(str(R.string.account_you_still_have_x_send_or, balances.joinToString { Money.format(it.balanceMinor, it.cur) }), NoteKind.Warn, Icons.Warning,
                        action = str(R.string.account_go_to_wallet), onAction = { close = false; nav.go(Routes.WALLET) })
                    Cap(str(R.string.account_we_ll_delete_your_recipients_cards))
                    CheckRow(understood, { understood = it }, str(R.string.account_i_understand_my_account_will_be))
                    FieldHint(call.error.value)
                    ArroButton(str(R.string.account_close_my_account), { scope.launchCall(call) { b.closeAccount() } }, style = Btn.Danger, loading = call.busy.value, enabled = understood)
                }
            }
        },
    ) {
        ScreenTitle(str(R.string.account_account_management_2))
        Over(str(R.string.account_your_data))
        Group {
            ListRow(str(R.string.account_download_my_data), flat = true, subtitle = str(R.string.account_everything_we_hold_about_you_as), leading = { Tile(Icons.Download, size = 36.dp, iconSize = 18.dp) }, trailing = { Chevron() },
                onClick = {
                    scope.launchCall(call) {
                        val f = withContext(Dispatchers.IO) { Share.cacheFile(context, "arro-my-data.json").apply { writeText(b.exportData()) } }
                        banner.show(str(R.string.account_your_data_is_ready), Share.saveToDownloads(context, f, "application/json"), BannerKind.Success)
                    }
                })
            GroupDivider()
            ListRow(str(R.string.account_privacy_choices), flat = true, subtitle = str(R.string.account_analytics_and_marketing_consent), leading = { Tile(Icons.Eye, size = 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { privacy = true })
        }
        Over(str(R.string.account_your_account))
        Group {
            ListRow(str(R.string.account_close_a_currency_account), flat = true, subtitle = a.wallets.filter { !it.main }.joinToString(" or ") { it.currency }.ifBlank { str(R.string.account_you_only_have_your_main_gbp) },
                leading = { Tile(Icons.WalletSimple, size = 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.WALLET) })
            GroupDivider()
            ListRow(str(R.string.account_pause_my_account), flat = true, subtitle = str(R.string.account_block_sending_and_logins_until_you), leading = { Tile(Icons.Pause, TileStyle.Warn, 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { pause = true })
            GroupDivider()
            ListRow(str(R.string.account_close_my_account), flat = true, titleStyle = T.RowTitle.copy(color = C.Err), subtitle = str(R.string.account_permanent_you_ll_need_a_zero),
                leading = { Tile(Icons.Trash, TileStyle.Err, 36.dp, iconSize = 18.dp) }, trailing = { Chevron() }, onClick = { call.error.value = null; close = true })
        }
        Note(str(R.string.account_even_after_closing_the_law_requires), NoteKind.Info, Icons.Info)
        Over(str(R.string.account_legal))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(str(R.string.account_terms_of_use), false, { nav.go(Routes.legal("terms")) })
            Chip(str(R.string.account_privacy_policy), false, { nav.go(Routes.legal("privacy")) })
            Chip(str(R.string.account_complaints), false, { nav.go(Routes.legal("complaints")) })
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
    val scope = rememberCoroutineScope()
    val offerCall = rememberCall()
    val now = nowTicker()
    val offerCur = b.relevantCurrency(a)
    val runningLock = a?.anyOfferLock(now)
    val waitingFriend = a?.referral?.friends?.firstOrNull { it.status == app.arro.core.FriendStatus.SIGNED_UP }
    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.account_demo_controls)) },
        gap = 14.dp,
        overlay = {
            PickerSheet(outagePicker, str(R.string.account_make_a_provider_unavailable), listOf(str(R.string.account_none)) + providers.map { it.name }, providers.firstOrNull { it.id == demo.outageProviderId }?.name ?: str(R.string.account_none), { name ->
                val id = providers.firstOrNull { it.name == name }?.id ?: ""
                b.setDemo { it.copy(outageProviderId = id) }
                banner.show(if (id.isEmpty()) str(R.string.account_all_providers_available) else str(R.string.account_x_is_now_offline, name), if (id.isEmpty()) "" else str(R.string.account_sending_with_it_shows_the_unavailable))
            }, { outagePicker = false }, subtitles = providers.associate { it.name to Catalog.country(it.country).name })
            ConfirmDialog(erase, str(R.string.account_erase_everything), str(R.string.account_this_deletes_the_account_transfers_and), str(R.string.account_erase), {
                erase = false; b.resetEverything(); nav.resetTo(Routes.WELCOME)
            }, { erase = false }, danger = true)
        },
    ) {
        ScreenTitle(str(R.string.account_try_every_path), str(R.string.account_these_controls_exist_only_in_this))
        Group {
            ToggleRow(str(R.string.account_fast_mode), str(R.string.account_transfers_checks_and_refunds_finish_in), demo.fast) { v -> b.setDemo { it.copy(fast = v) } }
            GroupDivider()
            ListRow(str(R.string.account_provider_outage), flat = true, minHeight = 58.dp, subtitle = providers.firstOrNull { it.id == demo.outageProviderId }?.name ?: str(R.string.account_none), trailing = { Chevron() }, onClick = { outagePicker = true })
            GroupDivider()
            ToggleRow(str(R.string.account_ask_for_a_retake_on_the), str(R.string.account_tests_the_rejected_photo_path), demo.rejectNextKyc) { v -> b.setDemo { it.copy(rejectNextKyc = v) } }
        }
        Over(str(R.string.account_lock_rate_and_rewards))
        Group {
            ToggleRow(str(R.string.account_random_rate_offers), str(R.string.account_offers_appear_25_to_90_seconds), demo.rateOffers) { v -> b.setDemo { it.copy(rateOffers = v) } }
            GroupDivider()
            ListRow(str(R.string.account_show_a_rate_offer_now), flat = true, minHeight = 56.dp, subtitle = if (offerCall.busy.value) str(R.string.account_getting_live_rates) else str(R.string.account_gbp_x_open_for_3_minutes, offerCur.code),
                leading = { Tile(Icons.Bolt, TileStyle.Lime, 36.dp, iconSize = 18.dp) }, enabled = a?.verified == true && !offerCall.busy.value,
                onClick = {
                    scope.launchCall(offerCall, onError = { e -> banner.show(str(R.string.account_no_offer), e.message ?: "", BannerKind.Error); true }) {
                        b.demoOfferNow(); nav.go(Routes.lockRate(offerCur.code))
                    }
                })
            GroupDivider()
            ListRow(str(R.string.account_end_my_locked_rate_now), flat = true, minHeight = 56.dp,
                subtitle = runningLock?.let { str(R.string.account_x_locked_until_x, it.to, Text.time(it.expiresAt)) } ?: str(R.string.account_no_locked_rate_running),
                leading = { Tile(Icons.Clock, size = 36.dp, iconSize = 18.dp) }, enabled = runningLock != null,
                onClick = { b.demoEndLocks() })
            GroupDivider()
            listOf(1.0, -1.0).forEachIndexed { i, pct ->
                if (i > 0) GroupDivider()
                ListRow(str(R.string.account_move_the_market_x_1, if (pct > 0) str(R.string.account_up) else str(R.string.account_down)), flat = true, minHeight = 56.dp,
                    subtitle = str(R.string.account_gbp_x_mid_market_rate_until, offerCur.code),
                    leading = { Tile(Icons.Trending, size = 36.dp, iconSize = 18.dp) },
                    onClick = {
                        b.demoMoveMarket(offerCur, pct)
                        banner.show(str(R.string.account_market_moved_x_1, if (pct > 0) str(R.string.account_up) else str(R.string.account_down)), str(R.string.account_live_rate_now_x_locked_rates, Money.rateLine(Currency.GBP, offerCur, b.customerRate(offerCur))))
                    })
            }
            GroupDivider()
            ListRow(str(R.string.account_a_friend_joins_with_my_code), flat = true, minHeight = 56.dp, subtitle = str(R.string.account_adds_a_sample_friend_to_referrals),
                leading = { Tile(Icons.Users, size = 36.dp, iconSize = 18.dp) }, enabled = a != null,
                onClick = { val f = b.demoFriendJoins(); banner.show(str(R.string.account_x_joined, f.name), str(R.string.account_sample_data_their_first_100_transfer), BannerKind.Success) })
            GroupDivider()
            ListRow(str(R.string.account_their_first_120_transfer_is_delivered), flat = true, minHeight = 56.dp,
                subtitle = waitingFriend?.let { str(R.string.account_x_pays_10_into_your_gbp, it.name) } ?: str(R.string.account_no_friend_waiting),
                leading = { Tile(Icons.Gift, size = 36.dp, iconSize = 18.dp) }, enabled = waitingFriend != null,
                onClick = {
                    val paid = waitingFriend?.let { b.referralTransferDelivered(it.id, 12_000) } ?: 0
                    if (paid == 0L) banner.show(str(R.string.account_no_reward_paid), str(R.string.account_verify_your_identity_first_or_the, app.arro.core.Loyalty.REFERRAL_YEARLY_CAP), BannerKind.Info)
                })
        }
        Group {
            ListRow(str(R.string.account_finish_everything_that_s_waiting), flat = true, minHeight = 56.dp, subtitle = str(R.string.account_deliver_transfers_refunds_and_id_checks),
                leading = { Tile(Icons.Bolt, TileStyle.Lime, 36.dp, iconSize = 18.dp) }, enabled = a != null,
                onClick = { b.fastForward(); banner.show(str(R.string.account_done), str(R.string.account_everything_waiting_has_moved_on), BannerKind.Success) })
            GroupDivider()
            ListRow(str(R.string.account_lift_the_24_hour_sending_pause), flat = true, minHeight = 56.dp, subtitle = if ((a?.sendPauseUntil ?: 0) > b.now) str(R.string.account_paused_until_x, Text.time(a!!.sendPauseUntil)) else str(R.string.account_not_paused),
                leading = { Tile(Icons.Clock, size = 36.dp, iconSize = 18.dp) }, enabled = a != null,
                onClick = { b.clearSendPause(); banner.show(str(R.string.account_sending_pause_lifted), kind = BannerKind.Success) })
            GroupDivider()
            ListRow(str(R.string.account_load_the_sample_account), flat = true, minHeight = 56.dp, subtitle = str(R.string.account_eric_nshuti_with_a_year_of),
                leading = { Tile(Icons.User, size = 36.dp, iconSize = 18.dp) },
                onClick = { b.loadDemoAccount(); banner.show(str(R.string.account_sample_account_loaded), str(R.string.account_passcode_x, Seed.DEMO_PASSCODE), BannerKind.Success); nav.resetTo(Routes.HOME) })
            GroupDivider()
            ListRow(str(R.string.account_erase_everything_on_this_phone), flat = true, minHeight = 56.dp, titleStyle = T.RowTitle.copy(color = C.Err),
                leading = { Tile(Icons.Trash, TileStyle.Err, 36.dp, iconSize = 18.dp) }, onClick = { erase = true })
        }
        Panel(gap = 8.dp) {
            Txt(str(R.string.account_test_triggers), T.H3)
            listOf(
                str(R.string.account_sample_login) to "${Seed.DEMO_EMAIL} · ${Seed.DEMO_PASSWORD}",
                str(R.string.account_passcode) to Seed.DEMO_PASSCODE,
                str(R.string.account_delivery_fails_then_refunds) to str(R.string.account_payout_number_or_account_ending_000),
                str(R.string.account_name_check_not_found) to str(R.string.account_number_ending_404),
                str(R.string.account_name_check_mismatch) to str(R.string.account_number_ending_999),
                str(R.string.account_security_hold) to str(R.string.account_any_transfer_over_1_000),
                str(R.string.account_card_declined_by_the_bank) to str(R.string.account_card_number_ending_0002),
                str(R.string.account_credit_card_1_75_fee) to str(R.string.account_mastercard_starting_51_to_53),
                str(R.string.account_rate_offers) to str(R.string.account_signed_in_and_verified_with_the),
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
        "privacy" -> str(R.string.account_privacy_policy) to listOf(
            str(R.string.account_what_we_collect) to str(R.string.account_your_name_contact_details_identity_documents),
            str(R.string.account_how_we_use_it) to str(R.string.account_to_move_your_money_keep_your),
            str(R.string.account_who_we_share_it_with) to str(R.string.account_payment_partners_payout_providers_in_the),
            str(R.string.account_how_long_we_keep_it) to str(R.string.account_transfer_records_for_5_years_after),
            str(R.string.account_your_rights) to str(R.string.account_you_can_download_your_data_correct),
        )
        "complaints" -> str(R.string.account_complaints) to listOf(
            str(R.string.account_tell_us) to str(R.string.account_chat_with_us_in_the_app),
            str(R.string.account_our_response) to str(R.string.account_we_aim_to_resolve_complaints_within),
            str(R.string.account_if_you_re_not_happy) to str(R.string.account_you_can_refer_your_complaint_to),
        )
        "referral" -> str(R.string.account_invite_rewards) to listOf(
            str(R.string.account_who_can_take_part) to str(R.string.account_verified_arro_customers_in_the_uk),
            str(R.string.account_the_reward) to str(R.string.account_your_friend_s_first_transfer_has),
            str(R.string.account_limits) to str(R.string.account_up_to_20_rewards_a_year),
        )
        else -> str(R.string.account_terms_of_use) to listOf(
            str(R.string.account_about_us) to str(R.string.account_arro_ltd_is_authorised_by_regulator),
            str(R.string.account_your_account) to str(R.string.account_you_must_be_18_or_over),
            str(R.string.account_sending_money) to str(R.string.account_we_show_the_fee_exchange_rate),
            str(R.string.account_cancellations_and_refunds) to str(R.string.account_you_can_cancel_a_transfer_that),
            str(R.string.account_safeguarding) to str(R.string.account_money_you_hold_with_arro_is),
        )
    }
    ArroScreen(topBar = { TopBar(onBack = { nav.back() }) }, gap = 14.dp) {
        ScreenTitle(title, str(R.string.account_last_updated_1_oct_2026_draft))
        sections.forEach { (h, body) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(h, T.H3)
                Txt(body, T.Body.copy(lineHeight = 23.sp), color = C.Ink)
            }
        }
        Note(str(R.string.account_this_is_placeholder_legal_text_for), NoteKind.Warn, Icons.Info)
    }
}
