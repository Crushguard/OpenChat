package com.piptechnologies.openchat.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.PhoneNumberNormalizer
import com.piptechnologies.openchat.core.send.RecentNumber
import com.piptechnologies.openchat.ui.components.CardColumn
import com.piptechnologies.openchat.ui.components.HairlineDivider
import com.piptechnologies.openchat.ui.components.SectionEyebrow
import com.piptechnologies.openchat.ui.components.ltr
import com.piptechnologies.openchat.ui.components.rememberTimeFormatter
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme
import kotlin.math.roundToInt

/** How far a recent row slides to uncover its Delete panel. */
private val RevealWidth = 96.dp

/** A release faster than this towards the start reveals the panel even when dragged less than half way. */
private val RevealFlingVelocity = 400.dp

/**
 * "Recent" eyebrow and the card of numbers (design map §4.3), newest first. Rows are 52 high: app
 * glyph 20, "+62 812 3456 7890" in mono 14.5 (left to right in every language), time label in mono
 * 11.5 ("2h", "Yesterday") measured from [nowMs] and worded in the UI language. The row [revealedId]
 * is slid 96 dp towards the start over its Delete panel.
 */
@Composable
internal fun RecentsSection(
    recents: List<RecentNumber>,
    nowMs: Long,
    revealedId: Long?,
    onTap: (RecentNumber) -> Unit,
    onReveal: (Long?) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = rememberTimeFormatter()
    Column(modifier = modifier.fillMaxWidth()) {
        SectionEyebrow(text = stringResource(R.string.home_recent))
        Spacer(Modifier.height(8.dp))
        CardColumn {
            recents.forEachIndexed { index, recent ->
                if (index > 0) HairlineDivider()
                key(recent.id) {
                    RecentRow(
                        recent = recent,
                        timeLabel = time.label(recent.usedAt, nowMs),
                        revealed = recent.id == revealedId,
                        onTap = onTap,
                        onReveal = onReveal,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

/**
 * One recent number over its Delete panel. Tap refills Home ([onTap]); long-press, or a swipe from
 * end to start past half the panel (or flung), reveals the panel ([onReveal] with the id). While
 * revealed, a tap on the row or a swipe back closes it ([onReveal] with null) and a tap on the
 * uncovered "Delete" removes the number ([onDelete]). The slide follows [revealed], animated.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    recent: RecentNumber,
    timeLabel: String,
    revealed: Boolean,
    onTap: (RecentNumber) -> Unit,
    onReveal: (Long?) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val c = OcTheme.colors
    val density = LocalDensity.current
    val revealPx = with(density) { RevealWidth.toPx() }
    val flingPx = with(density) { RevealFlingVelocity.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentRevealed by rememberUpdatedState(revealed)

    // Offset of the row towards the end: 0 closed, -revealPx open. Mirrored by Modifier.offset in RTL.
    val settled = remember { Animatable(if (revealed) -revealPx else 0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // Bumped when a drag ends, so the row settles even when the revealed state itself did not change.
    var settleRequests by remember { mutableIntStateOf(0) }
    LaunchedEffect(revealed, revealPx, settleRequests) {
        settled.animateTo(if (revealed) -revealPx else 0f)
    }
    val dragState = rememberDraggableState { delta ->
        dragOffset = (dragOffset + delta).coerceIn(-revealPx, 0f)
    }
    // The panel exists only while any of it can show, so a closed row has no hidden "Delete" for TalkBack.
    val panelShown by remember { derivedStateOf { dragging || settled.value != 0f } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (revealed || panelShown) {
            DeletePanel(onClick = { onDelete(recent.id) }, modifier = Modifier.matchParentSize())
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset((if (dragging) dragOffset else settled.value).roundToInt(), 0) }
                .background(c.surface)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    reverseDirection = rtl,
                    onDragStarted = {
                        dragOffset = settled.value
                        dragging = true
                    },
                    onDragStopped = { velocity ->
                        val reveal = dragOffset < -revealPx / 2f || velocity < -flingPx
                        settled.snapTo(dragOffset)
                        dragging = false
                        if (reveal != currentRevealed) onReveal(if (reveal) recent.id else null)
                        settleRequests++
                    },
                )
                .combinedClickable(
                    onClick = { if (currentRevealed) onReveal(null) else onTap(recent) },
                    onLongClick = { onReveal(recent.id) },
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppGlyphImage(glyph = recent.app.glyph, size = 20.dp, tint = c.inkMuted)
            Text(
                text = ltr(PhoneNumberNormalizer.displayInternational(recent.dialCode, recent.nationalNumber)),
                style = OcTheme.type.mono14_5,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = timeLabel, style = OcTheme.type.mono11_5, color = c.muted, maxLines = 1)
        }
    }
}

/**
 * The panel under a recent row: destructive tint across the row, trash 20 + "Delete" 13/600 at the
 * end, 18 from the edge. Only the [RevealWidth] the open row uncovers is the Delete button.
 */
@Composable
private fun DeletePanel(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(modifier = modifier.background(c.destructiveTint), contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .width(RevealWidth)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            LucideIconImage(icon = LucideIcon.Trash, size = 20.dp, tint = c.destructive)
            Text(text = stringResource(R.string.home_delete), style = OcTheme.type.label13, color = c.destructive, maxLines = 1)
        }
    }
}
