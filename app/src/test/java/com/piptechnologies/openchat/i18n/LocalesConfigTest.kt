package com.piptechnologies.openchat.i18n

import com.piptechnologies.openchat.ui.settings.Languages
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The 19 UI languages (plan 2026-09-24) are declared by hand in four places that must agree with
 * [Languages.all]: res/xml/locales_config.xml (Android 13+ per-app language settings, BCP-47 tags),
 * resourceConfigurations in app/build.gradle.kts (strips every other translation from the APK, ours
 * included), the values-<qualifier> folders, and the manifest's android:localeConfig. This pins them
 * together, and pins how a locale list maps to a language ([Languages.match], behind Languages.current).
 */
class LocalesConfigTest {

    private val expectedTags = listOf(
        "en", "id", "pt-BR", "pt", "ur", "hi", "tr", "es", "ar", "fa", "ps", "he", "fr", "de", "it", "ru", "zh", "ha", "my",
    )
    private val expectedQualifiers = listOf(
        "en", "in", "pt-rBR", "pt", "ur", "hi", "tr", "es", "ar", "fa", "ps", "iw", "fr", "de", "it", "ru", "zh", "ha", "my",
    )

    @Test
    fun languages_areTheNineteenOfThePlanInOrder() {
        assertEquals(expectedTags, Languages.all.map { it.tag })
        assertEquals(expectedQualifiers, Languages.all.map { it.qualifier })
    }

    @Test
    fun localesConfig_listsTheLanguagesInOrder() {
        assertEquals(Languages.all.map { it.tag }, localeConfigTags())
    }

    @Test
    fun localesConfig_usesBcp47TagsNotFolderCodes() {
        assertEquals(emptyList<String>(), localeConfigTags().filter { it == "in" || it == "iw" })
    }

    @Test
    fun manifest_pointsAtTheLocalesConfig() {
        assertTrue(moduleFile("src/main/AndroidManifest.xml").readText().contains("android:localeConfig=\"@xml/locales_config\""))
    }

    @Test
    fun resourceConfigurations_areTheLanguageQualifiersInOrder() {
        assertEquals(Languages.all.map { it.qualifier }, resourceConfigurations())
    }

    @Test
    fun rtl_isExactlyArabicPersianUrduPashtoAndHebrew() {
        assertEquals(setOf("ar", "fa", "ur", "ps", "he"), Languages.all.filter { it.rtl }.map { it.tag }.toSet())
    }

    @Test
    fun byTag_acceptsLegacyCodesAndOtherSpellings() {
        assertEquals("id", Languages.byTag("in").tag)
        assertEquals("id", Languages.byTag("id").tag)
        assertEquals("id", Languages.byTag("in-ID").tag)
        assertEquals("he", Languages.byTag("iw").tag)
        assertEquals("he", Languages.byTag("he").tag)
        assertEquals("he", Languages.byTag("iw-IL").tag)
        assertEquals("pt-BR", Languages.byTag("pt-BR").tag)
        assertEquals("pt-BR", Languages.byTag("pt_BR").tag)
        assertEquals("pt-BR", Languages.byTag("PT-br").tag)
        assertEquals("pt", Languages.byTag("pt").tag)
        assertEquals("pt", Languages.byTag("pt-PT").tag)
        assertEquals("es", Languages.byTag("es-MX").tag)
        assertEquals("zh", Languages.byTag("zh-CN").tag)
        assertEquals("en", Languages.byTag("zh-TW").tag)
        assertEquals("en", Languages.byTag("zh_HK").tag)
        assertEquals("en", Languages.byTag("zu").tag)
        assertEquals("en", Languages.byTag("").tag)
        for (option in Languages.all) assertEquals(option, Languages.byTag(option.tag))
    }

    @Test
    fun match_exactTagThenLanguageThenEnglish() {
        assertEquals("en", Languages.match(emptyList()).tag)
        assertEquals("pt-BR", match("pt-BR"))
        assertEquals("pt", match("pt"))
        assertEquals("pt", match("pt-PT"))
        assertEquals("pt", match("pt-AO"))
        assertEquals("en", match("zh-TW"))
        assertEquals("en", match("zh-Hant-TW"))
        assertEquals("zh", match("zh-Hans-CN"))
        assertEquals("id", match("in-ID"))
        assertEquals("id", match("id-ID"))
        assertEquals("he", match("iw-IL"))
        assertEquals("he", match("he-IL"))
        assertEquals("en", match("en-GB"))
        assertEquals("ar", match("ar-EG"))
        assertEquals("fa", match("fa-AF"))
        assertEquals("my", match("my-MM"))
        assertEquals("en", match("zu-ZA"))
    }

