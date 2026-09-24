package com.piptechnologies.openchat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme

/** Width of the Send-with card (design map §4.5); [HomeScreen] lines its end edge up with the split button's. */
internal val SendWithMenuWidth: Dp = 238.dp

/** Space between the split button and the card below it. */
internal val SendWithMenuGap: Dp = 8.dp

/** rgb(20,30,60): the card's shadow tint in the design (0 12 32 −10 at 28 %). */
private val MenuShadow = Color(0xFF141E3C)

/**
 * The Send-with card (design map §4.5): 238 wide, white, 1 px border, 14 radius, padding 6, shadow.
 * Eyebrow "SEND WITH", then one 46 high row per app in [apps]: [appIcon] 20 in ink soft, the name
 * 14.5/600 and a green check 18 on [current]. Picking a row calls [onPick]; any other tap inside the
 * card does nothing.
 */
@Composable
fun SendWithMenuContent(
    apps: List<MessagingApp>,
    current: MessagingApp,
    onPick: (MessagingApp) -> Unit,
    appIcon: @Composable (MessagingApp, Dp, Color) -> Unit,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    Column(
        modifier = Modifier
            .width(SendWithMenuWidth)
            .shadow(elevation = 12.dp, shape = shape, ambientColor = Color.Transparent, spotColor = MenuShadow)
            .background(c.surface, shape)
            .border(1.dp, c.border, shape)
            // A tap on the eyebrow or the padding stays in the card instead of reaching the full-size
            // catcher under it, which would close the menu.
            .pointerInput(Unit) {}
            .padding(6.dp),
    ) {
        Text(
            // Capitals by the UI language's rules (Turkish "i" → "İ").
            text = stringResource(R.string.home_send_with).uppercase(uiLocale()),
            style = OcTheme.type.eyebrow10,
            color = c.muted,
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 6.dp),
        )
        apps.forEach { app ->
            val selected = app == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(OcRadius.sm))
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(app) })
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                appIcon(app, 20.dp, c.inkSoft)
                // Brand names ("WhatsApp", "WhatsApp Business", "Telegram") are not translated.
                Text(
                    text = app.label,
                    style = OcTheme.type.label14_5,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    LucideIconImage(icon = LucideIcon.Check, size = 18.dp, tint = c.green, strokeWidth = 2.2f)
                }
            }
        }
    }
}
