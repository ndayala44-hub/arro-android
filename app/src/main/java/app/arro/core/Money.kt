package app.arro.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Currencies Arro holds or pays out in. [decimals] is the number of minor-unit digits shown. */
enum class Currency(
    val code: String,
    val displayName: String,
    val symbol: String?,
    val decimals: Int,
    val flag: String,
) {
    GBP("GBP", "British pound", "£", 2, "gb"),
    EUR("EUR", "Euro", "€", 2, "eu"),
    USD("USD", "US dollar", "$", 2, "us"),
    RWF("RWF", "Rwandan franc", null, 0, "rw"),
    KES("KES", "Kenyan shilling", null, 2, "ke"),
    UGX("UGX", "Ugandan shilling", null, 0, "ug"),
    NGN("NGN", "Nigerian naira", null, 2, "ng"),
    GHS("GHS", "Ghanaian cedi", null, 2, "gh"),
    INR("INR", "Indian rupee", null, 2, "in"),
    PHP("PHP", "Philippine peso", null, 2, "ph");

    val factor: Long get() = pow10(decimals)

    companion object {
        val wallets = listOf(GBP, EUR, USD)
        fun of(code: String): Currency = entries.firstOrNull { it.code == code } ?: GBP
        private fun pow10(n: Int): Long { var r = 1L; repeat(n) { r *= 10 }; return r }
    }
}

object Money {
    private val symbols = DecimalFormatSymbols(Locale.UK)

    private fun pattern(decimals: Int) = DecimalFormat(if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals), symbols)

    /** Plain number with grouping, e.g. 490,600 or 2,486.35. */
    fun number(minor: Long, cur: Currency, forceDecimals: Boolean = true): String {
        val v = BigDecimal.valueOf(minor).movePointLeft(cur.decimals)
        val d = if (forceDecimals) cur.decimals else if (minor % cur.factor == 0L) 0 else cur.decimals
        return pattern(d).format(v.setScale(d, RoundingMode.HALF_UP))
    }

    /** £2,486.35, €412.80, RWF 490,600, KES 25,875.00. Negative amounts get a leading minus. */
    fun format(minor: Long, cur: Currency, forceDecimals: Boolean = true): String {
        val sign = if (minor < 0) "−" else ""
        val n = number(kotlin.math.abs(minor), cur, forceDecimals)
        return if (cur.symbol != null) "$sign${cur.symbol}$n" else "$sign${cur.code} $n"
    }

    /** +£500.00 style for credits. */
    fun signed(minor: Long, cur: Currency): String = (if (minor > 0) "+" else "") + format(minor, cur)

    /** Splits £2,955.62 into "£2,955" and ".62" for the large balance style. */
    fun split(minor: Long, cur: Currency): Pair<String, String> {
        val full = format(minor, cur)
        val dot = full.lastIndexOf('.')
        return if (cur.decimals == 0 || dot < 0) full to "" else full.substring(0, dot) to full.substring(dot)
    }

    fun rate(value: Double, decimals: Int = 2): String = DecimalFormat("#,##0." + "0".repeat(decimals), symbols).format(value)

    /** Rate decimals: small numbers (EUR, USD) need four places to be meaningful. */
    /** Whole numbers with thousands separators, for points. */
    fun points(n: Int): String = pattern(0).format(n)

    fun rateDecimals(value: Double): Int = if (value < 10) 4 else 2

    fun rateLine(from: Currency, to: Currency, value: Double): String = "1 ${from.code} = ${rate(value, rateDecimals(value))} ${to.code}"

    fun percent(value: Double, decimals: Int = 1): String = DecimalFormat("0." + "0".repeat(decimals), symbols).format(value) + "%"

    /** Parse a user-typed amount ("1,250.5") into minor units; null when not a number. */
    fun parse(text: String, cur: Currency): Long? {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty() || clean == ".") return null
        return try {
            BigDecimal(clean).movePointRight(cur.decimals).setScale(0, RoundingMode.DOWN).longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    fun toMajor(minor: Long, cur: Currency): Double = minor.toDouble() / cur.factor

    fun fromMajor(major: Double, cur: Currency, mode: RoundingMode = RoundingMode.HALF_UP): Long =
        BigDecimal.valueOf(major).movePointRight(cur.decimals).setScale(0, mode).toLong()

    /** Converts [minor] of [from] into [to] at [rate] (units of [to] per one unit of [from]), rounding down. */
    fun convert(minor: Long, from: Currency, to: Currency, rate: Double): Long {
        val major = BigDecimal.valueOf(minor).movePointLeft(from.decimals)
        return major.multiply(BigDecimal.valueOf(rate)).movePointRight(to.decimals).setScale(0, RoundingMode.DOWN).toLong()
    }
}
