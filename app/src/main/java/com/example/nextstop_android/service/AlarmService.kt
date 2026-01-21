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
        const val ACTION_ALARM_DISMISSED = "com.example.nextstop_android.ALARM_DISMISSED"
        const val EXTRA_DESTINATION_NAME = "destination_name"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // 🔊 Volume Control Variables
    private var volumeHandler: Handler? = null
    private var currentVolume = 0.1f // Start at 10% for ducking
    private val MAX_ALARM_VOLUME = 0.8f

    // 🗣️ Text To Speech
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
                    intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: "your destination"
                startAlarm(destination)
            }

            ACTION_STOP -> stopAlarm()
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(destinationName: String) {
        Log.e("ALARM", "🚨 AlarmService STARTED - Coordinated Voice & Sound")

        requestAudioFocus()
        startVibration()

        // 1. Start Sound low (10%) to allow TTS to be heard
        startSound(initialVolume = 0.1f)

        // 2. Start TTS immediately
        startTextToSpeech(destinationName)

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LocationTrackingService.EXTRA_DESTINATION_NAME, destinationName)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
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
        startActivity(fullScreenIntent)
    }

    private fun startSound(initialVolume: Float) {
        audioManager?.let { am ->
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (maxVol * 0.7).toInt(), 0)
        }

        mediaPlayer = MediaPlayer().apply {
            @Suppress("DEPRECATION")
            setAudioStreamType(AudioManager.STREAM_ALARM)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            val afd = resources.openRawResourceFd(R.raw.arrival_alarm)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            isLooping = true
            setVolume(initialVolume, initialVolume)
            prepare()
            start()
        }

        // ⚡ WAIT 5 SECONDS before ramping up (gives TTS time to speak)
        volumeHandler = Handler(Looper.getMainLooper())
        volumeHandler?.postDelayed({
            rampUpVolume()
        }, 5000)
    }

    private fun rampUpVolume() {
        val incrementVolume = object : Runnable {
            override fun run() {
                if (mediaPlayer != null && currentVolume < MAX_ALARM_VOLUME) {
                    currentVolume += 0.05f
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                    volumeHandler?.postDelayed(this, 500)
                }
            }
        }
        volumeHandler?.post(incrementVolume)
    }

    private fun startTextToSpeech(destinationName: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.getDefault()

                val params = android.os.Bundle()
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                // Push TTS volume to max within the stream
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)

                // ⚡ START IMMEDIATELY once ready
                if (isTtsReady) {
                    tts?.speak(
                        "Next stop $destinationName. Time to get off.",
                        TextToSpeech.QUEUE_FLUSH,
                        params,
                        "ALARM_TTS"
                    )
                }
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    ).build()
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

    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    private fun stopAlarm() {
        Log.e("ALARM", "🛑 AlarmService STOPPED")
        volumeHandler?.removeCallbacksAndMessages(null)
        currentVolume = 0.1f

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()

        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        sendBroadcast(Intent(ACTION_ALARM_DISMISSED).apply { setPackage(packageName) })
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Arrival Alarm", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}