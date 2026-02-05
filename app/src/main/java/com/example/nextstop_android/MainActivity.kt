package com.example.nextstop_android

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
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.journey.JourneyScreen
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.splash.LEDMatrixSplashScreen
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val stepperViewModel: StepperViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    private var showCustomSplash by mutableStateOf(true)

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationTrackingService.ACTION_ALARM_STOPPED) {
                stepperViewModel.reset() // Clears Stepper's station
                mapViewModel.resetAllState() // Clears Map's station and camera lock
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        splashScreen.setKeepOnScreenCondition { false }
        window.setBackgroundDrawableResource(android.R.color.black)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(this@MainActivity) {}
            } catch (e: Exception) {
                // Silently fail
            }
        }

        val filter = IntentFilter(LocationTrackingService.ACTION_ALARM_STOPPED)
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Process the starting intent
        handleIntent(intent)

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
            // 🔥 FIX: Consume the extra immediately so it doesn't re-fire on re-creation
            intent.removeExtra("SCREEN_TO_LOAD")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}