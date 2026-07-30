package com.hobbyhub.webrtc

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class VoiceForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "hobbyhub_voice_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_VOICE"
        const val ACTION_STOP = "ACTION_STOP_VOICE"
        const val EXTRA_CHANNEL_NAME = "EXTRA_CHANNEL_NAME"

        fun startService(context: Context, channelName: String) {
            val intent = Intent(context, VoiceForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CHANNEL_NAME, channelName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VoiceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Voice Room"
                val notification = buildNotification(channelName)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HobbyHub Voice Lounge",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aktif saat bergabung di Voice Lounge"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(channelName: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HobbyHub Voice Lounge")
            .setContentText("Terhubung di #$channelName - Mikrofon Aktif")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
