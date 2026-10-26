package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvColorScheme = darkColorScheme(
    primary = TvOrangeFocus,
    onPrimary = Color.Black,
    secondary = TvLimeAccent,
    onSecondary = Color.Black,
    tertiary = TvLimeAccent,
    background = TvDarkBackground,
    onBackground = TvTextPrimary,
    surface = TvDarkSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvDarkCard,
    onSurfaceVariant = TvTextSecondary
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        typography = Typography,
        content = content
    )
}
