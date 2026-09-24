package com.piptechnologies.openchat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.GhostButton
import com.piptechnologies.openchat.ui.components.IconBox
import com.piptechnologies.openchat.ui.components.OcModalSheet
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.pluralText
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint
import com.piptechnologies.openchat.ui.theme.sans

private const val STAR_COUNT = 5

/** "How can we do better?" is 19/700 with the sheet titles' −.01em tracking (title19 is the 800 brand style). */
private val FeedbackTitle = sans(19.0, FontWeight.Bold, letterSpacing = (-0.01).em)

/**
 * The rating sheet as the app shows it (design map §4.19): a modal sheet with the confirmation radius
 * and no system handle, [RatingSheetContent] inside. Back and a tap on the scrim call [onClose].
 */
@Composable
fun RatingSheet(
    state: RatingState,
    onStar: (Int) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSendFeedback: () -> Unit,
    onRateOnPlay: () -> Unit,
    onClose: () -> Unit,
) {
    OcModalSheet(onDismissRequest = onClose, topRadius = OcRadius.dialog, showHandle = false) {
        RatingSheetContent(
            state = state,
            onStar = onStar,
            onFeedbackChange = onFeedbackChange,
            onSendFeedback = onSendFeedback,
            onRateOnPlay = onRateOnPlay,
            onClose = onClose,
        )
    }
}

/**
 * The four stages of the rating sheet (design map §4.19), stateless so screenshots render it in a
 * [com.piptechnologies.openchat.ui.components.SheetPreviewFrame]. Padding 10 22 24 with the sheet's
 * own 38×5 handle; STARS → [onStar] / [onClose] "Maybe later"; STORE → [onRateOnPlay] / [onClose]
 * "Not now"; FEEDBACK → [onFeedbackChange], [onSendFeedback] (disabled until there is text) / [onClose]
 * "Cancel"; THANKS → [onClose] "Done".
 */
@Composable
fun RatingSheetContent(
    state: RatingState,
    onStar: (Int) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSendFeedback: () -> Unit,
    onRateOnPlay: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 10.dp, end = 22.dp, bottom = 24.dp),
    ) {
        RatingHandle()
        when (state.stage) {
            RatingStage.STARS -> StarsStage(rating = state.rating, onStar = onStar, onLater = onClose)
            RatingStage.STORE -> StoreStage(onRateOnPlay = onRateOnPlay, onNotNow = onClose)
            RatingStage.FEEDBACK -> FeedbackStage(
                feedback = state.feedback,
                onFeedbackChange = onFeedbackChange,
                onSend = onSendFeedback,
                onCancel = onClose,
            )
            RatingStage.THANKS -> ThanksStage(onDone = onClose)
        }
    }
}

/** The rating sheet's handle: 38×5 in border, 2 dp above and 16 dp below, centred. */
@Composable
private fun ColumnScope.RatingHandle() {
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 2.dp, bottom = 16.dp)
            .size(width = 38.dp, height = 5.dp)
            .clip(RoundedCornerShape(OcRadius.pill))
            .background(OcTheme.colors.border),
    )
}

/** Icon box 64/19 greenTint star 32, title, body, five 38 stars (gold up to the rating), hint, "Maybe later". */
@Composable
private fun StarsStage(rating: Int, onStar: (Int) -> Unit, onLater: () -> Unit) {
    val c = OcTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBox(icon = LucideIcon.Star, tint = ToolTint(bg = c.greenTint, fg = c.green), size = 64.dp, radius = 19.dp, iconSize = 32.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.rating_title),
            style = OcTheme.type.title20,
            color = c.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.rating_body),
            style = OcTheme.type.body13_5,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 290.dp),
        )
        Row(
            modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (value in 1..STAR_COUNT) {
                val lit = value <= rating
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { onStar(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIconImage(
                        icon = LucideIcon.Star,
                        size = 38.dp,
                        tint = if (lit) c.gold else c.switchOff,
                        filled = lit,
                        contentDescription = pluralText(R.plurals.rating_star_cd, value).asString(),
                    )
                }
            }
        }
        Text(
            text = stringResource(hintFor(rating)),
            style = OcTheme.type.body11_5,
            color = c.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 16.dp),
        )
        Spacer(Modifier.height(14.dp))
        GhostButton(
            text = stringResource(R.string.rating_later),
            onClick = onLater,
            modifier = Modifier.fillMaxWidth(),
            height = 46.dp,
            textStyle = OcTheme.type.label14_5,
        )
    }
}

