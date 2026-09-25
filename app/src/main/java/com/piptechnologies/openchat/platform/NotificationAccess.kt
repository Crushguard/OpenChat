package com.piptechnologies.openchat.platform

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Notification Access, the one gate for tools 2, 3 and 4 (ruling R17, §5.5). */
object NotificationAccess {
    private const val LISTENER_CLASS = "com.piptechnologies.openchat.service.WaNotificationListenerService"

    /**
     * API 27+: the system's own record of the grant. The enabled_notification_listeners setting that
     * NotificationManagerCompat reads can trail a revocation (device run 22: 7 of 19 language passes on API 34 still
     * read "granted" after the listener had been unbound), and it keeps an old answer when the setting becomes empty.
     */
    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null) return manager.isNotificationListenerAccessGranted(ComponentName(context, LISTENER_CLASS))
        }
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    /** API 30+: the listener's own detail page; below, or when that page cannot be opened, the generic listener list. */
    fun openSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val component = ComponentName(context, LISTENER_CLASS).flattenToString()
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startActivitySafely(context, detail)) return
        }
        startActivitySafely(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
