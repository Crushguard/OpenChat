package com.piptechnologies.openchat.e2efixture

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** The launcher entry: gives the package a launch intent, which is how OpenChat decides the app is installed. */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(label(this, "E2E stand-in"))
    }
}

/**
 * Receives the chat links OpenChat opens (https://wa.me/<digits>?text=…, whatsapp://send?…), appends each one to
 * e2e/sends.txt and closes at once, the way the real app hands control back when a number has no account; that fast
 * return is what OpenChat's "not on WhatsApp" check looks for. With e2e/stay present it stays open instead.
 */
class ChatLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val link = intent?.dataString.orEmpty()
        // Recorded once per link, not again when the activity is recreated.
        if (savedInstanceState == null) StandInFiles.append(this, StandInFiles.SENDS, link)
        if (!StandInFiles.stayRequested(this)) {
            finish()
            return
        }
        setContentView(label(this, "E2E stand-in\n$link"))
    }
}

private fun label(activity: Activity, text: String): TextView =
    TextView(activity).apply {
        this.text = text
        val padding = (24 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
    }
