package com.example.nextstop_android.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.ads.AdBanner
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel

@Composable
fun JourneyScreen() {
    val mapViewModel: MapViewModel = viewModel()
    val stepperViewModel: StepperViewModel = viewModel()

    // 🔑 ROOT BOX
    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ───────────────── STEPPER SECTION ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.32f)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                StepperScreen(
                    mapViewModel = mapViewModel,
                    viewModel = stepperViewModel,
                    onAlarmCreated = { station ->
                        mapViewModel.startAlarm(station)
                    }
                )
            }

            // ───────────────── GAP DIVIDER ─────────────────
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.background)
            )

            // ───────────────── MAP SECTION ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                MapsScreen(
                    modifier = Modifier.fillMaxSize(),
                    mapViewModel = mapViewModel,
                    stepperViewModel = stepperViewModel
                )
            }
        }

        // 🎯 FLOATING AD BANNER
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
            ) {
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}