package com.digitaltwin.assistant.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitaltwin.assistant.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Local draft state so we only persist on blur/save, not on every keystroke.
    var userName by remember(state.userName) { mutableStateOf(state.userName) }
    var groqKey by remember(state.groqApiKey) { mutableStateOf(state.groqApiKey) }
    var geminiKey by remember(state.geminiApiKey) { mutableStateOf(state.geminiApiKey) }
    var recDir by remember(state.recordingsDir) { mutableStateOf(state.recordingsDir) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // --- Profile ---
        SectionTitle("Profile")
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Your name (used to detect delegation)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) vm.saveUserName(userName) },
            singleLine = true,
        )

        Divider()

        // --- Notification Listener ---
        SectionTitle("Notification capture")
        Text(
            "Digital Twin needs Notification Access to capture tasks from WhatsApp, SMS and email apps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) {
            Text("Open Notification Access settings")
        }

        Divider()

        // --- Call recording ---
        SectionTitle("Call recording capture")
        Text(
            "Set the folder your call recorder writes to. Digital Twin will scan it after each call.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        OutlinedTextField(
            value = recDir,
            onValueChange = { recDir = it },
            label = { Text("Recordings folder path (e.g. /storage/emulated/0/CallRecordings)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) vm.saveRecordingsDir(recDir) },
            singleLine = true,
        )

        Divider()

        // --- AI settings ---
        SectionTitle("AI & transcription")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Send text to cloud AI", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Required for Groq transcription and Gemini extraction. Text is never stored by them for training.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Switch(checked = state.cloudAiEnabled, onCheckedChange = { vm.setCloudAi(it) })
        }

        OutlinedTextField(
            value = groqKey,
            onValueChange = { groqKey = it },
            label = { Text("Groq API key (free at console.groq.com)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) vm.saveGroqKey(groqKey) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = state.cloudAiEnabled,
        )

        OutlinedTextField(
            value = geminiKey,
            onValueChange = { geminiKey = it },
            label = { Text("Gemini API key (Phase 2 — optional now)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) vm.saveGeminiKey(geminiKey) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = state.cloudAiEnabled,
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
