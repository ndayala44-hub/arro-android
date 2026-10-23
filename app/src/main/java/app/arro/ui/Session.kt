package app.arro.ui

import app.arro.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.arro.core.Currency
import app.arro.core.DocType
import app.arro.core.NameCheck
import app.arro.core.PayoutType
import app.arro.core.Quote
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf

/** The transfer being put together across the send screens. */
class SendDraft {
    var country by mutableStateOf("RW")
    var amountText by mutableStateOf("250")
    var payoutType by mutableStateOf(PayoutType.MOBILE_MONEY)
    var recipientId by mutableStateOf<String?>(null)
    var methodId by mutableStateOf<String?>(null)
    var fundingId by mutableStateOf<String?>(null)
    var reason by mutableStateOf(str(R.string.session_family_support))
    var quote by mutableStateOf<Quote?>(null)
    /** Set once the transfer is created, for the processing and success screens. */
    var transferId by mutableStateOf<String?>(null)
    var arroUser by mutableStateOf(false)

    fun reset(country: String = "RW") {
        this.country = country
        amountText = "250"
        payoutType = app.arro.core.Catalog.country(country).options.first().type
        recipientId = null
        methodId = null
        fundingId = null
        reason = str(R.string.session_family_support)
        quote = null
        transferId = null
        arroUser = false
    }
}

/** Details for a recipient being added, shared by the add, validation and name-check screens. */
class RecipientDraft {
    var country by mutableStateOf("RW")
    var type by mutableStateOf(PayoutType.MOBILE_MONEY)
    var providerId by mutableStateOf("mtn_rw")
    var number by mutableStateOf("")
    var name by mutableStateOf("")
    var relationship by mutableStateOf("")
    var city by mutableStateOf("")
    var accountType by mutableStateOf(str(R.string.session_savings))
    var check by mutableStateOf<NameCheck?>(null)
    /** Where to go once saved: back into the send flow, or to the recipient's profile. */
    var forSend by mutableStateOf(false)

    fun reset(country: String = "RW", type: PayoutType = PayoutType.MOBILE_MONEY, forSend: Boolean = false) {
        this.country = country
        this.type = type
        providerId = app.arro.core.Catalog.providersFor(country, type).firstOrNull()?.id ?: ""
        number = ""; name = ""; relationship = ""; city = ""; accountType = str(R.string.session_savings)
        check = null
        this.forSend = forSend
    }
}

class KycDraft {
    var docType by mutableStateOf(DocType.PASSPORT)
    var docCountry by mutableStateOf(str(R.string.session_united_kingdom))
    var photoPath by mutableStateOf<String?>(null)
    var quality by mutableFloatStateOf(0f)
    var qualityNotes by mutableStateOf<List<Pair<String, Boolean>>>(emptyList())
    var selfieDone by mutableStateOf(false)
}

/** Everything the UI keeps between screens for this process. */
class Session {
    val send = SendDraft()
    val recipient = RecipientDraft()
    val kyc = KycDraft()
    var walletCurrency by mutableStateOf(Currency.GBP)
    var pendingCardId by mutableStateOf<String?>(null)
    var scannedCard by mutableStateOf<Pair<String, String>?>(null)
    var cardReturnToSend by mutableStateOf(false)
    var resetUntil by mutableLongStateOf(0L)
    /** Set when the app locked itself after inactivity, to show the "Session ended" screen. */
    var sessionExpired by mutableStateOf(false)
    /** A screen to open once the app is unlocked (from a notification). */
    var pendingRoute by mutableStateOf<String?>(null)
    var askedNotifications by mutableStateOf(false)
    /** Transfer the support chat is about, shown as a pill at the top of the chat. */
    var chatAbout by mutableStateOf<String?>(null)
    /** Pre-selected topic (index) for the report-a-problem form. */
    var disputeTopic by mutableIntStateOf(0)
    /** Ask the tracking screen to open its cancel sheet straight away. */
    var openCancel by mutableStateOf(false)
}
