package com.piptechnologies.openchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The language the UI is shown in: the first locale of the current configuration (which follows the
 * per-app language on every API level, unlike the application context on API 24–32), or the JVM
 * default when that list is empty or unnamed. Never null. Reading it recomposes the caller on a
 * language change.
 */
@Composable
@ReadOnlyComposable
fun currentUiLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    val first = if (locales.isEmpty) null else locales[0]
    return first?.takeIf { it.language.isNotEmpty() } ?: Locale.getDefault()
}
