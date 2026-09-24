package com.piptechnologies.openchat.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** ICU skeleton results rewritten for java.text.SimpleDateFormat (see [javaDatePattern]). */
class JavaDatePatternTest {
    @Test
    fun `stand-alone weekday letters become E`() {
        assertEquals("EEE, d MMM", javaDatePattern("ccc, d MMM"))
        assertEquals("EEEE d MMM", javaDatePattern("cccc d MMM"))
        assertEquals("EEE d MMM", javaDatePattern("eee d MMM"))
    }

    @Test
    fun `quoted literals are kept`() {
        assertEquals("d 'de' MMM", javaDatePattern("d 'de' MMM"))
        assertEquals("EEE d 'de' MMM", javaDatePattern("ccc d 'de' MMM"))
        assertEquals("d 'ce' MMM", javaDatePattern("d 'ce' MMM"))
    }

    @Test
    fun `escaped apostrophes do not flip quoting`() {
        assertEquals("h 'o''clock' EEE", javaDatePattern("h 'o''clock' ccc"))
        assertEquals("'' EEE", javaDatePattern("'' ccc"))
    }

    @Test
    fun `patterns without ICU-only letters are unchanged`() {
        assertEquals("d MMM", javaDatePattern("d MMM"))
        assertEquals("EEE d MMM", javaDatePattern("EEE d MMM"))
        assertEquals("d. MMM", javaDatePattern("d. MMM"))
    }
}
