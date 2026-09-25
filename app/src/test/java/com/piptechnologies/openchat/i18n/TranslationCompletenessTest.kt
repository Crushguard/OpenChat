package com.piptechnologies.openchat.i18n

import com.piptechnologies.openchat.ui.settings.Languages
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.w3c.dom.Element
import org.xml.sax.SAXException

/**
 * The CLDR plural categories (as Android's ICU selects them) every <plurals> of a translation must carry, by BCP-47
 * tag: the table of the plan 2026-09-24, and the only one the tests use. tools/i18n/check_translations.py, the
 * translators' copy of these rules, keeps the same table by folder qualifier.
 */
internal val REQUIRED_PLURAL_CATEGORIES: Map<String, Set<String>> = mapOf(
    "id" to setOf("other"),
    "pt-BR" to setOf("one", "many", "other"),
    "pt" to setOf("one", "many", "other"),
    "ur" to setOf("one", "other"),
    "hi" to setOf("one", "other"),
    "tr" to setOf("one", "other"),
    "es" to setOf("one", "many", "other"),
    "ar" to setOf("zero", "one", "two", "few", "many", "other"),
    "fa" to setOf("one", "other"),
    "ps" to setOf("one", "other"),
    "he" to setOf("one", "two", "other"),
    "fr" to setOf("one", "many", "other"),
    "de" to setOf("one", "other"),
    "it" to setOf("one", "many", "other"),
    "ru" to setOf("one", "few", "many", "other"),
    "zh" to setOf("other"),
    "ha" to setOf("one", "other"),
    "my" to setOf("other"),
)

/**
 * Each translation of the 18 languages ([Languages.all] but English) that exists as res/values-<qualifier>, checked
 * against the English res/values/strings*.xml by the rules of tools/i18n/check_translations.py (its errors; its
 * warnings are advice for translators). See [TranslationRules]. A language whose folder does not exist yet is skipped;
 * LocalesConfigTest requires every folder once the first one lands.
 */
@RunWith(Parameterized::class)
class TranslationCompletenessTest(private val tag: String) {

    @Test
    fun translationMatchesTheEnglishStrings() {
        val language = Languages.all.first { it.tag == tag }
        val res = moduleFile("src/main/res")
        assumeTrue("values-${language.qualifier} does not exist yet", File(res, "values-${language.qualifier}").isDirectory)
        val required = REQUIRED_PLURAL_CATEGORIES[tag] ?: throw AssertionError("no plural categories for $tag in REQUIRED_PLURAL_CATEGORIES")
        val errors = TranslationRules.check(res, language.qualifier, required)
        if (errors.isNotEmpty()) {
            fail(errors.joinToString(separator = "\n", prefix = "${errors.size} error(s) in the $tag translation:\n"))
        }
    }

    /** [path] in the app module: Gradle runs unit tests in the module directory; other runners may use the repository root. */
    private fun moduleFile(path: String): File {
        val workingDir = File(System.getProperty("user.dir")).absoluteFile
        val module = listOf(workingDir, File(workingDir, "app")).firstOrNull { File(it, "src/main/AndroidManifest.xml").isFile }
            ?: throw AssertionError("app module not found from ${workingDir.path}")
        return File(module, path)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages(): List<String> = Languages.all.map { it.tag }.filter { it != "en" }
    }
}

/**
 * The rules of tools/i18n/check_translations.py for one folder res/values-<qualifier> against res/values/strings*.xml:
 * - every English strings file with something to translate has a same-named file, and no other strings file exists;
 * - every translatable <string> and <plurals> of the English file is in it, with no extra key and no key English marks
 *   translatable="false" (nor any translatable="false" of its own);
 * - no empty value; the same format placeholders as English (%d, %s, %1$s…; a zero, one or two item may drop some);
 * - the same links (http/https URLs), verbatim;
 * - every plural category the language needs ([REQUIRED_PLURAL_CATEGORIES]);
 * - apostrophes escaped (\'), unless the whole value is quoted; no unescaped leading @ or ?.
 */
