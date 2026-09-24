package com.piptechnologies.openchat.ui.components

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * User-visible text that is resolved where it is shown, not where it is decided. ViewModels expose
 * UiText (in state and toasts) instead of strings resolved with the application context: on API 24–32
 * the AppCompat per-app language does not reach the application context, and a resolved string kept
 * in state would stay in the old language after a language switch. Composables resolve it with
 * [asString]; code that has only a Context uses [asString] with that Context.
 *
 * Format arguments are plain values (String, Int, Long, …) or other UiText, which resolve first, in
 * the same language. Keep them immutable: equal UiText lets Compose skip recomposition.
 */
@Immutable
sealed interface UiText {
    /** A string resource, formatted with [args] when there are any. */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** A plurals resource for [count]; [args] are the format arguments, the count itself by default ("%d items"). */
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    /** Text that is not translated, such as a contact name or a phone number. */
    data class Raw(val value: String) : UiText
}

/** [UiText.Res] for [id] with the format [args]. */
fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

/** [UiText.Plural] for [id] and [count]; with no [args] the count is the only format argument. */
fun pluralText(@PluralsRes id: Int, count: Int, vararg args: Any): UiText =
    UiText.Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

/** This text in the current UI language; recomposes when the configuration (and so the language) changes. */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String {
    // Reading LocalConfiguration subscribes the caller to configuration changes, as stringResource does.
    LocalConfiguration.current
    return resolve(LocalContext.current.resources)
}

/** This text in [context]'s language (an Activity carries the per-app language; see withAppLocale for others). */
fun UiText.asString(context: Context): String = resolve(context.resources)

private fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Raw -> value
    // No arguments: the raw string, as stringResource(id) does, so a literal "%" needs no escaping.
    is UiText.Res -> if (args.isEmpty()) resources.getString(id) else resources.getString(id, *resolveArgs(args, resources))
    is UiText.Plural ->
        if (args.isEmpty()) {
            resources.getQuantityString(id, count)
        } else {
            resources.getQuantityString(id, count, *resolveArgs(args, resources))
        }
}

private fun resolveArgs(args: List<Any>, resources: Resources): Array<Any> =
    Array(args.size) { index ->
        when (val arg = args[index]) {
            is UiText -> arg.resolve(resources)
            else -> arg
        }
    }
