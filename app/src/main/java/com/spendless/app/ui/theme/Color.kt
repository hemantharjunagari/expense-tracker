package com.spendless.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Nothing OS Inspired Color Palette ─────────────────────────────────────────

// Pure blacks and whites
val NothingBlack = Color(0xFF000000)
val NothingWhite = Color(0xFFFFFFFF)

// Dark theme grays (surfaces)
val NothingGray50  = Color(0xFF0A0A0A)   // Near-black, primary background
val NothingGray100 = Color(0xFF111111)   // Slightly lifted surface
val NothingGray150 = Color(0xFF1A1A1A)   // Card backgrounds
val NothingGray200 = Color(0xFF222222)   // Elevated card
val NothingGray250 = Color(0xFF2A2A2A)   // Bottom sheet, dialogs
val NothingGray300 = Color(0xFF3A3A3A)   // Dividers, borders
val NothingGray400 = Color(0xFF606060)   // Muted icons
val NothingGray500 = Color(0xFF8A8A8A)   // Placeholder text
val NothingGray600 = Color(0xFFB0B0B0)   // Secondary text
val NothingGray700 = Color(0xFFD0D0D0)   // Tertiary text, subtitles
val NothingGray800 = Color(0xFFE8E8E8)   // Off-white accent / primary text dim

// Light theme grays
val LightGray50  = Color(0xFFFFFFFF)
val LightGray100 = Color(0xFFF8F8F8)
val LightGray150 = Color(0xFFF0F0F0)
val LightGray200 = Color(0xFFE8E8E8)
val LightGray300 = Color(0xFFD0D0D0)
val LightGray400 = Color(0xFFAAAAAA)
val LightGray500 = Color(0xFF888888)
val LightGray600 = Color(0xFF555555)
val LightGray700 = Color(0xFF333333)
val LightGray800 = Color(0xFF111111)

// Accent colors — monochrome with subtle warmth
val AccentPrimary    = Color(0xFFFFFFFF)  // White on dark
val AccentSecondary  = Color(0xFFE0E0E0)  // Soft white
val AccentTertiary   = Color(0xFFC0C0C0)  // Silver

// Nothing dot-matrix accent
val DotMatrixColor   = Color(0xFF3A3A3A)  // Subtle dot grid
val DotMatrixLight   = Color(0xFFD8D8D8)  // Light theme dots

// Status/semantic colors — desaturated for Nothing aesthetic
val ColorPositive    = Color(0xFFCCCCCC)  // Light gray for income/positive
val ColorNegative    = Color(0xFF888888)  // Muted gray for expenses
val ColorWarning     = Color(0xFFAAAAAA)  // Warning (gray toned)
val ColorError       = Color(0xFF9E9E9E)  // Error in monochrome

// Progress ring colors — concentric grays
val RingBackground   = Color(0xFF1E1E1E)  // Ring track (dark)
val RingForeground   = Color(0xFFFFFFFF)  // Progress ring (white)
val RingForegroundDim= Color(0xFF666666)  // Completed/dim ring

// Glass effect
val GlassBackground  = Color(0x14FFFFFF) // 8% white
val GlassBorder      = Color(0x1FFFFFFF) // 12% white border

// Chart colors (monochrome spectrum)
val ChartColors = listOf(
    Color(0xFFFFFFFF),  // White
    Color(0xFFCCCCCC),  // Light gray
    Color(0xFF999999),  // Medium gray
    Color(0xFF666666),  // Dark gray
    Color(0xFF444444),  // Very dark gray
    Color(0xFFE0E0E0),  // Off-white
    Color(0xFFB0B0B0),  // Silver
    Color(0xFF808080),  // Mid gray
    Color(0xFF505050),  // Charcoal
    Color(0xFF303030),  // Near black
)
