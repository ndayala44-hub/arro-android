package app.arro.widget

import app.arro.ui.str
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
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
import app.arro.ui.components.flagRes
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A recipient on the Quick Send widget. [color] is the app's avatar colour index; [flag] the payout country's flag. */
data class WidgetRecipient(val id: String, val firstName: String, val initials: String, val color: Int, val flag: String)

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
    /** The people sent to most recently (favourites first), for Quick Send. */
    val recents: List<WidgetRecipient>,
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
    private const val STALE_MS = 3 * 60 * 60_000L
    /** The widget's own refresh only fetches when rates are older than this (the provider asks for at most hourly). */
    private const val REFRESH_AFTER_MS = 60 * 60_000L
    /** Width (dp) from which the wider layouts are used. */
    private const val WIDE_DP = 250f

    /**
     * The sizes (dp) each widget has a layout for. Each step adds what fits: a short widget drops the button or
     * caption rather than cutting it off. Android 12+ picks from these itself; older versions use [fit].
     */
    private val BALANCE_SIZES = grid(listOf(110f, 150f), listOf(110f, 142f, 156f))
    private val RATE_SIZES = grid(listOf(110f, 165f, WIDE_DP), listOf(110f, 124f, 164f, 178f))
    private val QUICK_SIZES = grid(listOf(180f, 220f, 290f), listOf(110f, 126f, 152f, 180f))

    private fun grid(widths: List<Float>, heights: List<Float>) = widths.flatMap { w -> heights.map { h -> SizeF(w, h) } }

    /** The largest size in [sizes] that fits [wDp] x [hDp], as Android 12+ chooses; the smallest when none fits. */
    private fun fit(sizes: List<SizeF>, wDp: Float, hDp: Float): SizeF {
        val w = sizes.map { it.width }.filter { it <= wDp }.maxOrNull() ?: sizes.minOf { it.width }
        val h = sizes.map { it.height }.filter { it <= hDp }.maxOrNull() ?: sizes.minOf { it.height }
        return SizeF(w, h)
    }

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
            recents = if (a != null && show) Insights.recentRecipients(a).take(4).map {
                WidgetRecipient(it.id, it.firstName, it.initials, it.color, Catalog.countryOrNull(it.country)?.currency?.flag ?: "gb")
            } else emptyList(),
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
            a?.let { Insights.recentRecipients(it).take(4).map { r -> listOf(r.id, r.name, r.color, r.country) } },
            s.rates.fetchedAt, s.rates.demoMoved, s.rates.mid, c.backend.relevantCurrency(a),
            a?.liveOffer(now)?.id, a?.anyOfferLock(now)?.id, online,
        )
    }

    /** Redraws every Arro widget on the home screen. Cheap when none are placed. */
    fun update(context: Context, refreshing: Boolean = false) {
        val m = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
        val balanceIds = m.getAppWidgetIds(ComponentName(context, BalanceWidget::class.java))
        val quickIds = m.getAppWidgetIds(ComponentName(context, QuickSendWidget::class.java))
        val rateIds = m.getAppWidgetIds(ComponentName(context, RateWidget::class.java))
        if (balanceIds.isEmpty() && quickIds.isEmpty() && rateIds.isEmpty()) return
        val d = data(context.container)
        for (id in balanceIds) m.updateAppWidget(id, responsive(m.getAppWidgetOptions(id), BALANCE_SIZES, SizeF(150f, 156f)) { balanceViews(context, d, it) })
        for (id in quickIds) m.updateAppWidget(id, responsive(m.getAppWidgetOptions(id), QUICK_SIZES, SizeF(290f, 180f)) { quickSendViews(context, d, it) })
        for (id in rateIds) m.updateAppWidget(id, responsive(m.getAppWidgetOptions(id), RATE_SIZES, SizeF(165f, 164f)) { rateViews(context, d, it, refreshing) })
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

    /**
     * One widget's views for a [wDp] x [hDp] widget, from the current data, chosen the way the launcher would.
     * Used by the screenshot tour.
     */
    internal fun previewViews(context: Context, which: String, wDp: Float, hDp: Float): RemoteViews {
        val d = data(context.container)
        return when (which) {
            "balance" -> balanceViews(context, d, fit(BALANCE_SIZES, wDp, hDp))
            "quick" -> quickSendViews(context, d, fit(QUICK_SIZES, wDp, hDp))
            else -> rateViews(context, d, fit(RATE_SIZES, wDp, hDp), false)
        }
    }

    // ------------------------------------------------------------------ layouts

    /**
     * Responsive layouts: Android 12+ picks one of [sizes] itself as the widget is resized. Older versions get the
     * one for the size the launcher reports in portrait, or [fallback] when it doesn't report one.
     */
    private fun responsive(options: Bundle?, sizes: List<SizeF>, fallback: SizeF, build: (SizeF) -> RemoteViews): RemoteViews {
        if (Build.VERSION.SDK_INT >= 31) return RemoteViews(sizes.associateWith(build))
        val w = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val h = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
        return build(if (w <= 0 || h <= 0) fallback else fit(sizes, w.toFloat(), h.toFloat()))
    }

    /**
     * Widget A: balance and a Send button. Short widgets drop the button (the whole widget opens Arro); narrow ones
     * drop the logo and show whole pounds in a smaller size.
     */
    private fun balanceViews(context: Context, d: WidgetData, size: SizeF): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_balance)
        val twoLines = size.height >= 156f
        val narrow = size.width < 150f
        v.setOnClickPendingIntent(android.R.id.background, open(context, null))
        v.setViewVisibility(R.id.balance_icon, if (narrow) View.GONE else View.VISIBLE)
        v.setTextViewTextSize(R.id.balance_amount, TypedValue.COMPLEX_UNIT_SP, if (narrow) 20f else 24f)
        v.setViewVisibility(R.id.send_button, if (size.height >= 142f) View.VISIBLE else View.GONE)
        v.setInt(R.id.balance_sub, "setMaxLines", if (twoLines) 2 else 1)
        if (!d.signedIn) {
            v.setTextViewText(R.id.balance_label, str(R.string.widget_arro))
            v.setTextViewText(R.id.balance_amount, str(R.string.widget_log_in))
            v.setTextViewText(R.id.balance_sub, if (twoLines) str(R.string.widget_log_in_to_arro_to_see) else "")
            v.setViewVisibility(R.id.balance_sub, if (twoLines) View.VISIBLE else View.GONE)
            v.setTextViewText(R.id.send_text, str(R.string.widget_open_arro))
            v.setOnClickPendingIntent(R.id.send_button, open(context, null))
            return v
        }
        v.setTextViewText(R.id.balance_label, str(R.string.widget_gbp_balance))
        v.setTextViewText(R.id.balance_amount, balanceText(d, whole = narrow))
        v.setTextViewText(R.id.balance_sub, when {
            !d.showPrivate -> if (twoLines) str(R.string.widget_hidden_show_it_in_arro_security) else str(R.string.widget_balance_hidden)
            d.frozen -> str(R.string.widget_account_frozen)
            d.otherWallets > 0 -> str(R.string.widget_x_more_x_x, d.otherWallets, if (d.otherWallets == 1) str(R.string.widget_currency) else str(R.string.widget_currencies), Text.time(System.currentTimeMillis()))
            else -> str(R.string.widget_as_of_x, Text.time(System.currentTimeMillis()))
        })
        v.setContentDescription(R.id.balance_amount, balanceDescription(d))
        v.setTextViewText(R.id.send_text, if (narrow) str(R.string.widget_send_2) else str(R.string.widget_send_money_2))
        // Quick Send opens the send flow after unlocking; the usual checks, review and approval still apply.
        v.setOnClickPendingIntent(R.id.send_button, open(context, Routes.QUICK_SEND))
        return v
    }

    /** The GBP balance, or dots when it's hidden. [whole] drops the pence, for narrow widgets. */
    private fun balanceText(d: WidgetData, whole: Boolean = false) = when {
        !d.showPrivate -> "£ ••••"
        whole -> Money.split(d.balanceMinor, Currency.GBP).first
        else -> Money.format(d.balanceMinor, Currency.GBP)
    }
    private fun balanceDescription(d: WidgetData) = if (d.showPrivate) str(R.string.widget_available_balance_x, Money.format(d.balanceMinor, Currency.GBP)) else str(R.string.widget_balance_hidden)

    /**
     * Widget C: available balance, a Send button and people to send to again. Narrower widgets show fewer people;
     * shorter ones drop, in turn, the "Send again" caption, the label row and the names under the avatars.
     */
    private fun quickSendViews(context: Context, d: WidgetData, size: SizeF): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_quick_send)
        val slots = when { size.width >= 290f -> 4; size.width >= 220f -> 3; else -> 2 }
        val step = when { size.height >= 180f -> 3; size.height >= 152f -> 2; size.height >= 126f -> 1; else -> 0 }
        v.setOnClickPendingIntent(android.R.id.background, open(context, null))
        v.setViewVisibility(R.id.qs_title, if (step >= 3) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.qs_header, if (step >= 2) View.VISIBLE else View.GONE)
        v.setTextViewTextSize(R.id.qs_amount, TypedValue.COMPLEX_UNIT_SP, when (step) { 3 -> 28f; 2 -> 24f; else -> 22f })
        v.setViewVisibility(R.id.qs_send_text, if (step >= 1) View.VISIBLE else View.GONE)
        // Send starts a transfer after unlocking; the usual checks, review and approval still apply.
        v.setTextViewText(R.id.qs_send_text, if (d.signedIn) str(R.string.widget_send_2) else str(R.string.widget_open))
        v.setContentDescription(R.id.qs_send, if (d.signedIn) str(R.string.widget_send_money_2) else str(R.string.widget_open_arro))
        v.setOnClickPendingIntent(R.id.qs_send, open(context, if (d.signedIn) Routes.QUICK_SEND else null))
        val slotIds = listOf(
            listOf(R.id.qs_r1, R.id.qs_r1_initials, R.id.qs_r1_flag, R.id.qs_r1_name),
            listOf(R.id.qs_r2, R.id.qs_r2_initials, R.id.qs_r2_flag, R.id.qs_r2_name),
            listOf(R.id.qs_r3, R.id.qs_r3_initials, R.id.qs_r3_flag, R.id.qs_r3_name),
            listOf(R.id.qs_r4, R.id.qs_r4_initials, R.id.qs_r4_flag, R.id.qs_r4_name),
        )
        val people = if (d.signedIn) d.recents.take(slots) else emptyList()
        slotIds.forEachIndexed { i, (box, initials, flag, name) ->
            val r = people.getOrNull(i)
            v.setViewVisibility(box, if (r == null) View.GONE else View.VISIBLE)
            if (r == null) return@forEachIndexed
            val color = r.color.coerceIn(0, AVATAR_BG.lastIndex)
            v.setTextViewText(initials, r.initials)
            v.setInt(initials, "setBackgroundResource", AVATAR_BG[color])
            v.setTextColor(initials, AVATAR_FG[color])
            v.setTextViewText(name, r.firstName)
            v.setViewVisibility(name, if (step >= 1) View.VISIBLE else View.GONE)
            val flagBitmap = flagRes(r.flag).takeIf { it != 0 }?.let { roundFlag(context, it) }
            if (flagBitmap != null) v.setImageViewBitmap(flag, flagBitmap)
            v.setViewVisibility(flag, if (flagBitmap != null) View.VISIBLE else View.GONE)
            v.setContentDescription(box, str(R.string.widget_send_to_x, r.firstName))
            v.setOnClickPendingIntent(box, open(context, Routes.quickSend(r.id)))
        }
        val hint = when {
            !d.signedIn -> str(R.string.widget_log_in_to_arro_to_see)
            !d.showPrivate -> context.getString(R.string.widget_recents_hidden)
            people.isEmpty() -> str(R.string.widget_people_you_send_to_will_appear)
            else -> null
        }
        v.setViewVisibility(R.id.qs_hint, if (hint == null) View.GONE else View.VISIBLE)
        v.setTextViewText(R.id.qs_hint, hint ?: "")
        v.setTextViewText(R.id.qs_label, when {
            !d.signedIn -> str(R.string.widget_arro)
            d.frozen -> str(R.string.widget_available_balance_account_frozen)
            else -> str(R.string.widget_available_balance_2)
        })
        v.setTextViewText(R.id.qs_amount, if (d.signedIn) balanceText(d) else str(R.string.widget_log_in))
        v.setContentDescription(R.id.qs_amount, if (d.signedIn) balanceDescription(d) else str(R.string.widget_logged_out))
        return v
    }

    /** Avatar fills and text colours, the same as the app's (index 0 is the dark brand avatar). */
    private val AVATAR_BG = listOf(R.drawable.widget_avatar_0, R.drawable.widget_avatar_1, R.drawable.widget_avatar_2, R.drawable.widget_avatar_3,
        R.drawable.widget_avatar_4, R.drawable.widget_avatar_5, R.drawable.widget_avatar_6)
    private val AVATAR_FG = listOf(0xFFB0FE78, 0xFF1F5130, 0xFF7A3A0C, 0xFF134F86, 0xFF4E2C86, 0xFF8A1E33, 0xFF2F5748).map { it.toInt() }

    private val flagCache = HashMap<Int, Bitmap>()

    /** A country flag cut into a circle, as the app shows it on an avatar. Cached: the same few flags repeat. */
    private fun roundFlag(context: Context, res: Int): Bitmap? = synchronized(flagCache) {
        flagCache[res]?.let { return@synchronized it }
        val src = BitmapFactory.decodeResource(context.resources, res) ?: return@synchronized null
        val px = (16 * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = px.toFloat() / minOf(src.width, src.height)
        shader.setLocalMatrix(Matrix().apply {
            setScale(scale, scale)
            postTranslate((px - src.width * scale) / 2f, (px - src.height * scale) / 2f)
        })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        Canvas(out).drawCircle(px / 2f, px / 2f, px / 2f, paint)
        flagCache[res] = out
        out
    }

    /**
     * Widget B at [size]. Short widgets drop the button (the whole widget opens the rate), then the unit line; the
     * status gets a second line only on tall widgets, and 2 x 2 widgets use the shorter wording that fits one line.
     */
    private fun rateViews(context: Context, d: WidgetData, size: SizeF, refreshing: Boolean): RemoteViews {
        val wide = size.width >= WIDE_DP
        val tiny = size.width < 165f
        val lines = if (size.height >= 178f) 2 else 1
        val short = !wide && lines == 1
        val v = RemoteViews(context.packageName, if (wide) R.layout.widget_rate_wide else R.layout.widget_rate)
        // Narrow widgets name only the currency; the narrowest also drop refresh (rates refresh on their own).
        v.setViewVisibility(R.id.rate_refresh, if (tiny) View.GONE else View.VISIBLE)
        fun label(full: Int, narrow: Int, tinyText: Int) = str(if (wide) full else if (tiny) tinyText else narrow)
        v.setViewVisibility(if (wide) R.id.rate_bottom else R.id.rate_button, if (size.height >= 164f) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.rate_unit, if (size.height >= 124f) View.VISIBLE else View.GONE)
        v.setInt(R.id.rate_status, "setMaxLines", lines)
        v.setInt(R.id.rate_status_warn, "setMaxLines", lines)
        val cur = d.rateCur
        val now = System.currentTimeMillis()
        val sample = 25_000L
        v.setTextViewText(R.id.rate_pair, if (wide) str(R.string.widget_gbp_x, cur.code) else cur.code)
        v.setOnClickPendingIntent(android.R.id.background, open(context, if (d.signedIn) Routes.rate(cur.code) else null))
        v.setOnClickPendingIntent(R.id.rate_refresh, refreshIntent(context))
        var status: String
        var warn: String? = null
        var extra = ""
        when {
            d.lock != null -> {
                val l = d.lock
                badge(v, str(R.string.widget_locked), R.drawable.widget_pill_locked)
                v.setTextViewText(R.id.rate_value, Money.rate(l.rate, Money.rateDecimals(l.rate)))
                v.setTextViewText(R.id.rate_unit, if (tiny) str(R.string.widget_x_for_1, cur.code) else str(R.string.widget_x_for_1_locked_rate, cur.code))
                status = if (short) str(R.string.widget_held_until_x, Text.time(l.expiresAt))
                    else str(R.string.widget_held_for_your_next_transfer_until, Text.time(l.expiresAt))
                extra = str(R.string.widget_n_250_sends_x, Money.format(Money.convert(sample, Currency.GBP, cur, l.rate), cur))
                button(context, v, label(R.string.widget_send_at_this_rate, R.string.widget_send_2, R.string.widget_send_2), R.drawable.ic_arrow_up_right,
                    Catalog.countryForCurrency(cur)?.let { Routes.quickSendTo(it.code) } ?: Routes.lockRate(cur.code))
            }
            d.offer != null -> {
                val o = d.offer
                badge(v, str(R.string.widget_offer), R.drawable.widget_pill_offer)
                v.setTextViewText(R.id.rate_value, Money.rate(o.rate, Money.rateDecimals(o.rate)))
                v.setTextViewText(R.id.rate_unit, when {
                    wide -> str(R.string.widget_x_for_1_offer_not_locked, cur.code)
                    tiny -> str(R.string.widget_x_for_1, cur.code)
                    else -> str(R.string.widget_x_for_1_offer, cur.code)
                })
                status = if (short) str(R.string.widget_offer_ends_x, Text.time(o.expiresAt))
                    else str(R.string.widget_offer_ends_x_usually_x, Text.time(o.expiresAt), Money.rate(o.standardRate, Money.rateDecimals(o.standardRate)))
                extra = str(R.string.widget_lock_it_for_x_minutes, o.lockMinutes)
                button(context, v, label(R.string.widget_lock_this_rate, R.string.widget_lock_rate_short, R.string.widget_lock), R.drawable.ic_lock, Routes.lockRate(cur.code))
            }
            d.liveRate == null -> {
                badge(v, null, 0)
                v.setTextViewText(R.id.rate_value, "—")
                v.setTextViewText(R.id.rate_unit, str(R.string.widget_x_for_1, cur.code))
                status = ""
                warn = when {
                    refreshing -> str(R.string.widget_updating)
                    !d.online -> if (short) str(R.string.widget_you_re_offline) else str(R.string.widget_you_re_offline_rates_appear_once)
                    else -> if (short) str(R.string.widget_rate_unavailable) else str(R.string.widget_rate_unavailable_tap_refresh_to_try)
                }
                button(context, v, label(R.string.widget_open_arro, R.string.widget_open_arro, R.string.widget_open), R.drawable.ic_arrow_up_right, null)
            }
            else -> {
                val rate = d.liveRate
                badge(v, str(R.string.widget_live), R.drawable.widget_pill_live)
                v.setTextViewText(R.id.rate_value, Money.rate(rate, Money.rateDecimals(rate)))
                v.setTextViewText(R.id.rate_unit, if (tiny) str(R.string.widget_x_for_1, cur.code) else str(R.string.widget_x_for_1_today_s_rate, cur.code))
                val stale = now - d.ratesAt > STALE_MS
                status = if (short) str(R.string.widget_updated_x, Text.time(d.ratesAt))
                    else str(R.string.widget_updated_x_not_guaranteed_until_locked, Text.time(d.ratesAt)) + if (d.demoMoved) str(R.string.widget_demo_market_move) else ""
                val day = Text.relativeDay(d.ratesAt, now).lowercase()
                when {
                    refreshing -> { warn = str(R.string.widget_updating); status = "" }
                    stale && !d.online -> { warn = if (short) str(R.string.widget_offline_from_x, day) else str(R.string.widget_offline_rate_from_x, day); status = "" }
                    stale -> { warn = if (short) str(R.string.widget_updated_x, day) else str(R.string.widget_updated_x_may_be_out_of, day); status = "" }
                }
                extra = str(R.string.widget_n_250_sends_about_x, Money.format(Money.convert(sample, Currency.GBP, cur, rate), cur))
                if (d.signedIn) button(context, v, label(R.string.widget_lock_a_rate, R.string.widget_lock_rate_short, R.string.widget_lock), R.drawable.ic_lock, Routes.lockRate(cur.code))
                else button(context, v, label(R.string.widget_open_arro, R.string.widget_open_arro, R.string.widget_open), R.drawable.ic_arrow_up_right, null)
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

/** Widget A: GBP balance (if allowed) and a Send button. */
class BalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = Widgets.update(context)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        Widgets.update(context)
}

/** Widget C: Quick Send. Available balance (if allowed) and one tap to send again to the people you send to most. */
class QuickSendWidget : AppWidgetProvider() {
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
