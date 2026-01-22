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
import androidx.lifecycle.lifecycleScope
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.journey.JourneyScreen
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        // Install Android 12+ splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Force window background to black immediately
        window.setBackgroundDrawableResource(android.R.color.black)

        // Don't keep system splash screen
        splashScreen.setKeepOnScreenCondition { false }

        // Initialize AdMob on background thread
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

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
                // Show main app - JourneyScreen handles drawer & navigation internally
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
            stepperViewModel.resetToStep(4)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Safe catch in case already unregistered
        }
    }
}