package com.piptechnologies.openchat.ui.media

import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.imageLoader
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme
import java.io.File
import java.io.IOException
import java.util.Locale

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * The media detail preview (design map §4.13), drawn on the ink panel. Photos and stickers are a pinch- and
 * double-tap-zoomable image (Coil, fit). Videos play inline in a [VideoView] with the system
 * [MediaController]. Audio is a play/pause button over the file name and size, with one [MediaPlayer] that is
 * released when the preview leaves. Documents show the file icon, name and size.
 */
@Composable
fun MediaPreview(media: RecoveredMedia, modifier: Modifier) {
    when (media.category) {
        MediaCategory.PHOTO, MediaCategory.STICKER ->
            ZoomableImage(path = media.localPath, description = media.displayName, modifier = modifier)
        MediaCategory.VIDEO -> VideoPreview(path = media.localPath, modifier = modifier)
        MediaCategory.AUDIO -> AudioPreview(media = media, modifier = modifier)
        MediaCategory.DOCUMENT -> DocumentPreview(media = media, modifier = modifier)
    }
}

/** Fit to the panel; pinch zooms (1x to 5x) and pans within the zoomed image, double tap toggles 2.5x. */
@Composable
private fun ZoomableImage(path: String, description: String, modifier: Modifier) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    var failed by remember(path) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(path) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(path) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                    val maxX = size.width * (nextScale - 1f) / 2f
                    val maxY = size.height * (nextScale - 1f) / 2f
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                        y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                    )
                    scale = nextScale
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            contentDescription = description,
            imageLoader = context.imageLoader,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            onError = { failed = true },
            contentScale = ContentScale.Fit,
        )
        if (failed) {
            LucideIconImage(icon = LucideIcon.ImageOff, size = 32.dp, tint = OcTheme.colors.muted)
        }
    }
}

/** The VideoView the factory made, for the play button and for release. */
private class VideoHandle {
    var view: VideoView? = null
    var controller: MediaController? = null
}

/**
 * Inline [VideoView] showing its first frame, with a play button until playback starts (again after it
 * ends); tapping the video shows the system controller. The player is stopped when the view is released.
 */
@Composable
private fun VideoPreview(path: String, modifier: Modifier) {
    key(path) {
        val handle = remember { VideoHandle() }
        var started by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            if (failed) {
                LucideIconImage(icon = LucideIcon.Film, size = 32.dp, tint = OcTheme.colors.muted)
            } else {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            val controller = MediaController(context)
                            setMediaController(controller)
                            // Without a seek the surface stays black until playback starts.
                            setOnPreparedListener { seekTo(1) }
                            setOnCompletionListener { started = false }
                            // Returning true keeps VideoView from showing its own error dialog.
                            setOnErrorListener { _, _, _ ->
                                failed = true
                                true
                            }
                            setVideoPath(path)
                            handle.view = this
                            handle.controller = controller
                        }
                    },
                    onRelease = { view ->
                        handle.controller?.hide()
                        view.stopPlayback()
                        handle.view = null
                        handle.controller = null
                    },
                )
                if (!started) {
                    PlayPauseButton(
                        playing = false,
                        onClick = {
                            handle.view?.start()
                            started = true
                        },
                    )
                }
            }
        }
    }
}

/** Play/pause over the file name and size; the [MediaPlayer] is released in [DisposableEffect]. */
@Composable
private fun AudioPreview(media: RecoveredMedia, modifier: Modifier) {
    var playing by remember(media.localPath) { mutableStateOf(false) }
    val playback = remember(media.localPath) { AudioPlayback(media.localPath) }
    DisposableEffect(playback) {
        playback.onPlayingChanged = { playing = it }
        onDispose {
            playback.onPlayingChanged = {}
            playback.release()
        }
    }
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayPauseButton(playing = playing, onClick = { playback.toggle() })
        Spacer(Modifier.height(16.dp))
        FileCaption(name = media.displayName, sizeBytes = media.sizeBytes)
    }
}

/** File icon over the name and size; opening the file is left to Share. */
@Composable
private fun DocumentPreview(media: RecoveredMedia, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LucideIconImage(icon = LucideIcon.FileText, size = 44.dp, tint = Color.White)
        Spacer(Modifier.height(16.dp))
        FileCaption(name = media.displayName, sizeBytes = media.sizeBytes)
    }
}

/** 64 dp green circle with a filled play or pause 26 in white. */
@Composable
private fun PlayPauseButton(playing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(OcTheme.colors.green)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIconImage(
            icon = if (playing) LucideIcon.Pause else LucideIcon.Play,
            size = 26.dp,
            tint = Color.White,
            filled = true,
            contentDescription = stringResource(if (playing) R.string.media_pause else R.string.media_play),
        )
    }
}

/** File name (14/600 white, up to 3 lines) over its size (mono 12, muted). */
@Composable
private fun FileCaption(name: String, sizeBytes: Long) {
    Text(
        text = name,
        style = OcTheme.type.label14,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(4.dp))
    Text(text = fileSize(sizeBytes), style = OcTheme.type.mono12, color = OcTheme.colors.muted)
}

/** "284 KB", "3.5 MB": decimal units, as Android's short file sizes. */
private fun fileSize(bytes: Long): String = when {
    bytes < 1_000L -> "$bytes B"
    bytes < 1_000_000L -> "${bytes / 1_000L} KB"
    bytes < 1_000_000_000L -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
    else -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
}

/**
 * One [MediaPlayer] for one audio file, created and prepared (asynchronously) on the first play; its callbacks
 * arrive on the main thread, which created it. [release] frees it.
 */
private class AudioPlayback(private val path: String) {
    var onPlayingChanged: (Boolean) -> Unit = {}
    private var player: MediaPlayer? = null
    private var prepared = false

    fun toggle() {
        val current = player
        when {
            current == null -> prepareAndPlay()
            !prepared -> Unit // Still preparing: it starts on its own when ready.
            current.isPlaying -> {
                current.pause()
                onPlayingChanged(false)
            }
            else -> {
                current.start()
                onPlayingChanged(true)
            }
        }
    }

    fun release() {
        player?.release()
        player = null
        prepared = false
    }

    private fun prepareAndPlay() {
        val created = MediaPlayer()
        player = created
        try {
            created.setOnPreparedListener { ready ->
                prepared = true
                ready.start()
                onPlayingChanged(true)
            }
            created.setOnCompletionListener { onPlayingChanged(false) }
            created.setOnErrorListener { _, _, _ ->
                release()
                onPlayingChanged(false)
                true
            }
            created.setDataSource(path)
            created.prepareAsync()
        } catch (e: IOException) {
            release()
        } catch (e: RuntimeException) {
            // IllegalArgumentException, IllegalStateException, SecurityException: this file cannot be played.
            release()
        }
    }
}
