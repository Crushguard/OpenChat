package com.piptechnologies.openchat.core.messages

/**
 * What WhatsApp and WhatsApp Business for Android write in their notifications, in each of the app's 19 languages
 * (docs/design-map.md §5.2 has the coverage table). Sources: WhatsApp Android string resources (2017 and 2021),
 * WhatsApp iOS strings, real Android chat exports, WhatsApp Help Center. The strings are WhatsApp's own, without the
 * bidi marks its right-to-left and plural strings start with ([NotificationText] drops those before comparing);
 * `%s` and `%@` stand for a name, `%d` for a number.
 *
 * WhatsApp for Android has no Pashto ("ps") or Burmese ("my") UI: a phone set to either shows the English strings.
 */
internal object WhatsAppStrings {
    /**
     * The line WhatsApp shows in place of a message its sender deleted for everyone: every wording seen in the field
     * (the WhatsApp versions in use vary a lot), newest first. Notifications show it without the 🚫 of the chat bubble
     * (a drawable there).
     */
    val deletedPlaceholders: Map<String, List<String>> = linkedMapOf(
        "en" to listOf(
            "This message was deleted", // Android 2017, 2021 and current (high)
            // The app's earlier entry, kept: WhatsApp appends the admin's name (see deletedByAdmin), but as an exact
            // whole line it is harmless.
            "This message was deleted by admin",
        ),
        "id" to listOf(
            "Pesan ini dihapus", // current: Android exports 2022–2026, iOS (medium)
            "Pesan ini telah dihapus", // Android 2017, 2021 (high)
        ),
        "pt-BR" to listOf(
            "Mensagem apagada", // current: Android exports 2023–2026, iOS (medium)
            "Essa mensagem foi apagada", // Android 2021 (high)
            "Esta mensagem foi apagada", // Android 2017 (high)
        ),
        "pt" to listOf(
            "Esta mensagem foi apagada", // Android 2021, iOS (high)
            "Esta mensagem foi apagada pelo remetente.", // Android 2017 (high)
        ),
        "ur" to listOf(
            "یہ میسج حذف کر دیا گیا ہے۔", // iOS wording, likely current Android (low–medium)
            "یہ پیغام حذف کیا گیا", // Android 2017, 2021 (high)
            "یہ پیغام حذف کر دیا گیا", // in no WhatsApp source; kept: a harmless exact phrase
        ),
        "hi" to listOf(
            "यह मैसेज डिलीट कर दिया गया है.", // iOS wording, likely current Android (medium–low)
            "यह मैसेज मिटाया गया", // Android 2021 (high)
            "यह संदेश मिटाया गया", // Android 2017 (high)
            "यह मैसेज हटा दिया गया", // in no WhatsApp source; kept: a harmless exact phrase
        ),
        "tr" to listOf("Bu mesaj silindi"), // Android 2017, 2021, iOS (high)
        "es" to listOf(
            "Se eliminó este mensaje.", // current: Android exports 2023–2026, iOS (medium)
            "Este mensaje fue eliminado", // Android 2017–2021 (high)
        ),
        "ar" to listOf("تم حذف هذه الرسالة"), // Android 2017, 2021, iOS (high)
        "fa" to listOf(
            "این پیام حذف شده است.", // iOS wording, possibly current Android (low–medium)
            "این پیام حذف شد", // Android 2017, 2021 (high)
        ),
        "ps" to emptyList(), // no Pashto UI in WhatsApp for Android: English
        "he" to listOf("הודעה זו נמחקה"), // Android 2017, 2021, exports 2025, iOS (high)
        "fr" to listOf("Ce message a été supprimé."), // Android 2017, 2021, exports 2021/2026 (high)
        "de" to listOf("Diese Nachricht wurde gelöscht."), // Android 2017, 2021, iOS (high)
        "it" to listOf("Questo messaggio è stato eliminato"), // Android 2017, 2021, export 2022 (high)
        "ru" to listOf("Данное сообщение удалено"), // Android 2017, 2021, export 2022, iOS (high)
        "zh" to listOf(
            "这条消息已被删除。", // iOS wording, possibly current Android (low–medium)
            "消息已删除", // Android 2021 (high)
            "信息已删除", // Android 2017 (high)
        ),
        "ha" to listOf(
            // UNVERIFIED (low confidence): WhatsApp's Hausa strings (Hausa UI since 2022) could not be read. A guess,
            // kept because it is a full, specific sentence; older WhatsApp builds show English.
            "An goge wannan saƙon",
        ),
        "my" to emptyList(), // no Burmese UI in WhatsApp for Android: English
    )

