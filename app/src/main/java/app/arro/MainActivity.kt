package app.arro

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.arro.core.Appearance
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import app.arro.platform.Notifier
import app.arro.ui.ArroRoot
import app.arro.ui.Routes
import app.arro.ui.theme.ArroTheme

/** The only activity. A FragmentActivity so the system biometric prompt can attach to it. */
class MainActivity : FragmentActivity() {
    private var backgroundAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val state by container.backend.state.collectAsStateWithLifecycle()
            ArroTheme(state.account?.settings?.appearance ?: Appearance.LIGHT) { ArroRoot() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val transfer = i?.getStringExtra(Notifier.EXTRA_TRANSFER)
        val route = i?.getStringExtra(Notifier.EXTRA_ROUTE)
        when {
            !transfer.isNullOrEmpty() -> container.session.pendingRoute = Routes.track(transfer)
            !route.isNullOrEmpty() && Routes.isDeepLink(route) -> container.session.pendingRoute = route
        }
    }

    override fun onStart() {
        super.onStart()
        val c = container
        c.inForeground = true
        c.backend.foreground = true
        val away = if (backgroundAt > 0) System.currentTimeMillis() - backgroundAt else 0
        if (away > LOCK_AFTER_MS && c.backend.state.value.signedIn && c.backend.unlocked.value) {
            c.session.sessionExpired = true
            c.backend.lock()
        }
        backgroundAt = 0
        c.connectivity.recheck()
    }

    override fun onStop() {
        super.onStop()
        container.inForeground = false
        container.backend.foreground = false
        backgroundAt = System.currentTimeMillis()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        lastInteraction = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    companion object {
        /** Lock after five minutes in the background or without a touch. */
        const val LOCK_AFTER_MS = 5 * 60_000L
        @Volatile var lastInteraction: Long = System.currentTimeMillis()
    }
}
