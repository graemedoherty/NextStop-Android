package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.data.StationDataLoader
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.stations.StationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Step2Screen(
    selectedTransport: String,
    savedStation: Station?,
    onStationSelected: (stationName: String, latitude: Double, longitude: Double) -> Unit,
    onClearStation: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    mapViewModel: MapViewModel,
    stationViewModel: StationViewModel  // ADD THIS PARAMETER
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dataLoader = remember { StationDataLoader(context) }
    val mapUiState by mapViewModel.uiState.collectAsState()
    val userLocation = mapUiState.userLocation

    val stationDataList = remember(selectedTransport) {
        when (selectedTransport) {
            "Train" -> dataLoader.loadTrainStations()
            "Luas" -> dataLoader.loadLuasStations()
            "Bus" -> dataLoader.loadBusStations()
            else -> emptyList()
        }
    }

    val stationNames = stationDataList.map { it.name }
    var searchText by remember { mutableStateOf("") }
    val filteredStations = remember(searchText, selectedTransport) {
        if (searchText.length < 3) emptyList()
        else stationNames.filter { it.contains(searchText, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Step 2: Select destination station",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    
            )

            if (savedStation == null) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Station (min 3 letters)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Selected Station:", fontSize = 11.sp)
                            Text(
                                text = savedStation.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { onClearStation() }) {
                            Text("Change", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onNext()
                    // Force map to update nearby bus stations immediately
                    if (selectedTransport == "Bus" && userLocation != null) {
                        val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                            .include(
                                com.google.android.gms.maps.model.LatLng(
                                    userLocation.first - 0.01,
                                    userLocation.second - 0.01
                                )
                            )
                            .include(
                                com.google.android.gms.maps.model.LatLng(
                                    userLocation.first + 0.01,
                                    userLocation.second + 0.01
                                )
                            )
                            .build()
                        stationViewModel.updateVisibleBounds(bounds)  // FIXED: Now calls stationViewModel
                    }
                },
                enabled = savedStation != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }
        }

        if (filteredStations.isNotEmpty() && savedStation == null) {
            Column(
                modifier = Modifier
                    .padding(top = 110.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredStations.take(5).forEach { stationName ->
                    Surface(
                        onClick = {
                            val stationData = stationDataList.find { it.name == stationName }
                            stationData?.let {
                                keyboardController?.hide()
                                onStationSelected(stationName, it.lat, it.long)
                                searchText = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stationName,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}