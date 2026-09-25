package com.piptechnologies.openchat.e2e

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

/**
 * The system's "… isn't responding" and "… keeps stopping" dialogs (package "android"). A freshly cold-booted
 * emulator with software rendering often shows one for System UI or the launcher: it sits in front of whatever a test
 * opens next, which the Compose rule does not notice but UiAutomator does (device run 19, API 34: GateTest saw
 * "android" in front instead of Settings). Only dialogs about other apps are answered; one about OpenChat or the
 * stand-in is left on screen for the test to fail on.
 */
object SystemDialogs {
    private val ours = listOf("OpenChat", "E2E stand-in")

    /** Answers one such dialog if it is showing ("Wait" when it offers it, else "Close app"). True if it did. */
    fun dismissOthers(device: UiDevice): Boolean {
        val wait = device.findObject(By.res("android", "aerr_wait"))
        val close = device.findObject(By.res("android", "aerr_close"))
        val button = wait ?: close ?: return false
        val title = runCatching { device.findObject(By.res("android", "alertTitle"))?.text }.getOrNull().orEmpty()
        if (ours.any { title.contains(it) }) return false
        runCatching { button.click() }
        device.waitForIdle()
        return true
    }

    /** The focused window and app as the window manager reports them, for failure messages. */
    fun focus(): String =
        Shell.run("dumpsys window").lines()
            .filter { it.contains("mCurrentFocus") || it.contains("mFocusedApp") }
            .joinToString("; ") { it.trim() }
}
