package com.saiprasad.talktome.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class TalkToMeAudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null
    private var timeoutJob: Job? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    var isRecording: Boolean = false
        private set

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    /**
     * Starts recording audio. 
     * If recording runs for 30s, it automatically stops and invokes onTimeout.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(onTimeout: (File?) -> Unit) {
        if (isRecording) return

        outputFile = File(context.cacheDir, "talktome_audio_${System.currentTimeMillis()}.wav")
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("TalkToMeAudio", "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.d("TalkToMeAudio", "Recording started: ${outputFile?.absolutePath}")

            // Write PCM data to file in background
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                writeAudioDataToFile(outputFile!!)
            }

            // Start 30 second safety timeout
            timeoutJob = scope.launch {
                delay(30_000)
                if (isRecording) {
                    Log.d("TalkToMeAudio", "Safety timeout hit. Stopping recording.")
                    val file = stopRecording()
                    onTimeout(file)
                }
            }
        } catch (e: Exception) {
            Log.e("TalkToMeAudio", "Failed to start recording", e)
            isRecording = false
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun writeAudioDataToFile(file: File) {
        val data = ByteArray(bufferSize)
        var os: FileOutputStream? = null

        try {
            os = FileOutputStream(file)
            // Leave 44 bytes for the WAV header
            val emptyHeader = ByteArray(44)
            os.write(emptyHeader)

            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (read > 0) {
                    os.write(data, 0, read)
                }
            }
        } catch (e: Exception) {
            Log.e("TalkToMeAudio", "Error writing audio data", e)
        } finally {
            os?.close()
        }
    }

    private fun writeWavHeader(file: File) {
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            val totalAudioLen = file.length() - 44
            val totalDataLen = totalAudioLen + 36
            val channels = 1
            val byteRate = 16 * sampleRate * channels / 8

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte() // RIFF/WAVE header
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte() // 'fmt ' chunk
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 4 bytes: size of 'fmt ' chunk
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // format = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (1 * 16 / 8).toByte() // block align
            header[33] = 0
            header[34] = 16 // bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            randomAccessFile.seek(0)
            randomAccessFile.write(header, 0, 44)
            randomAccessFile.close()
        } catch (e: Exception) {
            Log.e("TalkToMeAudio", "Error writing WAV header", e)
        }
    }

    /**
     * Stops the active recording and returns the recorded audio file.
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        
        isRecording = false // This will stop the write loop in writeAudioDataToFile
        
        timeoutJob?.cancel()
        timeoutJob = null

        return try {
            audioRecord?.stop()
            audioRecord?.release()
            
            // Wait slightly for the IO coroutine to finish writing PCM
            Thread.sleep(100)
            
            if (outputFile != null && outputFile!!.exists()) {
                writeWavHeader(outputFile!!)
            }
            
            Log.d("TalkToMeAudio", "Recording stopped.")
            outputFile
        } catch (e: Exception) {
            Log.e("TalkToMeAudio", "Failed to stop recording", e)
            audioRecord?.release()
            null
        } finally {
            audioRecord = null
        }
    }
}
