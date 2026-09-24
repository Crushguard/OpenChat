package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [NotificationText.isSummaryOrNoise] in every language of [WhatsAppStrings.noise]: WhatsApp's status lines as it posts
 * them (bidi marks, no-break spaces, "…", 📹/☎), its counts (Latin, Arabic-Indic and Persian digits), the bundle
 * summary in each language's own order, and chat lines that merely share words with them.
 */
class LocalizedNoiseTest {
    private val lines = mapOf(
        "en" to listOf(
            "Checking for new calls", "Backing up messages (45%)", "Ringing…", "Ringing …", "☎ Missed call from Ayu",
            "☎ 2 missed calls from Ayu", "📹 Group video call…", "WhatsApp Web is currently active", "Tap for more info",
        ),
        "id" to listOf(
            "Memeriksa pesan baru", "Anda mungkin telah menerima pesan baru", "\u200E3 pesan baru", "Menyiapkan cadangan…",
            "Panggilan suara tak terjawab", "\u200E2 panggilan tak terjawab", "Berdering…",
        ),
        "pt-BR" to listOf(
            "Procurando novas mensagens", "Pode ser que você tenha novas mensagens", "\u200E1 nova mensagem",
            "Fazendo backup das mensagens", "Chamada de vídeo perdida", "Ligando…",
        ),
        "pt" to listOf(
            "A procurar novas mensagens", "Pode ter mensagens novas", "\u200E3 mensagens novas", "A preparar cópia de segurança…",
            "Recebida (voz)", "Videochamada não atendida", "A ligar…",
        ),
        "ur" to listOf(
            "\u200Fنئے پیغامات کے لیے پڑتال جاری ہے", "\u200Fآپ کے لیے نئے پیغامات ہو سکتے ہیں", "\u200F3 نئے پیغامات",
            "\u200Fبیک اپ تیار ہو رہا ہے…", "\u200Fمسڈ وائس کال", "\u200Fگھنٹی بج رہی ہے…",
        ),
        "hi" to listOf(
            "नए मैसेज की जाँच की जा रही है", "हो सकता है कि आपको नए मैसेज मिले हों", "\u200E3 नए मैसेज", "बैकअप तैयार हो रहा है…",
            "वॉइस कॉल मिस हुई", "\u200E2 मिस्ड कॉल्स", "घंटी बज रही है…",
        ),
        "tr" to listOf(
            "Bağlantınız kontrol ediliyor", "Yeni mesajınız olabilir", "\u200E3 yeni mesaj", "Yedekleme hazırlanıyor…",
            "Cevapsız görüntülü arama", "Çalıyor…",
        ),
        "es" to listOf(
            "Comprobando si hay mensajes nuevos", "Puede que tengas nuevos mensajes", "\u200E3 mensajes nuevos",
            "Preparando copia de seguridad…", "Llamada perdida", "\u200E2 llamadas perdidas", "Llamando…",
        ),
        "ar" to listOf(
            "\u200Fجارٍ التحقق من رسائل جديدة", "\u200Fقد يكون لديك رسائل جديدة", "\u200Fرسالة جديدة 1",
            "\u200F\u0663 رسائل جديدة", "\u200Fجارٍ التحضير للنسخ الاحتياطي…", "\u200Fمكالمة فيديو فائتة", "\u200Fيرنّ",
        ),
        "fa" to listOf(
            "\u200Fجستجو برای پیام جدید", "\u200Fپیام جدید دارید", "\u200F\u06F3 پیام جدید",
            "\u200Fدر حال آماده سازی نسخه پشتیبان…", "\u200Fتماس صوتی پاسخ داده نشده", "\u200Fبوق آزاد...",
        ),
        "he" to listOf(
            "\u200Fבודק אחר הודעות חדשות", "\u200Fייתכן שיש לך הודעות חדשות", "\u200Fהודעה 1 חדשה", "\u200Fמכין גיבוי…",
            "📹 שיחת וידאו נכנסת", "\u200Fמצלצל…",
        ),
        "fr" to listOf(
            "Recherche de nouveaux messages", "Vous pourriez avoir de nouveaux messages", "\u200E3 nouveaux messages",
            "En préparation de la sauvegarde…", "Appel vocal manqué", "\u200E2 appels manqués", "Appel en cours…",
        ),
        "de" to listOf(
            "Überprüfe auf neue Nachrichten", "Du könntest neue Nachrichten haben.", "\u200E3 neue Nachrichten",
            "Backup wird vorbereitet\u00A0…", "Verpasster Videoanruf", "Klingelt\u00A0…",
        ),
        "it" to listOf(
            "Controllo nuovi messaggi in corso", "Potresti avere nuovi messaggi", "\u200E3 nuovi messaggi", "Preparazione del backup…",
            "Videochiamata persa", "Sta squillando…",
        ),
        "ru" to listOf(
            "Поиск новых сообщений", "Возможно, у вас есть новые сообщения", "\u200E3 новых сообщения", "Подготовка…",
            "Пропущенный видеозвонок", "\u200E5 пропущенных звонков", "Соединение…",
        ),
        "zh" to listOf(
            "检查新消息", "您可能有新消息", "\u200E3 条新消息", "\u200E3条新消息", "正在准备备份…", "未接视频通话",
            "\u200E2 个未接电话", "响铃中…",
        ),
    )

