package com.example.nextstop_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextStopAndroidTheme {
                StepperScreen()
            }
        }
    }
}
