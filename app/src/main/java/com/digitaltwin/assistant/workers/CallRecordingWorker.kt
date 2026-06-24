package com.digitaltwin.assistant.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digitaltwin.assistant.ai.ExtractionContext
import com.digitaltwin.assistant.ai.TaskExtractor
import com.digitaltwin.assistant.ai.TranscriptionResult
import com.digitaltwin.assistant.ai.Transcriber
import com.digitaltwin.assistant.data.local.entity.CallRecord
import com.digitaltwin.assistant.data.model.Source
import com.digitaltwin.assistant.data.prefs.SettingsStore
import com.digitaltwin.assistant.data.repository.CaptureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Triggered 60 seconds after a call ends (to let the recorder finish writing).
 * 1. Finds the newest audio file in the configured recordings directory.
 * 2. Transcribes it via Groq Whisper.
 * 3. Runs the rule-based extractor over the transcript.
 * 4. Queues any extracted items for user approval.
 */
@HiltWorker
class CallRecordingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val captureRepository: CaptureRepository,
    private val transcriber: Transcriber,
    private val extractor: TaskExtractor,
    private val settings: SettingsStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val phoneNumber = inputData.getString(KEY_PHONE_NUMBER)
        val recordingsDir = settings.recordingsDir ?: return Result.success() // Not configured yet.

        val dir = File(recordingsDir)
        if (!dir.exists() || !dir.isDirectory) return Result.success()

        // Pick the audio file written most recently (within the last 30 minutes).
        val cutoff = System.currentTimeMillis() - 30 * 60 * 1000L
        val newest = dir.listFiles()
            ?.filter { it.isFile && it.lastModified() > cutoff && isAudio(it) }
            ?.maxByOrNull { it.lastModified() }
            ?: return Result.success()

        // Guard: don't process the same recording twice.
        if (captureRepository.findCallByPath(newest.absolutePath) != null) return Result.success()

        val callRecord = CallRecord(
            phoneNumber = phoneNumber,
            recordingPath = newest.absolutePath,
        )
        val id = captureRepository.storeCallRecord(callRecord)
        if (id == -1L) return Result.success() // Duplicate path.

        val storedRecord = callRecord.copy(id = id)

        return when (val result = transcriber.transcribe(newest)) {
            is TranscriptionResult.NotConfigured -> {
                // Groq API key not set — save the record, user can manually revisit.
                Result.success()
            }
            is TranscriptionResult.Error -> {
                // Store transcript as error note; don't retry indefinitely.
                captureRepository.updateCallRecord(
                    storedRecord.copy(transcript = "⚠ Transcription error: ${result.message}"),
                )
                Result.success()
            }
            is TranscriptionResult.Success -> {
                val transcript = result.text
                val ctx = ExtractionContext(
                    source = Source.CALL_RECORDING,
                    contact = storedRecord.contactName,
                    userName = settings.userName,
                )
                val candidates = extractor.extract(transcript, ctx)
                captureRepository.updateCallRecord(storedRecord.copy(transcript = transcript))
                captureRepository.processCallRecord(storedRecord, candidates)
                Result.success()
            }
        }
    }

    private fun isAudio(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("mp3", "m4a", "aac", "ogg", "wav", "amr", "3gp", "opus")
    }

    companion object {
        const val KEY_PHONE_NUMBER = "phone_number"
    }
}
