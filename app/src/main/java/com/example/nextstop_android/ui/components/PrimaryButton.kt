package com.example.nextstop_android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    disabledContentColor: Color = Color.Unspecified
) {
    val isDark = isSystemInDarkTheme()
    val brandPurple = Color(0xFF6F66E3)
    val softPurpleBg = brandPurple.copy(alpha = 0.40f) // Diluted background

    val finalDisabledTextColor = if (disabledContentColor != Color.Unspecified) {
        disabledContentColor
    } else {
        if (isDark) Color.Gray else brandPurple.copy(alpha = 0.4f)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp), // 📏 Unified Height
        shape = RoundedCornerShape(12.dp), // 📏 Unified Radius
        border = if (enabled) BorderStroke(2.dp, brandPurple) else null, // 📏 Unified Border
        colors = ButtonDefaults.buttonColors(
            containerColor = softPurpleBg,
            contentColor = brandPurple,
            disabledContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(
                alpha = 0.05f
            ),
            disabledContentColor = finalDisabledTextColor
        )
    ) {
        Text(
            text = text,
            fontSize = 15.sp, // Unified font size
            fontWeight = FontWeight.Bold
        )
    }
}