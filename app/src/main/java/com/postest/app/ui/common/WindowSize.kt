package com.postest.app.ui.common

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

val LocalWindowSize = compositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass not provided — wrap content in CompositionLocalProvider in MainActivity")
}

object Responsive {
    /** True for phones in portrait or very narrow windows. Cart/edit panels should stack. */
    val isCompact: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalWindowSize.current.widthSizeClass == WindowWidthSizeClass.Compact

    /** True for medium tablets / foldables / phones in landscape. */
    val isMedium: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalWindowSize.current.widthSizeClass == WindowWidthSizeClass.Medium

    /** True for the W1401 and other large/expanded screens. */
    val isExpanded: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalWindowSize.current.widthSizeClass == WindowWidthSizeClass.Expanded
}
