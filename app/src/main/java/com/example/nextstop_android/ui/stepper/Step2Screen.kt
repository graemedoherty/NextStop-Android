package com.example.nextstop_android.ui.stepper

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.data.StationDataLoader
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.Station
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Step2Screen(
    selectedTransport: String,
    savedStation: Station?,
    onStationSelected: (stationName: String, latitude: Double, longitude: Double) -> Unit,
    onClearStation: () -> Unit,
    onNext: () -> Unit, // ✅ Added parameter
    onBack: () -> Unit,
    mapViewModel: MapViewModel
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

    val stationNames = stationDataList.map { it.getName() }
    var searchText by remember { mutableStateOf("") }
    val filteredStations = remember(searchText, selectedTransport) {
        if (searchText.length < 3) emptyList()
        else stationNames.filter { it.contains(searchText, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 2: Select destination station",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
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
                        Text(text = savedStation.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { onClearStation() }) {
                        Text("Change", fontSize = 13.sp)
                    }
                }
            }
        }

        if (filteredStations.isNotEmpty() && savedStation == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredStations.take(5).forEach { stationName ->
                    Surface(
                        onClick = {
                            val stationData = stationDataList.find { it.getName() == stationName }
                            stationData?.let {
                                keyboardController?.hide()
                                onStationSelected(stationName, it.getLatitude(), it.getLongitude())
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stationName, modifier = Modifier.padding(16.dp), fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext, // ✅ Now uses the transition callback
            enabled = savedStation != null,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back")
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadius * c).roundToInt()
}