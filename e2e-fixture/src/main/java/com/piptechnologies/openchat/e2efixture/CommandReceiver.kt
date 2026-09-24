package com.piptechnologies.openchat.e2efixture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The end-to-end suite's remote control, driven from the shell:
 * `am broadcast -n com.whatsapp/com.piptechnologies.openchat.e2efixture.CommandReceiver -a <ACTION> [extras]`.
 * String extras are URL-encoded (UTF-8 form encoding; plain text without '%' or '+' reads the same), because
 * UiAutomation.executeShellCommand splits its command line on whitespace. Every action ends by appending
 * "done <ACTION> <arg>" to e2e/acks.txt ([StandInFiles]).
 *
 * - POST_CHAT  `--es title <name> --esa lines <epochMillis|sender|text>,…`: a MessagingStyle chat notification
 *   (category msg, channel "chats", tag = title, id 1) in a group with a summary, the shape a chat notification has.
 * - CANCEL_CHAT `--es title <name>`: withdraws that chat's notification (and the summary with the last chat).
 * - POST_SYSTEM: an ongoing "WhatsApp" / "Checking for new messages" notification (category service, no
 *   MessagingStyle), which OpenChat must ignore.
 * - WRITE_MEDIA `--es name <file.jpg>`: a generated 64×64 JPEG in WhatsApp/Media/WhatsApp Images, then a media scan.
 * - DELETE_MEDIA `--es name <file.jpg>`: deletes that file and its MediaStore row.
 * - RESET: cancels every notification, deletes the e2e files and the images (a clean slate per test).
 * - SET_STAY `--ez on true|false`: creates or deletes e2e/stay ([ChatLinkActivity] stays open while it exists).
 */
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (val action = intent.action.orEmpty()) {
            POST_CHAT -> {
                val title = intent.decoded(EXTRA_TITLE)
                val lines = intent.getStringArrayExtra(EXTRA_LINES).orEmpty().mapNotNull { ChatLine.parse(decode(it)) }
                ChatNotifications.postChat(app, title, lines)
                StandInFiles.ack(app, action, title)
            }
            CANCEL_CHAT -> {
                val title = intent.decoded(EXTRA_TITLE)
                ChatNotifications.cancelChat(app, title)
                StandInFiles.ack(app, action, title)
            }
            POST_SYSTEM -> {
                ChatNotifications.postSystem(app)
                StandInFiles.ack(app, action, ChatNotifications.APP_LABEL)
            }
            WRITE_MEDIA -> {
                val name = intent.decoded(EXTRA_NAME)
                val pending = goAsync()
                MediaFiles.write(app, name) {
                    StandInFiles.ack(app, action, name)
                    pending.finish()
                }
            }
            DELETE_MEDIA -> {
                val name = intent.decoded(EXTRA_NAME)
                MediaFiles.delete(app, name)
                StandInFiles.ack(app, action, name)
            }
            RESET -> {
                ChatNotifications.cancelAll(app)
                listOf(StandInFiles.SENDS, StandInFiles.ACKS, StandInFiles.STAY).forEach { StandInFiles.file(app, it).delete() }
                MediaFiles.deleteAll(app)
                StandInFiles.ack(app, action, "all")
            }
            SET_STAY -> {
                val on = intent.getBooleanExtra(EXTRA_ON, false)
                val flag = StandInFiles.file(app, StandInFiles.STAY)
                if (on) flag.writeText("stay") else flag.delete()
                StandInFiles.ack(app, action, on.toString())
            }
            else -> StandInFiles.ack(app, "UNKNOWN", action.ifEmpty { "-" })
        }
    }

    private fun Intent.decoded(key: String): String = decode(getStringExtra(key).orEmpty())

    companion object {
        const val POST_CHAT = "POST_CHAT"
        const val CANCEL_CHAT = "CANCEL_CHAT"
        const val POST_SYSTEM = "POST_SYSTEM"
        const val WRITE_MEDIA = "WRITE_MEDIA"
        const val DELETE_MEDIA = "DELETE_MEDIA"
        const val RESET = "RESET"
        const val SET_STAY = "SET_STAY"

        const val EXTRA_TITLE = "title"
        const val EXTRA_LINES = "lines"
        const val EXTRA_NAME = "name"
        const val EXTRA_ON = "on"

        fun decode(raw: String): String = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (e: IllegalArgumentException) {
            raw
        }
    }
}

/** One chat line: "epochMillis|sender|text" (the text may contain '|'). */
internal data class ChatLine(val timestamp: Long, val sender: String, val text: String) {
    companion object {
        fun parse(raw: String): ChatLine? {
            val parts = raw.split("|", limit = 3)
            if (parts.size != 3) return null
            val timestamp = parts[0].trim().toLongOrNull() ?: return null
            return ChatLine(timestamp, parts[1], parts[2])
        }
    }
}

/** The notifications a chat app posts: one MessagingStyle notification per chat, a group summary, a status line. */
internal object ChatNotifications {
    const val APP_LABEL = "WhatsApp"
    private const val CHANNEL_CHATS = "chats"
    private const val CHANNEL_SERVICE = "service"
    private const val GROUP = "group_key_messages"
    private const val CHAT_ID = 1
    private const val SUMMARY_TAG = "summary"
    private const val SUMMARY_ID = 0
    private const val SYSTEM_TAG = "system"
    private const val SYSTEM_ID = 2
    private const val SELF_NAME = "You"

