package com.saiprasad.talktome.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide settings. The app UI and the accessibility service run in the same process,
 * so a single instance lets the bubble react instantly when the in-app switch is flipped.
 */
class SettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("talktome_settings", Context.MODE_PRIVATE)

    private val _bubbleEnabled = MutableStateFlow(prefs.getBoolean(KEY_BUBBLE_ENABLED, true))
    val bubbleEnabled: StateFlow<Boolean> = _bubbleEnabled.asStateFlow()

    fun setBubbleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, enabled).apply()
        _bubbleEnabled.value = enabled
    }

    /** Which screen edge the bubble is docked to. Wispr Flow defaults to the right edge. */
    var bubbleOnRight: Boolean
        get() = prefs.getBoolean(KEY_BUBBLE_ON_RIGHT, true)
        set(value) = prefs.edit().putBoolean(KEY_BUBBLE_ON_RIGHT, value).apply()

    /** Gap between the bubble's bottom and the keyboard's top, in px. -1 means "use the default". */
    var bubbleLiftPx: Int
        get() = prefs.getInt(KEY_BUBBLE_LIFT_PX, -1)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_LIFT_PX, value).apply()

    companion object {
        private const val KEY_BUBBLE_ENABLED = "is_bubble_enabled"
        private const val KEY_BUBBLE_ON_RIGHT = "bubble_on_right"
        private const val KEY_BUBBLE_LIFT_PX = "bubble_lift_px"

        @Volatile private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
