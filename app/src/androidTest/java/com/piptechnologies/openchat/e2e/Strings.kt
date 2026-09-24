package com.piptechnologies.openchat.e2e

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R
import java.lang.reflect.Modifier
import java.util.Locale

/**
 * UI text as the app resolves it, never hardcoded English: [current] reads through the running activity (so it
 * follows the per-app language the way the screens do), [of] resolves for an explicit locale.
 */
object Strings {
    val ENGLISH: Locale = Locale.ENGLISH

    private val contexts = HashMap<String, Context>()

    fun current(@StringRes id: Int, vararg args: Any): String {
        val activity = Activities.resumedMain()
        var text = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            text = if (args.isEmpty()) activity.getString(id) else activity.getString(id, *args)
        }
        return text
    }

    fun currentPlural(@PluralsRes id: Int, count: Int, vararg args: Any): String {
        val activity = Activities.resumedMain()
        var text = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val formatArgs: Array<out Any> = if (args.isEmpty()) arrayOf(count) else args
            text = activity.resources.getQuantityString(id, count, *formatArgs)
        }
        return text
    }

    fun of(locale: Locale, @StringRes id: Int, vararg args: Any): String {
        val context = contextFor(locale)
        return if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
    }

    /**
     * True when the APK carries this app's own strings for [locale] (a values-<qualifier> folder): at least one of
     * the app's string resources resolves differently than in English. The app's R.string lists only the app's own
     * strings (android.nonTransitiveRClass=true), so library translations (AppCompat, Material) do not count.
     */
    fun hasTranslations(locale: Locale): Boolean {
        val localized = contextFor(locale)
        val english = contextFor(ENGLISH)
        return appStringIds().any { id -> localized.getString(id) != english.getString(id) }
    }

    private fun appStringIds(): List<Int> =
        R.string::class.java.fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
            .map { it.getInt(null) }

    private fun contextFor(locale: Locale): Context = synchronized(contexts) {
        contexts.getOrPut(locale.toLanguageTag()) {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocale(locale)
            base.createConfigurationContext(configuration)
        }
    }
}

/** The activity in front, as the test runner's lifecycle monitor sees it. */
object Activities {
    fun resumed(): Activity? {
        var found: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return found
    }

    fun resumedMain(timeoutMs: Long = 15_000): MainActivity {
        var main: MainActivity? = null
        Waits.until("OpenChat's activity to be in front", timeoutMs, pollMs = 100) {
            main = resumed() as? MainActivity
            main != null
        }
        return main!!
    }
}
