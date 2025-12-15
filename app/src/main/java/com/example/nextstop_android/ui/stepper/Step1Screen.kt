package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Step1Screen(
    onTransportSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Step 1: Select mode of transport")

        Spacer(Modifier.height(24.dp))

        listOf("Train", "Luas", "Bus").forEach { mode ->
            OutlinedButton(
                onClick = { selected = mode },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                border = BorderStroke(
                    2.dp,
                    if (selected == mode)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mode)
                    if (selected == mode) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { selected?.let(onTransportSelected) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}