    fun postChat(context: Context, title: String, lines: List<ChatLine>) {
        val manager = manager(context)
        val last = lines.lastOrNull()
        val notification = builder(context, CHANNEL_CHATS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(last?.text.orEmpty())
            .setStyle(messagingStyle(lines))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroup(GROUP)
            .setWhen(last?.timestamp ?: System.currentTimeMillis())
            .setShowWhen(true)
            .build()
        manager.notify(title, CHAT_ID, notification)
        val others = otherChats(manager, except = title)
        postSummary(context, chats = others.size + 1, messages = others.sumOf { it.second } + lines.size)
    }

    fun cancelChat(context: Context, title: String) {
        val manager = manager(context)
        manager.cancel(title, CHAT_ID)
        val others = otherChats(manager, except = title)
        if (others.isEmpty()) {
            manager.cancel(SUMMARY_TAG, SUMMARY_ID)
        } else {
            postSummary(context, chats = others.size, messages = others.sumOf { it.second })
        }
    }

    fun postSystem(context: Context) {
        val notification = builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(APP_LABEL)
            .setContentText("Checking for new messages")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        manager(context).notify(SYSTEM_TAG, SYSTEM_ID, notification)
    }

    private fun postSummary(context: Context, chats: Int, messages: Int) {
        val summary = builder(context, CHANNEL_CHATS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(APP_LABEL)
            .setContentText("$messages messages from $chats chats")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroup(GROUP)
            .setGroupSummary(true)
            .build()
        manager(context).notify(SUMMARY_TAG, SUMMARY_ID, summary)
    }

    /** The chat notifications showing now other than [except]'s, each with its number of messages. */
    private fun otherChats(manager: NotificationManager, except: String): List<Pair<String, Int>> =
        manager.activeNotifications
            .filter { it.id == CHAT_ID && it.tag != null && it.tag != except }
            .map { it.tag to messageCount(it.notification) }

    @Suppress("DEPRECATION") // getParcelableArray(String): the typed overload is API 33+.
    private fun messageCount(notification: Notification): Int =
        notification.extras?.getParcelableArray(Notification.EXTRA_MESSAGES)?.size ?: 1

    private fun messagingStyle(lines: List<ChatLine>): Notification.MessagingStyle {
        val style = if (Build.VERSION.SDK_INT >= 28) {
            Notification.MessagingStyle(Person.Builder().setName(SELF_NAME).build())
        } else {
            legacyStyle()
        }
        for (line in lines) {
            val message = if (Build.VERSION.SDK_INT >= 28) {
                Notification.MessagingStyle.Message(line.text, line.timestamp, Person.Builder().setName(line.sender).build())
            } else {
                legacyMessage(line)
            }
            style.addMessage(message)
        }
        return style
    }

    @Suppress("DEPRECATION") // The only constructor below API 28.
    private fun legacyStyle(): Notification.MessagingStyle = Notification.MessagingStyle(SELF_NAME)

    @Suppress("DEPRECATION") // The only constructor below API 28.
    private fun legacyMessage(line: ChatLine): Notification.MessagingStyle.Message =
        Notification.MessagingStyle.Message(line.text, line.timestamp, line.sender)

    private fun builder(context: Context, channel: String): Notification.Builder {
        if (Build.VERSION.SDK_INT < 26) return legacyBuilder(context)
        val manager = manager(context)
        if (manager.getNotificationChannel(channel) == null) {
            // Low / min importance: no heads-up over the app under test, no sound; listeners still receive them.
            val importance = if (channel == CHANNEL_CHATS) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_MIN
            manager.createNotificationChannel(NotificationChannel(channel, channel, importance))
        }
        return Notification.Builder(context, channel)
    }

    @Suppress("DEPRECATION") // The only constructor below API 26.
    private fun legacyBuilder(context: Context): Notification.Builder = Notification.Builder(context)

    fun cancelAll(context: Context) = manager(context).cancelAll()

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

/** Photos in WhatsApp/Media/WhatsApp Images, as OpenChat's media watcher finds them. */
internal object MediaFiles {
    private const val SCAN_TIMEOUT_MS = 8_000L
    private const val SIZE_PX = 64

    /** Writes a 64×64 JPEG named [name], scans it into the MediaStore, then calls [done] (at the latest after 8 s). */
    fun write(context: Context, name: String, done: () -> Unit) {
        val file = File(StandInFiles.imagesDir(context), name)
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(37, 211, 102))
        canvas.drawCircle(SIZE_PX / 2f, SIZE_PX / 2f, SIZE_PX / 4f, Paint().apply { color = Color.WHITE })
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val finished = AtomicBoolean(false)
        val finish = { if (finished.compareAndSet(false, true)) done() }
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, SCAN_TIMEOUT_MS)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg")) { _, _ -> finish() }
    }

    /** Deletes the photo [name] and its MediaStore row. */
    fun delete(context: Context, name: String) {
        remove(context, File(StandInFiles.imagesDir(context), name))
    }

    fun deleteAll(context: Context) {
        StandInFiles.imagesDir(context).listFiles()?.filter { it.isFile }?.forEach { remove(context, it) }
    }

    @Suppress("DEPRECATION") // MediaColumns.DATA: still filled in, and the way to find a file's row.
    private fun remove(context: Context, file: File) {
        val path = file.absolutePath
        file.delete()
        try {
            context.contentResolver.delete(MediaStore.Files.getContentUri("external"), "${MediaStore.MediaColumns.DATA}=?", arrayOf(path))
        } catch (e: RuntimeException) {
            // On API 30+ deleting the file already removed its row; below, a row we may not delete is left to the scanner.
        }
    }
}
