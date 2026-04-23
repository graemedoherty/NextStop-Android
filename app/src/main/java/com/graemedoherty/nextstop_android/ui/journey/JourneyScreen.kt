package com.graemedoherty.nextstop_android.ui.journey

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.graemedoherty.nextstop_android.ui.ads.AdBanner
import com.graemedoherty.nextstop_android.ui.burger.AboutScreen
import com.graemedoherty.nextstop_android.ui.burger.BurgerMenuButton
import com.graemedoherty.nextstop_android.ui.burger.BurgerMenuContent
import com.graemedoherty.nextstop_android.ui.maps.MapViewModel
import com.graemedoherty.nextstop_android.ui.maps.MapsScreen
import com.graemedoherty.nextstop_android.ui.permissions.PermissionOverlay
import com.graemedoherty.nextstop_android.ui.stepper.StepperScreen
import com.graemedoherty.nextstop_android.viewmodel.StepperViewModel
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

    // State for our custom fallback dialog if GPS specifically is off
    var showGpsFallbackDialog by remember { mutableStateOf(false) }

    // --- GPS Resolution Launcher (Standard System Popup) ---
    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showGpsFallbackDialog = false
        }
    }

    // Helper to check if the specific GPS provider is active
    fun checkGpsHardware(currentContext: Context) {
        val locationManager = currentContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // ⚡️ This is the check you need: is the physical GPS sensor enabled?
        val isGpsHardwareEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        
        Log.i("GPS_CHECK", "GPS Hardware Status: $isGpsHardwareEnabled")

        val hasPermission = ContextCompat.checkSelfPermission(
            currentContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        // 1. Try standard Play Services popup first (cleanest experience)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()
            
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(currentContext)
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                // If Play Services is "satisfied" but GPS sensor is STILL off, show our manual fallback
                if (!isGpsHardwareEnabled) {
                    Log.w("GPS_CHECK", "Location is ON but GPS sensor is OFF. Showing fallback dialog.")
                    showGpsFallbackDialog = true
                } else {
                    showGpsFallbackDialog = false
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        Log.i("GPS_CHECK", "Launching system resolution popup...")
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                        gpsLauncher.launch(intentSenderRequest)
                    } catch (e: Exception) {
                        showGpsFallbackDialog = true
                    }
                } else {
                    showGpsFallbackDialog = true
                }
            }
    }

    // --- Permissions Launchers ---
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        stepperViewModel.checkPermissions(context)
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            checkGpsHardware(context)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { stepperViewModel.checkPermissions(context) }

    // Startup check
    LaunchedEffect(Unit) {
        stepperViewModel.checkPermissions(context)
        checkGpsHardware(context)
    }

    // Resume check (covers returning to app)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                stepperViewModel.checkPermissions(context)
                checkGpsHardware(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.35f)
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdBanner(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // --- Custom Expert Dialog for GPS Provider Specifically ---
        if (showGpsFallbackDialog) {
            AlertDialog(
                onDismissRequest = { /* Force action */ },
                title = { Text("GPS Sensor Required", fontWeight = FontWeight.Bold) },
                text = { Text("Location services are on, but the GPS sensor is disabled. Next Stop needs direct satellite access to track your journey accurately. Please set Location Accuracy to 'High' or 'Device Only'.") },
                confirmButton = {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F66E3))
                    ) {
                        Text("Fix in Settings", color = Color.White)
                    }
                }
            )
        }

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

        AnimatedVisibility(visible = showAboutScreen, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AboutScreen(onBackClick = { showAboutScreen = false })
            }
        }
    }
}
