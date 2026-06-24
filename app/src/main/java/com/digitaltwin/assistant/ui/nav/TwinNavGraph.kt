package com.digitaltwin.assistant.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Today : Screen("today", "Today", Icons.Default.Today)
    data object Queue : Screen("queue", "Queue", Icons.Default.Inbox)
    data object Items : Screen("items", "Items", Icons.Default.Checklist)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(Screen.Today, Screen.Queue, Screen.Items, Screen.Settings)
