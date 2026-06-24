package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.ai.model.GroqTranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** Groq's OpenAI-compatible REST surface (only the audio transcription route we use). */
interface GroqApi {
    @Multipart
    @POST("openai/v1/audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
    ): GroqTranscriptionResponse

    companion object {
        const val BASE_URL = "https://api.groq.com/"
        const val WHISPER_MODEL = "whisper-large-v3"
    }
}
