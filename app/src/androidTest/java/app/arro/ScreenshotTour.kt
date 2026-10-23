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

    /**
     * Saves a screenshot after [settleMs]. If [ready] says the screen isn't drawn yet (say, Home still shows its
     * loading skeleton on a slow emulator), it waits and tries again, a few times.
     */
    private fun shot(name: String, settleMs: Long = 1_400, ready: (Bitmap) -> Boolean = { true }) {
        SystemClock.sleep(settleMs)
        var bmp: Bitmap? = null
        for (attempt in 0 until 6) {
            instrumentation.waitForIdleSync()
            bmp = instrumentation.uiAutomation.takeScreenshot() ?: return
            if (ready(bmp)) break
            SystemClock.sleep(1_500)
        }
        n++
        File(out, "%02d_%s.png".format(n, name)).outputStream().use { bmp!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** True once the top of the screen has text in the brand's ink colour (the loading skeleton is only grey). */
    private fun hasInk(bmp: Bitmap): Boolean {
        var ink = 0
        val h = bmp.height / 3
        for (y in 0 until h step 6) for (x in 0 until bmp.width step 6) {
            val p = bmp.getPixel(x, y)
            if (Color.red(p) < 40 && Color.green(p) < 70 && Color.blue(p) < 70) ink++
        }
        return ink > 40
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
        shot("home", 3_500, ::hasInk)

        // Lock Rate: an offer on Home (with its banner), the Lock a rate screen, then the locked rate.
        onMain { b.demoOfferNow() }
        shot("home_rate_offer", 1_800)
        SystemClock.sleep(3_500) // let the banner go
        // The offer is for where this account sends most recently: Kenya, to Wanjiru.
        val offer = b.state.value.account?.rateOffer
        val offerCur = Currency.of(offer?.to ?: "KES")
        open(Routes.lockRate(offerCur.code), "lock_rate_offer")
        if (offer != null) onMain { b.lockOffer(offer.id) }
        shot("lock_rate_locked")

        // Send to Wanjiru at the locked rate: amount (with the lock and points), payment, then review.
        instrumentation.runOnMainSync {
            val d = c.session.send
            d.reset("KE")
            d.recipientId = "R_WANJIRU"; d.methodId = "PM_WK"; d.payoutType = PayoutType.MOBILE_MONEY; d.amountText = "250"
        }
        open(Routes.SEND_AMOUNT, "send_amount")
        instrumentation.runOnMainSync {
            val d = c.session.send
            d.fundingId = "PAY_VISA"
            b.holdRate(Currency.KES)
            d.quote = b.quote(Currency.KES, 25_000, PayoutType.MOBILE_MONEY, "KE", "PAY_VISA")
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
            transferId = b.createTransfer(TransferDraft("R_WANJIRU", "PM_WK", q, "PAY_VISA", "Family support")).id
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
            widget("quick", wDp = 340, hDp = 184, night = night, name = "widget_quick_send$suffix")
            widget("balance", wDp = 168, hDp = 168, night = night, name = "widget_balance$suffix")
            widget("rate", wDp = 168, hDp = 168, night = night, name = "widget_rate$suffix")
            widget("rate", wDp = 340, hDp = 168, night = night, name = "widget_rate_wide$suffix")
        }
        // Layout checks at the smallest size of each step and at common launcher sizes, to catch clipped text.
        val checks = File(out, "checks").apply { mkdirs() }
        val sizes = mapOf(
            "quick" to listOf(110 to 110, 180 to 126, 250 to 152, 250 to 180, 300 to 140, 360 to 200, 400 to 110),
            "balance" to listOf(110 to 110, 110 to 142, 120 to 156, 150 to 150, 180 to 200),
            "rate" to listOf(110 to 110, 110 to 124, 110 to 164, 140 to 178, 250 to 110, 250 to 124, 250 to 164, 300 to 178, 400 to 220),
        )
        for ((which, list) in sizes) for ((w, h) in list) widget(which, w, h, night = false, name = "${which}_${w}x$h", dir = checks)
    }

    /** Renders one widget's RemoteViews into a PNG at the given size, in the light or dark theme. */
    private fun widget(which: String, wDp: Int, hDp: Int, night: Boolean, name: String, dir: File = out) {
        instrumentation.runOnMainSync {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val ctx: Context = app.createConfigurationContext(config)
            val density = ctx.resources.displayMetrics.density
            val w = (wDp * density).toInt(); val h = (hDp * density).toInt()
            val parent = FrameLayout(ctx)
            val view = Widgets.previewViews(app, which, wDp.toFloat(), hDp.toFloat()).apply(ctx, parent)
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.TRANSPARENT)
            view.draw(Canvas(bmp))
            val file = if (dir == out) File(out, "%02d_%s.png".format(++n, name)) else File(dir, "$name.png")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
