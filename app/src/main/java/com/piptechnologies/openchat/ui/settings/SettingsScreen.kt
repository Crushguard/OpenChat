package com.piptechnologies.openchat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.CardColumn
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.ConfirmSpec
import com.piptechnologies.openchat.ui.components.HairlineDivider
import com.piptechnologies.openchat.ui.components.HonestyCallout
import com.piptechnologies.openchat.ui.components.IconBox
import com.piptechnologies.openchat.ui.components.OcModalSheet
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.SectionEyebrow
import com.piptechnologies.openchat.ui.components.TopBarIconButton
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.ltr
import com.piptechnologies.openchat.ui.components.pluralText
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint
import java.text.NumberFormat
import java.util.Locale

/**
 * Settings (design map §4.15): the 56 dp bar, the notification access card, the Preferences and About
 * cards and the honesty callout, then the overlays the state asks for: the default-app sheet
 * ([SettingsUiState.defaultAppSheetOpen]), the clear-recents confirmation
 * ([SettingsUiState.confirmClearRecents]) and the rating sheet ([SettingsUiState.rating]). The
 * Language row shows [languageName], the native name of the language in effect. The design's "More
 * apps (AD)" row is not rendered (ruling R3), the Version row is not tappable, and "Clear recent
 * numbers" is disabled while there is nothing to clear.
 */
@Composable
fun SettingsScreen(state: SettingsUiState, languageName: String, callbacks: SettingsCallbacks) {
    val c = OcTheme.colors
    ScreenSurface {
        SettingsBar(onBack = callbacks.onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, top = 2.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AccessCard(granted = state.accessGranted, onClick = callbacks.onAccessCard)
            SettingsSection(title = stringResource(R.string.settings_preferences)) {
                SettingsRow(
                    title = stringResource(R.string.settings_default_app),
                    value = state.app.label,
                    onClick = callbacks.onDefaultApp,
                    leading = { AppGlyphImage(glyph = state.app.settingsGlyph(), size = 20.dp, tint = c.inkMuted) },
                )
                HairlineDivider()
                SettingsRow(
                    icon = LucideIcon.Languages,
                    title = stringResource(R.string.settings_language),
                    value = languageName,
                    onClick = callbacks.onLanguage,
                )
                HairlineDivider()
                SettingsRow(
                    icon = LucideIcon.Trash,
                    title = stringResource(R.string.settings_clear_recents),
                    value = uiDigits(state.recentsCount),
                    onClick = callbacks.onAskClearRecents,
                    enabled = state.recentsCount > 0,
                )
            }
            SettingsSection(title = stringResource(R.string.settings_about)) {
                SettingsRow(icon = LucideIcon.Star, title = stringResource(R.string.settings_rate), onClick = callbacks.onRate)
                HairlineDivider()
                SettingsRow(icon = LucideIcon.Mail, title = stringResource(R.string.settings_contact), onClick = callbacks.onContact)
                HairlineDivider()
                SettingsRow(icon = LucideIcon.Share2, title = stringResource(R.string.settings_share), onClick = callbacks.onShare)
                HairlineDivider()
                // Ruling R3: the design's "More apps (AD)" row is cross-promotion and is omitted.
                SettingsRow(
                    icon = LucideIcon.Shield,
                    title = stringResource(R.string.settings_privacy),
                    onClick = callbacks.onPrivacy,
                    trailing = LucideIcon.ArrowUpRight,
                )
                HairlineDivider()
                VersionRow(version = state.version)
            }
            HonestyCallout(text = stringResource(R.string.settings_honesty))
        }
    }
    if (state.defaultAppSheetOpen) {
        OcModalSheet(onDismissRequest = callbacks.onDismissDefaultApp) {
            DefaultAppSheetContent(apps = state.availableApps, current = state.app, onPick = callbacks.onPickApp)
        }
    }
    if (state.confirmClearRecents) {
        ConfirmSheet(
            spec = clearRecentsSpec(state.recentsCount),
            onDismiss = callbacks.onDismissClearRecents,
            onConfirm = callbacks.onConfirmClearRecents,
        )
    }
    val rating = state.rating
    if (rating != null) {
        RatingSheet(
            state = rating,
            onStar = callbacks.onRatingStar,
            onFeedbackChange = callbacks.onRatingFeedback,
            onSendFeedback = callbacks.onRatingSend,
            onRateOnPlay = callbacks.onRatingPlay,
            onClose = callbacks.onRatingClose,
        )
    }
}

