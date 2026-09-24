package com.piptechnologies.openchat.ui.components

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * This context in the per-app language: when the user picked one (AppCompatDelegate.getApplicationLocales()
 * is not empty), a configuration context whose locales are that choice; otherwise `this`, which already
 * follows the device language.
 *
 * Only for text that leaves the app in an outbound intent, such as the share text resolved with the
 * application context, which on API 24–32 does not carry AppCompat's per-app language. Text shown in
 * the UI goes through [UiText] and is resolved by composables instead.
 */
fun Context.withAppLocale(): Context {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    if (appLocales.isEmpty()) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(appLocales.toLanguageTags()))
    return createConfigurationContext(configuration)
}
