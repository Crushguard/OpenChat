package com.piptechnologies.openchat.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [MediaDetailViewModel] to [MediaDetailScreen] and goes back once the copy is deleted. Toasts go to the
 * app's host ([LocalToastHost]) when there is one, so "Deleted" stays up on the grid after the pop; their text is
 * resolved here, in the activity's language. [id] is the destination's argument; the ViewModel reads the same
 * value from its SavedStateHandle.
 */
@Composable
fun MediaDetailRoute(
    @Suppress("UNUSED_PARAMETER") id: Long,
    onBack: () -> Unit,
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appToast = LocalToastHost.current
    val toast = appToast ?: rememberToastHostState()
    val currentOnBack by rememberUpdatedState(onBack)
    val context = LocalContext.current
    LaunchedEffect(viewModel, toast) {
        viewModel.toasts.collect { toast.show(it.asString(context)) }
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
        // Without the app's host (screenshots, previews) the route shows its own toasts.
        if (appToast == null) DarkToastHost(state = toast)
    }
}
