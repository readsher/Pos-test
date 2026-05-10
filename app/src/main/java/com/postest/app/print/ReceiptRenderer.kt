package com.postest.app.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.postest.app.data.entity.OrderItem
import com.postest.app.data.entity.Payment
import com.postest.app.data.entity.PaymentMethod
import com.postest.app.data.entity.PosOrder
import com.postest.app.data.entity.SettingRow
import com.postest.app.data.entity.Shift
import com.postest.app.data.repo.ShiftSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ReceiptData(
    val order: PosOrder,
    val items: List<OrderItem>,
    val payments: List<Payment>,
    val cashier: String,
)

data class KitchenTicket(
    val orderId: Long,
    val tableLabel: String?,
    val items: List<OrderItem>,
    val cashier: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Singleton
class ReceiptRenderer @Inject constructor() {

    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun renderReceipt(data: ReceiptData, settings: SettingRow, lang: String, qr: Bitmap? = null): Bitmap {
        val widthPx = paperWidthPx(settings.paperWidthMm)
        val lines = mutableListOf<Line>()
        lines += Line(settings.shopName, big = true, center = true)
        if (settings.shopAddress.isNotBlank()) lines += Line(settings.shopAddress, center = true)
        lines += Line(rule(widthPx))
        lines += Line(loc("Receipt", "ใบเสร็จ", lang) + " #${data.order.id}")
        lines += Line(df.format(Date(data.order.createdAt)))
        lines += Line(loc("Cashier: ", "แคชเชียร์: ", lang) + data.cashier)
        if (data.order.tableId != null) lines += Line(loc("Table #${data.order.tableId}", "โต๊ะ #${data.order.tableId}", lang))
        lines += Line(rule(widthPx))
        for (it in data.items) {
            val name = if (lang == "th") it.nameTh else it.nameEn
            lines += Line("${it.qty} x $name")
            lines += Line(money(it.unitPriceCents * it.qty), right = true)
            if (!it.note.isNullOrBlank()) lines += Line("  * ${it.note}")
        }
        lines += Line(rule(widthPx))
        lines += twoCol(loc("Subtotal", "ยอดรวม", lang), money(data.order.subtotalCents))
        if (data.order.taxCents > 0) lines += twoCol(loc("Tax", "ภาษี", lang), money(data.order.taxCents))
        lines += twoCol(loc("TOTAL", "รวมทั้งสิ้น", lang), money(data.order.totalCents)).copy(big = true)
        for (p in data.payments) {
            val mname = when (p.method) {
                PaymentMethod.CASH -> loc("Cash", "เงินสด", lang)
                PaymentMethod.PROMPTPAY -> loc("PromptPay", "พร้อมเพย์", lang)
            }
            lines += twoCol(mname, money(p.amountCents))
            if (p.method == PaymentMethod.CASH && p.tenderedCents != null) {
                lines += twoCol(loc("Tendered", "รับเงิน", lang), money(p.tenderedCents))
                lines += twoCol(loc("Change", "เงินทอน", lang), money(p.tenderedCents - p.amountCents))
            }
        }
        lines += Line(rule(widthPx))
        lines += Line(loc("Thank you!", "ขอบคุณค่ะ!", lang), center = true)
        return rasterize(lines, widthPx, qrImage = qr)
    }

    fun renderKitchenTicket(t: KitchenTicket, settings: SettingRow, lang: String): Bitmap {
        val widthPx = paperWidthPx(settings.paperWidthMm)
        val lines = mutableListOf<Line>()
        lines += Line(loc("KITCHEN", "ครัว", lang), big = true, center = true)
        lines += Line(df.format(Date(t.createdAt)))
        lines += Line(loc("Order #${t.orderId}", "บิล #${t.orderId}", lang))
        if (t.tableLabel != null) lines += Line(loc("Table ", "โต๊ะ ", lang) + t.tableLabel, big = true)
        lines += Line(loc("By: ", "โดย: ", lang) + t.cashier)
        lines += Line(rule(widthPx))
        for (it in t.items) {
            val name = if (lang == "th") it.nameTh else it.nameEn
            lines += Line("${it.qty} x $name", big = true)
            if (!it.note.isNullOrBlank()) lines += Line("  * ${it.note}")
        }
        lines += Line(rule(widthPx))
        return rasterize(lines, widthPx)
    }

    fun renderShiftReport(summary: ShiftSummary, settings: SettingRow, lang: String, label: String): Bitmap {
        val widthPx = paperWidthPx(settings.paperWidthMm)
        val s = summary.shift
        val lines = mutableListOf<Line>()
        lines += Line(label, big = true, center = true)
        lines += Line(settings.shopName, center = true)
        lines += Line(rule(widthPx))
        lines += twoCol(loc("Opened", "เปิดเมื่อ", lang), df.format(Date(s.openedAt)))
        if (s.closedAt != null) lines += twoCol(loc("Closed", "ปิดเมื่อ", lang), df.format(Date(s.closedAt)))
        lines += twoCol(loc("Tx count", "จำนวนรายการ", lang), summary.txCount.toString())
        lines += twoCol(loc("Sales", "ยอดขาย", lang), money(summary.totalSalesCents))
        lines += Line(rule(widthPx))
        lines += Line(loc("By payment method:", "ตามวิธีชำระเงิน:", lang))
        for ((m, amt) in summary.byMethod) {
            val mname = if (m == PaymentMethod.CASH) loc("Cash", "เงินสด", lang) else loc("PromptPay", "พร้อมเพย์", lang)
            lines += twoCol("  $mname", money(amt))
        }
        lines += Line(rule(widthPx))
        lines += twoCol(loc("Opening cash", "เงินสดเริ่มต้น", lang), money(s.openingCashCents))
        lines += twoCol(loc("Expected cash", "เงินสดที่คาดไว้", lang), money(summary.expectedCashCents))
        if (s.closingCashCountedCents != null) {
            lines += twoCol(loc("Counted cash", "เงินสดที่นับได้", lang), money(s.closingCashCountedCents))
            lines += twoCol(loc("Over/short", "เกิน/ขาด", lang), money(summary.overShortCents ?: 0))
        }
        lines += Line(rule(widthPx))
        return rasterize(lines, widthPx)
    }

    // ---- impl ----

    private data class Line(
        val text: String,
        val big: Boolean = false,
        val center: Boolean = false,
        val right: Boolean = false,
    )

    private fun twoCol(left: String, right: String): Line =
        Line("$left|$right".let { it }, big = false).let {
            Line(text = padTwoCols(left, right))
        }

    private fun padTwoCols(left: String, right: String, width: Int = 32): String {
        val padCount = (width - left.length - right.length).coerceAtLeast(1)
        return left + " ".repeat(padCount) + right
    }

    private fun rule(widthPx: Int): String = "-".repeat(charsForWidth(widthPx))

    private fun charsForWidth(widthPx: Int) = if (widthPx <= 384) 32 else 48

    private fun money(cents: Long): String = "฿%.2f".format(cents / 100.0)

    private fun loc(en: String, th: String, lang: String): String = if (lang == "th") th else en

    private fun paperWidthPx(mm: Int): Int = when (mm) { 58 -> 384; 80 -> 576; else -> 576 }

    private fun rasterize(lines: List<Line>, widthPx: Int, qrImage: Bitmap? = null): Bitmap {
        val pTextSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.MONOSPACE
        }
        val pTextBig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val lineHeight = 30
        val bigLineHeight = 42

        // measure
        var height = 16
        for (l in lines) height += if (l.big) bigLineHeight else lineHeight
        if (qrImage != null) height += qrImage.height + 16

        val bmp = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        var y = 24
        for (l in lines) {
            val paint = if (l.big) pTextBig else pTextSmall
            val x = when {
                l.center -> ((widthPx - paint.measureText(l.text)) / 2).coerceAtLeast(0f)
                l.right -> (widthPx - paint.measureText(l.text) - 4f).coerceAtLeast(0f)
                else -> 4f
            }
            canvas.drawText(l.text, x, y.toFloat(), paint)
            y += if (l.big) bigLineHeight else lineHeight
        }
        if (qrImage != null) {
            val qx = ((widthPx - qrImage.width) / 2).coerceAtLeast(0)
            canvas.drawBitmap(qrImage, qx.toFloat(), y.toFloat(), null)
        }
        return bmp
    }
}
