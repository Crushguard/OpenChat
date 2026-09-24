package com.piptechnologies.openchat.ui.settings

/**
 * One row of the Language screen (design map §4.17): the BCP-47 [tag] that is persisted and handed to
 * AppCompat, the [native] name shown large, the [english] name under it (and in the toast), and
 * whether the language is written right to left.
 */
data class LanguageOption(val tag: String, val native: String, val english: String, val rtl: Boolean)

/** The eight languages of the design, in its order. Only English strings ship (ruling R13); the rest fall back to English. */
object Languages {
    val all: List<LanguageOption> = listOf(
        LanguageOption(tag = "en", native = "English", english = "English", rtl = false),
        LanguageOption(tag = "id", native = "Bahasa Indonesia", english = "Indonesian", rtl = false),
        LanguageOption(tag = "pt-BR", native = "Português (Brasil)", english = "Portuguese", rtl = false),
        LanguageOption(tag = "ur", native = "اردو", english = "Urdu", rtl = true),
        LanguageOption(tag = "hi", native = "हिन्दी", english = "Hindi", rtl = false),
        LanguageOption(tag = "tr", native = "Türkçe", english = "Turkish", rtl = false),
        LanguageOption(tag = "es-MX", native = "Español (México)", english = "Spanish", rtl = false),
        LanguageOption(tag = "zu", native = "isiZulu", english = "Zulu", rtl = false),
    )

    /**
     * The option for [tag], ignoring case; failing that, the one with the same language subtag
     * ("pt" and "pt-PT" both give Português (Brasil)); failing that, English.
     */
    fun byTag(tag: String): LanguageOption {
        val wanted = tag.trim()
        all.firstOrNull { it.tag.equals(wanted, ignoreCase = true) }?.let { return it }
        val language = wanted.substringBefore('-').substringBefore('_')
        if (language.isNotEmpty()) {
            all.firstOrNull { it.tag.substringBefore('-').equals(language, ignoreCase = true) }?.let { return it }
        }
        return all.first()
    }
}
