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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        // 🔑 1. Initialize the official System Splash Screen
        // This must be called BEFORE super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize AdMob
        MobileAds.initialize(this) {}

        // Register broadcast receiver for alarm stopped events
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
                // 🔑 2. Go straight to your content.
                // The system handles the transition from your logo to this screen.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
            // 🔑 Change this to 4 to show the Live Alarm view when coming from a notification
            stepperViewModel.resetToStep(4)
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