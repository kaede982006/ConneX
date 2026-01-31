package com.connex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GruvboxScheme = darkColorScheme(
    primary = GbOrange,
    onPrimary = GbBg,
    primaryContainer = GbOrangeDim,
    onPrimaryContainer = GbFg,
    
    secondary = GbYellow,
    onSecondary = GbBg,
    secondaryContainer = GbYellowDim,
    onSecondaryContainer = GbFg,

    tertiary = GbAqua,
    onTertiary = GbBg,
    tertiaryContainer = GbAquaDim,
    onTertiaryContainer = GbFg,

    background = GbBgHard,
    onBackground = GbFg,
    
    surface = GbBg,
    onSurface = GbFg,
    surfaceVariant = GbBg1,
    onSurfaceVariant = GbFg2,

    error = GbRed,
    onError = GbBg,
    errorContainer = GbRedDim,
    onErrorContainer = GbFg,

    outline = GbGray
)

@Composable
fun ConneXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GruvboxScheme,
        content = content
    )
}
