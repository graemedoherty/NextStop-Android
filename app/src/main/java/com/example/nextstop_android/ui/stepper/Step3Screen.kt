package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Step3Screen(
    selectedTransport: String,
    selectedStation: String,
    onAlarmSet: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Step 3: Set alarm")

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAlarmSet,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Alarm")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
