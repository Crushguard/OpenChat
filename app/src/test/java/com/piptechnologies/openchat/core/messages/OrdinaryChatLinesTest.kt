package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Regression: ordinary chat lines in each of the app's languages, some close to WhatsApp's own strings, are kept as
 * text: never a deleted placeholder, never noise (even where the noise rules apply: plain-text notifications and
 * notifications titled with the app label).
 */
class OrdinaryChatLinesTest {
    private val chatLines = mapOf(
        "en" to listOf("OK", "This message was deleted by mistake, sorry", "Was this message deleted?", "I deleted this message"),
        "id" to listOf("Oke", "Pesan ini dihapus ya?", "Kenapa pesan ini dihapus oleh admin grup?", "Nanti aku telepon"),
        "pt-BR" to listOf("Beleza", "Mensagem apagada sem querer, foi mal", "Essa mensagem foi apagada por quê?", "Te ligo mais tarde"),
        "pt" to listOf("Está bem", "Esta mensagem é para ti", "Esta mensagem foi apagada?", "Ligo-te logo"),
        "ur" to listOf("ٹھیک ہے", "کیا یہ پیغام حذف کیا گیا ہے؟", "میں بعد میں کال کروں گا"),
        "hi" to listOf("ठीक है", "क्या यह मैसेज मिटाया गया था?", "मैं बाद में कॉल करूँगा"),
        "tr" to listOf("Tamam", "Bu mesaj silindi mi?", "Seni sonra ararım"),
        "es" to listOf("Vale", "¿Se eliminó este mensaje?", "Este mensaje fue eliminado por error", "Te llamo luego"),
        "ar" to listOf("حسنا", "هل تم حذف هذه الرسالة؟", "سأتصل بك لاحقا"),
        "fa" to listOf("باشه", "این پیام حذف شد؟", "بعدا بهت زنگ میزنم"),
        "ps" to listOf("سمه ده", "مننه", "ستړي مه شې"),
        "he" to listOf("בסדר", "למה הודעה זו נמחקה?", "אתקשר אליך אחר כך"),
        "fr" to listOf("D'accord", "Ce message est pour toi", "Ce message a été supprimé par erreur", "Je t'appelle plus tard"),
        "de" to listOf("Alles klar", "Diese Nachricht wurde gelöscht, oder?", "Ich rufe dich später an"),
        "it" to listOf("Va bene", "Questo messaggio è stato eliminato per sbaglio", "Ti chiamo dopo"),
        "ru" to listOf("Ок", "Данное сообщение удалено?", "Данное сообщение удалено? Я его не вижу", "Это сообщение удалено", "Перезвоню позже"),
        "zh" to listOf("好的", "这条消息已被删除了吗？", "消息已删除了吗", "我晚点给你打电话"),
        "ha" to listOf("To", "An goge wannan saƙon ne?", "Sannu"),
        "my" to listOf("ဟုတ်ကဲ့", "ကျေးဇူးတင်ပါတယ်"),
    )

    @Test fun `ordinary chat lines are kept as text in every language`() {
        assertEquals(19, chatLines.size)
        chatLines.forEach { (language, lines) ->
            (lines + "OK").forEach {
                assertFalse("$language: $it", NotificationText.isDeletedPattern(it))
                assertEquals("$language: $it", MessageKind.TEXT, NotificationText.kindOf(it))
                assertFalse("$language: $it", NotificationText.isSummaryOrNoise("Ayu", it))
                assertFalse("$language: $it", NotificationText.isSummaryOrNoise("WhatsApp", it))
            }
        }
    }
}
