package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [NotificationText.isSystemNotification]: the language-independent filter, as a truth table. */
class SystemNotificationFilterTest {
    private fun system(
        category: String? = null,
        ongoing: Boolean = false,
        foregroundService: Boolean = false,
        showsProgress: Boolean = false,
        fromMessagingStyle: Boolean = true,
        titleIsAppLabel: Boolean = false,
    ) = NotificationText.isSystemNotification(category, ongoing, foregroundService, showsProgress, fromMessagingStyle, titleIsAppLabel)

    @Test fun `a chat notification is not a system one`() {
        assertFalse(system(category = "msg"))
        assertFalse(system(category = null))
        assertFalse(system(category = "msg", fromMessagingStyle = false)) // plain text titled with the chat's name
        assertFalse(system(category = "msg", titleIsAppLabel = true)) // MessagingStyle under the app label: the noise rules decide
    }

    @Test fun `system categories`() {
        // Notification.CATEGORY_CALL, _MISSED_CALL, _PROGRESS, _SERVICE, _STATUS, _TRANSPORT, _SYSTEM, _ALARM, _ERROR.
        listOf("call", "missed_call", "progress", "service", "status", "transport", "sys", "alarm", "err").forEach {
            assertTrue(it, system(category = it))
            assertTrue(it, system(category = it, fromMessagingStyle = true, titleIsAppLabel = false))
        }
        // Categories a chat notification can carry.
        listOf("msg", "social", "email", "event", "reminder", "promo", "recommendation", "", "CALL").forEach {
            assertFalse(it, system(category = it))
        }
    }

    @Test fun `every combination of the flags`() {
        // Without a system category, only these (ongoing, foregroundService, showsProgress, fromMessagingStyle,
        // titleIsAppLabel) combinations can be chat lines: every flag off, and no app-labelled plain-text notification.
        val chatCapable = setOf(
            listOf(false, false, false, true, false),
            listOf(false, false, false, true, true),
            listOf(false, false, false, false, false),
        )
        for (bits in 0 until 32) {
            val flags = List(5) { (bits and (1 shl it)) != 0 }
            for (category in listOf(null, "msg")) {
                val result = NotificationText.isSystemNotification(category, flags[0], flags[1], flags[2], flags[3], flags[4])
                assertEquals("$category $flags", flags !in chatCapable, result)
            }
            assertTrue("call $flags", NotificationText.isSystemNotification("call", flags[0], flags[1], flags[2], flags[3], flags[4]))
        }
    }

    @Test fun `app label`() {
        listOf("WhatsApp", "whatsapp", "WhatsApp Business", "WHATSAPP BUSINESS", " WhatsApp ", "\u200EWhatsApp\u200F", "\u2068WhatsApp\u2069")
            .forEach { assertTrue(it, NotificationText.isAppLabel(it)) }
        listOf("", "Ayu", "WhatsApp Web", "WhatsApp Support", "Whats App", "WA Business")
            .forEach { assertFalse(it, NotificationText.isAppLabel(it)) }
    }
}
