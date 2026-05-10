package com.postest.app.print

import android.content.Context
import android.graphics.Bitmap
import com.postest.app.data.entity.SettingRow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PrinterTarget {
    data class Lan(val ip: String) : PrinterTarget
    data class Bt(val mac: String) : PrinterTarget
    data object None : PrinterTarget
}

fun SettingRow.receiptPrinterTarget(): PrinterTarget = when (receiptPrinterTarget) {
    "STAR" -> if (starPrinterIp.isNotBlank()) PrinterTarget.Lan(starPrinterIp) else PrinterTarget.None
    "BT" -> if (btPrinterMac.isNotBlank()) PrinterTarget.Bt(btPrinterMac) else PrinterTarget.None
    else -> PrinterTarget.None
}

fun SettingRow.kitchenPrinterTarget(): PrinterTarget = when (kitchenPrinterTarget) {
    "STAR" -> if (starPrinterIp.isNotBlank()) PrinterTarget.Lan(starPrinterIp) else PrinterTarget.None
    "BT" -> if (btPrinterMac.isNotBlank()) PrinterTarget.Bt(btPrinterMac) else PrinterTarget.None
    else -> PrinterTarget.None
}

@Singleton
class PrinterService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val starPrinter: StarLanPrinter,
    private val btPrinter: BtEscPosPrinter,
) {
    suspend fun print(bitmap: Bitmap, target: PrinterTarget): Result<Unit> = runCatching {
        when (target) {
            is PrinterTarget.Lan -> starPrinter.printBitmap(target.ip, bitmap)
            is PrinterTarget.Bt -> btPrinter.printBitmap(target.mac, bitmap)
            PrinterTarget.None -> error("No printer configured")
        }
    }

    suspend fun testPrint(target: PrinterTarget): Result<Unit> {
        val testBmp = TestBitmap.make()
        return print(testBmp, target)
    }
}
