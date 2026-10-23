package app.arro.ui

import app.arro.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import app.arro.ui.components.refreshSystemBars
import app.arro.ui.theme.C
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.arro.MainActivity
import app.arro.core.BackendEvent
import app.arro.platform.Share
import app.arro.ui.screens.auth.UnlockScreen
import app.arro.ui.screens.send.startSend
import app.arro.ui.screens.system.OfflineBar
import kotlinx.coroutines.delay

/** Routes reachable without being signed in. */
private val publicRoutes = setOf(
    Routes.SPLASH, Routes.WELCOME, Routes.PHONE, Routes.OTP, Routes.ACCOUNT_SETUP, Routes.PASSCODE, Routes.LOGIN,
    Routes.FORGOT, Routes.RESET, Routes.RESET_DONE,
)

@Composable
fun ArroRoot() {
    val c = container()
    val context = LocalContext.current
    val state by c.backend.state.collectAsStateWithLifecycle()
    val unlocked by c.backend.unlocked.collectAsStateWithLifecycle()
    val online by c.connectivity.online.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val banner = remember { BannerState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.SPLASH
    var wasSignedIn by remember { mutableStateOf(state.signedIn) }

    // Keep the status and navigation bar icons readable when the theme changes.
    val view = LocalView.current
    val dark = C.dark
    LaunchedEffect(dark) { refreshSystemBars(view) }

    CompositionLocalProvider(LocalNav provides nav, LocalBanner provides banner) {
        // The background shows through while screens cross-fade, so it follows the theme too.
        Box(Modifier.fillMaxSize().background(C.White)) {
            ArroNavHost(nav)

            val locked = state.signedIn && !unlocked && route !in publicRoutes
            if (locked) UnlockScreen(expired = c.session.sessionExpired)

            if (!online && route !in setOf(Routes.SPLASH)) OfflineBar(c.connectivity.lastOnlineAt)
            BannerHost(banner)
        }
    }

    // Leave the app area when the user signs out (log out, password reset, pause).
    LaunchedEffect(state.signedIn) {
        if (wasSignedIn && !state.signedIn && route !in publicRoutes) {
            nav.resetTo(if (state.account == null) Routes.WELCOME else Routes.LOGIN)
        }
        wasSignedIn = state.signedIn
    }

    // Open a screen requested by a notification or a widget once the app is unlocked.
    LaunchedEffect(unlocked, c.session.pendingRoute, route) {
        val pending = c.session.pendingRoute
        if (pending != null && unlocked && state.signedIn && route !in publicRoutes) {
            c.session.pendingRoute = null
            when {
                // Quick Send from the widget goes through the normal send checks, review and approval.
                pending == Routes.QUICK_SEND -> startSend(c, nav, banner, hub = true)
                pending.startsWith(Routes.QUICK_SEND_TO + ":") -> startSend(c, nav, banner, country = pending.substringAfter(':'))
                pending.startsWith(Routes.QUICK_SEND + ":") -> {
                    val r = state.account?.recipient(pending.substringAfter(':'))
                    if (r != null) startSend(c, nav, banner, recipient = r) else startSend(c, nav, banner, hub = true)
                }
                else -> nav.go(pending)
            }
        }
    }

    // Lock after five minutes without a touch.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            val s = c.backend.state.value
            if (s.signedIn && c.backend.unlocked.value && System.currentTimeMillis() - MainActivity.lastInteraction > MainActivity.LOCK_AFTER_MS) {
                c.session.sessionExpired = true
                c.backend.lock()
            }
        }
    }

    // In-app banners for backend events.
    LaunchedEffect(Unit) {
        c.banners.collect { e ->
            when (e) {
                is BackendEvent.Code -> banner.show(str(R.string.root_message_from_arro), e.purpose, BannerKind.Code, str(R.string.root_copy)) {
                    Share.copy(context, str(R.string.root_arro_code), e.code)
                }
                is BackendEvent.TransferUpdate -> banner.show(e.title, e.body, BannerKind.Info, str(R.string.root_view)) { nav.go(Routes.track(e.transferId)) }
                is BackendEvent.RateHit -> banner.show(e.title, e.body, BannerKind.Info, str(R.string.root_rates)) { nav.go(Routes.RATES) }
                is BackendEvent.KycDecided -> if (nav.currentDestination?.route != Routes.KYC_STATUS) {
                    banner.show(if (e.approved) str(R.string.root_you_re_verified) else str(R.string.root_we_need_a_clearer_photo),
                        if (e.approved) str(R.string.root_your_account_is_ready_to_send) else str(R.string.root_retake_your_id_photo_to_finish),
                        if (e.approved) BannerKind.Success else BannerKind.Error, str(R.string.root_view)) { nav.go(Routes.KYC_STATUS) }
                }
                is BackendEvent.ChatReply -> if (nav.currentDestination?.route != Routes.CHAT) {
                    banner.show(str(R.string.root_amina_replied), e.text.take(90), BannerKind.Info, str(R.string.root_open)) { nav.go(Routes.CHAT) }
                }
                is BackendEvent.Info -> banner.show(e.title, e.body, BannerKind.Success)
                is BackendEvent.OfferAvailable -> if (nav.currentDestination?.route != Routes.LOCK_RATE) {
                    banner.show(e.title, e.body, BannerKind.Info, str(R.string.root_view)) { nav.go(Routes.lockRate(e.to)) }
                }
                is BackendEvent.LockEnded -> banner.show(e.title, e.body, BannerKind.Info, str(R.string.root_new_offer)) { nav.go(Routes.lockRate(e.to)) }
            }
        }
    }
}
