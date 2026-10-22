package app.arro.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import app.arro.AppContainer
import app.arro.MainActivity
import app.arro.R
import app.arro.container
import app.arro.core.AppState
import app.arro.core.Catalog
import app.arro.core.Currency
import app.arro.core.Insights
import app.arro.core.Money
import app.arro.core.RateLock
import app.arro.core.RateOffer
import app.arro.core.Rates
import app.arro.core.Text
import app.arro.platform.Notifier
import app.arro.ui.Routes
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What the widgets show. Built from the app's own backend state in this process, the same data the screens use;
 * nothing is exported to other apps. The balance and recent recipients appear only when the person has turned on
 * "Show balance on home screen widgets" in Security, because a widget can be seen without unlocking Arro.
 */
data class WidgetData(
    val signedIn: Boolean,
    val showPrivate: Boolean,
    val balanceMinor: Long,
    val otherWallets: Int,
    val frozen: Boolean,
    /** Recent recipients: id, first name, initials. */
    val recents: List<Triple<String, String, String>>,
    val rateCur: Currency,
    /** Today's customer rate, or null when no live rate has ever been fetched (fallback rates are never shown). */
    val liveRate: Double?,
    val ratesAt: Long,
    val demoMoved: Boolean,
    val online: Boolean,
    val offer: RateOffer?,
    val lock: RateLock?,
) {
    /** When the widgets next need redrawing because an offer or a locked rate ends. */
    val nextChangeAt: Long? get() = lock?.expiresAt ?: offer?.expiresAt
}

object Widgets {
    const val ACTION_REFRESH = "app.arro.widget.action.REFRESH"
    const val ACTION_EXPIRE = "app.arro.widget.action.EXPIRE"
    /** A rate older than this is labelled as possibly out of date. */
    private const val STALE_MS = 60 * 60_000L
    /** The widget's own refresh only fetches when rates are older than this. */
    private const val REFRESH_AFTER_MS = 15 * 60_000L
    /** Width from which the wider layouts are used. */
    private const val WIDE_DP = 250

    fun data(c: AppContainer, now: Long = System.currentTimeMillis()): WidgetData {
        val s = c.backend.state.value
        val a = if (s.signedIn) s.account else null
        val lock = a?.anyOfferLock(now)
        val offer = if (lock == null) a?.liveOffer(now) else null
        val cur = Currency.of(lock?.to ?: offer?.to ?: c.backend.relevantCurrency(a).code)
        val show = a?.settings?.widgetBalance == true
        return WidgetData(
            signedIn = a != null,
            showPrivate = show,
            balanceMinor = a?.wallet(Currency.GBP)?.balanceMinor ?: 0,
            otherWallets = ((a?.wallets?.size ?: 1) - 1).coerceAtLeast(0),
            frozen = a?.frozen == true,
            recents = if (a != null && show) Insights.recentRecipients(a).take(3).map { Triple(it.id, it.firstName, it.initials) } else emptyList(),
            rateCur = cur,
            liveRate = if (s.rates.fetchedAt > 0) Rates.customer(s.rates, Currency.GBP, cur) else null,
            ratesAt = s.rates.fetchedAt,
            demoMoved = s.rates.demoMoved,
            online = c.connectivity.online.value,
            offer = offer,
            lock = lock,
        )
    }

    /** The parts of the app state the widgets show, for change detection. Time isn't part of it. */
    fun key(c: AppContainer, s: AppState, online: Boolean): List<Any?> {
        val now = System.currentTimeMillis()
        val a = if (s.signedIn) s.account else null
        return listOf(
            a?.id, a?.settings?.widgetBalance, a?.wallet(Currency.GBP)?.balanceMinor, a?.wallets?.size, a?.frozen,
            a?.let { Insights.recentRecipients(it).take(3).map { r -> r.id + r.firstName } },
            s.rates.fetchedAt, s.rates.demoMoved, s.rates.mid, c.backend.relevantCurrency(a),
            a?.liveOffer(now)?.id, a?.anyOfferLock(now)?.id, online,
        )
    }

