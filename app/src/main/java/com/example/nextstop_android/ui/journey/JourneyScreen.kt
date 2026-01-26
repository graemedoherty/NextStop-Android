package com.example.nextstop_android.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.ads.AdBanner
import com.example.nextstop_android.ui.burger.AboutScreen
import com.example.nextstop_android.ui.burger.BurgerMenuButton
import com.example.nextstop_android.ui.burger.BurgerMenuContent
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    mapViewModel: MapViewModel = viewModel(),
    stepperViewModel: StepperViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutScreen by remember { mutableStateOf(false) }

    if (showAboutScreen) {
        AboutScreen(onBackClick = { showAboutScreen = false })
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            BurgerMenuContent(
                onSettingsClick = { scope.launch { drawerState.close() } },
                onHistoryClick = { scope.launch { drawerState.close() } },
                onAboutClick = { scope.launch { drawerState.close() }; showAboutScreen = true },
                onCloseClick = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // 🔑 Pushes content below the camera/status bar
                    .navigationBarsPadding() // 🔑 Respects bottom gesture bar
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.32f)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    StepperScreen(
                        mapViewModel = mapViewModel,
                        viewModel = stepperViewModel,
                        onAlarmCreated = { station -> mapViewModel.startAlarm(station) }
                    )
                }

                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    MapsScreen(
                        modifier = Modifier.fillMaxSize(),
                        mapViewModel = mapViewModel,
                        stepperViewModel = stepperViewModel
                    )

                    BurgerMenuButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}