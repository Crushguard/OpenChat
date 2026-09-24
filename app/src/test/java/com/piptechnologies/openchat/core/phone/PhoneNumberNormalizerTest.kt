package com.piptechnologies.openchat.core.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {
    private val id = DialCountries.byIso2("ID")!!
    private val us = DialCountries.byIso2("US")!!

    @Test fun `paste with plus sets country and strips code`() {
        val r = PhoneNumberNormalizer.normalizePaste("+62 812-3456-7890", us)
        assertEquals("ID", r.country?.iso2); assertEquals("81234567890", r.nationalDigits)
    }
    @Test fun `paste with 00 prefix behaves like plus`() {
        val r = PhoneNumberNormalizer.normalizePaste("0062 812 3456 7890", us)
        assertEquals("ID", r.country?.iso2); assertEquals("81234567890", r.nationalDigits)
    }
    @Test fun `leading trunk zero is dropped and country kept`() {
        val r = PhoneNumberNormalizer.normalizePaste("0812 3456 7890", id)
        assertNull(r.country); assertEquals("81234567890", r.nationalDigits)
    }
    @Test fun `parentheses and dashes are stripped`() {
        assertEquals("2025550123", PhoneNumberNormalizer.normalizePaste("(202) 555-0123", us).nationalDigits)
    }
    @Test fun `uk paste drops the bracketed trunk zero`() {
        val r = PhoneNumberNormalizer.normalizePaste("+44 (0)20 7946 0958", id)
        assertEquals("GB", r.country?.iso2); assertEquals("2079460958", r.nationalDigits)
    }
    @Test fun `nanp territory resolves to the longest prefix`() {
        val r = PhoneNumberNormalizer.normalizePaste("+1 684 555 0123", id)
        assertEquals("AS", r.country?.iso2); assertEquals("5550123", r.nationalDigits)
    }
    @Test fun `capped at fifteen digits`() {
        assertEquals(15, PhoneNumberNormalizer.normalizePaste("1234567890123456789", id).nationalDigits.length)
    }
    @Test fun `grouping is three four rest`() {
        assertEquals("812 3456 7890", PhoneNumberNormalizer.group("81234567890"))
        assertEquals("812", PhoneNumberNormalizer.group("812"))
        assertEquals("812 3456", PhoneNumberNormalizer.group("8123456"))
        assertEquals("", PhoneNumberNormalizer.group(""))
    }
    @Test fun `display and e164`() {
        assertEquals("6281234567890", PhoneNumberNormalizer.e164Digits("62", "81234567890"))
        assertEquals("+62 812 3456 7890", PhoneNumberNormalizer.displayInternational("62", "81234567890"))
    }
}
