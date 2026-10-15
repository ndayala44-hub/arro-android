package app.arro.core

import kotlinx.serialization.Serializable

@Serializable
enum class PayoutType(val label: String, val short: String) {
    MOBILE_MONEY("Mobile money", "mobile money"),
    BANK("Bank deposit", "bank deposit"),
    CASH("Cash pickup", "cash pickup"),
    EWALLET("E-wallet", "e-wallet"),
    UPI("UPI", "UPI"),
    ARRO("Arro user", "Arro");
}

data class Provider(
    val id: String,
    val name: String,
    val country: String,
    val type: PayoutType,
    /** Leading digits (after the country code) that identify this network, for gentle hints. */
    val prefixes: List<String> = emptyList(),
)

data class DeliveryOption(
    val type: PayoutType,
    val feeMinor: Long,
    val speed: String,
    val description: String,
    val badge: String? = null,
)

data class Country(
    val code: String,
    val name: String,
    val currency: Currency,
    val dialCode: String,
    /** Digits expected after the dial code for a mobile number. */
    val mobileDigits: Int,
    val adjective: String,
    val options: List<DeliveryOption>,
    val city: String,
    /** Short list of ways to receive, as shown under the country name. */
    val methodsSummary: String,
) {
    fun option(type: PayoutType): DeliveryOption? = options.firstOrNull { it.type == type }
}

data class Bank(val id: String, val name: String, val country: String, val accountDigits: IntRange, val note: String = "")

data class HelpArticle(
    val id: String,
    val category: String,
    val title: String,
    val updated: String,
    val minutes: Int,
    val intro: String,
    val facts: List<Pair<String, String>> = emptyList(),
    val sectionTitle: String = "",
    val bullets: List<String> = emptyList(),
    val body: String = "",
)

object Catalog {
    // ---------- destinations ----------
    val countries: List<Country> = listOf(
        Country("RW", "Rwanda", Currency.RWF, "+250", 9, "Rwandan", listOf(
            DeliveryOption(PayoutType.MOBILE_MONEY, 199, "Minutes", "MTN MoMo or Airtel Money wallet", "Fastest"),
            DeliveryOption(PayoutType.BANK, 99, "1 business day", "Bank of Kigali, Equity, I&M and other Rwandan banks", "Lowest fee"),
            DeliveryOption(PayoutType.CASH, 249, "Within 1 hour", "Collected with ID at a partner agent in Kigali and other cities"),
        ), "Kigali", "Mobile money, bank, cash"),
        Country("KE", "Kenya", Currency.KES, "+254", 9, "Kenyan", listOf(
            DeliveryOption(PayoutType.MOBILE_MONEY, 149, "Minutes", "M-Pesa or Airtel Money wallet", "Fastest"),
            DeliveryOption(PayoutType.BANK, 99, "1 business day", "Equity, KCB, Co-op and other Kenyan banks", "Lowest fee"),
        ), "Nairobi", "M-Pesa, bank"),
        Country("UG", "Uganda", Currency.UGX, "+256", 9, "Ugandan", listOf(
            DeliveryOption(PayoutType.MOBILE_MONEY, 99, "Minutes", "MTN Mobile Money or Airtel Money", "Fastest"),
            DeliveryOption(PayoutType.BANK, 99, "1 business day", "Stanbic, Centenary and other Ugandan banks", "Lowest fee"),
        ), "Kampala", "Mobile money, bank"),
        Country("GH", "Ghana", Currency.GHS, "+233", 9, "Ghanaian", listOf(
            DeliveryOption(PayoutType.MOBILE_MONEY, 199, "Minutes", "MTN MoMo or Telecel Cash wallet", "Fastest"),
            DeliveryOption(PayoutType.BANK, 99, "1 business day", "GCB, Ecobank and other Ghanaian banks", "Lowest fee"),
            DeliveryOption(PayoutType.CASH, 249, "Within 1 hour", "Collected with ID at a partner agent"),
        ), "Accra", "Mobile money, bank, cash"),
        Country("IN", "India", Currency.INR, "+91", 10, "Indian", listOf(
            DeliveryOption(PayoutType.BANK, 99, "Same day", "Any Indian bank account by IFSC", "Lowest fee"),
            DeliveryOption(PayoutType.UPI, 99, "Minutes", "Any UPI ID, like name@bank", "Fastest"),
        ), "Mumbai", "Bank, UPI"),
        Country("NG", "Nigeria", Currency.NGN, "+234", 10, "Nigerian", listOf(
            DeliveryOption(PayoutType.BANK, 99, "Minutes", "GTBank, Access, Zenith and other Nigerian banks", "Fastest"),
            DeliveryOption(PayoutType.CASH, 249, "Within 1 hour", "Collected with ID at a partner agent"),
        ), "Lagos", "Bank, cash"),
        Country("PH", "Philippines", Currency.PHP, "+63", 10, "Filipino", listOf(
            DeliveryOption(PayoutType.EWALLET, 149, "Minutes", "GCash or Maya wallet", "Fastest"),
            DeliveryOption(PayoutType.BANK, 99, "1 business day", "BDO, BPI and other Philippine banks", "Lowest fee"),
            DeliveryOption(PayoutType.CASH, 249, "Within 1 hour", "Collected with ID at a partner agent"),
        ), "Manila", "E-wallet, bank, cash"),
    )

