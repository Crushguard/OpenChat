package com.piptechnologies.openchat.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.core.send.SendLinkBuilder
import com.piptechnologies.openchat.ui.components.withAppLocale

/** Browser, Play Store, share sheet, email composer and WhatsApp (rulings R11, R12, R16). */
object ExternalLinks {
    fun openUrl(context: Context, url: String): Boolean =
        startActivitySafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /** market://details?id=<pkg>, falling back to the Play Store web page (ruling R12). */
    fun openPlayStore(context: Context): Boolean {
        val pkg = context.packageName
        return openUrl(context, "market://details?id=$pkg") ||
            openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
    }

    /**
     * The system share sheet with share_app_text in the app's language: [context] is usually the
     * application context, which on API 24–32 does not carry the per-app language, hence withAppLocale.
     */
    fun shareApp(context: Context) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, context.withAppLocale().getString(R.string.share_app_text))
        startActivitySafely(context, Intent.createChooser(send, null))
    }

    /** ACTION_SENDTO mailto: (email apps only) with recipient, subject and body; false when no email app (ruling R11). */
    fun composeEmail(context: Context, to: String, subject: String, body: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        return startActivitySafely(context, intent)
    }

    /** With digits: the chat through [SendLauncher]; without (or if that fails): the app itself. False when neither opens. */
    fun openWhatsAppChat(context: Context, e164Digits: String?, app: MessagingApp = MessagingApp.WHATSAPP): Boolean {
        if (!e164Digits.isNullOrBlank() && SendLauncher.launch(context, SendLinkBuilder.build(app, e164Digits, "")) != SendRoute.NONE) {
            return true
        }
        val launch = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return false
        return startActivitySafely(context, launch)
    }
}

/**
 * Every activity start of the platform helpers goes through here. Adds FLAG_ACTIVITY_NEW_TASK unless
 * [context] is an Activity (the ViewModels pass their application context), and turns "nothing can
 * handle it" into false instead of a crash.
 */
internal fun startActivitySafely(context: Context, intent: Intent): Boolean {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
