package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline, zero-cost first pass over captured text. It handles the common, unambiguous cases —
 * obvious action requests, delegations, follow-ups and status updates — in English and
 * Hindi/Hinglish, so most captures never need a paid AI call. Anything it can't confidently read
 * is left for the Gemini extractor (Phase 2) to interpret.
 */
@Singleton
class RuleBasedExtractor @Inject constructor() : TaskExtractor {

    override suspend fun extract(text: String, context: ExtractionContext): List<ExtractionCandidate> {
        val clean = text.trim()
        if (clean.length < 3) return emptyList()
        val lower = clean.lowercase()

        // A status update closing a prior commitment ("done", "sent", "ho gaya").
        if (STATUS_MARKERS.any { lower.contains(it) } && clean.length < 80) {
            return listOf(
                ExtractionCandidate(
                    title = clean,
                    type = ItemType.STATUS_UPDATE,
                    contact = context.contact,
                    confidence = 0.55,
                ),
            )
        }

        val type = when {
            DELEGATION_MARKERS.any { lower.contains(it) } -> ItemType.DELEGATED
            FOLLOW_UP_MARKERS.any { lower.contains(it) } -> ItemType.FOLLOW_UP
            ACTION_MARKERS.any { lower.contains(it) } -> ItemType.MY_TASK
            else -> return emptyList() // No actionable signal — defer to AI extractor.
        }

        val priority = if (URGENT_MARKERS.any { lower.contains(it) }) Priority.HIGH else Priority.MEDIUM

        return listOf(
            ExtractionCandidate(
                title = clean.take(140),
                description = if (clean.length > 140) clean else null,
                type = type,
                contact = context.contact,
                assignedTo = if (type == ItemType.DELEGATED) context.contact else null,
                waitingOn = if (type == ItemType.FOLLOW_UP) context.contact else null,
                priority = priority,
                dueAt = parseDueDate(lower),
                confidence = 0.7,
            ),
        )
    }

    /** Very small relative-date parser covering the phrases that dominate work chatter. */
    private fun parseDueDate(lower: String): Long? {
        val cal = Calendar.getInstance()
        return when {
            lower.contains("tomorrow") || lower.contains("kal") -> {
                cal.add(Calendar.DAY_OF_YEAR, 1); endOfDay(cal)
            }
            lower.contains("today") || lower.contains("aaj") || lower.contains("eod") -> endOfDay(cal)
            lower.contains("tonight") -> { cal.set(Calendar.HOUR_OF_DAY, 20); atMinute0(cal) }
            DAY_NAMES.any { lower.contains(it.key) } -> {
                val target = DAY_NAMES.entries.first { lower.contains(it.key) }.value
                advanceToWeekday(cal, target); endOfDay(cal)
            }
            else -> null
        }
    }

    private fun endOfDay(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 18); return atMinute0(cal)
    }

    private fun atMinute0(cal: Calendar): Long {
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun advanceToWeekday(cal: Calendar, weekday: Int) {
        var guard = 0
        while (cal.get(Calendar.DAY_OF_WEEK) != weekday && guard++ < 8) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    companion object {
        private val ACTION_MARKERS = listOf(
            "please", "pls", "plz", "can you", "could you", "need to", "have to", "make sure",
            "send", "share", "prepare", "review", "call", "schedule", "remind", "submit", "complete",
            "follow", "update", "finalize", "finalise", "draft", "check", "confirm", "arrange",
            // Hindi / Hinglish
            "karna", "karo", "kar do", "bhejo", "bhej do", "tayar", "banao", "dekho", "complete karo",
        )
        private val DELEGATION_MARKERS = listOf(
            "ask ", "tell ", "assign", "delegate", "get him", "get her", "have them",
            "ko bolo", "se bolo", "ko bol", "ko keh", "karwana", "karwao",
        )
        private val FOLLOW_UP_MARKERS = listOf(
            "waiting for", "waiting on", "follow up", "followup", "revert", "get back",
            "awaiting", "pending from", "intezaar", "wait kar",
        )
        private val STATUS_MARKERS = listOf(
            "done", "completed", "sent", "shared", "finished", "delivered",
            "ho gaya", "kar diya", "bhej diya", "complete ho",
        )
        private val URGENT_MARKERS = listOf(
            "urgent", "asap", "immediately", "right away", "critical", "today", "eod",
            "jaldi", "turant", "abhi",
        )
        private val DAY_NAMES = mapOf(
            "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY, "sunday" to Calendar.SUNDAY,
        )
    }
}
