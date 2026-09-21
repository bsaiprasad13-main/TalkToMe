package com.saiprasad.talktome.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object FocusEventBus {
    private val _isEditableFocused = MutableStateFlow(false)
    val isEditableFocused = _isEditableFocused.asStateFlow()

    fun updateFocusState(isFocused: Boolean) {
        _isEditableFocused.value = isFocused
    }
}
