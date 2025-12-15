//package com.example.nextstop_android.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.compose.*
//import com.example.nextstop_android.ui.maps.MapsScreen
//import com.example.nextstop_android.ui.stepper.StepperScreen
//
//@Composable
//fun NextStopNavGraph() {
//    val navController = rememberNavController()
//
//    NavHost(
//        navController = navController,
//        startDestination = "stepper"
//    ) {
//        composable("stepper") {
//            StepperScreen(
//                onAlarmCreated = {
//                    navController.navigate("map")
//                }
//            )
//        }
//
//        composable("map") {
//            MapsScreen(
//                onBack = {
//                    navController.popBackStack()
//                }
//            )
//        }
//    }
//}
