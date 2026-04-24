package com.visionassist.eyetest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackBlueScheme = lightColorScheme(
    primary = AppBrandRed,
    onPrimary = AppWhite,
    primaryContainer = AppBrandRedSoftStrong,
    onPrimaryContainer = AppBlack,
    secondary = AppBlack,
    onSecondary = AppWhite,
    secondaryContainer = AppBrandRedSoft,
    onSecondaryContainer = AppBlack,
    tertiary = AppBrandRedDark,
    onTertiary = AppWhite,
    background = AppWhite,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppBrandRedSoft,
    onSurfaceVariant = AppOnSurfaceMuted,
    outline = AppOutline,
    outlineVariant = AppBrandRedSoftStrong,
    error = Color(0xFFB00020),
    onError = AppWhite
)

@Composable
fun VisionAssistTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlackBlueScheme,
        typography = Typography,
        content = content
    )
}
