package com.piptechnologies.openchat.ui.home

import com.piptechnologies.openchat.core.phone.DialCountry
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.core.send.RecentNumber
import com.piptechnologies.openchat.platform.DetectedCountry
import com.piptechnologies.openchat.ui.icons.AppGlyph
import com.piptechnologies.openchat.ui.navigation.HomeTool

/** One Home tool row: which tool, and the status line under its title (design map §4.3). */
data class HomeToolStatus(val tool: HomeTool, val status: String)

/**
 * Everything Home shows (design map §4.3–§4.6).
 *
 * [country] and [nationalDigits] make up the number ([nationalDigits] holds digits only, at most 15);
 * [app] is the app Send opens, one of [availableApps]. [firstRun] is true while there are no
 * [recents]. [countrySheetOpen], [menuOpen] and [notOnWhatsApp] are the three overlays;
 * [revealedRecentId] is the recent row slid open over its Delete panel. [nowMs] is the time the
 * recent rows' "2h" / "Yesterday" labels are measured from.
 */
data class HomeUiState(
    val country: DialCountry,
    val detected: DetectedCountry?,
    val nationalDigits: String,
    val message: String,
    val app: MessagingApp,
    val availableApps: List<MessagingApp>,
    val recents: List<RecentNumber>,
    val firstRun: Boolean,
    val tools: List<HomeToolStatus>,
    val accessGranted: Boolean,
    val countrySheetOpen: Boolean,
    val countryQuery: String,
    val menuOpen: Boolean,
    val notOnWhatsApp: Boolean,
    val revealedRecentId: Long?,
    val nowMs: Long,
) {
    /** Send is live once there is at least one digit; the app selector stays live either way. */
    val canSend: Boolean get() = nationalDigits.isNotEmpty()
}

/** What Home reports; the defaults do nothing, so screenshots can pass `HomeCallbacks()`. */
class HomeCallbacks(
    val onSettings: () -> Unit = {},
    val onCountryChip: () -> Unit = {},
    val onDigitsChange: (String) -> Unit = {},
    val onPaste: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onMessageChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onToggleMenu: () -> Unit = {},
    val onDismissMenu: () -> Unit = {},
    val onPickApp: (MessagingApp) -> Unit = {},
    val onRecentTap: (RecentNumber) -> Unit = {},
    val onRecentReveal: (Long?) -> Unit = {},
    val onRecentDelete: (Long) -> Unit = {},
    val onTool: (HomeTool) -> Unit = {},
    val onCountryQuery: (String) -> Unit = {},
    val onCountryPick: (DialCountry) -> Unit = {},
    val onCountryDismiss: () -> Unit = {},
    val onNotOnWhatsAppEdit: () -> Unit = {},
    val onNotOnWhatsAppTelegram: () -> Unit = {},
)

/** The design's line glyph for each app (ruling R10). */
val MessagingApp.glyph: AppGlyph
    get() = when (this) {
        MessagingApp.WHATSAPP -> AppGlyph.WhatsApp
        MessagingApp.WHATSAPP_BUSINESS -> AppGlyph.WhatsAppBusiness
        MessagingApp.TELEGRAM -> AppGlyph.Telegram
    }
