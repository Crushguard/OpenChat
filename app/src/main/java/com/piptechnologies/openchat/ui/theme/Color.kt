package com.piptechnologies.openchat.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A tool's icon-box tint pair (background, foreground). */
@Immutable
data class ToolTint(val bg: Color, val fg: Color)

/**
 * Design tokens from the OpenChat design system (light only; the design has no dark mode).
 * Names follow the design: Canvas, Surface, Subtle, Ink, Ink 2, Border, Primary green, Attention amber,
 * Destructive, plus the four tool tints.
 */
@Immutable
data class OcColors(
    val canvas: Color = Color(0xFFFAFBFC),
    val surface: Color = Color(0xFFFFFFFF),
    val subtle: Color = Color(0xFFF3F5F8),
    val subtle2: Color = Color(0xFFF6F7F9),
    val ink: Color = Color(0xFF1E2128),
    val inkStrong: Color = Color(0xFF171A20),
    val ink2: Color = Color(0xFF626873),
    val inkSoft: Color = Color(0xFF3D4550),
    val inkMuted: Color = Color(0xFF565C67),
    val muted: Color = Color(0xFF8B929D),
    val hint: Color = Color(0xFF99A0AC),
    val placeholder: Color = Color(0xFFA2A9B4),
    val chevron: Color = Color(0xFFB4BAC4),
    val border: Color = Color(0xFFE7EAEF),
    val borderSoft: Color = Color(0xFFEEF0F4),
    val borderStrong: Color = Color(0xFFE1E5EB),
    val calloutBorder: Color = Color(0xFFEBEEF2),
    val hairline: Color = Color(0xFFF2F4F7),
    val borderDashed: Color = Color(0xFFD3D8E0),
    val switchOff: Color = Color(0xFFD8DCE3),
    val keyboardBg: Color = Color(0xFFE9EBEF),
    val green: Color = Color(0xFF0A7E3A),
    val greenDeep: Color = Color(0xFF086B31),
    val greenTint: Color = Color(0xFFDDFAE4),
    val greenTintBorder: Color = Color(0xFFACE8BE),
    val greenSoft: Color = Color(0xFF4CB98A),
    val successTint: Color = Color(0xFFE2F3EA),
    val successFg: Color = Color(0xFF2E9E6B),
    val amber: Color = Color(0xFFB8792A),
    val amberTint: Color = Color(0xFFFEFBF5),
    val amberTintBorder: Color = Color(0xFFF0DFC4),
    val destructive: Color = Color(0xFFC4553D),
    val destructiveTint: Color = Color(0xFFFBEFEC),
    val destructiveBorder: Color = Color(0xFFEEDAD3),
    val gold: Color = Color(0xFFE0A64B),
    /** rgba(20,22,28,.42): the scrim behind sheets (country picker, tool settings, default app). */
    val scrim: Color = Color(0x6B14161C),
    /** rgba(20,26,40,.4): the scrim behind the confirmation dialogs and the rating sheet (26 dp radius). */
    val dialogScrim: Color = Color(0x66141A28),
    val toolTicks: ToolTint = ToolTint(Color(0xFFD4F1D8), Color(0xFF1C8742)),
    val toolMessages: ToolTint = ToolTint(Color(0xFFD2EBFF), Color(0xFF1F74BF)),
    val toolMedia: ToolTint = ToolTint(Color(0xFFECE2FF), Color(0xFF7F5BB6)),
    val toolSecond: ToolTint = ToolTint(Color(0xFFC6F2F4), Color(0xFF008892)),
    val toolOrange: ToolTint = ToolTint(Color(0xFFFFDFD0), Color(0xFFB2511E)),
) {
    /** Avatar tints by design hue (150 green, 250 blue, 300 purple, 200 teal, 45 orange), used for initial avatars. */
    fun avatarTint(index: Int): ToolTint = when (Math.floorMod(index, 5)) {
        0 -> toolMessages
        1 -> toolTicks
        2 -> toolOrange
        3 -> toolMedia
        else -> toolSecond
    }
}

val LocalOcColors = staticCompositionLocalOf { OcColors() }
