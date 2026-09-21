package com.saiprasad.talktome.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TranscriptionEventBus {
    private val _transcriptions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val transcriptions = _transcriptions.asSharedFlow()

    fun emitTranscription(text: String) {
        _transcriptions.tryEmit(text)
    }
}
