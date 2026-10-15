package app.arro.core

/** The simulated support agent. Replies use what the app knows about the customer's latest transfer. */
object Agent {
    const val NAME = "Amina"

    private fun latest(a: Account): Transfer? = a.transfers.maxByOrNull { it.createdAt }

    fun greeting(a: Account, about: Transfer?): String {
        val tr = about ?: latest(a)?.takeIf { it.isOpen || System.currentTimeMillis() - it.createdAt < 3 * 86_400_000L }
        return if (tr != null) "Hi ${a.firstName}, I'm $NAME. I can see your ${Money.format(tr.sendMinor, tr.fromCur)} transfer to ${tr.firstName}. How can I help?"
        else "Hi ${a.firstName}, I'm $NAME from Arro support. How can I help today?"
    }

    fun reply(text: String, attachment: String, a: Account, first: Boolean): ChatMessage {
        val t = text.lowercase()
        val tr = latest(a)
        val now = System.currentTimeMillis()
        fun msg(s: String, file: String = "", note: String = "", quick: List<String> = emptyList()) = ChatMessage(Ids.id(), true, s, now, file, note, quick)

        if (attachment.isNotEmpty() && t.isBlank()) return msg("Thanks, I've got your file. Give me a moment to look at it.")
        return when {
            listOf("found it", "thanks", "thank you", "sorted", "all good").any { it in t } ->
                msg("Great, glad that's sorted! Is there anything else I can help with?")
            "still not" in t || "not there" in t ->
                msg("I'm sorry about that. I've asked ${tr?.providerName ?: "the provider"} to trace it and opened a case so you can follow it under Help. We'll update you within 1 working day.")
            "cancel" in t ->
                msg("You can cancel any transfer that hasn't been delivered yet. Open it and tap Request cancellation, and we'll refund the full amount, fee included.")
            listOf("deliver", "received", "receive", "arrive", "nothing", "hasn't", "has not", "where is", "missing").any { it in t } -> when {
                tr == null -> msg("I can't see any transfers on your account yet. Once you send one, you can track it live from Home.")
                tr.status == TransferStatus.DELIVERED -> msg(
                    "Thanks for flagging it. ${tr.providerName} confirmed the credit at ${Text.time(tr.deliveredAt)} to ${if (tr.payoutType == PayoutType.BANK) "account ${tr.payoutAccount}" else Text.formatPhone(tr.payoutAccount)}. " +
                        "Could ${tr.firstName} check their ${tr.providerName} transaction history? Here's the proof of delivery:",
                    "Proof of delivery.pdf", "${tr.providerName} ref ${tr.providerRef}", listOf("${tr.firstName} found it, thanks", "Still not there"))
                tr.status == TransferStatus.ON_HOLD -> msg("Your transfer to ${tr.firstName} is waiting for one quick answer from you. Open it from Home and it'll carry on straight away.")
                tr.isOpen -> msg("It's on track and should arrive ${Text.by(tr.deliveredAt, now)}. I'll keep an eye on it for you.")
                else -> msg("That transfer was ${tr.status.name.lowercase()}. ${if (tr.refundedAt > 0) "The refund is complete." else "The refund is on its way to ${tr.fundingLabel}."}")
            }
            "refund" in t -> msg("Refunds go back to where the money came from: your Arro balance straight away, a debit card in 3 to 5 working days, or a bank account in 1 to 2.")
            listOf("fee", "rate", "price", "cost").any { it in t } -> msg("You pay a small fixed fee plus a margin in our rate, from 0.29%. Both are shown before you pay, and your rate is locked for 30 minutes.")
            listOf("limit", "verify", "verification").any { it in t } -> msg("On Standard you can send up to £5,000 per transfer and £25,000 a month. You can raise that in Account › Verification and limits.")
            listOf("hello", "hi", "hey").any { t.startsWith(it) } -> msg(if (first) greeting(a, null) else "Hi again! What can I help with?")
            else -> msg("Thanks, ${a.firstName}. I'm looking into this now. If it needs more digging, I'll open a case so you can follow it under Help.")
        }
    }
}
