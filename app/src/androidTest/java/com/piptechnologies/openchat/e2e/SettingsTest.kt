package com.piptechnologies.openchat.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.send.MessagingApp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Design map §4.15–§4.19 (rulings R11, R12): the Settings rows that act on their own. */
@RunWith(AndroidJUnit4::class)
class SettingsTest : E2eTest() {
    @Test
    fun defaultAppSheetListsOnlyTheInstalledApp() {
        launchToHome()
        openSettings()
        compose.tap(hasTextOf(text(R.string.settings_default_app)))
        val whatsApp = MessagingApp.WHATSAPP.label
        compose.waitFor(hasTextOf(whatsApp) and isSelectable())
        // Only the stand-in (package com.whatsapp) is installed, so it is the only choice.
        val choices = compose.onAllNodes(isSelectable()).fetchSemanticsNodes().flatMap { it.texts() }.filter { it.isNotEmpty() }
        assertEquals("apps offered", listOf(whatsApp), choices)
        capture("default_app_sheet")
        compose.tap(hasTextOf(whatsApp) and isSelectable())
        compose.waitFor(hasTextOf(text(R.string.toast_default_app, whatsApp)))
    }

    @Test
    fun clearRecentNumbersAsksFirst_thenClears() {
        launchToHome()
        sendOnce()
        openSettings()
        compose.tap(hasTextOf(text(R.string.settings_clear_recents)) and hasTextOf("1"))
        compose.waitFor(hasTextOf(text(R.string.clear_recents_title)))
        capture("clear_recents")
        compose.tap(hasTextOf(text(R.string.clear_recents_clear)))
        compose.waitFor(hasTextOf(text(R.string.toast_recents_cleared)))
        // Nothing left to clear: the row shows 0 and is disabled.
        compose.waitFor(hasTextOf(text(R.string.settings_clear_recents)) and hasTextOf("0")).assertIsNotEnabled()
    }

    @Test
    fun rateUsGoesFromFiveStarsToTheStoreLink() {
        launchToHome()
        openSettings()
        compose.tap(hasTextOf(text(R.string.settings_rate)))
        compose.waitFor(hasTextOf(text(R.string.rating_title)))
        compose.tap(hasDescriptionOf(Strings.currentPlural(R.plurals.rating_star_cd, 5)))
        compose.waitFor(hasTextOf(text(R.string.rating_store_cta)), timeoutMs = 20_000)
        capture("rating_store")
        // The emulator image has no Play Store: the listing opens in a browser if there is one (ruling R12),
        // otherwise the app says that nothing can open it.
        expectExternalAppOrToast(
            tap = { compose.tap(hasTextOf(text(R.string.rating_store_cta))) },
            toasts = listOf(text(R.string.toast_play), text(R.string.toast_no_app_can_open)),
        )
    }

    @Test
    fun contactUsNeedsText_thenHandsTheNoteToEmail() {
        launchToHome()
        openSettings()
        compose.tap(hasTextOf(text(R.string.settings_contact)))
        compose.waitFor(hasTextOf(text(R.string.contact_title)))

        val send = hasTextOf(text(R.string.contact_send)) and hasClickAction()
        val emptySend = compose.waitFor(send)
        if (emptySend.fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)) {
            // The screen disables Send until there is text; the ViewModel's "write something first" guards the same.
            emptySend.assertIsNotEnabled()
        } else {
            emptySend.performClick()
            compose.waitFor(hasTextOf(text(R.string.toast_write_first)))
        }
        capture("contact_empty")

        compose.waitFor(hasSetTextAction() and hasTextOf(text(R.string.contact_placeholder))).performTextInput("The number does not open")
        // No email app on the emulator image: the app says so (ruling R11); with one, the composer opens.
        expectExternalAppOrToast(
            tap = { compose.tap(send) },
            toasts = listOf(text(R.string.toast_no_email), text(R.string.toast_sent)),
        )
    }

    /** A recent number through the real send flow: the stand-in bounces, "Edit number" closes the sheet. */
    private fun sendOnce() {
        compose.waitFor(hasSetTextAction() and hasTextOf(text(R.string.home_phone_placeholder))).performTextInput("+6281234567890")
        compose.waitFor(hasSetTextAction() and hasDigits("81234567890"))
        compose.tap(hasTextOf(text(R.string.home_send)))
        Waits.until("the stand-in to receive the chat link") { Fixture.sends().isNotEmpty() }
        compose.waitFor(hasTextOf(text(R.string.not_on_wa_title)), timeoutMs = 30_000)
        compose.tap(hasTextOf(text(R.string.not_on_wa_edit)))
        compose.waitFor(hasDigits("6281234567890") and hasClickAction())
    }

    /**
     * Runs [tap], then waits until another app comes to the front (then goes back to OpenChat) or one of [toasts]
     * shows in OpenChat. The strings are resolved before the tap, while OpenChat is in front.
     */
    private fun expectExternalAppOrToast(tap: () -> Unit, toasts: List<String>) {
        tap()
        var external = false
        Waits.until("another app to open or one of $toasts to show", timeoutMs = 20_000, pollMs = 200) {
            if (Activities.resumed() !is MainActivity) {
                external = true
                true
            } else {
                toasts.any { compose.exists(hasTextOf(it)) }
            }
        }
        if (external) {
            Capture.screen("flows", "SettingsTest-external_app")
            repeat(3) {
                if (Activities.resumed() is MainActivity) return
                device.pressBack()
                runCatching { Waits.until("OpenChat to come back", timeoutMs = 5_000, pollMs = 100) { Activities.resumed() is MainActivity } }
            }
            Activities.resumedMain()
        } else {
            capture("toast")
        }
    }
}
