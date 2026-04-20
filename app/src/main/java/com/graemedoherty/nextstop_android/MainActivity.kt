package com.graemedoherty.nextstop_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.graemedoherty.nextstop_android.service.LocationTrackingService
import com.graemedoherty.nextstop_android.ui.journey.JourneyScreen
import com.graemedoherty.nextstop_android.ui.maps.MapViewModel
import com.graemedoherty.nextstop_android.ui.splash.LEDMatrixSplashScreen
import com.graemedoherty.nextstop_android.ui.theme.NextStopAndroidTheme
import com.graemedoherty.nextstop_android.viewmodel.StepperViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val stepperViewModel: StepperViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    private var showCustomSplash by mutableStateOf(true)

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationTrackingService.ACTION_ALARM_STOPPED) {
                stepperViewModel.reset()
                mapViewModel.resetAllState()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Setup the system splash screen immediately
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        splashScreen.setKeepOnScreenCondition { false }
        window.setBackgroundDrawableResource(android.R.color.black)

        // 2. Safe Initialization Block
        // We delay heavy system calls (Ads/Receivers) to let the Emulator's
        // Keystore/Keymint services finish booting (fixes HARDWARE_TYPE_UNAVAILABLE)
        lifecycleScope.launch {
            delay(1500) // Give the emulator 1.5s to stabilize

            // Initialize Mobile Ads on IO thread
            launch(Dispatchers.IO) {
                try {
                    MobileAds.initialize(this@MainActivity) {}
                } catch (e: Exception) {
                    // Fail silently
                }
            }

            // Register Receiver after the hardware is ready
            try {
                val filter = IntentFilter(LocationTrackingService.ACTION_ALARM_STOPPED)
                ContextCompat.registerReceiver(
                    this@MainActivity,
                    stopReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (e: Exception) {
                // Handle edge cases where receiver registration might fail
            }
        }

        // 3. Process the starting intent
        handleIntent(intent)

        // 4. UI Setup
        setContent {
            NextStopAndroidTheme {
                var isMounted by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isMounted = true }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.graphicsLayer {
                            alpha = if (isMounted) 1f else 0f
                        }) {
                            JourneyScreen(
                                mapViewModel = mapViewModel,
                                stepperViewModel = stepperViewModel
                            )
                        }

                        AnimatedVisibility(
                            visible = showCustomSplash,
                            enter = fadeIn(animationSpec = tween(0)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(600, easing = EaseInOutQuart)
                            ) + fadeOut(animationSpec = tween(400))
                        ) {
                            if (isMounted) {
                                LEDMatrixSplashScreen(onTimeout = { showCustomSplash = false })
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
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
            intent.removeExtra("SCREEN_TO_LOAD")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
    }
}