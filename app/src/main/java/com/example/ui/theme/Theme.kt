package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val MenchColorScheme = darkColorScheme(
    primary = TempleGoldBright,
    onPrimary = TempleIndigoDark,
    primaryContainer = TempleIndigoCard,
    onPrimaryContainer = TempleCreamText,
    secondary = BluetoothBlue,
    onSecondary = TempleIndigoDark,
    secondaryContainer = Color(0xFF1E3A6B),
    onSecondaryContainer = Color(0xFFD6F1FF),
    tertiary = TeamGreenAccent,
    onTertiary = Color.White,
    background = TempleIndigoDark,
    onBackground = TempleCreamText,
    surface = TempleIndigoSurface,
    onSurface = TempleCreamText,
    surfaceVariant = TempleIndigoCard,
    onSurfaceVariant = TempleMutedLavender,
    error = CoralDanger,
    onError = Color.White
)

@Composable
fun MenchBluetoothTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = MenchColorScheme,
            typography = MenchTypography,
            content = content
        )
    }
}
