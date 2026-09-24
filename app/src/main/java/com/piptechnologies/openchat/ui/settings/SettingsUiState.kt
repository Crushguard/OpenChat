package com.piptechnologies.openchat.ui.settings

import androidx.compose.runtime.Stable
import com.piptechnologies.openchat.core.send.MessagingApp

/**
 * What Settings shows (design map §4.15): the access card state, the default [app] and the apps the
 * default-app sheet offers, the language row value, the recent-numbers count, the version label, and
 * which overlay is open (the default-app sheet, the clear-recents confirmation, or the rating sheet).
 */
data class SettingsUiState(
    val accessGranted: Boolean,
    val app: MessagingApp,
    val availableApps: List<MessagingApp>,
    val languageName: String,
    val recentsCount: Int,
    val version: String,
    val defaultAppSheetOpen: Boolean,
    val confirmClearRecents: Boolean,
    val rating: RatingState?,
)

/**
 * Everything Settings reports back. Each callback defaults to a no-op, so screenshots pass
 * `SettingsCallbacks()`. The `onRating*` callbacks drive the rating sheet the screen shows while
 * [SettingsUiState.rating] is not null.
 */
@Stable
class SettingsCallbacks(
    val onBack: () -> Unit = {},
    val onAccessCard: () -> Unit = {},
    val onDefaultApp: () -> Unit = {},
    val onDismissDefaultApp: () -> Unit = {},
    val onPickApp: (MessagingApp) -> Unit = {},
    val onLanguage: () -> Unit = {},
    val onAskClearRecents: () -> Unit = {},
    val onDismissClearRecents: () -> Unit = {},
    val onConfirmClearRecents: () -> Unit = {},
    val onRate: () -> Unit = {},
    val onContact: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onPrivacy: () -> Unit = {},
    val onRatingStar: (Int) -> Unit = {},
    val onRatingFeedback: (String) -> Unit = {},
    val onRatingSend: () -> Unit = {},
    val onRatingPlay: () -> Unit = {},
    val onRatingClose: () -> Unit = {},
)
