package com.graemedoherty.nextstop_android.viewmodel

import android.Manifest
import android.app.Activity
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
import androidx.core.app.ActivityCompat
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

    /* ---------------- Permission Helpers ---------------- */

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Determines if we should show the "Settings" button.
     * Android returns 'false' for rationale if the user has checked "Don't ask again"
     * OR if they've never been asked before.
     */
    private fun isPermanentlyDenied(context: Context, permission: String): Boolean {
        val activity = context as? Activity ?: return false
        val isGranted = isPermissionGranted(context, permission)

        // This is the key: If it's NOT granted, AND the system says we shouldn't show the rationale,
        // it means the user has already seen the popup and denied it permanently.
        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        // We only consider it "Hard Denied" if they've seen the popup at least once.
        // On the very first run, shouldShowRationale is false, but we still want the popup.
        // This logic ensures the popup gets a chance first.
        return !isGranted && !shouldShowRationale && hasAskedBefore(context, permission)
    }

    // Small helper to track if we've triggered the system popup at least once
    private fun hasAskedBefore(context: Context, permission: String): Boolean {
        return context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
            .getBoolean(permission, false)
    }

    private fun markAsAsked(context: Context, permission: String) {
        context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(permission, true).apply()
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

    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /* ---------------- Permission Logic ---------------- */

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

            onboardingPage == 1 && !hasLocation -> {
                val isHardDenied =
                    isPermanentlyDenied(context, Manifest.permission.ACCESS_FINE_LOCATION)
                updateUI(
                    title = "Location Access",
                    description = if (isHardDenied) {
                        "Location access is blocked. To fix this, please tap 'Open Settings' and enable Location permissions manually."
                    } else {
                        "We use your location to calculate how close you are to your stop."
                    },
                    buttonText = if (isHardDenied) "Open Settings" else "Allow Location"
                )
            }

            onboardingPage == 2 && !hasNotifications && Build.VERSION.SDK_INT >= 33 -> {
                val isHardDenied =
                    isPermanentlyDenied(context, Manifest.permission.POST_NOTIFICATIONS)
                updateUI(
                    title = "Notifications",
                    description = if (isHardDenied) {
                        "Notifications are blocked. Please enable them in Settings to receive arrival alerts."
                    } else {
                        "This allows us to send the alert while you use other apps."
                    },
                    buttonText = if (isHardDenied) "Open Settings" else "Enable Notifications"
                )
            }

            onboardingPage == 3 && !hasOverlay -> updateUI(
                "Display Next Stop Alarm Over Other Apps",
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
                onboardingPage = 1
                checkPermissions(context)
            }

            1 -> {
                if (isPermanentlyDenied(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    openAppSettings(context)
                } else {
                    markAsAsked(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    onLaunchLocation()
                }
            }

            2 -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    if (isPermanentlyDenied(context, Manifest.permission.POST_NOTIFICATIONS)) {
                        openAppSettings(context)
                    } else {
                        markAsAsked(context, Manifest.permission.POST_NOTIFICATIONS)
                        onLaunchNotifications()
                    }
                }
            }

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

    /* ---------------- Navigation Actions ---------------- */

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