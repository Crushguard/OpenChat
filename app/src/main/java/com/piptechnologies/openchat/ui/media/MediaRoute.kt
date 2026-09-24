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
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [MediaViewModel] to [MediaScreen]. The storage/media permission is requested only when the user taps
 * "Allow media access" (ruling R17); the result, and every resume, re-reads it and wakes the media watcher.
 * "Exclude chats" closes the sheet and points to Messages, where excluded chats are managed (§4.11).
 */
@Composable
fun MediaRoute(onBack: () -> Unit, onOpenDetail: (Long) -> Unit, viewModel: MediaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastHostState()
    val excludedHint = stringResource(R.string.toast_excluded_in_messages)
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
    val callbacks = remember(viewModel, permissionLauncher, toast, excludedHint) {
        MediaCallbacks(
            onBack = { currentOnBack() },
            onTab = viewModel::setTab,
            onOpen = { media -> currentOnOpenDetail(media.id) },
            onRequestPermission = { permissionLauncher.launch(StoragePermissions.required().toTypedArray()) },
            onOpenToolSettings = viewModel::openToolSettings,
            onDismissToolSettings = viewModel::dismissToolSettings,
            onTogglePause = viewModel::togglePause,
            onExclude = {
                viewModel.dismissToolSettings()
                toast.show(excludedHint)
            },
            onAskClear = viewModel::askClear,
            onDismissClear = viewModel::dismissClear,
            onConfirmClear = viewModel::confirmClear,
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MediaScreen(state = state, callbacks = callbacks)
        DarkToastHost(state = toast)
    }
}
