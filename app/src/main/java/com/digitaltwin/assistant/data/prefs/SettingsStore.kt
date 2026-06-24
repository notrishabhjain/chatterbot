package com.digitaltwin.assistant.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small synchronous settings + secrets store. Backed by a dedicated SharedPreferences file that is
 * excluded from cloud backup (see data_extraction_rules.xml) so API keys never leave the device.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var groqApiKey: String?
        get() = prefs.getString(KEY_GROQ, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_GROQ, value) }

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_GEMINI, value) }

    /** The user's own name, used to tell "tasks for me" from "tasks I delegated". */
    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    /** Folder the user's call recorder writes to. */
    var recordingsDir: String?
        get() = prefs.getString(KEY_REC_DIR, null)
        set(value) = prefs.edit { putString(KEY_REC_DIR, value) }

    /** Whether the user has opted in to sending text to cloud AI. */
    var cloudAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_AI, false)
        set(value) = prefs.edit { putBoolean(KEY_CLOUD_AI, value) }

    companion object {
        const val FILE = "twin_secrets"
        private const val KEY_GROQ = "groq_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_REC_DIR = "recordings_dir"
        private const val KEY_CLOUD_AI = "cloud_ai_enabled"
    }
}
