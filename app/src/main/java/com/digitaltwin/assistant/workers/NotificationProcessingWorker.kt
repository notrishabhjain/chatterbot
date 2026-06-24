package com.digitaltwin.assistant.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.digitaltwin.assistant.data.repository.CaptureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Runs every 5 minutes to batch-process notifications stored by [TwinNotificationListenerService].
 * Keeping the processing out of the hot path makes the listener's onNotificationPosted cheap.
 */
@HiltWorker
class NotificationProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val captureRepository: CaptureRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            captureRepository.processUnprocessedNotifications()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "notification_processing"

        fun enqueueRecurring(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<NotificationProcessingWorker>(5, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
