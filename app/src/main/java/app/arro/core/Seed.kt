package app.arro.core

import java.security.MessageDigest

/**
 * The sample account shown in the designs (Eric Nshuti), with history placed relative to today so it always looks
 * current. Log in with [DEMO_EMAIL] and [DEMO_PASSWORD]; the passcode is [DEMO_PASSCODE].
 */
object Seed {
    const val DEMO_EMAIL = "eric.nshuti@mail.com"
    const val DEMO_PHONE = "+447700900412"
    const val DEMO_PASSWORD = "Arro2026!"
    const val DEMO_PASSCODE = "246810"
    private const val DAY = 86_400_000L
    private const val MIN = 60_000L

    fun isDemoLogin(identifier: String, password: String): Boolean {
        val id = identifier.trim().lowercase()
        val digits = id.filter { it.isDigit() }.removePrefix("44").removePrefix("0")
        return password == DEMO_PASSWORD && (id == DEMO_EMAIL || digits == "7700900412")
    }

    /** Names the simulated payout networks return for known numbers (last 9 digits). */
    val registeredNames = mapOf(
        "788214412" to "CLAUDINE UWASE",
        "733451087" to "DIANE MUKAMANA",
        "712345908" to "WANJIRU KAMAU",
        "752400118" to "GRACE NAKATO",
        "123453305" to "CHIDI EMMANUEL EZE",
        "123457731" to "JEAN-PAUL HABIMANA",
        "040120452" to "CLAUDINE UWASE",
    )

    fun mismatchName(country: String) = when (country) {
        "RW" -> "JEAN BOSCO NIYONZIMA"
        "KE" -> "PETER OTIENO"
        "UG" -> "MOSES SSEMAKULA"
        "GH" -> "KWAME MENSAH"
        "NG" -> "TUNDE BAKARE"
        "IN" -> "RAHUL SHARMA"
        else -> "MARIA SANTOS"
    }

    val arroUsers = listOf(
        ArroUser("amani", "Amani Mugisha", "+447700900123"),
        ArroUser("aline", "Aline Ingabire", "+447700900456"),
        ArroUser("kofi", "Kofi Boateng", "+447700900789"),
        ArroUser("ngozi", "Ngozi Okafor", "+447700900321"),
    )

    fun issuerFor(digits: String): String = when {
        digits.startsWith("4921") -> "Monzo"
        digits.startsWith("4000") -> "Test Bank"
        digits.startsWith("51") || digits.startsWith("52") || digits.startsWith("53") -> "Barclaycard"
        digits.startsWith("5") || digits.startsWith("2") -> listOf("NatWest", "Santander", "Lloyds")[digits.sumOf { it - '0' } % 3]
        else -> listOf("Monzo", "Barclays", "HSBC UK", "Lloyds", "Starling", "NatWest")[digits.sumOf { it - '0' } % 6]
    }

    fun isCreditBin(digits: String): Boolean = digits.startsWith("51") || digits.startsWith("52") || digits.startsWith("53")

