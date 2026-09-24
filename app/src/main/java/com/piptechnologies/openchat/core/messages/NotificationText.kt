package com.piptechnologies.openchat.core.messages

import java.text.Normalizer

/**
 * Text rules for messaging-app notifications: system notifications, noise, deleted placeholders, message kinds,
 * conversation identity. The localized strings live in [WhatsAppStrings].
 */
object NotificationText {
    const val WHATSAPP = "com.whatsapp"
    const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    val watchedPackages: Set<String> = setOf(WHATSAPP, WHATSAPP_BUSINESS)

    private const val NO_ENTRY = "🚫"
    private const val EMOJI_PRESENTATION = "\uFE0F"

    /** What a name slot of a WhatsApp string matches: an admin's or a caller's name, 1–80 characters. */
    private const val NAME = "(.{1,80})"

    /** A number slot of a noise string: digits, the spaces around them optional ("12 条消息", "12条消息"). */
    private const val NUMBER = """ ?\p{Nd}+ ?"""

    /** Before a status line: symbols such as 📹 and ☎ (with their emoji presentation selector) and spaces. */
    private const val LEADING_SYMBOLS = """[\p{So}\p{Sk}\uFE0F ]*"""

    /** After a status line: anything but letters ("…", "...", " (45%)", a count). */
    private const val TRAILING_NON_LETTERS = """[^\p{L}\p{M}]*"""

    /** The bundle summary "12 messages from 2 chats" is short; a longer line is someone's text. */
    private const val SUMMARY_MAX_LENGTH = 80

    // The building blocks below are declared before the tables built with them (an object initializes in order).
    private val spaces = Regex("""[\s\p{Z}]+""")
    private val number = Regex("""\p{Nd}+""")

    /** A format slot of a WhatsApp string: `%s`, `%@`, `%d`, `%1$s`, `%2$d`, `%1$2d`. */
    private val slot = Regex("""%(?:\d+\$)?\d*[sd@]""")

    /** A number slot with the spaces around it, which [NUMBER] makes optional. */
    private val spacedNumberSlot = Regex(""" *(%(?:\d+\$)?\d*d) *""")

    /** Full stops a deleted placeholder may end with: Latin (also Hindi on iOS), Devanagari, Urdu, Chinese. */
    private val fullStops = setOf('.', '।', '۔', '。')

    private val noisePatterns = listOf(
        Regex("""(?i)\d+\s+(new\s+)?messages?\s+from\s+\d+\s+chats?"""),
        Regex("""(?i)^you have\s+\d+\s+(new\s+)?messages?"""),
        Regex("""(?i)^\d+\s+(new\s+)?messages?$"""),
        Regex("""(?i)checking for new messages"""),
        Regex("""(?i)you may have new messages"""),
        Regex("""(?i)tap to (view|open|see)"""),
        Regex("""(?i)backup"""),
        Regex("""(?i)restoring"""),
        Regex("""(?i)(incoming|ongoing|missed) (voice|video) call"""),
        Regex("""(?i)^calling"""),
    )
    private val appTitles = listOf("WhatsApp", "WhatsApp Business")
    private val appTitleNoise = Regex("""(?i)^(messages?|chats?|new messages?)$""")

    /**
     * `Notification.CATEGORY_*` values of notifications that are never chat lines, as plain strings so that this code
     * stays free of Android: call, missed call, progress, service, status, transport, system, alarm, error.
     */
    private val systemCategories =
        setOf("call", "missed_call", "progress", "service", "status", "transport", "sys", "alarm", "err")

    /** Every language's placeholders, as [deletedCore] reads them. */
    private val placeholderCores: List<String> =
        WhatsAppStrings.deletedPlaceholders.values.flatten().map(::deletedCore).distinct()

    /** Every language's admin variants, as anchored regexes (see [adminTemplate]). */
    private val adminTemplates: List<Regex> = WhatsAppStrings.deletedByAdmin.values.flatten().map(::adminTemplate)

    /** Every language's status lines: the whole line is one, give or take leading symbols and trailing non-letters. */
    private val statusNoise: List<Regex> = WhatsAppStrings.noise.values.flatMap { it.status }.distinct()
        .map { Regex(LEADING_SYMBOLS + noisePattern(it) + TRAILING_NON_LETTERS, RegexOption.IGNORE_CASE) }

    /** Every language's sentences that mark a line wherever they appear. */
    private val anywhereNoise: List<Regex> = WhatsAppStrings.noise.values.flatMap { it.anywhere }.distinct()
        .map { Regex(noisePattern(it), RegexOption.IGNORE_CASE) }

    /** Per language: a count next to its message word, and a count next to its chat word (see [isBundleSummary]). */
    private val summaryCounts: List<Pair<Regex, Regex>> = WhatsAppStrings.noise.values
        .filter { it.messageWords.isNotEmpty() && it.chatWords.isNotEmpty() }
        .map { countNextTo(it.messageWords) to countNextTo(it.chatWords) }

