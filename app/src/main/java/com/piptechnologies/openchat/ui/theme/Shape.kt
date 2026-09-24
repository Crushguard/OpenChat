package com.piptechnologies.openchat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Design radii: 10 · 14 · 20 · pill, plus the 6 inner radius of the split button, 16 cards, 24/26 sheets. */
object OcRadius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val card = 16.dp
    val lg = 20.dp
    val sheet = 24.dp
    val dialog = 26.dp
    val pill = 999.dp
}

val OcShapes = Shapes(
    extraSmall = RoundedCornerShape(OcRadius.xs),
    small = RoundedCornerShape(OcRadius.sm),
    medium = RoundedCornerShape(OcRadius.md),
    large = RoundedCornerShape(OcRadius.lg),
    extraLarge = RoundedCornerShape(OcRadius.dialog),
)
