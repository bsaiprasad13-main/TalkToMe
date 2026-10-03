package com.saiprasad.talktome.ui.bubble

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.saiprasad.talktome.data.SettingsRepository
import com.saiprasad.talktome.service.BubblePhase
import com.saiprasad.talktome.service.DictationController
import kotlin.math.roundToInt

/**
 * Owns the floating mic window.
 *
 * The window is a TYPE_ACCESSIBILITY_OVERLAY added by the accessibility service itself, so:
 * - it needs no "Display over other apps" permission,
 * - it is layered above the keyboard,
 * - it lives exactly as long as the accessibility service (no separate service to be killed).
 *
 * Like Wispr Flow, it docks to the left or right edge just above the keyboard; the user can drag
 * it and the chosen edge + height above the keyboard are remembered.
 */
class BubbleOverlay(
    private val service: AccessibilityService,
    private val settings: SettingsRepository,
    private val controller: DictationController,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private val lifecycleOwner = OverlayLifecycleOwner()

    private val edgeMarginPx = dp(6)
    private val defaultLiftPx = dp(12)
    private val minLiftPx = dp(4)
    private val topLimitPx = dp(48)

    private val showOverKeyboard = mutableStateOf(false)
    private var view: ComposeView? = null
    private var keyboardTop: Int? = null
    private var onRight = settings.bubbleOnRight

    private var dragging = false
    private var dragX = 0f
    private var dragY = 0f
    private var snapAnimator: ValueAnimator? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or if (onRight) Gravity.END else Gravity.START
        x = edgeMarginPx
        y = (screenHeight() * 0.55f).roundToInt()
    }

    fun attach() {
        if (view != null) return
        lifecycleOwner.start()

        val composeView = ComposeView(service).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val phase by controller.phase.collectAsState()
                FloatingBubble(
                    // Never vanish mid-dictation, even if the keyboard closes.
                    visible = showOverKeyboard.value || phase != BubblePhase.IDLE,
                    phase = phase,
                    onMicTap = controller::startRecording,
                    onCancel = controller::cancelRecording,
                    onAccept = controller::finishRecording,
                    onDragStart = ::onDragStart,
                    onDrag = ::onDrag,
                    onDragEnd = ::onDragEnd,
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
            view = composeView
        } catch (e: Exception) {
            Log.e(TAG, "Could not add bubble window", e)
            lifecycleOwner.destroy()
        }
    }

    fun detach() {
        snapAnimator?.cancel()
        view?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (e: Exception) {
                Log.w(TAG, "Could not remove bubble window", e)
            }
        }
        view = null
        lifecycleOwner.destroy()
    }

    /**
     * Called by the service whenever windows/focus change.
     * @param show whether the bubble should be offered (keyboard up, normal text field, enabled).
     * @param keyboardBounds the IME window's bounds on screen, or null if no keyboard is visible.
     */
    fun update(show: Boolean, keyboardBounds: Rect?) {
        showOverKeyboard.value = show
        if (keyboardBounds != null) keyboardTop = keyboardBounds.top
        if (keyboardBounds != null && !dragging && snapAnimator == null) {
            val targetY = yAboveKeyboard(keyboardBounds.top)
            if (targetY != params.y) {
                params.y = targetY
                applyLayout()
            }
        }
    }

    private fun yAboveKeyboard(top: Int): Int {
        val lift = settings.bubbleLiftPx.takeIf { it >= 0 } ?: defaultLiftPx
        return (top - bubbleHeightPx() - lift).coerceAtLeast(topLimitPx)
    }

    private fun onDragStart() {
        snapAnimator?.cancel()
        dragging = true
        // Work in absolute left/top coordinates while dragging.
        if (onRight) {
            params.x = screenWidth() - params.x - bubbleWidthPx()
            params.gravity = Gravity.TOP or Gravity.START
            onRight = false
        }
        dragX = params.x.toFloat()
        dragY = params.y.toFloat()
    }

    private fun onDrag(dx: Float, dy: Float) {
        if (!dragging) onDragStart()
        dragX += dx
        dragY += dy
        params.x = dragX.roundToInt()
        params.y = dragY.roundToInt().coerceIn(topLimitPx, screenHeight() - bubbleHeightPx())
        applyLayout()
    }

    private fun onDragEnd() {
        if (!dragging) return
        dragging = false

        val width = bubbleWidthPx()
        val screenWidth = screenWidth()
        val snapRight = params.x + width / 2 > screenWidth / 2
        val targetX = if (snapRight) screenWidth - width - edgeMarginPx else edgeMarginPx

        // Clamp so the bubble never ends up covering the keys, then remember the height.
        keyboardTop?.let { top ->
            val maxY = top - bubbleHeightPx() - minLiftPx
            if (params.y > maxY) params.y = maxY.coerceAtLeast(topLimitPx)
            settings.bubbleLiftPx = (top - bubbleHeightPx() - params.y).coerceAtLeast(minLiftPx)
        }
        settings.bubbleOnRight = snapRight

        snapAnimator = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                params.x = it.animatedValue as Int
                applyLayout()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    snapAnimator = null
                    if (snapRight) {
                        // Anchor to the right edge so the pill grows leftwards when recording.
                        params.gravity = Gravity.TOP or Gravity.END
                        params.x = edgeMarginPx
                        onRight = true
                    } else {
                        params.x = edgeMarginPx
                    }
                    applyLayout()
                }
            })
            start()
        }
    }

    private fun applyLayout() {
        val v = view ?: return
        if (!v.isAttachedToWindow) return
        try {
            windowManager.updateViewLayout(v, params)
        } catch (e: Exception) {
            Log.w(TAG, "updateViewLayout failed", e)
        }
    }

    private fun bubbleWidthPx(): Int = view?.width?.takeIf { it > 0 } ?: dp(BUBBLE_SIZE_DP)

    private fun bubbleHeightPx(): Int = view?.height?.takeIf { it > 0 } ?: dp(BUBBLE_SIZE_DP)

    private fun screenWidth(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            service.resources.displayMetrics.widthPixels
        }

    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            service.resources.displayMetrics.heightPixels
        }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private companion object {
        const val TAG = "TalkToMeOverlay"
    }
}

/** Minimal lifecycle so Compose can run inside a window that has no Activity. */
private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun start() {
        if (registry.currentState != Lifecycle.State.INITIALIZED) return
        savedStateController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        if (registry.currentState == Lifecycle.State.INITIALIZED || registry.currentState == Lifecycle.State.DESTROYED) return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
