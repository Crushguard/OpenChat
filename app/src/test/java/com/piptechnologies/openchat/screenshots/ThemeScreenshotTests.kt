package com.piptechnologies.openchat.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.ui.components.ChipState
import com.piptechnologies.openchat.ui.components.FilterPill
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.SecondaryButton
import com.piptechnologies.openchat.ui.components.StateChip
import com.piptechnologies.openchat.ui.icons.AppGlyph
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import org.junit.Rule
import org.junit.Test

/** Smoke test of the design system: fonts, tokens, icons and the basic controls render. */
class ThemeScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun theme_smoke() {
        paparazzi.snapshot {
            OpenChatTheme {
                ScreenSurface {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Allow notification access", style = OcTheme.type.display22, color = OcTheme.colors.inkStrong)
                        Text("Nothing is saved to your contacts.", style = OcTheme.type.body15, color = OcTheme.colors.inkSoft)
                        Text("812 3456 7890", style = OcTheme.type.mono22, color = OcTheme.colors.ink)
                        PrimaryButton(text = "Send Message", onClick = {})
                        PrimaryButton(text = "Send Message", onClick = {}, enabled = false)
                        SecondaryButton(text = "Open chat in WhatsApp", onClick = {})
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StateChip(ChipState.Off)
                            StateChip(ChipState.Active)
                            StateChip(ChipState.Linked, small = true)
                            StateChip(ChipState.Paused, small = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterPill("Photos", selected = true, onClick = {})
                            FilterPill("Videos", selected = false, onClick = {})
                            FilterPill("Audio", selected = false, onClick = {})
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppGlyphImage(AppGlyph.WhatsApp, 22.dp, OcTheme.colors.green)
                            AppGlyphImage(AppGlyph.WhatsAppBusiness, 22.dp, OcTheme.colors.green)
                            AppGlyphImage(AppGlyph.Telegram, 22.dp, OcTheme.colors.green)
                            LucideIconImage(LucideIcon.CheckCheck, 20.dp, OcTheme.colors.toolTicks.fg, strokeWidth = 2f)
                            LucideIconImage(LucideIcon.ArchiveRestore, 20.dp, OcTheme.colors.toolMessages.fg)
                            LucideIconImage(LucideIcon.Image, 20.dp, OcTheme.colors.toolMedia.fg)
                            LucideIconImage(LucideIcon.QrCode, 20.dp, OcTheme.colors.toolSecond.fg)
                        }
                    }
                }
            }
        }
    }
}
