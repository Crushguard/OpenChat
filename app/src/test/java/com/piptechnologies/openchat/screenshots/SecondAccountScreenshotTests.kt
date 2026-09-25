package com.piptechnologies.openchat.screenshots

import androidx.compose.runtime.Composable
import com.piptechnologies.openchat.ui.components.ConfirmSheetContent
import com.piptechnologies.openchat.ui.components.HatchedPlaceholder
import com.piptechnologies.openchat.ui.components.SheetPreviewFrame
import com.piptechnologies.openchat.ui.second.SecondAccountScreen
import com.piptechnologies.openchat.ui.second.SecondPhase
import com.piptechnologies.openchat.ui.second.SecondUiState
import com.piptechnologies.openchat.ui.second.logoutSpec
import com.piptechnologies.openchat.ui.theme.OcRadius
import org.junit.Rule
import org.junit.Test

/**
 * Second account entry and linked states, and the log-out confirmation over the linked state (design map
 * §4.14, §4.20). The WebView is replaced by the design's hatched box.
 */
object SecondAccountScenes {
    val secondAccountEntry = Scene("second_account_entry") {
        SecondAccountScreen(
            state = SecondUiState(phase = SecondPhase.ENTRY, confirmLogout = false),
            onBack = {},
            onScan = {},
            onReload = {},
            onAskLogout = {},
            web = {},
        )
    }

    val secondAccountLinked = Scene("second_account_linked") {
        LinkedScreen(confirmLogout = false)
    }

    val dialogLogout = Scene("dialog_logout") {
        SheetPreviewFrame(topRadius = OcRadius.dialog, screen = { LinkedScreen(confirmLogout = true) }) {
            ConfirmSheetContent(spec = logoutSpec(), onCancel = {}, onConfirm = {})
        }
    }

    val all: List<Scene> = listOf(secondAccountEntry, secondAccountLinked, dialogLogout)

    /**
     * The linked state with the design's placeholder in the web slot. Its label is the design's note for WhatsApp Web's
     * own page, which the app does not translate: the same in every language.
     */
    @Composable
    private fun LinkedScreen(confirmLogout: Boolean) {
        SecondAccountScreen(
            state = SecondUiState(phase = SecondPhase.LINKED, confirmLogout = confirmLogout),
            onBack = {},
            onScan = {},
            onReload = {},
            onAskLogout = {},
            web = { HatchedPlaceholder(it, label = "WHATSAPP WEB · WEBVIEW · FULL HEIGHT") },
        )
    }
}

/** [SecondAccountScenes] in English. */
class SecondAccountScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun second_account_entry() = paparazzi.snapshot(SecondAccountScenes.secondAccountEntry)

    @Test
    fun second_account_linked() = paparazzi.snapshot(SecondAccountScenes.secondAccountLinked)

    @Test
    fun dialog_logout() = paparazzi.snapshot(SecondAccountScenes.dialogLogout)
}
