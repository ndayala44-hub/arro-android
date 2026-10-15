package app.arro.platform

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
        c.drawText("arro", x + 34f, y + 20f, paint(f.bold, 20f))
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
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
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
        val status = when (t.status) { TransferStatus.DELIVERED -> "Delivered"; TransferStatus.REFUNDED -> "Refunded"; TransferStatus.CANCELLED -> "Cancelled"; TransferStatus.FAILED -> "Not delivered"; else -> "In progress" }
        c.drawText(status, r, 48f, paint(f.bold, 10f, if (t.status == TransferStatus.DELIVERED) OK else SLATE, Paint.Align.RIGHT))
        c.drawText("TRANSFER RECEIPT", l, 92f, paint(f.medium, 8.5f, SLATE).apply { letterSpacing = 0.12f })
        c.drawText(Money.format(t.receiveMinor, t.toCur), l, 122f, paint(f.bold, 26f))
        c.drawText("received by ${t.recipientName}", l, 140f, paint(f.regular, 10.5f, SLATE))
        c.drawLine(l, 158f, r, 158f, Paint().apply { color = LINE; strokeWidth = 1f })
        val sender = "${a.displayName}, ${a.profile.address?.city ?: "London"}, UK"
        val rows = listOf(
            "Sender" to sender,
            "Recipient" to "${t.recipientName}, ${app.arro.core.Catalog.countryOrNull(t.country)?.city ?: ""}, ${t.country}",
            "Delivered to" to "${t.providerName} ${if (t.payoutType == PayoutType.MOBILE_MONEY) Text.formatPhone(t.payoutAccount) else t.payoutAccount}",
            "Amount sent" to Money.format(t.sendMinor, t.fromCur),
            "Arro fee" to Money.format(t.feeMinor, t.fromCur),
        ) + (if (t.fundingFeeMinor > 0) listOf("Card fee" to Money.format(t.fundingFeeMinor, t.fromCur)) else emptyList()) + listOf(
            "Total paid" to Money.format(t.totalMinor, t.fromCur),
            "Exchange rate" to Money.rateLine(t.fromCur, t.toCur, t.rate),
            "Paid with" to t.fundingLabel,
            "Created" to "${Text.dateTime(t.createdAt)} ${Text.zoneName(t.createdAt)}",
        ) + (if (t.status == TransferStatus.DELIVERED) listOf("Delivered" to "${Text.dateTime(t.deliveredAt)} ${Text.zoneName(t.deliveredAt)}") else emptyList()) + listOf(
            "Reference" to t.reference,
        ) + (if (t.providerRef.isNotBlank()) listOf("Provider reference" to t.providerRef) else emptyList())
        var y = 182f
        rows.forEach { (k, v) -> kv(c, f, l, r, y, k, v, k == "Total paid"); y += 21f }
        y += 10f
        c.drawLine(l, y, r, y, Paint().apply { color = LINE; strokeWidth = 1f })
        y += 20f
        val legal = "Arro Ltd is authorised by [REGULATOR] as [LICENCE TYPE], reference [LICENCE NO.]. Questions? help@[DOMAIN] · Keep this receipt for your records."
        wrap(legal, paint(f.regular, 8.5f, SLATE), r - l).forEach { c.drawText(it, l, y, paint(f.regular, 8.5f, SLATE)); y += 12f }
        doc.finishPage(page)
        return save(context, doc, "Arro-receipt-${t.reference}.pdf")
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
        c.drawText("Refunded", r, 48f, paint(f.bold, 10f, OK, Paint.Align.RIGHT))
        c.drawText("REFUND RECEIPT", l, 92f, paint(f.medium, 8.5f, SLATE).apply { letterSpacing = 0.12f })
        c.drawText(Money.format(t.totalMinor, t.fromCur), l, 122f, paint(f.bold, 26f))
        c.drawText("refunded to ${if (t.fundingId == ArroBackend.BALANCE) "your GBP balance" else t.fundingLabel}", l, 140f, paint(f.regular, 10.5f, SLATE))
        var y = 182f
        listOf(
            "Account holder" to a.displayName,
            "Original transfer" to "To ${t.recipientName} · ${Text.date(t.createdAt)}",
            "Original reference" to t.reference,
            "Refund reference" to t.refundReference,
            "Reason" to (if (t.status == TransferStatus.REFUNDED && t.failReason.isNotBlank()) "Not delivered by ${t.providerName}" else t.cancelReason.ifBlank { "Cancelled" }),
            "Amount sent" to Money.format(t.sendMinor, t.fromCur),
            "Fees refunded" to Money.format(t.feeMinor + t.fundingFeeMinor, t.fromCur),
            "Total refunded" to Money.format(t.totalMinor, t.fromCur),
            "Refund issued" to Text.dateTime(t.refundStartedAt),
            "Completed" to if (t.refundedAt > 0) Text.date(t.refundedAt) else "In progress",
        ).forEach { (k, v) -> kv(c, f, l, r, y, k, v, k == "Total refunded"); y += 21f }
        doc.finishPage(page)
        return save(context, doc, "Arro-refund-${t.refundReference.ifBlank { t.reference }}.pdf")
    }

    data class Line(val at: Long, val description: String, val reference: String, val amountMinor: Long, val status: String)

    /** Statement lines for [cur] between [from] and [to], oldest first. */
    fun statementLines(backend: ArroBackend, a: Account, cur: Currency, from: Long, to: Long): List<Line> =
        backend.feedFor(a, cur, from, to).sortedBy { it.at }.map { item ->
            when (item) {
                is FeedItem.Sent -> {
                    val t = item.transfer
                    Line(t.createdAt, "To ${t.recipientName} · ${t.providerName}", t.reference, -t.totalMinor,
                        when (t.status) { TransferStatus.DELIVERED -> "Delivered"; TransferStatus.REFUNDED -> "Refunded"; TransferStatus.CANCELLED -> "Cancelled"; TransferStatus.FAILED -> "Failed"; else -> "In progress" })
                }
                is FeedItem.Entry -> {
                    val e = item.entry
                    val amount = if (e.currency == cur.code) e.amountMinor else e.counterMinor
                    Line(e.createdAt, when (e.type) {
                        LedgerType.CONVERSION -> "Converted ${e.currency} to ${e.counterCurrency}"
                        LedgerType.TOP_UP -> e.title
                        LedgerType.REWARD -> "Invite reward · ${e.subtitle}"
                        LedgerType.REFUND -> "Refund · ${e.subtitle}"
                    }, e.id.take(8), amount, "Completed")
                }
            }
        }

    fun statementCsv(context: Context, a: Account, cur: Currency, from: Long, to: Long, lines: List<Line>): File {
        val sb = StringBuilder("Date,Time,Description,Reference,Amount,Currency,Status\n")
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
            cv.drawText("Statement", r, 52f, paint(f.bold, 14f, INK, Paint.Align.RIGHT))
            cv.drawText("${cur.displayName} account · ${a.displayName}", l, 96f, paint(f.medium, 11f))
            val w0 = a.wallet(cur)
            val details = when {
                w0 == null -> ""
                w0.iban.isNotBlank() -> "IBAN ${w0.iban}"
                else -> "Sort code ${w0.sortCode} · Account ${w0.accountNumber}"
            }
            cv.drawText(details, l, 112f, paint(f.regular, 9.5f, SLATE))
            cv.drawText("${Text.date(from)} to ${Text.date(to)}", r, 96f, paint(f.regular, 10f, SLATE, Paint.Align.RIGHT))
            cv.drawText("Page $pageNo", r, 112f, paint(f.regular, 9f, SLATE, Paint.Align.RIGHT))
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
            if (line.status != "Delivered" && line.status != "Completed") cv.drawText(line.status, l + 70, y + 11, paint(f.regular, 8f, SLATE))
            if (line.amountMinor >= 0) totalIn += line.amountMinor else totalOut += -line.amountMinor
            y += if (line.status != "Delivered" && line.status != "Completed") 26f else 20f
        }
        if (lines.isEmpty()) { c!!.drawText("No transactions in this period.", l, y, paint(f.regular, 10f, SLATE)); y += 20f }
        val cv = c!!
        y += 6f
        cv.drawLine(l, y, r, y, Paint().apply { color = LINE; strokeWidth = 1f })
        y += 20f
        kv(cv, f, l, r, y, "Money in", Money.format(totalIn, cur)); y += 18f
        kv(cv, f, l, r, y, "Money out", Money.format(totalOut, cur)); y += 18f
        kv(cv, f, l, r, y, "Closing balance today", Money.format(a.wallet(cur)?.balanceMinor ?: 0, cur), true)
        cv.drawText("Arro Ltd is authorised by [REGULATOR] as [LICENCE TYPE], reference [LICENCE NO.].", l, h - 40f, paint(f.regular, 8f, SLATE))
        doc.finishPage(page!!)
        return save(context, doc, "Arro-statement-${cur.code}-${Text.localDate(from)}-to-${Text.localDate(to)}.pdf")
    }
}
