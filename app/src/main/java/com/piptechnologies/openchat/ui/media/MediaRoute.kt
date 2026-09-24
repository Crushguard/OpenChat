package com.piptechnologies.openchat.ui.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.platform.StoragePermissions
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.OcModalSheet
import com.piptechnologies.openchat.ui.components.rememberToastHostState
import com.piptechnologies.openchat.ui.messages.ExcludeChatsSheetContent
import com.piptechnologies.openchat.ui.messages.ToolSettingsSheetContent

/**
 * Binds [MediaViewModel] to [MediaScreen] and its sheets: tool settings and exclude chats in [OcModalSheet]s,
 * clear-all in a [ConfirmSheet]. The storage/media permission is requested only when the user taps "Allow
 * media access" (ruling R17); the result, and every resume, re-reads it and wakes the media watcher. Toasts go
 * to the app's host ([LocalToastHost]) when there is one, so they outlive this route.
 */
@Composable
fun MediaRoute(onBack: () -> Unit, onOpenDetail: (Long) -> Unit, viewModel: MediaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appToast = LocalToastHost.current
    val toast = appToast ?: rememberToastHostState()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnOpenDetail by rememberUpdatedState(onOpenDetail)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onPermissionResult()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel, toast) {
        viewModel.toasts.collect { toast.show(it) }
    }
    val callbacks = remember(viewModel, permissionLauncher) {
        MediaCallbacks(
            onBack = { currentOnBack() },
            onTab = viewModel::setTab,
            onOpen = { media -> currentOnOpenDetail(media.id) },
            onRequestPermission = { permissionLauncher.launch(StoragePermissions.required().toTypedArray()) },
            onOpenToolSettings = viewModel::openToolSettings,
            onDismissToolSettings = viewModel::dismissToolSettings,
            onTogglePause = viewModel::togglePause,
            onExclude = viewModel::openExclude,
            onDismissExclude = viewModel::dismissExclude,
            onToggleExclude = viewModel::toggleExclude,
            onAskClear = viewModel::askClear,
            onDismissClear = viewModel::dismissClear,
            onConfirmClear = viewModel::confirmClear,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaScreen(state = state, callbacks = callbacks)
        // Without the app's host (screenshots, previews) the route shows its own toasts.
        if (appToast == null) DarkToastHost(state = toast)
    }
    if (state.toolSettingsOpen) {
        OcModalSheet(onDismissRequest = callbacks.onDismissToolSettings) {
            Box {
                ToolSettingsSheetContent(
                    title = stringResource(R.string.media_title),
                    paused = state.paused,
                    excludedCount = state.excludedCount,
                    clearLabel = stringResource(R.string.tool_settings_clear_media),
                    onTogglePause = callbacks.onTogglePause,
                    onExclude = callbacks.onExclude,
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
        ConfirmSheet(spec = clearMediaSpec(), onDismiss = callbacks.onDismissClear, onConfirm = callbacks.onConfirmClear)
    }
}
