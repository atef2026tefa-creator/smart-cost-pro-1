package com.aistudio.smartcostpro.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape scale tuned for the app.
 * Cards use a slightly larger radius, and dialog / bottom-sheet shapes are
 * generous to keep the financial UI feeling approachable on small phones.
 */
val CostAppProShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object CustomShapes {
    val CardShape = RoundedCornerShape(16.dp)
    val PillShape = RoundedCornerShape(percent = 50)
    val DialogShape = RoundedCornerShape(24.dp)
    val SectionChipShape = RoundedCornerShape(20.dp)
    val AmountBadgeShape = RoundedCornerShape(12.dp)
}
