package com.example.nextstop_android.ui.stepper

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Step2Screen(
    selectedTransport: String,
    onStationSelected: (stationName: String, latitude: Double, longitude: Double) -> Unit,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val dataLoader = remember { StationDataLoader(context) }

    // Load stations based on selected transport
    val stationDataList = remember(selectedTransport) {
        when (selectedTransport) {
            "Train" -> dataLoader.loadTrainStations()
            "Luas" -> dataLoader.loadLuasStations()
            "Bus" -> dataLoader.loadBusStations()
            else -> emptyList()
        }
    }

    val stations = stationDataList.map { it.getName() }

    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    // Filter stations based on search text (minimum 3 characters)
    val filteredStations = remember(searchText, selectedTransport) {
        if (searchText.length < 3) {
            emptyList()
        } else {
            stations.filter { station ->
                station.contains(searchText, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Step 2: Select destination station",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = expanded && filteredStations.isNotEmpty(),
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { newValue ->
                        searchText = newValue
                        expanded = true
                    },
                    readOnly = false,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Station (min 3 letters)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filteredStations.isNotEmpty()) },
                    shape = RoundedCornerShape(12.dp)
                )

                if (filteredStations.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expanded && filteredStations.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredStations.forEach { station ->
                            DropdownMenuItem(
                                text = { Text(station) },
                                onClick = {
                                    val stationData = stationDataList.find { it.getName() == station }
                                    if (stationData != null) {
                                        searchText = station
                                        selected = station
                                        expanded = false
                                        keyboardController?.hide()
                                        onStationSelected(
                                            station,
                                            stationData.getLatitude(),
                                            stationData.getLongitude()
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (searchText.isNotEmpty() && searchText.length < 3) {
            Text(
                text = "Type at least 3 characters to see suggestions",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                selected?.let { stationName ->
                    val stationData = stationDataList.find { it.getName() == stationName }
                    if (stationData != null) {
                        onStationSelected(
                            stationName,
                            stationData.getLatitude(),
                            stationData.getLongitude()
                        )
                    }
                }
            },
            enabled = selected != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}