package com.piptechnologies.openchat.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.core.messages.NotificationText
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.ConfirmSpec
import com.piptechnologies.openchat.ui.components.IconSquareButton
import com.piptechnologies.openchat.ui.components.IconSquareKind
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.isolate
import com.piptechnologies.openchat.ui.components.ltr
import com.piptechnologies.openchat.ui.components.rememberTimeFormatter
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

/**
 * Media detail (design map §4.13): bar with the kind ("Photo") and the "1 / 3" counter, the [preview] on ink
 * (margin 0 20, radius 20, filling the height), the meta line ([metaLine]) under the amber deleted mark, then
 * Save to gallery, Share and Delete. [MediaDetailUiState.confirmDelete] opens the delete confirmation (§4.20).
 */
@Composable
fun MediaDetailScreen(
    state: MediaDetailUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onAskDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    preview: @Composable (RecoveredMedia, Modifier) -> Unit = { m, mod -> MediaPreview(m, mod) },
) {
    val c = OcTheme.colors
    val item = state.item
    ScreenSurface {
        OcTopBar(
            title = item?.let { stringResource(it.category.detailTitleRes) }.orEmpty(),
            onBack = onBack,
            actions = {
                if (state.index > 0 && state.total > 0) {
                    Text(
                        text = stringResource(R.string.media_counter, state.index, state.total),
                        style = OcTheme.type.mono12,
                        color = c.muted,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.ink),
            contentAlignment = Alignment.Center,
        ) {
            if (item != null) preview(item, Modifier.fillMaxSize())
        }
        if (item != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 14.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (item.deletedAt != null) {
                    LucideIconImage(icon = LucideIcon.MessageSquareDashed, size = 12.dp, tint = c.amber, strokeWidth = 2f)
                }
                Text(text = metaLine(item, state.nowMs), style = OcTheme.type.body12_5, color = c.ink2)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.media_save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    leading = { LucideIconImage(icon = LucideIcon.Download, size = 18.dp, tint = Color.White) },
                )
                IconSquareButton(icon = LucideIcon.Share2, contentDescription = stringResource(R.string.media_share), onClick = onShare)
                IconSquareButton(
                    icon = LucideIcon.Trash,
                    contentDescription = stringResource(R.string.media_delete),
                    onClick = onAskDelete,
                    kind = IconSquareKind.Destructive,
                )
            }
        }
        // Ruling R2: no ad banner slot; the screen keeps the slot's 14 dp bottom padding instead.
        Spacer(Modifier.height(14.dp))
    }
    if (state.confirmDelete && item != null) {
        ConfirmSheet(spec = deleteMediaSpec(item.category), onDismiss = onDismissDelete, onConfirm = onConfirmDelete)
    }
}

/**
 * "From <sender> · <day> <time> · Deleted <time>" when the sender is known, else "Received <day> <time> ·
 * Deleted <time>" (§4.13), in the UI language: the day ("Today", "Mon 21 Sep", relative to [nowMs]) and the
 * times come from [rememberTimeFormatter]. Received = the original's time, as on the grid. A copy that was not
 * deleted (opened from a conversation photo) drops the "Deleted" part.
 */
@Composable
private fun metaLine(item: RecoveredMedia, nowMs: Long): String {
    val time = rememberTimeFormatter()
    val day = time.dayLabel(item.originalModifiedAt, nowMs)
    val at = time.clock(item.originalModifiedAt)
    val sender = item.sender?.takeIf { it.isNotBlank() }?.let(::senderInText)
    val deletedAt = item.deletedAt
    return when {
        deletedAt == null && sender != null -> stringResource(R.string.media_meta_from_kept, sender, day, at)
        deletedAt == null -> stringResource(R.string.media_meta_received_kept, day, at)
        sender != null -> stringResource(R.string.media_meta_from, sender, day, at, time.clock(deletedAt))
        else -> stringResource(R.string.media_meta_received, day, at, time.clock(deletedAt))
    }
}

/**
 * [sender] for the meta line, isolated from the translated text around it. The notification names an unsaved
 * contact by its number ("+62 813 9922 0417"): a sender that [NotificationText.phoneNumberFrom] reads as a number
 * stays left to right ([ltr]), so a right-to-left language does not reorder its digit groups; a name keeps its own
 * direction ([isolate]), so a Latin name in Arabic (or an Arabic name in English) does not pull the separators
 * next to it to the wrong side. Display only.
 */
private fun senderInText(sender: String): String =
    if (NotificationText.phoneNumberFrom(sender) != null) ltr(sender) else isolate(sender)

/** "Delete this photo?" (§4.13, §4.20): trash in the destructive tints, Keep / Delete in destructive. */
@Composable
@ReadOnlyComposable
fun deleteMediaSpec(category: MediaCategory): ConfirmSpec {
    val c = OcTheme.colors
    return ConfirmSpec(
        icon = LucideIcon.Trash,
        tint = ToolTint(bg = c.destructiveTint, fg = c.destructive),
        title = stringResource(category.deleteTitleRes),
        body = stringResource(R.string.delete_media_body),
        cancelLabel = stringResource(R.string.keep),
        confirmLabel = stringResource(R.string.media_delete),
        destructive = true,
    )
}