internal object TranslationRules {
    private val PLACEHOLDER = Regex("""%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([sdfxXcb%])""")
    private val URL = Regex("""https?://[^\s<>"']+""")
    private val RAW_STRING = Regex("""<string\s+name="([^"]+)"([^>]*)>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val RAW_PLURALS = Regex("""<plurals\s+name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
    private val RAW_ITEM = Regex("""<item\s+quantity="([^"]+)"\s*>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
    private val UNESCAPED_APOSTROPHE = Regex("""(?<!\\)'""")
    private val MAY_DROP_ARGUMENTS = setOf("zero", "one", "two")

    /** The errors of res/values-<[qualifier]>, whose plurals need the categories [required]; empty when it is complete. */
    fun check(res: File, qualifier: String, required: Set<String>): List<String> {
        val english = stringsFiles(File(res, "values")).associate { it.name to parse(it) }
        val untranslatable = english.values.flatMap { it.untranslatable }.toSet()
        val folder = File(res, "values-$qualifier")
        val errors = mutableListOf<String>()
        for ((name, source) in english) {
            if (source.strings.isEmpty() && source.plurals.isEmpty()) continue
            val file = File(folder, name)
            val label = "values-$qualifier/$name"
            if (!file.isFile) {
                errors += "$label: file missing"
                continue
            }
            val translation = try {
                parse(file)
            } catch (e: SAXException) {
                errors += "$label: malformed XML: ${e.message}"
                continue
            }
            val text = file.readText()
            val rawStrings = RAW_STRING.findAll(text).associate { it.groupValues[1] to it.groupValues[3] }
            val rawPlurals = RAW_PLURALS.findAll(text).associate { plural ->
                plural.groupValues[1] to RAW_ITEM.findAll(plural.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] }
            }
            for (key in (translation.strings.keys + translation.plurals.keys).sorted()) {
                if (key in untranslatable) errors += "$label/$key: English marks it translatable=\"false\" — remove it"
            }
            for (key in translation.untranslatable) {
                errors += "$label/$key: translatable=\"false\" in a translation — only values/ declares it"
            }
            for ((key, en) in source.strings) {
                val value = translation.strings[key]
                if (value == null) {
                    errors += "$label/$key: missing"
                    continue
                }
                if (value.isBlank()) errors += "$label/$key: empty"
                if (placeholders(value) != placeholders(en)) {
                    errors += "$label/$key: placeholders ${placeholders(value)} != English ${placeholders(en)}"
                }
                if (links(value).sorted() != links(en).sorted()) {
                    errors += "$label/$key: links ${links(value)} != English ${links(en)} (keep them verbatim)"
                }
                checkRaw("$label/$key", rawStrings[key].orEmpty(), errors)
            }
            for (key in translation.strings.keys) {
                if (key !in source.strings && key !in untranslatable) errors += "$label/$key: extra key (not in English)"
            }
            for ((key, enItems) in source.plurals) {
                val items = translation.plurals[key]
                if (items == null) {
                    errors += "$label/$key: plural missing"
                    continue
                }
                val missing = required - items.keys
                if (missing.isNotEmpty()) {
                    errors += "$label/$key: plural categories missing ${missing.sorted()} (need ${required.sorted()})"
                }
                val enOther = placeholders(enItems["other"].orEmpty())
                for ((quantity, value) in items) {
                    val found = placeholders(value)
                    if (found != enOther && !(quantity in MAY_DROP_ARGUMENTS && enOther.containsAll(found))) {
                        errors += "$label/$key[$quantity]: placeholders $found != English other $enOther"
                    }
                    if (value.isBlank()) errors += "$label/$key[$quantity]: empty"
                    checkRaw("$label/$key[$quantity]", rawPlurals[key]?.get(quantity).orEmpty(), errors)
                }
            }
            for (key in translation.plurals.keys) {
                if (key !in source.plurals && key !in untranslatable) errors += "$label/$key: extra plural (not in English)"
            }
        }
        for (file in stringsFiles(folder)) {
            if (file.name !in english) errors += "values-$qualifier/${file.name}: no values/${file.name} to translate"
        }
        return errors
    }

    private class StringsFile(
        val strings: Map<String, String>,
        val plurals: Map<String, Map<String, String>>,
        val untranslatable: Set<String>,
    )

    /** One placeholder: its argument position and conversion; printed as a positional placeholder. */
    private data class Placeholder(val position: Int, val conversion: Char) : Comparable<Placeholder> {
        override fun compareTo(other: Placeholder): Int = compareValuesBy(this, other, { it.position }, { it.conversion })
        override fun toString(): String = "%$position\$$conversion"
    }

    private fun stringsFiles(folder: File): List<File> =
        folder.listFiles { file -> file.isFile && file.name.startsWith("strings") && file.name.endsWith(".xml") }
            .orEmpty().sortedBy { it.name }

    /** The texts (as Android reads them: entities resolved, markup dropped) of the top-level strings and plurals. */
    private fun parse(file: File): StringsFile {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val strings = LinkedHashMap<String, String>()
        val plurals = LinkedHashMap<String, Map<String, String>>()
        val untranslatable = LinkedHashSet<String>()
        for (element in root.childElements()) {
            val name = element.getAttribute("name")
            if (element.getAttribute("translatable") == "false") {
                untranslatable += name
                continue
            }
            when (element.tagName) {
                "string" -> strings[name] = element.textContent
                "plurals" -> plurals[name] = element.childElements()
                    .filter { it.tagName == "item" }
                    .associate { it.getAttribute("quantity") to it.textContent }
            }
        }
        return StringsFile(strings, plurals, untranslatable)
    }

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

    /** The format placeholders of [text] ("%%" is a literal), a plain one numbered by its place among them, sorted. */
    private fun placeholders(text: String): List<Placeholder> {
        var index = 0
        return PLACEHOLDER.findAll(text).mapNotNull { match ->
            val conversion = match.groupValues[2].single()
            if (conversion == '%') return@mapNotNull null
            index += 1
            Placeholder(match.groupValues[1].toIntOrNull() ?: index, conversion)
        }.toList().sorted()
    }

    private fun links(text: String): List<String> = URL.findAll(text).map { it.value }.toList()

    /** [raw] is the value as written in the XML file. */
    private fun checkRaw(label: String, raw: String, errors: MutableList<String>) {
        if (UNESCAPED_APOSTROPHE.containsMatchIn(raw) && !(raw.startsWith("\"") && raw.endsWith("\""))) {
            errors += "$label: unescaped apostrophe (use \\')"
        }
        val stripped = raw.trim()
        if (stripped.startsWith("@") || stripped.startsWith("?")) errors += "$label: starts with @ or ? (escape it)"
    }
}

/** [TranslationRules] against small resource folders, and [REQUIRED_PLURAL_CATEGORIES] against the language list. */
class TranslationRulesTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun pluralCategories_coverEveryTranslation() {
        assertEquals(Languages.all.map { it.tag }.filter { it != "en" }.toSet(), REQUIRED_PLURAL_CATEGORIES.keys)
        for ((tag, categories) in REQUIRED_PLURAL_CATEGORIES) assertTrue(tag, "other" in categories)
    }

