package app.arro.core

import java.time.LocalDate

/** Spending summaries for Home and the Insights screen. */
object Insights {
    data class Bucket(val label: String, val start: Long, val end: Long, val totalMinor: Long)
    data class Share(val recipientId: String, val name: String, val country: String, val totalMinor: Long)
    data class Summary(
        val periodLabel: String,
        val totalMinor: Long,
        val previousMinor: Long,
        val people: Int,
        val shares: List<Share>,
        val feesMinor: Long,
        val transfers: Int,
        val fastest: Transfer?,
        val daysLeft: Int,
    )

    enum class Period { MONTH, QUARTER, YEAR }

    private fun counted(a: Account) = a.transfers.filter { it.status == TransferStatus.DELIVERED || it.isOpen }

    /** Monthly totals for the last [n] months, oldest first (current month last). */
    fun months(a: Account, now: Long, n: Int = 6): List<Bucket> {
        val today = Text.localDate(now)
        return (n - 1 downTo 0).map { back ->
            val first = today.withDayOfMonth(1).minusMonths(back.toLong())
            val start = Text.startOfDay(first)
            val end = Text.startOfDay(first.plusMonths(1))
            Bucket(first.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, start, end,
                counted(a).filter { it.createdAt in start until end }.sumOf { it.sendMinor })
        }
    }

    private fun range(period: Period, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
        Period.MONTH -> today.withDayOfMonth(1).let { it to it.plusMonths(1) }
        Period.QUARTER -> today.withDayOfMonth(1).minusMonths(((today.monthValue - 1) % 3).toLong()).let { it to it.plusMonths(3) }
        Period.YEAR -> today.withDayOfYear(1).let { it to it.plusYears(1) }
    }

    fun summary(a: Account, now: Long, period: Period = Period.MONTH): Summary {
        val today = Text.localDate(now)
        val (from, to) = range(period, today)
        val start = Text.startOfDay(from); val end = Text.startOfDay(to)
        val prevFrom = when (period) { Period.MONTH -> from.minusMonths(1); Period.QUARTER -> from.minusMonths(3); Period.YEAR -> from.minusYears(1) }
        val prevStart = Text.startOfDay(prevFrom)
        val list = counted(a).filter { it.createdAt in start until end }
        val prev = counted(a).filter { it.createdAt in prevStart until start }
        val shares = list.groupBy { it.recipientId }.map { (id, ts) -> Share(id, ts.first().recipientName, ts.first().country, ts.sumOf { it.sendMinor }) }
            .sortedByDescending { it.totalMinor }
        val fastest = list.filter { it.status == TransferStatus.DELIVERED }.minByOrNull { it.deliveredAt - it.createdAt }
        val label = when (period) {
            Period.MONTH -> "${from.dayOfMonth} to ${today.dayOfMonth} ${Text.monthName(now).take(3)}"
            Period.QUARTER -> "Q${(from.monthValue - 1) / 3 + 1} ${from.year}"
            Period.YEAR -> "${from.year}"
        }
        return Summary(label, list.sumOf { it.sendMinor }, prev.sumOf { it.sendMinor }, shares.size, shares,
            list.sumOf { it.feeMinor + it.fundingFeeMinor }, list.size, fastest,
            (java.time.temporal.ChronoUnit.DAYS.between(today, to) - 1).toInt().coerceAtLeast(0))
    }

    /** Recipients ordered by how recently they were paid; favourites first. */
    fun recentRecipients(a: Account): List<Recipient> {
        val last = a.transfers.groupBy { it.recipientId }.mapValues { (_, v) -> v.maxOf { it.createdAt } }
        return a.recipients.sortedWith(compareByDescending<Recipient> { it.favourite }.thenByDescending { last[it.id] ?: it.createdAt })
    }

    fun sentThisYear(a: Account, recipientId: String, now: Long): Long {
        val start = Text.startOfDay(Text.localDate(now).withDayOfYear(1))
        return counted(a).filter { it.recipientId == recipientId && it.createdAt >= start }.sumOf { it.sendMinor }
    }
}
