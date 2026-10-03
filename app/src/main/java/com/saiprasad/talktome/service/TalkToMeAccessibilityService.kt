package com.saiprasad.talktome.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.saiprasad.talktome.data.SettingsRepository
import com.saiprasad.talktome.ui.bubble.BubbleOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The heart of the app. It
 * 1. watches for the on-screen keyboard (TYPE_WINDOWS_CHANGED + the IME window in [getWindows]),
 * 2. draws the mic bubble itself as an accessibility overlay right above the keyboard,
 * 3. types the transcript into the focused field.
 *
 * Everything runs inside this one system-bound service, so there is no second service for
 * Android or the OEM to kill, and nothing that can crash on a background restart.
 */
class TalkToMeAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val settings by lazy { SettingsRepository.getInstance(this) }

    private var dictation: DictationController? = null
    private var overlay: BubbleOverlay? = null
    private var settingsJob: Job? = null

    // Two passes: a quick one, and one after the keyboard's open/close animation settles.
    private val evaluateSoon = Runnable { evaluateBubble() }
    private val evaluateSettled = Runnable { evaluateBubble() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        val controller = DictationController(this, scope)
        dictation = controller
        overlay = BubbleOverlay(this, settings, controller).also { it.attach() }

        settingsJob = scope.launch {
            settings.bubbleEnabled.collect { enabled ->
                if (!enabled) controller.cancelRecording()
                evaluateBubble()
            }
        }

        ServiceStatus.setConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> scheduleEvaluation()
        }
    }

    private fun scheduleEvaluation() {
        handler.removeCallbacks(evaluateSoon)
        handler.postDelayed(evaluateSoon, 50)
        handler.removeCallbacks(evaluateSettled)
        handler.postDelayed(evaluateSettled, 400)
    }

    private fun evaluateBubble() {
        val overlay = overlay ?: return
        // Never let an exception escape: a crashing accessibility service gets disabled by Android.
        try {
            val keyboard = findKeyboardBounds()
            val show = settings.bubbleEnabled.value && keyboard != null && !isFocusedFieldSensitive()
            overlay.update(show, keyboard)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to evaluate bubble visibility", e)
        }
    }

    /** Bounds of the on-screen keyboard window, or null when no keyboard is showing. */
    private fun findKeyboardBounds(): Rect? {
        val windowList = try {
            windows
        } catch (e: Exception) {
            return null
        }
        for (window in windowList) {
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                if (bounds.height() > 0) return bounds
            }
        }
        return null
    }

    private fun isFocusedFieldSensitive(): Boolean {
        val node = TextInjector.findFocusedEditable(this) ?: return false
        return TextInjector.isSensitive(node)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        scope.cancel()
        super.onDestroy()
    }

    private fun tearDown() {
        handler.removeCallbacksAndMessages(null)
        settingsJob?.cancel()
        settingsJob = null
        dictation?.release()
        overlay?.detach()
        dictation = null
        overlay = null
        ServiceStatus.setConnected(false)
    }

    private companion object {
        const val TAG = "TalkToMeAccessibility"
    }
}
