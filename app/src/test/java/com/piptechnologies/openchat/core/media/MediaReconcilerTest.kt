package com.piptechnologies.openchat.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaReconcilerTest {
    private val f = OriginalFile("/r/WhatsApp Images/IMG-1.jpg", "IMG-1.jpg", 10, 1000)
    @Test fun `new files are copied once and duplicates collapse`() {
        val p = MediaReconciler.plan(emptyList(), listOf(f, f.copy()), 5000)
        assertEquals(1, p.toCopy.size); assertTrue(p.toMarkDeleted.isEmpty()); assertTrue(p.toPrune.isEmpty())
    }
    @Test fun `absent originals are marked deleted only once`() {
        val known = listOf(KnownCopy(1, f.path, null, 1000), KnownCopy(2, "/r/gone.jpg", 2000, 1000))
        val p = MediaReconciler.plan(known, emptyList(), 5000)
        assertEquals(listOf(1L), p.toMarkDeleted); assertTrue(p.toCopy.isEmpty())
    }
    @Test fun `old undeleted copies with originals still present are pruned`() {
        val old = KnownCopy(3, f.path, null, 0)
        val p = MediaReconciler.plan(listOf(old), listOf(f), MediaReconciler.KEEP_UNDELETED_MS + 1)
        assertEquals(listOf(3L), p.toPrune); assertTrue(p.toMarkDeleted.isEmpty())
    }
}
