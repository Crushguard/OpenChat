package com.piptechnologies.openchat.core.send

enum class MessagingApp(val packageName: String, val label: String) {
    WHATSAPP("com.whatsapp", "WhatsApp"),
    WHATSAPP_BUSINESS("com.whatsapp.w4b", "WhatsApp Business"),
    TELEGRAM("org.telegram.messenger", "Telegram");
    val isWhatsAppFamily: Boolean get() = this != TELEGRAM
    companion object { fun fromName(name: String?): MessagingApp? = entries.firstOrNull { it.name == name } }
}
