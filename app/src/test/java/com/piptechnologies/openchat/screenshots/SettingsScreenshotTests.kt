package com.piptechnologies.openchat.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.ui.components.ConfirmSheetContent
import com.piptechnologies.openchat.ui.components.SheetPreviewFrame
import com.piptechnologies.openchat.ui.settings.ContactScreen
import com.piptechnologies.openchat.ui.settings.DefaultAppSheetContent
import com.piptechnologies.openchat.ui.settings.LanguageScreen
import com.piptechnologies.openchat.ui.settings.RatingSheetContent
import com.piptechnologies.openchat.ui.settings.RatingStage
import com.piptechnologies.openchat.ui.settings.RatingState
import com.piptechnologies.openchat.ui.settings.SettingsCallbacks
import com.piptechnologies.openchat.ui.settings.SettingsScreen
import com.piptechnologies.openchat.ui.settings.SettingsUiState
import com.piptechnologies.openchat.ui.settings.clearRecentsSpec
import org.junit.Rule
import org.junit.Test

/**
 * Settings and its sheets, Language, Contact us and the four rating stages (design map §4.15–§4.20,
 * §6). Sheet contents render in [SheetPreviewFrame] over Settings, since a ModalBottomSheet opens
 * its own window, which Paparazzi does not capture. The language in effect is [LocalSceneLanguage]:
 * the Language row shows its native name and the Language screen checks it, as in the app.
 */
object SettingsScenes {
    /** Access granted, WhatsApp, four recent numbers, no overlay open. */
    private val settingsState = SettingsUiState(
        accessGranted = true,
        app = MessagingApp.WHATSAPP,
        availableApps = MessagingApp.entries.toList(),
        recentsCount = 4,
        version = "1.0.0 (1)",
        defaultAppSheetOpen = false,
        confirmClearRecents = false,
        rating = null,
    )

    val settings = Scene("settings") {
        Settings()
    }

    val defaultAppSheet = Scene("default_app_sheet") {
        SheetPreviewFrame(screen = { Settings() }) {
            DefaultAppSheetContent(
                apps = MessagingApp.entries.toList(),
                current = MessagingApp.WHATSAPP,
                onPick = {},
            )
        }
    }

    val language = Scene("language") {
        LanguageScreen(current = LocalSceneLanguage.current.tag, onBack = {}, onPick = {})
    }

    val contact = Scene("contact") {
        ContactScreen(
            text = "",
            email = "",
            onBack = {},
            onTextChange = {},
            onEmailChange = {},
            onSend = {},
        )
    }

    val ratingStars = Scene("rating_stars") {
        Rating(RatingState(stage = RatingStage.STARS, rating = 0, feedback = ""))
    }

    val ratingStore = Scene("rating_store") {
        Rating(RatingState(stage = RatingStage.STORE, rating = 5, feedback = ""))
    }

    val ratingFeedback = Scene("rating_feedback") {
        Rating(RatingState(stage = RatingStage.FEEDBACK, rating = 3, feedback = ""))
    }

    val ratingThanks = Scene("rating_thanks") {
        Rating(RatingState(stage = RatingStage.THANKS, rating = 3, feedback = "x"))
    }

    val dialogClearRecents = Scene("dialog_clear_recents") {
        SheetPreviewFrame(topRadius = 26.dp, screen = { Settings() }) {
            ConfirmSheetContent(spec = clearRecentsSpec(4), onCancel = {}, onConfirm = {})
        }
    }

    val all: List<Scene> = listOf(
        settings,
        defaultAppSheet,
        language,
        contact,
        ratingStars,
        ratingStore,
        ratingFeedback,
        ratingThanks,
        dialogClearRecents,
    )

    @Composable
    private fun Settings() {
        SettingsScreen(state = settingsState, languageName = LocalSceneLanguage.current.native, callbacks = SettingsCallbacks())
    }

    @Composable
    private fun Rating(state: RatingState) {
        SheetPreviewFrame(topRadius = 26.dp, screen = { Settings() }) {
            RatingSheetContent(
                state = state,
                onStar = {},
                onFeedbackChange = {},
                onSendFeedback = {},
                onRateOnPlay = {},
                onClose = {},
            )
        }
    }
}

/** [SettingsScenes] in English. */
class SettingsScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun settings() = paparazzi.snapshot(SettingsScenes.settings)

    @Test
    fun default_app_sheet() = paparazzi.snapshot(SettingsScenes.defaultAppSheet)

    @Test
    fun language() = paparazzi.snapshot(SettingsScenes.language)

    @Test
    fun contact() = paparazzi.snapshot(SettingsScenes.contact)

    @Test
    fun rating_stars() = paparazzi.snapshot(SettingsScenes.ratingStars)

    @Test
    fun rating_store() = paparazzi.snapshot(SettingsScenes.ratingStore)

    @Test
    fun rating_feedback() = paparazzi.snapshot(SettingsScenes.ratingFeedback)

    @Test
    fun rating_thanks() = paparazzi.snapshot(SettingsScenes.ratingThanks)

    @Test
    fun dialog_clear_recents() = paparazzi.snapshot(SettingsScenes.dialogClearRecents)
}
