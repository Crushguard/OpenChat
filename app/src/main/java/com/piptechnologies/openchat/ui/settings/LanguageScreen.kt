package com.piptechnologies.openchat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.CardColumn
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.HairlineDivider
import com.piptechnologies.openchat.ui.components.MonoTag
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.rememberToastHostState
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * Language (design map §4.17): the bar, the intro line and a card with one 58 dp row per language of
 * [Languages.all] (native name over its name in the language the UI is shown in, "RTL" tag for the
 * right-to-left ones, a green check on the one matching [current], a BCP-47 tag). A tap reports [onPick].
 */
@Composable
fun LanguageScreen(current: String, onBack: () -> Unit, onPick: (LanguageOption) -> Unit) {
    val c = OcTheme.colors
    val selectedTag = Languages.byTag(current).tag
    val uiLocale = Languages.uiLocale(LocalConfiguration.current)
    val names = remember(uiLocale) { Languages.all.map { Languages.displayName(it, uiLocale) } }
    ScreenSurface {
        OcTopBar(title = stringResource(R.string.language_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(text = stringResource(R.string.language_intro), style = OcTheme.type.body13_5, color = c.muted)
            Spacer(Modifier.height(12.dp))
            CardColumn {
                Languages.all.forEachIndexed { index, option ->
                    if (index > 0) HairlineDivider()
                    LanguageRow(option = option, name = names[index], selected = option.tag == selectedTag, onClick = { onPick(option) })
                }
            }
        }
    }
}

/** Row 58: padding 0 15, gap 12; native label14_5 ink over [name] (in the UI language) body11_5 hint; MonoTag "RTL"; check 20 (sw 2.2) green when selected. */
@Composable
private fun LanguageRow(option: LanguageOption, name: String, selected: Boolean, onClick: () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = option.native, style = OcTheme.type.label14_5, color = c.ink, maxLines = 1)
            Spacer(Modifier.height(1.dp))
            Text(text = name, style = OcTheme.type.body11_5, color = c.hint, maxLines = 1)
        }
        if (option.rtl) {
            MonoTag(text = stringResource(R.string.language_rtl))
        }
        if (selected) {
            LucideIconImage(icon = LucideIcon.Check, size = 20.dp, tint = c.green, strokeWidth = 2.2f)
        }
    }
}

/**
 * Binds [LanguageViewModel] to [LanguageScreen]. The checked row is the language in effect,
 * [Languages.current], read here rather than kept in the ViewModel: a switch recreates the activity and
 * this composition with it, while the ViewModel outlives both. The "Language: …" toast is resolved in
 * this composition's language and goes to the app-level toast host (own host when none is provided).
 */
@Composable
fun LanguageRoute(onBack: () -> Unit, viewModel: LanguageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val current = remember(configuration) { Languages.current(context) }
    val toast = LocalToastHost.current ?: rememberToastHostState()
    LaunchedEffect(viewModel, toast, context) {
        viewModel.toasts.collect { toast.show(it.asString(context)) }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LanguageScreen(current = current.tag, onBack = onBack, onPick = viewModel::pick)
        if (LocalToastHost.current == null) DarkToastHost(state = toast)
    }
}
