package com.piptechnologies.openchat.ui.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.OcModalSheet
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [MessagesViewModel] to [MessagesScreen] and its sheets: tool settings and exclude chats in
 * [OcModalSheet]s, clear-all in a [ConfirmSheet]. [mode] is the filter the Home row asked for; the user's
 * chip choice replaces it and survives recreation. A row opens the conversation in the current mode.
 * Toasts are resolved with the activity context, so they are in the app language.
 */
@Composable
fun MessagesRoute(
    mode: InboxMode,
    onBack: () -> Unit,
    onOpenConversation: (InboxMode, String) -> Unit,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    var shownMode by rememberSaveable { mutableStateOf(mode) }
    LaunchedEffect(viewModel, shownMode) { viewModel.setMode(shownMode) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastHostState()
    val context = LocalContext.current
    LaunchedEffect(viewModel, toast, context) { viewModel.toasts.collect { toast.show(it.asString(context)) } }

    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnOpenConversation by rememberUpdatedState(onOpenConversation)
    val callbacks = remember(viewModel) {
        MessagesCallbacks(
            onBack = { currentOnBack() },
            onMode = { m ->
                shownMode = m
                viewModel.setMode(m)
            },
            onOpenToolSettings = viewModel::openToolSettings,
            onDismissToolSettings = viewModel::dismissToolSettings,
            onTogglePause = viewModel::togglePause,
            onOpenExclude = viewModel::openExclude,
            onDismissExclude = viewModel::dismissExclude,
            onToggleExclude = viewModel::toggleExclude,
            onAskClear = viewModel::askClear,
            onDismissClear = viewModel::dismissClear,
            onConfirmClear = viewModel::confirmClear,
            onOpenConversation = { conversation -> currentOnOpenConversation(viewModel.state.value.mode, conversation.key) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MessagesScreen(state = state, callbacks = callbacks)
        DarkToastHost(state = toast)
    }
    if (state.toolSettingsOpen) {
        OcModalSheet(onDismissRequest = callbacks.onDismissToolSettings) {
            Box {
                ToolSettingsSheetContent(
                    title = stringResource(R.string.messages_title),
                    paused = state.paused,
                    excludedCount = state.excludedCount,
                    clearLabel = stringResource(R.string.tool_settings_clear_messages),
                    onTogglePause = callbacks.onTogglePause,
                    onExclude = callbacks.onOpenExclude,
                    onClear = callbacks.onAskClear,
                )
                // The pause / resume toast fires while this sheet is up, so it shows over the sheet as well.
                DarkToastHost(state = toast, modifier = Modifier.matchParentSize())
            }
        }
    }
    if (state.excludeOpen) {
        OcModalSheet(onDismissRequest = callbacks.onDismissExclude) {
            ExcludeChatsSheetContent(chats = state.excludable, onToggle = callbacks.onToggleExclude)
        }
    }
    if (state.confirmClear) {
        ConfirmSheet(spec = clearMessagesSpec(), onDismiss = callbacks.onDismissClear, onConfirm = callbacks.onConfirmClear)
    }
}
