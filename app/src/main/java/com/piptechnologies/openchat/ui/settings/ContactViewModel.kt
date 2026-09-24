package com.piptechnologies.openchat.ui.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.platform.AppVersion
import com.piptechnologies.openchat.platform.ExternalLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State of Contact us (design map §4.18, ruling R11). There is no backend: [send] opens the system
 * email composer with the note, the reply address and the app and Android versions, toasts
 * "Sent. Thank you." (on the app-level toast host, so it outlives this screen) and emits [sent] so
 * the route goes back.
 */
@HiltViewModel
class ContactViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Toast texts for the toast host. */
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    // Replays, so a route recreated right after Send still goes back.
    private val _sent = MutableSharedFlow<Unit>(replay = 1)

    /** Emits once the note has been handed to the email app; the route pops back on it. */
    val sent: SharedFlow<Unit> = _sent.asSharedFlow()

    fun setText(t: String) {
        _text.value = t
    }

    fun setEmail(e: String) {
        _email.value = e
    }

    /**
     * Opens the email composer to support_email with subject feedback_subject and the body
     * `text\n\nReply to: <email or —>\n<app version> · Android <release>`. Nothing typed → "Write
     * something first"; no email app → "No email app found" and Send stays usable; otherwise
     * "Sent. Thank you." then [sent].
     */
    fun send() {
        val note = _text.value.trim()
        if (note.isEmpty()) {
            _toasts.tryEmit(context.getString(R.string.toast_write_first))
            return
        }
        val replyTo = _email.value.trim().ifEmpty { NO_REPLY_ADDRESS }
        val body = note + "\n\n" +
            context.getString(R.string.feedback_reply_to, replyTo) + "\n" +
            context.getString(R.string.feedback_versions, AppVersion.label(context), Build.VERSION.RELEASE)
        val opened = ExternalLinks.composeEmail(
            context = context,
            to = context.getString(R.string.support_email),
            subject = context.getString(R.string.feedback_subject),
            body = body,
        )
        if (!opened) {
            _toasts.tryEmit(context.getString(R.string.toast_no_email))
            return
        }
        _toasts.tryEmit(context.getString(R.string.toast_sent))
        _sent.tryEmit(Unit)
    }

    private companion object {
        /** What the body says when no reply address was given. */
        const val NO_REPLY_ADDRESS = "—"
    }
}
