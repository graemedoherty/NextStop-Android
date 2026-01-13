package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
            text = "Step 3: Confirm & set alarm",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Details
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

        Spacer(modifier = Modifier.height(12.dp))

        // Destination Details
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

        Spacer(modifier = Modifier.weight(1f))

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