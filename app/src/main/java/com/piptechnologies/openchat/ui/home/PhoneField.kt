package com.piptechnologies.openchat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.DialCountry
import com.piptechnologies.openchat.core.phone.PhoneNumberNormalizer
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * The number field, 60 / 14 (design map §4.3): country chip, the digits in mono 22 grouped 3-4-rest
 * as typed, and Paste (empty) or clear (filled) at the end. A 1.5 dp green ring shows while the
 * field has focus; a tap anywhere on the field focuses it. The value stays digits only; the
 * grouping is display-only ([PhoneGroupingTransformation]).
 *
 * The field keeps its own [TextFieldValue] so the caret is right: an edit typed here keeps its caret,
 * and digits that arrive from elsewhere (Paste, a refilled recent, clear) put it at the end
 * ([reconciledWith]). Every text change is passed on raw to [onDigitsChange], which filters it.
 */
@Composable
internal fun PhoneField(
    country: DialCountry,
    digits: String,
    onCountryClick: () -> Unit,
    onDigitsChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    var focused by remember { mutableStateOf(false) }
    var edited by remember { mutableStateOf(TextFieldValue(text = digits, selection = TextRange(digits.length))) }
    val value = if (edited.text == digits) edited else edited.reconciledWith(digits)
    // After a reconciliation (paste, refill, clear) the field state adopts the reconciled value, so a later refill of
    // the same digits does not resurrect a stale caret.
    SideEffect { if (edited != value) edited = value }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(c.surface, shape)
            .border(width = if (focused) 1.5.dp else 1.dp, color = if (focused) c.green else c.border, shape = shape)
            .pointerInput(focusRequester) { detectTapGestures { focusRequester.requestFocusSafely() } }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CountryChip(country = country, onClick = onCountryClick)
        BasicTextField(
            value = value,
            onValueChange = { next ->
                edited = next
                if (next.text != value.text) onDigitsChange(next.text)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            textStyle = OcTheme.type.mono22.copy(color = c.ink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            visualTransformation = PhoneGroupingTransformation,
            cursorBrush = SolidColor(c.green),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (digits.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_phone_placeholder),
                            style = OcTheme.type.body17,
                            color = c.placeholder,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (digits.isEmpty()) {
            PastePill(onClick = onPaste)
        } else {
            ClearButton(onClick = onClear)
        }
    }
}

/**
 * This field's value once [digits] (from the ViewModel) replaced its text. When [digits] is this
 * value's own text with what the digits-only rule drops removed (a typed "+" or "-", a 16th digit),
 * the caret stays after the same digits. Any other change (Paste, a refilled recent, clear, a paste
 * the ViewModel normalised) puts the caret at the end.
 */
internal fun TextFieldValue.reconciledWith(digits: String): TextFieldValue {
    val ownEdit = PhoneNumberNormalizer.digitsOnly(text).take(PhoneNumberNormalizer.MAX_DIGITS) == digits
    val caret = if (ownEdit) {
        PhoneNumberNormalizer.digitsOnly(text.take(selection.end)).length.coerceAtMost(digits.length)
    } else {
        digits.length
    }
    return TextFieldValue(text = digits, selection = TextRange(caret))
}

/**
 * Shows the digits grouped first 3, next 4, rest ("81234567890" → "812 3456 7890") while the value
 * stays digits only. The offset mapping accounts for the one or two inserted spaces and clamps every
 * result into range, so it is exact for the field's own digits and never throws on odd input.
 */
internal object PhoneGroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            text = AnnotatedString(PhoneNumberNormalizer.group(text.text)),
            offsetMapping = PhoneGroupingOffsetMapping(text.text.length),
        )
}

/** Offsets across the spaces [PhoneNumberNormalizer.group] inserts after the 3rd and the 7th character. */
internal class PhoneGroupingOffsetMapping(private val length: Int) : OffsetMapping {
    private val transformedLength = length + (if (length > 3) 1 else 0) + (if (length > 7) 1 else 0)

