package com.example.nextstop_android.ui.stepper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.components.AdSection
import com.example.nextstop_android.viewmodel.StepperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepperScreen(
    onAlarmCreated: () -> Unit,
    viewModel: StepperViewModel = viewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedTransport by viewModel.selectedTransport.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        StepIndicators(currentStep)

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith
                            slideOutHorizontally { -it }
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1Screen(
                        onTransportSelected = viewModel::selectTransport
                    )

                    2 -> Step2Screen(
                        selectedTransport = selectedTransport ?: "",
                        onStationSelected = viewModel::selectStation,
                        onBack = viewModel::goBack
                    )

                    3 -> Step3Screen(
                        selectedTransport = selectedTransport ?: "",
                        selectedStation = selectedStation ?: "",
                        onAlarmSet = onAlarmCreated,
                        onBack = viewModel::goBack
                    )
                }
            }
        }

        AdSection()
    }
}
