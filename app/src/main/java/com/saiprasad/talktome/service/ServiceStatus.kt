package com.saiprasad.talktome.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the accessibility service is actually bound and running right now. The system
 * setting can say "on" while the service is not connected (e.g. after a crash), which is the
 * state the home screen needs to warn about.
 */
object ServiceStatus {
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    internal fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }
}
