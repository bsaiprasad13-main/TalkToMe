package com.saiprasad.talktome.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.saiprasad.talktome.BuildConfig
import com.saiprasad.talktome.audio.TalkToMeAudioRecorder
import com.saiprasad.talktome.data.HistoryRepository
import com.saiprasad.talktome.network.TranscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.IOException

enum class BubblePhase { IDLE, RECORDING, TRANSCRIBING }

/** Record → transcribe → insert, driven by taps on the bubble. All calls happen on the main thread. */
class DictationController(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
) {
    private val recorder = TalkToMeAudioRecorder(service)
    private val repository = TranscriptionRepository()

    private val _phase = MutableStateFlow(BubblePhase.IDLE)
    val phase: StateFlow<BubblePhase> = _phase.asStateFlow()

    private var timeoutJob: Job? = null
    private var targetNode: AccessibilityNodeInfo? = null

    fun startRecording() {
        if (_phase.value != BubblePhase.IDLE) return
        if (BuildConfig.SARVAM_API_KEY.isBlank()) {
            toast("Sarvam API key missing. Add SARVAM_API_KEY to local.properties and rebuild.")
            return
        }

        // Remember the field now; focus can wobble while we wait for the network.
        targetNode = TextInjector.findFocusedEditable(service)

        RecordingService.start(service)
        if (!recorder.start()) {
            RecordingService.stop(service)
            targetNode = null
            toast("Microphone is unavailable. Check the permission, or end any call using it.")
            return
        }

        _phase.value = BubblePhase.RECORDING
        timeoutJob = scope.launch {
            delay(MAX_RECORDING_MS)
            Log.d(TAG, "Max recording length reached, submitting")
            finishRecording()
        }
    }

    fun cancelRecording() {
        if (_phase.value != BubblePhase.RECORDING) return
        timeoutJob?.cancel()
        _phase.value = BubblePhase.IDLE
        targetNode = null
        scope.launch {
            recorder.cancel()
            RecordingService.stop(service)
        }
    }

    fun finishRecording() {
        if (_phase.value != BubblePhase.RECORDING) return
        timeoutJob?.cancel()
        _phase.value = BubblePhase.TRANSCRIBING
        scope.launch {
            try {
                val file = recorder.stop()
                RecordingService.stop(service)
                transcribeAndInsert(file)
            } finally {
                targetNode = null
                _phase.value = BubblePhase.IDLE
            }
        }
    }

    fun release() {
        timeoutJob?.cancel()
        recorder.release()
        RecordingService.stop(service)
        targetNode = null
        _phase.value = BubblePhase.IDLE
    }

    private suspend fun transcribeAndInsert(file: File?) {
        if (file == null) {
            toast("Recording failed. Please try again.")
            return
        }
        if (TalkToMeAudioRecorder.isTooShort(file)) {
            file.delete()
            toast("Too short. Tap the mic, speak, then tap ✔.")
            return
        }

        val transcript = repository.transcribeAudio(file).getOrElse { error ->
            Log.e(TAG, "Transcription failed", error)
            toast(describe(error))
            return
        }.trim()

        if (transcript.isEmpty()) {
            toast("Didn't catch that. Try again a little closer to the mic.")
            return
        }

        Log.d(TAG, "Transcript: $transcript")
        HistoryRepository.getInstance(service).addTranscription(transcript)

        if (TextInjector.inject(service, transcript, targetNode) == TextInjector.Result.COPIED_TO_CLIPBOARD) {
            toast("Couldn't type into this box. Copied to clipboard. Long-press to paste.")
        }
    }

    private fun describe(error: Throwable): String = when (error) {
        is HttpException -> when (error.code()) {
            401, 403 -> "Sarvam rejected the API key. Check SARVAM_API_KEY."
            429 -> "Too many requests to Sarvam. Wait a moment and retry."
            in 500..599 -> "Sarvam is having trouble right now. Try again shortly."
            else -> {
                val body = try {
                    error.response()?.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }
                "Transcription failed (${error.code()}): ${body ?: error.message()}"
            }
        }
        is IOException -> "No internet connection. Check your network and retry."
        else -> "Transcription failed: ${error.message ?: "unknown error"}"
    }

    private fun toast(message: String) {
        Toast.makeText(service, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "TalkToMeDictation"

        /** Sarvam's synchronous speech-to-text endpoint accepts up to 30 s of audio. */
        private const val MAX_RECORDING_MS = 29_000L
    }
}
