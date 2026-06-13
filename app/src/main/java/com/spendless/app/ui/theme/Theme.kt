package com.spendless.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalThemeStyle = staticCompositionLocalOf { "standard" }

// ── Dark Color Scheme (Nothing OS) ────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    primaryContainer = NothingGray200,
    onPrimaryContainer = NothingWhite,

    secondary = NothingGray700,
    onSecondary = NothingBlack,
    secondaryContainer = NothingGray200,
    onSecondaryContainer = NothingWhite,

    tertiary = NothingGray500,
    onTertiary = NothingWhite,
    tertiaryContainer = NothingGray150,
    onTertiaryContainer = NothingGray700,

    background = NothingBlack,
    onBackground = NothingWhite,

    surface = NothingGray50,
    onSurface = NothingWhite,
    surfaceVariant = NothingGray150,
    onSurfaceVariant = NothingGray700,

    surfaceTint = NothingWhite,

    inverseSurface = NothingWhite,
    inverseOnSurface = NothingBlack,
    inversePrimary = NothingBlack,

    outline = NothingGray300,
    outlineVariant = NothingGray200,

    error = Color(0xFFAAAAAA),
    onError = NothingBlack,
    errorContainer = NothingGray200,
    onErrorContainer = Color(0xFFEEEEEE),

    scrim = Color(0xCC000000)
)

// ── Light Color Scheme (Nothing OS Light Mode) ─────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = LightGray800,
    onPrimary = LightGray50,
    primaryContainer = LightGray200,
    onPrimaryContainer = LightGray800,

    secondary = LightGray600,
    onSecondary = LightGray50,
    secondaryContainer = LightGray200,
    onSecondaryContainer = LightGray700,

    tertiary = LightGray500,
    onTertiary = LightGray50,
    tertiaryContainer = LightGray150,
    onTertiaryContainer = LightGray700,

    background = LightGray50,
    onBackground = LightGray800,

    surface = LightGray100,
    onSurface = LightGray800,
    surfaceVariant = LightGray150,
    onSurfaceVariant = LightGray600,

    inverseSurface = LightGray800,
    inverseOnSurface = LightGray50,
    inversePrimary = LightGray50,

    outline = LightGray300,
    outlineVariant = LightGray200,

    error = Color(0xFF555555),
    onError = LightGray50,
    errorContainer = LightGray200,
    onErrorContainer = LightGray700
)

// ── Glass Dark Color Scheme (iOS Translucent Dark) ──────────────────────────────
private val GlassColorSchemeDark = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0x330A84FF),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF303030),
    onSecondary = Color.White,
    secondaryContainer = Color(0x1F767680),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFF8E8E93),
    onTertiary = Color.White,
    tertiaryContainer = Color(0x14FFFFFF),
    onTertiaryContainer = Color.White,

    background = Color(0xB3090A1A), // 70% opacity deep dark violet/blue
    onBackground = Color.White,

    surface = Color(0x26FFFFFF), // 15% opacity white for glass panels
    onSurface = Color.White,
    surfaceVariant = Color(0x14FFFFFF), // 8% opacity white
    onSurfaceVariant = Color(0xFFEBEBF5),

    surfaceTint = Color.White,

    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color.Black,

    outline = Color(0x33FFFFFF), // 20% white border
    outlineVariant = Color(0x1FFFFFFF), // 12% white border

    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0x33FF453A),
    onErrorContainer = Color.White,

    scrim = Color(0x99000000)
)

// ── Glass Light Color Scheme (iOS Translucent Light) ─────────────────────────────
private val GlassColorSchemeLight = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0x33007AFF),
    onPrimaryContainer = Color(0xFF007AFF),

    secondary = Color(0xFFE5E5EA),
    onSecondary = Color.Black,
    secondaryContainer = Color(0x1A000000),
    onSecondaryContainer = Color.Black,

    tertiary = Color(0xFF8E8E93),
    onTertiary = Color.White,
    tertiaryContainer = Color(0x0D000000),
    onTertiaryContainer = Color.Black,

    background = Color(0xB3F2F2F7), // 70% opacity light gray/blue tint
    onBackground = Color.Black,

    surface = Color(0xCCFFFFFF), // 80% opacity white for glass panels
    onSurface = Color.Black,
    surfaceVariant = Color(0x99FFFFFF), // 60% opacity white
    onSurfaceVariant = Color(0xFF3C3C43),

    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,

    outline = Color(0x33000000), // 20% black border
    outlineVariant = Color(0x1F000000), // 12% black border

    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0x33FF3B30),
    onErrorContainer = Color.Black
)

@Composable
fun SpendLessTheme(
    themeMode: String = "system",
    themeStyle: String = "standard",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val isDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark // "system" default
    }

    val colorScheme = if (themeStyle == "glass") {
        if (isDarkTheme) GlassColorSchemeDark else GlassColorSchemeLight
    } else {
        if (isDarkTheme) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper && context !is Activity) {
                context = context.baseContext
            }
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeStyle provides themeStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpendLessTypography,
            shapes = SpendLessShapes,
            content = content
        )
    }
}
