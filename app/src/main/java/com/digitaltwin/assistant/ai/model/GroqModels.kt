package com.digitaltwin.assistant.ai.model

import com.squareup.moshi.JsonClass

/** Response body from Groq's OpenAI-compatible audio transcription endpoint. */
@JsonClass(generateAdapter = true)
data class GroqTranscriptionResponse(
    val text: String,
)
