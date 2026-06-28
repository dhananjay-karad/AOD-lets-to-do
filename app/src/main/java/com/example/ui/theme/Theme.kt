package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AmberNeon,
    secondary = GoldAccent,
    tertiary = ElectricTeal,
    background = OnyxBlack,
    surface = MatteCharcoal,
    surfaceVariant = SlateGray,
    onPrimary = ElectricTeal,
    onSecondary = ElectricTeal,
    onTertiary = OnyxBlack,
    onBackground = IceWhite,
    onSurface = IceWhite,
    onSurfaceVariant = MutedGray,
    error = CyberRed,
    outline = BorderGray
)

private val LightColorScheme = lightColorScheme(
    primary = AmberNeonDim,
    secondary = SlateGray,
    tertiary = ElectricBlue,
    background = IceWhite,
    surface = IceWhite,
    surfaceVariant = IceWhite,
    onPrimary = OnyxBlack,
    onSecondary = IceWhite,
    onTertiary = IceWhite,
    onBackground = OnyxBlack,
    onSurface = OnyxBlack,
    onSurfaceVariant = MutedGray,
    error = CyberRed,
    outline = BorderGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark mode for optimal AOD battery friendly theme
    dynamicColor: Boolean = false, // We use custom theme to maintain consistent high-contrast branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
