package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HomeverseDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ObsidianBg,
    primaryContainer = Color(0xFF003840),
    onPrimaryContainer = CyberCyan,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF152A50),
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = ObsidianBg,
    onBackground = ArcticWhite,
    surface = SurfaceDark,
    onSurface = ArcticWhite,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = SlateMuted,
    outline = SurfaceBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun HomeverseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HomeverseDarkColorScheme,
        typography = Typography,
        content = content
    )
}
