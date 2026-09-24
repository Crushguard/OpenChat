package com.piptechnologies.openchat.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppMediaFoldersTest {
    private val root = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media"

    @Test fun `media files and their folders are not skipped`() {
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/IMG-1.jpg"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Voice Notes/202439/PTT-1.opus"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/Other/anything.bin"))
    }

    @Test fun `statuses, sent and hidden entries are skipped`() {
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/.Statuses/status.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Sent/IMG-1.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/.nomedia"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/.trash/IMG-1.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Video/.Thumbs/VID-1.jpg"))
    }

    @Test fun `profile photo and wallpaper folders are skipped, ignoring case`() {
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Profile Photos"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Profile Photos/4915551234567.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/whatsapp profile photos/4915551234567.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WallPaper"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WallPaper/wallpaper.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/Wallpaper/IMG-1.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("WhatsApp/Media/WALLPAPER/IMG-1.jpg"))
    }

    @Test fun `folders whose name contains backup excluded are skipped, ignoring case`() {
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Backup Excluded"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Backup Excluded/IMG-1.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Video/WhatsApp Video Backup Excluded/VID-1.mp4"))
        assertTrue(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/backup excluded/IMG-1.jpg"))
        assertTrue(WhatsAppMediaFolders.isSkipped("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/BACKUP EXCLUDED (2)/AUD-1.m4a"))
    }

    @Test fun `names that only resemble the skipped folders are kept`() {
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/wallpaper.jpg"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Wallpapers/IMG-1.jpg"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Profile/IMG-1.jpg"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Backup/IMG-1.jpg"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Images/Excluded/IMG-1.jpg"))
        assertFalse(WhatsAppMediaFolders.isSkipped("$root/WhatsApp Documents/backup-excluded.pdf"))
    }
}