/** The hint under the stars: "Tap a star" before a tap, then one line per rating. */
private fun hintFor(rating: Int): Int = when (rating) {
    1 -> R.string.rating_hint_1
    2 -> R.string.rating_hint_2
    3 -> R.string.rating_hint_3
    4 -> R.string.rating_hint_4
    5 -> R.string.rating_hint_5
    else -> R.string.rating_hint_tap
}

/** Icon box successTint party-popper 30, five gold 22 stars, "Thank you!", body, primary 54/15 "Rate on Google Play", "Not now". */
@Composable
private fun StoreStage(onRateOnPlay: () -> Unit, onNotNow: () -> Unit) {
    val c = OcTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBox(icon = LucideIcon.PartyPopper, tint = ToolTint(bg = c.successTint, fg = c.successFg), size = 64.dp, radius = 19.dp, iconSize = 30.dp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(STAR_COUNT) {
                LucideIconImage(icon = LucideIcon.Star, size = 22.dp, tint = c.gold, filled = true)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.rating_store_title),
            style = OcTheme.type.title20,
            color = c.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.rating_store_body),
            style = OcTheme.type.body13_5,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 290.dp),
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.rating_store_cta),
            onClick = onRateOnPlay,
            height = 54.dp,
            radius = 15.dp,
            leading = { LucideIconImage(icon = LucideIcon.ExternalLink, size = 18.dp, tint = Color.White) },
        )
        Spacer(Modifier.height(10.dp))
        GhostButton(
            text = stringResource(R.string.rating_not_now),
            onClick = onNotNow,
            modifier = Modifier.fillMaxWidth(),
            height = 46.dp,
            textStyle = OcTheme.type.label14_5,
        )
    }
}

/** Icon box 52/15 greenTint message-square-text 24, title 19/700, body, 104-high field, "Send feedback" (disabled until text), "Cancel". */
@Composable
private fun FeedbackStage(feedback: String, onFeedbackChange: (String) -> Unit, onSend: () -> Unit, onCancel: () -> Unit) {
    val c = OcTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        IconBox(icon = LucideIcon.MessageSquareText, tint = ToolTint(bg = c.greenTint, fg = c.green), size = 52.dp, radius = 15.dp, iconSize = 24.dp)
        Spacer(Modifier.height(14.dp))
        Text(text = stringResource(R.string.rating_feedback_title), style = FeedbackTitle, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(text = stringResource(R.string.rating_feedback_body), style = OcTheme.type.body13_5, color = c.ink2)
        Spacer(Modifier.height(14.dp))
        FeedbackField(value = feedback, onValueChange = onFeedbackChange)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = stringResource(R.string.rating_feedback_send),
            onClick = onSend,
            enabled = feedback.isNotBlank(),
            leading = { LucideIconImage(icon = LucideIcon.Send, size = 16.dp, tint = Color.White) },
        )
        Spacer(Modifier.height(10.dp))
        GhostButton(
            text = stringResource(R.string.rating_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            height = 46.dp,
            textStyle = OcTheme.type.label14_5,
        )
    }
}

/**
 * The feedback field: 104 high, radius 14, 1 px border, padding 13 14, body14 ink, placeholder in the placeholder
 * colour. Typed text takes its direction from its own content, not the layout's (as on Contact us).
 */
@Composable
private fun FeedbackField(value: String, onValueChange: (String) -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        textStyle = OcTheme.type.body14.copy(color = c.ink, textDirection = TextDirection.Content),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        cursorBrush = SolidColor(c.green),
        decorationBox = { innerField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rating_feedback_placeholder),
                        style = OcTheme.type.body14,
                        color = c.placeholder,
                    )
                }
                innerField()
            }
        },
    )
}

/** Icon box successTint check 32 (sw 2.4), "Thank you, we hear you", body, primary "Done". */
@Composable
private fun ThanksStage(onDone: () -> Unit) {
    val c = OcTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBox(
                icon = LucideIcon.Check,
                tint = ToolTint(bg = c.successTint, fg = c.successFg),
                size = 64.dp,
                radius = 19.dp,
                iconSize = 32.dp,
                strokeWidth = 2.4f,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.rating_thanks_title),
                style = OcTheme.type.title20,
                color = c.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.rating_thanks_body),
                style = OcTheme.type.body13_5,
                color = c.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 290.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = stringResource(R.string.rating_done), onClick = onDone)
    }
}
