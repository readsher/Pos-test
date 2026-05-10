package com.postest.app.print

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth ESC/POS thermal printer (generic).
 *
 * Uses Android's BluetoothSocket directly (SPP UUID 00001101-0000-1000-8000-00805F9B34FB)
 * and sends the same ESC/POS raster commands used by [StarLanPrinter]. Avoids pulling
 * extra dependencies for the basic path.
 *
 * Caller must already have BLUETOOTH_CONNECT permission and the device paired.
 */
@Singleton
class BtEscPosPrinter @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun pairedDevices(): List<BluetoothDevice> {
        val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return emptyList()
        val adapter: BluetoothAdapter = mgr.adapter ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    suspend fun printBitmap(mac: String, bitmap: Bitmap, timeoutMs: Int = 8000) =
        withContext(Dispatchers.IO) {
            val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = mgr.adapter ?: error("No Bluetooth adapter")
            val device: BluetoothDevice = adapter.getRemoteDevice(mac)
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(sppUuid)
            try {
                socket.connect()
                BufferedOutputStream(socket.outputStream).use { out ->
                    out.write(EscPos.init())
                    out.write(EscPos.center())
                    out.write(EscPos.rasterImage(bitmap))
                    out.write(EscPos.feed(3))
                    out.write(EscPos.cut())
                    out.flush()
                }
            } finally {
                runCatching { socket.close() }
            }
        }
}
