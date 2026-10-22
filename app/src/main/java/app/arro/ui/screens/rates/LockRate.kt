package app.arro.ui.screens.rates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.arro.core.Account
import app.arro.core.ArroBackend
import app.arro.core.ArroException
import app.arro.core.Catalog
import app.arro.core.Currency
import app.arro.core.LockKind
import app.arro.core.Money
import app.arro.core.RateLock
import app.arro.core.RateOffer
import app.arro.core.Text
import app.arro.ui.BannerKind
import app.arro.ui.BannerState
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.appState
import app.arro.ui.back
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.launchCall
import app.arro.ui.nowTicker
import app.arro.ui.rememberCall
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.Ic
import app.arro.ui.components.Icons
import app.arro.ui.components.KV
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.ProgressBar
import app.arro.ui.components.ScreenTitle
import app.arro.ui.components.TopBar
import app.arro.ui.components.Txt
import app.arro.ui.screens.send.startSend
import app.arro.ui.theme.C
import app.arro.ui.theme.T

/** Shows backend errors from offers and locks the same way everywhere. */
internal fun BannerState.offerError(e: ArroException): Boolean {
    show(
        when (e.code) {
            "offline" -> "Live rates unavailable"
            "offer_ended" -> "This offer has ended"
            "no_offer" -> "No offer right now"
            "kyc" -> "Verify your identity first"
            else -> "Couldn't lock the rate"
        },
        e.message ?: "", if (e.code == "no_offer" || e.code == "offer_ended") BannerKind.Info else BannerKind.Error,
    )
    return true
}

private fun lockedBody(l: RateLock) = "${Money.rateLine(Currency.GBP, Currency.of(l.to), l.rate)} until ${Text.time(l.expiresAt)}."

// ------------------------------------------------------------------ 9.05 Lock a rate

/**
 * Rate offers and locked rates for GBP → [code]. A locked rate is held on the server for one transfer until it
 * ends; the live rate shown alongside it keeps moving with the market.
 */
@Composable
fun LockRateScreen(code: String) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    val online by c.connectivity.online.collectAsStateWithLifecycle()
    val now = nowTicker()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val cur = Currency.of(code).takeIf { it != Currency.GBP } ?: b.relevantCurrency(a)
    val lock = a.offerLock(cur.code, now)
    val offer = a.liveOffer(now)?.takeIf { it.to == cur.code }
    val otherOffer = a.liveOffer(now)?.takeIf { it.to != cur.code }
    val ended = if (lock == null && offer == null) a.rateLocks.firstOrNull { it.kind == LockKind.OFFER && it.to == cur.code }
        ?.takeIf { it.expiredUnused(now) && now - it.expiresAt < 6 * 3_600_000L } else null
    val live = b.customerRate(cur)
    val country = Catalog.countryForCurrency(cur)
    val sample = 25_000L
    val fetched = state.rates.fetchedAt

    fun sendNow() {
        if (country != null) startSend(c, nav, banner, country = country.code)
    }

    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Lock a rate") },
        gap = 16.dp,
        footer = {
            when {
                lock != null -> ArroButton("Send at this rate", { sendNow() }, enabled = country != null, icon = Icons.ArrowUpRight)
                offer != null -> {
                    ArroButton("Lock this rate for ${offer.lockMinutes} minutes", {
                        scope.launchCall(call, onError = { banner.offerError(it) }) {
                            val l = b.lockOffer(offer.id)
                            banner.show("Rate locked", lockedBody(l), BannerKind.Success)
                        }
                    }, loading = call.busy.value, icon = Icons.Lock)
                    ArroButton("Not now", { b.dismissOffer(); nav.back() }, style = Btn.Text)
                }
                else -> {
                    ArroButton("Get a rate offer", {
                        scope.launchCall(call, onError = { banner.offerError(it) }) { b.requestOffer(cur) }
                    }, loading = call.busy.value, enabled = online)
                    ArroButton("Send at today's rate", { sendNow() }, style = Btn.Line, enabled = country != null)
                }
            }
        },
    ) {
        when {
            lock != null -> LockedPanel(lock, now)
            offer != null -> OfferPanel(offer, now)
            else -> {
                if (ended != null) Note("Your locked rate of ${Money.rate(ended.rate, Money.rateDecimals(ended.rate))} ${cur.code} ended at ${Text.time(ended.expiresAt)}. " +
                    "Ask for a new offer, or send at today's rate.", NoteKind.Warn, Icons.Clock)
                ScreenTitle("Lock a better rate", "When there's a better rate for ${cur.code}, you can hold it for ${ArroBackend.OFFER_LOCK_MINUTES} minutes while you send.")
            }
        }

        // What the locked or offered rate is worth next to today's live rate.
        val held = lock?.rate ?: offer?.rate
        Panel(gap = 10.dp) {
            if (held != null) {
                KV(if (lock != null) "Your locked rate" else "Offer rate", Money.rateLine(Currency.GBP, cur, held), strong = true)
                KV("Today's live rate", Money.rateLine(Currency.GBP, cur, live))
                KV("${Money.format(sample, Currency.GBP, false)} sends", Money.format(Money.convert(sample, Currency.GBP, cur, held), cur), strong = true)
                val extra = Money.convert(sample, Currency.GBP, cur, held) - Money.convert(sample, Currency.GBP, cur, live)
                val less: (@Composable () -> Unit)? = if (extra > 0) { { Pill("${Money.format(extra, cur)} less", PillKind.Neutral) } } else null
                KV("At today's rate", Money.format(Money.convert(sample, Currency.GBP, cur, live), cur), trailing = less)
            } else {
                KV("Today's live rate", Money.rateLine(Currency.GBP, cur, live), strong = true)
                KV("${Money.format(sample, Currency.GBP, false)} sends", Money.format(Money.convert(sample, Currency.GBP, cur, live), cur))
                KV("Updated", if (fetched > 0) Text.relativeDay(fetched, now) else "Not yet")
            }
            Cap(if (state.rates.demoMoved) "Demo: the market rate was moved from Demo controls. The next live update replaces it."
                else "Live rates come from ${state.rates.source.ifBlank { "our rate provider" }} and move with the market. They aren't guaranteed until you lock one.", size = 12f)
        }

        if (!b.ratesLive() && lock == null && offer == null) {
            Note(if (fetched > 0) "We couldn't update rates since ${Text.relativeDay(fetched, now).lowercase()}. Offers need live rates."
                else "We haven't been able to get live rates yet. Offers need live rates.", NoteKind.Warn, Icons.WifiOff)
        }
        if (otherOffer != null && lock == null) {
            Note("There's a rate offer for ${otherOffer.to} for the next ${Text.countdown(otherOffer.expiresAt - now)}.", NoteKind.Lime, Icons.Bolt,
                action = "See it", onAction = { nav.go(Routes.lockRate(otherOffer.to)) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Over("How locking works")
            listOf(
                "A locked rate is held for one transfer to ${cur.code}, for ${ArroBackend.OFFER_LOCK_MINUTES} minutes.",
                "It stays the same even if the market moves. If it ends before you send, you get today's rate.",
                "Arro fees and your sending limits still apply. Balance conversions use the live rate.",
                "Offers in this test build are demo promotions, priced from the live mid-market rate.",
            ).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 7.dp).size(5.dp).background(C.LimeDeep, CircleShape))
                    Cap(line, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LockedPanel(l: RateLock, now: Long) {
    val total = (l.expiresAt - l.createdAt).coerceAtLeast(1)
    Panel(kind = PanelKind.Dark, padding = 20.dp, gap = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(Icons.Lock, size = 16.dp, tint = C.Lime)
                Over("Rate locked", color = C.Lime)
            }
            if (l.offerId.isNotEmpty()) Pill("Demo offer", PillKind.OnDark)
        }
        Txt(Money.rateLine(Currency.GBP, Currency.of(l.to), l.rate), T.Big28, color = C.OnNight)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(Text.countdown(l.expiresAt - now), T.Big22, color = C.Lime)
            Cap("left · held until ${Text.time(l.expiresAt)}", Modifier.padding(bottom = 3.dp), color = C.DarkCap)
        }
        ProgressBar((l.expiresAt - now).toFloat() / total, track = C.OnNight.copy(alpha = 0.16f), fill = C.Lime, height = 6.dp)
    }
}