    /** Countries people search for that Arro doesn't serve yet (for the "no results" state). */
    val unsupported = listOf(
        "Bhutan", "Burundi", "Tanzania", "DR Congo", "Ethiopia", "Zambia", "Malawi", "Cameroon", "Senegal", "Pakistan",
        "Bangladesh", "Nepal", "Sri Lanka", "Vietnam", "Mexico", "Brazil", "Jamaica", "South Africa", "Zimbabwe", "Somalia",
    )

    fun country(code: String): Country = countries.firstOrNull { it.code == code } ?: countries.first()
    fun countryOrNull(code: String?): Country? = countries.firstOrNull { it.code == code }
    fun countryForCurrency(cur: Currency): Country? = countries.firstOrNull { it.currency == cur }

    // ---------- payout providers ----------
    val providers: List<Provider> = listOf(
        Provider("mtn_rw", "MTN MoMo", "RW", PayoutType.MOBILE_MONEY, listOf("78", "79")),
        Provider("airtel_rw", "Airtel Money", "RW", PayoutType.MOBILE_MONEY, listOf("72", "73")),
        Provider("mpesa_ke", "M-Pesa", "KE", PayoutType.MOBILE_MONEY, listOf("70", "71", "72", "74", "79", "11")),
        Provider("airtel_ke", "Airtel Money", "KE", PayoutType.MOBILE_MONEY, listOf("73", "75", "78", "10")),
        Provider("mtn_ug", "MTN Mobile Money", "UG", PayoutType.MOBILE_MONEY, listOf("77", "78", "76")),
        Provider("airtel_ug", "Airtel Money", "UG", PayoutType.MOBILE_MONEY, listOf("70", "75", "74")),
        Provider("mtn_gh", "MTN MoMo", "GH", PayoutType.MOBILE_MONEY, listOf("24", "54", "55", "59")),
        Provider("telecel_gh", "Telecel Cash", "GH", PayoutType.MOBILE_MONEY, listOf("20", "50")),
        Provider("gcash_ph", "GCash", "PH", PayoutType.EWALLET),
        Provider("maya_ph", "Maya", "PH", PayoutType.EWALLET),
        Provider("upi_in", "UPI", "IN", PayoutType.UPI),
        Provider("cash_rw", "Arro cash agents", "RW", PayoutType.CASH),
        Provider("cash_gh", "Arro cash agents", "GH", PayoutType.CASH),
        Provider("cash_ng", "Arro cash agents", "NG", PayoutType.CASH),
        Provider("cash_ph", "Arro cash agents", "PH", PayoutType.CASH),
        Provider("arro", "Arro", "GB", PayoutType.ARRO),
    )

    fun provider(id: String): Provider? = providers.firstOrNull { it.id == id } ?: banks.firstOrNull { it.id == id }?.let {
        Provider(it.id, it.name, it.country, PayoutType.BANK)
    }

    fun providerName(id: String): String = provider(id)?.name ?: id

    fun providersFor(country: String, type: PayoutType): List<Provider> = when (type) {
        PayoutType.BANK -> banksFor(country).map { Provider(it.id, it.name, it.country, PayoutType.BANK) }
        else -> providers.filter { it.country == country && it.type == type }
    }

