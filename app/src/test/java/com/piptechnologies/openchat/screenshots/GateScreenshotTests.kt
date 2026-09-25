package com.piptechnologies.openchat.screenshots

import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.uiText
import com.piptechnologies.openchat.ui.gate.GateScreen
import com.piptechnologies.openchat.ui.gate.GateUiState
import org.junit.Rule
import org.junit.Test

/** Notification access gate before and after the grant, opened from Messages (design map §4.7). */
object GateScenes {
    val gateBefore = Scene("gate_before") {
        GateScreen(
            state = GateUiState(title = uiText(R.string.gate_bar_messages), granted = false, waitingForSystem = false),
            onBack = {},
            onOpenSettings = {},
            onContinue = {},
        )
    }

    val gateAfter = Scene("gate_after") {
        GateScreen(
            state = GateUiState(title = uiText(R.string.gate_bar_messages), granted = true, waitingForSystem = false),
            onBack = {},
            onOpenSettings = {},
            onContinue = {},
        )
    }

    val all: List<Scene> = listOf(gateBefore, gateAfter)
}

/** [GateScenes] in English. */
class GateScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    @Test
    fun gate_before() = paparazzi.snapshot(GateScenes.gateBefore)

    @Test
    fun gate_after() = paparazzi.snapshot(GateScenes.gateAfter)
}