    /**
     * True for a notification that is never a chat line, in any language: a call, missed call, progress, service,
     * status, transport, system, alarm or error [category] (the `Notification.CATEGORY_*` values); an [ongoing] or
     * [foregroundService] one ("Checking for new messages", a call in progress); one that [showsProgress] (backup,
     * restore); one without MessagingStyle whose title is the app label ([titleIsAppLabel]: backup notices, "You may
     * have new messages" – a chat's notification carries the chat's name).
     */
    fun isSystemNotification(
        category: String?,
        ongoing: Boolean,
        foregroundService: Boolean,
        showsProgress: Boolean,
        fromMessagingStyle: Boolean,
        titleIsAppLabel: Boolean,
    ): Boolean =
        (category != null && category in systemCategories) || ongoing || foregroundService || showsProgress ||
            (!fromMessagingStyle && titleIsAppLabel)

    /** True when [title] is the app's own label, "WhatsApp" or "WhatsApp Business" (any case, bidi marks ignored). */
    fun isAppLabel(title: String): Boolean {
        val name = clean(title)
        return appTitles.any { it.equals(name, ignoreCase = true) }
    }

    /**
     * True for bundle summaries and status lines, in English ("12 messages from 2 chats", "You have 3 new messages",
     * "Checking for new messages", "You may have new messages", "N new messages", titles equal to "WhatsApp"/"WhatsApp
     * Business" with such text, "Tap to view", "Backup in progress", "Finished backup", "Restoring", "Calling…",
     * "Incoming voice call", "Ongoing voice call") and in every language of [WhatsAppStrings.noise]: its status lines,
     * its sentences, and the summary in the language's own order ("2 sohbetten 12 mesaj").
     * Meant for notifications whose title is the app label or that carry no MessagingStyle, so a contact's chat lines
     * are never filtered by it.
     */
    fun isSummaryOrNoise(title: String, text: String): Boolean {
        val body = clean(text)
        if (noisePatterns.any { it.containsMatchIn(body) }) return true
        val line = fold(text)
        if (statusNoise.any { it.matches(line) } || anywhereNoise.any { it.containsMatchIn(line) }) return true
        if (isBundleSummary(line)) return true
        return isAppLabel(title) && body.length < 40 && appTitleNoise.containsMatchIn(body)
    }

    /**
     * "This message was deleted" in every language of [WhatsAppStrings.deletedPlaceholders] (see docs/design-map.md
     * §5.2), and the admin variants of [WhatsAppStrings.deletedByAdmin] with any admin name. Exact after normalizing
     * both sides ([deletedCore]): invisible bidi marks and isolates, a leading 🚫, trailing full stops (".", "।", "۔",
     * "。") and spaces do not count, Arabic yeh/kaf equal the Persian/Urdu letters, case does not count.
     */
    fun isDeletedPattern(text: String): Boolean {
        val core = deletedCore(text)
        if (core.isEmpty()) return false
        return placeholderCores.any { it.equals(core, ignoreCase = true) } || adminTemplates.any { it.matches(core) }
    }

    /** Kind from the text prefix: 📷/"Photo" → PHOTO; 🎥/"Video" → VIDEO; 🎤/"Voice message" → VOICE; 🎵/"Audio" → AUDIO; 📄 or "Document" → DOCUMENT; "Sticker"/🧩 → STICKER; "GIF" → GIF; 👤/"Contact" → CONTACT; 📍/"Location"/"Live location" → LOCATION; deleted pattern → DELETED; else TEXT. Matching is on the trimmed text: exact word or emoji prefix, case-insensitive. */
    fun kindOf(text: String): MessageKind {
        val body = clean(text)
        fun hasPrefix(emoji: String) = body.startsWith(emoji)
        fun isWord(vararg words: String) = words.any { body.equals(it, ignoreCase = true) }
        return when {
            isDeletedPattern(body) -> MessageKind.DELETED
            hasPrefix("📷") || isWord("Photo") -> MessageKind.PHOTO
            hasPrefix("🎥") || isWord("Video") -> MessageKind.VIDEO
            hasPrefix("🎤") || isWord("Voice message") -> MessageKind.VOICE
            hasPrefix("🎵") || isWord("Audio") -> MessageKind.AUDIO
            hasPrefix("📄") || isWord("Document") -> MessageKind.DOCUMENT
            hasPrefix("🧩") || isWord("Sticker") -> MessageKind.STICKER
            isWord("GIF") -> MessageKind.GIF
            hasPrefix("👤") || isWord("Contact") -> MessageKind.CONTACT
            hasPrefix("📍") || isWord("Location", "Live location") -> MessageKind.LOCATION
            else -> MessageKind.TEXT
        }
    }

