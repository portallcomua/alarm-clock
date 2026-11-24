package com.example.tvclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmRingService : Service() {
    private val foregroundNotificationId = 2
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: android.media.Ringtone? = null
    private var textToSpeech: TextToSpeech? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Будильник"
        val alarmId = intent?.getIntExtra("ALARM_ID", 0) ?: 0
        val audioUri = intent?.getStringExtra("ALARM_AUDIO_URI")
        val spokenText = intent?.getStringExtra("ALARM_SPOKEN_TEXT")?.trim().orEmpty()
        val channelId = "alarm_ringing"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Будильники", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Сповіщення про спрацювання будильника"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                }
            )
        }

        val openIntent = Intent(this, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("ALARM_LABEL", label)
            putExtra("ALARM_ID", alarmId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, alarmId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $label")
            .setContentText("Час прокидатися")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(openPendingIntent, true)
            .setContentIntent(openPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(foregroundNotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(foregroundNotificationId, notification)
        }

        if (mediaPlayer == null && fallbackRingtone == null) startAlarmAudio(audioUri)
        if (spokenText.isNotBlank()) speakMessage(spokenText)
        return START_NOT_STICKY
    }

    private fun startAlarmAudio(customUri: String?) {
        val alarmUri = customUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(this, alarmUri)
            player.isLooping = true
            player.setOnPreparedListener { prepared ->
                if (mediaPlayer === prepared) prepared.start() else prepared.release()
            }
            player.setOnErrorListener { failed, _, _ ->
                if (mediaPlayer === failed) {
                    mediaPlayer = null
                    try { failed.release() } catch (_: Exception) { }
                    startFallbackRingtone(alarmUri)
                }
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            try { mediaPlayer?.release() } catch (_: Exception) { }
            mediaPlayer = null
            startFallbackRingtone(alarmUri)
        }
    }

    private fun speakMessage(message: String) {
        try {
            textToSpeech?.shutdown()
            textToSpeech = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = textToSpeech ?: return@TextToSpeech
                    val languageResult = engine.setLanguage(Locale.getDefault())
                    if (languageResult != TextToSpeech.LANG_MISSING_DATA &&
                        languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, "alarm_message")
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun startFallbackRingtone(uri: android.net.Uri) {
        try {
            fallbackRingtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
        } catch (_: Exception) {
            fallbackRingtone = null
        }
    }

    override fun onDestroy() {
        try { mediaPlayer?.stop() } catch (_: Exception) { }
        try { mediaPlayer?.release() } catch (_: Exception) { }
        mediaPlayer = null
        try { fallbackRingtone?.stop() } catch (_: Exception) { }
        fallbackRingtone = null
        try { textToSpeech?.stop() } catch (_: Exception) { }
        try { textToSpeech?.shutdown() } catch (_: Exception) { }
        textToSpeech = null
        super.onDestroy()
    }
}
