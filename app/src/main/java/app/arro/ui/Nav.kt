package app.arro.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.arro.ui.screens.account.AccountScreen
import app.arro.ui.screens.account.DemoScreen
import app.arro.ui.screens.account.LanguageScreen
import app.arro.ui.screens.account.LegalScreen
import app.arro.ui.screens.account.ManageScreen
import app.arro.ui.screens.account.NotificationSettingsScreen
import app.arro.ui.screens.account.PersonalScreen
import app.arro.ui.screens.account.ReferScreen
import app.arro.ui.screens.account.SecurityScreen
import app.arro.ui.screens.account.VerificationScreen
import app.arro.ui.screens.auth.AccountSetupScreen
import app.arro.ui.screens.auth.BiometricSetupScreen
import app.arro.ui.screens.auth.ForgotScreen
import app.arro.ui.screens.auth.LoginScreen
import app.arro.ui.screens.auth.OtpScreen
import app.arro.ui.screens.auth.PasscodeSetupScreen
import app.arro.ui.screens.auth.PhoneScreen
import app.arro.ui.screens.auth.ResetDoneScreen
import app.arro.ui.screens.auth.ResetScreen
import app.arro.ui.screens.auth.SplashScreen
import app.arro.ui.screens.auth.WelcomeScreen
import app.arro.ui.screens.home.HomeScreen
import app.arro.ui.screens.home.InsightsScreen
import app.arro.ui.screens.home.NotificationsScreen
import app.arro.ui.screens.kyc.KycAddressScreen
import app.arro.ui.screens.kyc.KycCaptureScreen
import app.arro.ui.screens.kyc.KycCheckScreen
import app.arro.ui.screens.kyc.KycDocumentScreen
import app.arro.ui.screens.kyc.KycIntroScreen
import app.arro.ui.screens.kyc.KycPersonalScreen
import app.arro.ui.screens.kyc.KycSelfieScreen
import app.arro.ui.screens.kyc.KycStatusScreen
import app.arro.ui.screens.payments.AddCardScreen
import app.arro.ui.screens.payments.BankConnectScreen
import app.arro.ui.screens.payments.BankListScreen
import app.arro.ui.screens.payments.CardApprovalScreen
import app.arro.ui.screens.payments.MethodAddedScreen
import app.arro.ui.screens.payments.PaymentMethodsScreen
import app.arro.ui.screens.payments.ScanCardScreen
import app.arro.ui.screens.rates.RateAlertScreen
import app.arro.ui.screens.rates.RateCompareScreen
import app.arro.ui.screens.rates.RateDetailScreen
import app.arro.ui.screens.rates.RatesScreen
import app.arro.ui.screens.recipients.AddRecipientScreen
import app.arro.ui.screens.recipients.BankPayoutScreen
import app.arro.ui.screens.recipients.EditRecipientScreen
import app.arro.ui.screens.recipients.NameCheckScreen
import app.arro.ui.screens.recipients.RecipientScreen
import app.arro.ui.screens.recipients.RecipientsScreen
import app.arro.ui.screens.send.ArroUserScreen
import app.arro.ui.screens.send.ScanQrScreen
import app.arro.ui.screens.send.SendAmountScreen
import app.arro.ui.screens.send.SendCountryScreen
import app.arro.ui.screens.send.SendDeliveryScreen
import app.arro.ui.screens.send.SendFundingScreen
import app.arro.ui.screens.send.SendHubScreen
import app.arro.ui.screens.send.SendProcessingScreen
import app.arro.ui.screens.send.SendRecipientScreen
import app.arro.ui.screens.send.SendReviewScreen
import app.arro.ui.screens.send.SendSuccessScreen
import app.arro.ui.screens.send.SendUnavailableScreen
import app.arro.ui.screens.support.ArticleScreen
import app.arro.ui.screens.support.CaseScreen
import app.arro.ui.screens.support.ChatScreen
import app.arro.ui.screens.support.DisputeScreen
import app.arro.ui.screens.support.HelpScreen
import app.arro.ui.screens.support.TransferHelpScreen
import app.arro.ui.screens.track.TrackScreen
import app.arro.ui.screens.wallet.ActivityScreen
import app.arro.ui.screens.wallet.AddMoneyScreen
import app.arro.ui.screens.wallet.ConvertScreen
import app.arro.ui.screens.wallet.ReceiptScreen
import app.arro.ui.screens.wallet.StatementsScreen
import app.arro.ui.screens.wallet.TransferDetailScreen
import app.arro.ui.screens.wallet.WalletAccountScreen
import app.arro.ui.screens.wallet.WalletScreen

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val PHONE = "phone"
    const val OTP = "otp"
    const val ACCOUNT_SETUP = "account_setup"
    const val PASSCODE = "passcode"
    const val BIOMETRIC = "biometric"
    const val LOGIN = "login"
    const val FORGOT = "forgot"
    const val RESET = "reset"
    const val RESET_DONE = "reset_done"

    const val KYC = "kyc"
    const val KYC_PERSONAL = "kyc_personal"
    const val KYC_ADDRESS = "kyc_address"
    const val KYC_DOCUMENT = "kyc_document"
    const val KYC_CAPTURE = "kyc_capture"
    const val KYC_CHECK = "kyc_check"
    const val KYC_SELFIE = "kyc_selfie"
    const val KYC_STATUS = "kyc_status"

    const val HOME = "home"
    const val NOTIFICATIONS = "notifications"
    const val INSIGHTS = "insights"

    const val SEND = "send"
    const val SEND_COUNTRY = "send_country"
    const val SEND_AMOUNT = "send_amount"
    const val SEND_DELIVERY = "send_delivery"
    const val SEND_RECIPIENT = "send_recipient"
    const val SEND_FUNDING = "send_funding"
    const val SEND_REVIEW = "send_review"
    const val SEND_PROCESSING = "send_processing"
    const val SEND_SUCCESS = "send_success"
    const val SEND_UNAVAILABLE = "send_unavailable"
    const val SEND_ARRO = "send_arro"
    const val SCAN_QR = "scan_qr"

    const val TRACK = "track/{id}"
    fun track(id: String) = "track/$id"

    const val RECIPIENTS = "recipients"
    const val RECIPIENT = "recipient/{id}"
    fun recipient(id: String) = "recipient/$id"
    const val RCP_ADD = "rcp_add"
    const val RCP_CHECK = "rcp_check"
    const val RCP_BANK = "rcp_bank/{id}"
    fun rcpBank(id: String) = "rcp_bank/$id"
    const val RCP_EDIT = "rcp_edit/{id}"
    fun rcpEdit(id: String) = "rcp_edit/$id"

    const val PAY_METHODS = "pay_methods"
    const val PAY_ADD_CARD = "pay_add_card"
    const val PAY_SCAN = "pay_scan"
    const val PAY_BANKS = "pay_banks"
    const val PAY_CONNECT = "pay_connect/{bank}"
    fun payConnect(bank: String) = "pay_connect/$bank"
    const val PAY_3DS = "pay_3ds"
    const val PAY_ADDED = "pay_added/{id}"
    fun payAdded(id: String) = "pay_added/$id"

    const val WALLET = "wallet"
    const val WALLET_ACCOUNT = "wallet_account/{cur}"
    fun walletAccount(cur: String) = "wallet_account/$cur"
    const val ADD_MONEY = "add_money/{cur}"
    fun addMoney(cur: String = "GBP") = "add_money/$cur"
    const val CONVERT = "convert/{from}"
    fun convert(from: String = "GBP") = "convert/$from"
    const val ACTIVITY = "activity"
    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/$id"
    const val RECEIPT = "receipt/{id}"
    fun receipt(id: String) = "receipt/$id"
    const val STATEMENTS = "statements"

    const val RATES = "rates"
    const val RATE = "rate/{cur}"
    fun rate(cur: String) = "rate/$cur"
    const val RATE_ALERT = "rate_alert/{cur}"
    fun rateAlert(cur: String) = "rate_alert/$cur"
    const val RATE_COMPARE = "rate_compare/{cur}"
    fun rateCompare(cur: String = "RWF") = "rate_compare/$cur"

    const val HELP = "help"
    const val ARTICLE = "article/{id}"
    fun article(id: String) = "article/$id"
    const val TRANSFER_HELP = "transfer_help/{id}"
    fun transferHelp(id: String) = "transfer_help/$id"
    const val CHAT = "chat"
    const val DISPUTE = "dispute/{id}"
    fun dispute(id: String) = "dispute/$id"
    const val CASE = "case/{id}"
    fun case(id: String) = "case/$id"

    const val ACCOUNT = "account"
    const val PERSONAL = "personal"
    const val SECURITY = "security"
    const val VERIFICATION = "verification"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val LANGUAGE = "language"
    const val REFER = "refer"
    const val MANAGE = "manage"
    const val DEMO = "demo"
    const val LEGAL = "legal/{doc}"
    fun legal(doc: String) = "legal/$doc"

    /** Screens with the floating tab bar. */
    val tabRoots = setOf(HOME, RECIPIENTS, ACTIVITY, ACCOUNT)
}

