package com.piptechnologies.openchat.core.send

import java.net.URLEncoder

/** [uri] opens the chat inside the app (set the package on the intent); [webUri] is the browser fallback. */
data class SendLink(val app: MessagingApp, val uri: String, val webUri: String)

object SendLinkBuilder {
    /** URL-encodes with UTF-8 and "%20" for spaces (java.net.URLEncoder then "+" → "%20"). */
    fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")

    /** WhatsApp / Business: https://wa.me/<digits>[?text=…] (webUri identical). Telegram: tg://resolve?phone=<digits>[&text=…], webUri https://t.me/+<digits>. Blank message → no text parameter. */
    fun build(app: MessagingApp, e164Digits: String, message: String): SendLink {
        val text = if (message.isBlank()) null else encode(message)
        return when (app) {
            MessagingApp.WHATSAPP, MessagingApp.WHATSAPP_BUSINESS -> {
                val uri = if (text == null) "https://wa.me/$e164Digits" else "https://wa.me/$e164Digits?text=$text"
                SendLink(app, uri, uri)
            }
            MessagingApp.TELEGRAM -> {
                val uri = if (text == null) "tg://resolve?phone=$e164Digits" else "tg://resolve?phone=$e164Digits&text=$text"
                SendLink(app, uri, "https://t.me/+$e164Digits")
            }
        }
    }
}
