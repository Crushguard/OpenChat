package com.piptechnologies.openchat.ui.second

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.ChipState
import com.piptechnologies.openchat.ui.components.ConfirmSpec
import com.piptechnologies.openchat.ui.components.HintLine
import com.piptechnologies.openchat.ui.components.IconBox
import com.piptechnologies.openchat.ui.components.InfoCallout
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.StateChip
import com.piptechnologies.openchat.ui.components.TopBarIconButton
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.icons.LucideIconImage
import com.piptechnologies.openchat.ui.theme.OcRadius
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint

/**
 * Second account (design map §4.14). [SecondPhase.ENTRY] is the entry layout whose "Scan QR" calls [onScan].
 * LINKING and LINKED show the white top bar (status "Linking…" or the Linked chip, reload → [onReload],
 * log-out → [onAskLogout]) over the [web] slot, which takes the rest of the screen. The screen never creates
 * a WebView: the route passes the session view in through [web]; screenshots pass a hatched placeholder.
 */
@Composable
fun SecondAccountScreen(
    state: SecondUiState,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onReload: () -> Unit,
    onAskLogout: () -> Unit,
    web: @Composable (Modifier) -> Unit,
) {
    when (state.phase) {
        SecondPhase.ENTRY -> EntryContent(onBack = onBack, onScan = onScan)
        SecondPhase.LINKING, SecondPhase.LINKED -> SessionContent(
            linked = state.phase == SecondPhase.LINKED,
            onBack = onBack,
            onReload = onReload,
            onAskLogout = onAskLogout,
            web = web,
        )
    }
}

/**
 * Entry: bar "Second account"; centre (padding 0 28 12) icon box 64/20 second tint qr-code 30, title
 * display22, body body15 inkMuted max 300, smartphone callout 22 below; bottom "Scan QR" with qr-code 18
 * and the notification hint.
 */
@Composable
private fun EntryContent(onBack: () -> Unit, onScan: () -> Unit) {
    val c = OcTheme.colors
    ScreenSurface {
        OcTopBar(title = stringResource(R.string.second_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconBox(
                icon = LucideIcon.QrCode,
                tint = c.toolSecond,
                size = 64.dp,
                radius = OcRadius.lg,
                iconSize = 30.dp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.second_entry_title),
                style = OcTheme.type.display22,
                color = c.inkStrong,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.second_entry_body),
                style = OcTheme.type.body15,
                color = c.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(22.dp))
            InfoCallout(text = stringResource(R.string.second_entry_callout), icon = LucideIcon.Smartphone, textStyle = OcTheme.type.body12_5)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp),
        ) {
            PrimaryButton(
                text = stringResource(R.string.second_scan),
                onClick = onScan,
                leading = { LucideIconImage(icon = LucideIcon.QrCode, size = 18.dp, tint = Color.White) },
            )
            Spacer(Modifier.height(10.dp))
            HintLine(text = stringResource(R.string.second_entry_hint))
        }
        // Ruling R2: no ad banner slot; the screen keeps the slot's 14 dp bottom padding instead.
        Spacer(Modifier.height(14.dp))
    }
}

/**
 * Linking / linked: the bar on white with a 1 dp borderSoft bottom line (as the prototype draws it), the
 * status (mono11_5 muted "Linking…" or the small Linked chip), rotate-cw and log-out 20; then the [web]
 * slot at full remaining height.
 */
@Composable
private fun SessionContent(
    linked: Boolean,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onAskLogout: () -> Unit,
    web: @Composable (Modifier) -> Unit,
) {
    val c = OcTheme.colors
    ScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface),
        ) {
            OcTopBar(
                title = stringResource(R.string.second_title),
                onBack = onBack,
                actions = {
                    if (linked) {
                        StateChip(state = ChipState.Linked, small = true)
                    } else {
                        Text(
                            text = stringResource(R.string.second_linking),
                            style = OcTheme.type.mono11_5,
                            color = c.muted,
                            maxLines = 1,
                        )
                    }
                    TopBarIconButton(
                        icon = LucideIcon.RotateCw,
                        contentDescription = stringResource(R.string.second_reload),
                        onClick = onReload,
                    )
                    TopBarIconButton(
                        icon = LucideIcon.LogOut,
                        contentDescription = stringResource(R.string.second_log_out),
                        onClick = onAskLogout,
                    )
                },
            )
            HorizontalDivider(thickness = 1.dp, color = c.borderSoft)
        }
        web(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        // Ruling R2: no ad banner slot; the screen keeps the slot's 14 dp bottom padding instead.
        Spacer(Modifier.height(14.dp))
    }
}

/** The "Log out of the linked account?" confirmation (§4.20): log-out 22 in destructiveTint/destructive, Stay / Log out (destructive). */
@Composable
@ReadOnlyComposable
fun logoutSpec(): ConfirmSpec {
    val c = OcTheme.colors
    return ConfirmSpec(
        icon = LucideIcon.LogOut,
        tint = ToolTint(bg = c.destructiveTint, fg = c.destructive),
        title = stringResource(R.string.logout_title),
        body = stringResource(R.string.logout_body),
        cancelLabel = stringResource(R.string.logout_stay),
        confirmLabel = stringResource(R.string.logout_confirm),
        destructive = true,
    )
}
