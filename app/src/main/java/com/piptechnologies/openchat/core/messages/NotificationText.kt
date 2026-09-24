package com.piptechnologies.openchat.core.messages

import java.text.Normalizer

/** Text rules for messaging-app notifications: noise, deleted placeholders, message kinds, conversation identity. */
object NotificationText {
    const val WHATSAPP = "com.whatsapp"
    const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    val watchedPackages: Set<String> = setOf(WHATSAPP, WHATSAPP_BUSINESS)

    private const val NO_ENTRY = "🚫"
    private const val EMOJI_PRESENTATION = "\uFE0F"

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

    private val deletedPatterns = listOf(
        "This message was deleted",
        "Pesan ini telah dihapus",
        "Essa mensagem foi apagada",
        "Esta mensagem foi apagada",
        "Se eliminó este mensaje",
        "Bu mesaj silindi",
        "यह मैसेज हटा दिया गया",
        "یہ پیغام حذف کر دیا گیا",
        "This message was deleted by admin",
    ).map { deletedCore(it) }

    /** True for bundle summaries and status lines: "12 messages from 2 chats", "You have 3 new messages", "Checking for new messages", "You may have new messages", "N new messages", titles equal to "WhatsApp"/"WhatsApp Business" with such text, "Tap to view", "Backup in progress", "Finished backup", "Restoring", "Calling…", "Incoming voice call", "Ongoing voice call". Meant for notifications whose title is the app label or that carry no MessagingStyle, so a contact's chat lines are never filtered by it. */
    fun isSummaryOrNoise(title: String, text: String): Boolean {
        val body = clean(text)
        if (noisePatterns.any { it.containsMatchIn(body) }) return true
        val appTitle = clean(title)
        val fromApp = appTitles.any { it.equals(appTitle, ignoreCase = true) }
        return fromApp && body.length < 40 && appTitleNoise.containsMatchIn(body)
    }

    /** "This message was deleted" and localized variants (see docs/design-map.md §5.2), with or without a leading 🚫 and trailing period, case-insensitive. */
    fun isDeletedPattern(text: String): Boolean {
        val core = deletedCore(text)
        return deletedPatterns.any { it.equals(core, ignoreCase = true) }
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

    /** [text] cleaned and NFC-normalized, without a leading 🚫 or trailing full stops (Latin, Devanagari and Urdu). */
    private fun deletedCore(text: String): String =
        Normalizer.normalize(clean(text), Normalizer.Form.NFC)
            .removePrefix(NO_ENTRY).removePrefix(EMOJI_PRESENTATION).trim()
            .trimEnd('.', '।', '۔').trimEnd()
}
