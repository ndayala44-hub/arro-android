# Lock Rate, Loyalty and rewards, home screen widgets

This adds three features to the existing app without changing its design, navigation or existing flows. New screens reuse the existing components (panels, rows, pills, buttons, sheets) and tokens.

## What's new

### Lock Rate

- **Rate offers.** A short-lived offer of a better rate for GBP → the currency you send to most (last transfer, then favourite recipient, then RWF). It shows the pair, the offered rate, today's usual rate, a countdown and a **Lock** button. Offers last 3 minutes.
- **Where offers show:** a card on Home under the quick actions, an in-app banner, a strip on the amount screen (lock without leaving the transfer), the **Lock a rate** screen (`lock_rate/{cur}`, also reached from Rates → a currency), and the rate widget.
- **Locking** holds the offered rate on the backend for 10 minutes for one transfer to that currency. A live refresh or a market move doesn't change it. The amount screen, review screen, fees sheet and transfer details show "Offer rate locked" and the time left.
- **When a lock ends** unused, the backend posts a notification and an in-app banner ("Your locked rate has ended") with a **New offer** action. A quote that used it is refused at send time with a fresh quote at today's rate. The Lock a rate screen offers **Get a rate offer** or **Send at today's rate**.
- **Random, but demonstrable:** offers appear 25–90 seconds after unlocking, then 3–8 minutes after the previous one ends, only while the app is open and unlocked, the account can send, and live rates are fresh. **Demo controls → Show a rate offer now** makes one on demand; **End my locked rate now** and **Move the market up/down 1%** show expiry and rate changes.
- **Existing 30-minute rate lock:** the amount screen already showed "Rate locked 29:41". The countdown previously ran only in the UI; it's now a real hold (`LockKind.QUOTE`) created by the backend and checked when the transfer is created. A locked offer takes priority over it.

### Loyalty and rewards

