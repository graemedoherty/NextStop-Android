package com.example.nextstop_android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.maps.Station
import com.example.nextstop_android.ui.stepper.StepperScreen

sealed class Route(val route: String) {
    object Stepper : Route("stepper")

    object Map : Route("map/{stationName}/{latitude}/{longitude}/{distance}") {
        fun createRoute(
            stationName: String,
            latitude: Double,
            longitude: Double,
            distance: Int
        ) = "map/$stationName/$latitude/$longitude/$distance"
    }
}

@Composable
fun NextStopNavGraph(
    openAlarm: Boolean = false,
    stationName: String? = null,
    latitude: Double = 0.0,
    longitude: Double = 0.0
) {
    val navController = rememberNavController()

    /**
     * 🔑 Resume active alarm from notification
     */
    LaunchedEffect(openAlarm) {
        if (openAlarm && stationName != null) {
            navController.navigate(
                Route.Map.createRoute(
                    stationName = stationName,
                    latitude = latitude,
                    longitude = longitude,
                    distance = 0
                )
            ) {
                popUpTo(Route.Stepper.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Stepper.route
    ) {

        /**
         * STEP 1 — Create Alarm
         */
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

        /**
         * STEP 2 — Live Map / Active Alarm
         */
        composable(
            route = Route.Map.route,
            arguments = listOf(
                navArgument("stationName") { type = NavType.StringType },
                navArgument("latitude") { type = NavType.FloatType },
                navArgument("longitude") { type = NavType.FloatType },
                navArgument("distance") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val stationNameArg =
                backStackEntry.arguments?.getString("stationName") ?: ""

            val latitudeArg =
                backStackEntry.arguments?.getFloat("latitude")?.toDouble() ?: 0.0

            val longitudeArg =
                backStackEntry.arguments?.getFloat("longitude")?.toDouble() ?: 0.0

            val distanceArg =
                backStackEntry.arguments?.getInt("distance") ?: 0

            val destinationStation = Station(
                name = stationNameArg,
                type = "Destination",
                latitude = latitudeArg,
                longitude = longitudeArg,
                distance = distanceArg
            )

            MapsScreen(
                destinationStation = destinationStation,
                onBack = {
                    // 🔁 Reset app to Step 1 cleanly
                    navController.navigate(Route.Stepper.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