    /** "12 messages from 2 chats" as each language composes it (2021 wording), with the bidi marks of its plural strings. */
    private val summaries = mapOf(
        "id" to listOf("\u200E\u200E12 pesan dari 2 chat"),
        "pt" to listOf("\u200E\u200E12 mensagens de 2 conversas"),
        "ur" to listOf("\u200F2 چیٹس سے \u200F12 پیغامات"),
        "hi" to listOf("\u200E2 चैट्स में \u200E12 मैसेज आए हैं"),
        "tr" to listOf("\u200E2 sohbetten \u200E12 mesaj"),
        "es" to listOf("\u200E\u200E12 mensajes de 2 chats"),
        "ar" to listOf("\u200F\u200F12 رسالة من 2 دردشة", "\u200F\u200F12 رسالة من دردشتين 2", "\u200F\u0661\u0662 رسالة من \u0663 دردشات"),
        "fa" to listOf("\u200F\u200F12 پیام در 2 گفتگو", "\u200F\u06F1\u06F2 پ\u064Aام در \u06F2 گفتگو"),
        "he" to listOf("\u200F\u200F12 הודעות מתוך 2 צ'אטים", "12 הודעות מתוך 2 צ׳אטים"),
        "fr" to listOf("\u200E\u200E12 messages de 2 discussions"),
        "de" to listOf("\u200E\u200E12 Nachrichten aus 2 Chats"),
        "it" to listOf("\u200E\u200E12 messaggi da 2 chat"),
        "ru" to listOf("\u200E\u200E12 сообщений из 2 чатов", "\u200E\u200E21 сообщение из 1 чата"),
        "zh" to listOf("来自 2 个对话的 \u200E12 条消息 条消息", "来自 2 个对话的 12 条消息"),
    )

    @Test fun `status lines in every language`() {
        lines.forEach { (language, texts) ->
            texts.forEach {
                assertTrue("$language: $it", NotificationText.isSummaryOrNoise("Ayu", it))
                assertTrue("$language: $it", NotificationText.isSummaryOrNoise("WhatsApp", it))
            }
        }
    }

    @Test fun `bundle summary in each language's order and reversed`() {
        summaries.forEach { (language, texts) ->
            texts.forEach { assertTrue("$language: $it", NotificationText.isSummaryOrNoise("WhatsApp", it)) }
        }
        listOf("De 2 chats, 12 mensajes", "Aus 2 Chats: 12 Nachrichten", "12 mesaj, 2 sohbetten", "12 条消息，来自 2 个对话")
            .forEach { assertTrue(it, NotificationText.isSummaryOrNoise("WhatsApp", it)) }
    }

    @Test fun `every listed noise string is noise`() {
        WhatsAppStrings.noise.forEach { (language, noise) ->
            (noise.status + noise.anywhere).forEach {
                val line = it.replace("%d", "12").replace("%s", "Ayu")
                assertTrue("$language: $line", NotificationText.isSummaryOrNoise("Ayu", line))
            }
            noise.anywhere.forEach { assertTrue("$language: $it", NotificationText.isSummaryOrNoise("Ayu", "Ayu - $it")) }
        }
    }

    @Test fun `chat lines that share words with noise are kept`() {
        listOf(
            "Te mandé 2 mensajes a las 10 por el chat", "Ich habe 2 Nachrichten in unserem Chat um 10 Uhr geschickt",
            "Я отправил 2 сообщения в чат в 10 часов", "我给你发了2条消息，在3个群", "Llamada perdida de mamá, llámala",
            "Звонок завтра в 5", "Подготовка к экзамену", "Appel en cours de préparation", "Chiamata in corso? Ti richiamo",
            "正在呼叫你", "Pesan baru dari Budi", "Mensajes nuevos para ti", "2 mesaj attım, bakar mısın?",
            "Ringing the bell at 5", "Tap for more info about the trip",
        ).forEach { assertFalse(it, NotificationText.isSummaryOrNoise("Ayu", it)) }
    }

    @Test fun `deleted placeholders are never noise`() {
        val placeholders = WhatsAppStrings.deletedPlaceholders.values.flatten() +
            WhatsAppStrings.deletedByAdmin.values.flatten().map { it.replace("%s", "Ayu").replace("%@", "Ayu") }
        placeholders.forEach {
            assertFalse(it, NotificationText.isSummaryOrNoise("Ayu", it))
            assertFalse(it, NotificationText.isSummaryOrNoise("WhatsApp", it))
        }
    }
}
