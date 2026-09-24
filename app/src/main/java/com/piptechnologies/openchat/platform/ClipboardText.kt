package com.piptechnologies.openchat.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Source of the Paste pill on Home (ruling R14). */
object ClipboardText {
    /** Item 0 of the primary clip coerced to text and trimmed; null when there is no clip or it is blank. */
    fun read(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip: ClipData? = try {
            clipboard.primaryClip
        } catch (e: SecurityException) {
            null
        }
        if (clip == null || clip.itemCount == 0) return null
        val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
        return text?.takeIf { it.isNotEmpty() }
    }
}
