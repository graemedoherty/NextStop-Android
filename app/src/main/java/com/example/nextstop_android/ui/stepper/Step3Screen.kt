package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.ui.components.PrimaryButton
import com.example.nextstop_android.ui.components.SecondaryButton

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
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
    ) {
        Text(
            text = "Step 3: Confirm alarm",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Transport",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = selectedTransport,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Destination",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = selectedStation,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f)) // Added weight to push buttons down consistently

        PrimaryButton(
            text = "Create Alarm",
            onClick = onAlarmSet
        )

        Spacer(modifier = Modifier.height(8.dp))

        SecondaryButton(
            text = "Back",
            onClick = onBack
        )
    }
}