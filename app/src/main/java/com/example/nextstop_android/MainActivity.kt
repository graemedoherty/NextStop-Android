package com.example.nextstop_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.journey.JourneyScreen
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔑 Read extras coming from notification intent
        val openAlarm =
            intent?.getBooleanExtra(
                LocationTrackingService.EXTRA_OPEN_ALARM,
                false
            ) ?: false

        val stationName =
            intent?.getStringExtra(
                LocationTrackingService.EXTRA_DESTINATION_NAME
            )

        val latitude =
            intent?.getDoubleExtra(
                LocationTrackingService.EXTRA_DESTINATION_LAT,
                0.0
            ) ?: 0.0

        val longitude =
            intent?.getDoubleExtra(
                LocationTrackingService.EXTRA_DESTINATION_LNG,
                0.0
            ) ?: 0.0

        setContent {
            NextStopAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JourneyScreen()
                }
            }
        }
    }
}