    // ---------- banks for payouts ----------
    val banks: List<Bank> = listOf(
        Bank("bk_rw", "Bank of Kigali", "RW", 10..16), Bank("equity_rw", "Equity Bank Rwanda", "RW", 10..16),
        Bank("im_rw", "I&M Bank Rwanda", "RW", 10..16), Bank("bpr_rw", "BPR Bank Rwanda", "RW", 10..16),
        Bank("access_rw", "Access Bank Rwanda", "RW", 10..16),
        Bank("equity_ke", "Equity Bank Kenya", "KE", 10..14), Bank("kcb_ke", "KCB Bank", "KE", 10..14),
        Bank("coop_ke", "Co-operative Bank", "KE", 10..14),
        Bank("stanbic_ug", "Stanbic Bank Uganda", "UG", 10..14), Bank("centenary_ug", "Centenary Bank", "UG", 10..14),
        Bank("gcb_gh", "GCB Bank", "GH", 10..16), Bank("ecobank_gh", "Ecobank Ghana", "GH", 10..16),
        Bank("sbi_in", "State Bank of India", "IN", 9..18), Bank("hdfc_in", "HDFC Bank", "IN", 9..18),
        Bank("icici_in", "ICICI Bank", "IN", 9..18),
        Bank("gtb_ng", "Guaranty Trust Bank (GTBank)", "NG", 10..10, "NUBAN, 10 digits"),
        Bank("access_ng", "Access Bank", "NG", 10..10, "NUBAN, 10 digits"), Bank("zenith_ng", "Zenith Bank", "NG", 10..10, "NUBAN, 10 digits"),
        Bank("bdo_ph", "BDO Unibank", "PH", 10..12), Bank("bpi_ph", "BPI", "PH", 10..12),
    )

    fun banksFor(country: String) = banks.filter { it.country == country }
    fun bank(id: String) = banks.firstOrNull { it.id == id }

    // ---------- UK banks for funding (Open Banking) ----------
    data class UkBank(val id: String, val name: String)

    val ukBanks = listOf(
        UkBank("barclays", "Barclays"), UkBank("hsbc", "HSBC UK"), UkBank("lloyds", "Lloyds"), UkBank("monzo", "Monzo"),
        UkBank("nationwide", "Nationwide"), UkBank("natwest", "NatWest"), UkBank("santander", "Santander"), UkBank("starling", "Starling"),
        UkBank("revolut", "Revolut"), UkBank("halifax", "Halifax"), UkBank("firstdirect", "first direct"), UkBank("tsb", "TSB"),
    )

    fun ukBank(id: String) = ukBanks.firstOrNull { it.id == id }

    // ---------- choices ----------
    val relationships = listOf("Sister", "Brother", "Mother", "Father", "Daughter", "Son", "Wife", "Husband", "Partner", "Cousin",
        "Aunt", "Uncle", "Friend", "Business", "Myself", "Other family")
    val reasons = listOf("Family support", "Education", "Medical expenses", "Paying for goods or services", "Savings or investment",
        "Gift", "Rent or property", "Business", "Other")
    val fundSources = listOf("Salary", "Savings", "Business income", "Other")
    val nationalities = listOf("British", "Rwandan", "Kenyan", "Ugandan", "Nigerian", "Ghanaian", "Indian", "Filipino", "Irish",
        "French", "Other")
    val occupations = listOf("Software engineer", "Nurse", "Teacher", "Student", "Accountant", "Driver", "Care worker", "Engineer",
        "Doctor", "Business owner", "Retail worker", "Self-employed", "Retired", "Other")
    val residenceCountries = listOf("United Kingdom" to "+44")
    val docTypes = listOf(
        DocType.PASSPORT to ("Passport" to "Photo page only · fastest to verify"),
        DocType.DRIVING_LICENCE to ("Driving licence" to "Photocard, front and back"),
        DocType.ID_CARD to ("National identity card" to "Front and back"),
    )
    val issuingCountries = listOf("United Kingdom", "Rwanda", "Kenya", "Uganda", "Nigeria", "Ghana", "India", "Philippines", "Ireland")

    // ---------- UK postcode lookup (demo directory) ----------
    fun addressesFor(postcode: String): List<Address> {
        val pc = Validation.normalisePostcode(postcode)
        if (!Validation.isUkPostcode(pc)) return emptyList()
        val (street, city) = when {
            pc.startsWith("E8") -> "Kestrel Road" to "London"
            pc.startsWith("M") -> "Wilmslow Road" to "Manchester"
            pc.startsWith("B") -> "Moseley Road" to "Birmingham"
            pc.startsWith("LS") -> "Harehills Lane" to "Leeds"
            else -> listOf("Station Road", "Church Lane", "Victoria Road", "Mill Lane")[kotlin.math.abs(pc.hashCode()) % 4] to "London"
        }
        val n = 21 + kotlin.math.abs(pc.hashCode()) % 60
        return listOf(
            Address("Flat 3, $n $street", city, pc), Address("Flat 1, $n $street", city, pc),
            Address("Flat 2, $n $street", city, pc), Address("${n + 2} $street", city, pc),
        )
    }

