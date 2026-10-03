package com.saiprasad.talktome.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Records 16 kHz mono 16-bit PCM straight into a .wav file in the cache dir.
 *
 * A background writer owns the [AudioRecord] while recording; [stop] waits for that writer to
 * finish before stopping/releasing the recorder, so the file is never truncated and the native
 * recorder is never released mid-read.
 */
class TalkToMeAudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null
    private var writerJob: Job? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var keepWriting = false

    val isRecording: Boolean
        get() = audioRecord != null

    /** Starts recording. Returns false if the mic is unavailable (no permission, busy, etc.). */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (audioRecord != null) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return false
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid min buffer size: $minBufferSize")
            return false
        }
        val bufferSize = minBufferSize * 2

        val record = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            return false
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            record.release()
            return false
        }

        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "startRecording failed", e)
            record.release()
            return false
        }

        // Another app (e.g. a phone call) holding the mic leaves us in the STOPPED state.
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(TAG, "Microphone is busy")
            record.release()
            return false
        }

        val file = File(context.cacheDir, "talktome_audio_${System.currentTimeMillis()}.wav")
        audioRecord = record
        outputFile = file
        keepWriting = true
        writerJob = ioScope.launch { writePcm(record, file, bufferSize) }
        Log.d(TAG, "Recording started: ${file.absolutePath}")
        return true
    }

    /** Stops recording and returns the finished .wav file, or null if nothing usable was captured. */
    suspend fun stop(): File? {
        val record = audioRecord ?: return null
        val file = outputFile
        val writer = writerJob
        audioRecord = null
        outputFile = null
        writerJob = null
        keepWriting = false

        return withContext(Dispatchers.IO) {
            writer?.join()
            try {
                record.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "AudioRecord.stop failed", e)
            }
            record.release()

            if (file == null || !file.exists() || file.length() <= WAV_HEADER_SIZE) {
                file?.delete()
                null
            } else {
                writeWavHeader(file)
                Log.d(TAG, "Recording stopped: ${file.length()} bytes")
                file
            }
        }
    }

    /** Stops and discards the current recording. */
    suspend fun cancel() {
        stop()?.delete()
    }

    /** Fire-and-forget cleanup for when the owning service is torn down. */
    fun release() {
        if (audioRecord == null) return
        ioScope.launch { cancel() }
    }

    private fun writePcm(record: AudioRecord, file: File, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        try {
            FileOutputStream(file).use { out ->
                // Placeholder for the WAV header, filled in once the length is known.
                out.write(ByteArray(WAV_HEADER_SIZE.toInt()))
                while (keepWriting) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        out.write(buffer, 0, read)
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord.read error: $read")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing audio data", e)
        }
    }

    private fun writeWavHeader(file: File) {
        try {
            val totalAudioLen = file.length() - WAV_HEADER_SIZE
            val totalDataLen = totalAudioLen + 36
            val channels = 1
            val bitsPerSample = 16
            val byteRate = bitsPerSample * SAMPLE_RATE * channels / 8
            val blockAlign = channels * bitsPerSample / 8

            val header = ByteArray(WAV_HEADER_SIZE.toInt())
            fun putAscii(offset: Int, value: String) = value.forEachIndexed { i, c -> header[offset + i] = c.code.toByte() }
            fun putIntLE(offset: Int, value: Long) {
                for (i in 0 until 4) header[offset + i] = ((value shr (8 * i)) and 0xff).toByte()
            }
            fun putShortLE(offset: Int, value: Int) {
                header[offset] = (value and 0xff).toByte()
                header[offset + 1] = ((value shr 8) and 0xff).toByte()
            }

            putAscii(0, "RIFF")
            putIntLE(4, totalDataLen)
            putAscii(8, "WAVE")
            putAscii(12, "fmt ")
            putIntLE(16, 16) // size of 'fmt ' chunk
            putShortLE(20, 1) // PCM
            putShortLE(22, channels)
            putIntLE(24, SAMPLE_RATE.toLong())
            putIntLE(28, byteRate.toLong())
            putShortLE(32, blockAlign)
            putShortLE(34, bitsPerSample)
            putAscii(36, "data")
            putIntLE(40, totalAudioLen)

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(header)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing WAV header", e)
        }
    }

    companion object {
        private const val TAG = "TalkToMeAudio"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val WAV_HEADER_SIZE = 44L

        /** Bytes of PCM per second: 16000 samples * 2 bytes. */
        private const val BYTES_PER_SECOND = SAMPLE_RATE * 2L

        /** Clips shorter than this are almost always accidental taps. */
        fun isTooShort(file: File, minMillis: Long = 400): Boolean =
            (file.length() - WAV_HEADER_SIZE) * 1000 / BYTES_PER_SECOND < minMillis
    }
}
