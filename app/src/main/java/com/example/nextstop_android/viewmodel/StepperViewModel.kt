package com.example.nextstop_android.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.nextstop_android.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepperViewModel : ViewModel() {

    /* ---------------- Navigation State ---------------- */
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _selectedTransport = MutableStateFlow<String?>(null)
    val selectedTransport: StateFlow<String?> = _selectedTransport.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _transportConfirmed = MutableStateFlow(false)
    val transportConfirmed: StateFlow<Boolean> = _transportConfirmed.asStateFlow()

    /* ---------------- Onboarding State ---------------- */
    var showPermissionOverlay by mutableStateOf(false)
        private set

    var onboardingPage by mutableIntStateOf(0)
        private set

    var permissionTitle by mutableStateOf("")
        private set
    var permissionDescription by mutableStateOf("")
        private set
    var permissionButtonText by mutableStateOf("")
        private set

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOnboardingComplete(context: Context): Boolean {
        return context.getSharedPreferences("nextstop_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_finished", false)
    }

    /**
     * 🔑 THE FIX: Explicitly named parameters to allow 'title = ...' style calls
     */
    private fun updateUI(title: String, description: String, buttonText: String) {
        this.permissionTitle = title
        this.permissionDescription = description
        this.permissionButtonText = buttonText
    }

    fun checkPermissions(context: Context) {
        if (getOnboardingComplete(context)) {
            showPermissionOverlay = false
            return
        }

        val hasLocation = isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasNotifications = if (Build.VERSION.SDK_INT >= 33) {
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else true
        val hasOverlay = Settings.canDrawOverlays(context)

        when {
            onboardingPage == 0 -> updateUI(
                title = "Welcome to Next Stop",
                description = """
        Quick Setup Guide.
        
        To alert you at just the right moment, Next Stop needs a few quick permissions. 
        Setup only takes a minute — and then you're good to go.
        
        How it works:
        1. Choose your mode of travel.
        2. Pick your destination from the dropdown or the interactive map.
        3. Set your alarm.
        4. Relax — we'll alert you when you are near your destination station.
    """.trimIndent(),
                buttonText = "Get Started"
            )


            onboardingPage == 1 && !hasLocation -> updateUI(
                title = "Location Access",
                description = """
        We use your location to track your journey and calculate how close you are to your stop.
        
        Your location is only used while a trip is active.
    """.trimIndent(),
                buttonText = "Allow Location"
            )


            onboardingPage == 2 && !hasNotifications -> updateUI(
                title = "Notifications",
                description = "This allows us to keep the tracking service active in the background while you relax or use other apps.",
                buttonText = "Enable Notifications"
            )

            onboardingPage == 3 && !hasOverlay -> updateUI(
                title = "Display Over Other Apps",
                description = """
        This allows your arrival alert to appear over apps like Google Maps or on your lock screen,
        so you never miss it.
    """.trimIndent(),
                buttonText = "Enable Overlay"
            )


            onboardingPage == 4 -> updateUI(
                title = "You're All Set",
                description = """
        Everything is ready to go.
        
        Choose your mode of transport, pick your destination station, and have a safe journey.
    """.trimIndent(),
                buttonText = "Start Your Journey"
            )


            else -> {
                if (onboardingPage < 4) {
                    onboardingPage++
                    checkPermissions(context)
                } else {
                    finishOnboarding(context)
                }
            }
        }
        showPermissionOverlay = true
    }

    fun handlePermissionRequest(
        context: Context,
        onLaunchLocation: () -> Unit,
        onLaunchNotifications: () -> Unit
    ) {
        when (onboardingPage) {
            0 -> {
                onboardingPage = 1; checkPermissions(context)
            }

            1 -> onLaunchLocation()
            2 -> onLaunchNotifications()
            3 -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            4 -> finishOnboarding(context)
        }
    }

    private fun finishOnboarding(context: Context) {
        context.getSharedPreferences("nextstop_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_finished", true).apply()
        showPermissionOverlay = false
    }

    /* ---------------- Standard Navigation ---------------- */
    fun nextStep() {
        if (_currentStep.value == 1 && _selectedTransport.value != null) {
            _transportConfirmed.value = true
        }
        if (_currentStep.value < 4) _currentStep.value++
    }

    fun resetToStep(s: Int) {
        _currentStep.value = s
        if (s <= 2) clearStation()
        if (s == 1) _transportConfirmed.value = false
    }

    fun goBack() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(1)
        if (_currentStep.value <= 2) clearStation()
    }

    fun selectTransport(t: String) {
        _selectedTransport.value = t
        _transportConfirmed.value = false
        clearStation()
    }

    fun selectStation(n: String, lat: Double, lon: Double) {
        _selectedStation.value = Station(n, _selectedTransport.value.orEmpty(), lat, lon, 0)
    }

    fun clearStation() {
        _selectedStation.value = null
    }

    fun reset() {
        _currentStep.value = 1
        _selectedTransport.value = null
        _selectedStation.value = null
        _transportConfirmed.value = false
    }
}