package com.piptechnologies.openchat.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaClassifierTest {
    private val root = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media"
    @Test fun `by extension and folder`() {
        assertEquals(MediaCategory.PHOTO, MediaClassifier.classify("IMG-1.jpg", null, "$root/WhatsApp Images/IMG-1.jpg"))
        assertEquals(MediaCategory.VIDEO, MediaClassifier.classify("VID-1.mp4", "video/mp4", "$root/WhatsApp Video/VID-1.mp4"))
        assertEquals(MediaCategory.AUDIO, MediaClassifier.classify("PTT-1.opus", null, "$root/WhatsApp Voice Notes/202638/PTT-1.opus"))
        assertEquals(MediaCategory.DOCUMENT, MediaClassifier.classify("invoice.pdf", null, "$root/WhatsApp Documents/invoice.pdf"))
        assertEquals(MediaCategory.STICKER, MediaClassifier.classify("STK-1.webp", "image/webp", "$root/WhatsApp Stickers/STK-1.webp"))
        assertEquals(MediaCategory.PHOTO, MediaClassifier.classify("IMG-2.webp", "image/webp", "$root/WhatsApp Images/IMG-2.webp"))
    }
    @Test fun `skipped and hidden files`() {
        assertNull(MediaClassifier.classify(".nomedia", null, "$root/WhatsApp Images/.nomedia"))
        assertNull(MediaClassifier.classify("IMG-1.jpg", null, "$root/WhatsApp Images/Sent/IMG-1.jpg"))
        assertNull(MediaClassifier.classify("x.jpg", null, "$root/.Statuses/x.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Sent/a.jpg"))
    }
    @Test fun `mime lookup`() { assertEquals("audio/ogg", MediaClassifier.mimeFor("a.opus")); assertNull(MediaClassifier.mimeFor("a.unknownext")) }
}
