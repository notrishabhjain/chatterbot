package com.digitaltwin.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.ui.viewmodel.QueueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    prefillText: String? = null,
    vm: QueueViewModel = hiltViewModel(),
) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val extracting by vm.extracting.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(prefillText != null) }
    var inputText by remember { mutableStateOf(prefillText ?: "") }

    LaunchedEffect(prefillText) {
        if (prefillText != null) showSheet = true
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showSheet = true }) {
                Icon(Icons.Default.Add, "Add task manually")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${queue.size} item${if (queue.size == 1) "" else "s"} awaiting your decision",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))

            if (queue.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All clear — nothing in the queue.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(queue, key = { it.id }) { item ->
                        QueueItemCard(
                            item = item,
                            onApprove = { vm.approve(item) },
                            onDiscard = { vm.discard(item) },
                        )
                    }
                }
            }
        }

        if (showSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = { showSheet = false; inputText = "" }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add item manually", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        label = { Text("Paste email, message, or type a task…") },
                        maxLines = 6,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSheet = false; inputText = "" }) { Text("Cancel") }
                        TextButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    vm.extractAndQueue(inputText)
                                    showSheet = false
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !extracting,
                        ) {
                            if (extracting) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            else Text("Add to Queue")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemCard(item: WorkItem, onApprove: () -> Unit, onDiscard: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TypeChip(item.type, modifier = Modifier.weight(1f))
                SourceLabel(item)
            }
            Spacer(Modifier.height(6.dp))
            Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!item.sourceContact.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("From: ${item.sourceContact}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onApprove) {
                    Icon(Icons.Default.Check, "Approve", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDiscard) {
                    Icon(Icons.Default.Close, "Discard", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TypeChip(type: ItemType, modifier: Modifier = Modifier) {
    val (label, color) = when (type) {
        ItemType.MY_TASK -> "My Task" to MaterialTheme.colorScheme.primary
        ItemType.DELEGATED -> "Delegated" to MaterialTheme.colorScheme.tertiary
        ItemType.FOLLOW_UP -> "Follow-up" to MaterialTheme.colorScheme.secondary
        ItemType.STATUS_UPDATE -> "Status Update" to MaterialTheme.colorScheme.outline
    }
    Text(
        text = label,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
fun SourceLabel(item: WorkItem) {
    val label = when (item.source) {
        com.digitaltwin.assistant.data.model.Source.WHATSAPP -> "WhatsApp"
        com.digitaltwin.assistant.data.model.Source.SMS -> "SMS"
        com.digitaltwin.assistant.data.model.Source.CALL_RECORDING -> "Call"
        com.digitaltwin.assistant.data.model.Source.EMAIL_NOTIF -> "Email"
        com.digitaltwin.assistant.data.model.Source.MANUAL -> "Manual"
        com.digitaltwin.assistant.data.model.Source.OTHER_NOTIF -> "Notif"
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
}
