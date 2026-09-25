package com.piptechnologies.openchat.ui.theme

import androidx.compose.ui.unit.TextUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** [forScriptOf]: mono labels keep JetBrains Mono in the scripts it covers, and take Hanken Grotesk otherwise. */
class ForScriptOfTest {
    private val eyebrow = OcTypography().eyebrow11

    @Test
    fun `Latin, Greek, Cyrillic, digits and punctuation keep the mono style`() {
        listOf("RECENT", "ZULETZT VERWENDET", "НЕДАВНИЕ", "ΠΡΟΣΦΑΤΑ", "TODAY · 2", "2h", "14:25", "SON KULLANILANLAR").forEach {
            assertSame(it, eyebrow, eyebrow.forScriptOf(it))
        }
    }

    @Test
    fun `other scripts take Hanken Grotesk without the tracking`() {
        listOf("हाल के नंबर", "الأرقام الأخيرة", "最近使用", "မကြာသေးမီက", "אחרונים", "2 घं॰").forEach {
            val style = eyebrow.forScriptOf(it)
            assertEquals(it, HankenGrotesk, style.fontFamily)
            assertEquals(it, TextUnit.Unspecified, style.letterSpacing)
            assertEquals(it, eyebrow.fontSize, style.fontSize)
            assertEquals(it, eyebrow.fontWeight, style.fontWeight)
        }
    }

    @Test
    fun `a sans style is left alone`() {
        val title = OcTypography().title16
        assertSame(title, title.forScriptOf("हाल के नंबर"))
    }
}
