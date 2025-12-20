package com.example.nextstop_android.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nextstop_android.ui.maps.Station
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

    fun selectTransport(transport: String) {
        if (_selectedTransport.value != transport) {
            _selectedTransport.value = transport
            _selectedStation.value = null
        }
        // Removed the automatic step increment from here
    }

    fun selectStation(stationName: String, latitude: Double, longitude: Double) {
        _selectedStation.value = Station(
            name = stationName,
            type = _selectedTransport.value.orEmpty(),
            latitude = latitude,
            longitude = longitude,
            distance = 0
        )
        // Removed the automatic step increment from here
    }

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
        }
    }

    fun goBack() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(1)
    }

    fun clearStation() {
        _selectedStation.value = null
    }

    fun reset() {
        _currentStep.value = 1
        _selectedTransport.value = null
        _selectedStation.value = null
    }
}