package com.postest.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Claude-inspired palette: warm coral on cream/ivory.
// Primary references the "Claude Orange" brand color.

private val ClaudeOrange      = Color(0xFFD97757)
private val ClaudeOrangeDeep  = Color(0xFFC96442)
private val ClaudeOrangeSoft  = Color(0xFFE8916D)
private val ClaudeCream       = Color(0xFFFAF9F5)
private val ClaudeCreamAlt    = Color(0xFFF5F4ED)
private val ClaudeInk         = Color(0xFF181818)
private val ClaudeInkSoft     = Color(0xFF2C2B27)
private val ClaudeBrown       = Color(0xFF5D4E40)
private val ClaudeBrownLight  = Color(0xFFA89888)
private val ClaudeNightBg     = Color(0xFF1F1E1B)
private val ClaudeNightSurface= Color(0xFF2A2926)
private val ClaudeNightText   = Color(0xFFEDE9DE)

private val LightColors = lightColorScheme(
    primary             = ClaudeOrangeDeep,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFF7E1D5),
    onPrimaryContainer  = ClaudeInkSoft,
    secondary           = ClaudeBrown,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFE8E1D6),
    onSecondaryContainer= ClaudeInkSoft,
    tertiary            = ClaudeOrange,
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFFBE6D9),
    onTertiaryContainer = ClaudeInkSoft,
    background          = ClaudeCream,
    onBackground        = ClaudeInk,
    surface             = Color.White,
    onSurface           = ClaudeInk,
    surfaceVariant      = ClaudeCreamAlt,
    onSurfaceVariant    = ClaudeInkSoft,
    outline             = ClaudeBrownLight,
    error               = Color(0xFFB3261E),
    onError             = Color.White,
    errorContainer      = Color(0xFFF9DEDC),
    onErrorContainer    = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary             = ClaudeOrangeSoft,
    onPrimary           = Color(0xFF2A1208),
    primaryContainer    = Color(0xFF7A3D26),
    onPrimaryContainer  = Color(0xFFFBE2D5),
    secondary           = Color(0xFFCAB6A2),
    onSecondary         = Color(0xFF2C2419),
    secondaryContainer  = Color(0xFF4A3F30),
    onSecondaryContainer= Color(0xFFEFE3D2),
    tertiary            = ClaudeOrange,
    onTertiary          = Color(0xFF2A1208),
    tertiaryContainer   = Color(0xFF7A3D26),
    onTertiaryContainer = Color(0xFFFBE2D5),
    background          = ClaudeNightBg,
    onBackground        = ClaudeNightText,
    surface             = ClaudeNightSurface,
    onSurface           = ClaudeNightText,
    surfaceVariant      = Color(0xFF36342F),
    onSurfaceVariant    = Color(0xFFD4CFC2),
    outline             = Color(0xFF8B8174),
    error               = Color(0xFFE39B95),
    onError             = Color(0xFF601410),
    errorContainer      = Color(0xFF8C1D18),
    onErrorContainer    = Color(0xFFF9DEDC),
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
