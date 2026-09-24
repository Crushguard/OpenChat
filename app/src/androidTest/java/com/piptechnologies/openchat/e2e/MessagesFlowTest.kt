package com.piptechnologies.openchat.e2e

import android.os.SystemClock
import androidx.compose.ui.test.hasClickAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.piptechnologies.openchat.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Design map §4.8, §4.9, §5.2 (README "Read without blue ticks", "Recover deleted messages"): chat notifications
 * posted by the stand-in are captured by the real notification listener, listed with their unread count, shown in
 * the conversation, and a message the sender deletes (the notification line turns into the placeholder) is kept
 * under "Deleted by sender". The app's own status notification never becomes a conversation.
 */
@RunWith(AndroidJUnit4::class)
class MessagesFlowTest : E2eTest() {
    @Test
    fun chatNotificationsAreListedAndOpened_andADeletedMessageIsKept() {
        Grants.notificationListener(allowed = true)
        launchToHome()

        val start = System.currentTimeMillis() - 120_000
        val first = ChatLine(start, AYU, "Hi kak, is the blue one still available?")
        val second = ChatLine(start + 60_000, AYU, "I can do 300k if you ship today")
        Fixture.postChat(AYU, listOf(first, second))

        openTool(R.string.tool_unseen_title)
        compose.waitFor(hasTextOf(text(R.string.messages_title)))
        val ayu = hasTextOf(AYU) and hasClickAction()
        // Two unread messages: the row's badge reads 2.
        compose.waitFor(ayu and hasTextOf("2"), timeoutMs = 30_000)
        capture("all")

        compose.tap(ayu)
        compose.waitFor(hasTextOf(first.text))
        compose.waitFor(hasTextOf(second.text))
        capture("conversation")

        // The sender deletes the second message: WhatsApp keeps its time and replaces the text with the placeholder.
        Fixture.postChat(AYU, listOf(first, second.copy(text = DELETED_PLACEHOLDER)))
        back()
        compose.tap(hasTextOf(text(R.string.messages_chip_deleted, 1)), timeoutMs = 30_000)
        compose.waitFor(ayu)
        capture("deleted_only")

        compose.tap(ayu)
        val label = compose.waitFor(hasTextOf(text(R.string.conversation_deleted_label), ignoreCase = true)).fetchSemanticsNode()
        val original = compose.waitFor(hasTextOf(second.text)).fetchSemanticsNode()
        assertTrue(
            "the \"deleted by sender\" label sits right above the original text",
            label.boundsInWindow.bottom <= original.boundsInWindow.top + 1f &&
                original.boundsInWindow.top - label.boundsInWindow.bottom < 200f,
        )
        assertFalse("the placeholder is not shown as a message", compose.exists(hasTextOf(DELETED_PLACEHOLDER)))
        capture("deleted_conversation")
        back()
        back()
        waitForHome()

        // WhatsApp's own "Checking for new messages" status notification, then a real chat. The listener handles them
        // in order, so once the chat is listed the status notification has been seen, and it must not be a conversation.
        Fixture.postSystem()
        Fixture.postChat(BUDI, listOf(ChatLine(System.currentTimeMillis(), BUDI, "Order is on the way")))
        openTool(R.string.tool_unseen_title)
        compose.waitFor(hasTextOf(BUDI) and hasClickAction(), timeoutMs = 30_000)
        compose.waitFor(ayu)
        assertStaysAbsent(hasTextOf(APP_LABEL) and hasClickAction(), forMs = 3_000)
        capture("all_after_status")
    }

    private fun assertStaysAbsent(matcher: androidx.compose.ui.test.SemanticsMatcher, forMs: Long) {
        val end = SystemClock.uptimeMillis() + forMs
        while (SystemClock.uptimeMillis() < end) {
            assertFalse("unexpected node with ${matcher.description}", compose.exists(matcher))
            Thread.sleep(200)
        }
    }

    private companion object {
        const val AYU = "Ayu Lestari"
        const val BUDI = "Budi Santoso"

        /** What the stand-in's notification shows, like WhatsApp in English, once the sender deleted a message. */
        const val DELETED_PLACEHOLDER = "This message was deleted"

        /** The title of the app's own status notification and group summary. */
        const val APP_LABEL = "WhatsApp"
    }
}