    /** The same line when a group admin deleted the message; `%s`/`%@` is the admin's name, which can sit mid-sentence. */
    val deletedByAdmin: Map<String, List<String>> = linkedMapOf(
        "en" to listOf( // Android current (high)
            "This message was deleted by an admin",
            "This message was deleted by admin %s",
        ),
        "id" to listOf( // iOS (low–medium)
            "Pesan ini dihapus oleh admin.",
            "Pesan ini dihapus oleh admin %@.",
        ),
        "pt-BR" to listOf( // iOS (low–medium)
            "Mensagem apagada por um admin",
            "Mensagem apagada por um admin (%@)",
        ),
        "pt" to listOf( // iOS; Help Center (low–medium)
            "Esta mensagem foi apagada por um/a administrador/a.",
            "Esta mensagem foi apagada por um/a administrador/a, %@.",
            "Esta mensagem foi apagada pelo/a administrador/a %s",
        ),
        "ur" to listOf( // iOS (low–medium)
            "یہ میسج ایک ایڈمن نے حذف کر دیا ہے۔",
            "یہ میسج ایڈمن %@ نے حذف کر دیا ہے۔",
        ),
        "hi" to listOf( // iOS (low–medium)
            "यह मैसेज किसी एडमिन ने डिलीट कर दिया है.",
            "यह मैसेज एडमिन %@ ने डिलीट कर दिया है.",
        ),
        "tr" to listOf( // iOS (low–medium)
            "Bu mesaj bir yönetici tarafından silindi.",
            "Bu mesaj %@ adlı yönetici tarafından silindi.",
        ),
        "es" to listOf( // iOS, Help Center (medium)
            "Un admin. eliminó este mensaje.",
            "Un admin., %@, eliminó este mensaje.",
        ),
        "ar" to listOf( // iOS (low–medium)
            "حذف أحد المشرفين هذه الرسالة.",
            "حذف المشرف %@ هذه الرسالة.",
        ),
        "fa" to listOf( // iOS; Help Center (medium)
            "یکی از مدیران این پیام را حذف کرد.",
            "این پیام را مدیر (%@) حذف کرد.",
            "این پیام را مدیر %s حذف کرد.",
        ),
        "he" to listOf( // iOS (low–medium)
            "ההודעה נמחקה על ידי מנהל/ת.",
            "ההודעה נמחקה על ידי %@, מנהל/ת הקבוצה.",
        ),
        "fr" to listOf( // iOS (low–medium)
            "Ce message a été supprimé par un ou une admin.",
            "Ce message a été supprimé par l'admin %@",
        ),
        "de" to listOf( // iOS, Help Center (medium)
            "Diese Nachricht wurde von einem*einer Admin gelöscht.",
            "Diese Nachricht wurde von Admin %@ gelöscht.",
        ),
        "it" to listOf( // iOS (low–medium)
            "Questo messaggio è stato eliminato da un amministratore.",
            "Questo messaggio è stato eliminato dall'amministratore %@.",
        ),
        "ru" to listOf( // iOS (low–medium)
            "Данное сообщение удалено админом.",
            "Данное сообщение удалено админом (%@).",
        ),
        "zh" to listOf( // iOS, Help Center in substance (low–medium)
            "管理员已删除这条消息。",
            "管理员 %@ 已删除这条消息。",
        ),
    )

