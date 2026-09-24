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
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import org.junit.Rule
import org.junit.Test

/**
 * Settings and its sheets, Language, Contact us and the four rating stages (design map §4.15–§4.20,
 * §6). Sheet contents render in [SheetPreviewFrame] over Settings, since a ModalBottomSheet opens
 * its own window, which Paparazzi does not capture.
 */
class SettingsScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    /** Access granted, WhatsApp, English, four recent numbers, no overlay open. */
    private val settingsState = SettingsUiState(
        accessGranted = true,
        app = MessagingApp.WHATSAPP,
        availableApps = MessagingApp.entries.toList(),
        languageName = "English",
        recentsCount = 4,
        version = "1.0.0 (1)",
        defaultAppSheetOpen = false,
        confirmClearRecents = false,
        rating = null,
    )

    @Composable
    private fun Settings() {
        SettingsScreen(state = settingsState, callbacks = SettingsCallbacks())
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

    @Test
    fun settings() {
        paparazzi.snapshot {
            OpenChatTheme {
                Settings()
            }
        }
    }

    @Test
    fun default_app_sheet() {
        paparazzi.snapshot {
            OpenChatTheme {
                SheetPreviewFrame(screen = { Settings() }) {
                    DefaultAppSheetContent(
                        apps = MessagingApp.entries.toList(),
                        current = MessagingApp.WHATSAPP,
                        onPick = {},
                    )
                }
            }
        }
    }

    @Test
    fun language() {
        paparazzi.snapshot {
            OpenChatTheme {
                LanguageScreen(current = "en", onBack = {}, onPick = {})
            }
        }
    }

    @Test
    fun contact() {
        paparazzi.snapshot {
            OpenChatTheme {
                ContactScreen(
                    text = "",
                    email = "",
                    onBack = {},
                    onTextChange = {},
                    onEmailChange = {},
                    onSend = {},
                )
            }
        }
    }

    @Test
    fun rating_stars() {
        paparazzi.snapshot {
            OpenChatTheme {
                Rating(RatingState(stage = RatingStage.STARS, rating = 0, feedback = ""))
            }
        }
    }

    @Test
    fun rating_store() {
        paparazzi.snapshot {
            OpenChatTheme {
                Rating(RatingState(stage = RatingStage.STORE, rating = 5, feedback = ""))
            }
        }
    }

    @Test
    fun rating_feedback() {
        paparazzi.snapshot {
            OpenChatTheme {
                Rating(RatingState(stage = RatingStage.FEEDBACK, rating = 3, feedback = ""))
            }
        }
    }

    @Test
    fun rating_thanks() {
        paparazzi.snapshot {
            OpenChatTheme {
                Rating(RatingState(stage = RatingStage.THANKS, rating = 3, feedback = "x"))
            }
        }
    }

    @Test
    fun dialog_clear_recents() {
        paparazzi.snapshot {
            OpenChatTheme {
                SheetPreviewFrame(topRadius = 26.dp, screen = { Settings() }) {
                    ConfirmSheetContent(spec = clearRecentsSpec(4), onCancel = {}, onConfirm = {})
                }
            }
        }
    }
}
