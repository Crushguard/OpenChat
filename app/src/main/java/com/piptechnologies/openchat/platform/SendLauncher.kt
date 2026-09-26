package com.piptechnologies.openchat.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.piptechnologies.openchat.core.send.SendLink

/** Where a chat link opened: in the chosen app, in the browser (its web link), or nowhere. */
enum class SendRoute { IN_APP, WEB, NONE }

/** Opens a chat link in the chosen messaging app, or its web link (wa.me / t.me) in the browser (§5.1). */
object SendLauncher {
    /** Tries link.uri with setPackage(app.packageName); on ActivityNotFoundException or when the package is not installed, opens link.webUri without a package. */
    fun launch(context: Context, link: SendLink): SendRoute {
        if (InstalledMessagingApps.isInstalled(context, link.app)) {
            val inApp = Intent(Intent.ACTION_VIEW, Uri.parse(link.uri)).setPackage(link.app.packageName)
            if (startActivitySafely(context, inApp)) return SendRoute.IN_APP
        }
        return if (startActivitySafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(link.webUri)))) SendRoute.WEB else SendRoute.NONE
    }
}
