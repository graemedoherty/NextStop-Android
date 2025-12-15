package com.example.nextstop_android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(12.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ad Placeholder")
        }
    }
}
