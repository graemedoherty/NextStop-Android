package com.example.nextstop_android.ui.stepper

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.journey.Step4Screen
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.stations.StationViewModel
import com.example.nextstop_android.viewmodel.StationViewModelFactory
import com.example.nextstop_android.viewmodel.StepperViewModel

@Composable
fun StepperScreen(
    onAlarmCreated: (Station) -> Unit,
    mapViewModel: MapViewModel,
    viewModel: StepperViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedTransport by viewModel.selectedTransport.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val mapUiState by mapViewModel.uiState.collectAsState()

    val stationViewModel: StationViewModel = viewModel(
        factory = StationViewModelFactory(context)
    )

    val themePurple = Color(0xFF6F66E3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        StepIndicatorsWithLabels(currentStep = currentStep, activeColor = themePurple)

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    val animationDuration = 700
                    val curve = EaseInOutQuart

                    (slideInHorizontally(
                        animationSpec = tween(
                            animationDuration,
                            easing = curve
                        )
                    ) { it * direction } +
                            fadeIn(animationSpec = tween(animationDuration)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(
                                    animationDuration,
                                    easing = curve
                                )
                            ) { -it * direction } +
                                    fadeOut(animationSpec = tween(animationDuration))
                        )
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1Screen(
                        selectedTransport = selectedTransport,
                        onTransportSelected = viewModel::selectTransport,
                        onNext = viewModel::nextStep
                    )

                    2 -> Step2Screen(
                        selectedTransport = selectedTransport ?: "",
                        savedStation = selectedStation,
                        onStationSelected = { name, lat, lng ->
                            viewModel.selectStation(name, lat, lng)
                            val stationObj = Station(name, selectedTransport ?: "", lat, lng, 0)
                            mapViewModel.setDestination(stationObj)
                        },
                        onClearStation = {
                            viewModel.clearStation()
                            mapViewModel.cancelAlarm(viewModel)
                        },
                        onNext = viewModel::nextStep,
                        onBack = viewModel::goBack,
                        mapViewModel = mapViewModel,
                        stationViewModel = stationViewModel
                    )

                    3 -> Step3Screen(
                        selectedTransport = selectedTransport ?: "",
                        selectedStation = selectedStation?.name ?: "",
                        onAlarmSet = {
                            selectedStation?.let { station ->
                                onAlarmCreated(station)
                                viewModel.nextStep()
                                val serviceIntent =
                                    Intent(context, LocationTrackingService::class.java).apply {
                                        action = LocationTrackingService.ACTION_SET_DESTINATION
                                        putExtra(
                                            LocationTrackingService.EXTRA_DESTINATION_LAT,
                                            station.latitude
                                        )
                                        putExtra(
                                            LocationTrackingService.EXTRA_DESTINATION_LNG,
                                            station.longitude
                                        )
                                        putExtra(
                                            LocationTrackingService.EXTRA_DESTINATION_NAME,
                                            station.name
                                        )
                                    }
                                context.startService(serviceIntent)
                            }
                        },
                        onBack = viewModel::goBack
                    )

                    4 -> Step4Screen(
                        destinationName = selectedStation?.name ?: "Unknown",
                        distanceMeters = mapUiState.distanceToDestination,
                        onCancel = {
                            mapViewModel.cancelAlarm(viewModel)
                            viewModel.resetToStep(1)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StepIndicatorsWithLabels(currentStep: Int, activeColor: Color) {
    val labels = listOf("Mode", "Destination", "Set", "Alarm")

    // 🔑 Theme-aware colors
    val inactiveLineColor = MaterialTheme.colorScheme.outlineVariant
    val inactiveIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeLabelColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val width = size.width
            val centerY = size.height / 2
            val circleRadiusPx = 14.dp.toPx()

            drawLine(
                color = inactiveLineColor,
                start = Offset(circleRadiusPx, centerY),
                end = Offset(width - circleRadiusPx, centerY),
                strokeWidth = 2.dp.toPx()
            )

            val progressFactor = (currentStep - 1) / (labels.size - 1).toFloat()
            val progressEnd = circleRadiusPx + (width - (circleRadiusPx * 2)) * progressFactor

            drawLine(
                color = activeColor,
                start = Offset(circleRadiusPx, centerY),
                end = Offset(progressEnd, centerY),
                strokeWidth = 2.dp.toPx()
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                val stepNumber = index + 1
                val isVisited = stepNumber <= currentStep
                val isActive = (index + 1) == currentStep

                Column(
                    horizontalAlignment = when (index) {
                        0 -> Alignment.Start
                        labels.size - 1 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isVisited) activeColor else inactiveIndicatorColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            color = if (isVisited) Color.White else labelTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label,
                        color = if (isActive) activeColor else labelTextColor,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textAlign = when (index) {
                            0 -> TextAlign.Start
                            labels.size - 1 -> TextAlign.End
                            else -> TextAlign.Center
                        }
                    )
                }
            }
        }
    }
}