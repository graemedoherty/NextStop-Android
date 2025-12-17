package com.example.nextstop_android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.maps.Station
import com.example.nextstop_android.ui.stepper.StepperScreen

@Composable
fun NextStopNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Stepper.route
    ) {
        composable(Route.Stepper.route) {
            StepperScreen(
                onAlarmCreated = { station ->
                    navController.navigate(
                        Route.Map.createRoute(
                            stationName = station.name,
                            latitude = station.latitude,
                            longitude = station.longitude,
                            distance = station.distance
                        )
                    )
                }
            )
        }

        composable(
            route = Route.Map.route,
            arguments = listOf(
                navArgument("stationName") { type = NavType.StringType },
                navArgument("latitude") { type = NavType.FloatType },
                navArgument("longitude") { type = NavType.FloatType },
                navArgument("distance") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val stationName = backStackEntry.arguments?.getString("stationName") ?: ""
            val latitude = backStackEntry.arguments?.getFloat("latitude")?.toDouble() ?: 0.0
            val longitude = backStackEntry.arguments?.getFloat("longitude")?.toDouble() ?: 0.0
            val distance = backStackEntry.arguments?.getInt("distance") ?: 0

            val destinationStation = Station(
                name = stationName,
                type = "Destination",
                latitude = latitude,
                longitude = longitude,
                distance = distance
            )

            MapsScreen(
                onBack = { navController.popBackStack() },
                destinationStation = destinationStation
            )
        }
    }
}