package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.ai.model.GeminiContent
import com.digitaltwin.assistant.ai.model.GeminiExtractionResponse
import com.digitaltwin.assistant.ai.model.GeminiGenerationConfig
import com.digitaltwin.assistant.ai.model.GeminiPart
import com.digitaltwin.assistant.ai.model.GeminiRequest
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority
import com.squareup.moshi.Moshi
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud extraction using Gemini 1.5 Flash. Only called when [RuleBasedExtractor] returns nothing
 * and the user has opted in to cloud AI. Returns candidates with confidence 0.85.
 */
@Singleton
class GeminiExtractor @Inject constructor(
    private val api: GeminiApi,
    private val moshi: Moshi,
) : TaskExtractor {

    override suspend fun extract(text: String, context: ExtractionContext): List<ExtractionCandidate> {
        return emptyList() // called directly via extractWithKey — not via interface
    }

    suspend fun extractWithKey(
        apiKey: String,
        text: String,
        context: ExtractionContext,
    ): List<ExtractionCandidate> {
        if (text.isBlank()) return emptyList()

        val prompt = buildPrompt(text, context)
        val response = runCatching {
            api.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(),
                ),
            )
        }.getOrNull() ?: return emptyList()

        val jsonText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: return emptyList()

        val parsed = runCatching {
            moshi.adapter(GeminiExtractionResponse::class.java).fromJson(jsonText)
        }.getOrNull() ?: return emptyList()

        return parsed.items.map { item ->
            ExtractionCandidate(
                title = item.title.take(140),
                description = item.description,
                type = parseType(item.type),
                contact = item.contact ?: context.contact,
                assignedTo = item.assignedTo,
                waitingOn = item.waitingOn,
                priority = parsePriority(item.priority),
                dueAt = parseDueDate(item.dueDateDescription),
                confidence = 0.85,
            )
        }
    }

    private fun buildPrompt(text: String, context: ExtractionContext): String {
        val userLine = if (context.userName != null) "My name is ${context.userName}. " else ""
        val contactLine = if (context.contact != null) "This message is from/about ${context.contact}. " else ""
        val sourceLine = "Source: ${context.source.name.lowercase().replace('_', ' ')}."

        return """
            ${userLine}${contactLine}${sourceLine}

            Analyze the following text and extract work items (tasks, delegations, follow-ups, status updates).
            Return ONLY a JSON object matching this schema, no other text:
            {
              "items": [
                {
                  "title": "short action title (max 140 chars)",
                  "description": "optional longer description or null",
                  "type": "MY_TASK | DELEGATED | FOLLOW_UP | STATUS_UPDATE",
                  "contact": "person name if mentioned or null",
                  "assignedTo": "who I assigned this to (for DELEGATED) or null",
                  "waitingOn": "who I am waiting on (for FOLLOW_UP) or null",
                  "priority": "HIGH | MEDIUM | LOW",
                  "dueDateDescription": "today | tomorrow | monday | friday | etc or null"
                }
              ]
            }

            Rules:
            - MY_TASK: something I need to do
            - DELEGATED: something I asked someone else to do
            - FOLLOW_UP: I am waiting for a response or action from someone
            - STATUS_UPDATE: a completion notice that closes a prior open item
            - If the text has no actionable content, return {"items": []}
            - Extract multiple items if multiple actions are present

            Text to analyze:
            """
            .trimIndent() + "\n\"$text\""
    }

    private fun parseType(raw: String): ItemType = when (raw.uppercase().trim()) {
        "DELEGATED" -> ItemType.DELEGATED
        "FOLLOW_UP" -> ItemType.FOLLOW_UP
        "STATUS_UPDATE" -> ItemType.STATUS_UPDATE
        else -> ItemType.MY_TASK
    }

    private fun parsePriority(raw: String): Priority = when (raw.uppercase().trim()) {
        "HIGH" -> Priority.HIGH
        "LOW" -> Priority.LOW
        else -> Priority.MEDIUM
    }

    private fun parseDueDate(desc: String?): Long? {
        if (desc == null) return null
        val lower = desc.lowercase().trim()
        val cal = Calendar.getInstance()
        return when {
            lower == "today" || lower == "eod" -> endOfDay(cal)
            lower == "tomorrow" -> { cal.add(Calendar.DAY_OF_YEAR, 1); endOfDay(cal) }
            lower == "monday" -> advanceTo(cal, Calendar.MONDAY)
            lower == "tuesday" -> advanceTo(cal, Calendar.TUESDAY)
            lower == "wednesday" -> advanceTo(cal, Calendar.WEDNESDAY)
            lower == "thursday" -> advanceTo(cal, Calendar.THURSDAY)
            lower == "friday" -> advanceTo(cal, Calendar.FRIDAY)
            lower == "saturday" -> advanceTo(cal, Calendar.SATURDAY)
            lower == "sunday" -> advanceTo(cal, Calendar.SUNDAY)
            else -> null
        }
    }

    private fun endOfDay(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 18)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun advanceTo(cal: Calendar, weekday: Int): Long {
        var guard = 0
        while (cal.get(Calendar.DAY_OF_WEEK) != weekday && guard++ < 8) cal.add(Calendar.DAY_OF_YEAR, 1)
        return endOfDay(cal)
    }
}