/**
 * [count] in the UI language's digits (Persian ۴, Arabic ٤), as %d writes it in a translated string such
 * as the clear-recents confirmation, and as the badge counts show it.
 */
@Composable
private fun uiDigits(count: Int): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(count, locale) { NumberFormat.getIntegerInstance(locale).format(count) }
}

/** "Clear recent numbers?" (design map §4.15, §4.20): trash in the destructive tints, Keep / Clear in destructive. */
@Composable
@ReadOnlyComposable
fun clearRecentsSpec(count: Int): ConfirmSpec {
    val c = OcTheme.colors
    return ConfirmSpec(
        icon = LucideIcon.Trash,
        tint = ToolTint(bg = c.destructiveTint, fg = c.destructive),
        title = stringResource(R.string.clear_recents_title),
        body = pluralText(R.plurals.clear_recents_body, count).asString(),
        cancelLabel = stringResource(R.string.clear_recents_keep),
        confirmLabel = stringResource(R.string.clear_recents_clear),
        destructive = true,
    )
}

/** The Settings bar: 56 dp, back 40/11 with arrow-left 22 ink, title17. */
@Composable
private fun SettingsBar(onBack: () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TopBarIconButton(
            icon = LucideIcon.ArrowLeft,
            contentDescription = stringResource(R.string.cd_back),
            onClick = onBack,
            tint = c.ink,
            iconSize = 22.dp,
            radius = 11.dp,
        )
        Text(text = stringResource(R.string.settings_title), style = OcTheme.type.title17, color = c.ink, maxLines = 1)
    }
}

/** Access card: white, 1 px border, radius 18, padding 18; bell 22 in the messages tint when granted, amber otherwise. */
@Composable
private fun AccessCard(granted: Boolean, onClick: () -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconBox(
            icon = LucideIcon.Bell,
            tint = if (granted) c.toolMessages else ToolTint(bg = c.amberTint, fg = c.amber),
            size = 46.dp,
            radius = 14.dp,
            iconSize = 22.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.settings_access), style = OcTheme.type.title16, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(if (granted) R.string.settings_access_on else R.string.settings_access_off),
                style = OcTheme.type.body13,
                color = c.ink2,
            )
        }
        LucideIconImage(icon = LucideIcon.ChevronRight, size = 18.dp, tint = c.chevron)
    }
}

/** Eyebrow, 8 dp, then the card with [rows]. */
@Composable
private fun SettingsSection(title: String, rows: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionEyebrow(text = title)
        Spacer(Modifier.height(8.dp))
        CardColumn(content = rows)
    }
}

/** A card row with a 20 dp Lucide glyph in inkMuted (chevron colour when disabled). */
@Composable
private fun SettingsRow(
    icon: LucideIcon,
    title: String,
    onClick: () -> Unit,
    value: String? = null,
    trailing: LucideIcon = LucideIcon.ChevronRight,
    enabled: Boolean = true,
) {
    val c = OcTheme.colors
    SettingsRow(
        title = title,
        onClick = onClick,
        value = value,
        trailing = trailing,
        enabled = enabled,
        leading = { LucideIconImage(icon = icon, size = 20.dp, tint = if (enabled) c.inkMuted else c.chevron) },
    )
}

/**
 * Card row: padding 15, gap 13, [leading] glyph, title label14_5, optional value 13.5 muted, [trailing]
 * 18 chevron colour. Not [enabled]: no tap, title in muted, no trailing icon.
 */
@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    value: String? = null,
    trailing: LucideIcon = LucideIcon.ChevronRight,
    enabled: Boolean = true,
) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        leading()
        Text(text = title, style = OcTheme.type.label14_5, color = if (enabled) c.ink else c.muted, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = OcTheme.type.body13_5, color = c.muted)
        }
        if (enabled) {
            LucideIconImage(icon = trailing, size = 18.dp, tint = c.chevron)
        }
    }
}

/** "Version" with the label in mono12 muted, isolated left to right so it keeps its order in a right-to-left layout; plain, not tappable, no chevron. */
@Composable
private fun VersionRow(version: String) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        LucideIconImage(icon = LucideIcon.Info, size = 20.dp, tint = c.inkMuted)
        Text(text = stringResource(R.string.settings_version), style = OcTheme.type.label14_5, color = c.ink, modifier = Modifier.weight(1f))
        Text(text = ltr(version), style = OcTheme.type.mono12, color = c.muted)
    }
}
