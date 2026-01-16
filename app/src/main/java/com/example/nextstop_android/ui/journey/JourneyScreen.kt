package com.example.nextstop_android.ui.journey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.ads.AdBanner
import com.example.nextstop_android.ui.maps.AlarmCard
import com.example.nextstop_android.ui.maps.AlarmStatus
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel

@Composable
fun JourneyScreen() {
    // 🔑 SINGLE source of truth for ViewModels
    val mapViewModel: MapViewModel = viewModel()
    val stepperViewModel: StepperViewModel = viewModel()

    val mapUiState by mapViewModel.uiState.collectAsState()

    // 🔑 ROOT BOX: Essential for layering the Ad Banner on top
    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ───────────────── STEPPER ─────────────────
            // This is inside a Column, so expandVertically/shrinkVertically works perfectly.
            AnimatedVisibility(
                visible = !mapUiState.alarmArmed,
                enter = expandVertically(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(1000)
                ) + shrinkVertically(animationSpec = tween(1000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.32f)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
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
            }

            // ───────────────── MAP AREA ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                // 🗺 MAP (Base Layer)
                MapsScreen(
                    modifier = Modifier.fillMaxSize(),
                    mapViewModel = mapViewModel,
                    stepperViewModel = stepperViewModel
                )

                // 🚨 ALARM CARD (Floating layer over map)
                // 🔑 FIX: Using explicit package call to avoid ColumnScope conflict
                androidx.compose.animation.AnimatedVisibility(
                    visible = mapUiState.alarmArmed,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(1000)
                    ) + fadeIn(animationSpec = tween(1000)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(1000)
                    ) + fadeOut(animationSpec = tween(1000)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp) // Clearance for the Ad Banner
                ) {
                    val status =
                        if (mapUiState.distanceToDestination in 0..100)
                            AlarmStatus.ARRIVED
                        else
                            AlarmStatus.ACTIVE

                    AlarmCard(
                        destination = mapUiState.selectedStation?.name ?: "Destination",
                        distanceMeters = mapUiState.distanceToDestination,
                        status = status,
                        onCancel = {
                            mapViewModel.cancelAlarm(stepperViewModel)
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 🎯 FLOATING AD BANNER (70% Width)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Keeps map visible on sides
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
            ) {
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}