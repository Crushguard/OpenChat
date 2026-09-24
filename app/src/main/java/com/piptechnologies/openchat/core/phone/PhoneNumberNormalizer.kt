package com.piptechnologies.openchat.core.phone

/** Result of normalising pasted/typed text: [country] is null when the current country should stay. */
data class NormalizedPaste(val country: DialCountry?, val nationalDigits: String)

/**
 * §5.1: turns pasted or typed phone text into digits the number field can use, resolving the
 * country from an international prefix when the text carries one.
 */
object PhoneNumberNormalizer {
    const val MAX_DIGITS = 15

    /**
     * Every decimal digit in [text], in order, as ASCII; all other characters (spaces, "+", punctuation) are
     * dropped. Digits of any script count, because phone keyboards in Persian, Arabic, Urdu, Hindi or Burmese
     * type their own digits: "۰۹۱۲" (Persian), "٠٩١٢" (Arabic-Indic), "०९१२" (Devanagari) all give "0912".
     */
    fun digitsOnly(text: String): String = buildString {
        for (c in text) if (c.isDigit()) append('0' + c.digitToInt())
    }

    /** [text] with every decimal digit of any script replaced by its ASCII digit; other characters kept. */
    fun asciiDigits(text: String): String = buildString {
        for (c in text) append(if (c.isDigit()) '0' + c.digitToInt() else c)
    }

    /**
     * §5.1: strips spaces, dashes, dots, parentheses, a leading "+" or "00"; "+"/"00" → longest
     * dial-code match sets the country and is stripped; one leading trunk "0" is dropped; capped
     * at [MAX_DIGITS].
     */
    fun normalizePaste(raw: String, current: DialCountry): NormalizedPaste {
        val t = asciiDigits(raw.trim())
        var international = t.startsWith("+") || t.startsWith("\uFF0B") // "+" or the full-width plus
        var d = digitsOnly(t)
        var country: DialCountry? = null
        if (!international && d.startsWith("00") && t.startsWith("00")) {
            d = d.drop(2)
            international = true
        }
        if (international) {
            DialCountries.longestPrefixMatch(d)?.let {
                country = it
                d = d.removePrefix(it.dialCode)
            }
        }
        d = stripTrunkZero(d).take(MAX_DIGITS)
        return NormalizedPaste(country, d)
    }

    /** Drops exactly one leading "0" when there is more than one digit. */
    fun stripTrunkZero(national: String): String =
        if (national.length > 1 && national.startsWith("0")) national.substring(1) else national

    /** "81234567890" → "812 3456 7890" (first 3, next 4, rest). */
    fun group(national: String): String {
        if (national.isEmpty()) return ""
        val first = national.take(3)
        if (national.length <= 3) return first
        val afterFirst = national.substring(3)
        val second = afterFirst.take(4)
        if (afterFirst.length <= 4) return "$first $second"
        val rest = afterFirst.substring(4)
        return "$first $second $rest"
    }

    /** dialCode + national digits, ready to send as the E.164 payload (no leading "+"). */
    fun e164Digits(dialCode: String, national: String): String = dialCode + national

    /** "+62 812 3456 7890" for display in the number field / recents. */
    fun displayInternational(dialCode: String, national: String): String = "+$dialCode ${group(national)}"
}