    /**
     * Status and summary lines, never chat lines: English as WhatsApp writes it today (beyond what
     * [NotificationText]'s English rules already catch), the other languages in their 2021 wording.
     */
    val noise: Map<String, Noise> = linkedMapOf(
        "en" to Noise(
            status = listOf(
                "Checking for new calls", "Backing up messages", "Tap for more info",
                "Group voice call", "Group video call", "Ringing", "Missed call from %s", "%d missed calls from %s",
                "WhatsApp Web is currently active", "WhatsApp Companion is currently active",
            ),
        ),
        "id" to Noise(
            status = listOf(
                "Memeriksa pesan baru",
                "%d pesan baru", "%d pesan",
                "Pencadangan sedang berlangsung", "Mencadangkan pesan", "Menyiapkan cadangan…", "Selesai mencadangkan",
                "Memulihkan pesan…",
                "Panggilan suara masuk", "Panggilan video masuk", "Panggilan suara grup...", "Panggilan video grup...",
                "Panggilan suara berlangsung", "Panggilan video berlangsung",
                "Panggilan suara tak terjawab", "Panggilan video tak terjawab", "%d panggilan tak terjawab",
                "Memanggil…", "Berdering…",
                "WhatsApp Web saat ini sedang aktif",
            ),
            anywhere = listOf("Anda mungkin telah menerima pesan baru"),
            messageWords = listOf("pesan"),
            chatWords = listOf("chat"),
        ),
        "pt-BR" to Noise(
            status = listOf(
                "Procurando novas mensagens",
                "%d nova mensagem", "%d novas mensagens", "%d mensagem", "%d mensagens",
                "Backup em andamento", "Fazendo backup das mensagens", "Preparando o backup…", "Backup concluído",
                "Restaurando mensagens…",
                "Chamada de voz recebida", "Chamada de vídeo recebida", "Chamada de voz em grupo…",
                "Chamada de vídeo em grupo…",
                "Chamada de voz ativa", "Chamada de vídeo ativa",
                "Chamada de voz perdida", "Chamada de vídeo perdida", "%d chamada perdida", "%d chamadas perdidas",
                "Ligando…", "Chamando…",
                "WhatsApp Web está ativo no momento",
            ),
            anywhere = listOf("Pode ser que você tenha novas mensagens"),
            messageWords = listOf("mensage"),
            chatWords = listOf("conversa"),
        ),
        "pt" to Noise(
            status = listOf(
                "A procurar novas mensagens", "Pode ter mensagens novas",
                "%d mensagem nova", "%d mensagens novas", "%d mensagem", "%d mensagens",
                "Cópia de segurança em curso", "A efetuar cópia…", "A preparar cópia de segurança…",
                "Cópia de segurança concluída", "A restaurar mensagens…",
                "Recebida (voz)", "A receber videochamada", "Chamada de voz em grupo", "Videochamada em grupo...",
                "Chamada de voz em curso", "Videochamada em curso",
                "Chamada de voz não atendida", "Videochamada não atendida", "%d chamada não atendida",
                "%d chamadas não atendidas",
                "A ligar…",
                "De momento, o WhatsApp Web está ativo",
            ),
            messageWords = listOf("mensage"),
            chatWords = listOf("conversa"),
        ),
        "ur" to Noise(
            status = listOf(
                "نئے پیغامات کے لیے پڑتال جاری ہے",
                "%d نیا پیغام", "%d نئے پیغامات", "%d پیغام", "%d پیغامات",
                "بیک اپ جاری ہے", "پیغامات بیک اپ کئے جارہے ہیں", "بیک اپ تیار ہو رہا ہے…", "بیک اپ مکمل ہو گیا",
                "پیغامات بحال ہورہے ہیں…",
                "آنے والی وائس کال", "آنے والی ویڈیو کال", "گروپ وائس کال…", "گروپ ویڈیو کال…",
                "وائس کال جاری ہے", "جاری ویڈیو کال",
                "مسڈ وائس کال", "مسڈ ویڈیو کال", "%d مسڈ کال", "%d مسڈ کالز",
                "کال جا رہی ہے…", "گھنٹی بج رہی ہے…",
                "ابھی WhatsApp ویب سرگرم ہے",
            ),
            anywhere = listOf("آپ کے لیے نئے پیغامات ہو سکتے ہیں"),
            messageWords = listOf("پیغام"),
            chatWords = listOf("چیٹ"),
        ),
        "hi" to Noise(
            status = listOf(
                "नए मैसेज की जाँच की जा रही है",
                "%d नया मैसेज", "%d नए मैसेज", "%d मैसेज",
                "बैकअप लिया जा रहा है", "मैसेज का बैकअप जारी", "बैकअप तैयार हो रहा है…", "बैकअप समाप्त",
                "मैसेज रीस्टोर हो रहे हैं…",
                "इनकमिंग वॉइस कॉल", "इनकमिंग वीडियो कॉल", "ग्रुप वॉइस कॉल…", "ग्रुप वीडियो कॉल...",
                "वॉइस कॉल चल रही है", "वीडियो कॉल चल रही है",
                "वॉइस कॉल मिस हुई", "वीडियो कॉल मिस हुई", "%d मिस्ड कॉल", "%d मिस्ड कॉल्स",
                "कॉल की जा रही है…", "घंटी बज रही है…",
                "WhatsApp वेब अभी एक्टिव है",
            ),
            anywhere = listOf("हो सकता है कि आपको नए मैसेज मिले हों"),
            messageWords = listOf("मैसेज"),
            chatWords = listOf("चैट"),
        ),
        "tr" to Noise(
            status = listOf(
                "Bağlantınız kontrol ediliyor", "Yeni mesajınız olabilir",
                "%d yeni mesaj", "%d mesaj",
                "Yedekleme işleniyor", "Mesajlar yedekleniyor", "Yedekleme hazırlanıyor…", "Yedekleme bitti",
                "Mesajlar geri yükleniyor…",
                "Gelen sesli arama", "Gelen görüntülü arama", "Sesli grup araması...", "Görüntülü grup araması...",
                "Devam eden sesli arama", "Devam eden görüntülü arama",
                "Cevapsız sesli arama", "Cevapsız görüntülü arama", "%d cevapsız arama",
                "Aranıyor…", "Çalıyor…",
                "WhatsApp Web şu an aktif",
            ),
            messageWords = listOf("mesaj"),
            chatWords = listOf("sohbet"),
        ),
        "es" to Noise(
            status = listOf(
                "Comprobando si hay mensajes nuevos",
                "%d mensaje nuevo", "%d mensajes nuevos", "%d mensaje", "%d mensajes",
                "Copia de seguridad en curso", "Creando copia de seguridad de mensajes",
                "Preparando copia de seguridad…", "Copia finalizada", "Restaurando mensajes…",
                "Llamada entrante", "Videollamada entrante", "Llamada grupal…", "Videollamada grupal…",
                "Llamada en curso", "Videollamada en curso",
                "Llamada perdida", "Videollamada perdida", "%d llamada perdida", "%d llamadas perdidas",
                "Llamando…",
                "WhatsApp Web está actualmente activo",
            ),
            anywhere = listOf("Puede que tengas nuevos mensajes"),
            messageWords = listOf("mensaje"),
            chatWords = listOf("chat"),
        ),
        "ar" to Noise(
            status = listOf(
                "جارٍ التحقق من رسائل جديدة",
                "%d من الرسائل الجديدة", "رسالة جديدة %d", "رسالتان %d جديدتان", "%d رسائل جديدة", "%d رسالة جديدة",
                "رسالة %d", "رسالتان %d", "%d رسائل", "%d رسالة",
                "جاري النسخ الاحتياطي", "جارٍ حفظ نسخة احتياطية للرسائل", "جارٍ التحضير للنسخ الاحتياطي…",
                "انتهى النسخ الاحتياطي", "جارٍ استعادة الرسائل…",
                "مكالمة صوتية واردة", "مكالمة فيديو واردة", "مكالمة صوتية جماعية...", "مكالمة فيديو جماعية...",
                "مكالمة صوتية جارية", "مكالمة فيديو جارية",
                "مكالمة صوتية فائتة", "مكالمة فيديو فائتة", "مكالمة فائتة %d", "مكالمتان فائتتان %d",
                "%d مكالمات فائتة", "%d مكالمة فائتة",
                "جارِ الاتصال…", "يرنّ",
                "واتساب ويب ناشط حالياً",
            ),
            anywhere = listOf("قد يكون لديك رسائل جديدة"),
            messageWords = listOf("رسال", "رسائل"),
            chatWords = listOf("دردش"),
        ),
        "fa" to Noise(
            status = listOf(
                "جستجو برای پیام جدید", "پیام جدید دارید",
                "%d پیام جدید", "%d پيام",
                "در حال گرفتن نسخه پشتیبان", "درحال پشتیبان گیری از پیامها", "در حال آماده سازی نسخه پشتیبان…",
                "گرفتن نسخه پشتیبان اتمام یافت", "در حال بازیابی پیام ها …",
                "تماس صوتی ورودی", "تماس تصویری ورودی", "تماس صوتی گروهی...", "تماس تصویری گروهی...",
                "در حال تماس صوتی", "در حال تماس تصویری",
                "تماس صوتی پاسخ داده نشده", "تماس تصویری پاسخ داده نشده", "%d تماس از دست رفته",
                "در حال تماس…", "بوق آزاد...",
                "واتساپ وب در حال حاضر فعال است",
            ),
            messageWords = listOf("پیام"),
            chatWords = listOf("گفتگو"),
        ),
        "he" to Noise(
            status = listOf(
                "בודק אחר הודעות חדשות",
                "הודעה %d חדשה", "%d הודעות חדשות", "הודעה %d", "%d הודעות",
                "הגיבוי מתבצע", "מגבה הודעות", "מכין גיבוי…", "הגיבוי הסתיים", "משחזר הודעות…",
                "שיחה קולית נכנסת", "שיחת וידאו נכנסת", "שיחה קולית קבוצתית...", "שיחת וידאו קבוצתית...",
                "שיחה קולית פעילה", "שיחת וידאו פעילה",
                "שיחה קולית שלא נענתה", "שיחת וידאו שלא נענתה", "%d שיחה שלא נענתה", "%d שיחות שלא נענו",
                "מתקשר…", "מצלצל…",
                "WhatsApp Web פעיל כרגע",
            ),
            anywhere = listOf("ייתכן שיש לך הודעות חדשות"),
            messageWords = listOf("הודע"),
            chatWords = listOf("צ'אט", "צ׳אט"),
        ),
        "fr" to Noise(
            status = listOf(
                "Recherche de nouveaux messages",
                "%d nouveau message", "%d nouveaux messages", "%d message", "%d messages",
                "Sauvegarde en cours", "Sauvegarde des messages en cours", "En préparation de la sauvegarde…",
                "Sauvegarde terminée", "Restauration des messages…",
                "Appel vocal entrant", "Appel vidéo entrant", "Appel vocal de groupe...", "Appel vidéo de groupe...",
                "Appel vocal en cours", "Appel vidéo en cours",
                "Appel vocal manqué", "Appel vidéo manqué", "%d appel manqué", "%d appels manqués",
                "Appel en cours…",
                "WhatsApp Web est actuellement actif",
            ),
            anywhere = listOf("Vous pourriez avoir de nouveaux messages"),
            messageWords = listOf("message"),
            chatWords = listOf("discussion"),
        ),
        "de" to Noise(
            status = listOf(
                "Überprüfe auf neue Nachrichten",
                "%d neue Nachricht", "%d neue Nachrichten", "%d Nachricht", "%d Nachrichten",
                "Backup wird erstellt", "Nachrichten werden gesichert", "Backup wird vorbereitet\u00A0…",
                "Backup abgeschlossen", "Nachrichten werden wiederhergestellt\u00A0…",
                "Eingehender Sprachanruf", "Eingehender Videoanruf", "Gruppen-Sprachanruf\u00A0…",
                "Gruppen-Videoanruf\u00A0…",
                "Laufender Sprachanruf", "Laufender Videoanruf",
                "Verpasster Sprachanruf", "Verpasster Videoanruf", "%d verpasster Anruf", "%d verpasste Anrufe",
                "Anrufen\u00A0…", "Klingelt\u00A0…",
                "WhatsApp Web ist gerade aktiv.",
            ),
            anywhere = listOf("Du könntest neue Nachrichten haben."),
            messageWords = listOf("Nachricht"),
            chatWords = listOf("Chat"),
        ),
        "it" to Noise(
            status = listOf(
                "Controllo nuovi messaggi in corso", "Potresti avere nuovi messaggi",
                "%d nuovo messaggio", "%d nuovi messaggi", "%d messaggio", "%d messaggi",
                "Backup in corso", "Backup dei messaggi in corso", "Preparazione del backup…", "Backup terminato",
                "Ripristino dei messaggi…",
                "Chiamata vocale in arrivo", "Videochiamata in arrivo", "Chiamata vocale di gruppo...",
                "Videochiamata di gruppo...",
                "Chiamata vocale in corso", "Videochiamata in corso",
                "Chiamata vocale persa", "Videochiamata persa", "%d chiamata persa", "%d chiamate perse",
                "Chiamata in corso", "Sta squillando…",
                "WhatsApp Web attualmente attivo",
            ),
            messageWords = listOf("messagg"),
            chatWords = listOf("chat"),
        ),
        "ru" to Noise(
            status = listOf(
                "Поиск новых сообщений",
                "%d новое сообщение", "%d новых сообщения", "%d новых сообщений", "%d сообщение", "%d сообщения",
                "%d сообщений",
                "Выполняется резервное копирование", "Резервное копирование сообщений", "Подготовка…",
                "Резервное копирование завершено", "Восстановление сообщений…",
                "Входящий аудиозвонок", "Входящий видеозвонок", "Групповой аудиозвонок…", "Групповой видеозвонок…",
                "Текущий аудиозвонок", "Текущий видеозвонок",
                "Пропущенный аудиозвонок", "Пропущенный видеозвонок", "%d пропущенный звонок", "%d пропущенных звонка",
                "%d пропущенных звонков",
                "Звонок…", "Соединение…",
                "Текущая сессия WhatsApp Web",
            ),
            anywhere = listOf("Возможно, у вас есть новые сообщения"),
            messageWords = listOf("сообщени"),
            chatWords = listOf("чат"),
        ),
        "zh" to Noise(
            status = listOf(
                "检查新消息", "您可能有新消息",
                "%d 条新消息", "%d 条消息",
                "备份正在进行中", "备份对话记录中", "正在准备备份…", "备份完成", "正在还原消息 ...",
                "语音通话来电", "视频通话来电", "群组语音通话…", "群组视频通话…",
                "正在进行语音通话", "拨出视频通话",
                "未接语音通话", "未接视频通话", "%d 个未接电话",
                "正在呼叫…", "响铃中…",
                "WhatsApp 网页版当前使用中",
            ),
            messageWords = listOf("消息"),
            chatWords = listOf("对话"),
        ),
    )

    /**
     * One language's notification noise. [status]: a line that is exactly one of these, give or take a leading symbol
     * (📹, ☎) and trailing punctuation, "…" or a count in brackets; `%d` stands for a number, `%s` for a name.
     * [anywhere]: sentences long enough to mark a line wherever they appear. [messageWords], [chatWords]: the bundle
     * summary "12 messages from 2 chats" is a line with two numbers, one next to a message word and one next to a chat
     * word, in whichever order the language puts them.
     */
    class Noise(
        val status: List<String>,
        val anywhere: List<String> = emptyList(),
        val messageWords: List<String> = emptyList(),
        val chatWords: List<String> = emptyList(),
    )
}
