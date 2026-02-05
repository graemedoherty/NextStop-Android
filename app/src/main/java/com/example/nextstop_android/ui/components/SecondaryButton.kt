package com.example.nextstop_android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandPurple = Color(0xFF6F66E3)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp), // 📏 Matches Primary
        shape = RoundedCornerShape(12.dp), // 📏 Matches Primary
        border = BorderStroke(2.dp, brandPurple), // 📏 Matches Primary thickness
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = brandPurple,
            containerColor = Color.Transparent // Clean outline look
        )
    ) {
        Text(
            text = text,
            fontSize = 15.sp, // Matches Primary
            fontWeight = FontWeight.Bold // Matches Primary weight
        )
    }
}