package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Tier-1 STT: Groq-hosted Whisper Large v3. Free tier covers typical personal call volume. */
@Singleton
class GroqTranscriber @Inject constructor(
    private val api: GroqApi,
    private val settings: SettingsStore,
) : Transcriber {

    override suspend fun transcribe(audio: File): TranscriptionResult = withContext(Dispatchers.IO) {
        val key = settings.groqApiKey ?: return@withContext TranscriptionResult.NotConfigured
        if (!audio.exists()) return@withContext TranscriptionResult.Error("Recording not found: ${audio.name}")

        try {
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = audio.name,
                body = audio.asRequestBody("audio/*".toMediaType()),
            )
            val plain = "text/plain".toMediaType()
            val response = api.transcribe(
                bearer = "Bearer $key",
                file = filePart,
                model = GroqApi.WHISPER_MODEL.toRequestBody(plain),
                responseFormat = "json".toRequestBody(plain),
            )
            TranscriptionResult.Success(response.text.trim())
        } catch (e: Exception) {
            TranscriptionResult.Error(e.message ?: "Transcription failed")
        }
    }
}
