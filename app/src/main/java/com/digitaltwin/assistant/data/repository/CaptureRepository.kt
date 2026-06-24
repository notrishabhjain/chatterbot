package com.digitaltwin.assistant.data.repository

import com.digitaltwin.assistant.ai.ExtractionCandidate
import com.digitaltwin.assistant.ai.ExtractionContext
import com.digitaltwin.assistant.ai.TaskExtractor
import com.digitaltwin.assistant.data.local.dao.CaptureDao
import com.digitaltwin.assistant.data.local.entity.CallRecord
import com.digitaltwin.assistant.data.local.entity.CapturedNotification
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.Source
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureRepository @Inject constructor(
    private val captureDao: CaptureDao,
    private val workItemRepository: WorkItemRepository,
    private val extractor: TaskExtractor,
    private val contactClassifier: ContactClassifier,
) {

    /** Called by the NotificationListenerService for every notification that arrives. */
    suspend fun storeNotification(notification: CapturedNotification): Boolean {
        val rowId = captureDao.insertNotification(notification)
        return rowId != -1L // false = duplicate (same contentHash), was already stored
    }

    /**
     * Batch-processes stored unprocessed notifications. Runs every 5 minutes from WorkManager.
     * Returns the count of work items queued.
     */
    suspend fun processUnprocessedNotifications(): Int {
        val pending = captureDao.getUnprocessedNotifications()
        var queued = 0
        val now = System.currentTimeMillis()

        for (n in pending) {
            val text = listOfNotNull(n.title, n.body).joinToString(" — ")
            if (text.isBlank()) {
                captureDao.updateNotification(n.copy(processedAt = now))
                continue
            }

            val source = sourceForPackage(n.appPackage)
            val context = ExtractionContext(source = source, contact = n.title)
            val candidates = extractor.extract(text, context)

            var workItemId: Long? = null
            if (candidates.isNotEmpty()) {
                val best = candidates.maxByOrNull { it.confidence }!!
                val category = contactClassifier.categoryFor(best.contact)
                val item = best.toWorkItem(source, n.title, category)
                workItemId = workItemRepository.insert(item)
                queued++
            }
            captureDao.updateNotification(n.copy(processedAt = now, workItemId = workItemId))
        }

        // Keep the notifications table small: prune anything older than 7 days.
        captureDao.pruneProcessedNotifications(now - PRUNE_AGE_MS)
        return queued
    }

    /** Stores a new call record. Returns the inserted id (or -1 if path already exists). */
    suspend fun storeCallRecord(record: CallRecord): Long =
        captureDao.insertCallRecord(record)

    /** Marks a call record processed and optionally stores extracted work items from the transcript. */
    suspend fun processCallRecord(record: CallRecord, candidates: List<ExtractionCandidate>): Int {
        val now = System.currentTimeMillis()
        var queued = 0
        for (candidate in candidates) {
            val category = contactClassifier.categoryFor(candidate.contact)
            val item = candidate.toWorkItem(Source.CALL_RECORDING, record.contactName, category)
                .copy(callRecordingPath = record.recordingPath)
            workItemRepository.insert(item)
            queued++
        }
        captureDao.updateCallRecord(record.copy(processedAt = now))
        return queued
    }

    suspend fun findCallByPath(path: String) = captureDao.findCallByPath(path)

    suspend fun getUnprocessedCalls() = captureDao.getUnprocessedCalls()

    suspend fun updateCallRecord(record: CallRecord) = captureDao.updateCallRecord(record)

    private fun sourceForPackage(pkg: String): Source = when {
        pkg.startsWith("com.whatsapp") -> Source.WHATSAPP
        pkg == "com.android.mms" || pkg == "com.google.android.apps.messaging" -> Source.SMS
        else -> Source.OTHER_NOTIF
    }

    companion object {
        private const val PRUNE_AGE_MS = 7 * 24 * 60 * 60 * 1000L
    }
}

// Extension so repositories don't depend on UI/AI layers at call site.
fun ExtractionCandidate.toWorkItem(
    source: Source,
    contact: String?,
    category: com.digitaltwin.assistant.data.model.Category,
) = WorkItem(
    title = title,
    description = description,
    type = type,
    source = source,
    status = ItemStatus.QUEUED,
    sourceContact = contact,
    assignedTo = assignedTo,
    waitingOn = waitingOn,
    category = category,
    priority = priority,
    dueAt = dueAt,
)
