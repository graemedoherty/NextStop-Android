package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.ui.components.PrimaryButton
import com.example.nextstop_android.ui.components.SecondaryButton

@Composable
fun Step1Screen(
    onTransportSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Step 1: Select mode of transport",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        listOf("Train", "Luas", "Bus").forEach { mode ->
            OutlinedButton(
                onClick = { selected = mode },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (selected == mode)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (selected == mode) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Next",
            enabled = selected != null,
            onClick = { selected?.let(onTransportSelected) }
        )
    }
}
