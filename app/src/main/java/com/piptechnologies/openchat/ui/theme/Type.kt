package com.piptechnologies.openchat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.piptechnologies.openchat.R

val HankenGrotesk: FontFamily = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
    Font(R.font.hanken_grotesk_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMono: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)
private val CenteredLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    family: FontFamily,
    size: Double,
    weight: FontWeight,
    lineHeight: Double?,
    letterSpacing: TextUnit,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = if (lineHeight == null) TextUnit.Unspecified else lineHeight.sp,
    letterSpacing = letterSpacing,
    platformStyle = NoFontPadding,
    lineHeightStyle = CenteredLines,
)

/** Hanken Grotesk text style. Sizes are the design's px values used 1:1 as sp. */
fun sans(
    size: Double,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Double? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
): TextStyle = style(HankenGrotesk, size, weight, lineHeight, letterSpacing)

/** JetBrains Mono text style, used for numbers, counts, times and eyebrows. */
fun mono(
    size: Double,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Double? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
): TextStyle = style(JetBrainsMono, size, weight, lineHeight, letterSpacing)

/** Every text style the screens use, named after the design (size / weight). */
@Immutable
data class OcTypography(
    val display26: TextStyle = sans(26.0, FontWeight.ExtraBold, lineHeight = 30.0, letterSpacing = (-0.02).em),
    val display24: TextStyle = sans(24.0, FontWeight.ExtraBold, letterSpacing = (-0.02).em),
    val display22: TextStyle = sans(22.0, FontWeight.ExtraBold, lineHeight = 26.4, letterSpacing = (-0.02).em),
    val title20: TextStyle = sans(20.0, FontWeight.Bold, letterSpacing = (-0.01).em),
    val title19: TextStyle = sans(19.0, FontWeight.ExtraBold, letterSpacing = (-0.02).em),
    val title18: TextStyle = sans(18.0, FontWeight.Bold, lineHeight = 23.4),
    val title17: TextStyle = sans(17.0, FontWeight.Bold),
    val title16: TextStyle = sans(16.0, FontWeight.Bold),
    val title15: TextStyle = sans(15.0, FontWeight.Bold),
    val title14: TextStyle = sans(14.0, FontWeight.Bold),
    val label16: TextStyle = sans(16.0, FontWeight.SemiBold),
    val label15: TextStyle = sans(15.0, FontWeight.SemiBold),
    val label14_5: TextStyle = sans(14.5, FontWeight.SemiBold),
    val label14: TextStyle = sans(14.0, FontWeight.SemiBold),
    val label13_5: TextStyle = sans(13.5, FontWeight.SemiBold),
    val label13: TextStyle = sans(13.0, FontWeight.SemiBold),
    val label12_5: TextStyle = sans(12.5, FontWeight.SemiBold),
    val label12: TextStyle = sans(12.0, FontWeight.SemiBold),
    val badge10_5: TextStyle = sans(10.5, FontWeight.Bold),
    val body17: TextStyle = sans(17.0, FontWeight.Normal),
    val body15: TextStyle = sans(15.0, FontWeight.Normal, lineHeight = 23.25),
    val body14_5: TextStyle = sans(14.5, FontWeight.Normal, lineHeight = 20.3),
    val body14: TextStyle = sans(14.0, FontWeight.Normal, lineHeight = 21.0),
    val body13_5: TextStyle = sans(13.5, FontWeight.Normal, lineHeight = 20.25),
    val body13: TextStyle = sans(13.0, FontWeight.Normal, lineHeight = 18.2),
    val body12_5: TextStyle = sans(12.5, FontWeight.Normal, lineHeight = 18.75),
    val body12: TextStyle = sans(12.0, FontWeight.Normal, lineHeight = 18.0),
    val body11_5: TextStyle = sans(11.5, FontWeight.Normal),
    val mono22: TextStyle = mono(22.0, FontWeight.Medium, letterSpacing = 0.01.em),
    val mono16: TextStyle = mono(16.0, FontWeight.Medium),
    val mono14_5: TextStyle = mono(14.5, FontWeight.Medium),
    val mono14: TextStyle = mono(14.0, FontWeight.Medium),
    val mono12_5: TextStyle = mono(12.5, FontWeight.Medium),
    val mono12: TextStyle = mono(12.0, FontWeight.Normal),
    val mono11_5: TextStyle = mono(11.5, FontWeight.Normal),
    val mono11: TextStyle = mono(11.0, FontWeight.Normal),
    val mono10_5: TextStyle = mono(10.5, FontWeight.Normal),
    val mono10: TextStyle = mono(10.0, FontWeight.Normal),
    val tileTime9_5: TextStyle = mono(9.5, FontWeight.Medium),
    val eyebrow11: TextStyle = mono(11.0, FontWeight.Bold, letterSpacing = 0.08.em),
    val eyebrow10: TextStyle = mono(10.0, FontWeight.SemiBold, letterSpacing = 0.08.em),
    val eyebrow9_5: TextStyle = mono(9.5, FontWeight.SemiBold, letterSpacing = 0.06.em),
    val eyebrow9: TextStyle = mono(9.0, FontWeight.SemiBold, letterSpacing = 0.06.em),
)

val LocalOcTypography = staticCompositionLocalOf { OcTypography() }

/** Material 3 slots mapped onto the design's styles so stock components read as part of the system. */
fun m3Typography(t: OcTypography): Typography = Typography(
    displayLarge = sans(34.0, FontWeight.ExtraBold, lineHeight = 38.0, letterSpacing = (-0.025).em),
    displayMedium = t.display26,
    displaySmall = t.display24,
    headlineLarge = t.display26,
    headlineMedium = t.display24,
    headlineSmall = t.display22,
    titleLarge = t.title18,
    titleMedium = t.title16,
    titleSmall = t.label14_5,
    bodyLarge = t.body15,
    bodyMedium = t.body14_5,
    bodySmall = t.body13,
    labelLarge = t.label16,
    labelMedium = t.label13,
    labelSmall = t.eyebrow11,
)