    private fun hash(salt: String, value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(("arro:$salt:$value").toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private data class Past(
        val daysAgo: Int, val minute: Int, val recipient: String, val method: String, val sendPounds: Double,
        val fee: Long, val rate: Double, val funding: String, val deliveredMin: Int, val ref: String = "",
    )

    fun account(now: Long): Account {
        val id = "DEMOERICN001"
        val t = { days: Int, minuteOfDay: Int -> Text.startOfDay(Text.localDate(now).minusDays(days.toLong())) + minuteOfDay * MIN }

        val claudine = Recipient(
            "R_CLAUDINE", "Claudine Uwase", "RW", "Sister", "", "Kigali", 1, true, true, t(260, 600),
            listOf(
                PayoutMethod("PM_CU_MTN", PayoutType.MOBILE_MONEY, "mtn_rw", "+250788214412", "CLAUDINE UWASE", "", true, t(260, 600)),
                PayoutMethod("PM_CU_BK", PayoutType.BANK, "bk_rw", "040120452", "CLAUDINE UWASE", "Savings", true, t(120, 600)),
            ), "PM_CU_MTN",
        )
        val diane = Recipient("R_DIANE", "Diane Mukamana", "RW", "Cousin", "", "Huye", 6, false, true, t(70, 700),
            listOf(PayoutMethod("PM_DM", PayoutType.MOBILE_MONEY, "airtel_rw", "+250733451087", "DIANE MUKAMANA", "", true, t(70, 700))), "PM_DM")
        val jp = Recipient("R_JP", "Jean-Paul Habimana", "RW", "Friend", "", "Kigali", 5, false, true, t(110, 700),
            listOf(PayoutMethod("PM_JP", PayoutType.BANK, "bk_rw", "040123457731", "JEAN-PAUL HABIMANA", "Current", true, t(110, 700))), "PM_JP")
        val wanjiru = Recipient("R_WANJIRU", "Wanjiru Kamau", "KE", "Friend", "", "Nairobi", 3, false, true, t(200, 700),
            listOf(PayoutMethod("PM_WK", PayoutType.MOBILE_MONEY, "mpesa_ke", "+254712345908", "WANJIRU KAMAU", "", true, t(200, 700))), "PM_WK")
        val grace = Recipient("R_GRACE", "Grace Nakato", "UG", "Friend", "", "Kampala", 4, false, true, t(150, 700),
            listOf(PayoutMethod("PM_GN", PayoutType.MOBILE_MONEY, "airtel_ug", "+256752400118", "GRACE NAKATO", "", true, t(150, 700))), "PM_GN")
        val chidi = Recipient("R_CHIDI", "Chidi Eze", "NG", "Business", "", "Lagos", 2, false, true, t(40, 700),
            listOf(PayoutMethod("PM_CE", PayoutType.BANK, "gtb_ng", "0123453305", "CHIDI EMMANUEL EZE", "Savings", true, t(40, 700), "Paying for goods or services")), "PM_CE")
        val recipients = listOf(claudine, diane, jp, wanjiru, grace, chidi)

        val visa = PaymentMethod("PAY_VISA", MethodKind.CARD, CardBrand.VISA, false, "4821", "08/29", "Monzo", "ERIC NSHUTI", status = MethodStatus.ACTIVE, isDefault = true, addedAt = t(43, 600))
        val credit = PaymentMethod("PAY_MC", MethodKind.CARD, CardBrand.MASTERCARD, true, "0917", "11/27", "Barclaycard", "ERIC NSHUTI", status = MethodStatus.ACTIVE, addedAt = t(43, 610))
        val bank = PaymentMethod("PAY_MONZO", MethodKind.BANK, last4 = "6402", issuer = "Monzo", holder = "ERIC NSHUTI", bankId = "monzo", status = MethodStatus.ACTIVE, addedAt = t(43, 620))

        val past = listOf(
            Past(3, 17 * 60 + 12, "R_WANJIRU", "PM_WK", 150.0, 149, 172.50, "PAY_VISA", 5),
            Past(9, 12 * 60 + 40, "R_GRACE", "PM_GN", 90.0, 99, 4786.00, "PAY_VISA", 4),
            Past(14, 18 * 60 + 5, "R_CLAUDINE", "PM_CU_MTN", 200.0, 199, 1954.20, ArroBackend.BALANCE, 6, "ARR-3M8D-71KQ"),
            Past(21, 9 * 60 + 30, "R_WANJIRU", "PM_WK", 120.0, 149, 171.80, "PAY_VISA", 7),
            Past(28, 19 * 60 + 10, "R_CHIDI", "PM_CE", 200.0, 99, 2003.40, "PAY_MONZO", 18),
            Past(43, 11 * 60 + 15, "R_CLAUDINE", "PM_CU_MTN", 200.0, 199, 1927.50, "PAY_VISA", 9),
            Past(50, 16 * 60, "R_CLAUDINE", "PM_CU_MTN", 300.0, 199, 1931.10, "PAY_VISA", 8),
            Past(60, 13 * 60, "R_GRACE", "PM_GN", 150.0, 99, 4750.00, "PAY_VISA", 6),
            Past(70, 10 * 60, "R_DIANE", "PM_DM", 150.0, 199, 1925.00, "PAY_VISA", 11),
            Past(80, 18 * 60, "R_WANJIRU", "PM_WK", 155.0, 149, 170.20, "PAY_VISA", 5),
            Past(95, 18 * 60 + 30, "R_CLAUDINE", "PM_CU_MTN", 200.0, 199, 1910.40, "PAY_VISA", 7),
            Past(110, 12 * 60, "R_JP", "PM_JP", 250.0, 99, 1905.00, "PAY_MONZO", 1100),
            Past(125, 17 * 60, "R_CLAUDINE", "PM_CU_MTN", 160.0, 199, 1898.70, "PAY_VISA", 6),
            Past(140, 14 * 60, "R_GRACE", "PM_GN", 100.0, 99, 4702.00, "PAY_VISA", 5),
            Past(150, 18 * 60, "R_CLAUDINE", "PM_CU_MTN", 200.0, 199, 1890.00, "PAY_VISA", 8),
            Past(170, 18 * 60, "R_CLAUDINE", "PM_CU_MTN", 400.0, 199, 1884.30, "PAY_VISA", 9),
            Past(200, 12 * 60, "R_WANJIRU", "PM_WK", 335.0, 149, 168.90, "PAY_VISA", 6),
            Past(230, 18 * 60, "R_CLAUDINE", "PM_CU_MTN", 300.0, 199, 1871.00, "PAY_VISA", 7),
            Past(260, 18 * 60, "R_CLAUDINE", "PM_CU_MTN", 300.0, 199, 1865.20, "PAY_VISA", 10),
        )
        val methodsById = recipients.flatMap { r -> r.methods.map { it.id to (r to it) } }.toMap()
        val fundingLabel = mapOf("PAY_VISA" to visa.label, "PAY_MONZO" to bank.label, ArroBackend.BALANCE to "Arro GBP balance")
        val transfers = past.mapIndexed { i, p ->
            val (r, m) = methodsById.getValue(p.method)
            val created = t(p.daysAgo, p.minute)
            val send = Math.round(p.sendPounds * 100)
            val cur = Catalog.country(r.country).currency
            val delivered = created + p.deliveredMin * MIN
            Transfer(
                id = "T_SEED_$i", reference = p.ref.ifBlank { "ARR-" + seedCode(i, 4) + "-" + seedCode(i + 40, 4) },
                recipientId = r.id, recipientName = r.name, recipientColor = r.color, country = r.country, to = cur.code,
                sendMinor = send, feeMinor = p.fee, totalMinor = send + p.fee, rate = p.rate, midRate = p.rate / (1 - Rates.TRANSFER_MARGIN),
                receiveMinor = Money.convert(send, Currency.GBP, cur, p.rate), payoutType = m.type, providerId = m.providerId,
                payoutAccount = m.account, fundingId = p.funding, fundingLabel = fundingLabel.getValue(p.funding), reason = "Family support",
                createdAt = created, status = TransferStatus.DELIVERED, paidAt = created + 20_000, convertedAt = created + 40_000,
                handedAt = created + 70_000, deliveredAt = delivered,
                providerRef = if (i == 2) "MP261001.1811.C44219" else Ids.providerRef(m.providerId, delivered),
            )
        }

        val ledger = listOf(
            LedgerEntry("L_CONV1", LedgerType.CONVERSION, "GBP", -10_000, "EUR", 11_530, 1.1530, "GBP to EUR", "Converted", t(15, 10 * 60)),
            LedgerEntry("L_TOP1", LedgerType.TOP_UP, "GBP", 50_000, title = "Added from Monzo", subtitle = "Bank transfer", createdAt = t(18, 9 * 60 + 5)),
            LedgerEntry("L_TOP2", LedgerType.TOP_UP, "GBP", 30_000, title = "Added from Monzo", subtitle = "Bank transfer", createdAt = t(46, 8 * 60 + 40)),
            LedgerEntry("L_REWARD", LedgerType.REWARD, "GBP", 1_000, title = "Invite reward", subtitle = "Amani joined Arro", createdAt = t(33, 12 * 60)),
            LedgerEntry("L_CONV2", LedgerType.CONVERSION, "GBP", -11_200, "USD", 15_000, 1.3393, "GBP to USD", "Converted", t(64, 15 * 60)),
            LedgerEntry("L_REWARD2", LedgerType.REWARD, "GBP", 1_000, title = "Invite reward", subtitle = "Kofi joined Arro", createdAt = t(90, 12 * 60)),
            LedgerEntry("L_TOP3", LedgerType.TOP_UP, "GBP", 100_000, title = "Added from Monzo", subtitle = "Bank transfer", createdAt = t(100, 9 * 60)),
        )

        val notifications = listOf(
            AppNotification("N1", NoticeKind.RATE, "Rate alert reached", "GBP → RWF is above 1,960. Your quote is ready to lock.", now - 55 * MIN),
            AppNotification("N2", NoticeKind.SECURITY, "New sign-in", "Web browser on a Mac in London. Not you? Secure your account now.", now - 5 * 60 * MIN),
            AppNotification("N3", NoticeKind.STATEMENT, "September statement", "Ready to download as PDF or CSV.", t(2, 8 * 60), read = true),
            AppNotification("N4", NoticeKind.TRANSFER, "Delivered to Wanjiru", "KES 25,875.00 arrived in her M-Pesa wallet.", transfers[0].deliveredAt, read = true, transferId = transfers[0].id),
            AppNotification("N5", NoticeKind.TRANSFER, "Delivered to Grace", "UGX 430,740 arrived via Airtel Money.", transfers[1].deliveredAt, read = true, transferId = transfers[1].id),
            AppNotification("N6", NoticeKind.ACCOUNT, "Identity verified", "You can send up to £5,000 per transfer.", t(43, 10 * 60), read = true),
        )

        return Account(
            id = id, createdAt = t(44, 9 * 60), residence = "GB", phone = DEMO_PHONE, email = DEMO_EMAIL, emailVerified = true,
            passwordHash = hash(id, DEMO_PASSWORD), passcodeHash = hash(id, DEMO_PASSCODE), biometricEnabled = false, username = "ericn",
            profile = Profile("Eric", "Nshuti", "1991-07-03", "British", "Software engineer", Address("Flat 3, 41 Kestrel Road", "London", "E8 2PL")),
            kyc = Kyc(KycStatus.APPROVED, KycLevel.STANDARD, DocType.PASSPORT, "United Kingdom", "2031-05-12", t(43, 9 * 60), 0, t(43, 10 * 60)),
            wallets = listOf(
                Wallet("GBP", 248_635, "04-29-83", "21943318", main = true, openedAt = t(44, 9 * 60)),
                Wallet("EUR", 41_280, iban = "GB71 ARRO 0429 8321 9433 18", openedAt = t(64, 15 * 60)),
                Wallet("USD", 15_000, sortCode = "026073150", accountNumber = "8314402917", openedAt = t(64, 15 * 60)),
            ),
            recipients = recipients, paymentMethods = listOf(visa, credit, bank), transfers = transfers, ledger = ledger,
            notifications = notifications,
            rateAlerts = listOf(RateAlert("A_KES", "KES", 175.00, t(5, 9 * 60), t(5, 9 * 60) + 30 * DAY)),
            statements = listOf(
                StatementRecord("S1", "GBP", Text.localDate(t(45, 0)).withDayOfMonth(1).toString(), Text.localDate(t(45, 0)).withDayOfMonth(1).plusMonths(1).minusDays(1).toString(), "PDF", 6, t(14, 9 * 60)),
                StatementRecord("S2", "GBP", Text.localDate(t(75, 0)).withDayOfMonth(1).toString(), Text.localDate(t(75, 0)).withDayOfMonth(1).plusMonths(1).minusDays(1).toString(), "PDF", 9, t(44, 9 * 60)),
            ),
            devices = listOf(
                Device("D1", ArroBackend.DEVICE_NAME, "London", now, current = true),
                Device("D2", "Web browser, Mac", "London", t(3, 20 * 60), laptop = true),
            ),
            referral = sampleReferral("ERIC-7Q4K", now),
            // The sample account shows its balance on home screen widgets so they can be demonstrated.
            // New accounts start with the balance hidden (see Settings.widgetBalance).
            settings = Settings(widgetBalance = true),
            firstTransferFree = false,
            setupDismissed = true,
            demo = true,
        ).let { Loyalty.backfill(it, now).copy(loyaltyVersion = ArroBackend.LOYALTY_VERSION) }
    }

    /**
     * Friends who joined with the sample account's code. They match the two invite rewards in its ledger, plus one
     * friend who hasn't made a qualifying transfer yet. Marked [ReferredFriend.sample] so the app labels them.
     */
    fun sampleReferral(code: String, now: Long): Referral {
        val t = { days: Int, minuteOfDay: Int -> Text.startOfDay(Text.localDate(now).minusDays(days.toLong())) + minuteOfDay * MIN }
        val friends = listOf(
            ReferredFriend("F_ALINE", "Aline Ingabire", FriendStatus.SIGNED_UP, t(6, 18 * 60), sample = true),
            ReferredFriend("F_AMANI", "Amani Mugisha", FriendStatus.QUALIFIED, t(36, 19 * 60), 15_000, t(33, 12 * 60), Loyalty.REFERRAL_REWARD_MINOR, sample = true),
            ReferredFriend("F_KOFI", "Kofi Boateng", FriendStatus.QUALIFIED, t(97, 9 * 60), 20_000, t(90, 12 * 60), Loyalty.REFERRAL_REWARD_MINOR, sample = true),
        )
        return Referral(code, friends.size, friends.sumOf { it.rewardMinor }, friends)
    }

    private fun seedCode(i: Int, n: Int): String {
        val alpha = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        var x = i * 7919 + 104_729
        return (1..n).map { x = (x * 1_103_515_245 + 12_345) and 0x7fffffff; alpha[x % alpha.length] }.joinToString("")
    }
}
