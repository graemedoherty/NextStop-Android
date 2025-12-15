package com.example.nextstop_android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.stepper.StepperScreen

sealed class Route(val route: String) {
    object Stepper : Route("stepper")
    object Map : Route("map")
}

@Composable
fun NextStopNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Stepper.route
    ) {
        composable(Route.Stepper.route) {
            StepperScreen(
                onAlarmCreated = {
                    navController.navigate(Route.Map.route)
                }
            )
        }

        composable(Route.Map.route) {
            MapsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}