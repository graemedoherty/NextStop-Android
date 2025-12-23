package com.example.nextstop_android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* ----------------------------------
   DARK COLOR SCHEME (OLED BLACK)
----------------------------------- */

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6F66E4),
    onPrimary = Color.White,

    // 🔑 Set both to Pure Black to match map landscape
    background = Color(0xFF000000),
    surface = Color(0xFF000000),

    onBackground = Color.White,
    onSurface = Color.White,

    // 🔑 Use the Map's "Water" or "POI" colors for outlines/dividers
    outline = Color(0xFF2B2B2B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6F66E4),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF2B2B2B),
    surface = Color.White,
    onSurface = Color(0xFF2B2B2B),
    outline = Color(0xFFD4D4D4)
)

@Composable
fun NextStopAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    /* ----------------------------------
       SYSTEM BAR POLISH
    ----------------------------------- */
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // 🔑 Set status bar to pure black
            window.statusBarColor = Color.Black.toArgb()

            // 🔑 Set navigation bar (bottom pill area) to pure black
            window.navigationBarColor = Color.Black.toArgb()

            val controller = WindowCompat.getInsetsController(window, view)
            // Ensure status bar icons (clock, battery) stay white on the black background
            controller.isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}