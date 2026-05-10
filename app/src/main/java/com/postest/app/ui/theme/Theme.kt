package com.postest.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F4C75),
    onPrimary = Color.White,
    secondary = Color(0xFF3282B8),
    background = Color(0xFFF6F8FA),
    surface = Color.White,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3282B8),
    onPrimary = Color.White,
    secondary = Color(0xFFBBE1FA),
    background = Color(0xFF0B132B),
    surface = Color(0xFF1C2541),
)

@Composable
fun PosTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
