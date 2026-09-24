package com.piptechnologies.openchat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.PhoneNumberNormalizer
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.ui.components.BrandMark
import com.piptechnologies.openchat.ui.components.ConfirmSpec
import com.piptechnologies.openchat.ui.components.HintLine
import com.piptechnologies.openchat.ui.components.TopBarIconButton
import com.piptechnologies.openchat.ui.components.ltr
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.icons.LucideIcon
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.ToolTint
import kotlin.math.roundToInt

/**
 * Home (design map §4.3). Stateless; the country sheet and confirmation sheet are rendered by
 * [HomeRoute] (modal) or the screenshot tests (SheetPreviewFrame), never here. The Send-with menu IS
 * rendered here as an inline overlay (ruling: no Popup, so screenshots capture it): a full-size
 * transparent catcher that closes it on any outside tap, and the card right-aligned 8 dp under the
 * split button.
 *
 * [appIcon] draws an app's icon in the Send-with menu (the design's line glyph by default;
 * [HomeRoute] passes the installed app's launcher icon, ruling R10). [focusRequester] is attached to
 * the number field so the caller can focus it (first run, refill, clear, "Edit number"); Send, the
 * app selector and the country chip take the focus off it, as the design closes the keyboard there.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
    appIcon: @Composable (MessagingApp, Dp, Color) -> Unit = { app, size, tint -> AppGlyphImage(app.glyph, size, tint) },
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val c = OcTheme.colors
    val focusManager = LocalFocusManager.current
    val root = remember { CoordinatesHolder() }
    // Bounds of the split button in this screen's coordinates: where the Send-with card hangs from.
    val menuAnchor = remember { mutableStateOf(IntRect.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .onPlaced { root.coordinates = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            BrandBar(onSettings = callbacks.onSettings)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 20.dp),
            ) {
                PhoneField(
                    country = state.country,
                    digits = state.nationalDigits,
                    onCountryClick = {
                        focusManager.clearFocus()
                        callbacks.onCountryChip()
                    },
                    onDigitsChange = callbacks.onDigitsChange,
                    onPaste = callbacks.onPaste,
                    onClear = callbacks.onClear,
                    focusRequester = focusRequester,
                )
                Spacer(Modifier.height(12.dp))
                MessageField(message = state.message, onMessageChange = callbacks.onMessageChange)
                Spacer(Modifier.height(12.dp))
                SplitSendButton(
                    app = state.app,
                    canSend = state.canSend,
                    onSend = {
                        focusManager.clearFocus()
                        callbacks.onSend()
                    },
                    onToggleMenu = {
                        focusManager.clearFocus()
                        callbacks.onToggleMenu()
                    },
                    modifier = Modifier.trackBounds(root, menuAnchor),
                )
                if (state.firstRun) {
                    // The design pulls the hint 2 dp up (12 gap, −2 margin).
                    Spacer(Modifier.height(10.dp))
                    HintLine(text = stringResource(R.string.home_first_run_hint))
                }
                if (state.recents.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    RecentsSection(
                        recents = state.recents,
                        nowMs = state.nowMs,
                        revealedId = state.revealedRecentId,
                        onTap = callbacks.onRecentTap,
                        onReveal = callbacks.onRecentReveal,
                        onDelete = callbacks.onRecentDelete,
                    )
                }
                Spacer(Modifier.height(20.dp))
                ToolRows(tools = state.tools, onTool = callbacks.onTool)
            }
        }
        if (state.menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null, onClick = callbacks.onDismissMenu),
            )
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Box(
                modifier = Modifier
                    // The anchor is in absolute pixels, so the card is placed from the top-left corner in both
                    // directions (the Box default, TopStart, is the top-right corner in RTL).
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset {
                        val anchor = menuAnchor.value
                        // The card's end edge lines up with the split button's end edge (its left edge in RTL).
                        val x = if (rtl) anchor.left else anchor.right - SendWithMenuWidth.roundToPx()
                        IntOffset(x, anchor.bottom + SendWithMenuGap.roundToPx())
                    },
            ) {
                SendWithMenuContent(
                    apps = state.availableApps,
                    current = state.app,
                    onPick = callbacks.onPickApp,
                    appIcon = appIcon,
                )
            }
        }
    }
}

/** Brand bar 52: mark 28/9 with glyph 15 and "OpenChat" 19/800 (ruling R1), gear 22 in a 44/12 button. */
@Composable
private fun BrandBar(onSettings: () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            BrandMark(size = 28.dp, radius = 9.dp, iconSize = 15.dp)
            Text(text = stringResource(R.string.app_name), style = OcTheme.type.title19, color = c.inkStrong, maxLines = 1)
        }
        TopBarIconButton(
            icon = LucideIcon.Settings,
            contentDescription = stringResource(R.string.home_settings_cd),
            onClick = onSettings,
            tint = c.inkSoft,
            iconSize = 22.dp,
            size = 44.dp,
        )
    }
}

/**
 * The not-on-WhatsApp confirmation (design map §4.6): message-circle-off in amber, "Edit number" / "Try Telegram" (green).
 * The number in the body stays one left-to-right unit inside right-to-left text.
 */
@Composable
fun notOnWhatsAppSpec(dialCode: String, national: String): ConfirmSpec {
    val c = OcTheme.colors
    return ConfirmSpec(
        icon = LucideIcon.MessageCircleOff,
        tint = ToolTint(bg = c.amberTint, fg = c.amber),
        title = stringResource(R.string.not_on_wa_title),
        body = stringResource(R.string.not_on_wa_body, ltr(PhoneNumberNormalizer.displayInternational(dialCode, national))),
        cancelLabel = stringResource(R.string.not_on_wa_edit),
        confirmLabel = stringResource(R.string.not_on_wa_telegram),
        destructive = false,
    )
}

/** The screen root's coordinates, captured while it is placed and read while its children are placed. */
private class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Keeps [bounds] equal to this element's bounds in [root]'s coordinates. [onPlaced] runs inside the
 * layout pass, before the overlay is placed later in that same pass, so the menu is right from its
 * first frame (screenshots included); [onGloballyPositioned] follows later moves such as scrolling.
 */
private fun Modifier.trackBounds(root: CoordinatesHolder, bounds: MutableState<IntRect>): Modifier {
    val update: (LayoutCoordinates) -> Unit = { coordinates ->
        val rootCoordinates = root.coordinates
        if (rootCoordinates != null && rootCoordinates.isAttached && coordinates.isAttached) {
            val topLeft = rootCoordinates.localPositionOf(coordinates, Offset.Zero)
            val left = topLeft.x.roundToInt()
            val top = topLeft.y.roundToInt()
            bounds.value = IntRect(left, top, left + coordinates.size.width, top + coordinates.size.height)
        }
    }
    return onPlaced(update).onGloballyPositioned(update)
}
