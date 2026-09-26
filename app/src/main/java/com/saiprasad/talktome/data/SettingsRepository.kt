package com.saiprasad.talktome.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("talktome_settings", Context.MODE_PRIVATE)

    var isBubbleEnabled: Boolean
        get() = prefs.getBoolean("is_bubble_enabled", true)
        set(value) = prefs.edit().putBoolean("is_bubble_enabled", value).apply()
}