private fun NavBackStackEntry.arg(name: String): String = arguments?.getString(name) ?: ""

private fun NavGraphBuilder.screen(route: String, content: @Composable (NavBackStackEntry) -> Unit) =
    composable(route) { entry -> content(entry) }

@Composable
fun ArroNavHost(nav: NavHostController) {
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        screen(Routes.SPLASH) { SplashScreen() }
        screen(Routes.WELCOME) { WelcomeScreen() }
        screen(Routes.PHONE) { PhoneScreen() }
        screen(Routes.OTP) { OtpScreen() }
        screen(Routes.ACCOUNT_SETUP) { AccountSetupScreen() }
        screen(Routes.PASSCODE) { PasscodeSetupScreen() }
        screen(Routes.BIOMETRIC) { BiometricSetupScreen() }
        screen(Routes.LOGIN) { LoginScreen() }
        screen(Routes.FORGOT) { ForgotScreen() }
        screen(Routes.RESET) { ResetScreen() }
        screen(Routes.RESET_DONE) { ResetDoneScreen() }

        screen(Routes.KYC) { KycIntroScreen() }
        screen(Routes.KYC_PERSONAL) { KycPersonalScreen() }
        screen(Routes.KYC_ADDRESS) { KycAddressScreen() }
        screen(Routes.KYC_DOCUMENT) { KycDocumentScreen() }
        screen(Routes.KYC_CAPTURE) { KycCaptureScreen() }
        screen(Routes.KYC_CHECK) { KycCheckScreen() }
        screen(Routes.KYC_SELFIE) { KycSelfieScreen() }
        screen(Routes.KYC_STATUS) { KycStatusScreen() }

        screen(Routes.HOME) { HomeScreen() }
        screen(Routes.NOTIFICATIONS) { NotificationsScreen() }
        screen(Routes.INSIGHTS) { InsightsScreen() }

        screen(Routes.SEND) { SendHubScreen() }
        screen(Routes.SEND_COUNTRY) { SendCountryScreen() }
        screen(Routes.SEND_AMOUNT) { SendAmountScreen() }
        screen(Routes.SEND_DELIVERY) { SendDeliveryScreen() }
        screen(Routes.SEND_RECIPIENT) { SendRecipientScreen() }
        screen(Routes.SEND_FUNDING) { SendFundingScreen() }
        screen(Routes.SEND_REVIEW) { SendReviewScreen() }
        screen(Routes.SEND_PROCESSING) { SendProcessingScreen() }
        screen(Routes.SEND_SUCCESS) { SendSuccessScreen() }
        screen(Routes.SEND_UNAVAILABLE) { SendUnavailableScreen() }
        screen(Routes.SEND_ARRO) { ArroUserScreen() }
        screen(Routes.SCAN_QR) { ScanQrScreen() }

        screen(Routes.TRACK) { TrackScreen(it.arg("id")) }

        screen(Routes.RECIPIENTS) { RecipientsScreen() }
        screen(Routes.RECIPIENT) { RecipientScreen(it.arg("id")) }
        screen(Routes.RCP_ADD) { AddRecipientScreen() }
        screen(Routes.RCP_CHECK) { NameCheckScreen() }
        screen(Routes.RCP_BANK) { BankPayoutScreen(it.arg("id")) }
        screen(Routes.RCP_EDIT) { EditRecipientScreen(it.arg("id")) }

        screen(Routes.PAY_METHODS) { PaymentMethodsScreen() }
        screen(Routes.PAY_ADD_CARD) { AddCardScreen() }
        screen(Routes.PAY_SCAN) { ScanCardScreen() }
        screen(Routes.PAY_BANKS) { BankListScreen() }
        screen(Routes.PAY_CONNECT) { BankConnectScreen(it.arg("bank")) }
        screen(Routes.PAY_3DS) { CardApprovalScreen() }
        screen(Routes.PAY_ADDED) { MethodAddedScreen(it.arg("id")) }

        screen(Routes.WALLET) { WalletScreen() }
        screen(Routes.WALLET_ACCOUNT) { WalletAccountScreen(it.arg("cur")) }
        screen(Routes.ADD_MONEY) { AddMoneyScreen(it.arg("cur")) }
        screen(Routes.CONVERT) { ConvertScreen(it.arg("from")) }
        screen(Routes.ACTIVITY) { ActivityScreen() }
        screen(Routes.DETAIL) { TransferDetailScreen(it.arg("id")) }
        screen(Routes.RECEIPT) { ReceiptScreen(it.arg("id")) }
        screen(Routes.STATEMENTS) { StatementsScreen() }

        screen(Routes.RATES) { RatesScreen() }
        screen(Routes.RATE) { RateDetailScreen(it.arg("cur")) }
        screen(Routes.RATE_ALERT) { RateAlertScreen(it.arg("cur")) }
        screen(Routes.RATE_COMPARE) { RateCompareScreen(it.arg("cur")) }

        screen(Routes.HELP) { HelpScreen() }
        screen(Routes.ARTICLE) { ArticleScreen(it.arg("id")) }
        screen(Routes.TRANSFER_HELP) { TransferHelpScreen(it.arg("id")) }
        screen(Routes.CHAT) { ChatScreen() }
        screen(Routes.DISPUTE) { DisputeScreen(it.arg("id")) }
        screen(Routes.CASE) { CaseScreen(it.arg("id")) }

        screen(Routes.ACCOUNT) { AccountScreen() }
        screen(Routes.PERSONAL) { PersonalScreen() }
        screen(Routes.SECURITY) { SecurityScreen() }
        screen(Routes.VERIFICATION) { VerificationScreen() }
        screen(Routes.NOTIFICATION_SETTINGS) { NotificationSettingsScreen() }
        screen(Routes.LANGUAGE) { LanguageScreen() }
        screen(Routes.REFER) { ReferScreen() }
        screen(Routes.MANAGE) { ManageScreen() }
        screen(Routes.DEMO) { DemoScreen() }
        screen(Routes.LEGAL) { LegalScreen(it.arg("doc")) }
    }
}
