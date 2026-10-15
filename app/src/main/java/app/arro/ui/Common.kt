package app.arro.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import app.arro.AppContainer
import app.arro.container
import app.arro.core.Account
import app.arro.core.AppState
import app.arro.core.ArroBackend
import app.arro.core.ArroException
import app.arro.ui.components.Ic
import app.arro.ui.components.Icons
import app.arro.ui.components.MaxContentWidth
import app.arro.ui.components.Txt
import app.arro.ui.components.tap
import app.arro.ui.theme.C
import app.arro.ui.theme.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalNav = staticCompositionLocalOf<NavHostController> { error("No navigation") }
val LocalBanner = staticCompositionLocalOf<BannerState> { error("No banner host") }

@Composable
fun container(): AppContainer = LocalContext.current.container

@Composable
fun backend(): ArroBackend = container().backend

@Composable
fun appState(): State<AppState> = backend().state.collectAsStateWithLifecycle()

/** The signed-in account, or null while signing out. */
@Composable
fun account(): Account? = appState().value.account

/** Ticks every second, for countdowns and live status. */
@Composable
fun nowTicker(periodMs: Long = 1000): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

// ---------------------------------------------------------------- navigation helpers

fun NavHostController.go(route: String) = navigate(route) { launchSingleTop = true }

fun NavHostController.back() {
    if (!popBackStack()) navigate(Routes.HOME) { popUpTo(0) }
}

/** Goes to a tab root, keeping only Home underneath. */
fun NavHostController.tab(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
    launchSingleTop = true
}

/** Clears the back stack and shows [route]. */
fun NavHostController.resetTo(route: String) = navigate(route) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

/** Pops back to [route] if it is on the stack, otherwise navigates there. */
fun NavHostController.backTo(route: String) {
    if (!popBackStack(route, inclusive = false)) go(route)
}

// ---------------------------------------------------------------- async calls with busy and error state

class Call(val busy: MutableState<Boolean>, val error: MutableState<String?>)

@Composable
fun rememberCall(): Call = remember { Call(mutableStateOf(false), mutableStateOf(null)) }

/** Runs [block], tracking busy state. ArroException messages go to [Call.error]; [onError] can intercept by code. */
fun CoroutineScope.launchCall(call: Call, onError: ((ArroException) -> Boolean)? = null, block: suspend () -> Unit) {
    if (call.busy.value) return
    call.error.value = null
    call.busy.value = true
    launch {
        try {
            block()
        } catch (e: ArroException) {
            if (onError?.invoke(e) != true) call.error.value = e.message
        } catch (e: Exception) {
            call.error.value = "Something went wrong. Please try again."
        } finally {
            call.busy.value = false
        }
    }
}

// ---------------------------------------------------------------- banners (.toast)

enum class BannerKind { Info, Success, Error, Code }

data class BannerMessage(val title: String, val body: String, val kind: BannerKind, val id: Long = System.nanoTime(), val action: String? = null, val onAction: (() -> Unit)? = null)

class BannerState {
    var current by mutableStateOf<BannerMessage?>(null)
        private set

    fun show(title: String, body: String = "", kind: BannerKind = BannerKind.Info, action: String? = null, onAction: (() -> Unit)? = null) {
        current = BannerMessage(title, body, kind, action = action, onAction = onAction)
    }

    fun dismiss() { current = null }
}

@Composable
fun BoxScope.BannerHost(state: BannerState) {
    val msg = state.current
    LaunchedEffect(msg?.id) {
        if (msg != null) {
            delay(if (msg.kind == BannerKind.Code) 9_000 else 4_000)
            state.dismiss()
        }
    }
    AnimatedVisibility(
        visible = msg != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        val m = msg ?: return@AnimatedVisibility
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(18.dp))
                .background(C.Ink, RoundedCornerShape(18.dp))
                .tap { m.onAction?.invoke(); state.dismiss() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            @DrawableRes val icon = when (m.kind) {
                BannerKind.Success -> Icons.Check
                BannerKind.Error -> Icons.Alert
                BannerKind.Code -> Icons.Chat
                BannerKind.Info -> Icons.Bell
            }
            Box(Modifier.background(if (m.kind == BannerKind.Error) C.Err else C.Lime, RoundedCornerShape(10.dp)).padding(6.dp)) {
                Ic(icon, tint = if (m.kind == BannerKind.Error) C.White else C.Ink, size = 18.dp)
            }
            Column(Modifier.weight(1f)) {
                Txt(m.title, T.H3, color = C.White)
                if (m.body.isNotEmpty()) Txt(m.body, T.Cap, color = C.DarkCap)
            }
            if (m.action != null) Txt(m.action, T.Link.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = C.Lime)
        }
    }
}
