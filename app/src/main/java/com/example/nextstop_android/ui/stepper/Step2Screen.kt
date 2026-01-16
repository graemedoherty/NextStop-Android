package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    stationViewModel: StationViewModel
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Step 2: Select destination station",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                if (savedStation == null) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        textStyle = TextStyle(fontSize = 14.sp),
                        label = { Text("Search Station...", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        // 🔑 CORRECTED: Using OutlinedTextFieldDefaults
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Selected Station:", fontSize = 10.sp)
                                Text(
                                    text = savedStation.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { onClearStation() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Change", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 🎯 POPUP (Renders above the Map)
                if (filteredStations.isNotEmpty() && savedStation == null) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        offset = IntOffset(0, 150),
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 8.dp,
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                filteredStations.take(5).forEach { stationName ->
                                    TextButton(
                                        onClick = {
                                            val stationData =
                                                stationDataList.find { it.name == stationName }
                                            stationData?.let {
                                                keyboardController?.hide()
                                                onStationSelected(stationName, it.lat, it.long)
                                                searchText = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 10.dp
                                        )
                                    ) {
                                        Text(
                                            text = stationName,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                        )
                                    }
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = Color.LightGray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ───────────────── NAVIGATION BUTTONS ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Back", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        onNext()
                        // Bus station bounds logic...
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
                            stationViewModel.updateVisibleBounds(bounds)
                        }
                    },
                    enabled = savedStation != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Next", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}