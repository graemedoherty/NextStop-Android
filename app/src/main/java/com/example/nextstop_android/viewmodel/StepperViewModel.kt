package com.example.nextstop_android.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StepperViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    private val _selectedTransport = MutableStateFlow<String?>(null)
    val selectedTransport: StateFlow<String?> = _selectedTransport

    private val _selectedStation = MutableStateFlow<String?>(null)
    val selectedStation: StateFlow<String?> = _selectedStation

    private val _selectedStationLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val selectedStationLocation: StateFlow<Pair<Double, Double>?> = _selectedStationLocation

    fun selectTransport(transport: String) {
        _selectedTransport.value = transport
        _currentStep.value = 2
    }

    fun selectStation(station: String, latitude: Double, longitude: Double) {
        _selectedStation.value = station
        _selectedStationLocation.value = Pair(latitude, longitude)
        _currentStep.value = 3
    }

    fun goBack() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(1)
    }

    fun setStep(step: Int) {
        _currentStep.value = step
    }
}