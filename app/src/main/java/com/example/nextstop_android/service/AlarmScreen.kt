package com.example.nextstop_android.service

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.R // 🔑 Make sure to import your R file
import com.example.nextstop_android.ui.theme.NextStopAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    destinationName: String,
    onStopAlarm: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onStopAlarm()
                true
            } else {
                false
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 🔑 REPLACED ICON WITH YOUR APP LOGO
            Image(
                painter = painterResource(id = R.drawable.white_app_icon),
                contentDescription = "Next Stop Logo",
                modifier = Modifier.size(100.dp), // Slightly larger for better branding
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "ARRIVED AT",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                letterSpacing = 2.sp
            )

            Text(
                text = destinationName.uppercase(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 42.sp
                ),
                textAlign = TextAlign.Center,
                color = Color.White,
                lineHeight = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Time to get off.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp, start = 24.dp, end = 24.dp),
            backgroundContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray.copy(alpha = 0.3f), CircleShape)
                )
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SWIPE TO STOP",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}