package com.digitaltwin.assistant.data.repository

import com.digitaltwin.assistant.data.local.dao.WorkItemDao
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.ItemType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkItemRepository @Inject constructor(
    private val dao: WorkItemDao,
) {
    fun queue(): Flow<List<WorkItem>> = dao.observeQueue()
    fun queueCount(): Flow<Int> = dao.observeQueueCount()
    fun activeByType(type: ItemType): Flow<List<WorkItem>> = dao.observeActiveByType(type)
    fun resolved(): Flow<List<WorkItem>> = dao.observeResolved()
    fun dueBetween(from: Long, to: Long): Flow<List<WorkItem>> = dao.observeDueBetween(from, to)

    suspend fun insert(item: WorkItem): Long = dao.insert(item)

    /**
     * Approve a queued item. Follow-up/delegated items move to WAITING (we're tracking someone
     * else); the user's own tasks become ACTIVE. Calendar scheduling is offered separately.
     */
    suspend fun approve(item: WorkItem) {
        val newStatus = when (item.type) {
            ItemType.DELEGATED, ItemType.FOLLOW_UP -> ItemStatus.WAITING
            else -> ItemStatus.ACTIVE
        }
        dao.update(item.copy(status = newStatus, lastUpdatedAt = System.currentTimeMillis()))
    }

    suspend fun discard(item: WorkItem) = dao.setStatus(item.id, ItemStatus.DISCARDED)

    suspend fun resolve(id: Long) = dao.setStatus(id, ItemStatus.RESOLVED)

    suspend fun markScheduled(id: Long, calendarEventId: String) {
        dao.getById(id)?.let {
            dao.update(
                it.copy(
                    status = ItemStatus.SCHEDULED,
                    calendarEventId = calendarEventId,
                    lastUpdatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun update(item: WorkItem) =
        dao.update(item.copy(lastUpdatedAt = System.currentTimeMillis()))

    suspend fun openWaitingItems(): List<WorkItem> = dao.getOpenWaiting()
    suspend fun overdue(now: Long = System.currentTimeMillis()): List<WorkItem> = dao.getOverdue(now)
}
