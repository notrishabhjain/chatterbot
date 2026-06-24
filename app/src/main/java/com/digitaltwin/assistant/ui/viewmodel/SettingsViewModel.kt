package com.digitaltwin.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.digitaltwin.assistant.data.prefs.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val groqApiKey: String = "",
    val geminiApiKey: String = "",
    val recordingsDir: String = "",
    val cloudAiEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            userName = store.userName ?: "",
            groqApiKey = store.groqApiKey ?: "",
            geminiApiKey = store.geminiApiKey ?: "",
            recordingsDir = store.recordingsDir ?: "",
            cloudAiEnabled = store.cloudAiEnabled,
        ),
    )
    val state = _state.asStateFlow()

    fun saveUserName(v: String) { store.userName = v; _state.value = _state.value.copy(userName = v) }
    fun saveGroqKey(v: String) { store.groqApiKey = v; _state.value = _state.value.copy(groqApiKey = v) }
    fun saveGeminiKey(v: String) { store.geminiApiKey = v; _state.value = _state.value.copy(geminiApiKey = v) }
    fun saveRecordingsDir(v: String) { store.recordingsDir = v; _state.value = _state.value.copy(recordingsDir = v) }
    fun setCloudAi(enabled: Boolean) { store.cloudAiEnabled = enabled; _state.value = _state.value.copy(cloudAiEnabled = enabled) }
}
