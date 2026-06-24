package com.digitaltwin.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.ItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkItemDao {

    @Insert
    suspend fun insert(item: WorkItem): Long

    @Update
    suspend fun update(item: WorkItem)

    @Query("SELECT * FROM work_items WHERE id = :id")
    suspend fun getById(id: Long): WorkItem?

    /** The approval queue: everything awaiting a decision, newest first. */
    @Query("SELECT * FROM work_items WHERE status = 'QUEUED' ORDER BY createdAt DESC")
    fun observeQueue(): Flow<List<WorkItem>>

    @Query("SELECT COUNT(*) FROM work_items WHERE status = 'QUEUED'")
    fun observeQueueCount(): Flow<Int>

    /** Approved items of a given type, used by the four list tabs. */
    @Query(
        """
        SELECT * FROM work_items
        WHERE type = :type AND status NOT IN ('QUEUED', 'DISCARDED', 'RESOLVED')
        ORDER BY
            CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END,
            (dueAt IS NULL), dueAt ASC
        """,
    )
    fun observeActiveByType(type: ItemType): Flow<List<WorkItem>>

    @Query("SELECT * FROM work_items WHERE status = 'RESOLVED' ORDER BY lastUpdatedAt DESC")
    fun observeResolved(): Flow<List<WorkItem>>

    /** Items due between [from] and [to] (for the Today view and morning briefing). */
    @Query(
        """
        SELECT * FROM work_items
        WHERE status NOT IN ('QUEUED', 'DISCARDED', 'RESOLVED')
          AND dueAt IS NOT NULL AND dueAt BETWEEN :from AND :to
        ORDER BY dueAt ASC
        """,
    )
    fun observeDueBetween(from: Long, to: Long): Flow<List<WorkItem>>

    @Query(
        """
        SELECT * FROM work_items
        WHERE status NOT IN ('QUEUED', 'DISCARDED', 'RESOLVED')
          AND dueAt IS NOT NULL AND dueAt < :now
        ORDER BY dueAt ASC
        """,
    )
    suspend fun getOverdue(now: Long): List<WorkItem>

    /**
     * Open delegated/follow-up items the assistant might be able to auto-close when a matching
     * status update arrives.
     */
    @Query(
        """
        SELECT * FROM work_items
        WHERE status IN ('ACTIVE', 'WAITING', 'SCHEDULED')
          AND type IN ('DELEGATED', 'FOLLOW_UP')
        ORDER BY lastUpdatedAt ASC
        """,
    )
    suspend fun getOpenWaiting(): List<WorkItem>

    @Query("UPDATE work_items SET status = :status, lastUpdatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: ItemStatus, now: Long = System.currentTimeMillis())
}