    /** Redraws every Arro widget on the home screen. Cheap when none are placed. */
    fun update(context: Context, refreshing: Boolean = false) {
        val m = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
        val balanceIds = m.getAppWidgetIds(ComponentName(context, BalanceWidget::class.java))
        val rateIds = m.getAppWidgetIds(ComponentName(context, RateWidget::class.java))
        if (balanceIds.isEmpty() && rateIds.isEmpty()) return
        val d = data(context.container)
        for (id in balanceIds) m.updateAppWidget(id, sized(context, m.getAppWidgetOptions(id)) { wide -> balanceViews(context, d, wide) })
        for (id in rateIds) m.updateAppWidget(id, sized(context, m.getAppWidgetOptions(id)) { wide -> rateViews(context, d, wide, refreshing) })
        scheduleExpiry(context, if (rateIds.isEmpty()) null else d.nextChangeAt)
    }

    /**
     * Fetches rates if they're old (or always, for a tap on refresh), then redraws. Runs inside the broadcast's
     * async window, with a timeout so the receiver always finishes.
     */
    fun refreshThenUpdate(context: Context, pending: BroadcastReceiver.PendingResult, force: Boolean) {
        val c = context.container
        c.scope.launch {
            try {
                val b = c.backend
                val age = System.currentTimeMillis() - b.state.value.rates.fetchedAt
                if (force || age > REFRESH_AFTER_MS) withTimeoutOrNull(8_000) { b.refreshRates() }
                c.connectivity.recheck()
                update(context)
            } finally {
                pending.finish()
            }
        }
    }

    // ------------------------------------------------------------------ layouts

    /** Responsive layouts: Android 12+ picks by size itself; older versions use the reported width. */
    private fun sized(context: Context, options: Bundle?, build: (Boolean) -> RemoteViews): RemoteViews {
        if (Build.VERSION.SDK_INT >= 31) {
            return RemoteViews(mapOf(SizeF(110f, 110f) to build(false), SizeF(WIDE_DP.toFloat(), 110f) to build(true)))
        }
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        return build(minWidth >= WIDE_DP)
    }

    private fun balanceViews(context: Context, d: WidgetData, wide: Boolean): RemoteViews {
        val v = RemoteViews(context.packageName, if (wide) R.layout.widget_balance_wide else R.layout.widget_balance)
        v.setOnClickPendingIntent(android.R.id.background, open(context, null))
        if (!d.signedIn) {
            v.setTextViewText(R.id.balance_label, "Arro")
            v.setTextViewText(R.id.balance_amount, "Log in")
            v.setTextViewText(R.id.balance_sub, "Log in to Arro to see your balance and send money.")
            v.setTextViewText(R.id.send_text, "Open Arro")
            v.setOnClickPendingIntent(R.id.send_button, open(context, null))
            if (wide) showRecents(context, v, emptyList(), "Your recent recipients appear here once you're logged in.")
            return v
        }
        v.setTextViewText(R.id.balance_label, "GBP balance")
        v.setTextViewText(R.id.balance_amount, if (d.showPrivate) Money.format(d.balanceMinor, Currency.GBP) else "£ ••••")
        v.setTextViewText(R.id.balance_sub, when {
            !d.showPrivate -> "Hidden. Show it in Arro, Security."
            d.frozen -> "Account frozen"
            d.otherWallets > 0 -> "+${d.otherWallets} more ${if (d.otherWallets == 1) "currency" else "currencies"} · ${Text.time(System.currentTimeMillis())}"
            else -> "As of ${Text.time(System.currentTimeMillis())}"
        })
        v.setContentDescription(R.id.balance_amount, if (d.showPrivate) "GBP balance ${Money.format(d.balanceMinor, Currency.GBP)}" else "Balance hidden")
        v.setTextViewText(R.id.send_text, "Send money")
        // Quick Send opens the send flow after unlocking; the usual checks, review and approval still apply.
        v.setOnClickPendingIntent(R.id.send_button, open(context, Routes.QUICK_SEND))
        if (wide) showRecents(context, v, d.recents, if (d.showPrivate) "Your recent recipients appear here after your first transfer." else context.getString(R.string.widget_recents_hidden))
        return v
    }

