package com.piptechnologies.openchat.ui.components

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** [uiLocaleFor]: formatting follows the language whose strings Android shows, English when it falls back. */
class UiLocaleTest {
    private fun ui(vararg tags: String): String = uiLocaleFor(tags.map(Locale::forLanguageTag)).toLanguageTag()

    @Test
    fun `a supported locale keeps its region`() {
        assertEquals("ar-EG", ui("ar-EG"))
        assertEquals("pt-BR", ui("pt-BR"))
        assertEquals("pt-PT", ui("pt-PT"))
        assertEquals("en-GB", ui("en-GB"))
    }

    @Test
    fun `a bare Portuguese choice formats as European Portuguese, like values-pt`() {
        assertEquals("pt-PT", ui("pt"))
    }

    @Test
    fun `an unsupported first choice gives way to the next supported one`() {
        assertEquals("fr-FR", ui("nl-NL", "fr-FR"))
        assertEquals("de-DE", ui("zh-TW", "de-DE"))
    }

    @Test
    fun `falling back to English formats in US English`() {
        assertEquals("en-US", ui("nl-NL"))
        assertEquals("en-US", ui("zh-TW"))
        assertEquals("en-US", ui())
    }
}
