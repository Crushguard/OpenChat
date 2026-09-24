package com.piptechnologies.openchat.ui.gate

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.ChipState
import com.piptechnologies.openchat.ui.components.HintLine
import com.piptechnologies.openchat.ui.components.IconBox
import com.piptechnologies.openchat.ui.components.OcSpinner
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.StateChip
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.uiText
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.navigation.GateTool
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * Notification access gate (design map §4.7). The centre block (icon box, title, body, Off/Active chip)
 * sits in the middle of the space between the bar and the bottom block. Before the grant the bottom block
 * is "Open settings" ([onOpenSettings]), which shows the spinner and "Waiting for Android…" while
 * [GateUiState.waitingForSystem]; after it, "Continue" ([onContinue]).
 */
@Composable
fun GateScreen(state: GateUiState, onBack: () -> Unit, onOpenSettings: () -> Unit, onContinue: () -> Unit) {
    val c = OcTheme.colors
    ScreenSurface {
        OcTopBar(title = state.title.asString(), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconBox(
                icon = LucideIcon.Bell,
                tint = c.toolMessages,
                size = 64.dp,
                radius = OcRadius.lg,
                iconSize = 30.dp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.gate_title),
                style = OcTheme.type.display22,
                color = c.inkStrong,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.gate_body),
                style = OcTheme.type.body15,
                color = c.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(18.dp))
            StateChip(state = if (state.granted) ChipState.Active else ChipState.Off)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp),
        ) {
            if (state.granted) {
                PrimaryButton(text = stringResource(R.string.gate_continue), onClick = onContinue)
                Spacer(Modifier.height(10.dp))
                HintLine(text = stringResource(R.string.gate_hint_after))
            } else {
                val waiting = state.waitingForSystem
                PrimaryButton(
                    text = stringResource(if (waiting) R.string.gate_waiting else R.string.gate_open_settings),
                    onClick = onOpenSettings,
                    leading = {
                        if (waiting) {
                            OcSpinner(size = 16.dp, color = Color.White)
                        } else {
                            LucideIconImage(icon = LucideIcon.ExternalLink, size = 18.dp, tint = Color.White)
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                HintLine(text = stringResource(R.string.gate_hint_before))
            }
        }
        // Ruling R2: no ad banner slot; the screen keeps the slot's 14 dp bottom padding instead.
        Spacer(Modifier.height(14.dp))
    }
}

/**
 * Binds [GateViewModel] to [GateScreen]. The bar title follows [tool]; the grant is re-read on every
 * resume, so coming back from the system settings flips the screen to Active. Continue reports [tool]
 * through [onContinue] so the caller can open the pending tool.
 */
@Composable
fun GateRoute(
    tool: GateTool,
    onBack: () -> Unit,
    onContinue: (GateTool) -> Unit,
    viewModel: GateViewModel = hiltViewModel(),
) {
    val title = uiText(barTitle(tool))
    LaunchedEffect(viewModel, title) { viewModel.setTitle(title) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    GateScreen(
        state = state,
        onBack = onBack,
        onOpenSettings = { viewModel.openSettings() },
        onContinue = { onContinue(tool) },
    )
}

/** Bar title: the tool the user came from, or "Notification access" when opened from Settings. */
@StringRes
private fun barTitle(tool: GateTool): Int = when (tool) {
    GateTool.UNSEEN, GateTool.DELETED_MESSAGES -> R.string.gate_bar_messages
    GateTool.MEDIA -> R.string.gate_bar_media
    GateTool.SETTINGS -> R.string.gate_bar_settings
}
