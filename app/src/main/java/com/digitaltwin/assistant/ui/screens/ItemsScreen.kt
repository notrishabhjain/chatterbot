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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.ui.viewmodel.ItemsViewModel

@Composable
fun ItemsScreen(vm: ItemsViewModel = hiltViewModel()) {
    val myTasks by vm.myTasks.collectAsStateWithLifecycle()
    val delegated by vm.delegated.collectAsStateWithLifecycle()
    val followUps by vm.followUps.collectAsStateWithLifecycle()
    val resolved by vm.resolved.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "My Tasks (${myTasks.size})",
        "Delegated (${delegated.size})",
        "Follow-ups (${followUps.size})",
        "Done",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Work Items",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }
        Spacer(Modifier.height(8.dp))

        val currentList = when (tab) {
            0 -> myTasks
            1 -> delegated
            2 -> followUps
            else -> resolved
        }

        if (currentList.isEmpty()) {
            Text(
                "Nothing here yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(currentList, key = { it.id }) { item ->
                    WorkItemCard(
                        item = item,
                        trailingContent = if (tab != 3) ({
                            IconButton(onClick = { vm.resolve(item) }) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Mark resolved",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }) else null,
                    )
                }
            }
        }
    }
}
