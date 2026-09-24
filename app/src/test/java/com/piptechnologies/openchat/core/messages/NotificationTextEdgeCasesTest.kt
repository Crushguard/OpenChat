package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cases beyond the brief's NotificationTextTest: every placeholder variant, the documented noise, kind and title edges. */
class NotificationTextEdgeCasesTest {
    @Test fun `every deleted placeholder variant`() {
        listOf(
            "This message was deleted", "🚫 This message was deleted", "This message was deleted.", "🚫 This message was deleted.",
            "🚫\uFE0F This message was deleted", "THIS MESSAGE WAS DELETED", "  this message was deleted  ",
            "\u200EThis message was deleted\u200F", "Pesan ini telah dihapus", "Essa mensagem foi apagada",
            "Esta mensagem foi apagada.", "Se eliminó este mensaje", "Se elimino\u0301 este mensaje", "Bu mesaj silindi",
            "यह मैसेज हटा दिया गया", "यह मैसेज हटा दिया गया।", "یہ پیغام حذف کر دیا گیا", "یہ پیغام حذف کر دیا گیا۔",
            "This message was deleted by admin Zeeshan", "🚫 This message was deleted by admin Zeeshan.",
            "This message was deleted by an admin",
        ).forEach { assertTrue(it, NotificationText.isDeletedPattern(it)) }
        listOf("", "🚫", "This message was deleted lol", "Deleted", "This message was delightful", "You deleted this message")
            .forEach { assertFalse(it, NotificationText.isDeletedPattern(it)) }
    }
    @Test fun `noise examples from the kdoc`() {
        listOf(
            "12 messages from 2 chats", "You have 3 new messages", "Checking for new messages", "You may have new messages",
            "5 new messages", "1 message", "Tap to view", "Backup in progress", "Finished backup", "Restoring", "Calling…",
            "Incoming voice call", "Ongoing voice call", "Missed video call",
        ).forEach { assertTrue(it, NotificationText.isSummaryOrNoise("Ayu", it)) }
        assertTrue(NotificationText.isSummaryOrNoise("WhatsApp", "Messages"))
        assertTrue(NotificationText.isSummaryOrNoise("WhatsApp Business", "New messages"))
        assertTrue(NotificationText.isSummaryOrNoise("whatsapp", "chats"))
        assertFalse(NotificationText.isSummaryOrNoise("Ayu", "Messages"))
        assertFalse(NotificationText.isSummaryOrNoise("WhatsApp", "Hello"))
        assertFalse(NotificationText.isSummaryOrNoise("Ayu", "See you at 5"))
    }
    @Test fun `kinds by emoji prefix or exact word`() {
        mapOf(
            "Photo" to MessageKind.PHOTO, "photo" to MessageKind.PHOTO, " 📷 Photo " to MessageKind.PHOTO,
            "📷 Look at this" to MessageKind.PHOTO, "\u200E📷 Photo" to MessageKind.PHOTO, "Photo of the beach" to MessageKind.TEXT,
            "🎥 Video" to MessageKind.VIDEO, "Video" to MessageKind.VIDEO, "Voice message" to MessageKind.VOICE,
            "🎵 Audio" to MessageKind.AUDIO, "AUDIO" to MessageKind.AUDIO, "Document" to MessageKind.DOCUMENT,
            "Sticker" to MessageKind.STICKER, "🧩" to MessageKind.STICKER, "GIF" to MessageKind.GIF, "gif" to MessageKind.GIF,
            "👤 Budi" to MessageKind.CONTACT, "Contact" to MessageKind.CONTACT, "📍 Live location" to MessageKind.LOCATION,
            "Live location" to MessageKind.LOCATION, "Location" to MessageKind.LOCATION,
            "🚫 This message was deleted." to MessageKind.DELETED, "" to MessageKind.TEXT,
        ).forEach { (text, kind) -> assertEquals(text, kind, NotificationText.kindOf(text)) }
    }
    @Test fun `numbers initials keys and hashes`() {
        assertEquals("6281399220417", NotificationText.phoneNumberFrom("\u202A+62 813-9922-0417\u202C"))
        assertEquals("6281399220417", NotificationText.phoneNumberFrom("+62\u00A0813\u20119922\u20110417"))
        assertEquals("14155550132", NotificationText.phoneNumberFrom("+1 (415) 555-0132"))
        assertEquals("081399220417", NotificationText.phoneNumberFrom("0813 9922 0417"))
        assertEquals("1234567", NotificationText.phoneNumberFrom("1234567"))
        assertEquals("966501234567", NotificationText.phoneNumberFrom("+٩٦٦ ٥٠ ١٢٣ ٤٥٦٧")) // Arabic-Indic digits
        listOf("123456", "+", "+Ayu", "", "Ayu 1234567", "2024 Reunion", "+62 813 9922 0417 (Work)")
            .forEach { assertNull(it, NotificationText.phoneNumberFrom(it)) }
        assertEquals("?", NotificationText.initialFor(""))
        assertEquals("?", NotificationText.initialFor("   "))
        assertEquals("?", NotificationText.initialFor("😊"))
        assertEquals("#", NotificationText.initialFor("0812"))
        assertEquals("#", NotificationText.initialFor("\u202A+62 813\u202C"))
        assertEquals("É", NotificationText.initialFor("émile"))
        assertEquals("A", NotificationText.initialFor("~ayu"))
        assertEquals("A", NotificationText.initialFor("\u200Eayu"))
        assertEquals("com.whatsapp.w4b|budi", NotificationText.conversationKey(NotificationText.WHATSAPP_BUSINESS, "BUDI"))
        assertEquals("hi".hashCode(), NotificationText.textHash(" hi "))
        assertEquals(setOf("com.whatsapp", "com.whatsapp.w4b"), NotificationText.watchedPackages)
    }
}
