package com.piptechnologies.openchat.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [MediaDetailViewModel] to [MediaDetailScreen] and goes back once the copy is deleted. [id] is the
 * destination's argument; the ViewModel reads the same value from its SavedStateHandle.
 */
@Composable
fun MediaDetailRoute(
    @Suppress("UNUSED_PARAMETER") id: Long,
    onBack: () -> Unit,
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastHostState()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, toast) {
        viewModel.toasts.collect { toast.show(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.closed.collect { currentOnBack() }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MediaDetailScreen(
            state = state,
            onBack = onBack,
            onSave = viewModel::save,
            onShare = viewModel::share,
            onAskDelete = viewModel::askDelete,
            onDismissDelete = viewModel::dismissDelete,
            onConfirmDelete = viewModel::confirmDelete,
        )
        DarkToastHost(state = toast)
    }
}
