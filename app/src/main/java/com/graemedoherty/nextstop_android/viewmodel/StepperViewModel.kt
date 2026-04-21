package com.graemedoherty.nextstop_android.viewmodel

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
import com.graemedoherty.nextstop_android.model.Station
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
                "Welcome",
                "Next Stop needs a few quick permissions to alert you at the right moment.",
                "Get Started"
            )

            onboardingPage == 1 && !hasLocation -> updateUI(
                "Location Access",
                "We use your location to calculate how close you are to your stop.",
                "Allow Location"
            )

            onboardingPage == 2 && !hasNotifications -> updateUI(
                "Notifications",
                "This allows us to send the alert while you use other apps.",
                "Enable Notifications"
            )

            // 🔑 UPDATED: Clear instructions for the technical step
            onboardingPage == 3 && !hasOverlay -> updateUI(
                "Display Over Other Apps",
                "1. Tap Enable below.\n2. Find 'Next Stop' in the list.\n3. Switch the toggle to ON.\n\nThis lets the alarm pop up even if your phone is locked.",
                "Enable"
            )

            onboardingPage == 4 -> updateUI(
                "You're Ready!",
                "Everything is setup correctly. Have a safe journey!",
                "Start"
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

    fun nextStep() {
        if (_currentStep.value == 1 && _selectedTransport.value != null) {
            _transportConfirmed.value = true
        }
        if (_currentStep.value < 4) _currentStep.value++
    }

    fun resetToStep(s: Int) {
        _currentStep.value = s
    }

    fun goBack() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(1)
    }

    fun selectTransport(t: String) {
        if (_selectedTransport.value != t) {
            _selectedTransport.value = t
            _transportConfirmed.value = false
            _selectedStation.value = null
        }
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