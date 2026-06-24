package com.digitaltwin.assistant.capture

import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.digitaltwin.assistant.data.local.entity.CapturedNotification
import com.digitaltwin.assistant.data.repository.CaptureRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Runs persistently once the user grants Notification Access. Every notification is hashed and
 * stored raw; a WorkManager job processes them in batch every 5 minutes. This keeps the hot path
 * (notification arrival) as cheap as a single DB insert.
 */
@AndroidEntryPoint
class TwinNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var captureRepository: CaptureRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // Packages that dominate work comms — others are stored but given a lower priority hint.
    private val workPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b", // WhatsApp Business
        "com.android.mms",
        "com.google.android.apps.messaging",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        // Never capture our own notifications — that would create infinite loops.
        if (pkg == packageName) return
        // Skip ongoing/persistent notifications (music playback, navigation, etc.).
        if (sbn.isOngoing) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title")
        val body = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()

        if (title.isNullOrBlank() && body.isNullOrBlank()) return

        val hash = sha1("$pkg|$title|$body")
        val notification = CapturedNotification(
            appPackage = pkg,
            title = title,
            body = body,
            contentHash = hash,
        )

        scope.launch {
            captureRepository.storeNotification(notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun sha1(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Check if the notification listener is currently enabled. */
        fun isEnabled(pm: PackageManager, packageName: String): Boolean {
            return try {
                val cn = ComponentName(packageName, TwinNotificationListenerService::class.java.name)
                pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } catch (_: Exception) {
                false
            }
        }
    }
}
