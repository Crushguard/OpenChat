package com.piptechnologies.openchat.core.web

/** Outcome of probing the WhatsApp Web page for its linked/QR state. */
enum class ProbeResult { LINKED, QR, UNKNOWN }

/** Constants and pure logic for the second-account WebView session (design map §5.4). */
object WebSession {
    const val URL = "https://web.whatsapp.com/"
    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    const val PROBE_INTERVAL_MS = 3_000L

    /** Evaluates to "linked", "qr" or "unknown". */
    const val LINKED_PROBE_JS = "(function(){try{if(document.querySelector('#pane-side')||document.querySelector('[data-testid=\"chat-list\"]'))return 'linked';if(document.querySelector('canvas[aria-label]')||document.querySelector('[data-ref]')||document.querySelector('[data-testid=\"qrcode\"]'))return 'qr';}catch(e){}return 'unknown';})()"

    /** WebView returns JSON-quoted strings ("\"linked\""); null/"null" → UNKNOWN. */
    fun parseProbe(result: String?): ProbeResult {
        if (result == null) return ProbeResult.UNKNOWN
        return when (result.trim().removeSurrounding("\"")) {
            "linked" -> ProbeResult.LINKED
            "qr" -> ProbeResult.QR
            else -> ProbeResult.UNKNOWN
        }
    }
}
