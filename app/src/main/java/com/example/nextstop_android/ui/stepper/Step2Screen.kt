package com.example.nextstop_android.ui.stepper

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.nextstop_android.model.Station
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Step2Screen(
    selectedTransport: String,
    savedStation: Station?,
    onStationSelected: (stationName: String, latitude: Double, longitude: Double) -> Unit,
    onClearStation: () -> Unit,
    onNext: () -> Unit,
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

    // 🔑 We use a Box so the search results can "float" over the buttons
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
    ) {
        // LAYER 1: The Main Content & Buttons
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Step 2: Select destination station",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp).align(Alignment.CenterHorizontally)
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

            // This spacer pushes the buttons to the bottom
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNext,
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

        // LAYER 2: The Floating Dropdown Results
        if (filteredStations.isNotEmpty() && savedStation == null) {
            Column(
                modifier = Modifier
                    .padding(top = 110.dp) // Starts just below the TextField
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredStations.take(5).forEach { stationName ->
                    Surface(
                        onClick = {
                            val stationData = stationDataList.find { it.getName() == stationName }
                            stationData?.let {
                                keyboardController?.hide()
                                onStationSelected(stationName, it.getLatitude(), it.getLongitude())
                                searchText = "" // Clear search after selection
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 8.dp, // Higher elevation to look like it's floating
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stationName, modifier = Modifier.padding(16.dp), fontSize = 15.sp)
                    }
                }
            }
        }
    }
}