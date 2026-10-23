package app.arro

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.arro.core.Appearance
import app.arro.core.Currency
import app.arro.core.PayoutType
import app.arro.core.TransferDraft
import app.arro.core.TransferStatus
import app.arro.ui.Routes
import app.arro.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the real app with the sample account and saves a screenshot of each main screen, plus the home
 * screen widgets, to filesDir/screenshots. Used to make the store and marketplace images, so they always show the
 * app as it is. Run on a device or emulator:
 *
 *   ./gradlew installDebug installDebugAndroidTest
 *   adb shell am instrument -w -e class app.arro.ScreenshotTour app.arro.debug.test/androidx.test.runner.AndroidJUnitRunner
 *   adb exec-out run-as app.arro.debug tar c files/screenshots > screenshots.tar
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as ArroApp
    private val c get() = app.container
    private val b get() = c.backend
    private val out = File(app.filesDir, "screenshots").apply { deleteRecursively(); mkdirs() }
    private var n = 0

    private fun shot(name: String, settleMs: Long = 1_400) {
        SystemClock.sleep(settleMs)
        instrumentation.waitForIdleSync()
        val bmp = instrumentation.uiAutomation.takeScreenshot() ?: return
        n++
        File(out, "%02d_%s.png".format(n, name)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun open(route: String, name: String, settleMs: Long = 1_400) {
        instrumentation.runOnMainSync {
            MainActivity.lastInteraction = System.currentTimeMillis() // keep the inactivity lock away
            c.session.pendingRoute = route
        }
        shot(name, settleMs)
    }

    /** Runs a backend call on the main thread. A failure (say, no network for live rates) skips that step only. */
    private fun onMain(block: suspend () -> Unit) {
        runCatching { runBlocking(Dispatchers.Main) { block() } }
    }

    private fun waitFor(timeoutMs: Long = 20_000, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(250)
    }

    @Test
    fun tour() {
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        instrumentation.runOnMainSync { b.resetEverything(); b.setDemo { it.copy(rateOffers = false) } }

        // First launch: welcome.
        var scenario = ActivityScenario.launch(MainActivity::class.java)
        shot("welcome", 3_500)
        scenario.close()

        // Sample account, signed in and unlocked.
        instrumentation.runOnMainSync {
            b.loadDemoAccount()
            b.setDemo { it.copy(rateOffers = false) }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor { b.state.value.rates.fetchedAt > 0 }
        shot("home", 3_500)

        // Lock Rate: an offer on Home (with its banner), the Lock a rate screen, then the locked rate.
        onMain { b.demoOfferNow() }
        shot("home_rate_offer", 1_800)
        SystemClock.sleep(3_500) // let the banner go
        open(Routes.lockRate("RWF"), "lock_rate_offer")
        val offerId = b.state.value.account?.rateOffer?.id
        if (offerId != null) onMain { b.lockOffer(offerId) }
        shot("lock_rate_locked")

        // Send: amount (locked rate and points), then review.
        instrumentation.runOnMainSync {
            val d = c.session.send
            d.reset("RW")
            d.recipientId = "R_CLAUDINE"; d.methodId = "PM_CU_MTN"; d.payoutType = PayoutType.MOBILE_MONEY; d.amountText = "250"
        }
        open(Routes.SEND_AMOUNT, "send_amount")
        instrumentation.runOnMainSync {
            val d = c.session.send
            d.fundingId = "PAY_VISA"
            b.holdRate(Currency.RWF)
            d.quote = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        }
        open(Routes.SEND_FUNDING, "send_funding")
        open(Routes.SEND_REVIEW, "send_review")

        val delivered = b.state.value.account?.transfers?.firstOrNull { it.status == TransferStatus.DELIVERED && it.payoutType == PayoutType.MOBILE_MONEY }?.id
        open(Routes.ACTIVITY, "activity")
        if (delivered != null) {
            open(Routes.detail(delivered), "transfer_detail")
            open(Routes.receipt(delivered), "receipt")
        }
        open(Routes.RECIPIENTS, "recipients")
        open(Routes.recipient("R_CLAUDINE"), "recipient_profile")
        open(Routes.WALLET, "wallet")
        open(Routes.walletAccount("GBP"), "wallet_gbp")
        open(Routes.convert("GBP"), "convert")
        open(Routes.RATES, "rates")
        open(Routes.rate("RWF"), "rate_detail")
        open(Routes.LOYALTY, "loyalty_points")
        open(Routes.REFER, "refer")
        open(Routes.INSIGHTS, "insights")
        open(Routes.NOTIFICATIONS, "notifications")
        open(Routes.ACCOUNT, "account")
        open(Routes.SECURITY, "security")
        open(Routes.VERIFICATION, "verification")
        open(Routes.PAY_METHODS, "payment_methods")
        open(Routes.STATEMENTS, "statements")
        open(Routes.HELP, "help")
        open(Routes.CHAT, "chat")
        open(Routes.DEMO, "demo_controls")

        // A real transfer, tracked while it moves.
        var transferId = ""
        onMain {
            val q = c.session.send.quote ?: return@onMain
            transferId = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support")).id
        }
        if (transferId.isNotEmpty()) open(Routes.track(transferId), "track_in_progress", 3_000)

        // Dark theme.
        instrumentation.runOnMainSync { b.updateSettings { it.copy(appearance = Appearance.DARK) } }
        open(Routes.HOME, "dark_home", 2_500)
        open(Routes.SEND_AMOUNT, "dark_send_amount")
        open(Routes.LOYALTY, "dark_loyalty")
        open(Routes.ACCOUNT, "dark_account")
        instrumentation.runOnMainSync { b.updateSettings { it.copy(appearance = Appearance.LIGHT) } }
        open(Routes.HOME, "home_again", 2_000)
        scenario.close()

        // Home screen widgets, drawn from the same views the launcher shows.
        onMain { b.demoOfferNow() }
        for (night in listOf(false, true)) {
            val suffix = if (night) "_dark" else ""
            widget("quick", wide = true, wDp = 340, hDp = 184, night = night, name = "widget_quick_send$suffix")
            widget("balance", wide = false, wDp = 168, hDp = 168, night = night, name = "widget_balance$suffix")
            widget("rate", wide = false, wDp = 168, hDp = 168, night = night, name = "widget_rate$suffix")
            widget("rate", wide = true, wDp = 340, hDp = 168, night = night, name = "widget_rate_wide$suffix")
        }
    }

    /** Renders one widget's RemoteViews into a PNG at the given size, in the light or dark theme. */
    private fun widget(which: String, wide: Boolean, wDp: Int, hDp: Int, night: Boolean, name: String) {
        instrumentation.runOnMainSync {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val ctx: Context = app.createConfigurationContext(config)
            val density = ctx.resources.displayMetrics.density
            val w = (wDp * density).toInt(); val h = (hDp * density).toInt()
            val parent = FrameLayout(ctx)
            val view = Widgets.previewViews(app, which, wide).apply(ctx, parent)
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.TRANSPARENT)
            view.draw(Canvas(bmp))
            n++
            File(out, "%02d_%s.png".format(n, name)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
