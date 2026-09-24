package com.piptechnologies.openchat.ui.media

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.platform.MediaExporter
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.uiText
import com.piptechnologies.openchat.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State and actions of the media detail (design map §4.13) for the copy named by the destination's `id`
 * argument. Save to gallery and Share go through [MediaExporter]; Delete removes the copy and its row, toasts
 * "Deleted" and emits [closed] so the route goes back.
 */
@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val media: MediaRepository,
) : ViewModel() {
    private val mediaId: Long = mediaIdOf(savedStateHandle.get<Any>(Routes.ARG_ID))

    private val confirmDelete = MutableStateFlow(false)

    private val _toasts = MutableSharedFlow<UiText>(extraBufferCapacity = TOAST_BUFFER)

    /** Toast texts for the route's dark toast, resolved there in the UI language. */
    val toasts: SharedFlow<UiText> = _toasts.asSharedFlow()

    // Replays, so a route recreated while the delete was running still goes back.
    private val _closed = MutableSharedFlow<Unit>(replay = 1)

    /** Emits once the copy is deleted: the screen closes. */
    val closed: SharedFlow<Unit> = _closed.asSharedFlow()

    /**
     * The last state that had the copy in it. Once the row is gone (deleted here or cleared elsewhere) the
     * screen keeps showing it while it closes instead of blanking. Only touched by the state flow below.
     */
    private var lastShown: MediaDetailUiState? = null

    private var saving: Job? = null

    val state: StateFlow<MediaDetailUiState> = combine(
        media.observe(mediaId),
        media.observeRecovered(),
        confirmDelete,
    ) { item, recovered, confirm ->
        if (item != null) lastShown = describe(item, recovered)
        (lastShown ?: EMPTY).copy(confirmDelete = confirm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EMPTY)

    /** Save to gallery (MediaStore insert, §5.3): "Saved to gallery" or "Could not save". A tap while saving is ignored. */
    fun save() {
        val item = state.value.item ?: return
        if (saving?.isActive == true) return
        saving = viewModelScope.launch {
            val saved = MediaExporter.saveToGallery(context, item)
            _toasts.emit(uiText(if (saved) R.string.toast_saved else R.string.toast_save_failed))
        }
    }

    /** Share through the FileProvider (ACTION_SEND chooser): "Opening share sheet…" or "Could not share". */
    fun share() {
        val item = state.value.item ?: return
        val opened = MediaExporter.share(context, item)
        _toasts.tryEmit(uiText(if (opened) R.string.toast_share else R.string.toast_share_failed))
    }

    fun askDelete() {
        confirmDelete.value = true
    }

    fun dismissDelete() {
        confirmDelete.value = false
    }

    /**
     * Deletes the copy (file and row; a gallery copy stays), toasts "Deleted" and closes. A second tap while
     * the confirmation closes is ignored; the delete itself outlives the screen.
     */
    fun confirmDelete() {
        if (!confirmDelete.value) return
        confirmDelete.value = false
        val item = state.value.item ?: return
        viewModelScope.launch {
            withContext(NonCancellable) { media.delete(item.id) }
            _toasts.emit(uiText(R.string.toast_deleted))
            _closed.emit(Unit)
        }
    }

    /**
     * Counter within the recovered items of the same category, newest first; 0 / 0 when the copy is not a recovered
     * one. The screen writes the meta line from the item, relative to now.
     */
    private fun describe(item: RecoveredMedia, recovered: List<RecoveredMedia>): MediaDetailUiState {
        val sameCategory = recovered.filter { it.category == item.category }
        val position = sameCategory.indexOfFirst { it.id == item.id }
        return MediaDetailUiState(
            item = item,
            index = position + 1,
            total = if (position >= 0) sameCategory.size else 0,
            nowMs = System.currentTimeMillis(),
            confirmDelete = false,
        )
    }

    private companion object {
        const val TOAST_BUFFER = 4
        const val STOP_TIMEOUT_MS = 5_000L
        const val NO_ID = -1L

        val EMPTY = MediaDetailUiState(item = null, index = 0, total = 0, nowMs = 0L, confirmDelete = false)

        /** The `id` argument: a Long when the destination declares `NavType.LongType`, else its text form. */
        fun mediaIdOf(raw: Any?): Long = when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is String -> raw.toLongOrNull() ?: NO_ID
            else -> NO_ID
        }
    }
}
