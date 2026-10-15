package app.arro.core

import java.time.LocalDate
import java.time.YearMonth

/** Input rules. Each check returns null when the value is fine, or a sentence that says how to fix it. */
object Validation {
    private val emailRe = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val postcodeRe = Regex("^[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}$")
    private val upiRe = Regex("^[A-Za-z0-9._-]{2,64}@[A-Za-z]{2,64}$")

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email address."
        !emailRe.matches(value.trim()) -> "Enter a full email address, like name@mail.com."
        else -> null
    }

    data class PasswordRules(val length: Boolean, val cases: Boolean, val number: Boolean, val symbol: Boolean) {
        val required: Boolean get() = length && cases && number
        val score: Int get() = listOf(length, cases, number, symbol).count { it }
        val label: String get() = when {
            !length -> "Too short"
            score <= 2 -> "Weak"
            score == 3 -> "Good"
            else -> "Strong"
        }
    }

    fun password(value: String) = PasswordRules(
        length = value.length >= 8,
        cases = value.any { it.isUpperCase() } && value.any { it.isLowerCase() },
        number = value.any { it.isDigit() },
        symbol = value.any { !it.isLetterOrDigit() },
    )

    /** UK mobile: 10 digits starting with 7 (leading 0 allowed). */
    fun ukMobile(digits: String): String? {
        val d = digits.filter { it.isDigit() }.removePrefix("0")
        return when {
            d.isEmpty() -> "Enter your mobile number."
            !d.startsWith("7") -> "UK mobile numbers start with 07."
            d.length != 10 -> "UK mobile numbers have 10 digits after +44. This one has ${d.length}."
            else -> null
        }
    }

    fun normaliseUkMobile(digits: String) = "+44" + digits.filter { it.isDigit() }.removePrefix("0")

    /** National digits for a destination mobile number, without the country code or leading 0. */
    fun nationalDigits(input: String, country: Country): String {
        var d = input.filter { it.isDigit() }
        val cc = country.dialCode.removePrefix("+")
        if (d.startsWith(cc) && d.length > country.mobileDigits) d = d.removePrefix(cc)
        return d.removePrefix("0")
    }

    fun mobile(input: String, country: Country): String? {
        val d = nationalDigits(input, country)
        return when {
            d.isEmpty() -> "Enter their mobile money number."
            d.length != country.mobileDigits ->
                "${country.adjective} numbers have ${country.mobileDigits} digits after ${country.dialCode}. This one has ${d.length}."
            else -> null
        }
    }

    /** A soft hint when the number looks like it belongs to a different network. */
    fun networkHint(input: String, country: Country, provider: Provider?): String? {
        if (provider == null || provider.prefixes.isEmpty()) return null
        val d = nationalDigits(input, country)
        if (d.length < 2) return null
        if (provider.prefixes.any { d.startsWith(it) }) return null
        val other = Catalog.providers.firstOrNull { it.country == country.code && it.type == provider.type && it.id != provider.id && it.prefixes.any { p -> d.startsWith(p) } }
        return other?.let { "This looks like an ${it.name} number. Check the provider." }
    }

    fun fullName(value: String, provider: String = ""): String? {
        val parts = value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "Enter their full name."
            parts.size < 2 -> "Add their last name too." + if (provider.isNotEmpty()) " It must match their $provider account." else ""
            parts.any { p -> p.any { it.isDigit() } } -> "Names can't contain numbers."
            else -> null
        }
    }

    fun personName(value: String, what: String): String? = when {
        value.isBlank() -> "Enter your $what."
        value.any { it.isDigit() } -> "Names can't contain numbers."
        value.trim().length < 2 -> "Enter your full $what, as it appears on your ID."
        else -> null
    }

    /** yyyy-MM-dd from day, month and year fields; the person must be 18 or over. */
    fun dob(day: String, month: String, year: String, today: LocalDate): Pair<String?, String?> {
        val d = day.toIntOrNull(); val m = month.toIntOrNull(); val y = year.toIntOrNull()
        if (d == null || m == null || y == null || year.length != 4) return null to "Enter your date of birth, like 03 07 1991."
        val date = runCatching { LocalDate.of(y, m, d) }.getOrNull() ?: return null to "That date doesn't exist. Check the day and month."
        if (date.isAfter(today.minusYears(18))) return null to "You need to be 18 or over to use Arro."
        if (date.isBefore(today.minusYears(110))) return null to "Check the year of birth."
        return date.toString() to null
    }

    fun normalisePostcode(value: String): String {
        val v = value.uppercase().filter { it.isLetterOrDigit() }
        return if (v.length > 3) v.dropLast(3) + " " + v.takeLast(3) else v
    }

    fun isUkPostcode(value: String) = postcodeRe.matches(normalisePostcode(value))

    fun postcode(value: String): String? = when {
        value.isBlank() -> "Enter your postcode."
        !isUkPostcode(value) -> "Enter a UK postcode, like E8 2PL."
        else -> null
    }

    // ---------- cards ----------
    fun luhn(number: String): Boolean {
        val d = number.filter { it.isDigit() }
        if (d.length < 12) return false
        var sum = 0
        d.reversed().forEachIndexed { i, c ->
            var n = c - '0'
            if (i % 2 == 1) { n *= 2; if (n > 9) n -= 9 }
            sum += n
        }
        return sum % 10 == 0
    }

    fun brand(number: String): CardBrand {
        val d = number.filter { it.isDigit() }
        return when {
            d.startsWith("4") -> CardBrand.VISA
            d.length >= 2 && d.take(2).toInt() in 51..55 -> CardBrand.MASTERCARD
            d.length >= 4 && d.take(4).toInt() in 2221..2720 -> CardBrand.MASTERCARD
            d.startsWith("34") || d.startsWith("37") -> CardBrand.AMEX
            else -> CardBrand.UNKNOWN
        }
    }

    fun cardLength(brand: CardBrand) = if (brand == CardBrand.AMEX) 15 else 16

    /** Groups as 4-4-4-4 (or 4-6-5 for Amex). */
    fun formatCard(number: String): String {
        val d = number.filter { it.isDigit() }
        return if (brand(d) == CardBrand.AMEX) listOf(d.take(4), d.drop(4).take(6), d.drop(10).take(5)).filter { it.isNotEmpty() }.joinToString(" ")
        else d.chunked(4).joinToString(" ")
    }

    fun cardNumber(number: String): String? {
        val d = number.filter { it.isDigit() }
        val b = brand(d)
        return when {
            d.isEmpty() -> "Enter the long number on the front of your card."
            b == CardBrand.UNKNOWN -> "We accept Visa and Mastercard."
            b == CardBrand.AMEX -> "We can't accept American Express yet. Use a Visa or Mastercard."
            d.length != cardLength(b) -> "Card numbers have ${cardLength(b)} digits. This one has ${d.length}."
            !luhn(d) -> "That card number isn't valid. Check each digit."
            else -> null
        }
    }

    /** MM/YY, must not be in the past. */
    fun expiry(value: String, today: LocalDate): String? {
        val d = value.filter { it.isDigit() }
        if (d.length != 4) return "Enter the expiry date as MM/YY."
        val m = d.take(2).toInt(); val y = 2000 + d.drop(2).toInt()
        if (m !in 1..12) return "The month must be between 01 and 12."
        val ym = YearMonth.of(y, m)
        if (ym.isBefore(YearMonth.from(today))) return "This card has expired."
        if (ym.isAfter(YearMonth.from(today).plusYears(20))) return "Check the expiry year."
        return null
    }

    fun cvv(value: String, brand: CardBrand): String? {
        val need = if (brand == CardBrand.AMEX) 4 else 3
        return if (value.filter { it.isDigit() }.length != need) "Enter the $need-digit code on the back of your card." else null
    }

    // ---------- bank accounts ----------
    fun bankAccount(value: String, bank: Bank?): String? {
        val d = value.filter { it.isDigit() }
        val range = bank?.accountDigits ?: 8..18
        return when {
            d.isEmpty() -> "Enter the account number."
            d.length !in range -> if (range.first == range.last) "${bank?.name ?: "This bank"} account numbers have ${range.first} digits. This one has ${d.length}."
            else "Account numbers at ${bank?.name ?: "this bank"} have ${range.first} to ${range.last} digits."
            else -> null
        }
    }

    fun upi(value: String): String? = if (upiRe.matches(value.trim())) null else "Enter a UPI ID, like name@okbank."

    fun passcodeWeak(code: String): Boolean {
        if (code.length != 6) return true
        if (code.toSet().size == 1) return true
        val asc = "0123456789012345"; val desc = "9876543210987654"
        return asc.contains(code) || desc.contains(code)
    }
}
