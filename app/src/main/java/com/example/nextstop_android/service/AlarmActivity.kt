package com.example.nextstop_android.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.nextstop_android.MainActivity
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔑 Ensure this happens before super.onCreate for some older OS versions
        setupLockscreenFlags()

        super.onCreate(savedInstanceState)

        Log.d("AlarmActivity", "🚨 AlarmActivity created")

        val destinationName =
            intent.getStringExtra(LocationTrackingService.EXTRA_DESTINATION_NAME)
                ?: "Destination"

        setContent {
            NextStopAndroidTheme {
                // Using Black background to match your AlarmScreen design
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black
                ) {
                    AlarmScreen(
                        destinationName = destinationName,
                        onStopAlarm = {
                            stopAlarmAndResetApp()
                        }
                    )
                }
            }
        }
    }

    private fun stopAlarmAndResetApp() {
        Log.d("AlarmActivity", "🛑 Alarm stopped — resetting app")

        // 1. Stop the tracking service
        startService(
            Intent(this, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            }
        )
        finishAndRemoveTask()

        // 2. Clear current task and return to a fresh MainActivity (Step 1)
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

            // startActivity(restartIntent)

        // 3. Dismiss the activity
       // finish()
    }

    private fun setupLockscreenFlags() {
        // 🔑 Tells the OS to light up the screen and show above the lock guard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        // 🔑 Legacy flags + Window management to keep screen from dimming immediately
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    // 🔒 Optional: Prevent accidental back-button dismissals
    // @Deprecated in API 33, but effective for blocking the hardware back button
//    override fun onBackPressed() {
//        // User must swipe the UI to stop the alarm
//    }
}