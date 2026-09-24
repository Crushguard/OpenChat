package com.piptechnologies.openchat.e2e

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.piptechnologies.openchat.platform.StoragePermissions

/** Device state set up through the shell: notification access, runtime permissions, the app's language. */
object Grants {
    const val APP = "com.piptechnologies.openchat"
    private const val LISTENER = "$APP/$APP.service.WaNotificationListenerService"

    /**
     * Allows or revokes OpenChat's notification access and waits until the system has bound (or unbound) the
     * listener, so that a notification posted next reaches it. A bind that is slow to come (the system backs off
     * after the process was restarted) is kicked once by revoking and allowing again.
     */
    fun notificationListener(allowed: Boolean) {
        val verb = if (allowed) "allow_listener" else "disallow_listener"
        Shell.run("cmd notification $verb $LISTENER")
        val settled = runCatching {
            Waits.until("the notification listener to ${if (allowed) "connect" else "disconnect"}", timeoutMs = 20_000, pollMs = 500) {
                listenerLive() == allowed
            }
        }
        if (settled.isSuccess) return
        if (allowed) {
            Shell.run("cmd notification disallow_listener $LISTENER")
            Shell.run("cmd notification allow_listener $LISTENER")
        }
        try {
            Waits.until("the notification listener to ${if (allowed) "connect" else "disconnect"}", timeoutMs = 25_000, pollMs = 500) {
                listenerLive() == allowed
            }
        } catch (e: AssertionError) {
            throw AssertionError("${e.message}; dumpsys notification says:\n${liveListenersSection().joinToString("\n")}", e)
        }
    }

    /** Whether the listener is in the "Live notification listeners" section of `dumpsys notification`. */
    private fun listenerLive(): Boolean = liveListenersSection().drop(1).any { it.contains("WaNotificationListenerService") }

    private fun liveListenersSection(): List<String> = liveListenersSection(Shell.run("dumpsys notification"))

    /** The "Live notification listeners (N):" header of [dump] and its entries (the lines indented below it). */
    internal fun liveListenersSection(dump: String): List<String> {
        val lines = dump.lines()
        val header = lines.indexOfFirst { it.trimStart().startsWith("Live notification listeners") }
        if (header < 0) return listOf("<no \"Live notification listeners\" section>")
        val indent = lines[header].indentation()
        return listOf(lines[header]) + lines.drop(header + 1).takeWhile { it.isNotBlank() && it.indentation() > indent }
    }

    private fun String.indentation(): Int = length - trimStart().length

    /** The media permissions the app asks for on this API level (never revoked: that would kill the process). */
    fun mediaPermissions() {
        StoragePermissions.required().forEach { Shell.run("pm grant $APP $it") }
    }

    /** POST_NOTIFICATIONS for [pkg] on API 33+, so no permission dialog interrupts a flow. */
    fun postNotifications(pkg: String) {
        if (Build.VERSION.SDK_INT >= 33) Shell.run("pm grant $pkg android.permission.POST_NOTIFICATIONS")
    }

    /**
     * Starts every test in English. On API 33+ the per-app language lives in the system and can outlive a data clear,
     * so it is set there; below, AppCompat holds it, set on the main thread before the activity starts.
     */
    fun forceEnglish() {
        if (Build.VERSION.SDK_INT >= 33) {
            Shell.run("cmd locale set-app-locales $APP --locales en")
        } else {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            }
        }
    }
}
