package com.piptechnologies.openchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import java.text.NumberFormat

/** State chips: Off (amber dot), Active (green check), Paused (amber pause), Linked (green dot). */
enum class ChipState { Off, Active, Paused, Linked }

/**
 * State chip 32 (large, gate screen) or 28 (small, top bars). Text 13/600 or 12/600.
 * Amber means off or paused, never red; green means active or linked.
 */
@Composable
fun StateChip(state: ChipState, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = OcTheme.colors
    val amber = state == ChipState.Off || state == ChipState.Paused
    val bg = if (amber) c.amberTint else c.greenTint
    val border = if (amber) c.amberTintBorder else c.greenTintBorder
    val fg = if (amber) c.amber else c.green
    val label = stringResource(
        when (state) {
            ChipState.Off -> R.string.chip_off
            ChipState.Active -> R.string.chip_active
            ChipState.Paused -> R.string.chip_paused
            ChipState.Linked -> R.string.chip_linked
        },
    )
    val height = if (small) 28.dp else 32.dp
    val gap = when {
        state == ChipState.Off && !small -> 7.dp
        state == ChipState.Active && !small -> 6.dp
        else -> 5.dp
    }
    val startPad = when (state) {
        ChipState.Off -> if (small) 8.dp else 10.dp
        ChipState.Active -> if (small) 8.dp else 9.dp
        ChipState.Paused -> 8.dp
        ChipState.Linked -> 8.dp
    }
    val endPad = if (small) 10.dp else 12.dp
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(OcRadius.pill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(OcRadius.pill))
            .padding(start = startPad, end = endPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        when (state) {
            ChipState.Off -> Box(Modifier.size(8.dp).clip(CircleShape).background(c.amber))
            ChipState.Linked -> Box(Modifier.size(7.dp).clip(CircleShape).background(c.green))
            ChipState.Active -> LucideIconImage(
                icon = LucideIcon.Check,
                size = if (small) 14.dp else 16.dp,
                tint = fg,
                strokeWidth = if (small) 2.6f else 2.4f,
            )
            ChipState.Paused -> LucideIconImage(icon = LucideIcon.Pause, size = 12.dp, tint = fg, strokeWidth = 2.4f)
        }
        Text(text = label, style = if (small) OcTheme.type.label12 else OcTheme.type.label13, color = fg)
    }
}

/** Filter chip 36, pill: selected = green tint + green border + green text; else white, border, ink 2. */
@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.pill)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) c.greenTint else c.surface)
            .border(1.dp, if (selected) c.green else c.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = OcTheme.type.label13, color = if (selected) c.green else c.ink2)
    }
}

/** Unread badge 20: green pill, white 10.5/700. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(
        modifier = modifier
            .height(20.dp)
            .clip(RoundedCornerShape(OcRadius.pill))
            .background(c.green)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        // The UI language's digits, like the numbers in translated strings (e.g. Persian ۷).
        val locale = LocalConfiguration.current.locales[0]
        val digits = remember(count, locale) { NumberFormat.getIntegerInstance(locale).format(count) }
        Text(text = digits, style = OcTheme.type.badge10_5, color = androidx.compose.ui.graphics.Color.White)
    }
}

/** Small mono tag such as "RTL" or "AD": subtle background, 9/600 mono, radius 5. */
@Composable
fun MonoTag(text: String, modifier: Modifier = Modifier, bordered: Boolean = false) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.subtle)
            .then(if (bordered) Modifier.border(1.dp, c.border, shape) else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(text = text, style = OcTheme.type.eyebrow9, color = if (bordered) c.ink2 else c.muted)
    }
}
