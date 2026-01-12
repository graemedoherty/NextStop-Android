package com.example.nextstop_android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nextstop_android.R
import java.util.Locale

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "arrival_alarm_channel"
        const val NOTIFICATION_ID = 99

        const val ACTION_START = "ALARM_START"
        const val ACTION_STOP = "ALARM_STOP"

        // 🔑 NEW: tells AlarmActivity to close
        const val ACTION_ALARM_DISMISSED =
            "com.example.nextstop_android.ALARM_DISMISSED"

        const val EXTRA_DESTINATION_NAME = "destination_name"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // 🔊 Text To Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val destination =
                    intent.getStringExtra(EXTRA_DESTINATION_NAME)
                        ?: "your destination"
                startAlarm(destination)
            }

            ACTION_STOP -> stopAlarm()
        }
        return START_NOT_STICKY
    }

    // --------------------------------------------------
    // ALARM START
    // --------------------------------------------------

    private fun startAlarm(destinationName: String) {
        Log.e("ALARM", "🚨 AlarmService STARTED")

        requestAudioFocus()
        startVibration()
        startSound()
        startTextToSpeech(destinationName)

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                LocationTrackingService.EXTRA_DESTINATION_NAME,
                destinationName
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmService::class.java).apply {
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Alarm",
                stopPendingIntent
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Launch UI immediately
        startActivity(fullScreenIntent)
    }

    // --------------------------------------------------
    // SOUND
    // --------------------------------------------------

    private fun startSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.arrival_alarm).apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            start()
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest =
                AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                            )
                            .build()
                    )
                    .build()

            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    // --------------------------------------------------
    // TEXT TO SPEECH
    // --------------------------------------------------

    private fun startTextToSpeech(destinationName: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.getDefault()

                // 🔑 Delay so speech cuts through alarm
                Handler(Looper.getMainLooper()).postDelayed({
                    speakDestination(destinationName)
                }, 2000)
            }
        }
    }

    private fun speakDestination(destinationName: String) {
        if (!isTtsReady) return

        val message =
            "Next stop, $destinationName. Get ready to embark."

        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "NEXT_STOP_TTS"
        )
    }

    private fun stopTextToSpeech() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    // --------------------------------------------------
    // VIBRATION
    // --------------------------------------------------

    private fun startVibration() {
        val pattern = longArrayOf(
            0, 1500, 300,
            1500, 300,
            2000
        )

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    // --------------------------------------------------
    // STOP
    // --------------------------------------------------

    private fun stopAlarm() {
        Log.e("ALARM", "🛑 AlarmService STOPPED")

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
        stopTextToSpeech()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        // 🔑 CLOSE AlarmActivity
        sendBroadcast(
            Intent(ACTION_ALARM_DISMISSED).apply {
                setPackage(packageName)
            }
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    // --------------------------------------------------
    // CHANNEL
    // --------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Arrival Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays when you reach your stop"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1500, 300, 1500)
                setBypassDnd(true)
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
