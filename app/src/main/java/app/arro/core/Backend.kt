package app.arro.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.time.DayOfWeek

fun interface Clock {
    fun now(): Long
}

/** Where the backend keeps its JSON document. The Android app writes it to internal storage. */
interface StateStore {
    val persistent: Boolean
    fun read(): String?
    fun write(text: String)
}

class MemoryStore : StateStore {
    override val persistent = false
    override fun read(): String? = null
    override fun write(text: String) {}
}

data class TransferDraft(
    val recipientId: String,
    val payoutMethodId: String,
    val quote: Quote,
    val fundingId: String,
    val reason: String,
)

data class NameCheck(val registeredName: String, val matches: Boolean, val providerName: String)

data class ArroUser(val handle: String, val name: String, val phone: String)

/**
 * The simulated Arro backend. It behaves like a remote API (latency, validation, errors) but keeps all data on the
 * device. Transfers, identity checks, refunds and support cases move forward over time in [tick].
 */
class ArroBackend(
    private val store: StateStore,
    private val rateSource: RateSource,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock { System.currentTimeMillis() },
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val latency: Boolean = true,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BackendEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BackendEvent> = _events.asSharedFlow()

    /** The app is unlocked for this process; it starts locked whenever someone is signed in. */
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var started = false
    private var failedPasscodes = 0

    // pending verification codes (in memory, like a server session)
    private data class PendingCode(val code: String, val target: String, val expires: Long, var attempts: Int = 0)
    private var signupCode: PendingCode? = null
    private var resetCode: PendingCode? = null
    private var resetVerified = false

    data class Registration(val residence: String = "GB", val phone: String = "", val phoneVerified: Boolean = false,
                            val email: String = "", val password: String = "", val marketing: Boolean = false)
    private val _registration = MutableStateFlow(Registration())
    val registration: StateFlow<Registration> = _registration.asStateFlow()

    val now: Long get() = clock.now()
    val account: Account? get() = _state.value.account
    private val fast: Boolean get() = _state.value.demo.fast

    // ------------------------------------------------------------------ lifecycle

    private fun load(): AppState {
        val text = runCatching { store.read() }.getOrNull() ?: return AppState()
        return runCatching { json.decodeFromString<AppState>(text) }.getOrElse { AppState() }
    }

    /** Starts persistence, the status engine and periodic rate refreshes. Call once. */
    fun start() {
        if (started) return
        started = true
        if (store.persistent) {
            scope.launch(io) {
                _state.drop(1).collect { s -> runCatching { store.write(json.encodeToString(s)) } }
            }
        }
        scope.launch {
            while (isActive) {
                runCatching { tick() }
                delay(1000)
            }
        }
        scope.launch {
            while (isActive) {
                refreshRates()
                delay(5 * 60_000L)
            }
        }
    }

    private suspend fun net(ms: Long = 650) {
        if (latency) delay(ms)
    }

    private fun mutate(block: (AppState) -> AppState) = _state.update(block)

    private fun mutateAccount(block: (Account) -> Account) = _state.update { s -> s.account?.let { s.copy(account = block(it)) } ?: s }

    private fun requireAccount(): Account = _state.value.account ?: throw ArroException("Please log in again.", "auth")

    private fun emit(e: BackendEvent) {
        _events.tryEmit(e)
    }

    private fun notify(kind: NoticeKind, title: String, body: String, transferId: String = "") {
        val n = AppNotification(Ids.id(), kind, title, body, now, false, transferId)
        mutateAccount { it.copy(notifications = listOf(n) + it.notifications) }
    }

    // ------------------------------------------------------------------ onboarding and sign-in

    fun markOnboardingSeen() = mutate { it.copy(onboardingSeen = true) }

    suspend fun sendSignupCode(residence: String, nationalDigits: String): String {
        Validation.ukMobile(nationalDigits)?.let { throw ArroException(it) }
        val phone = Validation.normaliseUkMobile(nationalDigits)
        net()
        val code = Ids.code()
        signupCode = PendingCode(code, phone, now + 10 * 60_000)
        _registration.value = Registration(residence = residence, phone = phone)
        emit(BackendEvent.Code(code, "Your Arro code is $code. Never share it, not even with Arro staff.", phone))
        return phone
    }

    suspend fun resendSignupCode() {
        val p = signupCode ?: throw ArroException("Start again with your mobile number.")
        net(400)
        val code = Ids.code()
        signupCode = PendingCode(code, p.target, now + 10 * 60_000)
        emit(BackendEvent.Code(code, "Your Arro code is $code. Never share it, not even with Arro staff.", p.target))
    }

    suspend fun verifySignupCode(code: String) {
        val p = signupCode ?: throw ArroException("That code has expired. Ask for a new one.")
        net(500)
        if (now > p.expires) throw ArroException("That code has expired. Ask for a new one.")
        p.attempts++
        if (p.attempts > 5) throw ArroException("Too many tries. Ask for a new code.")
        if (code != p.code) throw ArroException("That code isn't right. Check the SMS and try again.")
        _registration.update { it.copy(phoneVerified = true) }
    }

    suspend fun setLogin(email: String, password: String, marketing: Boolean) {
        Validation.email(email)?.let { throw ArroException(it) }
        if (!Validation.password(password).required) throw ArroException("Your password needs at least 8 characters, upper and lower case letters and a number.")
        net(400)
        _registration.update { it.copy(email = email.trim().lowercase(), password = password, marketing = marketing) }
    }

    /** Creates the account once the passcode is set. */
    suspend fun finishRegistration(passcode: String) {
        val r = _registration.value
        if (!r.phoneVerified || r.email.isBlank()) throw ArroException("Something went wrong. Please start again.")
        if (Validation.passcodeWeak(passcode)) throw ArroException("Choose a passcode that isn't a repeated or simple sequence.")
        net(700)
        val id = Ids.id()
        val t = now
        val handle = r.email.substringBefore('@').filter { it.isLetterOrDigit() }.take(12).lowercase()
        val acct = Account(
            id = id, createdAt = t, residence = r.residence, phone = r.phone, email = r.email,
            passwordHash = hash(id, r.password), passcodeHash = hash(id, passcode), marketingOptIn = r.marketing,
            username = handle.ifBlank { "arro" + id.take(4).lowercase() },
            wallets = listOf(Wallet("GBP", 0, Ids.sortCode(), Ids.accountNumber(), main = true, openedAt = t)),
            devices = listOf(Device(Ids.id(), DEVICE_NAME, "London", t, current = true)),
            referral = Referral(code = (handle.take(4).ifBlank { "ARRO" }).uppercase() + "-" + Ids.short(4)),
            settings = Settings(news = r.marketing, marketing = r.marketing),
            security = SecuritySettings(passcodeChangedAt = t, passwordChangedAt = t),
            notifications = listOf(AppNotification(Ids.id(), NoticeKind.ACCOUNT, "Welcome to Arro",
                "Verify your identity to start sending. It takes about 3 minutes.", t)),
        )
        mutate { it.copy(account = acct, signedIn = true, onboardingSeen = true) }
        _registration.value = Registration()
        signupCode = null
        _unlocked.value = true
    }

    fun setBiometric(enabled: Boolean) = mutateAccount { it.copy(biometricEnabled = enabled) }

    suspend fun login(identifier: String, password: String) {
        val id = identifier.trim().lowercase()
        if (id.isEmpty() || password.isEmpty()) throw ArroException("Enter your email or mobile number and your password.")
        net(800)
        val existing = _state.value.account
        if (existing != null && matchesIdentifier(existing, id)) {
            if (hash(existing.id, password) != existing.passwordHash) throw ArroException("That email or password isn't right.")
            mutate { it.copy(signedIn = true, account = existing.copy(paused = false)) }
            _unlocked.value = true
            failedPasscodes = 0
            return
        }
        if (Seed.isDemoLogin(id, password)) {
            mutate { it.copy(account = Seed.account(now), signedIn = true, onboardingSeen = true) }
            _unlocked.value = true
            failedPasscodes = 0
            return
        }
        throw ArroException("That email or password isn't right.")
    }

    private fun matchesIdentifier(a: Account, id: String): Boolean {
        if (a.email.equals(id, ignoreCase = true)) return true
        val digits = id.filter { it.isDigit() }
        if (digits.length < 9) return false
        val acc = a.phone.filter { it.isDigit() }
        return acc.endsWith(digits.removePrefix("0").takeLast(10))
    }

    /** Signs back in on this device with biometrics, when the user turned it on before. */
    fun loginWithBiometrics() {
        val a = _state.value.account ?: throw ArroException("Log in with your password first.")
        if (!a.biometricEnabled) throw ArroException("Turn on fingerprint or face unlock in Security after you log in.")
        if (a.paused) mutateAccount { it.copy(paused = false) }
        mutate { it.copy(signedIn = true) }
        _unlocked.value = true
        failedPasscodes = 0
    }

    suspend fun unlockWithPasscode(code: String) {
        val a = requireAccount()
        net(250)
        if (hash(a.id, code) != a.passcodeHash) {
            failedPasscodes++
            if (failedPasscodes >= 5) {
                failedPasscodes = 0
                mutate { it.copy(signedIn = false) }
                throw ArroException("Too many wrong tries. Log in with your password.", "locked_out")
            }
            throw ArroException("That passcode isn't right. ${5 - failedPasscodes} tries left.")
        }
        failedPasscodes = 0
        _unlocked.value = true
    }

    /** Checks the passcode without changing lock state (used to approve a transfer). */
    suspend fun checkPasscode(code: String): Boolean {
        val a = requireAccount()
        net(250)
        return hash(a.id, code) == a.passcodeHash
    }

    fun unlockWithBiometrics() {
        if (_state.value.account?.biometricEnabled == true) _unlocked.value = true
    }

    fun lock() {
        if (_state.value.signedIn) _unlocked.value = false
    }

    fun logout() {
        _unlocked.value = false
        mutate { it.copy(signedIn = false) }
    }

    suspend fun requestPasswordReset(email: String) {
        Validation.email(email)?.let { throw ArroException(it) }
        net(700)
        val e = email.trim().lowercase()
        val a = _state.value.account
        val known = (a != null && a.email == e) || e == Seed.DEMO_EMAIL
        if (!known) throw ArroException("We couldn't find an account with that email. Check it, or create an account.")
        if (a == null || a.email != e) mutate { it.copy(account = Seed.account(now)) }
        val code = Ids.code()
        resetCode = PendingCode(code, e, now + 15 * 60_000)
        resetVerified = false
        emit(BackendEvent.Code(code, "Your Arro password reset code is $code.", e))
    }

    val resetEmail: String get() = resetCode?.target ?: ""

    suspend fun verifyResetCode(code: String) {
        val p = resetCode ?: throw ArroException("Ask for a new reset code.")
        net(400)
        p.attempts++
        if (now > p.expires) throw ArroException("That code has expired. Ask for a new one.")
        if (code != p.code) throw ArroException("That code isn't right. Check your email and try again.")
        resetVerified = true
    }

    suspend fun resetPassword(newPassword: String): Long {
        if (!resetVerified) throw ArroException("Enter the code we emailed you first.")
        if (!Validation.password(newPassword).required) throw ArroException("Your password needs at least 8 characters, upper and lower case letters and a number.")
        net(700)
        val until = now + 24 * 3_600_000L
        mutateAccount { a ->
            a.copy(passwordHash = hash(a.id, newPassword), sendPauseUntil = until,
                security = a.security.copy(passwordChangedAt = now),
                devices = a.devices.filter { it.current })
        }
        notify(NoticeKind.SECURITY, "Password changed", "New transfers are paused until ${Text.time(until)} tomorrow while we make sure this was you.")
        mutate { it.copy(signedIn = false) }
        _unlocked.value = false
        resetCode = null
        resetVerified = false
        return until
    }

    suspend fun changePassword(current: String, newPassword: String) {
        val a = requireAccount()
        if (hash(a.id, current) != a.passwordHash) throw ArroException("Your current password isn't right.")
        if (!Validation.password(newPassword).required) throw ArroException("Your new password needs at least 8 characters, upper and lower case letters and a number.")
        net()
        mutateAccount { it.copy(passwordHash = hash(it.id, newPassword), security = it.security.copy(passwordChangedAt = now)) }
        notify(NoticeKind.SECURITY, "Password changed", "Your Arro password was changed on $DEVICE_NAME.")
    }

    suspend fun changePasscode(current: String, newCode: String) {
        val a = requireAccount()
        if (hash(a.id, current) != a.passcodeHash) throw ArroException("Your current passcode isn't right.")
        if (Validation.passcodeWeak(newCode)) throw ArroException("Choose a passcode that isn't a repeated or simple sequence.")
        net(400)
        mutateAccount { it.copy(passcodeHash = hash(it.id, newCode), security = it.security.copy(passcodeChangedAt = now)) }
        notify(NoticeKind.SECURITY, "Passcode changed", "Your passcode was changed on $DEVICE_NAME.")
    }

    // ------------------------------------------------------------------ identity verification

    fun savePersonal(first: String, last: String, dobIso: String, nationality: String, occupation: String) = mutateAccount {
        it.copy(profile = it.profile.copy(firstNames = first.trim(), lastName = last.trim(), dob = dobIso, nationality = nationality, occupation = occupation))
    }

    fun saveAddress(address: Address) = mutateAccount { it.copy(profile = it.profile.copy(address = address)) }

    fun chooseDocument(type: DocType, country: String) = mutateAccount { it.copy(kyc = it.kyc.copy(docType = type, docCountry = country)) }

    /** [quality] is 0..1 from the on-device photo check; very low quality leads to a retake request. */
    suspend fun submitIdentity(quality: Float) {
        val a = requireAccount()
        if (a.kyc.attemptsLeft <= 0) throw ArroException("You've used today's attempts. Try again tomorrow, or chat with us.")
        net(900)
        val reject = _state.value.demo.rejectNextKyc || quality < 0.25f
        val decide = now + if (fast) 7_000 else 90_000
        mutateAccount {
            it.copy(kyc = it.kyc.copy(status = KycStatus.IN_REVIEW, submittedAt = now, decideAt = decide, willReject = reject, rejectReason = ""))
        }
        mutate { it.copy(demo = it.demo.copy(rejectNextKyc = false)) }
    }

    fun retakeIdentity() = mutateAccount { it.copy(kyc = it.kyc.copy(status = KycStatus.NOT_STARTED, rejectReason = "")) }

    suspend fun requestEnhanced() {
        net(800)
        mutateAccount { it.copy(kyc = it.kyc.copy(enhancedRequested = true, decideAt = now + if (fast) 15_000 else 600_000)) }
        notify(NoticeKind.ACCOUNT, "Documents received", "We're reviewing your proof of address and income. We'll let you know shortly.")
    }

    // ------------------------------------------------------------------ rates and quotes

    suspend fun refreshRates(): Boolean {
        val fresh = runCatching { rateSource.fetch() }.getOrNull()
        if (fresh.isNullOrEmpty()) return false
        mutate { s ->
            val old = s.rates
            val rollPrevious = old.mid.isNotEmpty() && (old.previous.isEmpty() || now - old.previousAt > 20 * 3_600_000L)
            s.copy(rates = RatesSnapshot(
                mid = fresh.filterKeys { k -> Currency.entries.any { it.code == k } },
                fetchedAt = now, source = "open.er-api.com",
                previous = if (rollPrevious) old.mid else old.previous,
                previousAt = if (rollPrevious) old.fetchedAt else old.previousAt,
            ))
        }
        checkRateAlerts()
        return true
    }

    fun customerRate(to: Currency, from: Currency = Currency.GBP): Double = Rates.customer(_state.value.rates, from, to)
    fun midRate(to: Currency, from: Currency = Currency.GBP): Double = Rates.cross(_state.value.rates, from, to)
    fun conversionRate(from: Currency, to: Currency): Double = Rates.customer(_state.value.rates, from, to, Rates.CONVERSION_MARGIN)

    fun feeFor(country: String, type: PayoutType): Long =
        if (type == PayoutType.ARRO) 0 else Catalog.country(country).option(type)?.feeMinor ?: 199

    fun quote(to: Currency, sendMinor: Long, type: PayoutType, country: String, fundingId: String? = null): Quote {
        val a = _state.value.account
        val isArro = type == PayoutType.ARRO
        val target = if (isArro) Currency.GBP else to
        val mid = if (isArro) 1.0 else midRate(target)
        val rate = if (isArro) 1.0 else customerRate(target)
        val receive = Money.convert(sendMinor, Currency.GBP, target, rate)
        val waived = !isArro && a != null && a.firstTransferFree && a.transfers.isEmpty() && sendMinor <= 100_000 && sendMinor > 0
        val fee = if (sendMinor <= 0 || waived) 0 else feeFor(country, type)
        val method = a?.paymentMethod(fundingId)
        val fundingFee = if (method != null && method.kind == MethodKind.CARD && method.credit) Math.round(sendMinor * 0.0175) else 0L
        val margin = if (isArro || mid == 0.0) 0L else Math.round(sendMinor * (mid - rate) / mid)
        return Quote(Currency.GBP, target, sendMinor, receive, rate, mid, fee, fundingFee, margin, now + 30 * 60_000, type, waived)
    }

    /** Card fee for paying [sendMinor] with [fundingId] (credit cards add 1.75%). */
    fun fundingFee(sendMinor: Long, fundingId: String?): Long {
        val m = _state.value.account?.paymentMethod(fundingId) ?: return 0
        return if (m.kind == MethodKind.CARD && m.credit) Math.round(sendMinor * 0.0175) else 0
    }

    /** When a transfer created now would arrive, for the review screen. */
    fun estimateArrival(type: PayoutType): Long = plan(type, now)[3]

    /** True when [providerId] is the one marked as briefly offline. */
    fun isProviderDown(providerId: String?): Boolean = providerId != null && providerId == _state.value.demo.outageProviderId

    /** A fresh price for the same transfer, used when a rate lock runs out. */
    fun requote(q: Quote, country: String, fundingId: String?): Quote = quote(q.to, q.sendMinor, q.payoutType, country, fundingId)

    // ------------------------------------------------------------------ limits

    data class Usage(val monthMinor: Long, val yearMinor: Long)

    fun usage(a: Account = requireAccount()): Usage {
        val t = now
        val month = Text.startOfMonth(t)
        val year = Text.startOfDay(Text.localDate(t).withDayOfYear(1))
        val counted = a.transfers.filter { it.status != TransferStatus.CANCELLED && it.status != TransferStatus.REFUNDED && it.status != TransferStatus.FAILED }
        return Usage(counted.filter { it.createdAt >= month }.sumOf { it.sendMinor }, counted.filter { it.createdAt >= year }.sumOf { it.sendMinor })
    }

    /** Null when the amount is within limits, otherwise the reason. */
    fun limitProblem(sendMinor: Long): String? {
        val a = _state.value.account ?: return null
        val level = a.kyc.level
        if (level == KycLevel.NONE) return null
        if (sendMinor > level.perTransferMinor) return "That's over your ${Money.format(level.perTransferMinor, Currency.GBP, false)} per-transfer limit."
        val u = usage(a)
        if (u.monthMinor + sendMinor > level.perMonthMinor) return "That would take you over your ${Money.format(level.perMonthMinor, Currency.GBP, false)} monthly limit."
        if (u.yearMinor + sendMinor > level.perYearMinor) return "That would take you over your yearly limit."
        return null
    }

    // ------------------------------------------------------------------ transfers

    fun sendBlocker(): ArroException? {
        val a = _state.value.account ?: return ArroException("Please log in again.", "auth")
        return when {
            a.kyc.status != KycStatus.APPROVED -> ArroException("Verify your identity to start sending. It takes about 3 minutes.", "kyc")
            a.frozen -> ArroException("Your account is frozen. Unfreeze it in Security to send money.", "frozen")
            a.sendPauseUntil > now -> ArroException("New transfers are paused until ${Text.time(a.sendPauseUntil)} ${if (Text.isSameDay(a.sendPauseUntil, now)) "today" else "tomorrow"} after your password change.", "paused")
            else -> null
        }
    }

    suspend fun createTransfer(d: TransferDraft): Transfer {
        sendBlocker()?.let { throw it }
        val a = requireAccount()
        val r = a.recipient(d.recipientId) ?: throw ArroException("Choose who you're sending to.")
        val m = r.method(d.payoutMethodId) ?: r.defaultMethod ?: throw ArroException("Add a way for ${r.firstName} to receive money.")
        if (m.providerId == _state.value.demo.outageProviderId) throw ArroException("${m.providerName} is briefly offline.", "outage")
        if (d.quote.lockedUntil < now) throw ArroException("Your rate lock ran out. Here's a fresh quote.", "expired")
        limitProblem(d.quote.sendMinor)?.let { throw ArroException(it, "limit") }
        if (d.quote.sendMinor < 100) throw ArroException("The smallest amount you can send is £1.00.")
        val q = d.quote
        val fundingLabel: String
        if (d.fundingId == BALANCE) {
            val w = a.wallet(Currency.GBP)
            if (w == null || w.balanceMinor < q.totalMinor) throw ArroException("Not enough in your balance.", "insufficient")
            fundingLabel = "Arro GBP balance"
        } else {
            val pm = a.paymentMethod(d.fundingId) ?: throw ArroException("Choose how you'll pay.")
            if (pm.status != MethodStatus.ACTIVE) throw ArroException("Finish adding ${pm.label} first.")
            fundingLabel = pm.label
        }
        net(1200)
        val t = now
        val plan = plan(m.type, t)
        val nationalDigits = Text.splitPhone(m.account).second.ifEmpty { m.account.filter { it.isDigit() } }
        val willFail = m.type != PayoutType.ARRO && nationalDigits.endsWith("000")
        val willHold = m.type != PayoutType.ARRO && q.sendMinor > HOLD_THRESHOLD
        val tr = Transfer(
            id = Ids.id(), reference = Ids.reference(), recipientId = r.id, recipientName = r.name, recipientColor = r.color,
            country = r.country, from = "GBP", to = q.to.code, sendMinor = q.sendMinor, feeMinor = q.feeMinor,
            fundingFeeMinor = q.fundingFeeMinor, totalMinor = q.totalMinor, rate = q.rate, midRate = q.mid,
            receiveMinor = q.receiveMinor, payoutType = m.type, providerId = m.providerId, payoutAccount = m.account,
            fundingId = d.fundingId, fundingLabel = fundingLabel, reason = d.reason, createdAt = t,
            paidAt = plan[0], convertedAt = plan[1], handedAt = plan[2], deliveredAt = plan[3],
            holdAt = if (willHold) plan[0] + 1_000 else 0,
            failAt = if (willFail) plan[2] + (if (fast) 6_000 else 300_000) else 0,
            willHold = willHold, willFail = willFail,
        )
        mutateAccount { acc ->
            val wallets = if (d.fundingId == BALANCE) acc.wallets.map { w -> if (w.currency == "GBP") w.copy(balanceMinor = w.balanceMinor - q.totalMinor) else w } else acc.wallets
            acc.copy(transfers = listOf(tr) + acc.transfers, wallets = wallets, firstTransferFree = acc.firstTransferFree && !q.feeWaived)
        }
        return tr
    }

    /** Planned milestone times: paid, converted, handed to provider, delivered. */
    private fun plan(type: PayoutType, t: Long): LongArray {
        if (type == PayoutType.ARRO) return longArrayOf(t + 800, t + 1_200, t + 1_600, t + 2_500)
        if (fast) {
            val deliver = when (type) { PayoutType.BANK -> 60_000L; PayoutType.CASH -> 45_000L; else -> 30_000L }
            return longArrayOf(t + 2_000, t + 4_000, t + 7_000, t + deliver)
        }
        val deliver = when (type) {
            PayoutType.BANK -> nextBusinessMorning(t) - t
            PayoutType.CASH -> 40 * 60_000L
            else -> 9 * 60_000L
        }
        return longArrayOf(t + 5_000, t + 15_000, t + 60_000, t + deliver)
    }

    private fun nextBusinessMorning(t: Long): Long {
        var d = Text.localDate(t).plusDays(1)
        while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
        return Text.startOfDay(d) + 10 * 3_600_000L
    }

    suspend fun answerHold(transferId: String, relationship: String, source: String) {
        if (relationship.isBlank() || source.isBlank()) throw ArroException("Answer both questions to release your transfer.")
        net(900)
        updateTransfer(transferId) { tr ->
            if (tr.status != TransferStatus.ON_HOLD) tr
            else {
                val shift = now - tr.holdAt + 1_000
                tr.copy(status = TransferStatus.IN_PROGRESS, holdAnsweredAt = now, holdRelationship = relationship, holdSource = source,
                    convertedAt = maxOf(tr.convertedAt + shift, now + 1_000), handedAt = tr.handedAt + shift, deliveredAt = tr.deliveredAt + shift,
                    failAt = if (tr.willFail) tr.failAt + shift else 0)
            }
        }
    }

    fun canCancel(tr: Transfer): Boolean = tr.isOpen && now < tr.deliveredAt && tr.payoutType != PayoutType.ARRO

    suspend fun cancelTransfer(transferId: String, reason: String) {
        val tr = requireAccount().transfer(transferId) ?: throw ArroException("We couldn't find that transfer.")
        if (!canCancel(tr)) throw ArroException("This transfer has already reached ${tr.firstName}, so it can't be cancelled.")
        net(1000)
        startRefund(transferId, TransferStatus.CANCELLED, cancelReason = reason)
        notify(NoticeKind.TRANSFER, "Transfer cancelled", "Your ${Money.format(tr.totalMinor, tr.fromCur)} is on its way back to ${tr.fundingLabel}.", transferId)
    }

    private fun startRefund(transferId: String, status: TransferStatus, cancelReason: String = "", failReason: String = "") {
        val t = now
        var credit: Pair<Long, String>? = null
        updateTransfer(transferId) { tr ->
            val toBalance = tr.fundingId == BALANCE
            val due = when {
                toBalance -> t + 2_000
                fast -> t + 40_000
                else -> t + 3 * 86_400_000L
            }
            if (toBalance) credit = tr.totalMinor to tr.reference
            tr.copy(status = status, cancelledAt = if (status == TransferStatus.CANCELLED) t else tr.cancelledAt, cancelReason = cancelReason.ifBlank { tr.cancelReason },
                failReason = failReason.ifBlank { tr.failReason }, refundStartedAt = t + 1_000, refundDueAt = due, refundReference = Ids.refundReference(tr.reference))
        }
        credit?.let { (amount, ref) ->
            mutateAccount { acc ->
                acc.copy(wallets = acc.wallets.map { w -> if (w.currency == "GBP") w.copy(balanceMinor = w.balanceMinor + amount) else w },
                    ledger = listOf(LedgerEntry(Ids.id(), LedgerType.REFUND, "GBP", amount, title = "Refund", subtitle = ref, createdAt = t)) + acc.ledger)
            }
        }
    }

    fun setRepeatMonthly(transferId: String, on: Boolean) = updateTransfer(transferId) { it.copy(repeatMonthly = on) }

    private fun updateTransfer(id: String, block: (Transfer) -> Transfer) = mutateAccount { a ->
        a.copy(transfers = a.transfers.map { if (it.id == id) block(it) else it })
    }

    /** Advances everything that moves with time. Safe to call often. */
    fun tick() {
        val a = _state.value.account ?: return
        val t = now
        for (tr in a.transfers) advance(tr, t)
        // identity decision
        val k = a.kyc
        if (k.status == KycStatus.IN_REVIEW && k.decideAt in 1..t) {
            if (k.willReject) {
                mutateAccount { it.copy(kyc = it.kyc.copy(status = KycStatus.REJECTED, decidedAt = t, attemptsLeft = (it.kyc.attemptsLeft - 1).coerceAtLeast(0),
                    rejectReason = "We couldn't read your date of birth because of glare. It happens, and a retake usually fixes it.", willReject = false)) }
                notify(NoticeKind.ACCOUNT, "We need a clearer photo", "Retake your ID photo to finish verifying. Your details and selfie are saved.")
                emit(BackendEvent.KycDecided(false))
            } else {
                mutateAccount { it.copy(kyc = it.kyc.copy(status = KycStatus.APPROVED, level = KycLevel.STANDARD, decidedAt = t, docExpiry = "2031-05-12")) }
                notify(NoticeKind.ACCOUNT, "Identity verified", "You can send up to £5,000 per transfer.")
                emit(BackendEvent.KycDecided(true))
            }
        }
        if (k.enhancedRequested && k.level == KycLevel.STANDARD && k.decideAt in 1..t) {
            mutateAccount { it.copy(kyc = it.kyc.copy(level = KycLevel.ENHANCED, enhancedRequested = false)) }
            notify(NoticeKind.ACCOUNT, "Limits raised", "You can now send up to £25,000 per transfer and £100,000 a month.")
            emit(BackendEvent.Info("Limits raised", "You're now on Enhanced: up to £25,000 per transfer."))
        }
        // agent typing and case progress need no state changes; rate alerts are checked on refresh
    }

    private fun advance(tr: Transfer, t: Long) {
        when (tr.status) {
            TransferStatus.PROCESSING -> if (t >= tr.paidAt) updateTransfer(tr.id) { it.copy(status = TransferStatus.IN_PROGRESS) }
            TransferStatus.IN_PROGRESS -> when {
                tr.willHold && tr.holdAnsweredAt == 0L && t >= tr.holdAt -> {
                    updateTransfer(tr.id) { it.copy(status = TransferStatus.ON_HOLD, holdUntil = it.holdAt + 24 * 3_600_000L) }
                    val title = "Your transfer needs one answer"
                    val body = "Your ${Money.format(tr.sendMinor, tr.fromCur)} to ${tr.firstName} is paused until you answer a quick question."
                    notify(NoticeKind.TRANSFER, title, body, tr.id)
                    emit(BackendEvent.TransferUpdate(tr.id, title, body))
                }
                tr.willFail && t >= tr.failAt -> {
                    val reason = when (tr.payoutType) {
                        PayoutType.BANK -> "${tr.providerName} couldn't find an account numbered ${tr.payoutAccount}. The number may be mistyped or closed."
                        else -> "${tr.providerName} couldn't find a wallet for ${Text.formatPhone(tr.payoutAccount)}. The number may be mistyped or not registered for mobile money."
                    }
                    startRefund(tr.id, TransferStatus.FAILED, failReason = reason)
                    val title = "We couldn't deliver your transfer"
                    val body = "${Money.format(tr.totalMinor, tr.fromCur)} is on its way back to ${tr.fundingLabel}."
                    notify(NoticeKind.TRANSFER, title, body, tr.id)
                    emit(BackendEvent.TransferUpdate(tr.id, title, body))
                }
                !tr.willFail && t >= tr.deliveredAt -> {
                    updateTransfer(tr.id) { it.copy(status = TransferStatus.DELIVERED, providerRef = Ids.providerRef(it.providerId, it.deliveredAt)) }
                    if (tr.payoutType == PayoutType.ARRO) creditArroUser(tr)
                    val title = if (tr.payoutType == PayoutType.CASH) "Ready to collect" else "Delivered to ${tr.firstName}"
                    val body = when (tr.payoutType) {
                        PayoutType.CASH -> "${Money.format(tr.receiveMinor, tr.toCur)} is ready for ${tr.firstName} to collect with ID."
                        PayoutType.BANK -> "${Money.format(tr.receiveMinor, tr.toCur)} arrived in ${tr.firstName}'s ${tr.providerName} account."
                        PayoutType.ARRO -> "${Money.format(tr.receiveMinor, tr.toCur)} arrived in ${tr.firstName}'s Arro balance."
                        else -> "${Money.format(tr.receiveMinor, tr.toCur)} arrived in ${tr.firstName}'s ${tr.providerName} wallet."
                    }
                    notify(NoticeKind.TRANSFER, title, body, tr.id)
                    emit(BackendEvent.TransferUpdate(tr.id, title, body))
                }
            }
            TransferStatus.ON_HOLD -> if (tr.holdUntil in 1..t) {
                startRefund(tr.id, TransferStatus.CANCELLED, cancelReason = "No answer within 24 hours")
                notify(NoticeKind.TRANSFER, "Transfer refunded", "We didn't hear back about your transfer to ${tr.firstName}, so we're refunding it in full.", tr.id)
            }
            TransferStatus.FAILED, TransferStatus.CANCELLED -> if (tr.refundedAt == 0L && tr.refundDueAt in 1..t) {
                updateTransfer(tr.id) { it.copy(status = TransferStatus.REFUNDED, refundedAt = t) }
                val title = "Refund complete"
                val body = "${Money.format(tr.totalMinor, tr.fromCur)} is back on ${if (tr.fundingId == BALANCE) "your GBP balance" else "your " + tr.fundingLabel}."
                notify(NoticeKind.TRANSFER, title, body, tr.id)
                emit(BackendEvent.TransferUpdate(tr.id, title, body))
            }
            else -> Unit
        }
    }

    private fun creditArroUser(tr: Transfer) {
        // The recipient's Arro balance is on their own phone; nothing to change locally.
    }

    /** Completes everything that's waiting, for testing on a device. */
    fun fastForward() {
        val t = now
        mutateAccount { a ->
            a.copy(
                transfers = a.transfers.map { tr ->
                    when {
                        tr.status == TransferStatus.ON_HOLD -> tr
                        tr.isOpen -> tr.copy(paidAt = minOf(tr.paidAt, t), convertedAt = minOf(tr.convertedAt, t), handedAt = minOf(tr.handedAt, t),
                            holdAt = if (tr.willHold && tr.holdAnsweredAt == 0L) minOf(tr.holdAt, t) else tr.holdAt,
                            failAt = if (tr.willFail) minOf(tr.failAt, t) else 0, deliveredAt = if (tr.willFail) tr.deliveredAt else minOf(tr.deliveredAt, t))
                        tr.refundPending -> tr.copy(refundDueAt = minOf(tr.refundDueAt, t))
                        else -> tr
                    }
                },
                kyc = if (a.kyc.status == KycStatus.IN_REVIEW || a.kyc.enhancedRequested) a.kyc.copy(decideAt = minOf(a.kyc.decideAt, t)) else a.kyc,
            )
        }
        repeat(4) { tick() }
    }

    // ------------------------------------------------------------------ recipients

    suspend fun checkName(country: String, type: PayoutType, providerId: String, account: String, enteredName: String): NameCheck {
        net(1100)
        val provider = Catalog.providerName(providerId)
        val digits = account.filter { it.isDigit() }
        if (type != PayoutType.UPI && digits.endsWith("404")) {
            throw ArroException(if (type == PayoutType.BANK) "$provider has no account with that number. Check it with the recipient."
            else "$provider has no wallet registered to this number. Check it with the recipient.", "not_found")
        }
        val known = Seed.registeredNames[digits.takeLast(9)]
        val registered = when {
            known != null -> known
            digits.endsWith("999") -> Seed.mismatchName(country)
            else -> enteredName.trim().uppercase().replace(Regex("\\s+"), " ")
        }
        val matches = namesMatch(registered, enteredName)
        return NameCheck(registered, matches, provider)
    }

    private fun namesMatch(registered: String, entered: String): Boolean {
        val a = registered.uppercase().split(" ").filter { it.isNotBlank() }.toSet()
        val b = entered.uppercase().split(" ").filter { it.isNotBlank() }.toSet()
        return b.isNotEmpty() && a.containsAll(b.filter { it.length > 1 }) || a == b
    }

    fun addRecipient(name: String, country: String, relationship: String, city: String, method: PayoutMethod, nickname: String = ""): Recipient {
        val a = requireAccount()
        val r = Recipient(
            id = Ids.id(), name = Text.titleCase(name), country = country, relationship = relationship, nickname = nickname, city = city,
            color = (a.recipients.size % 6) + 1, createdAt = now, methods = listOf(method), defaultMethodId = method.id,
        )
        mutateAccount { it.copy(recipients = it.recipients + r) }
        return r
    }

    fun newPayoutMethod(type: PayoutType, providerId: String, account: String, accountName: String = "", accountType: String = "", usualReason: String = "", checked: Boolean = true) =
        PayoutMethod(Ids.id(), type, providerId, account, accountName, accountType, checked, if (checked) now else 0, usualReason)

    fun addPayoutMethod(recipientId: String, method: PayoutMethod, makeDefault: Boolean) = mutateAccount { a ->
        a.copy(recipients = a.recipients.map { r ->
            if (r.id != recipientId) r else r.copy(methods = r.methods + method, defaultMethodId = if (makeDefault || r.methods.isEmpty()) method.id else r.defaultMethodId)
        })
    }

    fun updateRecipient(id: String, block: (Recipient) -> Recipient) = mutateAccount { a ->
        a.copy(recipients = a.recipients.map { if (it.id == id) block(it) else it })
    }

    fun deleteRecipient(id: String) = mutateAccount { a -> a.copy(recipients = a.recipients.filterNot { it.id == id }) }

    fun toggleFavourite(id: String) = updateRecipient(id) { it.copy(favourite = !it.favourite) }

    fun removePayoutMethod(recipientId: String, methodId: String) = updateRecipient(recipientId) { r ->
        val left = r.methods.filterNot { it.id == methodId }
        r.copy(methods = left, defaultMethodId = if (r.defaultMethodId == methodId) left.firstOrNull()?.id ?: "" else r.defaultMethodId)
    }

    suspend fun findArroUser(query: String): ArroUser {
        net(700)
        val q = query.trim().lowercase().removePrefix("@")
        val digits = q.filter { it.isDigit() }
        return Seed.arroUsers.firstOrNull { it.handle == q || (digits.length >= 9 && it.phone.endsWith(digits.takeLast(9))) }
            ?: throw ArroException("We couldn't find an Arro user with that username or number.")
    }

    fun addArroRecipient(user: ArroUser): Recipient {
        val existing = requireAccount().recipients.firstOrNull { r -> r.methods.any { it.type == PayoutType.ARRO && it.account == "@" + user.handle } }
        if (existing != null) return existing
        return addRecipient(user.name, "GB", "", "", newPayoutMethod(PayoutType.ARRO, "arro", "@" + user.handle, user.name.uppercase()))
    }

    // ------------------------------------------------------------------ payment methods

    suspend fun addCard(number: String, expiry: String, cvv: String, holder: String, makeDefault: Boolean): PaymentMethod {
        val today = Text.localDate(now)
        Validation.cardNumber(number)?.let { throw ArroException(it, "number") }
        Validation.expiry(expiry, today)?.let { throw ArroException(it, "expiry") }
        val brand = Validation.brand(number)
        Validation.cvv(cvv, brand)?.let { throw ArroException(it, "cvv") }
        val digits = number.filter { it.isDigit() }
        val a = requireAccount()
        if (a.paymentMethods.any { it.kind == MethodKind.CARD && it.last4 == digits.takeLast(4) && it.brand == brand && it.status == MethodStatus.ACTIVE }) {
            throw ArroException("You've already added this card.", "number")
        }
        net(1000)
        val credit = Seed.isCreditBin(digits)
        val pm = PaymentMethod(
            id = Ids.id(), kind = MethodKind.CARD, brand = brand, credit = credit, last4 = digits.takeLast(4),
            expiry = expiry.filter { it.isDigit() }.let { it.take(2) + "/" + it.drop(2) }, issuer = Seed.issuerFor(digits),
            holder = holder.ifBlank { a.displayName }.uppercase(), status = MethodStatus.PENDING,
            isDefault = makeDefault || a.activeMethods.isEmpty(), addedAt = now,
        )
        mutateAccount { it.copy(paymentMethods = it.paymentMethods + pm) }
        return pm
    }

    /** Completes the bank's approval step for a newly added card. */
    suspend fun approveCard(id: String): PaymentMethod {
        net(1300)
        val pm = requireAccount().paymentMethod(id) ?: throw ArroException("Add the card again.")
        if (pm.last4 == "0002") {
            mutateAccount { a -> a.copy(paymentMethods = a.paymentMethods.filterNot { it.id == id }) }
            throw ArroException("${pm.issuer} declined this card. Try another card or pay by bank.", "declined")
        }
        mutateAccount { a ->
            a.copy(paymentMethods = a.paymentMethods.map {
                when {
                    it.id == id -> it.copy(status = MethodStatus.ACTIVE)
                    pm.isDefault -> it.copy(isDefault = false)
                    else -> it
                }
            })
        }
        notify(NoticeKind.ACCOUNT, "Card added", "${pm.label} is ready to pay for transfers and top-ups.")
        return requireAccount().paymentMethod(id)!!
    }

    fun cancelPendingMethod(id: String) = mutateAccount { a -> a.copy(paymentMethods = a.paymentMethods.filterNot { it.id == id && it.status == MethodStatus.PENDING }) }

    suspend fun connectBank(bankId: String): PaymentMethod {
        val bank = Catalog.ukBank(bankId) ?: throw ArroException("Choose your bank.")
        net(1500)
        val a = requireAccount()
        val pm = PaymentMethod(
            id = Ids.id(), kind = MethodKind.BANK, last4 = Ids.digits(4), issuer = bank.name, holder = a.displayName.uppercase(),
            bankId = bankId, status = MethodStatus.ACTIVE, isDefault = a.activeMethods.isEmpty(), addedAt = now,
        )
        mutateAccount { it.copy(paymentMethods = it.paymentMethods + pm) }
        notify(NoticeKind.ACCOUNT, "Bank connected", "${bank.name} •• ${pm.last4} is ready for transfers and top-ups.")
        return pm
    }

    fun setDefaultMethod(id: String) = mutateAccount { a -> a.copy(paymentMethods = a.paymentMethods.map { it.copy(isDefault = it.id == id) }) }

    fun removeMethod(id: String) = mutateAccount { a ->
        val left = a.paymentMethods.filterNot { it.id == id }
        val hasDefault = left.any { it.isDefault }
        a.copy(paymentMethods = left.mapIndexed { i, m -> if (!hasDefault && i == 0) m.copy(isDefault = true) else m })
    }

    // ------------------------------------------------------------------ wallet

    suspend fun topUp(cur: Currency, minor: Long, sourceId: String): LedgerEntry {
        if (minor < 100) throw ArroException("Add at least ${Money.format(100, cur)}.")
        if (minor > 1_000_000) throw ArroException("You can add up to ${Money.format(1_000_000, cur, false)} at a time.")
        val a = requireAccount()
        if (a.frozen) throw ArroException("Your account is frozen. Unfreeze it in Security first.")
        val pm = a.paymentMethod(sourceId) ?: throw ArroException("Choose where the money comes from.")
        net(1300)
        val e = LedgerEntry(Ids.id(), LedgerType.TOP_UP, cur.code, minor, title = "Added from ${pm.issuer}",
            subtitle = if (pm.kind == MethodKind.BANK) "Bank transfer" else pm.label, createdAt = now)
        mutateAccount { acc ->
            acc.copy(wallets = acc.wallets.map { w -> if (w.currency == cur.code) w.copy(balanceMinor = w.balanceMinor + minor) else w }, ledger = listOf(e) + acc.ledger)
        }
        notify(NoticeKind.ACCOUNT, "Money added", "${Money.format(minor, cur)} from ${pm.issuer} is in your ${cur.code} balance.")
        return e
    }

    suspend fun convert(from: Currency, to: Currency, minor: Long): LedgerEntry {
        if (from == to) throw ArroException("Choose two different currencies.")
        if (minor <= 0) throw ArroException("Enter an amount to convert.")
        val a = requireAccount()
        val wf = a.wallet(from) ?: throw ArroException("You don't have a ${from.code} balance.")
        if (wf.balanceMinor < minor) throw ArroException("You have ${Money.format(wf.balanceMinor, from)} in ${from.code}.", "insufficient")
        if (a.wallet(to) == null) throw ArroException("Open a ${to.code} balance first.")
        net(900)
        val rate = conversionRate(from, to)
        val out = Money.convert(minor, from, to, rate)
        val e = LedgerEntry(Ids.id(), LedgerType.CONVERSION, from.code, -minor, to.code, out, rate, "${from.code} to ${to.code}", "Converted", now)
        mutateAccount { acc ->
            acc.copy(wallets = acc.wallets.map { w ->
                when (w.currency) {
                    from.code -> w.copy(balanceMinor = w.balanceMinor - minor)
                    to.code -> w.copy(balanceMinor = w.balanceMinor + out)
                    else -> w
                }
            }, ledger = listOf(e) + acc.ledger)
        }
        return e
    }

    suspend fun openCurrency(cur: Currency) {
        val a = requireAccount()
        if (a.wallet(cur) != null) throw ArroException("You already have a ${cur.code} balance.")
        net(800)
        val w = when (cur) {
            Currency.EUR -> Wallet(cur.code, 0, iban = Ids.iban(), openedAt = now)
            Currency.USD -> Wallet(cur.code, 0, sortCode = "026073150", accountNumber = Ids.digits(10), openedAt = now)
            else -> Wallet(cur.code, 0, Ids.sortCode(), Ids.accountNumber(), openedAt = now)
        }
        mutateAccount { it.copy(wallets = it.wallets + w) }
        notify(NoticeKind.ACCOUNT, "${cur.displayName} balance opened", "You can now hold, receive and convert ${cur.code}.")
    }

    suspend fun closeCurrency(cur: Currency) {
        val w = requireAccount().wallet(cur) ?: throw ArroException("You don't have a ${cur.code} balance.")
        if (w.main) throw ArroException("Your main GBP account can't be closed here.")
        if (w.balanceMinor != 0L) throw ArroException("Convert or send your ${Money.format(w.balanceMinor, cur)} first. Balances must be zero to close.")
        net(700)
        mutateAccount { a -> a.copy(wallets = a.wallets.filterNot { it.currency == cur.code }) }
    }

    /** Total of all balances in GBP at today's mid-market rates. */
    fun totalInGbp(a: Account): Long = a.wallets.sumOf { w ->
        if (w.currency == "GBP") w.balanceMinor else Money.convert(w.balanceMinor, w.cur, Currency.GBP, 1 / midRate(w.cur))
    }

    fun inGbp(minor: Long, cur: Currency): Long = if (cur == Currency.GBP) minor else Money.convert(minor, cur, Currency.GBP, 1 / midRate(cur))

    /** Transfers and ledger entries, newest first. */
    fun feed(a: Account): List<FeedItem> =
        (a.transfers.map { FeedItem.Sent(it) } + a.ledger.filter { it.type != LedgerType.REFUND }.map { FeedItem.Entry(it) }).sortedByDescending { it.at }

    fun feedFor(a: Account, cur: Currency, fromMs: Long = 0, toMs: Long = Long.MAX_VALUE): List<FeedItem> = feed(a).filter { item ->
        item.at in fromMs..toMs && when (item) {
            is FeedItem.Sent -> item.transfer.from == cur.code && (item.transfer.fundingId == BALANCE || cur == Currency.GBP)
            is FeedItem.Entry -> item.entry.currency == cur.code || item.entry.counterCurrency == cur.code
        }
    }

    fun recordStatement(cur: Currency, fromIso: String, toIso: String, format: String, count: Int) = mutateAccount { a ->
        a.copy(statements = listOf(StatementRecord(Ids.id(), cur.code, fromIso, toIso, format, count, now)) + a.statements.take(11))
    }

    // ------------------------------------------------------------------ rate alerts

    fun createRateAlert(to: Currency, target: Double, push: Boolean, autoRecipientId: String, autoMinor: Long): RateAlert {
        val alert = RateAlert(Ids.id(), to.code, target, now, now + 30 * 86_400_000L, push, autoRecipientId, autoMinor)
        mutateAccount { a -> a.copy(rateAlerts = a.rateAlerts.filterNot { it.to == to.code && it.triggeredAt == 0L } + alert) }
        checkRateAlerts()
        return alert
    }

    fun deleteRateAlert(id: String) = mutateAccount { a -> a.copy(rateAlerts = a.rateAlerts.filterNot { it.id == id }) }

    private fun checkRateAlerts() {
        val a = _state.value.account ?: return
        for (al in a.rateAlerts.filter { it.triggeredAt == 0L && it.expiresAt > now }) {
            val cur = Currency.of(al.to)
            val r = customerRate(cur)
            if (r >= al.target) {
                mutateAccount { acc -> acc.copy(rateAlerts = acc.rateAlerts.map { if (it.id == al.id) it.copy(triggeredAt = now) else it }) }
                val title = "Rate alert reached"
                val body = "GBP → ${cur.code} is ${Money.rate(r)}, above your ${Money.rate(al.target)} target. Your quote is ready to lock."
                notify(NoticeKind.RATE, title, body)
                if (al.push) emit(BackendEvent.RateHit(title, body))
                if (al.autoSendRecipientId.isNotBlank() && al.autoSendMinor > 0) autoSend(al, cur)
            }
        }
    }

    private fun autoSend(al: RateAlert, cur: Currency) {
        val a = _state.value.account ?: return
        val r = a.recipient(al.autoSendRecipientId) ?: return
        val m = r.defaultMethod ?: return
        val q = quote(cur, al.autoSendMinor, m.type, r.country, BALANCE)
        scope.launch {
            runCatching { createTransfer(TransferDraft(r.id, m.id, q, BALANCE, "Family support")) }
                .onSuccess { emit(BackendEvent.TransferUpdate(it.id, "Sent automatically", "Your rate was reached, so we sent ${Money.format(q.receiveMinor, cur)} to ${r.firstName}.")) }
                .onFailure { notify(NoticeKind.RATE, "Automatic send didn't go", it.message ?: "Check your balance and send it yourself.") }
        }
    }

    // ------------------------------------------------------------------ support

    fun sendChat(text: String, attachment: String = "") {
        val clean = text.trim()
        if (clean.isEmpty() && attachment.isEmpty()) return
        val a = requireAccount()
        val first = a.chat.isEmpty()
        val msg = ChatMessage(Ids.id(), false, clean, now, attachment)
        mutateAccount { it.copy(chat = it.chat + msg) }
        scope.launch {
            delay(if (latency) 1_800 else 0)
            val reply = Agent.reply(clean, attachment, requireAccount(), first)
            mutateAccount { it.copy(chat = it.chat + reply) }
            emit(BackendEvent.ChatReply(reply.text))
        }
    }

    fun startChatAbout(transferId: String?) {
        val a = requireAccount()
        if (a.chat.isNotEmpty()) return
        val tr = a.transfer(transferId)
        val greet = ChatMessage(Ids.id(), true, Agent.greeting(a, tr), now)
        mutateAccount { it.copy(chat = listOf(greet)) }
    }

    suspend fun openCase(transferId: String, topic: String, details: String, replyBy: String, evidence: List<String>): SupportCase {
        if (details.trim().length < 10) throw ArroException("Tell us a little more, at least 10 characters.")
        net(1100)
        val t = now
        val c = SupportCase(
            id = Ids.id(), number = Ids.digits(5), transferId = transferId, topic = topic, details = details.trim(), replyBy = replyBy,
            evidence = evidence, createdAt = t,
            assignedAt = t + if (fast) 20_000 else 2 * 60_000, investigatingAt = t + if (fast) 45_000 else 4 * 60_000,
            resolvedAt = t + if (fast) 120_000 else 24 * 3_600_000L,
        )
        mutateAccount { it.copy(cases = listOf(c) + it.cases) }
        notify(NoticeKind.ACCOUNT, "Case ${c.number} opened", "We'll update you within 1 working day.")
        return c
    }

    fun addCaseDetails(caseId: String, text: String) = mutateAccount { a ->
        a.copy(cases = a.cases.map { if (it.id == caseId) it.copy(extraDetails = it.extraDetails + text.trim()) else it })
    }

    // ------------------------------------------------------------------ profile and settings

    suspend fun updateEmail(email: String) {
        Validation.email(email)?.let { throw ArroException(it) }
        net()
        mutateAccount { it.copy(email = email.trim().lowercase(), emailVerified = false) }
    }

    suspend fun updatePhone(digits: String) {
        Validation.ukMobile(digits)?.let { throw ArroException(it) }
        net()
        mutateAccount { it.copy(phone = Validation.normaliseUkMobile(digits)) }
        notify(NoticeKind.SECURITY, "Mobile number changed", "Security codes now go to ${Text.formatPhone(Validation.normaliseUkMobile(digits))}.")
    }

    private var phoneCode: PendingCode? = null

    /** Texts a code to a new mobile number before it replaces the current one. */
    suspend fun sendPhoneChangeCode(digits: String) {
        Validation.ukMobile(digits)?.let { throw ArroException(it) }
        val phone = Validation.normaliseUkMobile(digits)
        if (phone == account?.phone) throw ArroException("That's already your mobile number.")
        net(600)
        val code = Ids.code()
        phoneCode = PendingCode(code, phone, now + 10 * 60_000)
        emit(BackendEvent.Code(code, "Your Arro code is $code. It confirms ${Text.formatPhone(phone)} as your new number.", phone))
    }

    suspend fun confirmPhoneChange(code: String) {
        val p = phoneCode ?: throw ArroException("Ask for a new code.")
        net(450)
        if (now > p.expires) throw ArroException("That code has expired. Ask for a new one.")
        p.attempts++
        if (p.attempts > 5) throw ArroException("Too many tries. Ask for a new code.")
        if (code != p.code) throw ArroException("That code isn't right. Check the SMS and try again.")
        phoneCode = null
        mutateAccount { it.copy(phone = p.target) }
        notify(NoticeKind.SECURITY, "Mobile number changed", "Security codes now go to ${Text.formatPhone(p.target)}.")
    }

    /** Sends a verification link to the account email; the simulated inbox "clicks" it a few seconds later. */
    fun sendEmailVerification() {
        val email = account?.email ?: return
        emit(BackendEvent.Info("Verification link sent", "Check $email and tap the link."))
        scope.launch {
            delay(if (latency) 4_000 else 0)
            mutateAccount { it.copy(emailVerified = true) }
            emit(BackendEvent.Info("Email verified", "$email is confirmed."))
        }
    }

    suspend fun updateAddress(address: Address) {
        net()
        mutateAccount { it.copy(profile = it.profile.copy(address = address)) }
    }

    fun updateSettings(block: (Settings) -> Settings) = mutateAccount { it.copy(settings = block(it.settings)) }
    fun updateSecurity(block: (SecuritySettings) -> SecuritySettings) = mutateAccount { it.copy(security = block(it.security)) }

    fun signOutDevice(id: String) {
        mutateAccount { a -> a.copy(devices = a.devices.filterNot { it.id == id && !it.current }) }
        notify(NoticeKind.SECURITY, "Device signed out", "We signed out a device from your account.")
    }

    fun setFrozen(on: Boolean) {
        mutateAccount { it.copy(frozen = on) }
        notify(NoticeKind.SECURITY, if (on) "Account frozen" else "Account unfrozen",
            if (on) "Sending and card payments are blocked until you unfreeze." else "You can send money again.")
    }

    fun pauseAccount() {
        mutateAccount { it.copy(paused = true) }
        logout()
    }

    suspend fun closeAccount() {
        val a = requireAccount()
        if (a.wallets.any { it.balanceMinor != 0L }) throw ArroException("Your balances must be zero before you close your account.")
        if (a.transfers.any { it.isOpen }) throw ArroException("Wait for your transfers in progress to finish first.")
        net(1000)
        _unlocked.value = false
        mutate { AppState(onboardingSeen = true, rates = it.rates, demo = it.demo) }
    }

    fun markAllRead() = mutateAccount { a -> a.copy(notifications = a.notifications.map { it.copy(read = true) }) }
    fun markRead(id: String) = mutateAccount { a -> a.copy(notifications = a.notifications.map { if (it.id == id) it.copy(read = true) else it }) }
    fun dismissSetup() = mutateAccount { it.copy(setupDismissed = true) }
    fun notifyWhenAvailable(country: String) = mutate { it.copy(notifyCountries = (it.notifyCountries + country).distinct()) }

    fun exportData(): String = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(requireAccount().copy(passwordHash = "•", passcodeHash = "•"))

    // ------------------------------------------------------------------ demo controls

    fun setDemo(block: (DemoSettings) -> DemoSettings) = mutate { it.copy(demo = block(it.demo)) }

    fun clearSendPause() = mutateAccount { it.copy(sendPauseUntil = 0) }

    /** Signs in with the sample account (Eric Nshuti), replacing any data on this device. */
    fun loadDemoAccount() {
        mutate { it.copy(account = Seed.account(now), signedIn = true, onboardingSeen = true) }
        _unlocked.value = true
    }

    fun resetEverything() {
        _unlocked.value = false
        _registration.value = Registration()
        mutate { AppState(rates = it.rates) }
    }

    private fun hash(salt: String, value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(("arro:$salt:$value").toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    internal fun hashFor(salt: String, value: String) = hash(salt, value)

    companion object {
        const val BALANCE = "balance"
        const val HOLD_THRESHOLD = 100_000L
        var DEVICE_NAME = "This phone"
    }
}

/** Random identifiers in the formats the designs show. */
object Ids {
    private const val ALPHA = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    private val rnd = java.security.SecureRandom()

    fun id(): String = (1..12).map { ALPHA[rnd.nextInt(ALPHA.length)] }.joinToString("")
    fun short(n: Int): String = (1..n).map { ALPHA[rnd.nextInt(ALPHA.length)] }.joinToString("")
    fun digits(n: Int): String = (1..n).map { '0' + rnd.nextInt(10) }.joinToString("")
    fun code(): String = digits(6).let { if (it.toSet().size == 1) "482917" else it }
    fun reference(): String = "ARR-${short(4)}-${short(4)}"
    fun refundReference(ref: String): String = "RFD-${ref.takeLast(4)}-${digits(4)}"
    fun sortCode(): String = "04-${digits(2)}-${digits(2)}"
    fun accountNumber(): String = digits(8)
    fun iban(): String = "GB${digits(2)} ARRO ${digits(4)} ${digits(4)} ${digits(4)} ${digits(2)}"
    fun providerRef(providerId: String, at: Long): String {
        val p = when {
            providerId.startsWith("mtn") -> "MP"
            providerId.startsWith("mpesa") -> "QK"
            providerId.startsWith("airtel") -> "AM"
            else -> "PX"
        }
        val d = Text.localDate(at)
        return "$p${d.year % 100}${"%02d".format(d.monthValue)}${"%02d".format(d.dayOfMonth)}.${Text.time(at).replace(":", "")}.C${digits(5)}"
    }

}
