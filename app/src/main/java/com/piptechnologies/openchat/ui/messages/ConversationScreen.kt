package com.piptechnologies.openchat.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.CapturedMessage
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.ui.components.HintLine
import com.piptechnologies.openchat.ui.components.LocalMediaThumbnail
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.SecondaryButton
import com.piptechnologies.openchat.ui.components.TimeFormatter
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.currentUiLocale
import com.piptechnologies.openchat.ui.components.rememberTimeFormatter
import com.piptechnologies.openchat.ui.icons.AppGlyph
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.forScriptOf

private val BubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 6.dp)
private val PhotoShape = RoundedCornerShape(10.dp)
private const val DAY_MS = 86_400_000L

/**
 * Conversation (design map §4.9): two-line bar, a day pill over each day's messages, incoming bubbles
 * (at most 80 % wide, at the start edge) with the "Deleted by sender" label on deleted ones, and the footer
 * with "Open chat in WhatsApp" and the mode's hint. A photo whose image was copied shows through [thumbnail]
 * (200 × 130, radius 10) and opens Media detail ([onOpenMedia]) while its copy exists. Day pills and times
 * are in the UI language.
 */
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    onBack: () -> Unit,
    onOpenInWhatsApp: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    thumbnail: @Composable (localPath: String?, modifier: Modifier) -> Unit = { p, m -> LocalMediaThumbnail(p, m) },
) {
    ScreenSurface {
        OcTopBar(title = displayTitle(state.title), onBack = onBack, subtitle = state.subtitle.asString())
        MessageList(state = state, onOpenMedia = onOpenMedia, thumbnail = thumbnail, modifier = Modifier.weight(1f))
        Footer(mode = state.mode, onOpenInWhatsApp = onOpenInWhatsApp)
    }
}

/** A line of the message list: a day pill or a message. */
private sealed interface ListRow {
    val key: Any

    data class Day(val label: String, override val key: Any) : ListRow

    data class Message(val message: CapturedMessage, override val key: Any) : ListRow
}

@Composable
private fun MessageList(
    state: ConversationUiState,
    onOpenMedia: (Long) -> Unit,
    thumbnail: @Composable (localPath: String?, modifier: Modifier) -> Unit,
    modifier: Modifier,
) {
    val formatter = rememberTimeFormatter()
    val rows = remember(state.messages, state.nowMs, formatter) { listRows(state.messages, state.nowMs, formatter) }
    val listState = rememberLazyListState()
    // Long conversations open on their latest message, once; later arrivals do not yank the list.
    var openedAtEnd by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(rows.isEmpty()) {
        if (rows.isNotEmpty() && !openedAtEnd) {
            listState.scrollToItem(rows.lastIndex)
            openedAtEnd = true
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is ListRow.Day -> DayPill(label = row.label)
                is ListRow.Message -> MessageBubble(
                    message = row.message,
                    time = formatter.clock(row.message.timestamp),
                    localPath = row.message.mediaId?.let { state.thumbnails[it] },
                    onOpenMedia = onOpenMedia,
                    thumbnail = thumbnail,
                    modifier = Modifier
                        .fillParentMaxWidth(0.8f)
                        .wrapContentWidth(Alignment.Start),
                )
            }
        }
    }
}

/**
 * The messages with a day pill before the first message of each local day of [formatter]'s time zone,
 * labelled by [TimeFormatter.dayLabel] ("Today", "Yesterday", "Mon 21 Sep" in English).
 */
private fun listRows(messages: List<CapturedMessage>, nowMs: Long, formatter: TimeFormatter): List<ListRow> {
    val zone = formatter.timeZone
    fun localDay(ms: Long): Long = Math.floorDiv(ms + zone.getOffset(ms), DAY_MS)
    val rows = ArrayList<ListRow>(messages.size + 1)
    var previousDay: Long? = null
    for (message in messages) {
        val day = localDay(message.timestamp)
        if (day != previousDay) {
            rows += ListRow.Day(label = formatter.dayLabel(message.timestamp, nowMs), key = "day-$day")
            previousDay = day
        }
        rows += ListRow.Message(message = message, key = message.id)
    }
    return rows
}

