package com.piptechnologies.openchat.core.phone

/** A country in the picker: ISO 3166-1 alpha-2 code, display name and calling code digits (no plus). */
data class DialCountry(
    val iso2: String,
    val name: String,
    val dialCode: String,
) {
    /** "+62" for display in chips and rows. */
    val dialLabel: String get() = "+$dialCode"

    /** Regional-indicator flag emoji for [iso2]; empty when the code is not two letters. */
    val flagEmoji: String
        get() {
            if (iso2.length != 2) return ""
            val a = iso2[0].uppercaseChar()
            val b = iso2[1].uppercaseChar()
            if (a !in 'A'..'Z' || b !in 'A'..'Z') return ""
            val first = 0x1F1E6 + (a - 'A')
            val second = 0x1F1E6 + (b - 'A')
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
}
