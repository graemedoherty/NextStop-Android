package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.nextstop_android.data.StationDataLoader
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.ui.components.PrimaryButton
import com.example.nextstop_android.ui.components.SecondaryButton
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

    val themePurple = Color(0xFF6F66E3)
    val onSurfaceText = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantText = MaterialTheme.colorScheme.onSurfaceVariant

    val stationDataList = remember(selectedTransport) {
        when (selectedTransport) {
            "Train" -> dataLoader.loadTrainStations()
            "Luas" -> dataLoader.loadLuasStations()
            "Bus" -> dataLoader.loadBusStations()
            else -> emptyList()
        }
    }

    var searchText by remember { mutableStateOf("") }
    val filteredStations = remember(searchText, selectedTransport) {
        if (searchText.length < 2) emptyList()
        else stationDataList.map { it.name }.filter { it.contains(searchText, ignoreCase = true) }
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
            // --- TOP CONTENT ---
            Box(modifier = Modifier.fillMaxWidth()) {
                if (savedStation == null) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        textStyle = TextStyle(fontSize = 16.sp, color = onSurfaceText),
                        placeholder = {
                            Text(
                                "Search Station...",
                                color = onSurfaceVariantText,
                                fontSize = 16.sp
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = themePurple,
                            focusedBorderColor = themePurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                } else {
                    // 🔑 Removed border, kept the background color
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = themePurple.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Selected Station:",
                                    fontSize = 10.sp,
                                    color = themePurple,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = savedStation.name,
                                    fontSize = 16.sp,
                                    color = onSurfaceText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = { onClearStation() }) {
                                Text("Change", color = themePurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // --- DROPDOWN ---
                if (filteredStations.isNotEmpty() && savedStation == null) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        offset = IntOffset(0, 165),
                        properties = PopupProperties(dismissOnClickOutside = true)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .heightIn(max = 200.dp),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 8.dp,
                            shadowElevation = 10.dp,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                filteredStations.take(10).forEach { stationName ->
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
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stationName,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 14.sp,
                                            color = onSurfaceText,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- NAVIGATION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🔑 Now using your unified SecondaryButton component
                SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Next",
                    enabled = savedStation != null,
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}