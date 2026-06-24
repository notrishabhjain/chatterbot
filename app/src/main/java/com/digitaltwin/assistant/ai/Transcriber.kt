package com.digitaltwin.assistant.ai

import java.io.File

sealed interface TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult
    data class Error(val message: String) : TranscriptionResult
    /** Returned when no API key is configured yet, so callers can prompt the user. */
    data object NotConfigured : TranscriptionResult
}

/**
 * Speech-to-text for call recordings. Phase 1 implementation is Groq Whisper Large v3 (free tier,
 * Hindi + English). The interface keeps the Sarvam AI and on-device Whisper tiers from the plan
 * pluggable behind the same call site.
 */
interface Transcriber {
    suspend fun transcribe(audio: File): TranscriptionResult
}
