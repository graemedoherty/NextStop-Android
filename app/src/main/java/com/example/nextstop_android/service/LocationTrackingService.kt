package com.example.nextstop_android.service

import android.Manifest
import android.app.*
import android.content.Context
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
import com.google.android.gms.location.*
import kotlin.math.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var destinationLat: Double? = null
    private var destinationLng: Double? = null
    private var destinationName: String? = null

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        const val ACTION_SET_DESTINATION = "SET_DESTINATION"

        const val EXTRA_DESTINATION_LAT = "destination_lat"
        const val EXTRA_DESTINATION_LNG = "destination_lng"
        const val EXTRA_DESTINATION_NAME = "destination_name"

        const val ACTION_DISTANCE_UPDATE = "com.example.nextstop_android.DISTANCE_UPDATE"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_USER_LAT = "user_lat"
        const val EXTRA_USER_LNG = "user_lng"

        const val EXTRA_OPEN_ALARM = "open_alarm"
        const val ARRIVAL_THRESHOLD_METERS = 100
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startTracking()
            }

            ACTION_SET_DESTINATION -> {
                // 🔑 The Fix: Update the service's internal destination variables
                destinationLat = intent.getDoubleExtra(EXTRA_DESTINATION_LAT, 0.0)
                destinationLng = intent.getDoubleExtra(EXTRA_DESTINATION_LNG, 0.0)
                destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME)

                Log.d("LocationService", "Destination Set: $destinationName at $destinationLat, $destinationLng")
            }

            ACTION_STOP -> {
                stopTracking()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            stopSelf()
            return
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Next Stop", "Tracking location…")
        )

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { handleLocation(it) }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun handleLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude

        // Calculate distance only if we have a valid destination
        val distance = if (destinationLat != null && destinationLat != 0.0) {
            calculateDistance(lat, lng, destinationLat!!, destinationLng!!)
        } else {
            -1
        }

        val arrived = distance != -1 && distance <= ARRIVAL_THRESHOLD_METERS

        val content = when {
            arrived -> "Arrived at $destinationName"
            distance != -1 -> "$distance m to $destinationName"
            else -> "Calculating distance..."
        }

        // Update the persistent notification
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification("Next Stop", content, arrived))

        // 🔑 Broadcast the update to the MapViewModel
        sendBroadcast(Intent(ACTION_DISTANCE_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_USER_LAT, lat)
            putExtra(EXTRA_USER_LNG, lng)
            // Only send extra if it's a valid calculation
            if (distance != -1) {
                putExtra(EXTRA_DISTANCE, distance)
            }
        })
    }

    private fun stopTracking() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        // Reset destination on stop
        destinationLat = null
        destinationLng = null
    }

    private fun buildNotification(title: String, content: String, arrived: Boolean = false): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ALARM, true)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(!arrived)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "NextStop Tracking", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return (2 * r * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}