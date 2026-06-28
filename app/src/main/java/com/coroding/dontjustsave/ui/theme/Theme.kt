package com.coroding.dontjustsave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF17203A),
    onPrimary = Color.White,
    secondary = Color(0xFFB8C98D),
    tertiary = Color(0xFFF6C977),
    background = Color(0xFFFFF8EF),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFFFF4E4),
    onSurface = Color(0xFF17203A),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFE8DDCF),
)

@Composable
fun DontJustSaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
