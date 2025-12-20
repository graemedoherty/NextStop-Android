package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun StepIndicators(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Reduced vertical padding from 16dp to 8dp top and 0dp bottom
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val step = index + 1
            val active = step <= currentStep

            Box(
                modifier = Modifier
                    .size(36.dp) // Slightly smaller circles (40 -> 36) saves vertical space
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (step < 3) {
                HorizontalDivider( // Updated from Divider to HorizontalDivider (M3)
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp), // Reduced from 8dp
                    color = if (step < currentStep) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}