package com.piptechnologies.openchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val Tokens = OcColors()

/** Material 3 scheme derived from the design tokens. Green is the one action colour; amber is attention; red stays destructive. */
val OcLightColorScheme = lightColorScheme(
    primary = Tokens.green,
    onPrimary = Color.White,
    primaryContainer = Tokens.greenTint,
    onPrimaryContainer = Tokens.green,
    inversePrimary = Tokens.greenSoft,
    secondary = Tokens.ink2,
    onSecondary = Color.White,
    secondaryContainer = Tokens.subtle,
    onSecondaryContainer = Tokens.ink,
    tertiary = Tokens.amber,
    onTertiary = Color.White,
    tertiaryContainer = Tokens.amberTint,
    onTertiaryContainer = Tokens.amber,
    background = Tokens.canvas,
    onBackground = Tokens.ink,
    surface = Tokens.surface,
    onSurface = Tokens.ink,
    surfaceVariant = Tokens.subtle,
    onSurfaceVariant = Tokens.ink2,
    surfaceTint = Color.Transparent,
    inverseSurface = Tokens.ink,
    inverseOnSurface = Color.White,
    error = Tokens.destructive,
    onError = Color.White,
    errorContainer = Tokens.destructiveTint,
    onErrorContainer = Tokens.destructive,
    outline = Tokens.border,
    outlineVariant = Tokens.hairline,
    scrim = Color(0xFF14161C),
    surfaceBright = Tokens.surface,
    surfaceDim = Tokens.subtle,
    surfaceContainer = Tokens.subtle,
    surfaceContainerHigh = Tokens.borderSoft,
    surfaceContainerHighest = Tokens.border,
    surfaceContainerLow = Tokens.subtle2,
    surfaceContainerLowest = Tokens.surface,
)

@Composable
fun OpenChatTheme(content: @Composable () -> Unit) {
    val colors = Tokens
    val type = OcTypography()
    CompositionLocalProvider(
        LocalOcColors provides colors,
        LocalOcTypography provides type,
    ) {
        MaterialTheme(
            colorScheme = OcLightColorScheme,
            typography = m3Typography(type),
            shapes = OcShapes,
            content = content,
        )
    }
}

/** Accessors for the design tokens: `OcTheme.colors.green`, `OcTheme.type.title16`. */
object OcTheme {
    val colors: OcColors
        @Composable @ReadOnlyComposable get() = LocalOcColors.current
    val type: OcTypography
        @Composable @ReadOnlyComposable get() = LocalOcTypography.current
}