    fun conversationKey(appPackage: String, title: String): String = "$appPackage|${title.trim().lowercase()}"

    /** Digits when the title is a phone number (starts with "+" or is ≥ 7 characters of digits/spaces/dashes/parentheses), else null. */
    fun phoneNumberFrom(title: String): String? {
        val name = clean(title)
        val international = name.startsWith("+")
        val body = if (international) name.substring(1) else name
        val digits = body.filter { it.isDigit() }
        val onlyNumberChars = body.all { it.isDigit() || it.isWhitespace() || it == '(' || it == ')' || it.category == CharCategory.DASH_PUNCTUATION }
        if (digits.isEmpty() || !onlyNumberChars || !(international || name.length >= 7)) return null
        return digits.map { '0' + it.digitToInt() }.joinToString("")
    }

    /** Avatar initial: "#" when the title starts with "+" or a digit, else the first letter uppercased ("?" when empty). */
    fun initialFor(title: String): String {
        val name = clean(title)
        val first = name.firstOrNull() ?: return "?"
        if (first == '+' || first.isDigit()) return "#"
        return name.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "?"
    }

    fun textHash(text: String): Int = text.trim().hashCode()

    /** [text] without invisible format characters (the bidi marks and zero-width joiners notifications wrap names and numbers in), trimmed. */
    private fun clean(text: String): String = text.filterNot { it.category == CharCategory.FORMAT }.trim()

    /**
     * [text] [clean]ed and NFC-normalized, with Arabic yeh and kaf as the Persian/Urdu letters (ي U+064A → ی U+06CC,
     * ك U+0643 → ک U+06A9: keyboards and older strings mix them), the typographic apostrophe (U+2019) as "'", and each
     * run of spaces (no-break ones too) as one.
     */
    private fun fold(text: String): String =
        Normalizer.normalize(clean(text), Normalizer.Form.NFC)
            .replace('\u064A', '\u06CC').replace('\u0643', '\u06A9').replace('\u2019', '\'')
            .replace(spaces, " ")

    /**
     * [text] as the deleted-placeholder rules compare it: [fold]ed, without a leading 🚫 and without trailing full stops
     * and spaces.
     */
    private fun deletedCore(text: String): String =
        fold(text).removePrefix(NO_ENTRY).removePrefix(EMOJI_PRESENTATION).trim()
            .trimEnd { it in fullStops || it.isWhitespace() }

    /**
     * An admin variant as a regex over [deletedCore] text: the whole text, the template's words literal (any case) and
     * each name slot 1–80 characters, so a sentence that merely contains the words never matches.
     */
    private fun adminTemplate(template: String): Regex =
        Regex(deletedCore(template).split(slot).joinToString(NAME, "^", "\$") { literal(it) }, RegexOption.IGNORE_CASE)

    /**
     * Regex source for a noise string, over [fold]ed text: its words literal, without its trailing "…", "..." or full
     * stop; a number slot is digits ([NUMBER]), a name slot [NAME].
     */
    private fun noisePattern(template: String): String {
        val text = fold(template).trimEnd { it == '…' || it in fullStops || it.isWhitespace() }
            .replace(spacedNumberSlot, "\$1")
        val pattern = StringBuilder()
        var last = 0
        for (match in slot.findAll(text)) {
            pattern.append(literal(text.substring(last, match.range.first)))
            pattern.append(if (match.value.endsWith('d')) NUMBER else NAME)
            last = match.range.last + 1
        }
        return pattern.append(literal(text.substring(last))).toString()
    }

    /**
     * A number, then (after at most one short word, such as the Chinese measure words 条 and 个) one of [words]; or one of
     * [words], inflected, then a number ("رسالة 1", "دردشتين 2").
     */
    private fun countNextTo(words: List<String>): Regex {
        val word = words.joinToString("|", "(?:", ")") { literal(fold(it)) }
        return Regex("""\p{Nd}+ ?(?:\S{1,2} ?)?$word|$word\S* ?\p{Nd}+""", RegexOption.IGNORE_CASE)
    }

    /**
     * The bundle summary "12 messages from 2 chats" in any language of [WhatsAppStrings.noise]: a short line with two
     * numbers, one next to the language's message word and one next to its chat word, in either order ("2 sohbetten 12
     * mesaj", "12 رسالة من 2 دردشة").
     */
    private fun isBundleSummary(line: String): Boolean =
        line.length <= SUMMARY_MAX_LENGTH && number.findAll(line).take(2).count() == 2 &&
            summaryCounts.any { (messages, chats) -> messages.containsMatchIn(line) && chats.containsMatchIn(line) }

    private fun literal(text: String): String = if (text.isEmpty()) "" else Regex.escape(text)
}
