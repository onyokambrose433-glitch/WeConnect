package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val WeConnectLightColors = lightColorScheme(
    primary = WhatsAppTeal,
    onPrimary = PureWhite,
    primaryContainer = WhatsAppNavIndicatorLight,
    onPrimaryContainer = WhatsAppInkDark,
    secondary = WhatsAppEmerald,
    onSecondary = PureWhite,
    secondaryContainer = WhatsAppLightSurfaceVariant,
    onSecondaryContainer = WhatsAppInkDark,
    tertiary = WhatsAppGreen,
    onTertiary = PureWhite,
    tertiaryContainer = WhatsAppOutgoingBubbleLight,
    onTertiaryContainer = WhatsAppTeal,
    background = WhatsAppLightBg,
    onBackground = WhatsAppInkDark,
    surface = WhatsAppLightBg,
    onSurface = WhatsAppInkDark,
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onSurfaceVariant = WhatsAppMutedGray,
    outline = WhatsAppLightBorder,
    outlineVariant = WhatsAppLightBorder,
    error = WhatsAppCallRed,
    onError = PureWhite,
    errorContainer = WhatsAppErrorBgLight,
    onErrorContainer = WhatsAppCallRed
)

private val WeConnectDarkColors = darkColorScheme(
    primary = WhatsAppDarkAccent,
    onPrimary = WhatsAppDarkBg,
    primaryContainer = WhatsAppNavIndicatorDark,
    onPrimaryContainer = WhatsAppDarkText,
    secondary = WhatsAppGreen,
    onSecondary = WhatsAppDarkBg,
    secondaryContainer = WhatsAppDarkElevated,
    onSecondaryContainer = WhatsAppDarkText,
    tertiary = WhatsAppTeal,
    onTertiary = PureWhite,
    tertiaryContainer = WhatsAppDarkTeal,
    onTertiaryContainer = WhatsAppDarkText,
    background = WhatsAppDarkBg,
    onBackground = WhatsAppDarkText,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppDarkText,
    surfaceVariant = WhatsAppDarkElevated,
    onSurfaceVariant = WhatsAppDarkMuted,
    outline = WhatsAppDarkBorder,
    outlineVariant = WhatsAppDarkBorder,
    error = WhatsAppCallRed,
    onError = PureWhite,
    errorContainer = WhatsAppErrorBgDark,
    onErrorContainer = WhatsAppDarkText
)

val WeConnectShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun WeConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WeConnectDarkColors else WeConnectLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WeConnectTypography,
        shapes = WeConnectShapes,
        content = content
    )
}
