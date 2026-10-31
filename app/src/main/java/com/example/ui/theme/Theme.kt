package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = FreshBluePrimaryDark,
    onPrimary = FreshBlueOnPrimaryDark,
    primaryContainer = FreshBlueContainerDark,
    onPrimaryContainer = FreshBlueOnContainerDark,
    secondary = WarmOrangeSecondaryDark,
    onSecondary = WarmOrangeOnSecondaryDark,
    secondaryContainer = WarmOrangeContainerDark,
    onSecondaryContainer = WarmOrangeOnContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = TealOnTertiaryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = FreshBluePrimary,
    onPrimary = FreshBlueOnPrimary,
    primaryContainer = FreshBlueContainer,
    onPrimaryContainer = FreshBlueOnContainer,
    secondary = WarmOrangeSecondary,
    onSecondary = WarmOrangeOnSecondary,
    secondaryContainer = WarmOrangeContainer,
    onSecondaryContainer = WarmOrangeOnContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun MakanMinumTheme(
    themeMode: ThemeMode = ThemeMode.SISTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SISTEM -> isSystemInDarkTheme()
        ThemeMode.TERANG -> false
        ThemeMode.GELAP -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
