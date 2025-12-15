package com.example.nextstop_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextStopAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StepperScreen()
                }
            }
        }
    }
}

@Composable
fun StepperScreen() {
    var currentStep by remember { mutableStateOf(1) }
    var selectedTransport by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress indicators
        StepIndicators(currentStep)



        // Content with animation
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1Screen(
                        onTransportSelected = { transport ->
                            selectedTransport = transport
                            currentStep = 2
                        }
                    )
                    2 -> Step2Screen(
                        selectedTransport = selectedTransport ?: "",
                        onStationSelected = { station ->
                            selectedStation = station
                            currentStep = 3
                        },
                        onBack = { currentStep = 1 }
                    )
                    3 -> Step3Screen(
                        selectedTransport = selectedTransport ?: "",
                        selectedStation = selectedStation ?: "",
                        onAlarmSet = {
                            // Handle alarm set
                        },
                        onBack = { currentStep = 2 }
                    )
                }
            }
        }

        // Ad section at bottom
        AdSection()
    }
}

@Composable
fun StepIndicators(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val stepNum = index + 1
            val isActive = stepNum <= currentStep

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNum.toString(),
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (stepNum < 3) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 8.dp),
                    color = if (stepNum < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun Step1Screen(onTransportSelected: (String) -> Unit) {
    var selectedTransport by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Step 1: Select mode of transport",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val transportModes = listOf("Train", "Luas", "Bus")

        transportModes.forEach { mode ->
            OutlinedButton(
                onClick = { selectedTransport = mode },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 8.dp)
                    .border(
                        width = 2.dp,
                        color = if (selectedTransport == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, if (selectedTransport == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mode,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedTransport == mode) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { selectedTransport?.let { onTransportSelected(it) } },
            enabled = selectedTransport != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Step2Screen(
    selectedTransport: String,
    onStationSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val stations = when (selectedTransport) {
        "Train" -> listOf("Dublin Connolly", "Dublin Heuston", "Pearse Station", "Dún Laoghaire")
        "Luas" -> listOf("Tallaght", "Bride's Glen", "Smithfield", "The Point")
        "Bus" -> listOf("O'Connell Street", "College Green", "Dawson Street", "Grafton Street")
        else -> listOf()
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<String?>(null) }

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
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedStation ?: "Choose a station",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Station") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station) },
                            onClick = {
                                selectedStation = station
                                expandedDropdown = false
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { selectedStation?.let { onStationSelected(it) } },
            enabled = selectedStation != null,
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

@Composable
fun Step3Screen(
    selectedTransport: String,
    selectedStation: String,
    onAlarmSet: () -> Unit,
    onBack: () -> Unit
) {
    var alarmTime by remember { mutableStateOf("08:00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Step 3: Set alarm",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onAlarmSet,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Alarm", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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

@Preview(showBackground = true)
@Composable
fun StepperScreenPreview() {
    NextStopAndroidTheme {
        StepperScreen()
    }
}

@Composable
fun AdSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(modifier = Modifier.padding(bottom = 12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Special Offer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Get 20% off your next journey",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = {},
                    modifier = Modifier
                        .height(40.dp)
                        .padding(start = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Learn More", fontSize = 11.sp)
                }
            }
        }
    }
}