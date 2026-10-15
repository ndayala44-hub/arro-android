package app.arro.core

import kotlinx.serialization.Serializable

/** Everything the simulated backend knows, persisted as one JSON document on the device. */
@Serializable
data class AppState(
    val version: Int = 1,
    val onboardingSeen: Boolean = false,
    val account: Account? = null,
    /** True while a user is signed in on this device (the app may still be locked). */
    val signedIn: Boolean = false,
    val rates: RatesSnapshot = RatesSnapshot(),
    val demo: DemoSettings = DemoSettings(),
    val notifyCountries: List<String> = emptyList(),
)

@Serializable
data class DemoSettings(
    val fast: Boolean = true,
    /** Payout provider that is "briefly offline", or empty. */
    val outageProviderId: String = "",
    /** Force the next identity check to ask for a retake. */
    val rejectNextKyc: Boolean = false,
)

@Serializable
data class RatesSnapshot(
    /** Mid-market rates: units of each currency per 1 GBP. */
    val mid: Map<String, Double> = emptyMap(),
    val fetchedAt: Long = 0,
    val source: String = "",
    /** The previous day's rates, used for 24-hour change. */
    val previous: Map<String, Double> = emptyMap(),
    val previousAt: Long = 0,
)

@Serializable
data class Account(
    val id: String,
    val createdAt: Long,
    val residence: String = "GB",
    val phone: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val passwordHash: String = "",
    val passcodeHash: String = "",
    val biometricEnabled: Boolean = false,
    val marketingOptIn: Boolean = false,
    val username: String = "",
    val profile: Profile = Profile(),
    val kyc: Kyc = Kyc(),
    val wallets: List<Wallet> = emptyList(),
    val recipients: List<Recipient> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val rateAlerts: List<RateAlert> = emptyList(),
    val cases: List<SupportCase> = emptyList(),
    val chat: List<ChatMessage> = emptyList(),
    val statements: List<StatementRecord> = emptyList(),
    val devices: List<Device> = emptyList(),
    val settings: Settings = Settings(),
    val security: SecuritySettings = SecuritySettings(),
    val referral: Referral = Referral(),
    val frozen: Boolean = false,
    val paused: Boolean = false,
    /** New transfers are blocked until this time (set after a password reset). */
    val sendPauseUntil: Long = 0,
    /** First transfer up to £1,000 has no Arro fee. */
    val firstTransferFree: Boolean = true,
    val setupDismissed: Boolean = false,
    val demo: Boolean = false,
) {
    val displayName: String get() = profile.displayName.ifBlank { "there" }
    val firstName: String get() = profile.firstNames.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"
    val initials: String get() = profile.initials
    fun wallet(cur: Currency): Wallet? = wallets.firstOrNull { it.currency == cur.code }
    fun recipient(id: String?): Recipient? = recipients.firstOrNull { it.id == id }
    fun transfer(id: String?): Transfer? = transfers.firstOrNull { it.id == id }
    fun paymentMethod(id: String?): PaymentMethod? = paymentMethods.firstOrNull { it.id == id }
    val activeMethods: List<PaymentMethod> get() = paymentMethods.filter { it.status == MethodStatus.ACTIVE }
    val unreadCount: Int get() = notifications.count { !it.read }
    val verified: Boolean get() = kyc.status == KycStatus.APPROVED
}