- **Entry points:** an award button immediately after the bell in the Home header, and **Account → Loyalty and rewards**. Both open the same `loyalty` screen.
- **Points** (transaction rewards): 1 point per 1p of Arro fee actually paid, credited once the transfer is delivered, plus 250 bonus points on the first delivered transfer (Arro-to-Arro transfers have no fee and don't count for the bonus). Cancelled, failed and refunded transfers earn nothing. The amount and review screens show "You'll earn N points"; transfer details show points earned or on the way.
- **Redemption:** swap 1,000 / 2,500 / 5,000 / 10,000 points for £1 / £2.50 / £5 / £10 of fee credit. Credit comes off the Arro fee of the next transfers automatically (never off the amount sent or a card fee), shows as "Points credit" on quotes, reviews, details and receipts, and is returned if that transfer is cancelled or fails.
- **Referral earnings** (money): the existing Invite rewards terms: £10 into the GBP balance once a friend's first transfer of £100 or more is delivered, once per friend, up to 20 a year, for verified customers. Shown as their own category with friends, status, totals, and a link to the GBP balance, where the money can be sent or converted.
- **Empty states** for no points and no referrals. **Sample data** (the sample account's friends, and friends added from Demo controls) is labelled "Sample".

### Home screen widgets

- **Arro Quick Send** (4×2 by default): "Available balance", the brand mark, and a **Send again** row: a round **Send** button followed by up to four people (favourites first, then most recent), each with their avatar colour, initials, country flag and first name. Narrower sizes show three or two people; shorter sizes drop, in turn, the "Send again" caption, the label row and the names. **Send** opens the send flow; a person opens the amount screen for them. Either way the app asks for your passcode first and the usual checks, review and approval are unchanged.
- **Arro balance** (2×2, resizable): GBP balance and a **Send money** button (dropped when the widget is too short for it; the whole widget opens Arro).
- **Arro rate** (2×2, resizable to 4×2): today's rate for your currency labelled **Live** and "not guaranteed until locked", or a running **Offer** with its end time, or your **Locked** rate with its end time. Refresh button, and a **Lock a rate** / **Lock this rate** / **Send at this rate** button. Expired offers aren't shown; if no live rate has ever been fetched it says so instead of showing the built-in fallback rates. At 2×2 the status uses shorter wording that fits one line; short sizes drop the button, then the unit line, rather than cutting them off.
- **States handled:** signed out (log-in prompt), balance hidden, account frozen, offline, stale rates (over an hour old), rates unavailable, updating.

## Data changes and migration

There's no database: the backend keeps one JSON document (`filesDir/arro_state.json`). Every new field has a default, and the file is read with `ignoreUnknownKeys`, so older files load unchanged.

| Model | New fields |
|---|---|
| `Account` | `rateLocks`, `rateOffer`, `points`, `feeCreditMinor`, `loyaltyVersion` |
| `Transfer` | `rateLockId`, `pointsDiscountMinor` (and computed `chargesMinor`) |
| `Quote` | `lockId`, `discountMinor` (`lockedUntil` is now the hold's end, or 0 when nothing is held) |
| `Referral` | `friends: List<ReferredFriend>` |
| `Settings` | `widgetBalance` (default **off**) |
| `RatesSnapshot` | `demoMoved` |
| `DemoSettings` | `rateOffers` (default on) |
| New types | `RateOffer`, `RateLock`, `LockKind`, `PointsEntry`, `PointsKind`, `ReferredFriend`, `FriendStatus`; events `OfferAvailable`, `LockEnded` |

**Migration (`ArroBackend.migrate`, `LOYALTY_VERSION = 1`):** on first load of an older file, delivered transfers are credited with points (each transfer at most once, keyed `transfer:{id}`; the bonus is keyed `bonus:first-transfer`), and the sample account gets its sample referral records. It runs once per account.

## Backend API changes (`ArroBackend`)

New:

- `holdRate(to)`, `refreshHold(to)`: the 30-minute quote hold.
- `requestOffer(to, replace)`, `lockOffer(offerId)`, `dismissOffer()`, `ratesLive()`, `relevantCurrency(account)`, `foreground` (set by `MainActivity`).
- `redeemPoints(points)`, `referralTransferDelivered(friendId, minor)` (what a server would run when a referred friend's transfer is delivered).
- Demo only: `demoOfferNow()`, `demoEndLocks()`, `demoMoveMarket(cur, percent)`, `demoFriendJoins()`.

Changed:

- `quote()` uses the held or locked rate for that currency when there is one and carries its id; fee credit is applied as `discountMinor`.
- `requote()` holds today's rate again before re-pricing.
- `createTransfer()` now re-checks the price server-side: the rate must match an active, unused hold or lock for that currency (or today's live rate when none is held), and fee, card fee, credit and receive amount must match what the backend works out. Mismatches use the existing `"expired"` error code, so the review screen's existing refresh path handles them. The lock is marked used and credit is spent in the same atomic update.
- `tick()` ends offers, announces locks that ran out, makes random demo offers, and awards points on delivery.

Error codes added: `offer_ended`, `no_offer`, `offline` (offers), `points` (redemption).

## Configuration

`ArroBackend` companion: `QUOTE_HOLD_MS` (30 min), `OFFER_WINDOW_MS` (3 min), `OFFER_LOCK_MINUTES` (10), `RATES_FRESH_MS` (90 min), `RATES_REFRESH_MS` (60 min), `LOYALTY_VERSION`. `Rates.OFFER_MARGIN` (0.09%). `Loyalty`: bonus, points per pound, redemption tiers, referral amount, threshold and yearly cap. Random offers can be turned off in Demo controls.

## Permissions and Android setup

- **No new permissions.** Widget expiry redraws use an inexact, non-waking `AlarmManager.setWindow` alarm, so no exact-alarm permission is needed.
- **Manifest:** three `AppWidgetProvider` receivers (`.widget.QuickSendWidget`, `.widget.BalanceWidget`, `.widget.RateWidget`), `android:exported="false"`. Widget info in `res/xml/widget_*_info.xml`; layouts in `res/layout/widget_*.xml`; colours in `res/values/widget.xml` and `values-night/widget.xml`; Android 12+ corner radius in `values-v31`.
- **Data access:** widgets read the same in-process backend state as the screens. Nothing is exported to other apps. The balance and the people on Quick Send only appear when **Security → Show balance on home screen widgets** is on (off by default, on for the sample account), because widgets can be seen without unlocking.
- **Refresh:** widgets redraw as soon as what they show changes while Arro is running; `updatePeriodMillis` is 30 minutes (Android's minimum); the rate widget fetches rates when they're over an hour old (the free rate provider asks for at most one request an hour) or when you tap refresh, inside the broadcast's async window with an 8-second timeout.
- **Deep links:** widget and notification taps open `MainActivity` with a route that is applied after unlock. `MainActivity` now only accepts known routes (`Routes.isDeepLink`): `quicksend`, `quicksend:{recipientId}`, `quicksend_to:{country}`, `lock_rate/{cur}`, `rate/{cur}`, `track/{id}`, `chat`, `loyalty`.
- **Adding a widget:** long-press the home screen → Widgets → Arro → drag **Arro Quick Send**, **Arro balance** or **Arro rate**. Resize with a long-press.

## Testing

Automated (`./gradlew testDebugUnitTest`, 30 tests; 12 new in `LockAndRewardsTest`):

- a locked offer rate is used even after a live refresh and a market move; used once; next quote is at today's rate
- an ended lock is refused at send time, announced once, and a fresh quote holds today's rate again
- offers end, can't be locked after they end, need live rates, and can be renewed
- quote holds are checked server-side: edited rate, fee, receive amount or lock id, an ended hold, and an unheld quote at an old rate are all refused
- stale quote holds are re-priced when fresh rates arrive; locked offers aren't
- points are earned once on delivery, never for failed or cancelled transfers; backfill adds nothing twice; the first-transfer bonus
- redemption: tiers only, not more than you have, credit comes off the fee, is spent once and comes back on cancel
- referral reward: under £100 doesn't qualify, paid once per friend, unknown friends ignored, balance and ledger credited once, yearly cap
- migration of an older save file, and loading a file with none of the new fields

On a device (Fast mode on, sample account):

1. Wait on Home (or Demo controls → Show a rate offer now): the offer card and banner show the rate and countdown. Tap **Lock**.
2. Start a transfer to that country: the amount screen shows "Offer rate locked 9:xx" and the locked rate. Demo controls → Move the market: the live rate changes, the locked one doesn't.
3. Demo controls → End my locked rate now: the "locked rate has ended" banner and notification appear; the review screen asks for a new quote.
4. Send a transfer and let it deliver: Loyalty and rewards shows the new points (from Home's award button and from Account). Swap 1,000 points; the next quote shows "Points credit".
5. Demo controls → A friend joins, then Their first £120 transfer is delivered: £10 appears under Referrals and in the GBP balance.
6. Add the three widgets. On Quick Send, tap Send and then a person; narrow it to 2 or 3 columns to see fewer people. Toggle Security → Show balance on home screen widgets. On the rate widget tap Lock a rate and refresh, then turn on airplane mode and tap refresh.
7. Log out: the widgets ask you to log in.

## Limitations

- Offers are demo promotions priced from the live mid-market rate; a production pricing engine would replace `makeOffer`.
- Friends joining with your code are simulated (Demo controls), because sign-up with an invite code would happen on the friend's own phone against a real server.
- When Arro isn't running, Android 12+ may delay the widget redraw after an offer or lock ends by up to about 10 minutes. The widget always shows the end time, the app shows the true state, and the backend refuses an ended lock.
- Locks apply to transfers. Balance conversions keep using the live rate, as before.
