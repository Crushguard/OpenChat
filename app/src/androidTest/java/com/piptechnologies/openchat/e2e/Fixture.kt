package com.piptechnologies.openchat.e2e

import java.net.URLEncoder

/** One chat line for [Fixture.postChat]: when it was sent, by whom, and its text. */
data class ChatLine(val timestamp: Long, val sender: String, val text: String)

/**
 * Remote control for the CI-only stand-in app (module e2e-fixture, package com.whatsapp): shell broadcasts to its
 * CommandReceiver, each confirmed by a "done <ACTION> <arg>" line in its acks file, and the chat links it received.
 * Extras are URL-encoded so that they survive the whitespace split of [Shell.run]; the stand-in decodes them.
 */
object Fixture {
    const val PACKAGE = "com.whatsapp"
    private const val RECEIVER = "$PACKAGE/com.piptechnologies.openchat.e2efixture.CommandReceiver"
    private const val DIR = "/sdcard/Android/media/$PACKAGE/e2e"
    private const val SENDS = "$DIR/sends.txt"
    private const val ACKS = "$DIR/acks.txt"

    /** FLAG_RECEIVER_FOREGROUND | FLAG_INCLUDE_STOPPED_PACKAGES: delivered promptly even before the stand-in ever ran. */
    private const val BROADCAST_FLAGS = "0x10000020"

    /** Clears the stand-in's notifications, files and media: a clean slate for each test. */
    fun reset() = command("RESET", "all")

    /** A chat notification titled [title] showing [lines] (oldest first), like a new message arriving. */
    fun postChat(title: String, lines: List<ChatLine>) {
        val encoded = lines.joinToString(",") { enc("${it.timestamp}|${it.sender}|${it.text}") }
        command("POST_CHAT", title, "--es title ${enc(title)} --esa lines $encoded")
    }

    fun cancelChat(title: String) = command("CANCEL_CHAT", title, "--es title ${enc(title)}")

    /** The ongoing "Checking for new messages" status notification, which is not a chat. */
    fun postSystem() = command("POST_SYSTEM", "WhatsApp")

    /** A photo [name] in WhatsApp/Media/WhatsApp Images, scanned into the MediaStore. */
    fun writeMedia(name: String) = command("WRITE_MEDIA", name, "--es name ${enc(name)}")

    /** Deletes photo [name] and its MediaStore row, as when the sender deletes it for everyone. */
    fun deleteMedia(name: String) = command("DELETE_MEDIA", name, "--es name ${enc(name)}")

    /** While on, chat links keep the stand-in open instead of bouncing straight back. */
    fun stay(on: Boolean) = command("SET_STAY", on.toString(), "--ez on $on")

    /** The chat links the stand-in received, oldest first. */
    fun sends(): List<String> = readLines(SENDS)

    fun acks(): List<String> = readLines(ACKS)

    private fun command(action: String, arg: String, extras: String = "") {
        val ack = "done $action $arg"
        // RESET starts a new acks file, so its own confirmation is the first line of it.
        val before = if (action == "RESET") 0 else acks().count { it == ack }
        val output = Shell.run("am broadcast -n $RECEIVER -a $action -f $BROADCAST_FLAGS $extras".trim())
        Waits.until("the stand-in to confirm \"$ack\" (am broadcast said: ${output.trim().replace('\n', ' ')})") {
            acks().count { it == ack } > before
        }
    }

    private fun readLines(path: String): List<String> =
        Shell.run("cat $path").lines().map { it.trimEnd('\r') }.filter { it.isNotEmpty() }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
