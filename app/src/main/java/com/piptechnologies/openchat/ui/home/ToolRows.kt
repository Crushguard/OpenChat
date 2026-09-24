package com.piptechnologies.openchat.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.CardColumn
import com.piptechnologies.openchat.ui.components.HairlineDivider
import com.piptechnologies.openchat.ui.components.IconBox
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.icons.LUCIDE_STROKE
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.navigation.HomeTool
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

/**
 * The tools card (design map §4.3): one 64 high row per entry of [tools], in order: icon box 40/12 in
 * the tool's tint, title 14.5/600, status 12 muted on one line, chevron-right 18. A tap calls [onTool].
 */
@Composable
internal fun ToolRows(tools: List<HomeToolStatus>, onTool: (HomeTool) -> Unit, modifier: Modifier = Modifier) {
    CardColumn(modifier = modifier) {
        tools.forEachIndexed { index, item ->
            if (index > 0) HairlineDivider()
            ToolRow(item = item, onClick = { onTool(item.tool) })
        }
    }
}

@Composable
private fun ToolRow(item: HomeToolStatus, onClick: () -> Unit) {
    val c = OcTheme.colors
    val look = toolLook(item.tool)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconBox(
            icon = look.icon,
            tint = look.tint,
            size = 40.dp,
            radius = 12.dp,
            iconSize = 20.dp,
            strokeWidth = look.strokeWidth,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(look.title),
                style = OcTheme.type.label14_5,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.status.asString(),
                style = OcTheme.type.body12,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LucideIconImage(icon = LucideIcon.ChevronRight, size = 18.dp, tint = c.chevron)
    }
}

/** Title, icon and tint of a tool row, as on onboarding slide 2. */
private class ToolLook(
    @StringRes val title: Int,
    val icon: LucideIcon,
    val tint: ToolTint,
    val strokeWidth: Float = LUCIDE_STROKE,
)

@Composable
private fun toolLook(tool: HomeTool): ToolLook {
    val c = OcTheme.colors
    return when (tool) {
        HomeTool.UNSEEN -> ToolLook(R.string.tool_unseen_title, LucideIcon.CheckCheck, c.toolTicks, strokeWidth = 2f)
        HomeTool.DELETED_MESSAGES -> ToolLook(R.string.tool_deleted_title, LucideIcon.ArchiveRestore, c.toolMessages)
        HomeTool.MEDIA -> ToolLook(R.string.tool_media_title, LucideIcon.Image, c.toolMedia)
        HomeTool.SECOND -> ToolLook(R.string.tool_second_title, LucideIcon.QrCode, c.toolSecond)
    }
}
