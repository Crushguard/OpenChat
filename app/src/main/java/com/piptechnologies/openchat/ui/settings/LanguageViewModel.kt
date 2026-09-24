package com.piptechnologies.openchat.ui.settings

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State of the Language screen (design map §4.17, ruling R13). [current] is the persisted BCP-47 tag
 * ("en" until one is chosen). [pick] persists the tag, toasts "Language: <English name>" and applies
 * the per-app locale through AppCompat, which recreates the activity: the toast is therefore kept for
 * replay to the recreated screen for as long as it would be on screen.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : ViewModel() {
    val current: StateFlow<String> = settings.language.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DEFAULT_LANGUAGE,
    )

    /** replay = 1 so the toast survives the activity recreation that applying the locale triggers; cleared once it has had its time. */
    private val _toasts = MutableSharedFlow<String>(replay = 1)

    /** Toast texts for the route's dark toast. */
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class) // resetReplayCache: the toast is replayed only while it would still be on screen.
    fun pick(option: LanguageOption) {
        viewModelScope.launch {
            try {
                settings.setLanguage(option.tag)
            } catch (e: IOException) {
                Log.w(TAG, "Could not save the language", e)
            }
            _toasts.emit(context.getString(R.string.toast_language, option.english))
            // viewModelScope runs on the main thread, as AppCompat requires.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(option.tag))
            delay(TOAST_REPLAY_MS)
            _toasts.resetReplayCache()
        }
    }

    private companion object {
        const val TAG = "Language"
        const val DEFAULT_LANGUAGE = "en"
        const val STOP_TIMEOUT_MS = 5_000L

        /** How long a toast stays on screen (design map §4.21), and so how long it is worth replaying. */
        const val TOAST_REPLAY_MS = 2_200L
    }
}
