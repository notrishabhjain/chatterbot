package com.digitaltwin.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digitaltwin.assistant.data.local.entity.CallRecord
import com.digitaltwin.assistant.data.local.entity.CapturedNotification

@Dao
interface CaptureDao {

    /** Ignores duplicates by [CapturedNotification.contentHash]; returns -1 if it was a dupe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(n: CapturedNotification): Long

    @Query("SELECT * FROM captured_notifications WHERE processedAt IS NULL ORDER BY receivedAt ASC LIMIT :limit")
    suspend fun getUnprocessedNotifications(limit: Int = 50): List<CapturedNotification>

    @Update
    suspend fun updateNotification(n: CapturedNotification)

    @Query("DELETE FROM captured_notifications WHERE processedAt IS NOT NULL AND receivedAt < :before")
    suspend fun pruneProcessedNotifications(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCallRecord(c: CallRecord): Long

    @Query("SELECT * FROM call_records WHERE processedAt IS NULL ORDER BY calledAt ASC")
    suspend fun getUnprocessedCalls(): List<CallRecord>

    @Query("SELECT * FROM call_records WHERE recordingPath = :path LIMIT 1")
    suspend fun findCallByPath(path: String): CallRecord?

    @Update
    suspend fun updateCallRecord(c: CallRecord)
}
