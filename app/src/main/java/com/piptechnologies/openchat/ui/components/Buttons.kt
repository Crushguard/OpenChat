package com.piptechnologies.openchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme

/** Primary 52 / 14: green, white 16/600. Disabled = the whole button at 45% opacity, as in the design. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    radius: Dp = 14.dp,
    textStyle: TextStyle = OcTheme.type.label16,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(c.green)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leading?.invoke()
            Text(text = text, style = textStyle, color = Color.White)
        }
    }
}

/** Secondary 48 / 13: subtle background, 1 px border, ink 14.5/600. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    radius: Dp = 13.dp,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(c.subtle)
            .border(1.dp, c.border, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leading?.invoke()
            Text(text = text, style = OcTheme.type.label14_5, color = c.ink)
        }
    }
}

/** Ghost: text only, ink 2, 14/600 in a 42 (or given) tall touch target. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    textStyle: TextStyle = OcTheme.type.label14,
    color: Color = OcTheme.colors.ink2,
    horizontalPadding: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(11.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = textStyle, color = color)
    }
}

/** Destructive outline 42 / 11: white, 1 px destructive border, destructive 14/600. */
@Composable
fun DestructiveOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.destructiveBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = OcTheme.type.label14, color = c.destructive)
    }
}

enum class IconSquareKind { Secondary, Destructive }

/** Icon square 52 / 14 (media detail Share and Delete). */
@Composable
fun IconSquareButton(
    icon: LucideIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: IconSquareKind = IconSquareKind.Secondary,
    size: Dp = 52.dp,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val bg = if (kind == IconSquareKind.Secondary) c.subtle else c.surface
    val border = if (kind == IconSquareKind.Secondary) c.border else c.destructiveBorder
    val tint = if (kind == IconSquareKind.Secondary) c.ink else c.destructive
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIconImage(icon = icon, size = 20.dp, tint = tint, contentDescription = contentDescription)
    }
}

/** 40 dp icon button with a 12 radius, used in every top bar. */
@Composable
fun TopBarIconButton(
    icon: LucideIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OcTheme.colors.inkSoft,
    iconSize: Dp = 20.dp,
    strokeWidth: Float = 1.75f,
    size: Dp = 40.dp,
    radius: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIconImage(icon = icon, size = iconSize, tint = tint, strokeWidth = strokeWidth, contentDescription = contentDescription)
    }
}

/** The two equal 50 / 13 buttons at the bottom of a confirmation sheet. */
@Composable
fun DialogButtonRow(
    cancelLabel: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmColor: Color = OcTheme.colors.green,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(13.dp)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(shape)
                .background(c.subtle)
                .border(1.dp, c.border, shape)
                .clickable(role = Role.Button, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) { Text(text = cancelLabel, style = OcTheme.type.label15, color = c.ink) }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(shape)
                .background(confirmColor)
                .clickable(role = Role.Button, onClick = onConfirm),
            contentAlignment = Alignment.Center,
        ) { Text(text = confirmLabel, style = OcTheme.type.label15, color = Color.White) }
    }
}
