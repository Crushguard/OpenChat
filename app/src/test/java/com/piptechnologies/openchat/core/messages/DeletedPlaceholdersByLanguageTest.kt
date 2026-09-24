package com.piptechnologies.openchat.core.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WhatsApp's "This message was deleted" in each of the app's 19 languages (docs/design-map.md §5.2): every wording,
 * as a notification may carry it (bidi marks, isolates, a trailing full stop, a 🚫), the admin variants with a sample
 * name, and sentences that merely contain the words.
 */
class DeletedPlaceholdersByLanguageTest {
    /** [placeholder] (without its full stop) is detected bare, with bidi marks or isolates, with [stop], with a 🚫. */
    private fun assertPlaceholder(placeholder: String, stop: String) {
        listOf(
            placeholder, "\u200F$placeholder", "\u200E$placeholder\u200F", "\u2068$placeholder\u2069", "$placeholder$stop",
            "\u200F$placeholder$stop", "🚫 $placeholder", "🚫\uFE0F $placeholder$stop", "  $placeholder  ",
        ).forEach {
            assertTrue(it, NotificationText.isDeletedPattern(it))
            assertEquals(it, MessageKind.DELETED, NotificationText.kindOf(it))
        }
    }

    /** Each admin line is detected as it is, after a bidi mark, and with a trailing full stop. */
    private fun assertAdmin(vararg lines: String) = lines.forEach { line ->
        listOf(line, "\u200F$line", "\u200E$line.").forEach { assertTrue(it, NotificationText.isDeletedPattern(it)) }
    }

    private fun assertNotDeleted(vararg lines: String) = lines.forEach {
        assertFalse(it, NotificationText.isDeletedPattern(it))
        assertEquals(it, MessageKind.TEXT, NotificationText.kindOf(it))
    }

    @Test fun `english`() {
        assertPlaceholder("This message was deleted", ".")
        assertPlaceholder("This message was deleted by admin", ".") // the app's earlier exact entry, kept as a whole line
        assertAdmin(
            "This message was deleted by an admin", "This message was deleted by admin Zeeshan",
            "This message was deleted by admin Zeeshan.", "This message was deleted by admin \u2068+62 813-9922-0417\u2069",
            "THIS MESSAGE WAS DELETED BY ADMIN ZEESHAN",
        )
        assertNotDeleted(
            "I think this message was deleted by admin Zeeshan", "Was this message deleted by an admin?",
            "This message was deleted by an admin, right?", "This message was deleted by admin because of spam?",
            "This message was deleted by admin Zeeshan?", "This message was deleted by admin Zeeshan!",
            "This message was deleted by admin?", "You deleted this message", "You deleted this message as admin",
        )
    }

    @Test fun `indonesian`() {
        assertPlaceholder("Pesan ini dihapus", ".")
        assertPlaceholder("Pesan ini telah dihapus", ".")
        assertAdmin("Pesan ini dihapus oleh admin.", "Pesan ini dihapus oleh admin Budi.")
        assertNotDeleted(
            "Kayaknya pesan ini dihapus oleh admin Budi", "Pesan ini dihapus oleh admin?", "Pesan ini dihapus oleh admin Budi?",
            "Anda telah menghapus pesan ini",
        )
    }

    @Test fun `portuguese brazil`() {
        assertPlaceholder("Mensagem apagada", ".")
        assertPlaceholder("Essa mensagem foi apagada", ".")
        assertPlaceholder("Esta mensagem foi apagada", ".")
        assertAdmin("Mensagem apagada por um admin", "Mensagem apagada por um admin (Ana)")
        assertNotDeleted(
            "Acho que a mensagem apagada por um admin era minha", "Mensagem apagada por um admin?",
            "Mensagem apagada por um admin (Ana)?", "Mensagem apagada por um admin (Ana?)", "Você apagou essa mensagem",
        )
    }

    @Test fun `portuguese portugal`() {
        assertPlaceholder("Esta mensagem foi apagada", ".")
        assertPlaceholder("Esta mensagem foi apagada pelo remetente", ".")
        assertAdmin(
            "Esta mensagem foi apagada por um/a administrador/a.", "Esta mensagem foi apagada por um/a administrador/a, Rui.",
            "Esta mensagem foi apagada pelo/a administrador/a Rui",
        )
        assertNotDeleted(
            "Porque é que esta mensagem foi apagada por um/a administrador/a?", "Esta mensagem foi apagada por um administrador",
            "Apagou esta mensagem",
        )
    }

    @Test fun `urdu`() {
        assertPlaceholder("یہ میسج حذف کر دیا گیا ہے", "۔")
        assertPlaceholder("یہ پیغام حذف کیا گیا", "۔")
        assertPlaceholder("یہ پیغام حذف کر دیا گیا", "۔")
        // Arabic kaf/yeh instead of the Urdu letters: the same phrase.
        assertTrue(NotificationText.isDeletedPattern("یہ پیغام حذف کیا گیا".replace('\u06CC', '\u064A').replace('\u06A9', '\u0643')))
        assertAdmin("یہ میسج ایک ایڈمن نے حذف کر دیا ہے۔", "یہ میسج ایڈمن علی نے حذف کر دیا ہے۔")
        assertNotDeleted("کیا یہ میسج ایڈمن علی نے حذف کر دیا ہے؟", "یہ میسج ایڈمن نے حذف کر دیا ہے", "آپ نے یہ پیغام حذف کر دیا")
    }

