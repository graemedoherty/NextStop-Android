package com.example.nextstop_android.ui.journey

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.ads.AdBanner
import com.example.nextstop_android.ui.burger.AboutScreen
import com.example.nextstop_android.ui.burger.BurgerMenuButton
import com.example.nextstop_android.ui.burger.BurgerMenuContent
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.permissions.PermissionOverlay
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    mapViewModel: MapViewModel = viewModel(),
    stepperViewModel: StepperViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutScreen by remember { mutableStateOf(false) }

    // --- 🔑 PERMISSION LAUNCHERS (Kept Intact) ---
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { stepperViewModel.checkPermissions(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { stepperViewModel.checkPermissions(context) }

    // --- 🔑 LIFECYCLE OBSERVER (Kept Intact) ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                stepperViewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 🔑 THE FIX: Removed the 'return' block that was destroying the Map.

    Box(modifier = Modifier.fillMaxSize()) {
        // --- LAYER 1: MAIN CONTENT (Stays mounted in background) ---
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                BurgerMenuContent(
                    onSettingsClick = { scope.launch { drawerState.close() } },
                    onHistoryClick = { scope.launch { drawerState.close() } },
                    onAboutClick = {
                        scope.launch { drawerState.close() }
                        showAboutScreen = true
                    },
                    onCloseClick = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
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

                    Spacer(modifier = Modifier.height(8.dp))

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

                // --- LAYER 2: ADS (Kept Intact) ---
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    AdBanner(modifier = Modifier.fillMaxWidth())
                }

                // --- LAYER 3: ONBOARDING GATE (Kept Intact) ---
                if (stepperViewModel.showPermissionOverlay) {
                    PermissionOverlay(
                        pageIndex = stepperViewModel.onboardingPage,
                        title = stepperViewModel.permissionTitle,
                        description = stepperViewModel.permissionDescription,
                        buttonText = stepperViewModel.permissionButtonText,
                        onAction = {
                            stepperViewModel.handlePermissionRequest(
                                context = context,
                                onLaunchLocation = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onLaunchNotifications = {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }

        // --- LAYER 4: ABOUT SCREEN (Animated Overlay) ---
        // 🔑 Placing this last ensures it is on top, but the Map stays alive underneath.
        AnimatedVisibility(
            visible = showAboutScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AboutScreen(onBackClick = { showAboutScreen = false })
            }
        }
    }
}