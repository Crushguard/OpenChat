package com.piptechnologies.openchat.e2e

import android.os.Build

/**
 * Device screenshots: `screencap` to /sdcard/Download/openchat-e2e/api<sdk>/<folder>/<name>.png, which the device
 * workflow pulls into its artifact. Folders: a language tag for the language sweep, "flows" for the tool flows,
 * "failures" for the screen at the moment a test failed.
 */
object Capture {
    private val root = "/sdcard/Download/openchat-e2e/api${Build.VERSION.SDK_INT}"

    fun screen(folder: String, name: String) {
        val dir = "$root/${safe(folder)}"
        Shell.run("mkdir -p $dir")
        Shell.run("screencap -p $dir/${safe(name)}.png")
    }

    private fun safe(part: String): String = part.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
