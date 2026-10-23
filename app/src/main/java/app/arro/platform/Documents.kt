package app.arro.platform

import app.arro.ui.str
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import app.arro.R
import app.arro.core.Account
import app.arro.core.ArroBackend
import app.arro.core.Currency
import app.arro.core.FeedItem
import app.arro.core.LedgerType
import app.arro.core.Money
import app.arro.core.PayoutType
import app.arro.core.Text
import app.arro.core.Transfer
import app.arro.core.TransferStatus
import java.io.File

/** Generates the PDF and CSV documents the app lets people save and share. */
object Documents {
    private const val INK = 0xFF042D29.toInt()
    private const val LIME = 0xFFB0FE78.toInt()
    private const val SLATE = 0xFF5A6366.toInt()
    private const val LINE = 0xFFE2EBE5.toInt()
    private const val OK = 0xFF1A7340.toInt()

    private class Fonts(val regular: Typeface, val medium: Typeface, val bold: Typeface)

    private fun fonts(context: Context) = Fonts(
        ResourcesCompat.getFont(context, R.font.outfit_regular) ?: Typeface.DEFAULT,
        ResourcesCompat.getFont(context, R.font.outfit_medium) ?: Typeface.DEFAULT,
        ResourcesCompat.getFont(context, R.font.outfit_bold) ?: Typeface.DEFAULT_BOLD,
    )

    private fun paint(tf: Typeface, size: Float, color: Int = INK, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = tf; textSize = size; this.color = color; textAlign = align
    }

