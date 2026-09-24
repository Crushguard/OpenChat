package com.piptechnologies.openchat.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.platform.InstalledMessagingApps
import com.piptechnologies.openchat.platform.SendLauncher
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.OcModalSheet
import com.piptechnologies.openchat.ui.components.rememberToastHostState
import com.piptechnologies.openchat.ui.icons.AppGlyphImage
import com.piptechnologies.openchat.ui.navigation.HomeTool
import com.piptechnologies.openchat.ui.settings.RatingSheet

/** Height of the sheet handle row OcModalSheet draws above the content (10 + 4 + 10). */
private val SheetHandleArea = 24.dp

/** The country sheet is 86 % of the screen tall (design map §4.4). */
private const val CountrySheetFraction = 0.86f

/**
 * Binds [HomeViewModel] to [HomeScreen] and shows what the screen does not draw itself: the country
 * sheet, the not-on-WhatsApp sheet, the rating sheet (design map §4.19, once, on the return from the third send) and
 * the dark toast, which goes to the app-level host ([LocalToastHost]) when there is one. Sends are
 * opened here with the Activity context (LocalContext), so the chat opens in the app's own task and
 * Back returns to OpenChat, which the not-on-WhatsApp heuristic relies on. [onTool] gets the tool and
 * whether notification access is granted, so the caller can open the gate or the tool.
 */
@Composable
fun HomeRoute(
    onSettings: () -> Unit,
    onTool: (HomeTool, accessGranted: Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rating by viewModel.rating.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // AppRoot's host, so a toast outlives this screen; an own host where nothing provides one (previews, tests).
    val appToast = LocalToastHost.current
    val toast = appToast ?: rememberToastHostState()
    val focusRequester = remember { FocusRequester() }
    val currentOnSettings by rememberUpdatedState(onSettings)
    val currentOnTool by rememberUpdatedState(onTool)

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.toasts.collect { toast.show(it) }
    }
    LaunchedEffect(viewModel, context) {
        viewModel.launchRequests.collect { link ->
            val launched = SendLauncher.launch(context, link)
            viewModel.onLaunched(link, launched)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.focusRequests.collect {
            // One frame first, so a sheet that just closed (Edit number) is gone before the field takes focus.
            withFrameNanos { }
            focusRequester.requestFocusSafely()
        }
    }
    BackHandler(enabled = state.menuOpen) { viewModel.dismissMenu() }
    BackHandler(enabled = state.revealedRecentId != null) { viewModel.reveal(null) }

    val callbacks = remember(viewModel) {
        HomeCallbacks(
            onSettings = { currentOnSettings() },
            onCountryChip = viewModel::openCountry,
            onDigitsChange = viewModel::setDigits,
            onPaste = viewModel::paste,
            onClear = viewModel::clear,
            onMessageChange = viewModel::setMessage,
            onSend = viewModel::send,
            onToggleMenu = viewModel::toggleMenu,
            onDismissMenu = viewModel::dismissMenu,
            onPickApp = viewModel::pickApp,
            onRecentTap = viewModel::refill,
            onRecentReveal = viewModel::reveal,
            onRecentDelete = viewModel::deleteRecent,
            onTool = { tool -> currentOnTool(tool, viewModel.state.value.accessGranted) },
            onCountryQuery = viewModel::setCountryQuery,
            onCountryPick = viewModel::pickCountry,
            onCountryDismiss = viewModel::dismissCountry,
            onNotOnWhatsAppEdit = viewModel::notOnWhatsAppEdit,
            onNotOnWhatsAppTelegram = viewModel::notOnWhatsAppTelegram,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            state = state,
            callbacks = callbacks,
            appIcon = { app, size, tint -> LauncherIconOrGlyph(app = app, size = size, tint = tint) },
            focusRequester = focusRequester,
        )
        if (appToast == null) DarkToastHost(state = toast)
    }

    if (state.countrySheetOpen) {
        val contentHeight = ((LocalConfiguration.current.screenHeightDp * CountrySheetFraction).dp - SheetHandleArea).coerceAtLeast(0.dp)
        OcModalSheet(onDismissRequest = callbacks.onCountryDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
            ) {
                CountryPickerSheetContent(
                    query = state.countryQuery,
                    current = state.country,
                    detected = state.detected,
                    onQuery = callbacks.onCountryQuery,
                    onPick = callbacks.onCountryPick,
                    onClose = callbacks.onCountryDismiss,
                )
            }
        }
    }
    if (state.notOnWhatsApp) {
        ConfirmSheet(
            spec = notOnWhatsAppSpec(state.country.dialCode, state.nationalDigits),
            onDismiss = callbacks.onNotOnWhatsAppEdit,
            onConfirm = callbacks.onNotOnWhatsAppTelegram,
        )
    }
    // The rating sheet waits until the other sheets are closed, so modals never stack (it stays pending meanwhile).
    val ratingState = rating
    if (ratingState != null && !state.countrySheetOpen && !state.notOnWhatsApp && !state.menuOpen) {
        RatingSheet(
            state = ratingState,
            onStar = viewModel.rating::star,
            onFeedbackChange = viewModel.rating::setFeedback,
            onSendFeedback = viewModel::sendFeedback,
            onRateOnPlay = viewModel::rateOnPlay,
            onClose = viewModel.rating::close,
        )
    }
}

/** The installed app's own launcher icon (ruling R10) at [size]; the design's line glyph when it is not installed. */
@Composable
private fun LauncherIconOrGlyph(app: MessagingApp, size: Dp, tint: Color) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val icon = remember(app, sizePx) {
        InstalledMessagingApps.icon(context, app)?.toBitmap(width = sizePx, height = sizePx)?.asImageBitmap()
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        AppGlyphImage(glyph = app.glyph, size = size, tint = tint)
    }
}
