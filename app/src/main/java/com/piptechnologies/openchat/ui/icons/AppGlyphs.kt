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
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Third-party marks as line glyphs in our colours, exactly as the design draws them:
 * phone-in-bubble (WhatsApp), B-in-bubble (WhatsApp Business), paper plane (Telegram).
 * No trademarked logos are bundled; the installed app's own launcher icon is used when available.
 */
enum class AppGlyph { WhatsApp, WhatsAppBusiness, Telegram }

private const val BUBBLE = "M7.9 20A9 9 0 1 0 4 16.1L2 22Z"
private const val PHONE =
    "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"
private const val LETTER_B = "M9.5 8v8M9.5 8h2.5a2 2 0 0 1 0 4H9.5M9.5 12h3a2 2 0 0 1 0 4H9.5"

private val whatsAppVector: ImageVector by lazy {
    ImageVector.Builder(name = "glyph-whatsapp", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            addPath(
                pathData = PathParser().parsePathString(BUBBLE).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            group(translationX = 6.5f, translationY = 6.5f, scaleX = 0.46f, scaleY = 0.46f) {
                addPath(
                    pathData = PathParser().parsePathString(PHONE).toNodes(),
                    fill = SolidColor(Color.Black),
                )
            }
        }
        .build()
}

private val whatsAppBusinessVector: ImageVector by lazy {
    ImageVector.Builder(name = "glyph-whatsapp-business", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            addPath(
                pathData = PathParser().parsePathString(BUBBLE).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                pathData = PathParser().parsePathString(LETTER_B).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        .build()
}

fun AppGlyph.vector(): ImageVector = when (this) {
    AppGlyph.WhatsApp -> whatsAppVector
    AppGlyph.WhatsAppBusiness -> whatsAppBusinessVector
    AppGlyph.Telegram -> LucideIcon.Send.vector(strokeWidth = 1.9f)
}

@Composable
fun AppGlyphImage(glyph: AppGlyph, size: Dp, tint: Color, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val vector = remember(glyph) { glyph.vector() }
    Icon(imageVector = vector, contentDescription = contentDescription, tint = tint, modifier = modifier.size(size))
}
