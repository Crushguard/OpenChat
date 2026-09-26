package com.piptechnologies.openchat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.ui.icons.AppGlyph
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme

/** The line glyph the design draws for each app (ruling R10): phone-in-bubble, B-in-bubble, paper plane. */
internal fun MessagingApp.settingsGlyph(): AppGlyph = when (this) {
    MessagingApp.WHATSAPP -> AppGlyph.WhatsApp
    MessagingApp.WHATSAPP_BUSINESS -> AppGlyph.WhatsAppBusiness
    MessagingApp.TELEGRAM -> AppGlyph.Telegram
}

/**
 * The default-app sheet body (design map §4.16): title "Default app", then one 50 dp row per app in
 * [apps] (glyph 20 inkSoft, label, a green check on [current]). A tap reports [onPick]; the caller
 * closes the sheet and shows the toast.
 */
@Composable
fun DefaultAppSheetContent(apps: List<MessagingApp>, current: MessagingApp, onPick: (MessagingApp) -> Unit) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Top 6: as the tool settings sheet (SheetColumn), whose padding and handle the design shares.
            .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.default_app_title),
            style = OcTheme.type.title15,
            color = c.ink,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 10.dp),
        )
        apps.forEach { app ->
            val selected = app == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected = selected, role = Role.RadioButton) { onPick(app) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                AppGlyphImage(glyph = app.settingsGlyph(), size = 20.dp, tint = c.inkSoft)
                Text(text = app.label, style = OcTheme.type.label14_5, color = c.ink, modifier = Modifier.weight(1f))
                if (selected) {
                    LucideIconImage(icon = LucideIcon.Check, size = 18.dp, tint = c.green, strokeWidth = 2.2f)
                }
            }
        }
    }
}
