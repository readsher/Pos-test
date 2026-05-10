package com.postest.app.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object TestBitmap {
    fun make(widthPx: Int = 576): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, 200, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        c.drawText("PosTest — TEST PRINT", 20f, 60f, p)
        p.textSize = 24f
        p.typeface = Typeface.MONOSPACE
        c.drawText("If you can read this, the printer", 20f, 110f, p)
        c.drawText("is wired up correctly.", 20f, 144f, p)
        c.drawText("ทดสอบภาษาไทย: สวัสดี", 20f, 180f, p)
        return bmp
    }
}
