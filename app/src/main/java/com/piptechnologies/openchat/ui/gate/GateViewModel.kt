package com.piptechnologies.openchat.ui.gate

import android.content.Context
import androidx.lifecycle.ViewModel
import com.piptechnologies.openchat.platform.NotificationAccess
import com.piptechnologies.openchat.ui.components.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State of the notification access gate (design map §4.7). The grant is read when the ViewModel is
 * created and again on every [refresh], which the route calls on each resume: that is how a grant
 * (or a revoke) made in the system settings shows up when the user comes back.
 */
@HiltViewModel
class GateViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(
        GateUiState(title = UiText.Raw(""), granted = NotificationAccess.isGranted(context), waitingForSystem = false),
    )
    val state: StateFlow<GateUiState> = _state.asStateFlow()

    /** The bar title; the route picks it from the tool the gate was opened for. */
    fun setTitle(title: UiText) {
        _state.update { it.copy(title = title) }
    }

    /** Re-reads the grant and ends any wait for the system settings screen. */
    fun refresh() {
        val granted = NotificationAccess.isGranted(context)
        _state.update { it.copy(granted = granted, waitingForSystem = false) }
    }

    /**
     * Shows the waiting state and opens the notification listener settings. A second tap while
     * waiting is ignored, so a double tap cannot open the settings screen twice; the next resume
     * ends the wait, and so does a phone with no such settings screen (else the spinner never stops).
     */
    fun openSettings() {
        if (_state.value.waitingForSystem) return
        _state.update { it.copy(waitingForSystem = true) }
        if (!NotificationAccess.openSettings(context)) _state.update { it.copy(waitingForSystem = false) }
    }
}