    @Test
    fun match_regionalVariantsFallBackToTheirLanguage() {
        assertEquals("pt", match("pt-PT"))
        assertEquals("pt-BR", match("pt-BR"))
        assertEquals("pt", match("pt"))
        assertEquals("id", match("id"))
        assertEquals("id", match("in"))
        assertEquals("he", match("he"))
        assertEquals("he", match("iw"))
        assertEquals("es", match("es-MX"))
        assertEquals("fr", match("fr-CA"))
        assertEquals("ur", match("ur-IN"))
    }

    @Test
    fun match_chineseIsSimplifiedOnly() {
        // values-zh holds Simplified Chinese (Hans). Android matches resource folders by script, so a Traditional
        // Chinese locale (Hant, or Taiwan, Hong Kong, Macau without a script) gets the English strings: it must
        // not be reported as 中文.
        assertEquals("zh", match("zh"))
        assertEquals("zh", match("zh-CN"))
        assertEquals("zh", match("zh-Hans"))
        assertEquals("zh", match("zh-SG"))
        assertEquals("zh", match("zh-Hans-HK"))
        assertEquals("en", match("zh-TW"))
        assertEquals("en", match("zh-Hant-TW"))
        assertEquals("en", match("zh-HK"))
        assertEquals("en", match("zh-MO"))
        assertEquals("en", match("zh-Hant"))
        assertEquals("en", match("zh-Hant-CN"))
        // As in Android's resolution, the next language in the list that has strings wins.
        assertEquals("fr", match("zh-TW", "fr-FR"))
        assertEquals("zh", match("zh-HK", "zh-CN"))
    }

    @Test
    fun match_followsTheOrderOfPreference() {
        assertEquals("fr", match("zu-ZA", "fr-CA", "de-DE"))
        assertEquals("de", match("de-AT", "pt-BR"))
        // A language match on the first choice beats an exact match on a later one.
        assertEquals("pt", match("pt-PT", "pt-BR"))
        assertEquals("en", match("zu-ZA", "xh-ZA"))
    }

    @Test
    fun localeFolders_useTheQualifiersOfTheLanguages() {
        // values-<language>[-r<REGION>] or values-b+…; "car" is the one three-letter qualifier that is not a language.
        val localeFolder = Regex("values-([a-z]{2,3}(?:-r[A-Z]{2})?|b\\+[^-]+)(?:-.+)?")
        val known = Languages.all.map { it.qualifier }.toSet()
        val strays = resFolders().mapNotNull { localeFolder.matchEntire(it)?.groupValues?.get(1) }
            .filter { it !in known && it != "car" }
        assertEquals("folders resourceConfigurations would strip, or Android would never pick", emptyList<String>(), strays)
    }

    @Test
    fun everyTranslationHasItsValuesFolder() {
        // Translations land in wave V2: skipped until the first values-<qualifier> folder exists; from then on
        // every language needs its folder, named with the qualifier resourceConfigurations keeps.
        val translated = Languages.all.drop(1).map { it.qualifier }
        val present = translated.filter { "values-$it" in resFolders() }
        assumeTrue("no translations yet (wave V2)", present.isNotEmpty())
        assertEquals(translated, present)
    }

    private fun match(vararg tags: String): String = Languages.match(tags.map { Locale.forLanguageTag(it) }).tag

    private fun localeConfigTags(): List<String> {
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(moduleFile("src/main/res/xml/locales_config.xml"))
        assertEquals("locale-config", doc.documentElement.tagName)
        val nodes = doc.getElementsByTagName("locale")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
    }

    private fun resourceConfigurations(): List<String> {
        val gradle = moduleFile("build.gradle.kts").readText()
        assertEquals("resourceConfigurations declarations", 1, Regex("\\bresourceConfigurations\\b").findAll(gradle).count())
        val list = Regex("resourceConfigurations\\s*\\+=\\s*listOf\\(([^)]*)\\)").find(gradle)?.groupValues?.get(1)
            ?: throw AssertionError("no `resourceConfigurations += listOf(…)` in app/build.gradle.kts")
        return Regex("\"([^\"]+)\"").findAll(list).map { it.groupValues[1] }.toList()
    }

    private fun resFolders(): List<String> =
        moduleFile("src/main/res").listFiles().orEmpty().filter { it.isDirectory }.map { it.name }

    /** [path] in the app module: Gradle runs unit tests in the module directory; other runners may use the repository root. */
    private fun moduleFile(path: String): File {
        val workingDir = File(System.getProperty("user.dir")).absoluteFile
        val module = listOf(workingDir, File(workingDir, "app")).firstOrNull { File(it, "src/main/AndroidManifest.xml").isFile }
            ?: throw AssertionError("app module not found from ${workingDir.path}")
        return File(module, path)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
