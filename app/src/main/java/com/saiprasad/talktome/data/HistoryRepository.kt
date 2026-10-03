package com.saiprasad.talktome.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Single shared instance so a transcription saved by the accessibility service shows up
 * immediately in the app's history list.
 */
class HistoryRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("talktome_history", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val historyString = prefs.getString("history_list", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(historyString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            _history.value = list
        } catch (e: Exception) {
            _history.value = emptyList()
        }
    }

    @Synchronized
    fun addTranscription(text: String) {
        if (text.isBlank()) return

        val currentList = _history.value.toMutableList()
        currentList.add(0, text) // Add to top
        while (currentList.size > MAX_ITEMS) {
            currentList.removeAt(currentList.size - 1)
        }

        _history.value = currentList

        // Save to SharedPreferences
        val jsonArray = JSONArray()
        currentList.forEach { jsonArray.put(it) }
        prefs.edit().putString("history_list", jsonArray.toString()).apply()
    }

    companion object {
        private const val MAX_ITEMS = 10

        @Volatile private var instance: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository =
            instance ?: synchronized(this) {
                instance ?: HistoryRepository(context.applicationContext).also { instance = it }
            }
    }
}
