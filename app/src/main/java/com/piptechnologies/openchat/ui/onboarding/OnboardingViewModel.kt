package com.piptechnologies.openchat.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Onboarding only records that it was completed; the slide index lives in [OnboardingRoute]. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    /**
     * Persists that onboarding is done. The route navigates away (and this ViewModel is cleared) right
     * after the call, so the write runs NonCancellable to be sure it lands.
     */
    fun markDone() {
        viewModelScope.launch {
            withContext(NonCancellable) { settings.setOnboardingDone() }
        }
    }
}
