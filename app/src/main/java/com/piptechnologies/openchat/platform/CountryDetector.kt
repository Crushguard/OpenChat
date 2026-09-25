package com.piptechnologies.openchat.platform

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.telephony.TelephonyManager
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.DialCountry

enum class CountrySource { SIM, NETWORK, LOCALE }

data class DetectedCountry(val country: DialCountry, val source: CountrySource)

/** Default country of the number field: SIM → network → locale → US (§5.5). */
object CountryDetector {
    fun detect(context: Context): DetectedCountry {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        DialCountries.byIso2(readIso { telephony?.simCountryIso })?.let { return DetectedCountry(it, CountrySource.SIM) }
        DialCountries.byIso2(readIso { telephony?.networkCountryIso })?.let { return DetectedCountry(it, CountrySource.NETWORK) }
        phoneLocales(context).let { locales ->
            (0 until locales.size()).firstNotNullOfOrNull { DialCountries.byIso2(locales[it].country) }
        }?.let { return DetectedCountry(it, CountrySource.LOCALE) }
        return DetectedCountry(DialCountries.byIso2("US")!!, CountrySource.LOCALE)
    }

    /**
     * The phone's language and region setting. Not Locale.getDefault(): once a language is picked in the app, the
     * default follows it (the per-app locale on Android 13+, AppCompat below), and "fr" or "ar" carry no region.
     */
    private fun phoneLocales(context: Context): LocaleList =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.systemLocales
                ?: Resources.getSystem().configuration.locales
        } else {
            Resources.getSystem().configuration.locales
        }

    /** Devices without telephony may throw here (API 35 feature enforcement); that counts as unknown. */
    private fun readIso(read: () -> String?): String? =
        try {
            read()?.trim()?.takeIf { it.length == 2 }
        } catch (e: UnsupportedOperationException) {
            null
        } catch (e: SecurityException) {
            null
        }
}
