package com.digitaltwin.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Raw notification text harvested by the listener service. Stored first, processed later in batches
 * (every few minutes) to keep the capture path cheap and battery-friendly. [contentHash] is used
 * for de-duplication so the same message isn't turned into a task twice.
 */
@Entity(
    tableName = "captured_notifications",
    indices = [
        Index("contentHash", unique = true),
        Index("processedAt"),
    ],
)
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val title: String?,
    val body: String?,
    val contentHash: String,
    val receivedAt: Long = System.currentTimeMillis(),
    /** Null until the batch worker has processed this notification. */
    val processedAt: Long? = null,
    /** The work item produced, if any. */
    val workItemId: Long? = null,
)
