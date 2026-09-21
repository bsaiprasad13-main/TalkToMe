package com.saiprasad.talktome.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class TranscriptionRepository {

    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val model = "saaras:v4".toRequestBody("text/plain".toMediaTypeOrNull())
            val mode = "translit".toRequestBody("text/plain".toMediaTypeOrNull())
            val languageCode = "te-IN".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = SarvamApiClient.instance.transcribeAudio(body, model, mode, languageCode)
            
            if (response.transcript != null) {
                Result.success(response.transcript)
            } else {
                Result.failure(Exception("Transcript was null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Clean up the temporary audio file
            if (audioFile.exists()) {
                audioFile.delete()
            }
        }
    }
}
