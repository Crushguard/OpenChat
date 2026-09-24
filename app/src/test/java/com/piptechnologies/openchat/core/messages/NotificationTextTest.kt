package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTextTest {
    @Test fun `deleted patterns`() {
        assertTrue(NotificationText.isDeletedPattern("This message was deleted"))
        assertTrue(NotificationText.isDeletedPattern("🚫 This message was deleted."))
        assertTrue(NotificationText.isDeletedPattern("Pesan ini telah dihapus"))
        assertFalse(NotificationText.isDeletedPattern("This message was delightful"))
    }
    @Test fun `kinds`() {
        assertEquals(MessageKind.PHOTO, NotificationText.kindOf("📷 Photo"))
        assertEquals(MessageKind.VOICE, NotificationText.kindOf("🎤 Voice message (0:12)"))
        assertEquals(MessageKind.DOCUMENT, NotificationText.kindOf("📄 invoice.pdf"))
        assertEquals(MessageKind.DELETED, NotificationText.kindOf("This message was deleted"))
        assertEquals(MessageKind.TEXT, NotificationText.kindOf("Sorry, wrong chat"))
    }
    @Test fun `noise`() {
        assertTrue(NotificationText.isSummaryOrNoise("WhatsApp", "12 messages from 2 chats"))
        assertTrue(NotificationText.isSummaryOrNoise("WhatsApp", "Checking for new messages"))
        assertFalse(NotificationText.isSummaryOrNoise("Ayu Lestari", "Sorry, wrong chat"))
    }
    @Test fun `keys numbers initials`() {
        assertEquals("com.whatsapp|ayu lestari", NotificationText.conversationKey("com.whatsapp", " Ayu Lestari "))
        assertEquals("6281399220417", NotificationText.phoneNumberFrom("+62 813-9922-0417"))
        assertNull(NotificationText.phoneNumberFrom("Ayu Lestari"))
        assertEquals("#", NotificationText.initialFor("+62 813 9922 0417"))
        assertEquals("A", NotificationText.initialFor("ayu"))
    }
}
