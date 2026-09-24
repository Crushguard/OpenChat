package com.piptechnologies.openchat.ui.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/** The design's default Lucide stroke width (the SVGs ship with 2). */
const val LUCIDE_STROKE = 1.75f

private val vectorCache = ConcurrentHashMap<String, ImageVector>()

/**
 * The Lucide icons that point along the reading direction (back and forward arrows, chevrons, the
 * external-link and log-out arrows, the send planes, the backspace-shaped delete): in a right-to-left
 * layout they are drawn mirrored. Up/down arrows, the circular rotate/refresh arrows (a rotation, not a
 * direction; Material does not mirror refresh either), media controls and symbols stay as drawn.
 * arrow-right has no generated entry yet; it is listed so it mirrors once it is added.
 */
private val MIRRORED_IN_RTL: Set<String> = setOf(
    "arrow-left",
    "arrow-right",
    "arrow-up-right",
    "chevron-left",
    "chevron-right",
    "delete",
    "external-link",
    "log-out",
    "send",
    "send-horizontal",
)

/** Whether this icon points along the reading direction, so [LucideIconImage] mirrors it in right-to-left layouts. */
val LucideIcon.mirrorsInRtl: Boolean get() = iconName in MIRRORED_IN_RTL

/**
 * Builds (and caches) this icon as an [ImageVector]. [strokeWidth] is in viewport units of the
 * 24x24 Lucide grid, so 1.75 here matches `stroke-width="1.75"` in the design. Tint it through
 * [Icon]'s `tint`, which colours both strokes and fills. [autoMirror] makes it draw mirrored in a
 * right-to-left layout; it is off here so brand glyphs built from Lucide paths (AppGlyphs' Telegram
 * plane) keep their orientation, and [LucideIconImage] turns it on for [mirrorsInRtl] icons.
 */
fun LucideIcon.vector(strokeWidth: Float = LUCIDE_STROKE, filled: Boolean = false, autoMirror: Boolean = false): ImageVector =
    vectorCache.getOrPut("$iconName/$strokeWidth/$filled/$autoMirror") {
        val builder = ImageVector.Builder(
            name = iconName,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        )
        for (d in paths) {
            builder.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        builder.build()
    }

/**
 * A Lucide icon drawn at [size] in [tint]. Directional icons ([mirrorsInRtl]) are mirrored in a
 * right-to-left layout; pass [autoMirror] = false where the drawing must keep its orientation (a logo).
 */
@Composable
fun LucideIconImage(
    icon: LucideIcon,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = LUCIDE_STROKE,
    filled: Boolean = false,
    contentDescription: String? = null,
    autoMirror: Boolean = icon.mirrorsInRtl,
) {
    val vector = remember(icon, strokeWidth, filled, autoMirror) { icon.vector(strokeWidth, filled, autoMirror) }
    Icon(
        imageVector = vector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
