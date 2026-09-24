package com.piptechnologies.openchat.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.ConfirmSpec
import com.piptechnologies.openchat.ui.components.OcSwitch
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

/**
 * §4.10, shared with Deleted media. The sheet body under the handle: [title], then three 52 rows —
 * "Pause recovery" with a switch (on = [paused]), "Exclude chats" with [excludedCount] and a chevron, and
 * the destructive [clearLabel] row.
 */
@Composable
fun ToolSettingsSheetContent(
    title: String,
    paused: Boolean,
    excludedCount: Int,
    clearLabel: String,
    onTogglePause: () -> Unit,
    onExclude: () -> Unit,
    onClear: () -> Unit,
) {
    val c = OcTheme.colors
    SheetColumn {
        SheetTitle(text = title)
        SheetRow(
            icon = LucideIcon.Pause,
            label = stringResource(R.string.tool_settings_pause),
            modifier = Modifier.toggleable(value = paused, role = Role.Switch, onValueChange = { onTogglePause() }),
        ) {
            OcSwitch(checked = paused)
        }
        SheetRow(
            icon = LucideIcon.EyeOff,
            label = stringResource(R.string.tool_settings_exclude),
            modifier = Modifier.clickable(role = Role.Button, onClick = onExclude),
        ) {
            Text(text = excludedCount.toString(), style = OcTheme.type.body13_5, color = c.muted)
            LucideIconImage(icon = LucideIcon.ChevronRight, size = 18.dp, tint = c.chevron)
        }
        SheetRow(
            icon = LucideIcon.Trash,
            label = clearLabel,
            modifier = Modifier.clickable(role = Role.Button, onClick = onClear),
            color = c.destructive,
        )
    }
}

/** "Clear all recovered messages?" (§4.10, §4.20): trash in the destructive tint, Keep / Clear (destructive). */
@Composable
fun clearMessagesSpec(): ConfirmSpec {
    val c = OcTheme.colors
    return ConfirmSpec(
        icon = LucideIcon.Trash,
        tint = ToolTint(bg = c.destructiveTint, fg = c.destructive),
        title = stringResource(R.string.clear_messages_title),
        body = stringResource(R.string.clear_body),
        cancelLabel = stringResource(R.string.keep),
        confirmLabel = stringResource(R.string.clear),
        destructive = true,
    )
}

/**
 * The sheet grammar of §4.10 and §4.11: padding 14 12 20 around the content. The sheet containers put the
 * handle 10 dp from the top with 10 below; the extra 6 on top lands the title where the design has it.
 */
@Composable
internal fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 20.dp),
    ) {
        content()
    }
}

/** Sheet title 15/700, padding 4 12 10. */
@Composable
internal fun SheetTitle(text: String) {
    Text(
        text = text,
        style = OcTheme.type.title15,
        color = OcTheme.colors.ink,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 10.dp),
    )
}

/** Sheet row 52 high, padding 0 12, gap 13: a 20 icon (ink muted unless [color] is given), the label 14.5/600, then [trailing]. */
@Composable
internal fun SheetRow(
    icon: LucideIcon,
    label: String,
    modifier: Modifier,
    color: Color? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = OcTheme.colors
    SheetRowFrame(modifier = modifier) {
        LucideIconImage(icon = icon, size = 20.dp, tint = color ?: c.inkMuted)
        SheetRowLabel(text = label, color = color ?: c.ink)
        trailing()
    }
}

/** The 52 row shell shared by the tool settings and exclude rows; [modifier] carries the click or toggle. */
@Composable
internal fun SheetRowFrame(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        content = content,
    )
}

/** The row label: 14.5/600, one line, taking the room between the leading and trailing parts. */
@Composable
internal fun RowScope.SheetRowLabel(text: String, color: Color) {
    Text(
        text = text,
        style = OcTheme.type.label14_5,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}
