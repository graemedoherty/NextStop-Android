package com.example.nextstop_android.ui.burger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun BurgerMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🔑 Pull colors from the active theme
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val contentColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            // Background adapts: Dark in Dark Mode, Light in Light Mode
            .background(backgroundColor)
            // Border adapts to the theme's outline color
            .border(0.5.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Open Menu",
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}