    override fun originalToTransformed(offset: Int): Int {
        val original = offset.coerceIn(0, length)
        val transformed = when {
            original <= 3 -> original
            original <= 7 -> original + 1
            else -> original + 2
        }
        return transformed.coerceIn(0, transformedLength)
    }

    override fun transformedToOriginal(offset: Int): Int {
        val transformed = offset.coerceIn(0, transformedLength)
        val original = when {
            transformed <= 3 -> transformed
            transformed <= 8 -> transformed - 1
            else -> transformed - 2
        }
        return original.coerceIn(0, length)
    }
}

/** Country chip 44 / 10 on subtle: flag, dial code mono 16, chevron-down 16. Opens the country sheet. */
@Composable
private fun CountryChip(country: DialCountry, onClick: () -> Unit) {
    val c = OcTheme.colors
    val label = stringResource(R.string.home_country_cd)
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(OcRadius.sm))
            .background(c.subtle)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        FlagEmoji(country = country, width = 26.dp, height = 18.dp, fontSize = 19.sp)
        Text(text = country.dialLabel, style = OcTheme.type.mono16, color = c.ink, maxLines = 1)
        LucideIconImage(icon = LucideIcon.ChevronDown, size = 16.dp, tint = c.ink2, strokeWidth = 2f)
    }
}

/** Paste pill 36: green tint and border, clipboard 14 + "Paste" 13/600 in green. */
@Composable
private fun PastePill(onClick: () -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.pill)
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(c.greenTint)
            .border(1.dp, c.greenTintBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LucideIconImage(icon = LucideIcon.Clipboard, size = 14.dp, tint = c.green, strokeWidth = 2f)
        Text(text = stringResource(R.string.home_paste), style = OcTheme.type.label13, color = c.green, maxLines = 1)
    }
}

/** Clear: a 36 subtle circle with x 18 in ink 2. */
@Composable
private fun ClearButton(onClick: () -> Unit) {
    val c = OcTheme.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(c.subtle)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIconImage(
            icon = LucideIcon.X,
            size = 18.dp,
            tint = c.ink2,
            contentDescription = stringResource(R.string.home_clear_cd),
        )
    }
}

/**
 * Message field: min 56 / 14, white, 1 px border, body 15 at line height 1.4 in ink, placeholder
 * "Message (optional)". The whole card is the text field's touch target and it grows with the text.
 */
@Composable
internal fun MessageField(message: String, onMessageChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    val style = OcTheme.type.body15.copy(color = c.ink, lineHeight = 21.sp)
    BasicTextField(
        value = message,
        onValueChange = onMessageChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        cursorBrush = SolidColor(c.green),
        decorationBox = { innerTextField ->
            // Padding 6 14 around the design's textarea, which adds 8 of its own above and below.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(c.surface, shape)
                    .border(1.dp, c.border, shape)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (message.isEmpty()) {
                    Text(text = stringResource(R.string.home_message_placeholder), style = style, color = c.placeholder)
                }
                innerTextField()
            }
        },
    )
}

/** A flag emoji (ruling R8) centred in a [width] × [height] slot: 26×18 in the chip, 30×20 in the picker. */
@Composable
internal fun FlagEmoji(country: DialCountry, width: Dp, height: Dp, fontSize: TextUnit) {
    Box(modifier = Modifier.size(width = width, height = height), contentAlignment = Alignment.Center) {
        Text(
            text = country.flagEmoji,
            style = TextStyle(fontSize = fontSize),
            modifier = Modifier.wrapContentSize(unbounded = true),
        )
    }
}

/** [FocusRequester.requestFocus] throws while the requester is not attached to a node; that case is a no-op here. */
internal fun FocusRequester.requestFocusSafely() {
    try {
        requestFocus()
    } catch (e: IllegalStateException) {
        // Not attached yet (or any more): nothing to focus.
    }
}
