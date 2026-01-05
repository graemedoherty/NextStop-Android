package com.example.nextstop_android.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nextstop_android.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepperViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _selectedTransport = MutableStateFlow<String?>(null)
    val selectedTransport: StateFlow<String?> = _selectedTransport.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    // 🔑 NEW: transport confirmation signal
    private val _transportConfirmed = MutableStateFlow(false)
    val transportConfirmed: StateFlow<Boolean> = _transportConfirmed.asStateFlow()

    /* ---------------- Navigation ---------------- */

    // Forces UI to Journey/Map screen
    fun moveToJourney() {
        _currentStep.value = 3
    }

    fun nextStep() {
        // 🚍 When moving forward AFTER transport selection,
        // we confirm the transport choice
        if (_currentStep.value == 1 && _selectedTransport.value != null) {
            _transportConfirmed.value = true
        }

        if (_currentStep.value < 3) {
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

            // Reset confirmation if user changes transport
            _transportConfirmed.value = false
        }
    }

    fun selectStation(
        stationName: String,
        latitude: Double,
        longitude: Double
    ) {
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
