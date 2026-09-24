package com.piptechnologies.openchat.ui.splash

import androidx.lifecycle.ViewModel
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/** Tells the launch screen where to go: onboarding on the first run, Home afterwards. */
@HiltViewModel
class SplashViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    /**
     * Whether onboarding was completed. The repository already reads IO failures as defaults; any other
     * failure also counts as not done, so the launch screen always moves on instead of crashing.
     */
    val onboardingDone: Flow<Boolean> = settings.onboardingDone.catch { emit(false) }
}
