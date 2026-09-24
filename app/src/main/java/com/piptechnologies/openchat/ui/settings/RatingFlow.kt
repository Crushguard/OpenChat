package com.piptechnologies.openchat.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The four stages of the rating sheet (design map §4.19). */
enum class RatingStage { STARS, STORE, FEEDBACK, THANKS }

/** What the rating sheet shows: the [stage], the [rating] tapped so far (0 = none) and the [feedback] typed. */
data class RatingState(val stage: RatingStage, val rating: Int, val feedback: String)

/**
 * Plain state holder for the rating sheet, shared by Settings ("Rate us") and Home (after the third
 * successful send). [open] shows the stars; [star] records the rating at once and, 350 ms later, moves
 * to STORE (4–5 stars) or FEEDBACK (1–3), a second tap restarting the wait; [sendFeedback] moves to
 * THANKS (sending the note itself is the caller's job); [close] hides the sheet. Timers run in [scope],
 * so a ViewModel passes its viewModelScope and a test its test scope. Not thread-safe: call it from
 * the main thread, as the UI does.
 */
class RatingFlow(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<RatingState?>(null)

    /** Null while the sheet is closed. */
    val state: StateFlow<RatingState?> = _state.asStateFlow()

    private var pendingMove: Job? = null

    fun open() {
        cancelPendingMove()
        _state.value = RatingState(stage = RatingStage.STARS, rating = 0, feedback = "")
    }

    /** Lights up [value] stars right away, then moves on after the delay unless another star is tapped first. */
    fun star(value: Int) {
        val current = _state.value ?: return
        if (current.stage != RatingStage.STARS) return
        _state.value = current.copy(rating = value.coerceIn(1, STAR_COUNT))
        cancelPendingMove()
        pendingMove = scope.launch {
            delay(STAGE_DELAY_MS)
            _state.update { s ->
                if (s == null || s.stage != RatingStage.STARS) {
                    s
                } else {
                    s.copy(stage = if (s.rating >= STORE_THRESHOLD) RatingStage.STORE else RatingStage.FEEDBACK)
                }
            }
        }
    }

    fun setFeedback(text: String) {
        _state.update { it?.copy(feedback = text) }
    }

    fun sendFeedback() {
        _state.update { it?.copy(stage = RatingStage.THANKS) }
    }

    fun close() {
        cancelPendingMove()
        _state.value = null
    }

    private fun cancelPendingMove() {
        pendingMove?.cancel()
        pendingMove = null
    }

    private companion object {
        const val STAGE_DELAY_MS = 350L
        const val STAR_COUNT = 5
        const val STORE_THRESHOLD = 4
    }
}
