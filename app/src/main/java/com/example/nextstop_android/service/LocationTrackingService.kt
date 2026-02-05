package com.example.nextstop_android.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nextstop_android.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var destinationLat: Double? = null
    private var destinationLng: Double? = null
    private var destinationName: String? = null

    private var alarmTriggered = false
    private var lastNotifiedDistance = -1

    // 🔥 FIX: Flag to prevent broadcasts after service is stopped
    private var isStopped = false

    companion object {
        const val CHANNEL_ID = "arrival_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_SET_DESTINATION = "com.example.nextstop_android.SET_DESTINATION"
        const val ACTION_STOP = "com.example.nextstop_android.STOP_TRACKING"
        const val ACTION_DISTANCE_UPDATE = "com.example.nextstop_android.DISTANCE_UPDATE"
        const val ACTION_ALARM_STOPPED = "com.example.nextstop_android.ALARM_STOPPED"
        const val EXTRA_DESTINATION_LAT = "destination_lat"
        const val EXTRA_DESTINATION_LNG = "destination_lng"
        const val EXTRA_DESTINATION_NAME = "destination_name"
        const val ARRIVAL_THRESHOLD_METERS = 300
    }

    // ----------------------------------------------------
    // LIFECYCLE
    // ----------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        isStopped = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_SET_DESTINATION -> {
                // 🔥 FIX: Reset the stopped flag when starting new tracking
                isStopped = false

                destinationLat = intent.getDoubleExtra(EXTRA_DESTINATION_LAT, 0.0)
                destinationLng = intent.getDoubleExtra(EXTRA_DESTINATION_LNG, 0.0)
                destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME)

                alarmTriggered = false
                lastNotifiedDistance = -1

                startForeground(
                    NOTIFICATION_ID,
                    buildTrackingNotification("Calculating distance…")
                )

                startLocationUpdates()
            }

            ACTION_STOP -> {
                stopEverything()
            }
        }

        return START_NOT_STICKY
    }

    // ----------------------------------------------------
    // LOCATION TRACKING
    // ----------------------------------------------------

    private fun startLocationUpdates() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val request =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3_000L
            ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { handleLocation(it) }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun handleLocation(location: Location) {
        // 🔥 CRITICAL FIX: Don't process or broadcast if service is stopped
        if (isStopped) return

        val lat = destinationLat ?: return
        val lng = destinationLng ?: return

        val distance = calculateDistance(
            location.latitude,
            location.longitude,
            lat,
            lng
        )

        // 🔥 FIX: Double-check we're not stopped before broadcasting
        if (!isStopped) {
            // 📡 Broadcast distance updates to UI
            sendBroadcast(
                Intent(ACTION_DISTANCE_UPDATE).apply {
                    setPackage(packageName)
                    putExtra("distance", distance)
                    putExtra("user_lat", location.latitude)
                    putExtra("user_lng", location.longitude)
                }
            )
        }

        when {
            distance <= ARRIVAL_THRESHOLD_METERS && !alarmTriggered -> {
                triggerAlarm()
            }

            !alarmTriggered && shouldUpdateNotification(distance) -> {
                updateTrackingNotification(distance)
            }
        }
    }

    // ----------------------------------------------------
    // ALARM HANDOFF
    // ----------------------------------------------------

    private fun triggerAlarm() {
        Log.e("ALARM", "🚨 ARRIVAL THRESHOLD REACHED")
        alarmTriggered = true

        startService(
            Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START
                putExtra(
                    AlarmService.EXTRA_DESTINATION_NAME,
                    destinationName
                )
            }
        )
    }

    // ----------------------------------------------------
    // NOTIFICATIONS
    // ----------------------------------------------------

    private fun shouldUpdateNotification(distance: Int): Boolean {
        if (lastNotifiedDistance == -1) {
            lastNotifiedDistance = distance
            return true
        }

        val delta = abs(lastNotifiedDistance - distance)
        return if (delta >= 50) {
            lastNotifiedDistance = distance
            true
        } else false
    }

    private fun updateTrackingNotification(distance: Int) {
        val distanceText =
            if (distance >= 1000) {
                "%.1f km remaining".format(distance / 1000.0)
            } else {
                "$distance m remaining"
            }

        getSystemService(NotificationManager::class.java)
            .notify(
                NOTIFICATION_ID,
                buildTrackingNotification(distanceText)
            )
    }

    private fun buildTrackingNotification(text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

        val openAppPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Heading to $destinationName")
            .setContentText(text)
            .setContentIntent(openAppPending)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Arrival Tracking",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // ----------------------------------------------------
    // STOP & CLEANUP
    // ----------------------------------------------------

    private fun stopEverything() {
        Log.e("ALARM", "🛑 Tracking stopped")

        // 🔥 CRITICAL FIX: Set flag IMMEDIATELY to prevent any more broadcasts
        isStopped = true

        // 🔥 FIX: Stop location updates FIRST before clearing data
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null

        // 🔥 FIX: Clear destination data BEFORE broadcasting stopped event
        destinationLat = null
        destinationLng = null
        destinationName = null
        alarmTriggered = false
        lastNotifiedDistance = -1

        // Now send the stopped broadcast (with no destination data to leak)
        sendBroadcast(
            Intent(ACTION_ALARM_STOPPED).apply {
                setPackage(packageName)
                putExtra("FORCE_RESET", true)
            }
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 🔥 FIX: Ensure we're marked as stopped
        isStopped = true
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----------------------------------------------------
    // DISTANCE MATH
    // ----------------------------------------------------

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)

        return (2 * r * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
    }
}