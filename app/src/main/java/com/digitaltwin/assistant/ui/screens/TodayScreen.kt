package com.digitaltwin.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.ui.viewmodel.TodayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(vm: TodayViewModel = hiltViewModel()) {
    val items by vm.todayItems.collectAsStateWithLifecycle()
    val queueCount by vm.queueCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (queueCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("$queueCount in queue", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            Text(
                "No tasks due today. ✓",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item -> WorkItemCard(item) }
            }
        }
    }
}

@Composable
fun WorkItemCard(item: WorkItem, trailingContent: @Composable (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (!item.sourceContact.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "From: ${item.sourceContact}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (item.dueAt != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Due: ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(item.dueAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}
