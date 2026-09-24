package com.piptechnologies.openchat.ui.second

import android.Manifest
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.ui.components.ConfirmSheet
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.asString
import com.piptechnologies.openchat.ui.components.rememberToastHostState

/**
 * Binds [SecondAccountViewModel] to [SecondAccountScreen]: the session WebView goes into the screen's web
 * slot, toasts (resolved here, in the UI language) into a [DarkToastHost], and the log-out confirmation is a
 * [ConfirmSheet]. Scan QR first asks for POST_NOTIFICATIONS on API 33+ (the permission only decides whether
 * the "Second account linked" notification is visible) and then scans whatever the answer was. Back
 * ([onBack]) leaves the screen; the session and its service keep running.
 */
@Composable
fun SecondAccountRoute(onBack: () -> Unit, viewModel: SecondAccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val probeCount = viewModel.probeCount.collectAsStateWithLifecycle()
    val toast = rememberToastHostState()
    // The Activity context carries the per-app language (the application context does not on API 24–32).
    val context = LocalContext.current
    LaunchedEffect(viewModel, toast) { viewModel.toasts.collect { toast.show(it.asString(context)) } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.scan()
    }
    val holder = viewModel.holder
    Box(modifier = Modifier.fillMaxSize()) {
        SecondAccountScreen(
            state = state,
            onBack = onBack,
            onScan = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.scan()
                }
            },
            onReload = viewModel::reload,
            onAskLogout = viewModel::askLogout,
            web = { modifier -> SessionWebView(holder = holder, probeCount = probeCount, modifier = modifier) },
        )
        DarkToastHost(state = toast)
    }
    if (state.confirmLogout) {
        ConfirmSheet(spec = logoutSpec(), onDismiss = viewModel::dismissLogout, onConfirm = viewModel::confirmLogout)
    }
}

/**
 * The holder's WebView inside a FrameLayout. The holder is the only place the view is created, and it is
 * asked for it here only, on the main thread. The update block re-runs after every probe (it reads
 * [probeCount]), so a view the holder rebuilt after a renderer death is attached again within one probe
 * interval. Leaving the slot takes the view out of the container, ready for the next composition.
 */
@Composable
private fun SessionWebView(holder: WhatsAppWebViewHolder, probeCount: State<Int>, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> FrameLayout(context).also { container -> attach(holder, container) } },
        modifier = modifier,
        update = { container ->
            probeCount.value
            attach(holder, container)
        },
    )
    DisposableEffect(Unit) {
        onDispose { holder.detach() }
    }
}

/** Puts the holder's current view into [container] unless it is already there. */
private fun attach(holder: WhatsAppWebViewHolder, container: FrameLayout) {
    val view = holder.get()
    if (view.parent === container) return
    (view.parent as? ViewGroup)?.removeView(view)
    container.removeAllViews()
    container.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}
