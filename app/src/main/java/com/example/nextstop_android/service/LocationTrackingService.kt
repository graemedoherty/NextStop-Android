package com.example.nextstop_android.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nextstop_android.MainActivity
import com.google.android.gms.location.*
import java.util.Locale
import kotlin.math.*

class LocationTrackingService : Service(), TextToSpeech.OnInitListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null

    private var destinationLat: Double? = null
    private var destinationLng: Double? = null
    private var destinationName: String? = null

    private var alarmTriggered = false
    private var lastNotifiedDistance = -1

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

        const val ARRIVAL_THRESHOLD_METERS = 100
    }

    // ----------------------------------------------------
    // LIFECYCLE
    // ----------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.UK
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_DESTINATION -> {
                destinationLat = intent.getDoubleExtra(EXTRA_DESTINATION_LAT, 0.0)
                destinationLng = intent.getDoubleExtra(EXTRA_DESTINATION_LNG, 0.0)
                destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME)

                alarmTriggered = false
                lastNotifiedDistance = -1

                startForeground(
                    NOTIFICATION_ID,
                    buildTrackingNotification("Calculating distance...")
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L).build()

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
        val lat = destinationLat ?: return
        val lng = destinationLng ?: return

        val distance = calculateDistance(
            location.latitude,
            location.longitude,
            lat,
            lng
        )

        // 🔔 Send broadcast to update App UI
        sendBroadcast(
            Intent(ACTION_DISTANCE_UPDATE).apply {
                setPackage(packageName)
                putExtra("distance", distance)
                putExtra("user_lat", location.latitude)
                putExtra("user_lng", location.longitude)
            }
        )

        if (distance <= ARRIVAL_THRESHOLD_METERS && !alarmTriggered) {
            triggerAlarm()
        } else if (!alarmTriggered && shouldUpdateNotification(distance)) {
            updateTrackingNotification(distance)
        }
    }

    // ----------------------------------------------------
    // ALARM TRIGGER
    // ----------------------------------------------------

    private fun triggerAlarm() {
        Log.e("ALARM", "========== ALARM TRIGGERED ==========")
        alarmTriggered = true

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("Alarm", "Ringtone failed", e)
        }

        vibrator?.let {
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }

        if (isTtsReady) {
            tts?.speak(
                "Arriving at $destinationName",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ARRIVAL"
            )
        }

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION_NAME, destinationName)
        }

        try {
            startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e("ALARM", "FAILED to start AlarmActivity!", e)
        }

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Arrived!")
            .setContentText("You are at $destinationName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Alarm",
                stopPendingIntent
            )
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    // ----------------------------------------------------
    // NOTIFICATION UPDATES
    // ----------------------------------------------------

    private fun shouldUpdateNotification(distance: Int): Boolean {
        if (lastNotifiedDistance == -1) {
            lastNotifiedDistance = distance
            return true
        }
        // Update notification only every 50 meters to save battery
        val delta = abs(lastNotifiedDistance - distance)
        return if (delta >= 50) {
            lastNotifiedDistance = distance
            true
        } else false
    }

    private fun updateTrackingNotification(distance: Int) {
        // 🔑 Formatting: Use KM when > 1km, otherwise use Meters
        val distanceText = if (distance >= 1000) {
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
        // 🔑 These flags make the notification click act like the App Icon click
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
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
            .setContentTitle("Heading to $destinationName") // Destination always visible
            .setContentText(text) // Distance updates here
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
                "Arrival Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // ----------------------------------------------------
    // STOP & RESET
    // ----------------------------------------------------

    private fun stopEverything() {
        Log.e("ALARM", "Stopping service and cleaning up...")

        // Tell UI to reset to first step
        sendBroadcast(
            Intent(ACTION_ALARM_STOPPED).apply {
                setPackage(packageName)
                putExtra("FORCE_RESET", true)
            }
        )

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        ringtone?.stop()
        vibrator?.cancel()
        tts?.stop()

        destinationLat = null
        destinationLng = null
        destinationName = null
        alarmTriggered = false
        lastNotifiedDistance = -1

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----------------------------------------------------
    // DISTANCE MATH
    // ----------------------------------------------------

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Int {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return (2 * r * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
    }
}