/** Date pill: mono 10/600 .06em uppercase (in scripts that have case), muted on subtle, padding 4 9, centred. */
@Composable
private fun DayPill(label: String) {
    val c = OcTheme.colors
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val shown = label.uppercase(currentUiLocale())
        Text(
            text = shown,
            style = OcTheme.type.eyebrow10.copy(letterSpacing = 0.06.em).forScriptOf(shown),
            color = c.muted,
            modifier = Modifier
                .clip(RoundedCornerShape(OcRadius.pill))
                .background(c.subtle)
                .padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

/**
 * An incoming bubble: optional "Deleted by sender" label, then text or photo, then the [time]. The message
 * text takes its own direction (TextDirection.Content), so an English message in a right-to-left UI, or an
 * Arabic one in a left-to-right UI, keeps its punctuation in place.
 */
@Composable
private fun MessageBubble(
    message: CapturedMessage,
    time: String,
    localPath: String?,
    onOpenMedia: (Long) -> Unit,
    thumbnail: @Composable (localPath: String?, modifier: Modifier) -> Unit,
    modifier: Modifier,
) {
    val c = OcTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (message.isDeleted) DeletedLabel()
        Column(
            modifier = Modifier
                .background(c.surface, BubbleShape)
                .border(1.dp, c.border, BubbleShape)
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 7.dp),
        ) {
            val mediaId = message.mediaId
            if (isPhotoBubble(message) && mediaId != null) {
                Box(
                    modifier = Modifier
                        .size(width = 200.dp, height = 130.dp)
                        .clip(PhotoShape)
                        .clickable(enabled = localPath != null, role = Role.Image, onClick = { onOpenMedia(mediaId) })
                        .border(1.dp, c.border, PhotoShape),
                ) {
                    thumbnail(localPath, Modifier.matchParentSize())
                }
            } else {
                Text(text = message.text, style = OcTheme.type.body14_5.copy(textDirection = TextDirection.Content), color = c.ink)
            }
            Text(
                text = time,
                style = OcTheme.type.mono10_5,
                color = c.muted,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 5.dp),
            )
        }
    }
}

/** message-square-dashed 12 (sw 2) + "DELETED BY SENDER" eyebrow 9.5 in amber, 4 in from the bubble edge. */
@Composable
private fun DeletedLabel() {
    val c = OcTheme.colors
    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        LucideIconImage(icon = LucideIcon.MessageSquareDashed, size = 12.dp, tint = c.amber, strokeWidth = 2f)
        val label = stringResource(R.string.conversation_deleted_label).uppercase(currentUiLocale())
        Text(text = label, style = OcTheme.type.eyebrow9_5.forScriptOf(label), color = c.amber)
    }
}

/** White footer under a soft top border: "Open chat in WhatsApp" with the glyph 18, then the mode's hint. */
@Composable
private fun Footer(mode: InboxMode, onOpenInWhatsApp: () -> Unit) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface),
    ) {
        HorizontalDivider(thickness = 1.dp, color = c.borderSoft)
        Column(modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp)) {
            SecondaryButton(
                text = stringResource(R.string.conversation_open),
                onClick = onOpenInWhatsApp,
                leading = { AppGlyphImage(glyph = AppGlyph.WhatsApp, size = 18.dp, tint = c.ink) },
            )
            Spacer(Modifier.height(8.dp))
            HintLine(text = stringResource(if (mode == InboxMode.ALL) R.string.conversation_hint_all else R.string.conversation_hint_deleted))
        }
        // Ruling R2: no ad banner slot; the footer keeps the slot's 14 dp bottom padding (white, as in the design).
        Spacer(Modifier.height(14.dp))
    }
}
