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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* ----------------------------------
   DARK COLOR SCHEME (PRIMARY)
----------------------------------- */

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6F66E4),
    onPrimary = Color.White,

    background = Color(0xFF000000),
    onBackground = Color.White,

    surface = Color(0xFF121212),
    onSurface = Color.White,

    outline = Color(0xFF2A2A2A)
)

/* ----------------------------------
   LIGHT COLOR SCHEME (OPTIONAL)
   (kept minimal & neutral)
----------------------------------- */

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6F66E4),
    onPrimary = Color.White,

    background = Color.White,
    onBackground = Color(0xFF2B2B2B),

    surface = Color.White,
    onSurface = Color(0xFF2B2B2B),

    outline = Color(0xFFD4D4D4)
)

/* ----------------------------------
   APP THEME
----------------------------------- */

@Composable
fun NextStopAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

