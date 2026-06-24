package com.digitaltwin.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF2E5BFF)
private val OnPrimary = Color.White
private val PrimaryContainer = Color(0xFFDDE3FF)
private val Secondary = Color(0xFF5B74FF)
private val Surface = Color(0xFFFFFBFE)
private val OnSurface = Color(0xFF1C1B1F)
private val SurfaceVariant = Color(0xFFE7E0EC)
private val Error = Color(0xFFB3261E)

private val TwinColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color(0xFF001164),
    secondary = Secondary,
    onSecondary = OnPrimary,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    error = Error,
)

@Composable
fun DigitalTwinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TwinColors,
        content = content,
    )
}
