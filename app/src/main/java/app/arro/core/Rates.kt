package app.arro.core

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Source of live mid-market rates (per 1 GBP). Returns null when offline or the service fails. */
fun interface RateSource {
    suspend fun fetch(): Map<String, Double>?
}

/** Prices currency exchange from mid-market rates, adds Arro's margin and builds chart history. */
object Rates {
    /** Arro's margin on transfers and on balance conversions. */
    const val TRANSFER_MARGIN = 0.0029
    const val CONVERSION_MARGIN = 0.0027
    /** Margin on a rate offer: a smaller markup than usual, for a short time. */
    const val OFFER_MARGIN = 0.0009

    /** Mid-market rates used until live rates arrive (units per 1 GBP). */
    val fallbackMid: Map<String, Double> = mapOf(
        "GBP" to 1.0, "EUR" to 1.1561, "USD" to 1.3369, "RWF" to 1968.11, "KES" to 173.00, "UGX" to 4800.00,
        "NGN" to 2011.83, "GHS" to 15.2542, "INR" to 117.6913, "PHP" to 77.0635,
    )

    val payoutCurrencies = listOf(Currency.RWF, Currency.KES, Currency.UGX, Currency.NGN, Currency.GHS, Currency.PHP, Currency.INR)

    fun mid(snapshot: RatesSnapshot, cur: Currency): Double =
        snapshot.mid[cur.code]?.takeIf { it > 0 } ?: fallbackMid[cur.code] ?: 1.0

    /** Mid-market rate from [from] to [to]. */
    fun cross(snapshot: RatesSnapshot, from: Currency, to: Currency): Double =
        if (from == to) 1.0 else mid(snapshot, to) / mid(snapshot, from)

    /** The rate a customer gets, with the margin taken out. */
    fun customer(snapshot: RatesSnapshot, from: Currency, to: Currency, margin: Double = TRANSFER_MARGIN): Double =
        if (from == to) 1.0 else round(cross(snapshot, from, to) * (1 - margin), to)

    /** The rate a rate offer gives: the live mid-market rate with the smaller offer margin. */
    fun offer(snapshot: RatesSnapshot, from: Currency, to: Currency): Double = customer(snapshot, from, to, OFFER_MARGIN)

    /** Rates are quoted to 2 decimals for large numbers and 4 for small ones. */
    fun round(v: Double, to: Currency? = null): Double {
        val places = if (v < 10) 4 else 2
        val f = if (places == 4) 10_000.0 else 100.0
        return Math.round(v * f) / f
    }

    /** 24-hour change in percent for GBP -> [to]. */
    fun change24h(snapshot: RatesSnapshot, to: Currency, now: Long): Double {
        val cur = mid(snapshot, to)
        val prev = snapshot.previous[to.code]
        if (prev != null && prev > 0 && abs(prev - cur) / cur < 0.2) return (cur - prev) / prev * 100
        val series = history(snapshot, to, 2, now, hourly = true)
        val old = series.first()
        return (cur - old) / old * 100
    }

    /**
     * Deterministic price history ending at today's rate. Daily points by default, hourly when [hourly] is set.
     * The walk is seeded per currency so charts stay stable between refreshes.
     */
    fun history(snapshot: RatesSnapshot, to: Currency, points: Int, now: Long, hourly: Boolean = false): List<Double> {
        val end = mid(snapshot, to)
        val stepMs = if (hourly) 3_600_000L else 86_400_000L
        val bucket = now / stepMs
        val vol = if (hourly) 0.0009 else 0.0042
        val out = DoubleArray(points)
        var logLevel = 0.0
        for (i in points - 1 downTo 0) {
            out[i] = end * exp(logLevel)
            val t = bucket - (points - 1 - i)
            val rnd = Random(to.code.hashCode() * 31L + t)
            val shock = (rnd.nextDouble() - 0.5) * 2 * vol
            val wave = sin(t / (if (hourly) 9.0 else 23.0)) * vol * 0.35
            logLevel -= shock + wave
        }
        return out.toList()
    }

    fun change(series: List<Double>): Double = if (series.size < 2) 0.0 else (series.last() - series.first()) / series.first() * 100
}
