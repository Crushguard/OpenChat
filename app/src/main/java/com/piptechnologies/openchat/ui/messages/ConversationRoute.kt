package com.piptechnologies.openchat.ui.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [ConversationViewModel] to [ConversationScreen]. [mode] and [key] are this destination's
 * navigation arguments (`Routes.CONVERSATION`); the ViewModel reads the same two values from its
 * SavedStateHandle, which is also where they survive process death. A photo opens Media detail through
 * [onOpenMedia].
 */
@Composable
fun ConversationRoute(
    mode: InboxMode,
    key: String,
    onBack: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastHostState()
    LaunchedEffect(viewModel, toast) { viewModel.toasts.collect { toast.show(it) } }
    Box(modifier = Modifier.fillMaxSize()) {
        ConversationScreen(
            state = state,
            onBack = onBack,
            onOpenInWhatsApp = viewModel::openInWhatsApp,
            onOpenMedia = onOpenMedia,
        )
        DarkToastHost(state = toast)
    }
}
