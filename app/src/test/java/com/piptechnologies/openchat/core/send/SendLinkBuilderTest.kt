package com.piptechnologies.openchat.core.send

import org.junit.Assert.assertEquals
import org.junit.Test

class SendLinkBuilderTest {
    @Test fun `whatsapp link with message`() {
        val l = SendLinkBuilder.build(MessagingApp.WHATSAPP, "6281234567890", "Hi! Is the blue one still available?")
        assertEquals("https://wa.me/6281234567890?text=Hi%21%20Is%20the%20blue%20one%20still%20available%3F", l.uri)
        assertEquals(l.uri, l.webUri)
    }
    @Test fun `business link without message has no text parameter`() {
        assertEquals("https://wa.me/6281234567890", SendLinkBuilder.build(MessagingApp.WHATSAPP_BUSINESS, "6281234567890", "   ").uri)
    }
    @Test fun `telegram link and web fallback`() {
        val l = SendLinkBuilder.build(MessagingApp.TELEGRAM, "6281234567890", "Hello")
        assertEquals("tg://resolve?phone=6281234567890&text=Hello", l.uri)
        assertEquals("https://t.me/+6281234567890", l.webUri)
    }
    @Test fun `fromName round trips and rejects unknown`() {
        assertEquals(MessagingApp.TELEGRAM, MessagingApp.fromName("TELEGRAM")); assertEquals(null, MessagingApp.fromName("x"))
    }
}
