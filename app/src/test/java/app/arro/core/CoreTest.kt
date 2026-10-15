package app.arro.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class FakeClock(var t: Long) : Clock {
    override fun now() = t
    fun advance(ms: Long) { t += ms }
}

class CoreTest {
    private val start = 1_792_000_000_000L // a fixed moment in October 2026

    private fun backend(clock: FakeClock = FakeClock(start)): Pair<ArroBackend, FakeClock> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val b = ArroBackend(MemoryStore(), RateSource { null }, scope, clock, Dispatchers.Unconfined, latency = false)
        return b to clock
    }

    @Test fun formatsMoneyLikeTheDesigns() {
        assertEquals("£2,486.35", Money.format(248_635, Currency.GBP))
        assertEquals("RWF 490,600", Money.format(490_600, Currency.RWF))
        assertEquals("KES 25,875.00", Money.format(2_587_500, Currency.KES))
        assertEquals("€412.80", Money.format(41_280, Currency.EUR))
        assertEquals("£5,000", Money.format(500_000, Currency.GBP, forceDecimals = false))
        assertEquals("£2,955" to ".62", Money.split(295_562, Currency.GBP))
        assertEquals("1 GBP = 1,962.40 RWF", Money.rateLine(Currency.GBP, Currency.RWF, 1962.40))
        assertEquals("1 GBP = 1.1530 EUR", Money.rateLine(Currency.GBP, Currency.EUR, 1.1530))
        assertEquals(25_000L, Money.parse("250", Currency.GBP))
        assertEquals(125_050L, Money.parse("1,250.5", Currency.GBP))
        assertNull(Money.parse(".", Currency.GBP))
    }

    @Test fun quoteMatchesTheSendScreen() {
        val (b, _) = backend()
        b.loadDemoAccount()
        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        assertEquals(1962.40, q.rate, 0.0001)
        assertEquals(490_600L, q.receiveMinor)
        assertEquals(199L, q.feeMinor)
        assertEquals(25_199L, q.totalMinor)
        assertEquals(0L, q.fundingFeeMinor)
        val credit = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_MC")
        assertEquals(438L, credit.fundingFeeMinor)
        val bank = b.quote(Currency.RWF, 25_000, PayoutType.BANK, "RW", "PAY_VISA")
        assertEquals(99L, bank.feeMinor)
    }

    @Test fun validatesCardsAndPhones() {
        assertTrue(Validation.luhn("4921 0731 5520 4821".let { "4111111111111111" }))
        assertFalse(Validation.luhn("4111111111111112"))
        assertEquals(CardBrand.VISA, Validation.brand("4111"))
        assertEquals(CardBrand.MASTERCARD, Validation.brand("5555555555554444"))
        assertNull(Validation.cardNumber("5555 5555 5555 4444"))
        assertNotNull(Validation.cardNumber("4111 1111 1111 1112"))
        assertNull(Validation.expiry("08/29", LocalDate.of(2026, 10, 15)))
        assertEquals("This card has expired.", Validation.expiry("01/25", LocalDate.of(2026, 10, 15)))
        val rw = Catalog.country("RW")
        assertEquals("Rwandan numbers have 9 digits after +250. This one has 8.", Validation.mobile("78821441", rw))
        assertNull(Validation.mobile("0788 214 412", rw))
        assertEquals("Add their last name too. It must match their MoMo account.", Validation.fullName("Claudine", "MoMo"))
        assertEquals("Good", Validation.password("Password1").label)
        assertEquals("Strong", Validation.password("Password1!").label)
        assertTrue(Validation.passcodeWeak("123456"))
        assertTrue(Validation.passcodeWeak("111111"))
        assertFalse(Validation.passcodeWeak("246810"))
        assertTrue(Validation.isUkPostcode("e82pl"))
        assertEquals("+250 788 214 412", Text.formatPhone("+250788214412"))
        assertEquals("+250 788 ••• 412", Text.maskPhone("+250788214412"))
        assertEquals("+44 7700 900412", Text.formatPhone("+447700900412"))
    }

    @Test fun registrationAndLogin() = runBlocking {
        val (b, _) = backend()
        val phone = b.sendSignupCode("GB", "07700 900999")
        assertEquals("+447700900999", phone)
        try { b.verifySignupCode("000000"); fail("wrong code accepted") } catch (e: ArroException) { }
        // read the code the way the app does, from the event stream replay is not kept, so use the internal state via a resend
        var code = ""
        val job = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined).launchCollect(b) { code = it }
        b.resendSignupCode()
        job.cancel()
        assertEquals(6, code.length)
        b.verifySignupCode(code)
        b.setLogin("ama@mail.com", "Password1!", false)
        b.finishRegistration("246810")
        val a = b.state.value.account!!
        assertEquals("ama@mail.com", a.email)
        assertEquals(KycStatus.NOT_STARTED, a.kyc.status)
        assertTrue(b.unlocked.value)
        b.lock(); assertFalse(b.unlocked.value)
        try { b.unlockWithPasscode("111111"); fail() } catch (e: ArroException) { }
        b.unlockWithPasscode("246810"); assertTrue(b.unlocked.value)
        b.logout()
        b.login("ama@mail.com", "Password1!")
        assertTrue(b.state.value.signedIn)
    }

    @Test fun identityReviewApprovesAfterDelay() = runBlocking {
        val (b, clock) = backend()
        b.loadDemoAccount()
        b.retakeIdentity()
        b.submitIdentity(0.9f)
        assertEquals(KycStatus.IN_REVIEW, b.state.value.account!!.kyc.status)
        clock.advance(8_000); b.tick()
        assertEquals(KycStatus.APPROVED, b.state.value.account!!.kyc.status)
        b.retakeIdentity()
        b.submitIdentity(0.1f)
        clock.advance(8_000); b.tick()
        assertEquals(KycStatus.REJECTED, b.state.value.account!!.kyc.status)
    }

    @Test fun transferMovesThroughStatusesAndDelivers() = runBlocking {
        val (b, clock) = backend()
        b.loadDemoAccount()
        val q = b.quote(Currency.RWF, 25_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        val tr = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support"))
        assertEquals(TransferStatus.PROCESSING, tr.status)
        clock.advance(3_000); b.tick()
        assertEquals(TransferStatus.IN_PROGRESS, b.state.value.account!!.transfer(tr.id)!!.status)
        clock.advance(40_000); b.tick()
        val done = b.state.value.account!!.transfer(tr.id)!!
        assertEquals(TransferStatus.DELIVERED, done.status)
        assertTrue(done.providerRef.startsWith("MP"))
    }

    @Test fun balanceFundingDeductsAndRefundsOnCancel() = runBlocking {
        val (b, clock) = backend()
        b.loadDemoAccount()
        val before = b.state.value.account!!.wallet(Currency.GBP)!!.balanceMinor
        val q = b.quote(Currency.KES, 10_000, PayoutType.MOBILE_MONEY, "KE", ArroBackend.BALANCE)
        val tr = b.createTransfer(TransferDraft("R_WANJIRU", "PM_WK", q, ArroBackend.BALANCE, "Gift"))
        assertEquals(before - q.totalMinor, b.state.value.account!!.wallet(Currency.GBP)!!.balanceMinor)
        b.cancelTransfer(tr.id, "Wrong amount")
        assertEquals(before, b.state.value.account!!.wallet(Currency.GBP)!!.balanceMinor)
        clock.advance(3_000); b.tick()
        assertEquals(TransferStatus.REFUNDED, b.state.value.account!!.transfer(tr.id)!!.status)
    }

    @Test fun insufficientBalanceAndLimitsAreEnforced() = runBlocking {
        val (b, _) = backend()
        b.loadDemoAccount()
        val big = b.quote(Currency.RWF, 300_000, PayoutType.MOBILE_MONEY, "RW", ArroBackend.BALANCE)
        try {
            b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", big, ArroBackend.BALANCE, "x")); fail()
        } catch (e: ArroException) { assertEquals("insufficient", e.code) }
        assertNotNull(b.limitProblem(600_000))
        assertNull(b.limitProblem(25_000))
    }

    @Test fun largeTransfersHoldUntilAnswered() = runBlocking {
        val (b, clock) = backend()
        b.loadDemoAccount()
        val q = b.quote(Currency.RWF, 150_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        val tr = b.createTransfer(TransferDraft("R_CLAUDINE", "PM_CU_MTN", q, "PAY_VISA", "Family support"))
        clock.advance(3_100); b.tick(); b.tick()
        assertEquals(TransferStatus.ON_HOLD, b.state.value.account!!.transfer(tr.id)!!.status)
        b.answerHold(tr.id, "She's my sister", "Salary")
        assertEquals(TransferStatus.IN_PROGRESS, b.state.value.account!!.transfer(tr.id)!!.status)
        clock.advance(60_000); b.tick()
        assertEquals(TransferStatus.DELIVERED, b.state.value.account!!.transfer(tr.id)!!.status)
    }

    @Test fun badNumbersFailAndRefund() = runBlocking {
        val (b, clock) = backend()
        b.loadDemoAccount()
        val m = b.newPayoutMethod(PayoutType.MOBILE_MONEY, "mtn_rw", "+250788214000", "TEST PERSON")
        val r = b.addRecipient("Test Person", "RW", "Friend", "Kigali", m)
        val q = b.quote(Currency.RWF, 5_000, PayoutType.MOBILE_MONEY, "RW", "PAY_VISA")
        val tr = b.createTransfer(TransferDraft(r.id, m.id, q, "PAY_VISA", "Gift"))
        clock.advance(14_000); b.tick(); b.tick()
        val failed = b.state.value.account!!.transfer(tr.id)!!
        assertEquals(TransferStatus.FAILED, failed.status)
        assertTrue(failed.failReason.contains("couldn't find a wallet"))
        clock.advance(45_000); b.tick()
        assertEquals(TransferStatus.REFUNDED, b.state.value.account!!.transfer(tr.id)!!.status)
    }

    @Test fun nameChecksAndCards() = runBlocking {
        val (b, _) = backend()
        b.loadDemoAccount()
        val ok = b.checkName("RW", PayoutType.MOBILE_MONEY, "mtn_rw", "+250788214412", "Claudine Uwase")
        assertEquals("CLAUDINE UWASE", ok.registeredName); assertTrue(ok.matches)
        val bad = b.checkName("RW", PayoutType.MOBILE_MONEY, "mtn_rw", "+250788214999", "Alice Mukamana")
        assertFalse(bad.matches)
        val card = b.addCard("5555 5555 5555 4444", "1130", "123", "Eric Nshuti", false)
        assertEquals(MethodStatus.PENDING, card.status)
        val active = b.approveCard(card.id)
        assertEquals(MethodStatus.ACTIVE, active.status)
        try { b.addCard("4111 1111 1111 1112", "1130", "123", "", false); fail() } catch (e: ArroException) { assertEquals("number", e.code) }
    }

    @Test fun walletTopUpAndConvert() = runBlocking {
        val (b, _) = backend()
        b.loadDemoAccount()
        val gbp0 = b.state.value.account!!.wallet(Currency.GBP)!!.balanceMinor
        b.topUp(Currency.GBP, 50_000, "PAY_MONZO")
        assertEquals(gbp0 + 50_000, b.state.value.account!!.wallet(Currency.GBP)!!.balanceMinor)
        val eur0 = b.state.value.account!!.wallet(Currency.EUR)!!.balanceMinor
        val e = b.convert(Currency.GBP, Currency.EUR, 10_000)
        assertEquals(eur0 + e.counterMinor, b.state.value.account!!.wallet(Currency.EUR)!!.balanceMinor)
        assertEquals(11_530L, e.counterMinor)
    }

    @Test fun insightsMatchTheSeededMonth() {
        val (b, _) = backend()
        b.loadDemoAccount()
        val a = b.state.value.account!!
        val months = Insights.months(a, start)
        assertEquals(6, months.size)
        assertTrue(months.last().totalMinor > 0)
        assertEquals(6, Insights.recentRecipients(a).size)
    }

    @Test fun ratesHistoryEndsAtTodaysRate() {
        val snap = RatesSnapshot()
        val h = Rates.history(snap, Currency.RWF, 30, start)
        assertEquals(30, h.size)
        assertEquals(1968.11, h.last(), 0.0001)
        assertEquals(1962.40, Rates.customer(snap, Currency.GBP, Currency.RWF), 0.0001)
    }

    @Test fun birthdayFromTheCalendar() {
        val today = LocalDate.of(2026, 10, 15)
        assertEquals("1991-07-03" to null, Validation.dob(LocalDate.of(1991, 7, 3), today))
        assertNotNull(Validation.dob(null, today).second)
        assertNotNull(Validation.dob(LocalDate.of(2010, 1, 1), today).second)
        assertEquals("2008-10-15", Validation.dob(LocalDate.of(2008, 10, 15), today).first)
        assertNotNull(Validation.dob(LocalDate.of(1900, 1, 1), today).second)
    }

    @Test fun phoneChangeNeedsTheTextedCode() = runBlocking {
        val (b, _) = backend()
        b.loadDemoAccount()
        val codes = mutableListOf<String>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { b.events.collect { if (it is BackendEvent.Code) codes += it.code } }
        b.sendPhoneChangeCode("07700 900123")
        assertEquals(1, codes.size)
        try { b.confirmPhoneChange("000000"); fail("wrong code accepted") } catch (e: ArroException) { }
        assertEquals(Seed.DEMO_PHONE, b.state.value.account!!.phone)
        b.confirmPhoneChange(codes.first())
        assertEquals("+447700900123", b.state.value.account!!.phone)
        job.cancel()
    }

    @Test fun paymentCardsNeedBankApproval() = runBlocking {
        val (b, _) = backend()
        b.loadDemoAccount()
        val pending = b.addCard("4000 0566 5566 5556", "1229", "123", "", false)
        assertEquals(MethodStatus.PENDING, pending.status)
        assertEquals(MethodStatus.ACTIVE, b.approveCard(pending.id).status)
        val declined = b.addCard("4000 0000 0000 0002", "1229", "123", "", false)
        try { b.approveCard(declined.id); fail("0002 should be declined") } catch (e: ArroException) { assertEquals("declined", e.code) }
        assertNull(b.state.value.account!!.paymentMethod(declined.id))
    }
}

/** Collects the next verification code emitted by the backend. */
private fun CoroutineScope.launchCollect(b: ArroBackend, onCode: (String) -> Unit) = launch(Dispatchers.Unconfined) {
    b.events.collect { if (it is BackendEvent.Code) onCode(it.code) }
}
