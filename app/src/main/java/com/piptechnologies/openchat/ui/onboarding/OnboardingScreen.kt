package com.piptechnologies.openchat.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.DialCountry
import com.piptechnologies.openchat.ui.components.CardColumn
import com.piptechnologies.openchat.ui.components.GhostButton
import com.piptechnologies.openchat.ui.components.HairlineDivider
import com.piptechnologies.openchat.ui.components.HonestyLine
import com.piptechnologies.openchat.ui.components.IconBox
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.icons.AppGlyph
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LUCIDE_STROKE
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

private const val SLIDE_COUNT = 2

/** The country the slide 1 illustration shows: Indonesia, as in the design. */
private val SampleCountry: DialCountry =
    DialCountries.byIso2("ID") ?: DialCountry(iso2 = "ID", name = "Indonesia", dialCode = "62")

/** rgb(20,22,28): the soft drop shadow under the slide illustrations. */
private val IllustrationShadow = Color(0xFF14161C)

/**
 * Onboarding (design map §4.2), [slide] 0 or 1. Skip (slide 0 only) and the primary button report
 * through [onSkip] and [onNext]; the route decides what they do.
 */
@Composable
fun OnboardingScreen(slide: Int, onSkip: () -> Unit, onNext: () -> Unit) {
    val first = slide == 0
    ScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 26.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (first) {
                    GhostButton(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = onSkip,
                        height = 36.dp,
                        textStyle = OcTheme.type.label13_5,
                        horizontalPadding = 12.dp,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (first) {
                    NumberEntryIllustration()
                    SlideText(
                        title = stringResource(R.string.onboarding_1_title),
                        body = stringResource(R.string.onboarding_1_body),
                    )
                } else {
                    ToolsIllustration()
                    SlideText(
                        title = stringResource(R.string.onboarding_2_title),
                        body = stringResource(R.string.onboarding_2_body),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            DotPager(selected = slide, count = SLIDE_COUNT)
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                text = stringResource(if (first) R.string.onboarding_next else R.string.onboarding_start),
                onClick = onNext,
            )
            Spacer(Modifier.height(14.dp))
            HonestyLine(text = stringResource(R.string.splash_honesty))
        }
    }
}

/**
 * Holds the slide. Next moves to slide 1; Skip and Get started mark onboarding done, then [onDone].
 * Back on slide 1 returns to slide 0.
 */
@Composable
fun OnboardingRoute(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var slide by rememberSaveable { mutableIntStateOf(0) }
    val finish: () -> Unit = {
        viewModel.markDone()
        onDone()
    }
    BackHandler(enabled = slide > 0) { slide = 0 }
    OnboardingScreen(
        slide = slide,
        onSkip = finish,
        onNext = {
            if (slide < SLIDE_COUNT - 1) {
                slide += 1
            } else {
                finish()
            }
        },
    )
}

/** Title display26 inkStrong and body15 inkMuted, centred, 10 apart. */
@Composable
private fun SlideText(title: String, body: String) {
    val c = OcTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = OcTheme.type.display26,
            color = c.inkStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = OcTheme.type.body15,
            color = c.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Slide 1: Home's phone field (laid out left to right in every language, as Home lays it out), split Send
 * button and "not added" line, static and not interactive.
 */
@Composable
private fun NumberEntryIllustration() {
    val c = OcTheme.colors
    val fieldShape = RoundedCornerShape(OcRadius.md)
    Column(
        modifier = Modifier
            .widthIn(max = 342.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Like Home's phone field, the sample field reads left to right in every language: the dial code, then
        // the digits. Its texts are then left-to-right paragraphs, so "+62" and "812 3456 7890" keep their order
        // in a right-to-left layout too.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(elevation = 10.dp, shape = fieldShape, ambientColor = Color.Transparent, spotColor = IllustrationShadow)
                    .background(c.surface, fieldShape)
                    .border(1.dp, c.border, fieldShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(OcRadius.sm))
                        .background(c.subtle)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    FlagEmoji(country = SampleCountry)
                    Text(text = SampleCountry.dialLabel, style = OcTheme.type.mono16, color = c.ink)
                    LucideIconImage(icon = LucideIcon.ChevronDown, size = 16.dp, tint = c.ink2, strokeWidth = 2f)
                }
                Text(
                    text = stringResource(R.string.onboarding_1_sample_number),
                    style = OcTheme.type.mono22,
                    color = c.ink,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SplitSendButton()
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LucideIconImage(icon = LucideIcon.Check, size = 16.dp, tint = c.green, strokeWidth = 2.4f)
            Text(text = stringResource(R.string.onboarding_1_check), style = OcTheme.type.body12_5, color = c.ink2)
        }
    }
}

/** Flag emoji (ruling R8) centred in the chip's 26×18 slot. */
@Composable
private fun FlagEmoji(country: DialCountry) {
    Box(
        modifier = Modifier.size(width = 26.dp, height = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = country.flagEmoji,
            style = TextStyle(fontSize = 19.sp),
            modifier = Modifier.wrapContentSize(unbounded = true),
        )
    }
}

/** The split button: green "Send Message" segment (14/6 radii), 2 dp gap, 62-wide greenTint segment with the WhatsApp glyph. */
@Composable
private fun SplitSendButton() {
    val c = OcTheme.colors
    val leadingShape = RoundedCornerShape(
        topStart = OcRadius.md,
        topEnd = OcRadius.xs,
        bottomEnd = OcRadius.xs,
        bottomStart = OcRadius.md,
    )
    val trailingShape = RoundedCornerShape(
        topStart = OcRadius.xs,
        topEnd = OcRadius.md,
        bottomEnd = OcRadius.md,
        bottomStart = OcRadius.xs,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(c.green, leadingShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.onboarding_1_send), style = OcTheme.type.label16, color = Color.White)
        }
        Row(
            modifier = Modifier
                .width(62.dp)
                .fillMaxHeight()
                .background(c.greenTint, trailingShape)
                .border(1.dp, c.greenTintBorder, trailingShape),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppGlyphImage(glyph = AppGlyph.WhatsApp, size = 22.dp, tint = c.green)
            LucideIconImage(icon = LucideIcon.ChevronDown, size = 14.dp, tint = c.green, strokeWidth = 2.2f)
        }
    }
}

