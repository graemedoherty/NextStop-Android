package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2Screen(
    selectedTransport: String,
    onStationSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val stations = when (selectedTransport) {
        "Train" -> listOf("Connolly", "Heuston", "Pearse")
        "Luas" -> listOf("Tallaght", "Smithfield")
        "Bus" -> listOf("O'Connell Street", "Grafton Street")
        else -> emptyList()
    }

    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Step 2: Select station")

        Spacer(Modifier.height(24.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Station") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                stations.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            selected = it
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { selected?.let(onStationSelected) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
