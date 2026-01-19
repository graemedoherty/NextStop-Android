package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
            // Tightened padding to match the compact look of Steps 1 and 2
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        // 🔑 Distributes title, info, and buttons evenly
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ───────────────── DATA FIELDS IN A ROW ─────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transport Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transport",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = selectedTransport,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Destination Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Destination",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = selectedStation,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1 // Keeps it from expanding vertically
                )
            }
        }

        // ───────────────── HORIZONTAL BUTTONS ─────────────────
        // Changed to Row to match the "Back/Next" pattern from Step 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )

            PrimaryButton(
                text = "Create Alarm",
                onClick = onAlarmSet,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )
        }
    }
}