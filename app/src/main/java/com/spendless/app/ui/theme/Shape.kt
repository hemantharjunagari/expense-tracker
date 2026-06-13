package com.spendless.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Nothing OS uses clean, large radius shapes with some asymmetry.
 */
val SpendLessShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom shape constants
val CardShape = RoundedCornerShape(20.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ChipShape = RoundedCornerShape(50)  // Pill shape
val ButtonShape = RoundedCornerShape(16.dp)
val SmallCardShape = RoundedCornerShape(12.dp)
val CircularShape = RoundedCornerShape(50)
