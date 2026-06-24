package com.digitaltwin.assistant.ai.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Request ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user",
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String = "application/json",
    val temperature: Float = 0.2f,
)

// ── Response ─────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
)

// ── Structured extraction output (parsed from the JSON text part) ─────────────

@JsonClass(generateAdapter = true)
data class GeminiExtractionResponse(
    val items: List<GeminiExtractedItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeminiExtractedItem(
    val title: String,
    val description: String? = null,
    val type: String = "MY_TASK",
    val contact: String? = null,
    @Json(name = "assignedTo") val assignedTo: String? = null,
    @Json(name = "waitingOn") val waitingOn: String? = null,
    val priority: String = "MEDIUM",
    @Json(name = "dueDateDescription") val dueDateDescription: String? = null,
)

// ── Briefing output ───────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class GeminiBriefingResponse(
    val summary: String,
)
