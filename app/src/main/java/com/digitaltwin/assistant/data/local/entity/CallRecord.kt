package com.digitaltwin.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A finished phone call the assistant noticed. The recording at [recordingPath] is transcribed
 * (Groq Whisper) and the transcript mined for tasks/follow-ups. [transcript] is cached so we never
 * pay to transcribe the same file twice.
 */
@Entity(
    tableName = "call_records",
    indices = [Index("recordingPath", unique = true)],
)
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String? = null,
    val phoneNumber: String? = null,
    val durationSec: Int = 0,
    val recordingPath: String? = null,
    val calledAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val transcript: String? = null,
)
