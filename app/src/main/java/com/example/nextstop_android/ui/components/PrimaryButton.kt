package com.example.nextstop_android.ui.components

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
    // 🎨 New parameter to control text color when disabled
    disabledContentColor: Color = Color.Unspecified
) {
    val isDark = isSystemInDarkTheme()

    // Fallback logic if no color is provided:
    // Light mode defaults to Purple, Dark mode defaults to Gray
    val finalDisabledTextColor = if (disabledContentColor != Color.Unspecified) {
        disabledContentColor
    } else {
        if (isDark) Color.Gray else Color(0xFF6F66E3)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6F66E3),
            contentColor = Color.White,

            // Still transparent so the parent Box's background shows through
            disabledContainerColor = Color.Transparent,
            // ⚡ Use our dynamic color here
            disabledContentColor = finalDisabledTextColor
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}