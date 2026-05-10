package com.postest.app.util

object Money {
    fun fmt(cents: Long): String = "฿%.2f".format(cents / 100.0)
    fun parse(text: String): Long? {
        val t = text.trim().replace("฿", "").replace(",", "")
        val v = t.toDoubleOrNull() ?: return null
        return (v * 100).toLong()
    }
}