private class ToolPreview(
    @StringRes val title: Int,
    val icon: LucideIcon,
    val tint: ToolTint,
    val strokeWidth: Float = LUCIDE_STROKE,
)

/** Slide 2: a 300-wide card with the four tool rows (icon box 40/12, title 14.5/600, chevron), no status lines. */
@Composable
private fun ToolsIllustration() {
    val c = OcTheme.colors
    val tools = listOf(
        ToolPreview(R.string.tool_unseen_title, LucideIcon.CheckCheck, c.toolTicks, strokeWidth = 2f),
        ToolPreview(R.string.tool_deleted_title, LucideIcon.ArchiveRestore, c.toolMessages),
        ToolPreview(R.string.tool_media_title, LucideIcon.Image, c.toolMedia),
        ToolPreview(R.string.tool_second_title, LucideIcon.QrCode, c.toolSecond),
    )
    CardColumn(
        modifier = Modifier
            .width(300.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(OcRadius.card),
                ambientColor = Color.Transparent,
                spotColor = IllustrationShadow,
            ),
    ) {
        tools.forEachIndexed { index, tool ->
            if (index > 0) HairlineDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                IconBox(
                    icon = tool.icon,
                    tint = tool.tint,
                    size = 40.dp,
                    radius = 12.dp,
                    iconSize = 20.dp,
                    strokeWidth = tool.strokeWidth,
                )
                Text(
                    text = stringResource(tool.title),
                    style = OcTheme.type.label14_5,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                LucideIconImage(icon = LucideIcon.ChevronRight, size = 18.dp, tint = c.chevron)
            }
        }
    }
}

/** Dot pager: 8 high pills 6 apart; the current one 22 wide in green, the others 8 wide in switchOff. */
@Composable
private fun DotPager(selected: Int, count: Int) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val active = index == selected
            val width by animateDpAsState(targetValue = if (active) 22.dp else 8.dp, label = "OnboardingDotWidth")
            val color by animateColorAsState(targetValue = if (active) c.green else c.switchOff, label = "OnboardingDotColor")
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(OcRadius.pill))
                    .background(color),
            )
        }
    }
}