    @Test
    fun aCompleteTranslation_passes() {
        val res = res(
            "values-xx/strings.xml" to """
                <resources>
                    <string name="title">Pesan</string>
                    <string name="subtitle">Aplikasi \'kita\'</string>
                    <string name="greeting">%2${'$'}d baru untuk %1${'$'}s</string>
                    <string name="share">Unduh: https://example.com/app</string>
                    <plurals name="count">
                        <item quantity="one">satu pesan</item>
                        <item quantity="other">%d pesan</item>
                    </plurals>
                </resources>
            """,
            "values-xx/strings_home.xml" to """<resources><string name="home">Beranda</string></resources>""",
        )
        assertEquals(emptyList<String>(), TranslationRules.check(res, "xx", setOf("one", "other")))
    }

    @Test
    fun eachRule_reportsItsError() {
        val res = res(
            "values-xx/strings.xml" to """
                <resources>
                    <string name="app">OpenChat</string>
                    <string name="note" translatable="false">x</string>
                    <string name="title"> </string>
                    <string name="greeting">Hi %1${'$'}s, it's %2${'$'}s</string>
                    <string name="share">@string/title https://example.com</string>
                    <string name="extra">Extra</string>
                    <plurals name="count">
                        <item quantity="one">one message</item>
                        <item quantity="other">%s messages</item>
                    </plurals>
                    <plurals name="more">
                        <item quantity="other">more</item>
                    </plurals>
                </resources>
            """,
            "values-xx/strings_extra.xml" to """<resources/>""",
        )
        assertEquals(
            listOf(
                "values-xx/strings.xml/app: English marks it translatable=\"false\" — remove it",
                "values-xx/strings.xml/note: translatable=\"false\" in a translation — only values/ declares it",
                "values-xx/strings.xml/title: empty",
                "values-xx/strings.xml/subtitle: missing",
                "values-xx/strings.xml/greeting: placeholders [%1\$s, %2\$s] != English [%1\$s, %2\$d]",
                "values-xx/strings.xml/greeting: unescaped apostrophe (use \\')",
                "values-xx/strings.xml/share: links [https://example.com] != English [https://example.com/app] (keep them verbatim)",
                "values-xx/strings.xml/share: starts with @ or ? (escape it)",
                "values-xx/strings.xml/extra: extra key (not in English)",
                "values-xx/strings.xml/count: plural categories missing [few] (need [few, one, other])",
                "values-xx/strings.xml/count[other]: placeholders [%1\$s] != English other [%1\$d]",
                "values-xx/strings.xml/more: extra plural (not in English)",
                "values-xx/strings_home.xml: file missing",
                "values-xx/strings_extra.xml: no values/strings_extra.xml to translate",
            ),
            TranslationRules.check(res, "xx", setOf("one", "few", "other")),
        )
    }

    /** A res/ folder with the English strings below and [files] (path to content) beside them. */
    private fun res(vararg files: Pair<String, String>): File {
        val res = temporary.newFolder("res")
        val english = listOf(
            "values/strings.xml" to """
                <resources>
                    <string name="app" translatable="false">OpenChat</string>
                    <string name="title">Messages</string>
                    <string name="subtitle">Our \'app\'</string>
                    <string name="greeting">Hi %1${'$'}s, %2${'$'}d new</string>
                    <string name="share">Get it: https://example.com/app</string>
                    <plurals name="count">
                        <item quantity="one">%d message</item>
                        <item quantity="other">%d messages</item>
                    </plurals>
                </resources>
            """,
            "values/strings_home.xml" to """<resources><string name="home">Home</string></resources>""",
        )
        for ((path, content) in english + files) {
            File(res, path).apply { parentFile.mkdirs() }.writeText(content.trimIndent())
        }
        return res
    }
}