    /** Draws the Arro tile and wordmark at (x, y) top-left. */
    private fun brand(c: Canvas, f: Fonts, x: Float, y: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LIME }
        c.drawRoundRect(RectF(x, y, x + 26f, y + 26f), 7.6f, 7.6f, p)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; style = Paint.Style.STROKE; strokeWidth = 2.7f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val s = 26f / 48f
        c.drawCircle(x + 13.5f * s, y + 34.5f * s, 4.2f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })
        val path = android.graphics.Path().apply {
            moveTo(x + 19.5f * s, y + 29f * s); cubicTo(x + 23.5f * s, y + 22f * s, x + 28.5f * s, y + 17.5f * s, x + 35f * s, y + 14.2f * s)
            moveTo(x + 26.5f * s, y + 12.6f * s); lineTo(x + 35.6f * s, y + 14f * s); lineTo(x + 34f * s, y + 23f * s)
        }
        c.drawPath(path, ink)
        c.drawText(str(R.string.brand_wordmark), x + 34f, y + 20f, paint(f.bold, 20f))
    }

    private fun kv(c: Canvas, f: Fonts, left: Float, right: Float, y: Float, k: String, v: String, strong: Boolean = false) {
        c.drawText(k, left, y, paint(if (strong) f.bold else f.regular, 10.5f, if (strong) INK else SLATE))
        c.drawText(v, right, y, paint(if (strong) f.bold else f.medium, 10.5f, INK, Paint.Align.RIGHT))
    }

    private fun wrap(text: String, p: Paint, width: Float): List<String> {
        val out = mutableListOf<String>()
        var line = ""
        for (word in text.split(" ")) {
            val next = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(next) > width && line.isNotEmpty()) { out += line; line = word } else line = next
        }
        if (line.isNotEmpty()) out += line
        return out
    }

    private fun save(context: Context, doc: PdfDocument, name: String): File {
        val f = Share.cacheFile(context, name)
        try {
            f.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return f
    }

    /** A transfer receipt, laid out like the receipt screen (8.07). */
    fun transferReceipt(context: Context, a: Account, t: Transfer): File {
        val f = fonts(context)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(420, 595, 1).create())
        val c = page.canvas
        val l = 32f; val r = 388f
        c.drawColor(Color.WHITE)
        brand(c, f, l, 30f)
        val status = when (t.status) { TransferStatus.DELIVERED -> str(R.string.documents_delivered); TransferStatus.REFUNDED -> str(R.string.documents_refunded); TransferStatus.CANCELLED -> str(R.string.documents_cancelled); TransferStatus.FAILED -> str(R.string.documents_not_delivered); else -> str(R.string.documents_in_progress) }
        c.drawText(status, r, 48f, paint(f.bold, 10f, if (t.status == TransferStatus.DELIVERED) OK else SLATE, Paint.Align.RIGHT))
        c.drawText(str(R.string.documents_transfer_receipt), l, 92f, paint(f.medium, 8.5f, SLATE).apply { letterSpacing = 0.12f })
        c.drawText(Money.format(t.receiveMinor, t.toCur), l, 122f, paint(f.bold, 26f))
        c.drawText(str(R.string.documents_received_by_x, t.recipientName), l, 140f, paint(f.regular, 10.5f, SLATE))
        c.drawLine(l, 158f, r, 158f, Paint().apply { color = LINE; strokeWidth = 1f })
        val sender = str(R.string.documents_x_x_uk, a.displayName, a.profile.address?.city ?: str(R.string.documents_london))
        val rows = listOf(
            str(R.string.documents_sender) to sender,
            str(R.string.documents_recipient) to "${t.recipientName}, ${app.arro.core.Catalog.countryOrNull(t.country)?.city ?: ""}, ${t.country}",
            str(R.string.documents_delivered_to) to "${t.providerName} ${if (t.payoutType == PayoutType.MOBILE_MONEY) Text.formatPhone(t.payoutAccount) else t.payoutAccount}",
            str(R.string.documents_amount_sent) to Money.format(t.sendMinor, t.fromCur),
            str(R.string.documents_arro_fee) to Money.format(t.feeMinor, t.fromCur),
        ) + (if (t.fundingFeeMinor > 0) listOf(str(R.string.documents_card_fee) to Money.format(t.fundingFeeMinor, t.fromCur)) else emptyList()) + listOf(
            str(R.string.documents_total_paid) to Money.format(t.totalMinor, t.fromCur),
            str(R.string.documents_exchange_rate) to Money.rateLine(t.fromCur, t.toCur, t.rate),
            str(R.string.documents_paid_with) to t.fundingLabel,
            str(R.string.documents_created) to "${Text.dateTime(t.createdAt)} ${Text.zoneName(t.createdAt)}",
        ) + (if (t.status == TransferStatus.DELIVERED) listOf(str(R.string.documents_delivered) to "${Text.dateTime(t.deliveredAt)} ${Text.zoneName(t.deliveredAt)}") else emptyList()) + listOf(
            str(R.string.documents_reference) to t.reference,
        ) + (if (t.providerRef.isNotBlank()) listOf(str(R.string.documents_provider_reference) to t.providerRef) else emptyList())
        var y = 182f
        rows.forEach { (k, v) -> kv(c, f, l, r, y, k, v, k == str(R.string.documents_total_paid)); y += 21f }
        y += 10f
        c.drawLine(l, y, r, y, Paint().apply { color = LINE; strokeWidth = 1f })
        y += 20f
        val legal = str(R.string.documents_arro_ltd_is_authorised_by_regulator)
        wrap(legal, paint(f.regular, 8.5f, SLATE), r - l).forEach { c.drawText(it, l, y, paint(f.regular, 8.5f, SLATE)); y += 12f }
        doc.finishPage(page)
        return save(context, doc, str(R.string.documents_arro_receipt_x_pdf, t.reference))
    }

    /** A refund confirmation for a cancelled or undelivered transfer. */
    fun refundReceipt(context: Context, a: Account, t: Transfer): File {
        val f = fonts(context)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(420, 595, 1).create())
        val c = page.canvas
        val l = 32f; val r = 388f
        c.drawColor(Color.WHITE)
        brand(c, f, l, 30f)
        c.drawText(str(R.string.documents_refunded), r, 48f, paint(f.bold, 10f, OK, Paint.Align.RIGHT))
        c.drawText(str(R.string.documents_refund_receipt), l, 92f, paint(f.medium, 8.5f, SLATE).apply { letterSpacing = 0.12f })
        c.drawText(Money.format(t.totalMinor, t.fromCur), l, 122f, paint(f.bold, 26f))
        c.drawText(str(R.string.documents_refunded_to_x, if (t.fundingId == ArroBackend.BALANCE) str(R.string.documents_your_gbp_balance) else t.fundingLabel), l, 140f, paint(f.regular, 10.5f, SLATE))
        var y = 182f
        listOf(
            str(R.string.documents_account_holder) to a.displayName,
            str(R.string.documents_original_transfer) to str(R.string.documents_to_x_x, t.recipientName, Text.date(t.createdAt)),
            str(R.string.documents_original_reference) to t.reference,
            str(R.string.documents_refund_reference) to t.refundReference,
            str(R.string.documents_reason) to (if (t.status == TransferStatus.REFUNDED && t.failReason.isNotBlank()) str(R.string.documents_not_delivered_by_x, t.providerName) else t.cancelReason.ifBlank { str(R.string.documents_cancelled) }),
            str(R.string.documents_amount_sent) to Money.format(t.sendMinor, t.fromCur),
            str(R.string.documents_fees_refunded) to Money.format(t.feeMinor + t.fundingFeeMinor, t.fromCur),
            str(R.string.documents_total_refunded) to Money.format(t.totalMinor, t.fromCur),
            str(R.string.documents_refund_issued) to Text.dateTime(t.refundStartedAt),
            str(R.string.documents_completed) to if (t.refundedAt > 0) Text.date(t.refundedAt) else str(R.string.documents_in_progress),
        ).forEach { (k, v) -> kv(c, f, l, r, y, k, v, k == str(R.string.documents_total_refunded)); y += 21f }
        doc.finishPage(page)
        return save(context, doc, str(R.string.documents_arro_refund_x_pdf, t.refundReference.ifBlank { t.reference }))
    }

    data class Line(val at: Long, val description: String, val reference: String, val amountMinor: Long, val status: String)

    /** Statement lines for [cur] between [from] and [to], oldest first. */
    fun statementLines(backend: ArroBackend, a: Account, cur: Currency, from: Long, to: Long): List<Line> =
        backend.feedFor(a, cur, from, to).sortedBy { it.at }.map { item ->
            when (item) {
                is FeedItem.Sent -> {
                    val t = item.transfer
                    Line(t.createdAt, str(R.string.documents_to_x_x, t.recipientName, t.providerName), t.reference, -t.totalMinor,
                        when (t.status) { TransferStatus.DELIVERED -> str(R.string.documents_delivered); TransferStatus.REFUNDED -> str(R.string.documents_refunded); TransferStatus.CANCELLED -> str(R.string.documents_cancelled); TransferStatus.FAILED -> str(R.string.documents_failed); else -> str(R.string.documents_in_progress) })
                }
                is FeedItem.Entry -> {
                    val e = item.entry
                    val amount = if (e.currency == cur.code) e.amountMinor else e.counterMinor
                    Line(e.createdAt, when (e.type) {
                        LedgerType.CONVERSION -> str(R.string.documents_converted_x_to_x, e.currency, e.counterCurrency)
                        LedgerType.TOP_UP -> e.title
                        LedgerType.REWARD -> str(R.string.documents_invite_reward_x, e.subtitle)
                        LedgerType.REFUND -> str(R.string.documents_refund_x, e.subtitle)
                    }, e.id.take(8), amount, str(R.string.documents_completed))
                }
            }
        }

    fun statementCsv(context: Context, a: Account, cur: Currency, from: Long, to: Long, lines: List<Line>): File {
        val sb = StringBuilder(str(R.string.documents_date_time_description_reference_amount_currency))
        lines.forEach { l ->
            val amount = java.math.BigDecimal.valueOf(l.amountMinor).movePointLeft(cur.decimals).toPlainString()
            sb.append("${Text.numericDate(l.at)},${Text.time(l.at)},\"${l.description.replace("\"", "'")}\",${l.reference},$amount,${cur.code},${l.status}\n")
        }
        val f = Share.cacheFile(context, "Arro-statement-${cur.code}-${Text.localDate(from)}-to-${Text.localDate(to)}.csv")
        f.writeText(sb.toString())
        return f
    }

    fun statementPdf(context: Context, a: Account, cur: Currency, from: Long, to: Long, lines: List<Line>): File {
        val f = fonts(context)
        val doc = PdfDocument()
        val w = 595; val h = 842
        val l = 40f; val r = 555f
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var c: Canvas? = null
        var y = 0f
        fun header() {
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
            c = page!!.canvas
            val cv = c!!
            cv.drawColor(Color.WHITE)
            brand(cv, f, l, 36f)
            cv.drawText(str(R.string.documents_statement), r, 52f, paint(f.bold, 14f, INK, Paint.Align.RIGHT))
            cv.drawText(str(R.string.documents_x_account_x, cur.displayName, a.displayName), l, 96f, paint(f.medium, 11f))
            val w0 = a.wallet(cur)
            val details = when {
                w0 == null -> ""
                w0.iban.isNotBlank() -> "IBAN ${w0.iban}"
                else -> str(R.string.documents_sort_code_x_account_x, w0.sortCode, w0.accountNumber)
            }
            cv.drawText(details, l, 112f, paint(f.regular, 9.5f, SLATE))
            cv.drawText(str(R.string.documents_x_to_x, Text.date(from), Text.date(to)), r, 96f, paint(f.regular, 10f, SLATE, Paint.Align.RIGHT))
            cv.drawText(str(R.string.documents_page_x, pageNo), r, 112f, paint(f.regular, 9f, SLATE, Paint.Align.RIGHT))
            y = 146f
            val hp = paint(f.medium, 8.5f, SLATE)
            cv.drawText("DATE", l, y, hp); cv.drawText("DESCRIPTION", l + 70, y, hp); cv.drawText("REFERENCE", l + 330, y, hp)
            cv.drawText("AMOUNT", r, y, paint(f.medium, 8.5f, SLATE, Paint.Align.RIGHT))
            y += 8f
            cv.drawLine(l, y, r, y, Paint().apply { color = LINE; strokeWidth = 1f })
            y += 18f
        }
        header()
        var totalIn = 0L; var totalOut = 0L
        lines.forEach { line ->
            if (y > h - 90) { doc.finishPage(page!!); header() }
            val cv = c!!
            cv.drawText(Text.dayMonth(line.at), l, y, paint(f.regular, 9.5f))
            cv.drawText(line.description.take(48), l + 70, y, paint(f.regular, 9.5f))
            cv.drawText(line.reference, l + 330, y, paint(f.regular, 9f, SLATE))
            cv.drawText(Money.signed(line.amountMinor, cur), r, y, paint(f.medium, 9.5f, if (line.amountMinor > 0) OK else INK, Paint.Align.RIGHT))
            if (line.status != str(R.string.documents_delivered) && line.status != str(R.string.documents_completed)) cv.drawText(line.status, l + 70, y + 11, paint(f.regular, 8f, SLATE))
            if (line.amountMinor >= 0) totalIn += line.amountMinor else totalOut += -line.amountMinor
            y += if (line.status != str(R.string.documents_delivered) && line.status != str(R.string.documents_completed)) 26f else 20f
        }
        if (lines.isEmpty()) { c!!.drawText(str(R.string.documents_no_transactions_in_this_period), l, y, paint(f.regular, 10f, SLATE)); y += 20f }
        val cv = c!!
        y += 6f
        cv.drawLine(l, y, r, y, Paint().apply { color = LINE; strokeWidth = 1f })
        y += 20f
        kv(cv, f, l, r, y, str(R.string.documents_money_in), Money.format(totalIn, cur)); y += 18f
        kv(cv, f, l, r, y, str(R.string.documents_money_out), Money.format(totalOut, cur)); y += 18f
        kv(cv, f, l, r, y, str(R.string.documents_closing_balance_today), Money.format(a.wallet(cur)?.balanceMinor ?: 0, cur), true)
        cv.drawText(str(R.string.documents_arro_ltd_is_authorised_by_regulator_2), l, h - 40f, paint(f.regular, 8f, SLATE))
        doc.finishPage(page!!)
        return save(context, doc, str(R.string.documents_arro_statement_x_x_to_x, cur.code, Text.localDate(from), Text.localDate(to)))
    }
}
