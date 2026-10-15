package app.arro.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** How many dark screens are showing right now (navigation briefly composes two at once). */
private var darkRequests = 0

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

private fun applyBars(view: View) {
    val window = view.context.activity()?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)
    val light = darkRequests == 0
    controller.isAppearanceLightStatusBars = light
    controller.isAppearanceLightNavigationBars = light
}

/** Call from a screen with a dark background so the clock, battery and navigation icons turn white. */
@Composable
fun DarkSystemBars() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        darkRequests++
        applyBars(view)
        onDispose {
            darkRequests = (darkRequests - 1).coerceAtLeast(0)
            applyBars(view)
        }
    }
}
