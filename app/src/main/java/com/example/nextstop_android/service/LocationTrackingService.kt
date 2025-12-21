package com.example.nextstop_android.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nextstop_android.MainActivity
import com.google.android.gms.location.*
import java.util.*
import kotlin.math.*

class LocationTrackingService : Service(), TextToSpeech.OnInitListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null
    private var isAlarmTriggered = false

    private var destinationLat: Double? = null
    private var destinationLng: Double? = null
    private var destinationName: String? = null
    private var lastLocation: Location? = null

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        const val ACTION_SET_DESTINATION = "SET_DESTINATION"
        const val ACTION_DISTANCE_UPDATE = "com.example.nextstop_android.DISTANCE_UPDATE"

        const val EXTRA_DESTINATION_LAT = "destination_lat"
        const val EXTRA_DESTINATION_LNG = "destination_lng"
        const val EXTRA_DESTINATION_NAME = "destination_name"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_USER_LAT = "user_lat"
        const val EXTRA_USER_LNG = "user_lng"
        const val EXTRA_OPEN_ALARM = "open_alarm"

        const val ARRIVAL_THRESHOLD_METERS = 100
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_DESTINATION -> {
                destinationLat = intent.getDoubleExtra(EXTRA_DESTINATION_LAT, 0.0)
                destinationLng = intent.getDoubleExtra(EXTRA_DESTINATION_LNG, 0.0)
                destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: "Destination"
                isAlarmTriggered = false

                // 🔑 1. Start Foreground ONLY when destination is set
                val initialNotification = buildNotification("Alarm Set", "Monitoring distance to $destinationName...", false)
                startForeground(NOTIFICATION_ID, initialNotification)

                // 🔑 2. Begin GPS tracking
                startTracking()
            }
            ACTION_STOP -> stopAlarmAndService()
        }
        return START_STICKY
    }

    private fun startTracking() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        // More aggressive settings for better testing
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L) // 3s intervals (faster for testing)
            .setMinUpdateIntervalMillis(1_000L) // Accept updates as fast as 1 second
            .setMaxUpdateDelayMillis(5_000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    Log.d("LocationService", "New location: ${it.latitude}, ${it.longitude}, accuracy: ${it.accuracy}m")
                    lastLocation = it
                    handleLocation(it)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        Log.d("LocationService", "Location tracking started with 3s interval")
    }

    private fun handleLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude

        Log.d("LocationService", "handleLocation called: lat=$lat, lng=$lng")
        Log.d("LocationService", "Destination: lat=$destinationLat, lng=$destinationLng")

        val distance = if (destinationLat != null && destinationLat != 0.0) {
            calculateDistance(lat, lng, destinationLat!!, destinationLng!!)
        } else -1

        Log.d("LocationService", "Distance to destination: $distance meters")

        val arrived = distance != -1 && distance <= ARRIVAL_THRESHOLD_METERS

        if (arrived && !isAlarmTriggered) {
            Log.d("LocationService", "ARRIVED! Triggering alarm")
            triggerAlarm()
        }

        // 🔑 Updates visual text, but doesn't "bombard" with sound/vibrate
        updateNotification(distance, arrived)

        sendBroadcast(Intent(ACTION_DISTANCE_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_DISTANCE, distance)
            putExtra(EXTRA_USER_LAT, lat)
            putExtra(EXTRA_USER_LNG, lng)
        })

        Log.d("LocationService", "Broadcast sent with distance: $distance")
    }

    private fun triggerAlarm() {
        isAlarmTriggered = true

        // Ensure volume is up for the alarm
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (maxVol * 0.8).toInt(), 0)
        }

        requestAudioFocus()

        // Play Ringtone
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("LocationService", "Ringtone failed", e)
        }

        // Vibrate
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(pattern, 0)
        }

        // TTS
        if (isTtsReady) {
            val message = "Next stop, $destinationName. Please prepare to disembark."
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ALARM_TTS")
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .build()
            focusRequest?.let { audioManager?.requestAudioFocus(it) }
        }
    }

    private fun updateNotification(distance: Int, arrived: Boolean) {
        val title = if (arrived) "🚨 Arrived!" else "Next Stop Tracking"
        val content = when {
            arrived -> "You have arrived at $destinationName"
            distance != -1 -> "$distance m remaining to $destinationName"
            else -> "Calculating distance..."
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, content, arrived))
    }

    private fun buildNotification(title: String, content: String, arrived: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ALARM, true)
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // 🔑 Prevents noise every 5 seconds
            .setSilent(!arrived)    // 🔑 Quiet while tracking, loud when alarm triggers
            .setPriority(if (arrived) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Arrival Alerts", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return (2 * r * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
    }

    private fun stopAlarmAndService() {
        ringtone?.let { if (it.isPlaying) it.stop() }
        ringtone = null
        tts?.stop()
        vibrator?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager?.abandonAudioFocusRequest(focusRequest!!)
        }
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        stopForeground(true)
        stopSelf()

        // 🔑 Notify the UI to close the map
        sendBroadcast(Intent(ACTION_STOP).setPackage(packageName))
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}