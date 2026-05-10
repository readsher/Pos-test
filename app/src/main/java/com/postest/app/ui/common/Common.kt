package com.postest.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PinPad(
    title: String,
    onComplete: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
    error: String? = null,
    maxLen: Int = 6,
) {
    var pin by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("• ".repeat(pin.length).trim(), style = MaterialTheme.typography.headlineSmall)
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        val rows = listOf(
            listOf("1","2","3"),
            listOf("4","5","6"),
            listOf("7","8","9"),
            listOf("⌫","0","✓"),
        )
        for (row in rows) {
            Row {
                for (k in row) {
                    OutlinedButton(
                        onClick = {
                            when (k) {
                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                "✓" -> if (pin.length >= 4) onComplete(pin)
                                else -> if (pin.length < maxLen) pin += k
                            }
                        },
                        modifier = Modifier.padding(4.dp).size(64.dp),
                    ) { Text(k, style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        if (onCancel != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
