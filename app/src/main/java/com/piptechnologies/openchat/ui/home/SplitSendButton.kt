package com.piptechnologies.openchat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * The split Send button, 52 high (design map §4.3). Leading: green "Send Message" with radii
 * 14 6 6 14, at 45 % opacity and inert while [canSend] is false. Trailing, 2 dp to the right: 62 wide,
 * radii 6 14 14 6, green tint and border, the chosen [app]'s glyph 22 and chevron-down 14. The
 * trailing segment stays live without digits: it opens the Send-with menu ([onToggleMenu]).
 */
@Composable
internal fun SplitSendButton(
    app: MessagingApp,
    canSend: Boolean,
    onSend: () -> Unit,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val leadingShape = RoundedCornerShape(
        topStart = OcRadius.md,
        topEnd = OcRadius.xs,
        bottomEnd = OcRadius.xs,
        bottomStart = OcRadius.md,
    )
    val trailingShape = RoundedCornerShape(
        topStart = OcRadius.xs,
        topEnd = OcRadius.md,
        bottomEnd = OcRadius.md,
        bottomStart = OcRadius.xs,
    )
    val chooseApp = stringResource(R.string.home_choose_app_cd)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .alpha(if (canSend) 1f else 0.45f)
                .clip(leadingShape)
                .background(c.green)
                .clickable(enabled = canSend, role = Role.Button, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.home_send), style = OcTheme.type.label16, color = Color.White, maxLines = 1)
        }
        Row(
            modifier = Modifier
                .width(62.dp)
                .fillMaxHeight()
                .clip(trailingShape)
                .background(c.greenTint)
                .border(1.dp, c.greenTintBorder, trailingShape)
                .clickable(role = Role.Button, onClick = onToggleMenu)
                .semantics { contentDescription = chooseApp }
                .padding(start = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppGlyphImage(glyph = app.glyph, size = 22.dp, tint = c.green)
            LucideIconImage(icon = LucideIcon.ChevronDown, size = 14.dp, tint = c.green, strokeWidth = 2.2f)
        }
    }
}
