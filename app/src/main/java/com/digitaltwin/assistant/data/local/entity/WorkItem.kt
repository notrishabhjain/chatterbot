package com.digitaltwin.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.digitaltwin.assistant.data.model.Category
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority
import com.digitaltwin.assistant.data.model.Source

/**
 * The central record. Everything the assistant captures becomes a [WorkItem]: the user's own tasks,
 * things they delegated, things they're waiting on, and status updates that close those out.
 */
@Entity(
    tableName = "work_items",
    indices = [
        Index("status"),
        Index("type"),
        Index("parentItemId"),
    ],
)
data class WorkItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val title: String,
    val description: String? = null,

    val type: ItemType,
    val source: Source,
    val status: ItemStatus,

    /** Contact this item came from / is about (display name). */
    val sourceContact: String? = null,

    /** For DELEGATED items: who it was assigned to. */
    val assignedTo: String? = null,

    /** For FOLLOW_UP items: who the user is waiting on. */
    val waitingOn: String? = null,

    val category: Category = Category.UNCATEGORIZED,
    val priority: Priority = Priority.MEDIUM,

    /** Epoch millis the item is due / scheduled for, if any. */
    val dueAt: Long? = null,

    /** Google Calendar event id once scheduled. */
    val calendarEventId: String? = null,

    /** Links a STATUS_UPDATE (or follow-up resolution) back to the item it updates. */
    val parentItemId: Long? = null,

    /** Absolute path of the call recording this item was extracted from, if applicable. */
    val callRecordingPath: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
)
