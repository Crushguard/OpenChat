package com.piptechnologies.openchat.ui.media

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.MediaDayGrouper
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.data.repo.MessagesRepository
import com.piptechnologies.openchat.platform.StoragePermissions
import com.piptechnologies.openchat.service.MediaWatcher
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.uiText
import com.piptechnologies.openchat.ui.messages.ExcludableChat
import com.piptechnologies.openchat.ui.messages.excludableChats
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * State of Deleted media (design map §4.12) with its tool settings (§4.10), exclude chats (§4.11, shared with
 * Messages through [MessagesRepository]) and clear-all confirmation. Items come from [MediaRepository] only:
 * this screen never lists storage itself. On every resume (and after the permission dialog) it re-reads the
 * storage/media permission and, when granted, starts [MediaWatcher] and asks it for a reconcile pass, so media
 * deleted while the app was away shows up (§5.3).
 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val media: MediaRepository,
    private val messages: MessagesRepository,
    private val settings: SettingsRepository,
    private val watcher: MediaWatcher,
) : ViewModel() {
    /** What only this screen holds: the tab, the permission as last read, the open sheets, the clock for day labels. */
    private data class Local(
        val tab: MediaCategory,
        val hasPermission: Boolean,
        val toolSettingsOpen: Boolean,
        val excludeOpen: Boolean,
        val confirmClear: Boolean,
        val nowMs: Long,
    )

    /** The Exclude chats rows and the count on the tool settings row. */
    private data class Exclusions(val chats: List<ExcludableChat>, val count: Int)

    private val local = MutableStateFlow(
        Local(
            tab = MediaCategory.PHOTO,
            hasPermission = StoragePermissions.hasAll(context),
            toolSettingsOpen = false,
            excludeOpen = false,
            confirmClear = false,
            nowMs = System.currentTimeMillis(),
        ),
    )

    private val _toasts = MutableSharedFlow<UiText>(extraBufferCapacity = TOAST_BUFFER)

    /** Toast texts for the route's dark toast, resolved there in the UI language. */
    val toasts: SharedFlow<UiText> = _toasts.asSharedFlow()

    /** Serializes the pause and exclude toggles, so a double tap flips twice instead of racing. */
    private val toggleLock = Mutex()

    private val exclusions: Flow<Exclusions> =
        combine(messages.observeConversationRefs(), messages.observeExcluded()) { refs, excluded ->
            Exclusions(chats = excludableChats(refs, excluded), count = excluded.size)
        }

    val state: StateFlow<MediaUiState> = combine(
        media.observeRecovered(),
        settings.recoveryPaused,
        exclusions,
        local,
    ) { recovered, paused, excluded, ui ->
        MediaUiState(
            tab = ui.tab,
            // Grouped here by calendar day; the screen names each day in the UI language.
            groups = MediaDayGrouper.group(recovered.filter { it.category == ui.tab }, ui.nowMs),
            hasPermission = ui.hasPermission,
            paused = paused,
            excludedCount = excluded.count,
            toolSettingsOpen = ui.toolSettingsOpen,
            confirmClear = ui.confirmClear,
            nowMs = ui.nowMs,
            excludeOpen = ui.excludeOpen,
            excludable = excluded.chats,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = MediaUiState(
            tab = local.value.tab,
            groups = emptyList(),
            hasPermission = local.value.hasPermission,
            paused = false,
            excludedCount = 0,
            toolSettingsOpen = false,
            confirmClear = false,
            nowMs = local.value.nowMs,
        ),
    )

    fun setTab(category: MediaCategory) {
        local.update { it.copy(tab = category) }
    }

    /** After the permission dialog: the grant may have changed. */
    fun onPermissionResult() {
        refreshPermission()
    }

    /** Refreshes "now" for the day labels and the permission, then wakes the watcher (§5.3: the media screen on open). */
    fun onResume() {
        local.update { it.copy(nowMs = System.currentTimeMillis()) }
        refreshPermission()
    }

    fun openToolSettings() {
        local.update { it.copy(toolSettingsOpen = true) }
    }

    fun dismissToolSettings() {
        local.update { it.copy(toolSettingsOpen = false) }
    }

    /**
     * Flips Pause recovery (shared with Messages) and confirms it with "Recovery paused" / "Recovery resumed".
     * On resume the watcher looks at the folders at once: it left them alone while paused.
     */
    fun togglePause() {
        viewModelScope.launch {
            val paused = try {
                toggleLock.withLock {
                    val next = !settings.recoveryPaused.first()
                    settings.setRecoveryPaused(next)
                    next
                }
            } catch (e: IOException) {
                Log.w(TAG, "Could not save Pause recovery", e)
                return@launch
            }
            if (!paused) watcher.requestReconcile()
            _toasts.emit(uiText(if (paused) R.string.toast_paused else R.string.toast_resumed))
        }
    }

    /** The exclude sheet replaces the tool settings sheet, as on Messages. */
    fun openExclude() {
        local.update { it.copy(toolSettingsOpen = false, excludeOpen = true) }
    }

    fun dismissExclude() {
        local.update { it.copy(excludeOpen = false) }
    }

    /** Excludes or lets back in [chat] (from its stored state); the listener checks the list for every notification (ruling R15). */
    fun toggleExclude(chat: ExcludableChat) {
        viewModelScope.launch {
            toggleLock.withLock { messages.setExcluded(chat.key, chat.title, !messages.isExcluded(chat.key)) }
        }
    }

    fun askClear() {
        local.update { it.copy(confirmClear = true) }
    }

    fun dismissClear() {
        local.update { it.copy(confirmClear = false) }
    }

    /**
     * Deletes every recovered copy (files and rows), closes both sheets and toasts "Cleared". A second tap
     * while the confirmation closes is ignored; the delete itself outlives the screen.
     */
    fun confirmClear() {
        if (!local.value.confirmClear) return
        local.update { it.copy(confirmClear = false, toolSettingsOpen = false) }
        viewModelScope.launch {
            withContext(NonCancellable) { media.clearRecovered() }
            _toasts.emit(uiText(R.string.toast_cleared))
        }
    }

    /** Review focus 5: the watcher only starts once the permission is there, and nothing here touches storage. */
    private fun refreshPermission() {
        val granted = StoragePermissions.hasAll(context)
        local.update { it.copy(hasPermission = granted) }
        if (granted) {
            watcher.start()
            watcher.requestReconcile()
        }
    }

    private companion object {
        const val TAG = "MediaViewModel"
        const val TOAST_BUFFER = 4
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
