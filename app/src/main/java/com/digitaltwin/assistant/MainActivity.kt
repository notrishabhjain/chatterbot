package com.digitaltwin.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.digitaltwin.assistant.ui.nav.Screen
import com.digitaltwin.assistant.ui.nav.bottomNavScreens
import com.digitaltwin.assistant.ui.screens.ItemsScreen
import com.digitaltwin.assistant.ui.screens.QueueScreen
import com.digitaltwin.assistant.ui.screens.SettingsScreen
import com.digitaltwin.assistant.ui.screens.TodayScreen
import com.digitaltwin.assistant.ui.theme.DigitalTwinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture text shared from other apps (e.g. corporate email browser).
        val sharedText = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            DigitalTwinTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            bottomNavScreens.forEach { screen ->
                                NavigationBarItem(
                                    selected = current == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Today.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (sharedText != null) Screen.Queue.route else Screen.Today.route,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(Screen.Today.route) { TodayScreen() }
                        composable(Screen.Queue.route) {
                            QueueScreen(prefillText = sharedText.takeIf { current == Screen.Queue.route })
                        }
                        composable(Screen.Items.route) { ItemsScreen() }
                        composable(Screen.Settings.route) { SettingsScreen() }
                    }
                }
            }
        }
    }
}
