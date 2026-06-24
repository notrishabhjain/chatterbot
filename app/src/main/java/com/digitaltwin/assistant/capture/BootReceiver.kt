package com.digitaltwin.assistant.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.digitaltwin.assistant.workers.NotificationProcessingWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Re-enqueues recurring WorkManager jobs after device restart. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        NotificationProcessingWorker.enqueueRecurring(workManager)
    }
}
