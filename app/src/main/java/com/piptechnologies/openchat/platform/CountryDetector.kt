package com.piptechnologies.openchat.platform

import android.content.Context
import android.telephony.TelephonyManager
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.DialCountry
import java.util.Locale

enum class CountrySource { SIM, NETWORK, LOCALE }

data class DetectedCountry(val country: DialCountry, val source: CountrySource)

/** Default country of the number field: SIM → network → locale → US (§5.5). */
object CountryDetector {
    fun detect(context: Context): DetectedCountry {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        DialCountries.byIso2(readIso { telephony?.simCountryIso })?.let { return DetectedCountry(it, CountrySource.SIM) }
        DialCountries.byIso2(readIso { telephony?.networkCountryIso })?.let { return DetectedCountry(it, CountrySource.NETWORK) }
        DialCountries.byIso2(Locale.getDefault().country)?.let { return DetectedCountry(it, CountrySource.LOCALE) }
        return DetectedCountry(DialCountries.byIso2("US")!!, CountrySource.LOCALE)
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
