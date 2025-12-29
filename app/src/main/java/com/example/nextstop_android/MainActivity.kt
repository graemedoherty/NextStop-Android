package com.example.nextstop_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat // 🔑 Required for API compatibility
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.journey.JourneyScreen
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private val stepperViewModel: StepperViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationTrackingService.ACTION_ALARM_STOPPED) {
                stepperViewModel.reset()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AdMob
        MobileAds.initialize(this) {}

        // 🔑 FIX: Use ContextCompat to support API 24+
        val filter = IntentFilter(LocationTrackingService.ACTION_ALARM_STOPPED)
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        handleIntent(intent)

        setContent {
            NextStopAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 🔑 FIX: Call without parameters if JourneyScreen handles its own ViewModels
                    JourneyScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val screenToLoad = intent?.getStringExtra("SCREEN_TO_LOAD")
        if (screenToLoad == "JOURNEY_SCREEN") {
            stepperViewModel.moveToJourney()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Safe catch in case it was already unregistered
        }
    }
}