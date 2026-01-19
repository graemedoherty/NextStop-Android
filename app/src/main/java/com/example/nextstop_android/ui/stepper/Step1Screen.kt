package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
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

    // 🎨 Dynamic Colors
    val unselectedGrey = if (isDark) Color(0xFF2E2E2E) else Color(0xFFD4D4D4)
    val contentColor = if (isDark) Color.White else Color(0xFF2E2E2E)
    val selectedGrey = Color(0xFF6F66E3)

    Column(
        modifier = Modifier
            .fillMaxSize() // Fills the 32% area allocated in JourneyScreen
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        // 🔑 SpaceBetween ensures 'Next' stays at the bottom of the allocated area
        verticalArrangement = Arrangement.SpaceBetween
    ) {


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
                        // 🔑 Fixed height to ensure it fits in the header
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) selectedGrey else unselectedGrey
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(16.dp))
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

        // 🎯 THE NEXT BUTTON
        PrimaryButton(
            text = "Next",
            enabled = selectedTransport != null,
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp) // Compact height to fit the screen
        )
    }
}