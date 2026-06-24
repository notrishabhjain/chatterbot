package com.digitaltwin.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.digitaltwin.assistant.data.model.Category

/**
 * User-assigned classification for a contact (Client / Team / etc.). Lets the assistant
 * auto-categorise and prioritise captured items by who they involve. Keyed by a normalised
 * display name or number so it can be matched against notification senders and call participants.
 */
@Entity(
    tableName = "contact_tags",
    indices = [Index("matchKey", unique = true)],
)
data class ContactTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Lower-cased contact name or phone number used for matching. */
    val matchKey: String,
    val displayName: String,
    val category: Category,
)
