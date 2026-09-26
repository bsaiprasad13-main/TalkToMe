package com.saiprasad.talktome.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.saiprasad.talktome.audio.TalkToMeAudioRecorder
import com.saiprasad.talktome.ui.bubble.FloatingBubble

class BubbleService : Service(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var audioRecorder: TalkToMeAudioRecorder
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        audioRecorder = TalkToMeAudioRecorder(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val settingsRepo = com.saiprasad.talktome.data.SettingsRepository(this)
        com.saiprasad.talktome.service.FocusEventBus.updateBubbleEnabled(settingsRepo.isBubbleEnabled)
        
        startForegroundService()
        showFloatingBubble()
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "talktome_bubble_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TalkToMe Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("TalkToMe")
                .setContentText("Listening for your voice...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now) // placeholder
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("TalkToMe")
                .setContentText("Listening for your voice...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun showFloatingBubble() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleService)
            setViewTreeSavedStateRegistryOwner(this@BubbleService)
            setContent {
                FloatingBubble(
                    audioRecorder = audioRecorder,
                    windowManager = windowManager,
                    layoutParams = params,
                    composeView = this,
                    onAudioRecorded = { file ->
                        if (file != null) {
                            val repository = com.saiprasad.talktome.network.TranscriptionRepository()
                            val result = repository.transcribeAudio(file)
                            result.onSuccess { transcript ->
                                android.util.Log.d("TalkToMe", "Transcript: $transcript")
                                
                                // Save to history
                                com.saiprasad.talktome.data.HistoryRepository(this@BubbleService).addTranscription(transcript)
                                
                                TranscriptionEventBus.emitTranscription(transcript)
                            }.onFailure { error ->
                                var errorMessage = error.message ?: "Unknown error"
                                if (error is retrofit2.HttpException) {
                                    try {
                                        val errorBody = error.response()?.errorBody()?.string()
                                        if (errorBody != null) {
                                            errorMessage = errorBody
                                        }
                                    } catch (e: Exception) {}
                                }
                                android.util.Log.e("TalkToMe", "Transcription failed: $errorMessage", error)
                                // We can still try to show a Toast for the error message, but the spinner will stop anyway.
                                android.widget.Toast.makeText(this@BubbleService, "Error: $errorMessage", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    // LifecycleOwner method
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
