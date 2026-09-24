package com.piptechnologies.openchat.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.piptechnologies.openchat.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Design map §4.14, §5.4 (README "Second account"): the entry screen, then Scan QR (the notification permission is
 * granted beforehand, so no dialog) opens the WhatsApp Web session in a WebView, still "Linking…". Nothing is linked.
 */
@RunWith(AndroidJUnit4::class)
class SecondAccountTest : E2eTest() {
    @Test
    fun scanQrOpensTheWebSession_inTheLinkingState() {
        Grants.postNotifications(Grants.APP)
        launchToHome()

        openTool(R.string.tool_second_title)
        compose.waitFor(hasTextOf(text(R.string.second_entry_title)))
        capture("entry")

        compose.tap(hasTextOf(text(R.string.second_scan)))
        compose.waitFor(hasTextOf(text(R.string.second_linking)), timeoutMs = 30_000)
        assertTrue("a WebView is on screen", device.wait(Until.hasObject(By.clazz("android.webkit.WebView")), 30_000))
        capture("linking")

        back()
        waitForHome()
    }
}
