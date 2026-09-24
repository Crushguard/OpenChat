package com.piptechnologies.openchat.core.send

object SendOutcomeHeuristic {
    const val NOT_ON_WHATSAPP_WINDOW_MS = 7_000L

    /** Ruling R6: a WhatsApp-family launch followed by a resume within the window suggests WhatsApp bounced the number. */
    fun suspectsNotOnWhatsApp(app: MessagingApp, launchedAtMs: Long, resumedAtMs: Long): Boolean {
        if (!app.isWhatsAppFamily) return false
        val elapsed = resumedAtMs - launchedAtMs
        return elapsed in 0 until NOT_ON_WHATSAPP_WINDOW_MS
    }
}
