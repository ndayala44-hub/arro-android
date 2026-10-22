package app.arro.core

import java.time.Instant

/**
 * Arro Rewards rules, kept in one place so the backend, the seed data and the tests agree.
 *
 * Points: 1 point for every 1p of Arro fee actually paid, credited once the transfer is delivered, plus a one-off
 * bonus on the first delivered transfer to someone outside Arro. 1,000 points are worth £1.00 of fee credit, which is
 * taken off the next transfer fees automatically.
 *
 * Referrals follow the existing Invite rewards terms: £10 into the GBP balance once a friend's first transfer of
 * £100 or more is delivered, once per friend, up to 20 rewards a year.
 */
object Loyalty {
    const val FIRST_TRANSFER_BONUS = 250
    /** Points per £1.00 (100p) of fee credit. */
    const val POINTS_PER_POUND = 1_000
    const val REFERRAL_REWARD_MINOR = 1_000L
    const val REFERRAL_QUALIFY_MINOR = 10_000L
    const val REFERRAL_YEARLY_CAP = 20

    /** Redemption options: points to fee credit (minor units of GBP). */
    val tiers: List<Pair<Int, Long>> = listOf(1_000 to 100L, 2_500 to 250L, 5_000 to 500L, 10_000 to 1_000L)

    fun creditFor(points: Int): Long = points.toLong() * 100 / POINTS_PER_POUND

    /** Points a transfer earns once delivered: the Arro fee actually paid, in pence. */
    fun pointsFor(t: Transfer): Int =
        if (t.status != TransferStatus.DELIVERED) 0 else preview(t.feeMinor, t.pointsDiscountMinor)

    /** Points that a fee of [feeMinor] after [discountMinor] credit will earn. */
    fun preview(feeMinor: Long, discountMinor: Long): Int = (feeMinor - discountMinor).coerceAtLeast(0).toInt()

    /** Points the transfer in [q] will earn once delivered, including the first-transfer bonus if it's still to come. */
    fun pointsToEarn(a: Account, q: Quote): Int =
        preview(q.feeMinor, q.discountMinor) + if (q.payoutType != PayoutType.ARRO && a.points.none { it.ref == BONUS_REF }) FIRST_TRANSFER_BONUS else 0

    fun earnedRef(transferId: String) = "transfer:$transferId"
    const val BONUS_REF = "bonus:first-transfer"

    /**
     * Adds the points [t] earns, if it hasn't earned them already. Returns the same account when there is nothing to
     * add, so it is safe to call every time a transfer is seen as delivered.
     */
    fun award(a: Account, t: Transfer, now: Long): Account {
        if (t.status != TransferStatus.DELIVERED) return a
        val at = if (t.deliveredAt > 0) minOf(t.deliveredAt, now) else now
        val add = mutableListOf<PointsEntry>()
        val ref = earnedRef(t.id)
        val pts = pointsFor(t)
        if (pts > 0 && a.points.none { it.ref == ref }) {
            add += PointsEntry(Ids.id(), PointsKind.TRANSFER, pts, ref, "Transfer to ${t.recipientName}",
                "${Money.format(t.feeMinor - t.pointsDiscountMinor, t.fromCur)} Arro fee", at)
        }
        if (t.payoutType != PayoutType.ARRO && a.points.none { it.ref == BONUS_REF }) {
            add += PointsEntry(Ids.id(), PointsKind.BONUS, FIRST_TRANSFER_BONUS, BONUS_REF, "First transfer bonus",
                "Welcome to Arro Rewards", at)
        }
        if (add.isEmpty()) return a
        return a.copy(points = (add + a.points).sortedByDescending { it.createdAt })
    }

    /** Credits every delivered transfer that hasn't earned its points yet, oldest first (the first gets the bonus). */
    fun backfill(a: Account, now: Long): Account =
        a.transfers.filter { it.status == TransferStatus.DELIVERED }.sortedBy { it.deliveredAt }.fold(a) { acc, t -> award(acc, t, now) }

    /** Rewards already paid in the calendar year of [now]. */
    fun rewardsThisYear(r: Referral, now: Long): Int {
        val year = year(now)
        return r.friends.count { it.rewardMinor > 0 && year(it.qualifiedAt) == year }
    }

    /**
     * Applies the referral rules to a delivered transfer of [transferMinor] by the friend [friendId]. Returns the
     * updated referral and the reward to pay into the GBP balance (0 when the friend doesn't qualify, has already
     * qualified, or the yearly cap is reached).
     */
    fun qualify(r: Referral, friendId: String, transferMinor: Long, now: Long): Pair<Referral, Long> {
        val f = r.friends.firstOrNull { it.id == friendId } ?: return r to 0
        if (f.status != FriendStatus.SIGNED_UP || transferMinor < REFERRAL_QUALIFY_MINOR) return r to 0
        val reward = if (rewardsThisYear(r, now) < REFERRAL_YEARLY_CAP) REFERRAL_REWARD_MINOR else 0
        val updated = f.copy(status = FriendStatus.QUALIFIED, qualifyingMinor = transferMinor, qualifiedAt = now, rewardMinor = reward)
        val friends = r.friends.map { if (it.id == friendId) updated else it }
        return r.copy(friends = friends, earnedMinor = r.earnedMinor + reward, friendsJoined = maxOf(r.friendsJoined, friends.size)) to reward
    }

    private fun year(t: Long) = Instant.ofEpochMilli(t).atZone(Text.zone).year
}
