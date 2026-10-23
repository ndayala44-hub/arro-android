package app.arro.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Text helpers shared by the backend and the UI: names, phone numbers, dates and times. */
object Text {
    var zone: ZoneId = ZoneId.systemDefault()

    fun initials(name: String): String {
        val parts = name.trim().split(Regex("[\\s-]+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    /** "claudine uwase" -> "Claudine Uwase"; keeps hyphenated names tidy. */
    fun titleCase(name: String): String = name.trim().split(Regex("\\s+")).joinToString(" ") { w ->
        w.split("-").joinToString("-") { p -> p.lowercase().replaceFirstChar { it.uppercase() } }
    }

    /** Splits an E.164 number into dial code and national digits using the countries Arro knows. */
    fun splitPhone(e164: String): Pair<String, String> {
        val digits = e164.filter { it.isDigit() || it == '+' }
        val dials = Catalog.countries.map { it.dialCode } + listOf("+44")
        val dial = dials.sortedByDescending { it.length }.firstOrNull { digits.startsWith(it) } ?: return "" to digits.trimStart('+')
        return dial to digits.removePrefix(dial)
    }

    /** +250 788 214 412, +44 7700 900412, +254 712 345 908. */
    fun formatPhone(e164: String): String {
        val (dial, n) = splitPhone(e164)
        if (dial.isEmpty()) return e164
        val grouped = when {
            dial == "+44" && n.length == 10 -> n.substring(0, 4) + " " + n.substring(4)
            n.length == 9 -> n.substring(0, 3) + " " + n.substring(3, 6) + " " + n.substring(6)
            n.length == 10 -> n.substring(0, 3) + " " + n.substring(3, 6) + " " + n.substring(6)
            else -> n
        }
        return "$dial $grouped"
    }

    /** +250 788 ••• 412 */
    fun maskPhone(e164: String): String {
        val (dial, n) = splitPhone(e164)
        if (dial.isEmpty() || n.length < 7) return e164
        return "$dial ${n.take(3)} ••• ${n.takeLast(3)}"
    }

    /** +44 •••• 900412 style used for SMS confirmations. */
    fun maskPhoneShort(e164: String): String {
        val (dial, n) = splitPhone(e164)
        return "$dial •••• ${n.takeLast(6)}"
    }

    /** e•••••@mail.com */
    fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return email
        return email.take(1) + "•".repeat((at - 1).coerceIn(3, 6)) + email.substring(at)
    }

    // ---------- time ----------
    private fun dt(ms: Long): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(zone)
    private val hm = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    private val dM = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val dMy = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK)
    private val month = DateTimeFormatter.ofPattern("MMMM", Locale.UK)
    private val dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)

    fun time(ms: Long): String = dt(ms).format(hm)
    fun dayMonth(ms: Long): String = dt(ms).format(dM)
    fun date(ms: Long): String = dt(ms).format(dMy)
    fun dateTime(ms: Long): String = dt(ms).format(full)
    fun monthYear(ms: Long): String = dt(ms).format(monthYear)
    fun monthName(ms: Long): String = dt(ms).format(month)
    fun numericDate(ms: Long): String = dt(ms).format(dmy)
    fun zoneName(ms: Long): String = dt(ms).format(DateTimeFormatter.ofPattern("zzz", Locale.UK))

    fun isSameDay(a: Long, b: Long): Boolean = dt(a).toLocalDate() == dt(b).toLocalDate()
    fun isYesterday(a: Long, now: Long): Boolean = dt(a).toLocalDate() == dt(now).toLocalDate().minusDays(1)
    fun localDate(ms: Long): LocalDate = dt(ms).toLocalDate()
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun startOfMonth(ms: Long): Long = startOfDay(localDate(ms).withDayOfMonth(1))

    /** "14:32" today, "12 Oct" this year, "12 Oct 2025" otherwise. */
    fun when_(ms: Long, now: Long): String = when {
        isSameDay(ms, now) -> time(ms)
        dt(ms).year == dt(now).year -> dayMonth(ms)
        else -> date(ms)
    }

    /** "Today, 14:32", "Yesterday, 09:10", "1 Oct". */
    fun relativeDay(ms: Long, now: Long): String = when {
        isSameDay(ms, now) -> "Today, ${time(ms)}"
        isYesterday(ms, now) -> "Yesterday, ${time(ms)}"
        else -> when_(ms, now)
    }

    /** "by 14:45 today", "by 10:00 tomorrow", "by 21 Oct". */
    fun by(ms: Long, now: Long): String = when {
        isSameDay(ms, now) -> "by ${time(ms)} today"
        localDate(ms) == localDate(now).plusDays(1) -> "by ${time(ms)} tomorrow"
        else -> "by ${dayMonth(ms)}"
    }

    /** "about 8 min", "about 2 hours", "1 business day". */
    fun remaining(ms: Long, now: Long): String {
        val s = ((ms - now) / 1000).coerceAtLeast(0)
        return when {
            s < 60 -> "under a minute"
            s < 3600 -> "about ${(s + 59) / 60} min"
            s < 86_400 -> "about ${(s + 1799) / 3600} hours"
            else -> "about ${(s + 43_199) / 86_400} days"
        }
    }

    /** "6 min", "1 h 5 min", "45 s". */
    fun duration(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return when {
            s < 60 -> "$s s"
            s < 3600 -> "${(s + 30) / 60} min"
            s < 86_400 -> "${s / 3600} h ${(s % 3600) / 60} min"
            else -> "${s / 86_400} days"
        }
    }

    /** mm:ss countdown. */
    fun countdown(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun greeting(now: Long): String = when (dt(now).hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    /** "3 Jul 1991" from yyyy-MM-dd. */
    fun isoToDisplay(iso: String): String = runCatching { LocalDate.parse(iso).format(dMy) }.getOrDefault(iso)
}