    private fun showRecents(context: Context, v: RemoteViews, recents: List<Triple<String, String, String>>, empty: String) {
        val slots = listOf(
            Triple(R.id.recent_1, R.id.recent_1_initials, R.id.recent_1_name),
            Triple(R.id.recent_2, R.id.recent_2_initials, R.id.recent_2_name),
            Triple(R.id.recent_3, R.id.recent_3_initials, R.id.recent_3_name),
        )
        slots.forEachIndexed { i, (box, initials, name) ->
            val r = recents.getOrNull(i)
            v.setViewVisibility(box, if (r == null) View.GONE else View.VISIBLE)
            if (r != null) {
                v.setTextViewText(initials, r.third)
                v.setTextViewText(name, r.second)
                v.setContentDescription(box, "Send to ${r.second}")
                v.setOnClickPendingIntent(box, open(context, Routes.quickSend(r.first)))
            }
        }
        v.setViewVisibility(R.id.recents_empty, if (recents.isEmpty()) View.VISIBLE else View.GONE)
        v.setTextViewText(R.id.recents_empty, empty)
    }

    private fun rateViews(context: Context, d: WidgetData, wide: Boolean, refreshing: Boolean): RemoteViews {
        val v = RemoteViews(context.packageName, if (wide) R.layout.widget_rate_wide else R.layout.widget_rate)
        val cur = d.rateCur
        val now = System.currentTimeMillis()
        val sample = 25_000L
        v.setTextViewText(R.id.rate_pair, "GBP → ${cur.code}")
        v.setOnClickPendingIntent(android.R.id.background, open(context, if (d.signedIn) Routes.rate(cur.code) else null))
        v.setOnClickPendingIntent(R.id.rate_refresh, refreshIntent(context))
        var status: String
        var warn: String? = null
        var extra = ""
        when {
            d.lock != null -> {
                val l = d.lock
                badge(v, "Locked", R.drawable.widget_pill_locked)
                v.setTextViewText(R.id.rate_value, Money.rate(l.rate, Money.rateDecimals(l.rate)))
                v.setTextViewText(R.id.rate_unit, "${cur.code} for £1 · locked rate")
                status = "Held for your next transfer until ${Text.time(l.expiresAt)}"
                extra = "£250 sends ${Money.format(Money.convert(sample, Currency.GBP, cur, l.rate), cur)}"
                button(context, v, "Send at this rate", R.drawable.ic_arrow_up_right,
                    Catalog.countryForCurrency(cur)?.let { Routes.quickSendTo(it.code) } ?: Routes.lockRate(cur.code))
            }
            d.offer != null -> {
                val o = d.offer
                badge(v, "Offer", R.drawable.widget_pill_offer)
                v.setTextViewText(R.id.rate_value, Money.rate(o.rate, Money.rateDecimals(o.rate)))
                v.setTextViewText(R.id.rate_unit, "${cur.code} for £1 · offer, not locked yet")
                status = "Offer ends ${Text.time(o.expiresAt)}. Usually ${Money.rate(o.standardRate, Money.rateDecimals(o.standardRate))}."
                extra = "Lock it for ${o.lockMinutes} minutes"
                button(context, v, "Lock this rate", R.drawable.ic_lock, Routes.lockRate(cur.code))
            }
            d.liveRate == null -> {
                badge(v, null, 0)
                v.setTextViewText(R.id.rate_value, "—")
                v.setTextViewText(R.id.rate_unit, "${cur.code} for £1")
                status = ""
                warn = if (refreshing) "Updating…" else if (!d.online) "You're offline. Rates appear once you're connected." else "Rate unavailable. Tap refresh to try again."
                button(context, v, "Open Arro", R.drawable.ic_arrow_up_right, null)
            }
            else -> {
                val rate = d.liveRate
                badge(v, "Live", R.drawable.widget_pill_live)
                v.setTextViewText(R.id.rate_value, Money.rate(rate, Money.rateDecimals(rate)))
                v.setTextViewText(R.id.rate_unit, "${cur.code} for £1 · today's rate")
                val stale = now - d.ratesAt > STALE_MS
                status = "Updated ${Text.time(d.ratesAt)} · not guaranteed until locked" + if (d.demoMoved) " · demo market move" else ""
                when {
                    refreshing -> { warn = "Updating…"; status = "" }
                    stale && !d.online -> { warn = "Offline · rate from ${Text.relativeDay(d.ratesAt, now).lowercase()}"; status = "" }
                    stale -> { warn = "Updated ${Text.relativeDay(d.ratesAt, now).lowercase()} · may be out of date"; status = "" }
                }
                extra = "£250 sends about ${Money.format(Money.convert(sample, Currency.GBP, cur, rate), cur)}"
                if (d.signedIn) button(context, v, "Lock a rate", R.drawable.ic_lock, Routes.lockRate(cur.code))
                else button(context, v, "Open Arro", R.drawable.ic_arrow_up_right, null)
            }
        }
        v.setTextViewText(R.id.rate_status, status)
        v.setViewVisibility(R.id.rate_status, if (status.isEmpty()) View.GONE else View.VISIBLE)
        v.setTextViewText(R.id.rate_status_warn, warn ?: "")
        v.setViewVisibility(R.id.rate_status_warn, if (warn == null) View.GONE else View.VISIBLE)
        if (wide) v.setTextViewText(R.id.rate_extra, extra)
        return v
    }

