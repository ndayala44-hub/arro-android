package app.arro.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Rate locks and offers, Arro Rewards points and referral rewards. */
class LockAndRewardsTest {
    private val start = 1_792_000_000_000L

    /** A rate provider that answers like the live one, with rates we can move. */
    private class MovableRates : RateSource {
        var mid = Rates.fallbackMid.toMutableMap()
        var online = true
        override suspend fun fetch(): Map<String, Double>? = if (online) mid.toMap() else null
    }

    private fun backend(rates: MovableRates = MovableRates(), store: StateStore = MemoryStore()): Triple<ArroBackend, FakeClock, MovableRates> {
        val clock = FakeClock(start)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        return Triple(ArroBackend(store, rates, scope, clock, Dispatchers.Unconfined, latency = false), clock, rates)
    }

    private fun ArroBackend.acc() = state.value.account!!

    private fun ArroBackend.events(): MutableList<BackendEvent> {
        val seen = mutableListOf<BackendEvent>()
        CoroutineScope(Dispatchers.Unconfined).launch { events.collect { seen += it } }
        return seen
    }

    // ------------------------------------------------------------------ Lock Rate

    @Test fun lockedOfferRateStaysTheSameWhenTheMarketMoves() = runBlocking {
        val (b, _, rates) = backend()
        b.loadDemoAccount()
        assertTrue(b.refreshRates())
        val standard = b.customerRate(Currency.RWF)
        val offer = b.requestOffer(Currency.RWF)
        assertTrue("an offer beats the usual rate", offer.rate > standard)
        val lock = b.lockOffer(offer.id)
        assertEquals(offer.rate, lock.rate, 0.0)
        assertEquals(LockKind.OFFER, lock.kind)
        assertNull("the offer is used up", b.acc().rateOffer)

        // The market moves twice, through a live refresh and a demo nudge; the locked rate doesn't.
        rates.mid["RWF"] = rates.mid["RWF"]!! * 0.97
        assertTrue(b.refreshRates())
        b.demoMoveMarket(Currency.RWF, -1.0)
        assertNotEquals(standard, b.customerRate(Currency.RWF), 0.0001)
        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        assertEquals(lock.id, q.lockId)
        assertEquals(lock.rate, q.rate, 0.0)
        assertEquals(lock.expiresAt, q.lockedUntil)

        val tr = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support"))
        assertEquals(lock.rate, tr.rate, 0.0)
        assertEquals(lock.id, tr.rateLockId)
        assertEquals(tr.id, b.acc().rateLocks.first { it.id == lock.id }.transferId)
        // Used once: the same quote can't be sent again, and the next quote is at today's rate.
        try { b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Again")); fail() } catch (e: ArroException) { assertEquals("expired", e.code) }
        assertEquals(b.customerRate(Currency.RWF), b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA").rate, 0.0)
    }

    @Test fun lockExpiryIsCheckedWhenSendingAndAnnounced() = runBlocking<Unit> {
        val (b, clock, _) = backend()
        b.loadDemoAccount()
        b.refreshRates()
        val seen = b.events()
        val lock = b.lockOffer(b.requestOffer(Currency.RWF).id)
        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        clock.advance(ArroBackend.OFFER_LOCK_MINUTES * 60_000L - 1_000)
        assertNotNull(b.acc().offerLock("RWF", clock.t))
        clock.advance(2_000)
        assertNull(b.acc().offerLock("RWF", clock.t))
        try {
            b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support")); fail("an ended lock must be refused")
        } catch (e: ArroException) {
            assertEquals("expired", e.code)
            assertTrue(e.message!!.contains("ended"))
        }
        b.tick(); b.tick()
        assertEquals(1, seen.count { it is BackendEvent.LockEnded })
        assertEquals(1, b.acc().notifications.count { it.title == "Your locked rate has ended" })
        assertTrue(b.acc().rateLocks.first { it.id == lock.id }.endNoticed)
        // A fresh quote holds today's rate again.
        val fresh = b.requote(q, "RW", "PAY_VISA")
        assertEquals(LockKind.QUOTE, b.acc().rateLocks.first { it.id == fresh.lockId }.kind)
        assertEquals(b.customerRate(Currency.RWF), fresh.rate, 0.0)
        b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", fresh, "PAY_VISA", "Family support"))
    }

    @Test fun offersEndAndCanBeRenewed() = runBlocking {
        val (b, clock, rates) = backend()
        b.loadDemoAccount()
        b.refreshRates()
        val first = b.requestOffer(Currency.KES)
        assertEquals(first.id, b.requestOffer(Currency.KES).id)
        clock.advance(ArroBackend.OFFER_WINDOW_MS + 1)
        try { b.lockOffer(first.id); fail() } catch (e: ArroException) { assertEquals("offer_ended", e.code) }
        b.tick()
        assertNull("an ended offer is cleared", b.acc().rateOffer)
        // A new offer needs live rates.
        rates.online = false
        clock.advance(ArroBackend.RATES_FRESH_MS + 1)
        try { b.requestOffer(Currency.KES); fail() } catch (e: ArroException) { assertEquals("offline", e.code) }
        rates.online = true
        val renewed = b.requestOffer(Currency.KES)
        assertNotEquals(first.id, renewed.id)
        assertTrue(renewed.liveAt(clock.t))
    }

    @Test fun quoteHoldsAreCheckedOnTheServer() = runBlocking {
        val (b, clock, _) = backend()
        b.loadDemoAccount()
        b.refreshRates()
        val hold = b.holdRate(Currency.RWF)!!
        assertEquals(hold.id, b.holdRate(Currency.RWF)!!.id)
        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        assertEquals(hold.id, q.lockId)
        // A quote edited on the device is refused.
        for (bad in listOf(q.copy(rate = q.rate + 5), q.copy(feeMinor = 0), q.copy(receiveMinor = q.receiveMinor + 100), q.copy(lockId = "NOPE"))) {
            try { b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", bad, "PAY_VISA", "x")); fail() } catch (e: ArroException) { assertEquals("expired", e.code) }
        }
        clock.advance(ArroBackend.QUOTE_HOLD_MS + 1)
        try { b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "x")); fail() } catch (e: ArroException) { assertEquals("expired", e.code) }
        // Without a hold, only today's rate is accepted.
        val live = b.quote(Currency.KES, 10_000, PayoutType.MOBILE_MONEY, "KE", "PAY_VISA")
        assertEquals("", live.lockId)
        b.demoMoveMarket(Currency.KES, 1.0)
        try { b.createTransfer(TransferDraft("R_WANJIRU", "PM_WK", live, "PAY_VISA", "x")); fail() } catch (e: ArroException) { assertEquals("expired", e.code) }
    }

    @Test fun staleQuoteHoldsAreRepricedButLockedOffersAreNot() = runBlocking {
        val (b, _, rates) = backend()
        b.loadDemoAccount()
        b.refreshRates()
        val old = b.holdRate(Currency.RWF)!!
        rates.mid["RWF"] = rates.mid["RWF"]!! * 1.01
        b.refreshRates()
        val repriced = b.refreshHold(Currency.RWF)!!
        assertNotEquals(old.id, repriced.id)
        assertEquals(b.customerRate(Currency.RWF), repriced.rate, 0.0)
        val locked = b.lockOffer(b.requestOffer(Currency.RWF).id)
        rates.mid["RWF"] = rates.mid["RWF"]!! * 1.02
        b.refreshRates()
        assertEquals(locked.id, b.refreshHold(Currency.RWF)!!.id)
    }

    // ------------------------------------------------------------------ Points

    @Test fun pointsAreEarnedOnceWhenATransferIsDelivered() = runBlocking {
        val (b, clock, _) = backend()
        b.loadDemoAccount()
        val before = b.acc().pointsBalance
        assertTrue("the sample account starts with points from its history", before > 0)
        assertEquals("bonus is only given once", 1, b.acc().points.count { it.ref == Loyalty.BONUS_REF })
        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        val tr = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support"))
        assertEquals("nothing until delivery", before, b.acc().pointsBalance)
        clock.advance(60_000); repeat(5) { b.tick() }
        b.fastForward()
        assertEquals(TransferStatus.DELIVERED, b.acc().transfer(tr.id)!!.status)
        assertEquals(before + 199, b.acc().pointsBalance)
        assertEquals(1, b.acc().points.count { it.ref == Loyalty.earnedRef(tr.id) })
        // Running the backfill again (as a migration would) doesn't add anything.
        assertEquals(b.acc().points, Loyalty.backfill(b.acc(), clock.t).points)
    }

    @Test fun failedAndCancelledTransfersEarnNothing() = runBlocking {
        val (b, clock, _) = backend()
        b.loadDemoAccount()
        val before = b.acc().pointsBalance
        val m = b.newPayoutMethod(PayoutType.MOBILE_MONEY, "mtn_rw", "+250788214000", "TEST PERSON")
        val r = b.addRecipient("Test Person", "RW", "Friend", "Kigali", m)
        val failing = b.createTransfer(TransferDraft(r.id, m.id, b.quote(Currency.RWF, 5_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA"), "PAY_VISA", "Gift"))
        val cancelled = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", b.quote(Currency.RWF, 5_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA"), "PAY_VISA", "Gift"))
        b.cancelTransfer(cancelled.id, "Wrong amount")
        clock.advance(120_000); repeat(3) { b.tick() }
        clock.advance(60_000); b.tick()
        assertEquals(TransferStatus.REFUNDED, b.acc().transfer(failing.id)!!.status)
        assertEquals(TransferStatus.REFUNDED, b.acc().transfer(cancelled.id)!!.status)
        assertEquals(before, b.acc().pointsBalance)
    }

    @Test fun newAccountsStartEmptyAndEarnTheBonus() {
        val a = Account("NEW", start, transfers = emptyList())
        assertEquals(0, a.pointsBalance)
        val t = Transfer(id = "T1", reference = "R", recipientId = "X", recipientName = "Ama Owusu", recipientColor = 1, country = "GH", from = "GBP", to = "GHS",
            sendMinor = 10_000, feeMinor = 0, fundingFeeMinor = 0, totalMinor = 10_000, rate = 15.0, midRate = 15.1, receiveMinor = 150_000,
            payoutType = PayoutType.MOBILE_MONEY, providerId = "mtn_gh", payoutAccount = "+233", fundingId = "PAY", fundingLabel = "Visa", reason = "",
            createdAt = start, status = TransferStatus.DELIVERED, deliveredAt = start + 1)
        val after = Loyalty.award(a, t, start + 2)
        assertEquals("a free first transfer still earns the bonus", Loyalty.FIRST_TRANSFER_BONUS, after.pointsBalance)
        assertEquals(after, Loyalty.award(after, t, start + 3))
    }

    @Test fun redeemingPointsGivesFeeCreditThatComesOffTheFee() = runBlocking {
        val (b, _, _) = backend()
        b.loadDemoAccount()
        val points = b.acc().pointsBalance
        assertTrue(points >= 1_000)
        try { b.redeemPoints(123); fail() } catch (e: ArroException) { }
        assertEquals(100L, b.redeemPoints(1_000))
        assertEquals(points - 1_000, b.acc().pointsBalance)
        assertEquals(100L, b.acc().feeCreditMinor)
        try { b.redeemPoints(10_000_000); fail() } catch (e: ArroException) { }

        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        assertEquals(100L, q.discountMinor)
        assertEquals(25_000L + 199 - 100, q.totalMinor)
        assertEquals("points are earned on the fee actually paid", 99, Loyalty.pointsToEarn(b.acc(), q))
        val tr = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support"))
        assertEquals(100L, tr.pointsDiscountMinor)
        assertEquals(0L, b.acc().feeCreditMinor)
        // A cancelled transfer gives the credit back.
        b.cancelTransfer(tr.id, "Wrong amount")
        assertEquals(100L, b.acc().feeCreditMinor)
        // Credit never comes off more than the fee.
        val arro = b.quote(Currency.GBP, 5_000, PayoutType.ARRO, "GB", "PAY_VISA")
        assertEquals(0L, arro.discountMinor)
    }

    // ------------------------------------------------------------------ Referrals

    @Test fun referralRewardIsPaidOncePerQualifyingFriend() {
        val (b, _, _) = backend()
        b.loadDemoAccount()
        val gbp = b.acc().wallet(Currency.GBP)!!.balanceMinor
        val earned = b.acc().referral.earnedMinor
        val f = b.demoFriendJoins()
        assertTrue(f.sample)
        assertEquals(FriendStatus.SIGNED_UP, b.acc().referral.friends.first { it.id == f.id }.status)
        assertEquals("under £100 doesn't qualify", 0L, b.referralTransferDelivered(f.id, 9_999))
        assertEquals(1_000L, b.referralTransferDelivered(f.id, 12_000))
        assertEquals("only once per friend", 0L, b.referralTransferDelivered(f.id, 50_000))
        assertEquals(0L, b.referralTransferDelivered("UNKNOWN", 50_000))
        assertEquals(gbp + 1_000, b.acc().wallet(Currency.GBP)!!.balanceMinor)
        assertEquals(earned + 1_000, b.acc().referral.earnedMinor)
        assertEquals(1, b.acc().ledger.count { it.type == LedgerType.REWARD && it.subtitle == "${f.name} joined Arro" })
        // Referral money and points are separate.
        assertEquals(b.acc().referral.friends.sumOf { it.rewardMinor }, b.acc().referral.earnedMinor)
    }

    @Test fun referralRewardsStopAtTheYearlyLimit() {
        val now = start
        val friends = (1..Loyalty.REFERRAL_YEARLY_CAP).map {
            ReferredFriend("F$it", "Friend $it", FriendStatus.QUALIFIED, now - 5_000, 10_000, now - 1_000, Loyalty.REFERRAL_REWARD_MINOR)
        } + ReferredFriend("LAST", "Last Friend", FriendStatus.SIGNED_UP, now - 5_000)
        val r = Referral("CODE", friends.size, Loyalty.REFERRAL_YEARLY_CAP * Loyalty.REFERRAL_REWARD_MINOR, friends)
        val (after, pay) = Loyalty.qualify(r, "LAST", 20_000, now)
        assertEquals(0L, pay)
        assertEquals(FriendStatus.QUALIFIED, after.friends.first { it.id == "LAST" }.status)
        assertEquals(r.earnedMinor, after.earnedMinor)
    }

    // ------------------------------------------------------------------ Saved data from older versions

    @Test fun olderSavesAreMigratedOnce() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val old = Seed.account(start).copy(points = emptyList(), loyaltyVersion = 0, referral = Referral("ERIC-7Q4K", 2, 2_000))
        val saved = json.encodeToString(AppState(account = old, signedIn = true))
        val store = object : StateStore {
            override val persistent = false
            override fun read() = saved
            override fun write(text: String) {}
        }
        val (b, _, _) = backend(store = store)
        val a = b.acc()
        assertEquals(ArroBackend.LOYALTY_VERSION, a.loyaltyVersion)
        assertEquals(Seed.account(start).pointsBalance, a.pointsBalance)
        assertEquals(1, a.points.count { it.ref == Loyalty.BONUS_REF })
        assertEquals(2_000L, a.referral.earnedMinor)
        assertTrue(a.referral.friends.all { it.sample })
        // Data saved before this version has no rate locks or offers and still loads.
        val bare = json.decodeFromString<AppState>("""{"account":{"id":"A","createdAt":1}}""")
        assertTrue(bare.account!!.rateLocks.isEmpty() && bare.account!!.rateOffer == null && !bare.account!!.settings.widgetBalance)
    }
}
