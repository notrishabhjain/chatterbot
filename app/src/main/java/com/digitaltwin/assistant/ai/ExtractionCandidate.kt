package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.data.model.Category
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority

/**
 * A potential work item produced by an extractor from a piece of text (notification, transcript,
 * shared content). Candidates always land in the approval queue — the user confirms before
 * anything is acted on. [confidence] (0..1) lets the UI sort/flag low-confidence guesses.
 */
data class ExtractionCandidate(
    val title: String,
    val description: String? = null,
    val type: ItemType = ItemType.MY_TASK,
    val contact: String? = null,
    val assignedTo: String? = null,
    val waitingOn: String? = null,
    val category: Category = Category.UNCATEGORIZED,
    val priority: Priority = Priority.MEDIUM,
    val dueAt: Long? = null,
    val confidence: Double = 0.5,
)
