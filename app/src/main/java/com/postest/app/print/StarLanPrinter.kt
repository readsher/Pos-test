package com.postest.app.print

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Star TSP100III over LAN, port 9100 (raw socket).
 *
 * Approach: send ESC/POS raster bitmap commands. The TSP100III must be configured
 * in **ESC/POS emulation mode** (one-time setup via futurePRNT or printer config —
 * see README). This avoids needing the proprietary StarPRNT SDK in the build.
 *
 * If you prefer the official StarPRNT SDK, drop the AARs into app/libs/ and
 * replace the body of [printBitmap] with StarIOPort + StarIoExt calls — same
 * `bitmap` is the input.
 */
@Singleton
class StarLanPrinter @Inject constructor() {

    suspend fun printBitmap(ip: String, bitmap: Bitmap, port: Int = 9100, timeoutMs: Int = 5000) =
        withContext(Dispatchers.IO) {
            Socket().use { sock ->
                sock.connect(InetSocketAddress(ip, port), timeoutMs)
                BufferedOutputStream(sock.getOutputStream()).use { out ->
                    out.write(EscPos.init())
                    out.write(EscPos.center())
                    out.write(EscPos.rasterImage(bitmap))
                    out.write(EscPos.feed(3))
                    out.write(EscPos.cut())
                    out.flush()
                }
            }
        }
}

internal object EscPos {

    fun init(): ByteArray = byteArrayOf(0x1B, 0x40)        // ESC @
    fun center(): ByteArray = byteArrayOf(0x1B, 0x61, 1)   // ESC a 1
    fun feed(n: Int): ByteArray = byteArrayOf(0x1B, 0x64, n.toByte()) // ESC d n
    fun cut(): ByteArray = byteArrayOf(0x1D, 0x56, 0x00)   // GS V 0 — full cut

    /**
     * GS v 0 raster bit image command.
     * Format: GS v 0 m xL xH yL yH [data]
     *   m = 0 (normal), xL/xH = bytes per row, yL/yH = rows
     */
    fun rasterImage(src: Bitmap): ByteArray {
        // Convert to monochrome 1-bit per pixel, MSB-first per row, padded to byte.
        val w = src.width
        val h = src.height
        val widthBytes = (w + 7) / 8
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val data = ByteArray(widthBytes * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val argb = pixels[y * w + x]
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                val luma = (r * 30 + g * 59 + b * 11) / 100
                if (luma < 128) {
                    val byteIdx = y * widthBytes + (x ushr 3)
                    val bit = 0x80 ushr (x and 7)
                    data[byteIdx] = (data[byteIdx].toInt() or bit).toByte()
                }
            }
        }
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (h and 0xFF).toByte()
        val yH = ((h shr 8) and 0xFF).toByte()
        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
        return header + data
    }

    @Suppress("unused")
    private fun isDark(c: Int): Boolean = Color.luminance(c) < 0.5f
}
