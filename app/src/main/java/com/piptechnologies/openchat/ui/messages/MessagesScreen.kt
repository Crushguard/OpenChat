package com.piptechnologies.openchat.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.ConversationSummary
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.core.phone.RelativeTime
import com.piptechnologies.openchat.ui.components.ChipState
import com.piptechnologies.openchat.ui.components.CountBadge
import com.piptechnologies.openchat.ui.components.EmptyState
import com.piptechnologies.openchat.ui.components.FilterPill
import com.piptechnologies.openchat.ui.components.HairlineDivider
import com.piptechnologies.openchat.ui.components.InfoCallout
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.StateChip
import com.piptechnologies.openchat.ui.components.TopBarIconButton
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * Messages (design map §4.8): bar with the Active/Paused chip and the tool settings button, the All /
 * Deleted only chips, then the conversations in one card with the mode's info callout below, or the
 * empty state. The sheets are rendered by [MessagesRoute].
 */
@Composable
fun MessagesScreen(state: MessagesUiState, callbacks: MessagesCallbacks) {
    ScreenSurface {
        OcTopBar(
            title = stringResource(R.string.messages_title),
            onBack = callbacks.onBack,
            actions = {
                StateChip(state = if (state.paused) ChipState.Paused else ChipState.Active, small = true)
                TopBarIconButton(
                    icon = LucideIcon.SlidersHorizontal,
                    contentDescription = stringResource(R.string.messages_tool_settings),
                    onClick = callbacks.onOpenToolSettings,
                )
            },
        )
        ModeChips(mode = state.mode, unreadTotal = state.unreadTotal, deletedTotal = state.deletedTotal, onMode = callbacks.onMode)
        if (state.conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = LucideIcon.MessageSquareDashed,
                    title = stringResource(R.string.messages_empty_title),
                    body = stringResource(R.string.messages_empty_body),
                )
            }
        } else {
            ConversationList(state = state, onOpen = callbacks.onOpenConversation, modifier = Modifier.weight(1f))
        }
        // Ruling R2: no ad banner slot; the screen keeps the slot's 14 dp bottom padding instead.
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ModeChips(mode: InboxMode, unreadTotal: Int, deletedTotal: Int, onMode: (InboxMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = stringResource(R.string.messages_chip_all, unreadTotal),
            selected = mode == InboxMode.ALL,
            onClick = { onMode(InboxMode.ALL) },
        )
        FilterPill(
            label = stringResource(R.string.messages_chip_deleted, deletedTotal),
            selected = mode == InboxMode.DELETED,
            onClick = { onMode(InboxMode.DELETED) },
        )
    }
}

/** The card of conversation rows (one lazy item per row, each drawing its slice of the card), then the info callout. */
@Composable
private fun ConversationList(state: MessagesUiState, onOpen: (ConversationSummary) -> Unit, modifier: Modifier) {
    val c = OcTheme.colors
    val lastIndex = state.conversations.lastIndex
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
    ) {
        itemsIndexed(state.conversations, key = { _, conversation -> conversation.key }) { index, conversation ->
            Column(modifier = Modifier.cardSlice(first = index == 0, last = index == lastIndex, fill = c.surface, border = c.borderSoft)) {
                ConversationRow(
                    conversation = conversation,
                    mode = state.mode,
                    time = RelativeTime.conversationTime(conversation.lastTimestamp, state.nowMs),
                    onClick = { onOpen(conversation) },
                )
                if (index != lastIndex) HairlineDivider()
            }
        }
        item(key = "info") {
            InfoCallout(
                text = stringResource(if (state.mode == InboxMode.ALL) R.string.messages_info_all else R.string.messages_info_deleted),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/**
 * Row 12/14 padding, 12 gap: avatar 42, name (ellipsis) and time on one baseline, then the preview with
 * the deleted mark first in Deleted only, and the badge (unread in All, deleted in Deleted only; none at 0).
 */
@Composable
private fun ConversationRow(conversation: ConversationSummary, mode: InboxMode, time: String, onClick: () -> Unit) {
    val c = OcTheme.colors
    val badge = if (mode == InboxMode.ALL) conversation.unreadCount else conversation.deletedCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConversationAvatar(
            initial = conversation.initial,
            colorIndex = conversation.colorIndex,
            size = 42.dp,
            textStyle = OcTheme.type.title15,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = conversation.title,
                    style = OcTheme.type.label14_5,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                )
                Text(text = time, style = OcTheme.type.mono11, color = c.muted, maxLines = 1, modifier = Modifier.alignByBaseline())
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mode == InboxMode.DELETED) {
                    LucideIconImage(icon = LucideIcon.MessageSquareDashed, size = 12.dp, tint = c.amber, strokeWidth = 2f)
                }
                Text(
                    text = conversation.preview,
                    style = OcTheme.type.body13,
                    color = c.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (badge > 0) CountBadge(count = badge, modifier = Modifier.widthIn(min = 20.dp))
            }
        }
    }
}

/** Round avatar in the conversation's tint with its initial ("#" for a number). Also used by the exclude sheet. */
@Composable
internal fun ConversationAvatar(initial: String, colorIndex: Int, size: Dp, textStyle: TextStyle) {
    val tint = OcTheme.colors.avatarTint(colorIndex)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = textStyle, color = tint.fg)
    }
}

/**
 * This row's slice of the white 16-radius card: rounded top on the first row, rounded bottom on the last,
 * and the 1 dp border on the card's outer edges only. The outline is drawn for the whole card, stretched
 * past this slice's open edges, and the clip keeps just this slice's part of it.
 */
private fun Modifier.cardSlice(first: Boolean, last: Boolean, fill: Color, border: Color): Modifier {
    val top = if (first) OcRadius.card else 0.dp
    val bottom = if (last) OcRadius.card else 0.dp
    return this
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
        .background(fill)
        .drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            val radius = OcRadius.card.toPx()
            val outlineTop = if (first) 0f else -(radius + stroke)
            val outlineBottom = if (last) size.height else size.height + radius + stroke
            drawRoundRect(
                color = border,
                topLeft = Offset(stroke / 2, outlineTop + stroke / 2),
                size = Size(size.width - stroke, outlineBottom - outlineTop - stroke),
                cornerRadius = CornerRadius(radius - stroke / 2),
                style = Stroke(width = stroke),
            )
        }
}
