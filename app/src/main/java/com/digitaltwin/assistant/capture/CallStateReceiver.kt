package com.digitaltwin.assistant.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.digitaltwin.assistant.workers.CallRecordingWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Listens for PHONE_STATE broadcasts. On IDLE (call ended), fires a delayed WorkManager job that
 * scans the recordings folder, finds the file that was just written, and processes it. The 60-second
 * delay gives recorder apps time to finish writing the audio file.
 */
@AndroidEntryPoint
class CallStateReceiver : BroadcastReceiver() {

    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_IDLE) return

        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val request = OneTimeWorkRequestBuilder<CallRecordingWorker>()
            .setInputData(workDataOf(CallRecordingWorker.KEY_PHONE_NUMBER to number))
            .setInitialDelay(RECORDING_SETTLE_SECS, TimeUnit.SECONDS)
            .build()

        workManager.enqueue(request)
    }

    companion object {
        private const val RECORDING_SETTLE_SECS = 60L
    }
}
