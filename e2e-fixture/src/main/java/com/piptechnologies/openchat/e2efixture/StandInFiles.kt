package com.piptechnologies.openchat.e2efixture

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * The stand-in's files, all under `<externalMediaDirs[0]>` = /sdcard/Android/media/com.whatsapp, where the shell
 * (and so the instrumented tests, through UiAutomation) can read them:
 * - `e2e/sends.txt`: one line per chat link received by [ChatLinkActivity];
 * - `e2e/acks.txt`: one line "done <ACTION> <arg>" per command [CommandReceiver] finished;
 * - `e2e/stay`: when present, [ChatLinkActivity] stays open instead of closing at once;
 * - `WhatsApp/Media/WhatsApp Images/`: the media folder OpenChat's watcher reads.
 */
internal object StandInFiles {
    const val SENDS = "sends.txt"
    const val ACKS = "acks.txt"
    const val STAY = "stay"

    private val lock = Any()

    @Suppress("DEPRECATION") // getExternalMediaDirs: deprecated on 30+, still the app's own Android/media folder.
    fun mediaRoot(context: Context): File =
        context.externalMediaDirs.firstOrNull { it != null } ?: error("No external media directory")

    fun e2eDir(context: Context): File = File(mediaRoot(context), "e2e").apply { mkdirs() }

    fun imagesDir(context: Context): File = File(mediaRoot(context), "WhatsApp/Media/WhatsApp Images").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(e2eDir(context), name)

    /** Appends [line] and a newline to e2e/[name]. */
    fun append(context: Context, name: String, line: String) {
        synchronized(lock) {
            FileOutputStream(file(context, name), true).use { it.write((line + "\n").toByteArray(Charsets.UTF_8)) }
        }
    }

    fun ack(context: Context, action: String, arg: String) = append(context, ACKS, "done $action $arg")

    fun stayRequested(context: Context): Boolean = file(context, STAY).exists()
}
