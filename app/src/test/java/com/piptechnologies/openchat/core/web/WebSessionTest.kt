package com.piptechnologies.openchat.core.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSessionTest {
    @Test fun `parses quoted probe results`() {
        assertEquals(ProbeResult.LINKED, WebSession.parseProbe("\"linked\""))
        assertEquals(ProbeResult.QR, WebSession.parseProbe("qr"))
        assertEquals(ProbeResult.UNKNOWN, WebSession.parseProbe(null)); assertEquals(ProbeResult.UNKNOWN, WebSession.parseProbe("null"))
    }
    @Test fun `desktop user agent looks like chrome on windows`() {
        assertTrue(WebSession.DESKTOP_USER_AGENT.contains("Windows NT")); assertTrue(WebSession.DESKTOP_USER_AGENT.contains("Chrome/"))
    }
}
