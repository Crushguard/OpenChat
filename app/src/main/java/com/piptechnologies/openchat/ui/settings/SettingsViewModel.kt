package com.piptechnologies.openchat.ui.settings

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
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
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.uiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
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
 * State of Settings (design map §4.15, §5.5). The default app comes from [SettingsRepository], the
 * recents count from [RecentsRepository]; the notification grant and the installed apps are read from
 * the platform when the ViewModel is created and again on every [onResume], which the route calls on
 * each resume, so a grant made in the system settings shows up on the way back. The language is not
 * here: the route reads the language in effect ([Languages.current]) where it composes the screen. The
 * rating sheet is [rating], a [RatingFlow] on this ViewModel's scope. Toasts are [UiText] that the
 * route resolves in the screen's language; one that answers a tap inside the rating sheet is emitted
 * after the sheet closes, since the sheet's window would hide it. The feedback email stays English
 * (support-facing).
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

    private val _toasts = MutableSharedFlow<UiText>(extraBufferCapacity = 1)

    /** Toasts for the toast host. */
    val toasts: SharedFlow<UiText> = _toasts.asSharedFlow()

    /** The rating sheet's state; the route forwards star taps, typing and Close to it directly. */
    val rating = RatingFlow(viewModelScope)

    private val version: String = AppVersion.label(context)

    val state: StateFlow<SettingsUiState> = combine(
        settings.sendApp,
        recents.observeCount(),
        local,
        rating.state,
    ) { sendApp, recentsCount, ui, ratingState ->
        val app = resolveApp(sendApp, ui.installed)
        SettingsUiState(
            accessGranted = ui.accessGranted,
            app = app,
            availableApps = availableApps(ui.installed, app),
            recentsCount = recentsCount,
            version = version,
            defaultAppSheetOpen = ui.defaultAppSheetOpen,
            confirmClearRecents = ui.confirmClearRecents,
            rating = ratingState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = resolveApp(null, local.value.installed).let { app ->
            SettingsUiState(
                accessGranted = local.value.accessGranted,
                app = app,
                availableApps = availableApps(local.value.installed, app),
                recentsCount = 0,
                version = version,
                defaultAppSheetOpen = false,
                confirmClearRecents = false,
                rating = null,
            )
        },
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
            _toasts.emit(uiText(R.string.toast_default_app, app.label))
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
            _toasts.emit(uiText(R.string.toast_recents_cleared))
        }
    }

    /** "Rate us": opens the rating sheet at the stars. */
    fun rate() {
        rating.open()
    }

    /**
     * "Rate on Google Play": closes the sheet, opens the store listing (ruling R12) and toasts
     * "Opening Google Play…", or "No app can open this" when neither the store nor a browser opens.
     */
    fun rateOnPlay() {
        rating.close()
        val opened = ExternalLinks.openPlayStore(context)
        _toasts.tryEmit(uiText(if (opened) R.string.toast_play else R.string.toast_no_app_can_open))
    }

    /**
     * "Send feedback": opens the email composer with the note, the rating and the versions (ruling
     * R11), then moves the sheet to Thanks. With no email app the sheet closes and the toast says so
     * (it would be hidden under the sheet otherwise); with nothing typed it asks for a note (the
     * button is disabled in that case anyway).
     */
    fun sendFeedback() {
        val current = rating.state.value ?: return
        val note = current.feedback.trim()
        if (note.isEmpty()) {
            _toasts.tryEmit(uiText(R.string.toast_write_first))
            return
        }
        // Support-facing, so English throughout: Resources.getString(id, args) formats in the resources' locale,
        // which writes numbers in Arabic-Indic, Persian or Burmese digits in those languages.
        fun english(@StringRes id: Int, vararg args: Any): String = String.format(Locale.US, context.getString(id), *args)
        val body = note + "\n\n" +
            english(R.string.feedback_rating_line, current.rating) + "\n" +
            english(R.string.feedback_versions, version, Build.VERSION.RELEASE)
        val opened = ExternalLinks.composeEmail(
            context = context,
            to = context.getString(R.string.support_email),
            subject = context.getString(R.string.feedback_subject),
            body = body,
        )
        if (!opened) {
            rating.close()
            _toasts.tryEmit(uiText(R.string.toast_no_email))
            return
        }
        rating.sendFeedback()
    }

    /** "Share app": the system share sheet with the store link (the chooser itself always opens). */
    fun share() {
        ExternalLinks.shareApp(context)
    }

    /** "Privacy policy": the browser at privacy_policy_url (ruling R16); "No app can open this" when none opens. */
    fun privacy() {
        if (!ExternalLinks.openUrl(context, context.getString(R.string.privacy_policy_url))) {
            _toasts.tryEmit(uiText(R.string.toast_no_app_can_open))
        }
    }

    private companion object {
        const val TAG = "Settings"
        const val STOP_TIMEOUT_MS = 5_000L
        val DEFAULT_APP = MessagingApp.WHATSAPP

        /** The chosen app while it is installed (or when none is), else the first installed one, else WhatsApp: as Home resolves it. */
        fun resolveApp(chosen: MessagingApp?, installed: List<MessagingApp>): MessagingApp =
            chosen?.takeIf { installed.isEmpty() || it in installed } ?: installed.firstOrNull() ?: DEFAULT_APP

        /** The installed apps in enum order, always including [current] so the sheet has a checked row. */
        fun availableApps(installed: List<MessagingApp>, current: MessagingApp): List<MessagingApp> =
            MessagingApp.entries.filter { it == current || it in installed }
    }
}
