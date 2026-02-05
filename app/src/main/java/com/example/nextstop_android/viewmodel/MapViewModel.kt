package com.example.nextstop_android.ui.maps

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    // ⚡ Controls the "One-time" snap to user location
    var hasInitialCenterPerformed = false

    // 🔥 FIX: Flag to prevent broadcasts from overwriting the reset
    private var isResetting = false

    private val distanceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 🔥 FIX: Ignore broadcasts during reset
            if (isResetting) return

            if (intent == null) return
            val distance = intent.getIntExtra("distance", -1)
            val lat = intent.getDoubleExtra("user_lat", 0.0)
            val lng = intent.getDoubleExtra("user_lng", 0.0)

            // 🔥 FIX: Only update tracking if we actually have an active alarm
            if (_uiState.value.alarmActive) {
                updateTracking(lat, lng, distance)
            }
        }
    }

    private val alarmStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            resetAllState()
        }
    }

    init {
        // 🔥 CRITICAL FIX: Force kill any lingering service FIRST
        // This ensures no old broadcasts can come through
        val killServiceIntent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        try {
            appContext.startService(killServiceIntent)
        } catch (e: Exception) {
            // Service might not be running, that's fine
        }

        // 🔥 FIX: Clear everything on fresh start
        resetAllState()

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // 🔥 FIX: Only set if we don't already have a location AND we're not resetting
                    if (_uiState.value.userLocation == null && !isResetting) {
                        updateUserLocation(it.latitude, it.longitude)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission handled in UI
        }

        ContextCompat.registerReceiver(
            appContext,
            distanceReceiver,
            IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            appContext,
            alarmStoppedReceiver,
            IntentFilter(LocationTrackingService.ACTION_ALARM_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun setStations(stations: List<Station>) {
        _uiState.update { it.copy(stations = stations, isStationsLoading = false, error = null) }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun setError(message: String?) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    fun setDestination(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                distanceToDestination = -1,
                stations = emptyList()
            )
        }
    }

    fun clearDestination() {
        hasInitialCenterPerformed = false
        _uiState.update { currentState ->
            currentState.copy(
                selectedStation = null,
                destinationLocation = null,
                distanceToDestination = -1,
                alarmArmed = false,
                alarmActive = false,
                alarmArrived = false,
                stations = emptyList()
            )
        }
    }

    fun startAlarm(station: Station) {
        isResetting = false // Re-enable broadcasts
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                alarmArmed = true,
                alarmActive = true,
                alarmArrived = false,
                distanceToDestination = -1,
                stations = emptyList()
            )
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        // 🔥 FIX: Don't update location if we're in the middle of resetting
        if (isResetting) return

        _uiState.update {
            it.copy(userLocation = latitude to longitude)
        }
    }

    fun updateTracking(latitude: Double, longitude: Double, distanceMeters: Int) {
        // 🔥 FIX: Don't update tracking if we're resetting or alarm isn't active
        if (isResetting || !_uiState.value.alarmActive) return

        val arrived = distanceMeters in 0..LocationTrackingService.ARRIVAL_THRESHOLD_METERS
        _uiState.update {
            it.copy(
                userLocation = latitude to longitude,
                distanceToDestination = distanceMeters,
                alarmArrived = arrived
            )
        }
    }

    fun cancelAlarm(stepperViewModel: StepperViewModel? = null) {
        val stopIntent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        appContext.startService(stopIntent)
        resetAllState()
        stepperViewModel?.reset()
    }

    fun resetAllState() {
        // 🔥 FIX: Set flag to block any incoming broadcasts during reset
        isResetting = true

        hasInitialCenterPerformed = false

        // 🔥 CRITICAL FIX: Don't preserve old location - start completely fresh
        // The old code was preserving userLocation, which kept the map centered on the old spot
        _uiState.value = MapUiState()

        // 🔥 FIX: Allow broadcasts again after a short delay
        // This prevents race conditions with service broadcasts
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isResetting = false
        }, 1000) // Increased to 1 second for extra safety
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(distanceReceiver)
            appContext.unregisterReceiver(alarmStoppedReceiver)
        } catch (_: Exception) {
            // Ignore
        }
    }
}