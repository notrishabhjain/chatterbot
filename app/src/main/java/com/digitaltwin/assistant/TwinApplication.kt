package com.digitaltwin.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.digitaltwin.assistant.workers.NotificationProcessingWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TwinApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workManager: WorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        NotificationProcessingWorker.enqueueRecurring(workManager)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CAPTURE,
                getString(R.string.channel_capture_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.channel_capture_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = getString(R.string.channel_reminders_desc) },
        )
    }

    companion object {
        const val CHANNEL_CAPTURE = "capture"
        const val CHANNEL_REMINDERS = "reminders"
    }
}