@Serializable
data class Profile(
    val firstNames: String = "",
    val lastName: String = "",
    val dob: String = "",          // yyyy-MM-dd
    val nationality: String = "",
    val occupation: String = "",
    val address: Address? = null,
) {
    val displayName: String get() = listOf(firstNames.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")
    val initials: String get() = Text.initials(displayName.ifBlank { "Arro" })
}

@Serializable
data class Address(val line1: String, val city: String, val postcode: String, val country: String = "United Kingdom") {
    val oneLine: String get() = "$line1, $city $postcode"
}

@Serializable
enum class KycStatus { NOT_STARTED, IN_REVIEW, APPROVED, REJECTED }

@Serializable
enum class KycLevel(val label: String, val perTransferMinor: Long, val perMonthMinor: Long, val perYearMinor: Long) {
    NONE("Not verified", 0, 0, 0),
    STANDARD("Standard", 500_000, 2_500_000, 10_000_000),
    ENHANCED("Enhanced", 2_500_000, 10_000_000, 50_000_000),
}

@Serializable
enum class DocType(val label: String, val sides: Int) {
    PASSPORT("Passport", 1), DRIVING_LICENCE("Driving licence", 2), ID_CARD("National identity card", 2)
}

@Serializable
data class Kyc(
    val status: KycStatus = KycStatus.NOT_STARTED,
    val level: KycLevel = KycLevel.NONE,
    val docType: DocType = DocType.PASSPORT,
    val docCountry: String = "United Kingdom",
    val docExpiry: String = "",
    val submittedAt: Long = 0,
    val decideAt: Long = 0,
    val decidedAt: Long = 0,
    val rejectReason: String = "",
    val attemptsLeft: Int = 3,
    val willReject: Boolean = false,
    val enhancedRequested: Boolean = false,
)

@Serializable
data class Wallet(
    val currency: String,
    val balanceMinor: Long = 0,
    val sortCode: String = "",
    val accountNumber: String = "",
    val iban: String = "",
    val main: Boolean = false,
    val openedAt: Long = 0,
) {
    val cur: Currency get() = Currency.of(currency)
    val last4: String get() = (if (accountNumber.isNotBlank()) accountNumber else iban).filter { it.isDigit() }.takeLast(4)
}

@Serializable
data class Recipient(
    val id: String,
    val name: String,
    val country: String,
    val relationship: String = "",
    val nickname: String = "",
    val city: String = "",
    val color: Int = 1,
    val favourite: Boolean = false,
    val smsOnArrival: Boolean = true,
    val createdAt: Long = 0,
    val methods: List<PayoutMethod> = emptyList(),
    val defaultMethodId: String = "",
) {
    val initials: String get() = Text.initials(name)
    val firstName: String get() = name.trim().split(" ").first()
    val defaultMethod: PayoutMethod? get() = methods.firstOrNull { it.id == defaultMethodId } ?: methods.firstOrNull()
    fun method(id: String?): PayoutMethod? = methods.firstOrNull { it.id == id }
    fun methodFor(type: PayoutType): PayoutMethod? =
        (if (defaultMethod?.type == type) defaultMethod else null) ?: methods.firstOrNull { it.type == type }
}

@Serializable
data class PayoutMethod(
    val id: String,
    val type: PayoutType,
    val providerId: String,
    /** Phone number in E.164 for mobile money, account number for banks, UPI ID, or @username. */
    val account: String,
    val accountName: String = "",
    val accountType: String = "",
    val nameChecked: Boolean = false,
    val checkedAt: Long = 0,
    val usualReason: String = "",
) {
    val providerName: String get() = Catalog.providerName(providerId)
    /** "+250 788 ••• 412" or "•• 7731". */
    val masked: String get() = when (type) {
        PayoutType.MOBILE_MONEY, PayoutType.EWALLET -> Text.maskPhone(account)
        PayoutType.BANK -> "•• " + account.takeLast(4)
        else -> account
    }
    val full: String get() = when (type) {
        PayoutType.MOBILE_MONEY, PayoutType.EWALLET -> Text.formatPhone(account)
        PayoutType.BANK -> account
        else -> account
    }
}

@Serializable
enum class MethodKind { CARD, BANK }

@Serializable
enum class MethodStatus { PENDING, ACTIVE }

@Serializable
enum class CardBrand(val label: String) { VISA("Visa"), MASTERCARD("Mastercard"), AMEX("American Express"), UNKNOWN("Card") }

@Serializable
data class PaymentMethod(
    val id: String,
    val kind: MethodKind,
    val brand: CardBrand = CardBrand.UNKNOWN,
    val credit: Boolean = false,
    val last4: String,
    val expiry: String = "",
    val issuer: String = "",
    val holder: String = "",
    val bankId: String = "",
    val status: MethodStatus = MethodStatus.ACTIVE,
    val isDefault: Boolean = false,
    val addedAt: Long = 0,
) {
    /** "Visa debit •• 4821", "Monzo •• 6402". */
    val label: String get() = when (kind) {
        MethodKind.CARD -> "${brand.label} ${if (credit) "credit" else "debit"} •• $last4"
        MethodKind.BANK -> "$issuer •• $last4"
    }
    val shortLabel: String get() = when (kind) {
        MethodKind.CARD -> "${brand.label} •• $last4"
        MethodKind.BANK -> "$issuer •• $last4"
    }
}

@Serializable
enum class TransferStatus { PROCESSING, IN_PROGRESS, ON_HOLD, DELIVERED, FAILED, CANCELLED, REFUNDED }

@Serializable
data class Transfer(
    val id: String,
    val reference: String,
    val recipientId: String,
    val recipientName: String,
    val recipientColor: Int = 1,
    val country: String,
    val from: String = "GBP",
    val to: String,
    val sendMinor: Long,
    val feeMinor: Long,
    val fundingFeeMinor: Long = 0,
    val totalMinor: Long,
    val rate: Double,
    val midRate: Double,
    val receiveMinor: Long,
    val payoutType: PayoutType,
    val providerId: String,
    val payoutAccount: String,
    /** Payment method id, or "balance". */
    val fundingId: String,
    val fundingLabel: String,
    val reason: String = "",
    val createdAt: Long,
    val status: TransferStatus = TransferStatus.PROCESSING,
    // planned milestones (epoch ms); 0 when not planned
    val paidAt: Long = 0,
    val convertedAt: Long = 0,
    val handedAt: Long = 0,
    val deliveredAt: Long = 0,
    // outcomes
    val holdAt: Long = 0,
    val holdUntil: Long = 0,
    val holdRelationship: String = "",
    val holdSource: String = "",
    val holdAnsweredAt: Long = 0,
    val failAt: Long = 0,
    val failReason: String = "",
    val cancelledAt: Long = 0,
    val cancelReason: String = "",
    val refundStartedAt: Long = 0,
    val refundDueAt: Long = 0,
    val refundedAt: Long = 0,
    val refundReference: String = "",
    val providerRef: String = "",
    /** Flags decided at creation from the simulated rails. */
    val willHold: Boolean = false,
    val willFail: Boolean = false,
    val repeatMonthly: Boolean = false,
) {
    val fromCur: Currency get() = Currency.of(from)
    val toCur: Currency get() = Currency.of(to)
    val providerName: String get() = Catalog.providerName(providerId)
    val isOpen: Boolean get() = status == TransferStatus.PROCESSING || status == TransferStatus.IN_PROGRESS || status == TransferStatus.ON_HOLD
    val refundPending: Boolean get() = (status == TransferStatus.FAILED || status == TransferStatus.CANCELLED) && refundedAt == 0L
    val firstName: String get() = recipientName.trim().split(" ").first()
}

@Serializable
enum class LedgerType { TOP_UP, CONVERSION, REFUND, REWARD }

@Serializable
data class LedgerEntry(
    val id: String,
    val type: LedgerType,
    val currency: String,
    val amountMinor: Long,
    val counterCurrency: String = "",
    val counterMinor: Long = 0,
    val rate: Double = 0.0,
    val title: String,
    val subtitle: String = "",
    val createdAt: Long,
)

@Serializable
enum class NoticeKind { TRANSFER, RATE, SECURITY, ACCOUNT, STATEMENT }

@Serializable
data class AppNotification(
    val id: String,
    val kind: NoticeKind,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean = false,
    val transferId: String = "",
)

@Serializable
data class RateAlert(
    val id: String,
    val to: String,
    val target: Double,
    val createdAt: Long,
    val expiresAt: Long,
    val push: Boolean = true,
    val autoSendRecipientId: String = "",
    val autoSendMinor: Long = 0,
    val triggeredAt: Long = 0,
)

@Serializable
enum class CaseStatus(val label: String) { OPEN("Open"), IN_REVIEW("In review"), RESOLVED("Resolved") }

@Serializable
data class CaseEvent(val title: String, val subtitle: String, val at: Long)

@Serializable
data class SupportCase(
    val id: String,
    val number: String,
    val transferId: String,
    val topic: String,
    val details: String,
    val replyBy: String,
    val evidence: List<String> = emptyList(),
    val createdAt: Long,
    val assignedAt: Long,
    val investigatingAt: Long,
    val resolvedAt: Long,
    val extraDetails: List<String> = emptyList(),
) {
    fun status(now: Long): CaseStatus = when {
        now >= resolvedAt -> CaseStatus.RESOLVED
        now >= assignedAt -> CaseStatus.IN_REVIEW
        else -> CaseStatus.OPEN
    }
}

@Serializable
data class ChatMessage(
    val id: String,
    val fromAgent: Boolean,
    val text: String,
    val at: Long,
    val attachment: String = "",
    val attachmentNote: String = "",
    val quickReplies: List<String> = emptyList(),
)

@Serializable
data class StatementRecord(
    val id: String,
    val currency: String,
    val from: String,      // yyyy-MM-dd
    val to: String,
    val format: String,    // PDF or CSV
    val count: Int,
    val createdAt: Long,
)

@Serializable
data class Device(val id: String, val name: String, val location: String, val lastActive: Long, val current: Boolean = false, val laptop: Boolean = false)

@Serializable
enum class Appearance { SYSTEM, LIGHT, DARK }

@Serializable
data class Settings(
    val transferUpdates: Boolean = true,
    val smsRecipients: Boolean = true,
    val rateAlerts: Boolean = true,
    val statements: Boolean = true,
    val news: Boolean = false,
    val quietHours: Boolean = true,
    val quietFrom: String = "22:00",
    val quietTo: String = "07:00",
    val language: String = "English (UK)",
    val appearance: Appearance = Appearance.LIGHT,
    val analytics: Boolean = true,
    val marketing: Boolean = false,
)

@Serializable
data class SecuritySettings(
    val confirmNewDevices: Boolean = true,
    val passcodeOverThreshold: Boolean = true,
    val passcodeChangedAt: Long = 0,
    val passwordChangedAt: Long = 0,
)

@Serializable
data class Referral(val code: String = "", val friendsJoined: Int = 0, val earnedMinor: Long = 0)

/** Price of a transfer, valid until [lockedUntil]. */
data class Quote(
    val from: Currency,
    val to: Currency,
    val sendMinor: Long,
    val receiveMinor: Long,
    val rate: Double,
    val mid: Double,
    val feeMinor: Long,
    val fundingFeeMinor: Long,
    val marginMinor: Long,
    val lockedUntil: Long,
    val payoutType: PayoutType,
    val feeWaived: Boolean,
) {
    val totalMinor: Long get() = sendMinor + feeMinor + fundingFeeMinor
    val totalCostMinor: Long get() = feeMinor + fundingFeeMinor + marginMinor
    val marginPercent: Double get() = if (mid == 0.0) 0.0 else (mid - rate) / mid * 100
}

/** A row in the activity feed: either a transfer or a ledger entry. */
sealed class FeedItem {
    abstract val at: Long
    data class Sent(val transfer: Transfer) : FeedItem() { override val at: Long get() = transfer.createdAt }
    data class Entry(val entry: LedgerEntry) : FeedItem() { override val at: Long get() = entry.createdAt }
}

/** Events the app turns into system notifications or in-app banners. */
sealed class BackendEvent {
    data class Code(val code: String, val purpose: String, val to: String) : BackendEvent()
    data class TransferUpdate(val transferId: String, val title: String, val body: String) : BackendEvent()
    data class RateHit(val title: String, val body: String) : BackendEvent()
    data class KycDecided(val approved: Boolean) : BackendEvent()
    data class ChatReply(val text: String) : BackendEvent()
    data class Info(val title: String, val body: String) : BackendEvent()
}

/** Thrown by the simulated API for anything the user can fix. [message] is shown as is. */
class ArroException(message: String, val code: String = "error") : Exception(message)
