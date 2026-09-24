package com.piptechnologies.openchat.screenshots

import com.piptechnologies.openchat.ui.gate.GateScreen
import com.piptechnologies.openchat.ui.gate.GateUiState
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import org.junit.Rule
import org.junit.Test

/** Notification access gate before and after the grant, opened from Messages (design map §4.7). */
class GateScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun gate_before() {
        paparazzi.snapshot {
            OpenChatTheme {
                GateScreen(
                    state = GateUiState(title = "Messages", granted = false, waitingForSystem = false),
                    onBack = {},
                    onOpenSettings = {},
                    onContinue = {},
                )
            }
        }
    }

    @Test
    fun gate_after() {
        paparazzi.snapshot {
            OpenChatTheme {
                GateScreen(
                    state = GateUiState(title = "Messages", granted = true, waitingForSystem = false),
                    onBack = {},
                    onOpenSettings = {},
                    onContinue = {},
                )
            }
        }
    }
}
