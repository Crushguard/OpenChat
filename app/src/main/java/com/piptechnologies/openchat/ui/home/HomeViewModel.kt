package com.piptechnologies.openchat.ui.home

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.DialCountry
import com.piptechnologies.openchat.core.phone.PhoneNumberNormalizer
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.core.send.RecentNumber
import com.piptechnologies.openchat.core.send.SendLink
import com.piptechnologies.openchat.core.send.SendLinkBuilder
import com.piptechnologies.openchat.core.send.SendOutcomeHeuristic
import com.piptechnologies.openchat.data.prefs.SettingsRepository
import com.piptechnologies.openchat.data.repo.MediaRepository
import com.piptechnologies.openchat.data.repo.MessagesRepository
import com.piptechnologies.openchat.data.repo.RecentsRepository
import com.piptechnologies.openchat.platform.AppVersion
import com.piptechnologies.openchat.platform.ClipboardText
import com.piptechnologies.openchat.platform.CountryDetector
import com.piptechnologies.openchat.platform.DetectedCountry
import com.piptechnologies.openchat.platform.ExternalLinks
import com.piptechnologies.openchat.platform.InstalledMessagingApps
import com.piptechnologies.openchat.platform.NotificationAccess
import com.piptechnologies.openchat.ui.navigation.HomeTool
import com.piptechnologies.openchat.ui.settings.RatingFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home (design map §4.3–§4.6, behaviour §5.1).
 *
 * [state] is one [MutableStateFlow] that the text fields write synchronously (an asynchronous round
 * trip would drop keystrokes); the repositories are collected into it. Sends go through the route:
 * [send] emits the link on [launchRequests], [HomeRoute] opens it with the Activity context, then
 * reports [onLaunched], which toasts, records the recent, counts the send ([sendCompleted]) and
 * remembers the launch for the not-on-WhatsApp heuristic (ruling R6) checked in [onResume]. The third
 * completed send arms the rating sheet ([rating], design map §4.19), which opens 1.8 s after the next
 * resume (the user is back from the chat) and which the route draws; [rateOnPlay] and [sendFeedback] do
 * what the same buttons do in Settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recents: RecentsRepository,
    private val messages: MessagesRepository,
    private val media: MediaRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val detected: DetectedCountry = CountryDetector.detect(context)

    /** Installed messaging apps, re-read on every resume. */
    private var installed: List<MessagingApp> = InstalledMessagingApps.installed(context)

    /** The app picked in the Send-with menu (DataStore `send_app`); null until one is picked. */
    private var storedApp: MessagingApp? = null

    private var counts = ToolCounts()
    private var pendingSend: PendingSend? = null
    private var lastLaunch: Launch? = null
    private var recentsLoaded = false

    /** Start of the "recovered this week" window, moved forward on every resume. */
    private val recoveredSince = MutableStateFlow(weekAgo(System.currentTimeMillis()))

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    private val _sendCompleted = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    /** The new send count after each successful launch; [armRatingPrompt] watches it for the third. */
    val sendCompleted: SharedFlow<Int> = _sendCompleted.asSharedFlow()

    /** The rating sheet (design map §4.19): opened once, 1.8 s after Home resumes from the third successful send. The route draws it. */
    val rating = RatingFlow(viewModelScope)

    /** Armed by the third completed send, in memory only; the next [onResume] turns it into the timed prompt. */
    private var ratingPromptArmed = false

    /** Set once the timed prompt is scheduled, so it can never be scheduled twice in this process. */
    private var ratingPromptScheduled = false

    private val _launchRequests = MutableSharedFlow<SendLink>(extraBufferCapacity = 4)

    /** Links for [HomeRoute] to open with the Activity context; it reports back through [onLaunched]. */
    val launchRequests: SharedFlow<SendLink> = _launchRequests.asSharedFlow()

    private val _focusRequests = Channel<Unit>(Channel.CONFLATED)

    /** Moments the number field should take the focus: first run, a refilled recent, clear, "Edit number". */
    val focusRequests: Flow<Unit> = _focusRequests.receiveAsFlow()

    init {
        viewModelScope.launch {
            recents.observeRecent().collect { list ->
                val firstLoad = !recentsLoaded
                recentsLoaded = true
                _state.update { s ->
                    s.copy(
                        recents = list,
                        firstRun = list.isEmpty(),
                        revealedRecentId = s.revealedRecentId?.takeIf { id -> list.any { it.id == id } },
                        nowMs = System.currentTimeMillis(),
                    )
                }
                // First run opens Home with the number field focused (design map §4.2).
                if (firstLoad && list.isEmpty()) _focusRequests.trySend(Unit)
            }
        }
        viewModelScope.launch {
            settings.sendApp.collect { app ->
                storedApp = app
                _state.update { it.copy(app = resolveApp(app, it.availableApps)) }
            }
        }
        viewModelScope.launch {
            combine(
                messages.observeUnseenCount(),
                messages.observeDeletedCount(),
                recoveredSince.flatMapLatest { since -> media.observeRecoveredCountSince(since) },
                settings.secondLinked,
                settings.recoveryPaused,
            ) { unseen, deleted, recovered, linked, paused ->
                ToolCounts(unseen = unseen, deleted = deleted, recovered = recovered, secondLinked = linked, paused = paused)
            }.collect { latest ->
                counts = latest
                _state.update { it.copy(tools = toolStatuses(latest, it.accessGranted)) }
            }
        }
        viewModelScope.launch {
            sendCompleted.collect { count -> armRatingPrompt(count) }
        }
    }

    /**
     * Re-reads notification access and the installed apps, moves the "this week" window and the time
     * the recent labels are measured from, and applies the not-on-WhatsApp heuristic (ruling R6) to
     * the last launch: coming back within 7 s of opening WhatsApp or Business shows the sheet. Coming
     * back from the third send's chat also starts the rating prompt's timer ([scheduleRatingPrompt]).
     */
    fun onResume() {
        val now = System.currentTimeMillis()
        val granted = NotificationAccess.isGranted(context)
        installed = InstalledMessagingApps.installed(context)
        val available = availableApps(installed)
        val bounced = lastLaunch?.let { SendOutcomeHeuristic.suspectsNotOnWhatsApp(it.app, it.atMs, now) } ?: false
        lastLaunch = null
        recoveredSince.value = weekAgo(now)
        _state.update {
            it.copy(
                accessGranted = granted,
                availableApps = available,
                app = if (it.app in available) it.app else resolveApp(storedApp, available),
                tools = toolStatuses(counts, granted),
                nowMs = now,
                notOnWhatsApp = it.notOnWhatsApp || bounced,
            )
        }
        scheduleRatingPrompt()
    }

    /**
     * Keeps digits only, at most 15. Several characters arriving at once in the empty field are a
     * paste (the text toolbar or the keyboard's clipboard chip), normalised like the Paste pill so
     * "+62 812-3456-7890" sets Indonesia instead of becoming "6281234567890".
     */
    fun setDigits(raw: String) {
        if (_state.value.nationalDigits.isEmpty() && raw.length > 1) {
            applyPaste(raw, fromPill = false)
            return
        }
        val digits = PhoneNumberNormalizer.digitsOnly(raw).take(PhoneNumberNormalizer.MAX_DIGITS)
        _state.update { it.copy(nationalDigits = digits) }
    }

    /** The Paste pill: clipboard text through [PhoneNumberNormalizer.normalizePaste], with a toast. */
    fun paste() {
        val raw = ClipboardText.read(context)
        if (raw == null) {
            toast(context.getString(R.string.toast_clipboard_empty))
            return
        }
        applyPaste(raw, fromPill = true)
    }

    fun clear() {
        _state.update { it.copy(nationalDigits = "") }
        _focusRequests.trySend(Unit)
    }

    fun setMessage(m: String) {
        _state.update { it.copy(message = m) }
    }

    /** Builds the link for the chosen app and hands it to the route to open (see [launchRequests]). */
    fun send() {
        sendWith(_state.value.app)
    }

    /**
     * The route opened (or failed to open) [link]. On success: "Opening WhatsApp…", the number joins
     * the recents with the app used, the send is counted and [sendCompleted] emits the new count. Only
     * an in-app launch arms the not-on-WhatsApp check; the browser fallback cannot bounce back.
     */
    fun onLaunched(link: SendLink, success: Boolean) {
        val sent = pendingSend?.takeIf { it.link == link }
        pendingSend = null
        if (!success) {
            toast(context.getString(R.string.toast_no_app))
            return
        }
        toast(context.getString(R.string.toast_opening, link.app.label))
        lastLaunch = if (link.app in installed) Launch(app = link.app, atMs = System.currentTimeMillis()) else null
        if (sent == null) return
        viewModelScope.launch {
            recents.record(sent.dialCode, sent.nationalNumber, link.app)
            try {
                _sendCompleted.emit(settings.incrementSendCount())
            } catch (e: IOException) {
                // The count could not be stored; the chat opened all the same.
            }
        }
    }

    fun toggleMenu() {
        _state.update { it.copy(menuOpen = !it.menuOpen, revealedRecentId = null) }
    }

    fun dismissMenu() {
        _state.update { it.copy(menuOpen = false) }
    }

    /** The Send-with choice, remembered in DataStore (design map §4.5). */
    fun pickApp(app: MessagingApp) {
        _state.update { it.copy(app = app, menuOpen = false) }
        persistApp(app)
    }

    fun openCountry() {
        _state.update { it.copy(countrySheetOpen = true, countryQuery = "", menuOpen = false, revealedRecentId = null) }
    }

    fun setCountryQuery(q: String) {
        _state.update { it.copy(countryQuery = q) }
    }

    fun pickCountry(c: DialCountry) {
        _state.update { it.copy(country = c, countrySheetOpen = false, countryQuery = "") }
    }

    fun dismissCountry() {
        _state.update { it.copy(countrySheetOpen = false, countryQuery = "") }
    }

    /**
     * A recent row tapped: its country (the current one when it shares the dial code, so a Canadian
     * "+1" stays Canada), its digits and its app when that app is available; then the field is focused.
     */
    fun refill(r: RecentNumber) {
        _state.update {
            val country = if (it.country.dialCode == r.dialCode) {
                it.country
            } else {
                DialCountries.byDialCode(r.dialCode).firstOrNull() ?: it.country
            }
            it.copy(
                country = country,
                nationalDigits = PhoneNumberNormalizer.digitsOnly(r.nationalNumber).take(PhoneNumberNormalizer.MAX_DIGITS),
                app = if (r.app in it.availableApps) r.app else it.app,
                revealedRecentId = null,
                menuOpen = false,
            )
        }
        _focusRequests.trySend(Unit)
    }

    /** Slides recent [id] open over its Delete panel; null closes it. */
    fun reveal(id: Long?) {
        _state.update { it.copy(revealedRecentId = id) }
    }

    fun deleteRecent(id: Long) {
        _state.update { it.copy(revealedRecentId = null) }
        viewModelScope.launch { recents.delete(id) }
    }

    /** "Edit number": closes the sheet and puts the focus back in the number field. */
    fun notOnWhatsAppEdit() {
        _state.update { it.copy(notOnWhatsApp = false) }
        _focusRequests.trySend(Unit)
    }

    /**
     * "Try Telegram": Telegram becomes the remembered app and the same number and message go out again,
     * to Telegram explicitly: when Telegram is not installed the stored choice falls back to the first
     * installed app in [state], but this send still opens the Telegram web link.
     */
    fun notOnWhatsAppTelegram() {
        _state.update { it.copy(notOnWhatsApp = false, app = MessagingApp.TELEGRAM) }
        sendWith(MessagingApp.TELEGRAM)
        persistApp(MessagingApp.TELEGRAM)
    }

    /** "Rate on Google Play": toasts "Opening Google Play…", opens the store listing (ruling R12) and closes the sheet. */
    fun rateOnPlay() {
        toast(context.getString(R.string.toast_play))
        ExternalLinks.openPlayStore(context)
        rating.close()
    }

    /**
     * "Send feedback": the note, the rating and the versions go to the email composer (ruling R11), then
     * the sheet moves to Thanks. With no email app the sheet stays put and says so; with nothing typed it
     * asks for a note (the button is disabled in that case anyway). The same as in Settings.
     */
    fun sendFeedback() {
        val current = rating.state.value ?: return
        val note = current.feedback.trim()
        if (note.isEmpty()) {
            toast(context.getString(R.string.toast_write_first))
            return
        }
        val body = note + "\n\n" +
            context.getString(R.string.feedback_rating_line, current.rating) + "\n" +
            context.getString(R.string.feedback_versions, AppVersion.label(context), Build.VERSION.RELEASE)
        val opened = ExternalLinks.composeEmail(
            context = context,
            to = context.getString(R.string.support_email),
            subject = context.getString(R.string.feedback_subject),
            body = body,
        )
        if (!opened) {
            toast(context.getString(R.string.toast_no_email))
            return
        }
        rating.sendFeedback()
    }

    private fun sendWith(app: MessagingApp) {
        val s = _state.value
        if (!s.canSend) return
        val link = SendLinkBuilder.build(
            app = app,
            e164Digits = PhoneNumberNormalizer.e164Digits(s.country.dialCode, s.nationalDigits),
            message = s.message,
        )
        pendingSend = PendingSend(link = link, dialCode = s.country.dialCode, nationalNumber = s.nationalDigits)
        _state.update { it.copy(menuOpen = false, revealedRecentId = null) }
        _launchRequests.tryEmit(link)
    }

    private fun applyPaste(raw: String, fromPill: Boolean) {
        val before = _state.value
        val pasted = PhoneNumberNormalizer.normalizePaste(raw, before.country)
        if (pasted.nationalDigits.isEmpty()) {
            // Nothing that reads as a number: the field stays as it was.
            if (fromPill) toast(context.getString(R.string.toast_clipboard_empty))
            return
        }
        val country = pasted.country ?: before.country
        _state.update { it.copy(country = country, nationalDigits = pasted.nationalDigits) }
        when {
            country != before.country -> toast(context.getString(R.string.toast_pasted_country, country.name))
            fromPill -> toast(context.getString(R.string.toast_pasted))
        }
    }

    private fun persistApp(app: MessagingApp) {
        viewModelScope.launch {
            try {
                settings.setSendApp(app)
            } catch (e: IOException) {
                // Not remembered for next time; this session keeps the choice in [state].
            }
        }
    }

    /**
     * The rating prompt (design map §4.19, §5.5), part one: the third completed send arms it, unless the
     * sheet was already shown (`rating_shown`) or the prompt was already scheduled in this process. Nothing
     * is persisted here; a ViewModel cleared before the next resume loses the arm, and the next send re-arms.
     */
    private suspend fun armRatingPrompt(count: Int) {
        if (count < RATING_PROMPT_SENDS || ratingPromptArmed || ratingPromptScheduled) return
        if (settings.ratingShown.first()) return
        ratingPromptArmed = true
    }

    /**
     * Part two, from [onResume]: the user is back from the chat the third send opened, so the sheet opens
     * [RATING_PROMPT_DELAY_MS] later. `rating_shown` is written just before it opens, so it never shows
     * twice, even across process deaths; [ratingPromptScheduled] keeps a later resume from scheduling it again.
     */
    private fun scheduleRatingPrompt() {
        if (!ratingPromptArmed || ratingPromptScheduled) return
        ratingPromptArmed = false
        ratingPromptScheduled = true
        viewModelScope.launch {
            delay(RATING_PROMPT_DELAY_MS)
            try {
                settings.setRatingShown()
            } catch (e: IOException) {
                // Not remembered; the sheet still opens now, once for this process.
            }
            rating.open()
        }
    }

    private fun toast(text: String) {
        _toasts.tryEmit(text)
    }

    private fun initialState(): HomeUiState {
        val granted = NotificationAccess.isGranted(context)
        val available = availableApps(installed)
        return HomeUiState(
            country = detected.country,
            detected = detected,
            nationalDigits = "",
            message = "",
            app = resolveApp(null, available),
            availableApps = available,
            recents = emptyList(),
            // Stays false until the recents have loaded, so the first-run hint does not flash.
            firstRun = false,
            tools = toolStatuses(counts, granted),
            accessGranted = granted,
            countrySheetOpen = false,
            countryQuery = "",
            menuOpen = false,
            notOnWhatsApp = false,
            revealedRecentId = null,
            nowMs = System.currentTimeMillis(),
        )
    }

    /** The tool rows' status lines (design map §4.3). */
    private fun toolStatuses(counts: ToolCounts, granted: Boolean): List<HomeToolStatus> {
        val res = context.resources
        val unseen = when {
            !granted -> res.getString(R.string.tool_unseen_status_off)
            counts.paused -> res.getString(R.string.tool_unseen_status_paused)
            else -> res.getQuantityString(R.plurals.tool_unseen_status_granted, counts.unseen, counts.unseen)
        }
        val deleted = if (granted) {
            res.getQuantityString(R.plurals.tool_deleted_status_granted, counts.deleted, counts.deleted)
        } else {
            res.getString(R.string.tool_deleted_status_off)
        }
        val recovered = if (granted) {
            res.getQuantityString(R.plurals.tool_media_status_granted, counts.recovered, counts.recovered)
        } else {
            res.getString(R.string.tool_media_status_off)
        }
        val second = res.getString(if (counts.secondLinked) R.string.tool_second_status_linked else R.string.tool_second_status_off)
        return listOf(
            HomeToolStatus(HomeTool.UNSEEN, unseen),
            HomeToolStatus(HomeTool.DELETED_MESSAGES, deleted),
            HomeToolStatus(HomeTool.MEDIA, recovered),
            HomeToolStatus(HomeTool.SECOND, second),
        )
    }

    private data class ToolCounts(
        val unseen: Int = 0,
        val deleted: Int = 0,
        val recovered: Int = 0,
        val secondLinked: Boolean = false,
        val paused: Boolean = false,
    )

    private data class PendingSend(val link: SendLink, val dialCode: String, val nationalNumber: String)

    private data class Launch(val app: MessagingApp, val atMs: Long)

    private companion object {
        const val MINUTE_MS = 60_000L
        const val WEEK_MS = 7 * 24 * 60 * MINUTE_MS

        /** The rating sheet comes once, after this many completed sends, this long after Home resumes from that send's chat (design map §4.19). */
        const val RATING_PROMPT_SENDS = 3
        const val RATING_PROMPT_DELAY_MS = 1_800L

        /** The installed apps, or all three when none is (Send then opens the web link, design map §4.5). */
        fun availableApps(installed: List<MessagingApp>): List<MessagingApp> = installed.ifEmpty { MessagingApp.entries.toList() }

        /** [preferred] when it is available, else the first available app (WhatsApp when installed). */
        fun resolveApp(preferred: MessagingApp?, available: List<MessagingApp>): MessagingApp =
            preferred?.takeIf { it in available } ?: available.first()

        /** A week before [nowMs], on a whole minute so resuming does not re-query for nothing. */
        fun weekAgo(nowMs: Long): Long = (nowMs - WEEK_MS) / MINUTE_MS * MINUTE_MS
    }
}
