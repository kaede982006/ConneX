package com.connex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GruvboxScheme = darkColorScheme(
    primary = GruvboxBlue,
    onPrimary = GruvboxBgHard,
    primaryContainer = GruvboxBlueDim,
    onPrimaryContainer = GruvboxFg,
    secondary = GruvboxAqua,
    onSecondary = GruvboxBgHard,
    secondaryContainer = GruvboxAquaDim,
    onSecondaryContainer = GruvboxFg,
    tertiary = GruvboxPurple,
    onTertiary = GruvboxBgHard,
    tertiaryContainer = GruvboxPurpleDim,
    onTertiaryContainer = GruvboxFg,
    background = GruvboxBg,
    onBackground = GruvboxFg,
    surface = GruvboxBg1,
    onSurface = GruvboxFg1,
    surfaceVariant = GruvboxBg2,
    onSurfaceVariant = GruvboxFg2,
    error = GruvboxRed,
    onError = GruvboxFg,
    outline = GruvboxBg4,
    outlineVariant = GruvboxBg3
)

@Composable
fun ConneXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GruvboxScheme,
        typography = Typography,
        content = content
    )
}
