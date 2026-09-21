package com.saiprasad.talktome.network

import com.saiprasad.talktome.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

data class TranscriptionResponse(
    val transcript: String?,
    val language_code: String?,
    val language_probability: Double?
)

interface SarvamApi {
    @Multipart
    @POST("speech-to-text")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("mode") mode: RequestBody,
        @Part("language_code") languageCode: RequestBody
    ): TranscriptionResponse
}

object SarvamApiClient {
    private const val BASE_URL = "https://api.sarvam.ai/"

    val instance: SarvamApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("api-subscription-key", BuildConfig.SARVAM_API_KEY)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SarvamApi::class.java)
    }
}
