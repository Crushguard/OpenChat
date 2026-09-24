package com.piptechnologies.openchat.ui.messages

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.CapturedMessage
import com.piptechnologies.openchat.core.messages.InboxBuilder
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.core.messages.NotificationText
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.data.repo.MessagesRepository
import com.piptechnologies.openchat.platform.ExternalLinks
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.uiText
import com.piptechnologies.openchat.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One conversation (design map §4.9), read from the `mode` and `key` navigation arguments. Opening it
 * marks its messages seen here, once (§5.2); nothing is ever sent to WhatsApp until the user taps
 * "Open chat in WhatsApp". The application context only launches WhatsApp; the bar subtitle and the
 * toast are [UiText], resolved in the UI language by the screen and [ConversationRoute].
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val messages: MessagesRepository,
    private val media: MediaRepository,
) : ViewModel() {
    private val mode: InboxMode = inboxModeArg(savedStateHandle.get<String>(Routes.ARG_MODE))
    private val key: String = conversationKeyArg(savedStateHandle.get<String>(Routes.ARG_KEY))
    private val _toasts = MutableSharedFlow<UiText>(extraBufferCapacity = 1)

    /** "Opening WhatsApp…" once the chat (or the app) was launched. */
    val toasts: SharedFlow<UiText> = _toasts.asSharedFlow()

    val state: StateFlow<ConversationUiState> = messages.observeConversation(key)
        .distinctUntilChanged()
        .map { conversation -> buildState(conversation) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConversationUiState(
                mode = mode,
                title = "",
                subtitle = UiText.Raw(""),
                messages = emptyList(),
                thumbnails = emptyMap(),
                phoneDigits = null,
                nowMs = System.currentTimeMillis(),
            ),
        )

    init {
        if (key.isNotEmpty()) viewModelScope.launch { messages.markConversationSeen(key) }
    }

    /**
     * Opens the chat in WhatsApp (or WhatsApp Business for its conversations): wa.me/<digits> when the
     * conversation is a number, else the app itself. This is the one step that sends read receipts.
     */
    fun openInWhatsApp() {
        val app = if (key.substringBefore('|') == NotificationText.WHATSAPP_BUSINESS) MessagingApp.WHATSAPP_BUSINESS else MessagingApp.WHATSAPP
        if (ExternalLinks.openWhatsAppChat(context, state.value.phoneDigits, app)) {
            _toasts.tryEmit(uiText(R.string.toast_opening_whatsapp))
        }
    }

    private suspend fun buildState(conversation: List<CapturedMessage>): ConversationUiState {
        val summary = InboxBuilder.build(conversation, InboxMode.ALL).firstOrNull()
        val shown = conversationMessages(conversation, mode)
        return ConversationUiState(
            mode = mode,
            title = summary?.title.orEmpty(),
            subtitle = conversationSubtitle(summary, mode),
            messages = shown,
            thumbnails = thumbnailsOf(shown),
            phoneDigits = summary?.phoneNumber,
            nowMs = System.currentTimeMillis(),
        )
    }

    /** The local copy of each listed photo; a copy deleted from Deleted media has no entry. */
    private suspend fun thumbnailsOf(shown: List<CapturedMessage>): Map<Long, String> {
        val ids = shown.filter(::isPhotoBubble).mapNotNull { it.mediaId }.distinct()
        return ids.mapNotNull { id -> media.get(id)?.let { copy -> id to copy.localPath } }.toMap()
    }
}

/** "ALL" / "DELETED" from the route; anything else reads as All. */
internal fun inboxModeArg(raw: String?): InboxMode = InboxMode.entries.firstOrNull { it.name == raw } ?: InboxMode.ALL

/**
 * The conversation key from the route. Navigation hands path arguments over URI-decoded already; a key
 * still in its [Uri.encode] form has no "|" (encoded as %7C) and is decoded here, so a title holding "%"
 * is never decoded twice.
 */
internal fun conversationKeyArg(raw: String?): String = when {
    raw == null -> ""
    '|' in raw -> raw
    else -> Uri.decode(raw)
}
