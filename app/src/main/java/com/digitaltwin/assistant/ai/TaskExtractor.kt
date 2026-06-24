package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.data.model.Source

/** Context passed to an extractor so it can attribute and classify what it finds. */
data class ExtractionContext(
    val source: Source,
    val contact: String? = null,
    /** The user's own name, so the extractor can tell "tasks for me" from "tasks I delegated". */
    val userName: String? = null,
)

/**
 * Turns free text into candidate work items. Phase 1 ships [RuleBasedExtractor]; Phase 2 adds a
 * Gemini-backed implementation. The pipeline tries rules first (free, offline, instant) and only
 * escalates ambiguous text to the cloud, keeping API cost near zero.
 */
interface TaskExtractor {
    suspend fun extract(text: String, context: ExtractionContext): List<ExtractionCandidate>
}