    private fun badge(v: RemoteViews, text: String?, background: Int) {
        v.setViewVisibility(R.id.rate_badge, if (text == null) View.GONE else View.VISIBLE)
        if (text != null) {
            v.setTextViewText(R.id.rate_badge, text)
            v.setInt(R.id.rate_badge, "setBackgroundResource", background)
        }
    }

    private fun button(context: Context, v: RemoteViews, text: String, icon: Int, route: String?) {
        v.setTextViewText(R.id.rate_button_text, text)
        v.setImageViewResource(R.id.rate_button_icon, icon)
        v.setOnClickPendingIntent(R.id.rate_button, open(context, route))
    }

    // ------------------------------------------------------------------ intents and alarms

    /** Opens Arro, then [route] once it's unlocked. Each route gets its own PendingIntent. */
    private fun open(context: Context, route: String?): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            action = "app.arro.widget.OPEN." + (route ?: "app")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (route != null) putExtra(Notifier.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(context, (route ?: "app").hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun refreshIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, 1, Intent(context, RateWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /**
     * Redraws the widgets when an offer or locked rate ends, so an ended offer isn't left looking active. Uses an
     * inexact, non-waking alarm (no exact-alarm permission); the widget also always shows the end time.
     */
    fun cancelExpiry(context: Context) = scheduleExpiry(context, null)

    private fun scheduleExpiry(context: Context, at: Long?) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(context, 2, Intent(context, RateWidget::class.java).setAction(ACTION_EXPIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (at == null) am.cancel(pi) else am.setWindow(AlarmManager.RTC, at + 1_000, 60_000, pi)
    }
}

/** Widget A: GBP balance (if allowed) and Quick Send. */
class BalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = Widgets.update(context)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        Widgets.update(context)
}

/** Widget B: today's rate for where you send, a rate offer, or your locked rate. */
class RateWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Widgets.ACTION_REFRESH -> {
                Widgets.update(context, refreshing = true)
                Widgets.refreshThenUpdate(context, goAsync(), force = true)
            }
            Widgets.ACTION_EXPIRE -> Widgets.update(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        Widgets.refreshThenUpdate(context, goAsync(), force = false)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        Widgets.update(context)

    /** The last rate widget was removed: nothing needs redrawing when an offer ends. */
    override fun onDisabled(context: Context) = Widgets.cancelExpiry(context)
}
