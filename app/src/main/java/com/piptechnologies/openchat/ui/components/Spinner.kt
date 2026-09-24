package com.piptechnologies.openchat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * The design's spinner: a [size] ring of [stroke] in [color] with its right quarter open, turning once
 * every 0.9 s. Drawn by hand rather than with M3's indeterminate indicator so a screenshot (frame 0)
 * shows the ring as the design does.
 */
@Composable
fun OcSpinner(size: Dp, color: Color = OcTheme.colors.green, stroke: Dp = 2.dp) {
    val transition = rememberInfiniteTransition(label = "OcSpinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = LinearEasing)),
        label = "OcSpinnerRotation",
    )
    Canvas(
        modifier = Modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        val strokePx = stroke.toPx()
        val inset = strokePx / 2f
        rotate(degrees = rotation) {
            // Open from -45° to 45° (the transparent right border of the design's CSS ring).
            drawArc(
                color = color,
                startAngle = 45f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - strokePx, this.size.height - strokePx),
                style = Stroke(width = strokePx),
            )
        }
    }
}
