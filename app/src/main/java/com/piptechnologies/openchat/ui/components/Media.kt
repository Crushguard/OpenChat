package com.piptechnologies.openchat.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import com.piptechnologies.openchat.ui.theme.OcTheme
import java.io.File
import kotlin.math.sqrt

// The design's hatch colours: repeating-linear-gradient(135deg, band 0 w, base w 2w).
private val LightBand = Color(0xFFEEF1F5)
private val LightBase = Color(0xFFF6F7F9)
private val DarkBand = Color(0xFF262A33)
private val DarkBase = Color(0xFF1E2128)
private val Sqrt2 = sqrt(2f)

/** Diagonal hatch placeholder like the design's media tiles: light (#EEF1F5/#F6F7F9, 6 px bands at 135°) or dark (#262A33/#1E2128, 8 px bands). Optional centred mono 9.5/500 .06em label in placeholder colour (light) or muted (dark). */
@Composable
fun HatchedPlaceholder(modifier: Modifier = Modifier, dark: Boolean = false, label: String? = null) {
    val c = OcTheme.colors
    Box(
        modifier = modifier.drawBehind {
            drawHatch(
                base = if (dark) DarkBase else LightBase,
                band = if (dark) DarkBand else LightBand,
                bandWidth = (if (dark) 8.dp else 6.dp).toPx(),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(
                text = label,
                style = OcTheme.type.tileTime9_5.copy(letterSpacing = 0.06.em),
                color = if (dark) c.muted else c.placeholder,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/**
 * Fills the area with [base], then draws [band] stripes [bandWidth] wide every 2 × [bandWidth], at
 * 135° like the CSS gradient: bands run along x + y = const and the first one starts in the top-left
 * corner.
 */
private fun DrawScope.drawHatch(base: Color, band: Color, bandWidth: Float) {
    drawRect(color = base)
    if (bandWidth <= 0f) return
    val width = size.width
    val height = size.height
    val step = 2f * bandWidth * Sqrt2 // x + y distance between band centre lines
    val first = bandWidth / 2f * Sqrt2 // x + y of the first band's centre line
    clipRect {
        var offset = first
        while (offset - first <= width + height) {
            drawLine(
                color = band,
                start = Offset(offset + bandWidth, -bandWidth),
                end = Offset(offset - height - bandWidth, height + bandWidth),
                strokeWidth = bandWidth,
            )
            offset += step
        }
    }
}

/** Coil AsyncImage of File(localPath) with ContentScale.Crop over a HatchedPlaceholder; null path renders only the placeholder (screenshots pass null). */
@Composable
fun LocalMediaThumbnail(localPath: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier = modifier) {
        HatchedPlaceholder(modifier = Modifier.matchParentSize())
        if (localPath != null) {
            val context = LocalContext.current
            val imageLoader = remember(context) { ThumbnailImageLoader.get(context) }
            val file = remember(localPath) { File(localPath) }
            AsyncImage(
                model = file,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
            )
        }
    }
}

/**
 * The app's Coil loader plus a video frame decoder, so video copies get a thumbnail too. Built once
 * with [ImageLoader.newBuilder], which shares the default loader's memory and disk caches.
 */
private object ThumbnailImageLoader {
    @Volatile
    private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        loader ?: synchronized(this) {
            loader ?: context.applicationContext.imageLoader.newBuilder()
                .components { add(VideoFrameDecoder.Factory()) }
                .build()
                .also { loader = it }
        }
}
