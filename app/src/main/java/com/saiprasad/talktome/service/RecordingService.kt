package com.saiprasad.talktome.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Short-lived microphone foreground service that runs only while the user is dictating.
 *
 * It exists to show the "TalkToMe is listening" notification and to keep the mic alive on
 * aggressive OEM builds. It is deliberately started with [Context.startService] (not
 * startForegroundService) and never sticky: if Android refuses to promote it to the foreground,
 * we just log and carry on, instead of crashing the process and taking the accessibility service
 * down with it (which is what makes Android disable accessibility services).
 */
class RecordingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ensureChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TalkToMe")
                .setContentText("Listening…")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setSilent(true)
                .build()
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } catch (e: Exception) {
            Log.w(TAG, "Could not promote recording service to foreground; continuing without it", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Dictation", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TalkToMeRecording"
        private const val CHANNEL_ID = "talktome_recording_channel"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            try {
                context.startService(Intent(context, RecordingService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start recording service", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, RecordingService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop recording service", e)
            }
        }
    }
}