@Composable
private fun OfferPanel(o: RateOffer, now: Long) {
    Panel(kind = PanelKind.Dark, padding = 20.dp, gap = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(Icons.Bolt, size = 16.dp, tint = C.Lime)
                Over("Rate offer", color = C.Lime)
            }
            if (o.demo) Pill("Demo offer", PillKind.OnDark)
        }
        Txt(Money.rateLine(Currency.GBP, Currency.of(o.to), o.rate), T.Big28, color = C.OnNight)
        Cap("Usually ${Money.rate(o.standardRate, Money.rateDecimals(o.standardRate))} ${o.to} today. Lock it to hold it for ${o.lockMinutes} minutes.", color = C.DarkCap)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ic(Icons.Clock, size = 16.dp, tint = C.Lime)
            Txt("Offer ends in ${Text.countdown(o.expiresAt - now)}", T.Body14.copy(fontWeight = FontWeight.SemiBold), color = C.OnNight)
        }
    }
}

// ------------------------------------------------------------------ Home card for a running offer or lock

/** A compact card for the home screen while a rate offer is open or a locked rate is running. Shows nothing otherwise. */
@Composable
fun RateOfferCard(a: Account, modifier: Modifier = Modifier) {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val now = nowTicker()
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val lock = a.anyOfferLock(now)
    val offer = if (lock == null) a.liveOffer(now) else null
    val cur = Currency.of(lock?.to ?: offer?.to ?: return)
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Night)
            .clickable(role = Role.Button) { nav.go(Routes.lockRate(cur.code)) }.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(C.Lime.copy(alpha = 0.16f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Ic(if (lock != null) Icons.Lock else Icons.Bolt, tint = C.Lime)
        }
        Column(Modifier.weight(1f)) {
            Txt(if (lock != null) "Rate locked · ${Text.countdown(lock.expiresAt - now)}" else "Rate offer · ends in ${Text.countdown(offer!!.expiresAt - now)}",
                T.H3, color = C.OnNight, maxLines = 1)
            Cap(Money.rateLine(Currency.GBP, cur, lock?.rate ?: offer!!.rate) + if (lock != null) " until ${Text.time(lock.expiresAt)}" else " for ${offer!!.lockMinutes} min",
                color = C.DarkCap, size = 12f)
        }
        if (lock != null) {
            ArroButton("Send", {
                Catalog.countryForCurrency(cur)?.let { startSend(c, nav, banner, country = it.code) }
            }, style = Btn.Lime, size = BtnSize.S)
        } else {
            ArroButton("Lock", {
                scope.launchCall(call, onError = { banner.offerError(it) }) {
                    val l = b.lockOffer(offer!!.id)
                    banner.show("Rate locked", lockedBody(l), BannerKind.Success)
                }
            }, style = Btn.Lime, size = BtnSize.S, loading = call.busy.value)
        }
    }
}
