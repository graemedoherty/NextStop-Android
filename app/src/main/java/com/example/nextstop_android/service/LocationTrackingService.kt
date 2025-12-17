package com.example.nextstop_android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlin.math.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0
    private var destinationName: String = ""

    companion object {
        private const val TAG = "LocationTrackingService"
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        const val EXTRA_DESTINATION_LAT = "destination_lat"
        const val EXTRA_DESTINATION_LNG = "destination_lng"
        const val EXTRA_DESTINATION_NAME = "destination_name"
        const val ARRIVAL_THRESHOLD_METERS = 100 // Trigger alarm within 100m

        // Broadcast action for distance updates
        const val ACTION_DISTANCE_UPDATE = "com.example.nextstop_android.DISTANCE_UPDATE"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_USER_LAT = "user_lat"
        const val EXTRA_USER_LNG = "user_lng"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                destinationLat = intent.getDoubleExtra(EXTRA_DESTINATION_LAT, 0.0)
                destinationLng = intent.getDoubleExtra(EXTRA_DESTINATION_LNG, 0.0)
                destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: "Destination"
                Log.d(TAG, "Starting tracking to: $destinationName at ($destinationLat, $destinationLng)")
                startTracking()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping tracking")
                stopTracking()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        Log.d(TAG, "startTracking called")
        val notification = createNotification("Tracking to $destinationName", "Calculating distance...")
        Log.d(TAG, "Notification created, calling startForeground")

        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Update every 5 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000L) // Fastest update every 2 seconds
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Location update received: ${location.latitude}, ${location.longitude}")
                    handleLocationUpdate(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location updates requested")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            stopSelf()
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val distance = calculateDistance(
            location.latitude, location.longitude,
            destinationLat, destinationLng
        )

        Log.d(TAG, "Distance calculated: ${distance}m")
        Log.d(TAG, "User at: (${location.latitude}, ${location.longitude})")
        Log.d(TAG, "Destination at: ($destinationLat, $destinationLng)")

        // Update notification
        val notification = createNotification(
            "Tracking to $destinationName",
            "${distance}m away"
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Broadcast distance update to the app (make it explicit)
        val broadcastIntent = Intent(ACTION_DISTANCE_UPDATE).apply {
            setPackage(packageName) // Make it explicit
            putExtra(EXTRA_DISTANCE, distance)
            putExtra(EXTRA_USER_LAT, location.latitude)
            putExtra(EXTRA_USER_LNG, location.longitude)
        }
        sendBroadcast(broadcastIntent)
        Log.d(TAG, "Broadcast sent: distance=$distance to package: $packageName")

        // Check if arrived
        if (distance <= ARRIVAL_THRESHOLD_METERS) {
            Log.d(TAG, "Arrival threshold reached!")
            handleArrival()
        }
    }

    private fun handleArrival() {
        // Update notification for arrival
        val notification = createNotification(
            "You've Arrived!",
            "You have reached $destinationName",
            isArrived = true
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Send arrival broadcast (make it explicit)
        val arrivalIntent = Intent(ACTION_DISTANCE_UPDATE).apply {
            setPackage(packageName) // Make it explicit
            putExtra(EXTRA_DISTANCE, 0)
        }
        sendBroadcast(arrivalIntent)

        Log.d(TAG, "Arrival notification sent")
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Int {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).roundToInt()
    }

    private fun stopTracking() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "Location updates stopped")
        }
    }

    private fun createNotification(title: String, content: String, isArrived: Boolean = false): Notification {
        val intent = Intent(this, Class.forName("com.example.nextstop_android.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(!isArrived)
            .setPriority(if (isArrived) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location to notify you when approaching your destination"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}