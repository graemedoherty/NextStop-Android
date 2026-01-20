package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onNext: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // 🎨 Updated Colors
    // Light Mode: Off-white (#F5F5F5) | Dark Mode: Onyx (#080808)
    val unselectedContainer = if (isDark) Color(0xFF080808) else Color(0xFFF5F5F5)

    // Borders: Subtle contrast for both modes
    val unselectedBorder = if (isDark) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)

    // Text/Icon colors
    val contentColor = if (isDark) Color.White else Color(0xFF2E2E2E)
    val selectedPurple = Color(0xFF6F66E3)

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
                        containerColor = if (isSelected) selectedPurple else unselectedContainer
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) selectedPurple else unselectedBorder
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Checkmark indicator to confirm selection
                        Box(modifier = Modifier.height(16.dp)) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(id = iconResource),
                            contentDescription = mode,
                            modifier = Modifier.size(32.dp),
                            tint = if (isSelected) Color.White else contentColor
                        )

                        Text(
                            text = mode,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else contentColor
                        )
                    }
                }
            }
        }

        // --- THE NEXT BUTTON ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = if (selectedTransport != null) selectedPurple else unselectedContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (selectedTransport != null) Color.Transparent else unselectedBorder,
                    shape = RoundedCornerShape(12.dp)
                ),
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