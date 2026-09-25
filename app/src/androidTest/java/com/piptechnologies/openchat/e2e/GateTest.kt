package com.piptechnologies.openchat.e2e

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R
import java.util.regex.Pattern
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Design map §4.7 (ruling R17): without notification access a tool row opens the gate (chip Off); "Open settings"
 * opens the system's notification-access settings; once access is granted there, coming back re-checks it (chip
 * Active) and Continue opens the tool.
 */
@RunWith(AndroidJUnit4::class)
class GateTest : E2eTest() {
    @Test
    fun gateOpensTheSystemSettings_andTurnsActiveWhenAccessIsGranted() {
        Grants.notificationListener(allowed = false)
        launchToHome()

        openTool(R.string.tool_unseen_title)
        compose.waitFor(hasTextOf(text(R.string.gate_title)))
        compose.waitFor(hasTextOf(text(R.string.chip_off)))
        capture("gate_off")

        compose.tap(hasTextOf(text(R.string.gate_open_settings)))
        val settingsApp = Pattern.compile("(?i).*settings.*")
        if (!settingsInFront(settingsApp)) {
            throw AssertionError("the system settings did not open (in front: ${device.currentPackageName}; ${SystemDialogs.focus()})")
        }
        val inFront = device.currentPackageName
        assertTrue("the settings app is in front, not $inFront", inFront != Grants.APP && inFront.contains("settings", ignoreCase = true))
        Capture.screen("flows", "GateTest-system_settings")

        // Granted while the settings screen is up, as the user would toggle it there.
        Grants.notificationListener(allowed = true)
        returnToApp()

        compose.waitFor(hasTextOf(text(R.string.chip_active)))
        capture("gate_active")
        compose.tap(hasTextOf(text(R.string.gate_continue)))
        // "Messages" is also the gate's own bar title: wait for the gate to go and the Messages screen's own button.
        compose.waitForGone(hasTextOf(text(R.string.gate_title)))
        compose.waitFor(hasDescriptionOf(text(R.string.messages_tool_settings)))
        compose.waitFor(hasTextOf(text(R.string.messages_title)))
        capture("messages")
    }

    /**
     * Waits up to 20 s for a settings app at the top of the screen, answering a system "isn't responding" dialog about
     * another app on the way (the cold-booted emulator's System UI, typically), which would otherwise cover it.
     */
    private fun settingsInFront(settingsApp: Pattern): Boolean {
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.wait(Until.hasObject(By.pkg(settingsApp).depth(0)), 2_000)) return true
            SystemDialogs.dismissOthers(device)
        }
        return false
    }

    /** Back from the settings screen until OpenChat is in front again (bounded: three presses). */
    private fun returnToApp() {
        repeat(3) {
            device.pressBack()
            val back = runCatching {
                Waits.until("OpenChat to come back", timeoutMs = 5_000, pollMs = 100) { Activities.resumed() is MainActivity }
            }.isSuccess
            if (back) return
        }
        Activities.resumedMain()
    }
}