    @Test fun `hindi`() {
        assertPlaceholder("यह मैसेज डिलीट कर दिया गया है", ".")
        assertPlaceholder("यह मैसेज डिलीट कर दिया गया है", "।")
        assertPlaceholder("यह मैसेज मिटाया गया", "।")
        assertPlaceholder("यह संदेश मिटाया गया", "।")
        assertPlaceholder("यह मैसेज हटा दिया गया", "।")
        assertAdmin("यह मैसेज किसी एडमिन ने डिलीट कर दिया है.", "यह मैसेज एडमिन राहुल ने डिलीट कर दिया है.")
        assertNotDeleted("क्या यह मैसेज एडमिन राहुल ने डिलीट कर दिया है?", "यह मैसेज आपने मिटाया है")
    }

    @Test fun `turkish`() {
        assertPlaceholder("Bu mesaj silindi", ".")
        assertAdmin("Bu mesaj bir yönetici tarafından silindi.", "Bu mesaj Ayşe adlı yönetici tarafından silindi.")
        assertNotDeleted("Bu mesaj yönetici tarafından silindi mi?", "Sanırım bu mesaj Ayşe adlı yönetici tarafından silindi", "Bu mesajı sildiniz")
    }

    @Test fun `spanish`() {
        assertPlaceholder("Se eliminó este mensaje", ".")
        assertPlaceholder("Se elimino\u0301 este mensaje", ".") // decomposed accent: NFC
        assertPlaceholder("Este mensaje fue eliminado", ".")
        assertAdmin("Un admin. eliminó este mensaje.", "Un admin., Lucía, eliminó este mensaje.")
        assertNotDeleted("Un admin eliminó este mensaje, creo", "Creo que un admin., Lucía, eliminó este mensaje", "Eliminaste este mensaje")
    }

    @Test fun `arabic`() {
        assertPlaceholder("تم حذف هذه الرسالة", ".")
        assertAdmin("حذف أحد المشرفين هذه الرسالة.", "حذف المشرف أحمد هذه الرسالة.")
        assertNotDeleted("لماذا حذف المشرف أحمد هذه الرسالة؟", "أنت حذفت هذه الرسالة")
    }

    @Test fun `persian`() {
        assertPlaceholder("این پیام حذف شده است", ".")
        assertPlaceholder("این پیام حذف شد", ".")
        // Arabic yeh instead of the Persian one, as older keyboards type it: the same phrase.
        assertTrue(NotificationText.isDeletedPattern("این پیام حذف شد".replace('\u06CC', '\u064A')))
        assertAdmin("یکی از مدیران این پیام را حذف کرد.", "این پیام را مدیر (رضا) حذف کرد.", "این پیام را مدیر رضا حذف کرد.")
        assertNotDeleted("چرا این پیام را مدیر رضا حذف کرد؟", "شما این پیام را حذف کردید.")
    }

    @Test fun `pashto uses english`() {
        // WhatsApp for Android has no Pashto UI: a phone set to Pashto shows the English placeholder.
        assertPlaceholder("This message was deleted", ".")
        assertAdmin("This message was deleted by admin وحید")
        assertNotDeleted("سمه ده", "مننه")
    }

    @Test fun `hebrew`() {
        assertPlaceholder("הודעה זו נמחקה", ".")
        assertAdmin("ההודעה נמחקה על ידי מנהל/ת.", "ההודעה נמחקה על ידי דנה, מנהל/ת הקבוצה.")
        assertNotDeleted("למה ההודעה נמחקה על ידי מנהל/ת?", "מחקת הודעה זו")
    }

    @Test fun `french`() {
        assertPlaceholder("Ce message a été supprimé", ".")
        assertTrue(NotificationText.isDeletedPattern("Ce message a été\u00A0supprimé.")) // no-break space
        assertAdmin(
            "Ce message a été supprimé par un ou une admin.", "Ce message a été supprimé par l'admin Julie",
            "Ce message a été supprimé par l\u2019admin Julie",
        )
        assertNotDeleted(
            "Ce message a été supprimé par erreur", "Je crois que ce message a été supprimé par l'admin Julie",
            "Ce message a été supprimé par l'admin Julie ?", "Ce message a été supprimé par l'admin Julie\u00A0?",
            "Ce message a été supprimé par l\u2019admin Julie?", "Vous avez supprimé ce message",
        )
    }

    @Test fun `german`() {
        assertPlaceholder("Diese Nachricht wurde gelöscht", ".")
        assertAdmin("Diese Nachricht wurde von einem*einer Admin gelöscht.", "Diese Nachricht wurde von Admin Max gelöscht.")
        assertNotDeleted(
            "Diese Nachricht wurde von Admin gelöscht", "Diese Nachricht wurde von Admin Max gelöscht, glaube ich",
            "Ich glaube, diese Nachricht wurde von Admin Max gelöscht", "Du hast diese Nachricht gelöscht.",
        )
    }

