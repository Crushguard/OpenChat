package com.piptechnologies.openchat.e2e

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Shell commands run as the shell user through UiAutomation. No shell parses the line: it is split on whitespace,
 * so arguments must not contain spaces (the stand-in's extras are URL-encoded for that reason). Only standard output
 * comes back, read to the end.
 */
object Shell {
    fun run(command: String): String {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes().toString(Charsets.UTF_8) }
    }
}

/** Bounded polling for conditions outside Compose (files, system state); Compose conditions use the rule's waitUntil. */
object Waits {
    fun until(what: String, timeoutMs: Long = 20_000, pollMs: Long = 250, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!condition()) {
            if (SystemClock.uptimeMillis() >= deadline) throw AssertionError("Timed out after $timeoutMs ms waiting for $what")
            Thread.sleep(pollMs)
        }
    }
}
