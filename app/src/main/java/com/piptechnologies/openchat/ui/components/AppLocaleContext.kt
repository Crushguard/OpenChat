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
 * For text resolved outside an activity, which on API 24–32 does not carry AppCompat's per-app
 * language: the share text in an outbound intent, and notification text built in a service (the
 * second-account foreground notification). Text shown in the UI goes through [UiText] and is
 * resolved by composables instead.
 */
fun Context.withAppLocale(): Context {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    if (appLocales.isEmpty()) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(appLocales.toLanguageTags()))
    return createConfigurationContext(configuration)
}
