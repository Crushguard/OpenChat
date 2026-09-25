package com.piptechnologies.openchat.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.uiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * The Language screen's action (design map §4.17, ruling R13). The language in effect is not held here:
 * AppCompat's application locales are the source of truth, and the route reads them ([Languages.current])
 * each time it is composed. [pick] applies the per-app locale through AppCompat, which recreates the
 * activity, and toasts "Language: <native name>". The toast is a [UiText] that the route resolves, so the
 * recreated screen shows it in the new language; it is kept for replay to that screen for as long as it
 * would be on screen.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor() : ViewModel() {
    /** replay = 1 so the toast survives the activity recreation that applying the locale triggers; cleared once it has had its time. */
    private val _toasts = MutableSharedFlow<UiText>(replay = 1)

    /** Toasts for the route's dark toast. */
    val toasts: SharedFlow<UiText> = _toasts.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class) // resetReplayCache: the toast is replayed only while it would still be on screen.
    fun pick(option: LanguageOption) {
        viewModelScope.launch {
            // The native name reads the same in every language, the new one included.
            _toasts.emit(uiText(R.string.toast_language, UiText.Raw(option.native)))
            // viewModelScope runs on the main thread, as AppCompat requires.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(option.localeTag))
            delay(TOAST_REPLAY_MS)
            _toasts.resetReplayCache()
        }
    }

    private companion object {
        /** How long a toast stays on screen (design map §4.21), and so how long it is worth replaying. */
        const val TOAST_REPLAY_MS = 2_200L
    }
}
