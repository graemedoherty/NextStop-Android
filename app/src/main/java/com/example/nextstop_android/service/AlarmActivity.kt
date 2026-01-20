package com.example.nextstop_android.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme

class AlarmActivity : ComponentActivity() {

    // 🔔 Receiver to close screen when alarm stopped from notification
    private val alarmDismissedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmService.ACTION_ALARM_DISMISSED) {
                Log.d("AlarmActivity", "📴 Alarm dismissed via notification")
                stopEverythingAndExit()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupLockscreenFlags()
        super.onCreate(savedInstanceState)

        Log.d("AlarmActivity", "🚨 AlarmActivity created")

        val destinationName =
            intent.getStringExtra(LocationTrackingService.EXTRA_DESTINATION_NAME)
                ?: "Destination"

        setContent {
            NextStopAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AlarmScreen(
                        destinationName = destinationName,
                        onStopAlarm = {
                            stopEverythingAndExit()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        ContextCompat.registerReceiver(
            this,
            alarmDismissedReceiver,
            IntentFilter(AlarmService.ACTION_ALARM_DISMISSED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(alarmDismissedReceiver)
        super.onStop()
    }
    
    private fun stopEverythingAndExit() {
        Log.d("AlarmActivity", "🛑 Alarm dismissed — exiting app")

        // 1. Stop AlarmService
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        })

        // 2. Stop location tracking
        startService(Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        })

        // 3. Close this task completely
        finishAndRemoveTask()

        // 4. Explicitly return to HOME
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager =
                getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}
