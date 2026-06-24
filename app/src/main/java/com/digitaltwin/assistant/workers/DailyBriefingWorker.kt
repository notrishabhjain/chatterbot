package com.digitaltwin.assistant.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.digitaltwin.assistant.MainActivity
import com.digitaltwin.assistant.R
import com.digitaltwin.assistant.TwinApplication
import com.digitaltwin.assistant.ai.GeminiBriefingService
import com.digitaltwin.assistant.data.local.dao.WorkItemDao
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.prefs.SettingsStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyBriefingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val briefingService: GeminiBriefingService,
    private val workItemDao: WorkItemDao,
    private val settings: SettingsStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val allItems = workItemDao.observeQueue().first() +
            workItemDao.getOverdue(System.currentTimeMillis()) +
            workItemDao.observeActiveByType(com.digitaltwin.assistant.data.model.ItemType.MY_TASK).first()

        val text = if (settings.cloudAiEnabled && settings.geminiApiKey != null) {
            briefingService.generateBriefing(
                apiKey = settings.geminiApiKey!!,
                items = allItems,
                userName = settings.userName,
            )
        } else {
            buildFallback(allItems)
        } ?: buildFallback(allItems)

        postNotification(text)
        return Result.success()
    }

    private fun buildFallback(items: List<com.digitaltwin.assistant.data.local.entity.WorkItem>): String {
        val queued = items.count { it.status == ItemStatus.QUEUED }
        val overdue = items.count { it.status == ItemStatus.ACTIVE && it.dueAt != null && it.dueAt < System.currentTimeMillis() }
        return buildString {
            if (queued > 0) append("$queued item${if (queued > 1) "s" else ""} awaiting review. ")
            if (overdue > 0) append("$overdue overdue task${if (overdue > 1) "s" else ""}.")
            if (isEmpty()) append("You're all caught up today.")
        }
    }

    private fun postNotification(text: String) {
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, TwinApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Good morning")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, notification)
    }

    companion object {
        private const val WORK_NAME = "daily_briefing"
        private const val NOTIF_ID = 2001

        fun enqueueAt8Am(workManager: WorkManager) {
            val now = Calendar.getInstance()
            val next8am = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = next8am.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailyBriefingWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
