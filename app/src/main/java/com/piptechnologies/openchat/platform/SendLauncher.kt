package com.piptechnologies.openchat.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.piptechnologies.openchat.core.send.SendLink

/** Opens a chat link in the chosen messaging app, or its web link (wa.me / t.me) in the browser (§5.1). */
object SendLauncher {
    /** Tries link.uri with setPackage(app.packageName); on ActivityNotFoundException or when the package is not installed, opens link.webUri without a package. Returns false only when nothing could handle it. */
    fun launch(context: Context, link: SendLink): Boolean {
        if (InstalledMessagingApps.isInstalled(context, link.app)) {
            val inApp = Intent(Intent.ACTION_VIEW, Uri.parse(link.uri)).setPackage(link.app.packageName)
            if (startActivitySafely(context, inApp)) return true
        }
        return startActivitySafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(link.webUri)))
    }
}
