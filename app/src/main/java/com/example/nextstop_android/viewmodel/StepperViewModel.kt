package com.example.nextstop_android.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
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

    /* ---------------- Permission State (For Overlay) ---------------- */

    // 🔑 Controls if the Glassmorphism Overlay is visible
    var showPermissionOverlay by mutableStateOf(false)
        private set

    // 🔑 Controls the text inside the Permission Card
    var permissionTitle by mutableStateOf("")
        private set
    var permissionDescription by mutableStateOf("")
        private set
    var permissionButtonText by mutableStateOf("Grant Permission")
        private set

    /* ---------------- Permission Logic ---------------- */

    /**
     * Checks all required permissions and updates the overlay state accordingly.
     * Call this in JourneyScreen's LaunchedEffect or on app start.
     */
    fun checkPermissions(context: Context) {
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotifications = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val hasOverlay = Settings.canDrawOverlays(context)

        when {
            !hasLocation -> {
                updatePermissionOverlay(
                    title = "Location Access",
                    description = "Next Stop needs your location to track your journey and wake you up before your station.",
                    buttonText = "Allow Location"
                )
            }

            !hasNotifications && Build.VERSION.SDK_INT >= 33 -> {
                updatePermissionOverlay(
                    title = "Notifications",
                    description = "We need permission to send notifications so we can alert you even if your screen is off.",
                    buttonText = "Allow Notifications"
                )
            }

            !hasOverlay -> {
                updatePermissionOverlay(
                    title = "Display Over Other Apps",
                    description = "This allows the alarm to appear on top of other apps (like Google Maps) while you travel.",
                    buttonText = "Enable Overlay"
                )
            }

            else -> {
                // All permissions granted! Hide the shield.
                showPermissionOverlay = false
            }
        }
    }

    private fun updatePermissionOverlay(title: String, description: String, buttonText: String) {
        permissionTitle = title
        permissionDescription = description
        permissionButtonText = buttonText
        showPermissionOverlay = true
    }

    /**
     * Called when the user clicks the action button on the PermissionStepCard.
     */
    fun handlePermissionRequest(
        context: Context,
        onLaunchLocation: () -> Unit,
        onLaunchNotifications: () -> Unit
    ) {
        when (permissionTitle) {
            "Location Access" -> onLaunchLocation()
            "Notifications" -> onLaunchNotifications()
            "Display Over Other Apps" -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    /* ---------------- Navigation ---------------- */

    fun resetToStep(step: Int) {
        _currentStep.value = step
        if (step == 1) {
            _transportConfirmed.value = false
            _selectedStation.value = null
        }
    }

    fun nextStep() {
        if (_currentStep.value == 1 && _selectedTransport.value != null) {
            _transportConfirmed.value = true
        }
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun goBack() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(1)
    }

    /* ---------------- Selection ---------------- */

    fun selectTransport(transport: String) {
        if (_selectedTransport.value != transport) {
            _selectedTransport.value = transport
            _selectedStation.value = null
            _transportConfirmed.value = false
        }
    }

    fun selectStation(stationName: String, latitude: Double, longitude: Double) {
        _selectedStation.value = Station(
            name = stationName,
            type = _selectedTransport.value.orEmpty(),
            latitude = latitude,
            longitude = longitude,
            distance = 0
        )
    }

    fun clearStation() {
        _selectedStation.value = null
    }

    /* ---------------- Reset ---------------- */

    fun reset() {
        _currentStep.value = 1
        _selectedTransport.value = null
        _selectedStation.value = null
        _transportConfirmed.value = false
    }
}