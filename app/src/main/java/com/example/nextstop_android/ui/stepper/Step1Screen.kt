package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.R
import com.example.nextstop_android.ui.components.PrimaryButton

@Composable
fun Step1Screen(
    selectedTransport: String?,
    onTransportSelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit = {} // Keep as optional to prevent StepperScreen errors
) {
    val isDark = isSystemInDarkTheme()

    // 🎨 UI Palette
    val unselectedContainer = if (isDark) Color(0xFF080808) else Color(0xFFF5F5F5)
    val unselectedBorder = if (isDark) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)
    val contentColor = if (isDark) Color.White else Color(0xFF2E2E2E)
    val themePurple = Color(0xFF6F66E3)

    // Bunq-style diluted background (12% opacity)
    val softPurpleBg = themePurple.copy(alpha = 0.12f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- TRANSPORT SELECTION ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Train", "Luas", "Bus").forEach { mode ->
                val isSelected = selectedTransport == mode
                val iconResource = when (mode) {
                    "Train" -> R.drawable.train
                    "Luas" -> R.drawable.luas
                    else -> R.drawable.bus
                }

                OutlinedButton(
                    onClick = { onTransportSelected(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        // 🔑 Styling Tweak: Use soft purple when selected
                        containerColor = if (isSelected) softPurpleBg else unselectedContainer
                    ),
                    border = BorderStroke(
                        // 🔑 Styling Tweak: Thicker 3.dp border
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) themePurple else unselectedBorder
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.height(16.dp)) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = themePurple, // 🔑 Icon matches theme
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(id = iconResource),
                            contentDescription = mode,
                            modifier = Modifier.size(32.dp),
                            tint = if (isSelected) themePurple else contentColor
                        )

                        Text(
                            text = mode,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) themePurple else contentColor
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            PrimaryButton(
                text = "Next",
                enabled = selectedTransport != null,
                onClick = onNext,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}