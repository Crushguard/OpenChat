package com.piptechnologies.openchat.e2e

import android.net.Uri
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.piptechnologies.openchat.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Design map §4.3, §4.6, §5.1 (README "Open a chat with any number"): a pasted international number, a message and
 * Send open https://wa.me/<digits>?text=<encoded> in the installed stand-in; it hands straight back, which the app
 * reads as "not on WhatsApp" (ruling R6); the number lands in Recent and can be deleted there.
 */
@RunWith(AndroidJUnit4::class)
class SendFlowTest : E2eTest() {
    @Test
    fun sendOpensTheChatLink_aQuickReturnShowsNotOnWhatsApp_andTheRecentCanBeDeleted() {
        launchToHome()
        val message = "Hi! Is the blue one still available?"

        // Typed all at once into the empty field it counts as a paste: "+62" picks Indonesia, the rest stays.
        compose.waitFor(hasSetTextAction() and hasTextOf(text(R.string.home_phone_placeholder))).performTextInput("+6281234567890")
        compose.waitFor(hasDescriptionOf(text(R.string.home_country_cd)) and hasTextOf("+62"))
        compose.waitFor(hasSetTextAction() and hasDigits("81234567890"))
        compose.waitFor(hasSetTextAction() and hasTextOf(text(R.string.home_message_placeholder))).performTextInput(message)
        compose.waitFor(hasSetTextAction() and hasTextOf(message))
        capture("filled")

        compose.tap(hasTextOf(text(R.string.home_send)))
        Waits.until("the stand-in to receive the chat link") { Fixture.sends().isNotEmpty() }
        val sent = Fixture.sends().last()
        val link = Uri.parse(sent)
        assertEquals("scheme of $sent", "https", link.scheme)
        assertEquals("host of $sent", "wa.me", link.host)
        assertEquals("number in $sent", "/6281234567890", link.path)
        assertEquals("message in $sent", message, link.getQueryParameter("text"))
        assertFalse("the message is URL-encoded in $sent", link.encodedQuery.orEmpty().contains(' '))

        // The stand-in closed at once, so OpenChat is back within 7 s: it suspects the number has no account.
        compose.waitFor(hasTextOf(text(R.string.not_on_wa_title)), timeoutMs = 30_000)
        capture("not_on_whatsapp")
        compose.tap(hasTextOf(text(R.string.not_on_wa_edit)))
        compose.waitForGone(hasTextOf(text(R.string.not_on_wa_title)))

        compose.waitFor(hasTextOf(text(R.string.home_recent), ignoreCase = true))
        val recent = hasDigits("6281234567890") and hasClickAction()
        val row = compose.waitFor(recent)
        if (runCatching { row.performScrollTo() }.isSuccess) compose.settle()
        capture("recent")
        row.performTouchInput { longClick() }
        compose.settle()
        compose.tap(hasTextOf(text(R.string.home_delete)))
        compose.waitForGone(recent)
        capture("recent_deleted")
    }
}