    @Test fun `italian`() {
        assertPlaceholder("Questo messaggio è stato eliminato", ".")
        assertAdmin(
            "Questo messaggio è stato eliminato da un amministratore.", "Questo messaggio è stato eliminato dall'amministratore Luca.",
        )
        assertNotDeleted(
            "Perché questo messaggio è stato eliminato dall'amministratore Luca?",
            "Questo messaggio è stato eliminato dall'amministratore Luca?", "Hai eliminato questo messaggio",
        )
    }

    @Test fun `russian`() {
        assertPlaceholder("Данное сообщение удалено", ".")
        assertTrue(NotificationText.isDeletedPattern("ДАННОЕ СООБЩЕНИЕ УДАЛЕНО"))
        assertAdmin(
            "Данное сообщение удалено админом.", "Данное сообщение удалено админом (Иван).",
            "ДАННОЕ СООБЩЕНИЕ УДАЛЕНО АДМИНОМ (ИВАН).", // Unicode case folding in the admin regexes
        )
        assertNotDeleted(
            "Данное сообщение удалено админом?", "Кажется, данное сообщение удалено админом (Иван)",
            "Данное сообщение удалено админом (Иван)?", "Данное сообщение удалено админом (Иван?)", "Вы удалили данное сообщение",
        )
    }

    @Test fun `chinese`() {
        assertPlaceholder("这条消息已被删除", "。")
        assertPlaceholder("消息已删除", "。")
        assertPlaceholder("信息已删除", "。")
        // WhatsApp pads the name with spaces; Chinese text often has none, so they are optional.
        assertAdmin(
            "管理员已删除这条消息。", "管理员 张伟 已删除这条消息。", "管理员张伟已删除这条消息。", "管理员 张伟已删除这条消息",
            "管理员张伟 已删除这条消息", "管理员 Wei Zhang 已删除这条消息。",
        )
        assertNotDeleted(
            "为什么管理员已删除这条消息？", "为什么管理员张伟已删除这条消息", "管理员张伟已删除这条消息吗？", "管理员张伟？已删除这条消息",
            "您已删除这条消息",
        )
    }

    @Test fun `hausa`() {
        // Unverified guess (WhatsApp's Hausa strings could not be read); older WhatsApp builds show English.
        assertPlaceholder("An goge wannan saƙon", ".")
        assertPlaceholder("This message was deleted", ".")
        assertNotDeleted("An goge wannan saƙon ne?", "Sannu")
    }

    @Test fun `burmese uses english`() {
        // WhatsApp for Android has no Burmese UI: a phone set to Burmese shows the English placeholder.
        assertPlaceholder("This message was deleted", ".")
        assertNotDeleted("ဟုတ်ကဲ့", "ကျေးဇူးတင်ပါတယ်")
    }

    @Test fun `every listed placeholder and admin variant is detected`() {
        WhatsAppStrings.deletedPlaceholders.forEach { (language, placeholders) ->
            placeholders.forEach { assertTrue("$language: $it", NotificationText.isDeletedPattern(it)) }
        }
        WhatsAppStrings.deletedByAdmin.forEach { (language, templates) ->
            templates.forEach {
                val line = it.replace("%s", "Ayu").replace("%@", "Ayu")
                assertTrue("$language: $line", NotificationText.isDeletedPattern(line))
            }
        }
        assertEquals(19, WhatsAppStrings.deletedPlaceholders.size)
    }

    @Test fun `a name never holds a question or exclamation mark in any script`() {
        listOf("?", "!", "\u061F", "\uFF1F", "\uFF01").forEach { mark ->
            assertNotDeleted(
                "This message was deleted by admin Zeeshan$mark", "Diese Nachricht wurde von Admin Max$mark gelöscht.",
            )
        }
        assertAdmin("This message was deleted by admin Zeeshan", "Diese Nachricht wurde von Admin Max gelöscht.")
    }

    @Test fun `a text with a line break is never a placeholder`() {
        assertNotDeleted(
            "This message was deleted by admin Zeeshan\nand nobody knows why", "This message was deleted\nby admin Zeeshan",
            "This message\nwas deleted", "This message was deleted\r\nlol", "Данное сообщение\u2028удалено",
            "Diese Nachricht wurde von Admin Max\ngelöscht.", "管理员张伟\n已删除这条消息",
        )
        // Outer whitespace is trimmed first, line breaks included, as kindOf does: both agree on such a line.
        assertTrue(NotificationText.isDeletedPattern("This message was deleted\n"))
        assertEquals(MessageKind.DELETED, NotificationText.kindOf("This message was deleted\n"))
    }

    @Test fun `admin names are bounded`() {
        val name = "A".repeat(80)
        assertTrue(NotificationText.isDeletedPattern("Diese Nachricht wurde von Admin $name gelöscht."))
        assertFalse(NotificationText.isDeletedPattern("Diese Nachricht wurde von Admin ${name}A gelöscht."))
    }
}
