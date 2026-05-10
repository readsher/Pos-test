package com.postest.app.print

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * EMVCo / PromptPay QR payload generator.
 * Spec ref: PromptPay QR Standard (NITMX) — TLV format.
 *
 * - id: Thai mobile (e.g. "0812345678") or 13-digit national ID, or 15-digit eWallet ID
 * - amountTHB: nullable for "any-amount" QR
 */
object PromptPayQr {

    fun buildPayload(id: String, amountTHB: Double? = null): String {
        val cleaned = id.filter { it.isDigit() }
        val mobileOrNid = when (cleaned.length) {
            10 -> "0066" + cleaned.substring(1) // mobile, country code 66
            13 -> cleaned                       // citizen ID
            15 -> cleaned                       // eWallet
            else -> cleaned
        }

        // Tag 29 = Merchant Account Information for PromptPay
        val sub = buildString {
            append(tlv("00", "A000000677010111"))
            // 01 = mobile/NID, 02 = NID, 03 = eWallet — most use 01 for both mobile and NID with formatted value
            append(tlv("01", mobileOrNid))
        }

        val sb = StringBuilder()
        sb.append(tlv("00", "01"))                  // Payload format indicator
        sb.append(tlv("01", if (amountTHB == null) "11" else "12")) // 11=static, 12=dynamic
        sb.append(tlv("29", sub))
        sb.append(tlv("53", "764"))                 // Currency: THB
        if (amountTHB != null) sb.append(tlv("54", "%.2f".format(amountTHB)))
        sb.append(tlv("58", "TH"))                  // Country
        sb.append("6304")                            // CRC tag
        val crc = crc16(sb.toString())
        sb.append("%04X".format(crc))
        return sb.toString()
    }

    fun renderBitmap(payload: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (y in 0 until sizePx) for (x in 0 until sizePx) {
            bmp.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
        return bmp
    }

    private fun tlv(tag: String, value: String): String =
        tag + "%02d".format(value.length) + value

    // CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF, no xorout) — required by EMV QR
    private fun crc16(data: String): Int {
        var crc = 0xFFFF
        for (b in data.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc
    }
}
