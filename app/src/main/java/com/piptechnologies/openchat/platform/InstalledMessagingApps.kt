package com.piptechnologies.openchat.platform

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.piptechnologies.openchat.core.send.MessagingApp

/** The messaging apps the Send button can target; the manifest `<queries>` makes them visible on API 30+. */
object InstalledMessagingApps {
    /** Apps that have a launcher entry, in [MessagingApp] enum order. */
    fun installed(context: Context): List<MessagingApp> =
        MessagingApp.entries.filter { isInstalled(context, it) }

    /** The app's own launcher icon (ruling R10); null when the app is not installed. */
    fun icon(context: Context, app: MessagingApp): Drawable? =
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    internal fun isInstalled(context: Context, app: MessagingApp): Boolean =
        context.packageManager.getLaunchIntentForPackage(app.packageName) != null
}