    // ---------- help centre ----------
    val helpCategories = listOf(
        "Sending" to "send", "Fees and rates" to "percent", "Verification" to "id-card", "Paying" to "credit-card",
        "Security" to "shield", "Refunds" to "refresh",
    )

    val articles: List<HelpArticle> = listOf(
        HelpArticle("arrive", "Sending money", "When will my money arrive?", "2 Oct 2026", 2,
            "We show an expected arrival time before you pay, and update it live on the tracking screen. Speed depends mostly on how your recipient gets the money.",
            listOf("Mobile money" to "Usually minutes", "Cash pickup" to "Within 1 hour", "Bank deposit" to "1 business day"),
            "What can slow it down", listOf(
                "A security check on a first or unusually large transfer",
                "Public holidays or weekends for bank deposits",
                "A name that doesn't match the recipient's account",
            )),
        HelpArticle("hold", "Sending money", "Why is my transfer on hold?", "28 Sep 2026", 2,
            "Regulations ask us to check some transfers before they leave, usually large or first-time ones. We'll ask you one or two quick questions in the app.",
            listOf("Typical check" to "Under 10 minutes", "Your rate" to "Held for 24 hours"),
            "How to release it", listOf(
                "Open the transfer from Home or Activity",
                "Answer how you know the recipient and where the money comes from",
                "If we don't hear from you within 24 hours, we refund you in full",
            )),
        HelpArticle("cancel", "Sending money", "Can I cancel a transfer?", "30 Sep 2026", 1,
            "You can ask us to cancel any transfer that hasn't reached your recipient yet. Open it and tap Request cancellation.",
            listOf("Before delivery" to "Full refund, fee included", "After delivery" to "Can't be reversed"),
            "Refund timing", listOf(
                "Arro balance: straight away",
                "Debit card: 3 to 5 working days",
                "Bank account: 1 to 2 working days",
            )),
        HelpArticle("fees", "Fees and rates", "How do your fees work?", "1 Oct 2026", 2,
            "You pay a small fixed fee and a margin that's built into our exchange rate. We show both before you pay.",
            listOf("Mobile money" to "From £1.99", "Bank deposit" to "From £0.99", "Rate margin" to "From 0.29%"),
            "Good to know", listOf(
                "Your first transfer has no Arro fee, up to £1,000",
                "Credit cards add 1.75% and may count as a cash advance",
                "Your rate is locked for 30 minutes once you see a quote",
            )),
        HelpArticle("verify", "Verification", "Why do I need to verify my identity?", "15 Sep 2026", 2,
            "The law requires us to confirm who you are before you send money abroad. It takes about 3 minutes and protects you and your recipients.",
            listOf("Standard" to "Up to £5,000 per transfer", "Enhanced" to "Up to £25,000 per transfer"),
            "What you'll need", listOf(
                "A passport, driving licence or national ID card",
                "Your current home address",
                "A quick selfie, matched once against your ID",
            )),
        HelpArticle("cards", "Paying", "Which cards can I use?", "20 Sep 2026", 1,
            "Visa and Mastercard debit and credit cards issued in the UK, in your name. You can also pay by bank or from your Arro balance.",
            listOf("Debit card" to "No fee", "Pay by bank" to "No fee", "Credit card" to "1.75% fee"),
            "If a card is declined", listOf(
                "Check the expiry date and security code",
                "Approve the payment in your bank's app if it asks",
                "Try paying by bank or from your balance instead",
            )),
        HelpArticle("security", "Security", "How do you keep my account safe?", "10 Sep 2026", 2,
            "Every transfer needs your passcode or Face ID. We check new devices by SMS and never ask for your passcode by phone or email.",
            listOf("Passcode" to "Required for every transfer", "New devices" to "Confirmed by SMS"),
            "If something looks wrong", listOf(
                "Freeze your account from Security in one tap",
                "Change your password and passcode",
                "Chat with us, any time of day",
            )),
        HelpArticle("refunds", "Refunds", "How do refunds work?", "25 Sep 2026", 1,
            "If a transfer is cancelled or can't be delivered, we refund the full amount including the fee, to where the money came from.",
            listOf("Arro balance" to "Instant", "Debit card" to "3 to 5 working days", "Bank account" to "1 to 2 working days"),
            "Tracking a refund", listOf(
                "Open the transfer to see the refund timeline",
                "Download a refund receipt for your records",
                "Contact us if it hasn't arrived after 7 working days",
            )),
    )

    fun article(id: String) = articles.firstOrNull { it.id == id }
}
