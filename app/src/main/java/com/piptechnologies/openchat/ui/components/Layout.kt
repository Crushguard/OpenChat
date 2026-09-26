package com.piptechnologies.openchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint
import com.piptechnologies.openchat.ui.theme.forScriptOf

/** Full-screen canvas background with system-bar insets; every screen sits in one of these. */
@Composable
fun ScreenSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OcTheme.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        content = content,
    )
}

/**
 * Small top bar, 52 dp (56 with a subtitle): back 40/12, title 16/700, trailing actions.
 * Pass [onBack] = null for a bar without a back button (Home uses its own brand bar).
 */
@Composable
fun OcTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = OcTheme.type.title16,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle == null) 52.dp else 56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) {
            TopBarIconButton(
                icon = LucideIcon.ArrowLeft,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
                tint = c.ink,
                iconSize = 22.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = titleStyle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = OcTheme.type.mono11_5,
                    color = c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

/**
 * Uppercase mono eyebrow 11/700 (Settings' sections: 11/600), letter-spacing .08em, muted. Uppercased with the UI
 * language's rules (Turkish İ).
 */
@Composable
fun SectionEyebrow(text: String, modifier: Modifier = Modifier, style: TextStyle = OcTheme.type.eyebrow11) {
    val shown = text.uppercase(currentUiLocale())
    Text(
        text = shown,
        style = style.forScriptOf(shown),
        color = OcTheme.colors.muted,
        modifier = modifier.padding(horizontal = 2.dp),
    )
}

/** White card, 16 radius, 1 px soft border, clipped so rows and hairlines stay inside. */
@Composable
fun CardColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.borderSoft, shape),
        content = content,
    )
}

/** 1 px hairline between rows. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = OcTheme.colors.hairline)
}

/** Tinted icon box (tool rows 40/12, gate 64/20, settings 46/14, dialogs 44/13). */
@Composable
fun IconBox(icon: LucideIcon, tint: ToolTint, size: Dp, radius: Dp, iconSize: Dp, modifier: Modifier = Modifier, strokeWidth: Float = 1.75f) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(tint.bg),
        contentAlignment = Alignment.Center,
    ) {
        LucideIconImage(icon = icon, size = iconSize, tint = tint.fg, strokeWidth = strokeWidth)
    }
}

/** Info callout: subtle background, 14 radius, info icon 14 muted, text 12/1.5 ink 2 (12.5/1.5 on Second account). */
@Composable
fun InfoCallout(
    text: String,
    modifier: Modifier = Modifier,
    icon: LucideIcon = LucideIcon.Info,
    textStyle: TextStyle = OcTheme.type.body12,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.subtle2)
            .border(1.dp, c.calloutBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        LucideIconImage(icon = icon, size = 14.dp, tint = c.muted, modifier = Modifier.padding(top = 2.dp))
        Text(text = text, style = textStyle, color = c.ink2, modifier = Modifier.weight(1f))
    }
}

/** Green honesty callout used at the bottom of Settings. */
@Composable
fun HonestyCallout(text: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.greenTint)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LucideIconImage(icon = LucideIcon.HeartHandshake, size = 16.dp, tint = c.green, modifier = Modifier.padding(top = 1.dp))
        Text(text = text, style = OcTheme.type.body12_5, color = c.inkMuted, modifier = Modifier.weight(1f))
    }
}

/** "Free · No ads · No account" line: heart-handshake 12 + 12/400 hint. */
@Composable
fun HonestyLine(text: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIconImage(icon = LucideIcon.HeartHandshake, size = 12.dp, tint = c.hint)
        Text(text = text, style = OcTheme.type.body12, color = c.hint)
    }
}

/** One-line hint under a primary action: 11.5/400 hint, centred. */
@Composable
fun HintLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = OcTheme.type.body11_5,
        color = OcTheme.colors.hint,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Empty state: icon box 56/16 subtle, title 17/700, body 13.5 muted, 240 wide. */
@Composable
fun EmptyState(icon: LucideIcon, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val c = OcTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.subtle),
            contentAlignment = Alignment.Center,
        ) {
            LucideIconImage(icon = icon, size = 28.dp, tint = c.chevron)
        }
        Spacer(Modifier.height(14.dp))
        Text(text = title, style = OcTheme.type.title17, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(
            text = body,
            style = OcTheme.type.body13_5,
            color = c.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

/** Switch 48 x 28: green when on, switch-off grey when off, 22 white knob. */
@Composable
fun OcSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(OcRadius.pill))
            .background(if (checked) c.green else c.switchOff)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White),
        )
    }
}

/** Brand mark: green rounded square with the send-horizontal glyph in white. */
@Composable
fun BrandMark(size: Dp, radius: Dp, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(OcTheme.colors.green),
        contentAlignment = Alignment.Center,
    ) {
        // A logo keeps its orientation in right-to-left layouts.
        LucideIconImage(icon = LucideIcon.SendHorizontal, size = iconSize, tint = Color.White, strokeWidth = 2f, autoMirror = false)
    }
}
