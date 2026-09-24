package com.piptechnologies.openchat.core.send

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendOutcomeHeuristicTest {
    @Test fun `quick return from whatsapp is suspicious`() {
        assertTrue(SendOutcomeHeuristic.suspectsNotOnWhatsApp(MessagingApp.WHATSAPP, 1_000, 4_000))
    }
    @Test fun `slow return or telegram is not`() {
        assertFalse(SendOutcomeHeuristic.suspectsNotOnWhatsApp(MessagingApp.WHATSAPP, 1_000, 9_000))
        assertFalse(SendOutcomeHeuristic.suspectsNotOnWhatsApp(MessagingApp.TELEGRAM, 1_000, 2_000))
        assertFalse(SendOutcomeHeuristic.suspectsNotOnWhatsApp(MessagingApp.WHATSAPP, 5_000, 4_000))
    }
}
