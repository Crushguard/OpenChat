package com.piptechnologies.openchat.ui.second

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.web.ProbeResult
import com.piptechnologies.openchat.core.web.WebSession
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.service.WebSessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State of the Second account screen (design map §4.14, §5.4).
 *
 * The session lives in [holder], which outlives this ViewModel. While the screen is in LINKING or LINKED
 * the ViewModel probes the page every [WebSession.PROBE_INTERVAL_MS]. A linked page sets LINKED, mirrors it
 * to DataStore `second_linked` and starts [WebSessionService] (on every linked result: the service is not
 * sticky). A QR code while LINKING keeps LINKING. A QR code after a probe of this ViewModel confirmed the
 * link is transient for two results; the third in a row means the session was ended on the other phone, and
 * the screen goes back to LINKING with the service stopped and the mirror false. If DataStore says linked
 * when the ViewModel is created, the screen starts LINKED and probes at once, before the first delay: a
 * linked page restarts the service after process death, and a QR code demotes the stale LINKED the same way
 * (service stopped, mirror false, LINKING).
 *
 * The probe loop runs in `viewModelScope`, so clearing the ViewModel cancels it; the WebView keeps running.
 */
@HiltViewModel
class SecondAccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    /** The session's WebView holder; the route attaches [WhatsAppWebViewHolder.get] to its AndroidView. */
    val holder: WhatsAppWebViewHolder,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SecondUiState(phase = SecondPhase.ENTRY, confirmLogout = false))
    val state: StateFlow<SecondUiState> = _state.asStateFlow()

    /** Toast texts ("Linked", "Reloading…", "Logged out") for the route's toast host. */
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    /**
     * Counts finished probes. The route reads it in its AndroidView update block, so the block runs again
     * after every probe and re-attaches a view the holder rebuilt after a renderer death.
     */
    private val _probeCount = MutableStateFlow(0)
    val probeCount: StateFlow<Int> = _probeCount.asStateFlow()

    /** The one probe loop; [startProbing] replaces it, and viewModelScope cancels it in onCleared. */
    private var probeJob: Job? = null

    /** Whether a probe of this ViewModel has seen the page linked, as opposed to LINKED restored from DataStore. */
    private var linkConfirmed = false

    /** QR results in a row while LINKED with a confirmed link; [REMOTE_LOGOUT_QR_RESULTS] of them end the link. */
    private var qrResultsInARow = 0

    init {
        viewModelScope.launch {
            // Only if Scan QR was not tapped while DataStore was being read: the loop settles LINKING anyway.
            if (settings.secondLinked.first() && _state.value.phase == SecondPhase.ENTRY) {
                _state.update { it.copy(phase = SecondPhase.LINKED) }
                startProbing(probeFirst = true)
            }
        }
    }

    /** Scan QR: shows the WebView (WhatsApp Web renders the QR code) and starts probing. */
    fun scan() {
        if (_state.value.phase != SecondPhase.ENTRY) return
        _state.update { it.copy(phase = SecondPhase.LINKING) }
        startProbing(probeFirst = false)
    }

    /** Reloads WhatsApp Web and toasts "Reloading…". */
    fun reload() {
        holder.reload()
        _toasts.tryEmit(context.getString(R.string.toast_reloading))
    }

    /** Opens the "Log out of the linked account?" sheet. */
    fun askLogout() {
        _state.update { it.copy(confirmLogout = true) }
    }

    /** Stay: closes the sheet. */
    fun dismissLogout() {
        _state.update { it.copy(confirmLogout = false) }
    }

    /**
     * Log out: ends the session on this phone (cookies, storage, cache), stops the service, goes back to
     * the entry state and toasts "Logged out". The DataStore write runs NonCancellable so it lands even if
     * the user leaves the screen right away.
     */
    fun confirmLogout() {
        if (_state.value.phase == SecondPhase.ENTRY) {
            _state.update { it.copy(confirmLogout = false) }
            return
        }
        probeJob?.cancel()
        probeJob = null
        linkConfirmed = false
        qrResultsInARow = 0
        _state.value = SecondUiState(phase = SecondPhase.ENTRY, confirmLogout = false)
        holder.logout()
        WebSessionService.stop(context)
        _toasts.tryEmit(context.getString(R.string.toast_logged_out))
        viewModelScope.launch {
            withContext(NonCancellable) { persistLinked(false) }
        }
    }

    /**
     * Runs the probe loop until the phase is ENTRY again; a running loop is replaced, never doubled. With
     * [probeFirst] the first probe runs before the first delay (the restore path, so a linked page gets its
     * service back right away); after that, and from the start otherwise, it is delay → probe.
     */
    private fun startProbing(probeFirst: Boolean) {
        probeJob?.cancel()
        probeJob = viewModelScope.launch {
            var waitFirst = !probeFirst
            while (_state.value.phase != SecondPhase.ENTRY) {
                if (waitFirst) delay(WebSession.PROBE_INTERVAL_MS)
                waitFirst = true
                val result = holder.probe()
                _probeCount.update { it + 1 }
                // A probe that finished during a logout must not link the session again.
                if (_state.value.phase == SecondPhase.ENTRY) break
                when (result) {
                    ProbeResult.LINKED -> onLinked()
                    ProbeResult.QR -> onQr()
                    ProbeResult.UNKNOWN -> Unit
                }
            }
        }
    }

    /**
     * Every linked result: LINKED, the DataStore mirror and the service, all idempotent. The service start
     * on every result is what brings it back after process death (it is not sticky). The toast shows only
     * for the LINKING → LINKED transition. A linked result also ends any run of QR results.
     */
    private suspend fun onLinked() {
        val justLinked = _state.value.phase == SecondPhase.LINKING
        linkConfirmed = true
        qrResultsInARow = 0
        _state.update { it.copy(phase = SecondPhase.LINKED) }
        if (justLinked) _toasts.tryEmit(context.getString(R.string.toast_linked))
        persistLinked(true)
        WebSessionService.start(context)
    }

    /**
     * While LINKING the QR code is what the user is scanning: nothing changes. While LINKED it means the
     * session is gone unless it is transient: a restored LINKED that no probe has confirmed is demoted at
     * once, a confirmed link after [REMOTE_LOGOUT_QR_RESULTS] QR results in a row (the page reloading shows
     * one or two at most; a session ended on the other phone shows the QR code for good).
     */
    private suspend fun onQr() {
        if (_state.value.phase != SecondPhase.LINKED) return
        if (!linkConfirmed) {
            endLink()
            return
        }
        qrResultsInARow += 1
        if (qrResultsInARow >= REMOTE_LOGOUT_QR_RESULTS) endLink()
    }

    /** The link is gone: notification and mirror corrected first, then back to LINKING so the user can scan again. */
    private suspend fun endLink() {
        linkConfirmed = false
        qrResultsInARow = 0
        WebSessionService.stop(context)
        persistLinked(false)
        _state.update { it.copy(phase = SecondPhase.LINKING) }
    }

    /** A failed write is only logged; the next probe (or the next logout) writes again. */
    private suspend fun persistLinked(linked: Boolean) {
        try {
            settings.setSecondLinked(linked)
        } catch (e: IOException) {
            Log.w(TAG, "Could not save the second account's linked state", e)
        }
    }

    private companion object {
        const val TAG = "SecondAccount"

        /** QR results in a row that turn a confirmed link into a remote logout. */
        const val REMOTE_LOGOUT_QR_RESULTS = 3
    }
}
