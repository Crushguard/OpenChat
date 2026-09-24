package com.piptechnologies.openchat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** How long a toast stays up (design map §4.21). */
private const val TOAST_DURATION_MS = 2_200L

/** rgb(20,30,60): the toast's shadow tint in the design. */
private val ToastShadow = Color(0xFF141E3C)

/** Holds the dark toast's text. [show] replaces whatever is showing and restarts the timer, even for the same text. */
@Stable
class ToastHostState {
    private val _message = MutableStateFlow<String?>(null)
    private val _serial = MutableStateFlow(0L)

    /** The text on screen, or null when no toast is showing. */
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Bumped by every [show], so showing the same text again restarts the 2.2 s timer. */
    internal val serial: StateFlow<Long> = _serial.asStateFlow()

    /** Shows [text] in place of the previous message. */
    fun show(text: String) {
        _serial.update { it + 1 }
        _message.value = text
    }

    /** Hides the toast unless something newer was shown after [shownSerial]. */
    internal fun hide(shownSerial: Long) {
        if (_serial.value == shownSerial) _message.value = null
    }
}

@Composable
fun rememberToastHostState(): ToastHostState = remember { ToastHostState() }

/**
 * The app-level toast host, provided by AppRoot above the NavHost so a toast outlives the route that showed it
 * (design map §4.21: the toast shows over any screen). Null where nothing provides it (screenshot tests); routes
 * then fall back to their own host: `LocalToastHost.current ?: rememberToastHostState()`.
 */
val LocalToastHost = staticCompositionLocalOf<ToastHostState?> { null }

/** Dark toast: ink background, white 13.5/600, check 16 (sw 2.4) greenSoft, 12/16 padding, 13 radius, 90 dp above the bottom, auto-hides after 2200 ms. Place inside a Box over the screen. */
@Composable
fun DarkToastHost(state: ToastHostState, modifier: Modifier = Modifier) {
    val message by state.message.collectAsState()
    val serial by state.serial.collectAsState()
    // Keeps the text on the toast while it fades out after the message is cleared.
    var lastText by remember { mutableStateOf("") }
    val shownMessage = message
    val shownSerial = serial
    LaunchedEffect(shownSerial, shownMessage) {
        if (shownMessage == null) return@LaunchedEffect
        lastText = shownMessage
        delay(TOAST_DURATION_MS)
        state.hide(shownSerial)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 90.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = shownMessage != null,
            enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(animationSpec = tween(200), initialOffsetY = { it / 5 }),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            DarkToastVisual(text = shownMessage ?: lastText)
        }
    }
}

/** The toast itself, static; screenshots place it 90 dp above the bottom as [DarkToastHost] does. */
@Composable
fun DarkToastVisual(text: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = shape, ambientColor = Color.Transparent, spotColor = ToastShadow)
            .background(c.ink, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        LucideIconImage(icon = LucideIcon.Check, size = 16.dp, tint = c.greenSoft, strokeWidth = 2.4f)
        Text(text = text, style = OcTheme.type.label13_5, color = Color.White)
    }
}
