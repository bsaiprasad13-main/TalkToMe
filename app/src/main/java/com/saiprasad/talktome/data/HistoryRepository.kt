package com.saiprasad.talktome.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class HistoryRepository(context: Context) {
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

    fun addTranscription(text: String) {
        val currentList = _history.value.toMutableList()
        currentList.add(0, text) // Add to top
        if (currentList.size > 10) {
            currentList.removeAt(currentList.size - 1) // Keep only 10
        }
        
        _history.value = currentList
        
        // Save to SharedPreferences
        val jsonArray = JSONArray()
        currentList.forEach { jsonArray.put(it) }
        prefs.edit().putString("history_list", jsonArray.toString()).apply()
    }
}
