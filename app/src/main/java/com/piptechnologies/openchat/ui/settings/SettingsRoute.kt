package com.piptechnologies.openchat.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [SettingsViewModel] to [SettingsScreen]. The access card opens the gate ([onOpenGate]), the
 * Language and Contact rows navigate ([onLanguage], [onContact]); everything else is handled by the
 * ViewModel, whose toasts are resolved in this composition's language and go to the app-level toast
 * host (AppRoot's [LocalToastHost]) or, where none is provided (screenshots, previews), to a host drawn
 * over the screen. The grant and the installed apps are re-read on every resume. The Language row
 * shows the language in effect ([Languages.current]), read here rather than in the ViewModel: a
 * language switch recreates the activity and this composition with it, while the ViewModel, and any
 * name it had cached, outlives both.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenGate: () -> Unit,
    onLanguage: () -> Unit,
    onContact: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val languageName = remember(configuration) { Languages.current(context).native }
    val toast = LocalToastHost.current ?: rememberToastHostState()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnOpenGate by rememberUpdatedState(onOpenGate)
    val currentOnLanguage by rememberUpdatedState(onLanguage)
    val currentOnContact by rememberUpdatedState(onContact)
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel, toast, context) {
        viewModel.toasts.collect { toast.show(it.asString(context)) }
    }
    val callbacks = remember(viewModel) {
        SettingsCallbacks(
            onBack = { currentOnBack() },
            onAccessCard = { currentOnOpenGate() },
            onDefaultApp = viewModel::openDefaultApp,
            onDismissDefaultApp = viewModel::dismissDefaultApp,
            onPickApp = viewModel::pickApp,
            onLanguage = { currentOnLanguage() },
            onAskClearRecents = viewModel::askClearRecents,
            onDismissClearRecents = viewModel::dismissClearRecents,
            onConfirmClearRecents = viewModel::confirmClearRecents,
            onRate = viewModel::rate,
            onContact = { currentOnContact() },
            onShare = viewModel::share,
            onPrivacy = viewModel::privacy,
            onRatingStar = viewModel.rating::star,
            onRatingFeedback = viewModel.rating::setFeedback,
            onRatingSend = viewModel::sendFeedback,
            onRatingPlay = viewModel::rateOnPlay,
            onRatingClose = viewModel.rating::close,
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScreen(state = state, languageName = languageName, callbacks = callbacks)
        if (LocalToastHost.current == null) DarkToastHost(state = toast)
    }
}
