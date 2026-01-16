package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
    // 🔑 These dynamic colors will adapt to Light/Dark mode automatically
    val unselectedGrey = MaterialTheme.colorScheme.surfaceVariant
    val selectedGrey = MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Step 1: Select mode of transport",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Train", "Luas", "Bus").forEach { mode ->
                val isSelected = selectedTransport == mode

                val iconResource = when (mode) {
                    "Train" -> R.drawable.train
                    "Luas" -> R.drawable.luas
                    "Bus" -> R.drawable.bus
                    else -> R.drawable.bus
                }

                OutlinedButton(
                    onClick = { onTransportSelected(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(8.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        // 🔑 Using the grey palette
                        containerColor = if (isSelected) selectedGrey else unselectedGrey
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Checkmark indicator
                        Box(modifier = Modifier.height(24.dp)) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Icon(
                            painter = painterResource(id = iconResource),
                            contentDescription = mode,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = mode,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            // 🔑 Text color follows the theme (Black in light mode, White in dark)
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Next",
            enabled = selectedTransport != null,
            onClick = onNext
        )
    }
}