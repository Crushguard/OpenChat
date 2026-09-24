package com.piptechnologies.openchat.ui.settings

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.data.repo.RecentsRepository
import com.piptechnologies.openchat.platform.AppVersion
import com.piptechnologies.openchat.platform.ExternalLinks
import com.piptechnologies.openchat.platform.InstalledMessagingApps
import com.piptechnologies.openchat.platform.NotificationAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State of Settings (design map §4.15, §5.5). The default app and the language come from
 * [SettingsRepository], the recents count from [RecentsRepository]; the notification grant and the
 * installed apps are read from the platform when the ViewModel is created and again on every
 * [onResume], which the route calls on each resume, so a grant made in the system settings shows up
 * on the way back. The rating sheet is [rating], a [RatingFlow] on this ViewModel's scope.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val recents: RecentsRepository,
) : ViewModel() {
    /** What only this screen holds: the grant and installed apps as last read, and the open overlays. */
    private data class Local(
        val accessGranted: Boolean,
        val installed: List<MessagingApp>,
        val defaultAppSheetOpen: Boolean,
        val confirmClearRecents: Boolean,
    )

    private val local = MutableStateFlow(
        Local(
            accessGranted = NotificationAccess.isGranted(context),
            installed = InstalledMessagingApps.installed(context),
            defaultAppSheetOpen = false,
            confirmClearRecents = false,
        ),
    )

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Toast texts for the route's dark toast. */
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    /** The rating sheet's state; the route forwards star taps, typing and Close to it directly. */
    val rating = RatingFlow(viewModelScope)

    private val version: String = AppVersion.label(context)

    val state: StateFlow<SettingsUiState> = combine(
        settings.sendApp,
        settings.language,
        recents.observeCount(),
        local,
        rating.state,
    ) { sendApp, language, recentsCount, ui, ratingState ->
        val app = sendApp ?: DEFAULT_APP
        SettingsUiState(
            accessGranted = ui.accessGranted,
            app = app,
            availableApps = availableApps(ui.installed, app),
            languageName = Languages.byTag(language).english,
            recentsCount = recentsCount,
            version = version,
            defaultAppSheetOpen = ui.defaultAppSheetOpen,
            confirmClearRecents = ui.confirmClearRecents,
            rating = ratingState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(
            accessGranted = local.value.accessGranted,
            app = DEFAULT_APP,
            availableApps = availableApps(local.value.installed, DEFAULT_APP),
            languageName = Languages.byTag(DEFAULT_LANGUAGE).english,
            recentsCount = 0,
            version = version,
            defaultAppSheetOpen = false,
            confirmClearRecents = false,
            rating = null,
        ),
    )

    /** Re-reads the notification grant and the installed apps; the route calls it on every resume. */
    fun onResume() {
        val granted = NotificationAccess.isGranted(context)
        val installed = InstalledMessagingApps.installed(context)
        local.update { it.copy(accessGranted = granted, installed = installed) }
    }

    fun openDefaultApp() {
        local.update { it.copy(defaultAppSheetOpen = true) }
    }

    fun dismissDefaultApp() {
        local.update { it.copy(defaultAppSheetOpen = false) }
    }

    /** Closes the sheet, saves [app] as the default and toasts "Default app: <label>" once the save has landed. */
    fun pickApp(app: MessagingApp) {
        local.update { it.copy(defaultAppSheetOpen = false) }
        viewModelScope.launch {
            try {
                settings.setSendApp(app)
            } catch (e: IOException) {
                Log.w(TAG, "Could not save the default app", e)
                return@launch
            }
            _toasts.emit(context.getString(R.string.toast_default_app, app.label))
        }
    }

    fun askClearRecents() {
        local.update { it.copy(confirmClearRecents = true) }
    }

    fun dismissClearRecents() {
        local.update { it.copy(confirmClearRecents = false) }
    }

    /** Closes the confirmation, removes every recent number and toasts "Recent numbers cleared". */
    fun confirmClearRecents() {
        local.update { it.copy(confirmClearRecents = false) }
        viewModelScope.launch {
            recents.clear()
            _toasts.emit(context.getString(R.string.toast_recents_cleared))
        }
    }

    /** "Rate us": opens the rating sheet at the stars. */
    fun rate() {
        rating.open()
    }

    /** Toasts "Opening Google Play…", opens the store listing (ruling R12) and closes the sheet. */
    fun rateOnPlay() {
        _toasts.tryEmit(context.getString(R.string.toast_play))
        ExternalLinks.openPlayStore(context)
        rating.close()
    }

    /**
     * "Send feedback": opens the email composer with the note, the rating and the versions (ruling
     * R11), then moves the sheet to Thanks. With no email app the sheet stays put and says so; with
     * nothing typed it asks for a note (the button is disabled in that case anyway).
     */
    fun sendFeedback() {
        val current = rating.state.value ?: return
        val note = current.feedback.trim()
        if (note.isEmpty()) {
            _toasts.tryEmit(context.getString(R.string.toast_write_first))
            return
        }
        val body = note + "\n\n" +
            context.getString(R.string.feedback_rating_line, current.rating) + "\n" +
            context.getString(R.string.feedback_versions, version, Build.VERSION.RELEASE)
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
        rating.sendFeedback()
    }

    /** "Share app": the system share sheet with the store link. */
    fun share() {
        ExternalLinks.shareApp(context)
    }

    /** "Privacy policy": the browser at privacy_policy_url (ruling R16). */
    fun privacy() {
        ExternalLinks.openUrl(context, context.getString(R.string.privacy_policy_url))
    }

    private companion object {
        const val TAG = "Settings"
        const val STOP_TIMEOUT_MS = 5_000L
        const val DEFAULT_LANGUAGE = "en"
        val DEFAULT_APP = MessagingApp.WHATSAPP

        /** The installed apps in enum order, always including [current] so the sheet has a checked row. */
        fun availableApps(installed: List<MessagingApp>, current: MessagingApp): List<MessagingApp> =
            MessagingApp.entries.filter { it == current || it in installed }
    }
}
