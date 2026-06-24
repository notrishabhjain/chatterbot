package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.ai.model.GeminiContent
import com.digitaltwin.assistant.ai.model.GeminiGenerationConfig
import com.digitaltwin.assistant.ai.model.GeminiPart
import com.digitaltwin.assistant.ai.model.GeminiRequest
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.ItemType
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiBriefingService @Inject constructor(
    private val api: GeminiApi,
    private val moshi: Moshi,
) {

    /** Returns a 2-3 sentence plain-English morning briefing, or null on error. */
    suspend fun generateBriefing(
        apiKey: String,
        items: List<WorkItem>,
        userName: String?,
    ): String? {
        val queued = items.count { it.status == ItemStatus.QUEUED }
        val overdue = items.count { it.status == ItemStatus.ACTIVE && it.dueAt != null && it.dueAt < System.currentTimeMillis() }
        val myTasks = items.count { it.type == ItemType.MY_TASK && it.status == ItemStatus.ACTIVE }
        val delegated = items.count { it.type == ItemType.DELEGATED && it.status == ItemStatus.WAITING }
        val followUps = items.count { it.type == ItemType.FOLLOW_UP && it.status == ItemStatus.WAITING }

        val greeting = if (userName != null) "Good morning $userName." else "Good morning."

        val prompt = """
            $greeting Here is the current task snapshot for a project manager:
            - Items waiting in approval queue: $queued
            - Overdue active tasks: $overdue
            - Active personal tasks: $myTasks
            - Items delegated and waiting on others: $delegated
            - Follow-ups pending a reply: $followUps

            Write a concise 2-3 sentence morning briefing summarizing what needs attention today.
            Be direct and action-oriented. Do not use bullet points. Plain text only.
        """.trimIndent()

        val response = runCatching {
            api.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(responseMimeType = "text/plain"),
                ),
            )
        }.getOrNull() ?: return fallbackBriefing(queued, overdue, myTasks)

        return response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?: fallbackBriefing(queued, overdue, myTasks)
    }

    private fun fallbackBriefing(queued: Int, overdue: Int, myTasks: Int): String {
        return buildString {
            if (queued > 0) append("You have $queued item${if (queued > 1) "s" else ""} awaiting review. ")
            if (overdue > 0) append("$overdue task${if (overdue > 1) "s are" else " is"} overdue. ")
            if (myTasks > 0) append("$myTasks active task${if (myTasks > 1) "s" else ""} on your list.")
            if (isEmpty()) append("You're all caught up — no pending items.")
        }
    }
}
