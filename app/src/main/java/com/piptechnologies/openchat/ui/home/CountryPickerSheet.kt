package com.piptechnologies.openchat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.DialCountry
import com.piptechnologies.openchat.platform.CountrySource
import com.piptechnologies.openchat.platform.DetectedCountry
import com.piptechnologies.openchat.ui.components.SectionEyebrow
import com.piptechnologies.openchat.ui.components.TopBarIconButton
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import java.text.Normalizer
import java.util.Locale

/** Height of a country row. */
private val CountryRowHeight = 54.dp

/**
 * The country sheet's content (design map §4.4), below the sheet handle; it fills the height it is
 * given. "Country" title 18 with a close x, the search field (always drawn with the focused green
 * border), "All countries" / "N results", the countries as 54 high card rows (flag 30×20, name, dial
 * code, green check on [current]) and, under the card, where the default country came from
 * ([detected]). A blank [query] lists every country with [current] and then the detected one first.
 */
@Composable
fun CountryPickerSheetContent(
    query: String,
    current: DialCountry,
    detected: DetectedCountry?,
    onQuery: (String) -> Unit,
    onPick: (DialCountry) -> Unit,
    onClose: () -> Unit,
) {
    val c = OcTheme.colors
    val countries = remember(query, current, detected) { countryPickerList(query, current, detected?.country) }
    val eyebrow = when {
        query.isBlank() -> stringResource(R.string.country_all)
        countries.size == 1 -> stringResource(R.string.country_result_one)
        else -> stringResource(R.string.country_results, countries.size)
    }
    val detectedLine = detected?.let { detectedLabel(it) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.country_title),
                style = OcTheme.type.title18,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
            TopBarIconButton(
                icon = LucideIcon.X,
                contentDescription = stringResource(R.string.country_close_cd),
                onClick = onClose,
                tint = c.ink2,
                iconSize = 18.dp,
                // The design pulls the 40 dp button 8 dp into the sheet's side padding.
                modifier = Modifier.offset(x = 8.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        CountrySearchField(query = query, onQuery = onQuery)
        Spacer(Modifier.height(16.dp))
        SectionEyebrow(text = eyebrow)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            itemsIndexed(countries, key = { _, country -> country.iso2 }) { index, country ->
                CountryRow(
                    country = country,
                    selected = country.iso2 == current.iso2,
                    index = index,
                    count = countries.size,
                    onPick = onPick,
                )
            }
            if (detectedLine != null) {
                item(key = "detected") {
                    Text(
                        text = detectedLine,
                        style = OcTheme.type.body12,
                        color = c.hint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                    )
                }
            }
        }
    }
}

/** "Detected from your SIM: Indonesia · +62" (network / locale by [DetectedCountry.source]). */
@Composable
private fun detectedLabel(detected: DetectedCountry): String {
    val res = when (detected.source) {
        CountrySource.SIM -> R.string.country_detected_sim
        CountrySource.NETWORK -> R.string.country_detected_network
        CountrySource.LOCALE -> R.string.country_detected_locale
    }
    return stringResource(res, detected.country.name, detected.country.dialLabel)
}

/** Search 44 / 12 with a 1 px green border: search 18 muted, then the query in body 15. */
@Composable
private fun CountrySearchField(query: String, onQuery: (String) -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val style = OcTheme.type.body15.copy(color = c.ink)
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(c.green),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(c.surface, shape)
                    .border(1.dp, c.green, shape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                LucideIconImage(icon = LucideIcon.Search, size = 18.dp, tint = c.muted)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(text = stringResource(R.string.country_search), style = style, color = c.placeholder, maxLines = 1)
                    }
                    innerTextField()
                }
            }
        },
    )
}

/** A 54 high country row: flag 30×20, name 14.5/600, dial code mono 14 in ink 2, check 18 when [selected]. */
@Composable
private fun CountryRow(country: DialCountry, selected: Boolean, index: Int, count: Int, onPick: (DialCountry) -> Unit) {
    val c = OcTheme.colors
    val first = index == 0
    val last = index == count - 1
    val corner = OcRadius.card
    val shape = RoundedCornerShape(
        topStart = if (first) corner else 0.dp,
        topEnd = if (first) corner else 0.dp,
        bottomEnd = if (last) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CountryRowHeight)
            .cardSlice(index = index, count = count, border = c.borderSoft, hairline = c.hairline)
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(country) })
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlagEmoji(country = country, width = 30.dp, height = 20.dp, fontSize = 21.sp)
        Text(
            text = country.name,
            style = OcTheme.type.label14_5,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(text = country.dialLabel, style = OcTheme.type.mono14, color = c.ink2, maxLines = 1)
        if (selected) {
            LucideIconImage(icon = LucideIcon.Check, size = 18.dp, tint = c.green, strokeWidth = 2.2f)
        }
    }
}

/**
 * Draws row [index] of a [count]-row card of equal-height rows: this row's slice of the card's
 * 1 dp rounded border (the card outline drawn relative to this row, clipped to it) and a hairline
 * under every row but the last. Lets a lazy list look like one card without composing every row.
 */
private fun Modifier.cardSlice(index: Int, count: Int, border: Color, hairline: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val rowHeight = size.height
    if (index < count - 1) {
        val y = rowHeight - stroke / 2f
        drawLine(color = hairline, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke)
    }
    clipRect {
        drawRoundRect(
            color = border,
            topLeft = Offset(stroke / 2f, -index * rowHeight + stroke / 2f),
            size = Size(size.width - stroke, count * rowHeight - stroke),
            cornerRadius = CornerRadius(OcRadius.card.toPx()),
            style = Stroke(width = stroke),
        )
    }
}

/**
 * The picker's rows. A blank [query] lists every country with [current] and then [detected] pinned
 * first; otherwise the countries whose name starts with the query (ignoring case and accents) or whose
 * dial code starts with it (any "+" ignored, so "+" alone matches every code), in name order.
 */
internal fun countryPickerList(query: String, current: DialCountry, detected: DialCountry?): List<DialCountry> {
    val q = query.trim()
    if (q.isEmpty()) {
        val pinned = listOfNotNull(current, detected).distinctBy { it.iso2 }
        return pinned + DialCountries.all.filter { country -> pinned.none { it.iso2 == country.iso2 } }
    }
    val name = foldForSearch(q)
    val dial = q.replace("+", "")
    return DialCountries.all.filter { country ->
        foldForSearch(country.name).startsWith(name) || country.dialCode.startsWith(dial)
    }
}

/** Lower case without diacritics: "Åland Islands" → "aland islands", "Türkiye" → "turkiye". */
private fun foldForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)

private val CombiningMarks = Regex("\\p{Mn}+")
