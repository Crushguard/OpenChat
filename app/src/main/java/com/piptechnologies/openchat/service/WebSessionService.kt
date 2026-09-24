package com.piptechnologies.openchat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.piptechnologies.openchat.MainActivity
import com.piptechnologies.openchat.R

/**
 * The second account's foreground service (type specialUse, §4.14, §5.4). While the WhatsApp Web session is linked
 * it shows the ongoing "Second account linked" notification, which keeps the process, and with it the WebView of
 * [com.piptechnologies.openchat.ui.second.WhatsAppWebViewHolder], alive after the user leaves the screen. The
 * Second account screen decides when it runs: [start] once it sees the session linked, [stop] on logout.
 * POST_NOTIFICATIONS is requested by that screen (API 33+); the service runs either way.
 */
class WebSessionService : Service() {
    private val notification: Notification by lazy { buildNotification() }

    /** Whether a startForeground() of this instance has gone through. */
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Stops only if this is the newest request, so a start queued after the stop still wins. The system
            // removes the foreground notification together with the service.
            stopSelf(startId)
        } else {
            enterForeground(startId)
        }
        // Not restarted after process death: the WebView died with the process, and the screen starts the service
        // again once it sees the session linked.
        return START_NOT_STICKY
    }

    /**
     * Runs on every start, the first and each repeat: re-posting the same notification under the same id only
     * refreshes it, and each startForegroundService() is followed by the startForeground() it requires.
     */
    private fun enterForeground(startId: Int) {
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType())
            inForeground = true
        } catch (e: RuntimeException) {
            // e.g. ForegroundServiceStartNotAllowedException (API 31+) for a start the system refuses. A service
            // that is already in the foreground stays there; one that never got there has nothing to show.
            Log.w(TAG, "Could not enter the foreground", e)
            if (!inForeground) stopSelf(startId)
        }
    }

    /** API 34+ must name the type; below, 0 is a subset of any manifest type and API 28- has no types at all. */
    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    /** API 26+; re-creating an existing channel only refreshes its name. */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.second_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.setShowBadge(false)
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    /** Built once per service instance, so a repeated start posts an identical notification. */
    private fun buildNotification(): Notification {
        // The launcher's intent: brings the existing task back as it was, or starts the app.
        val open = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.second_notification_title))
            .setContentText(getString(R.string.second_notification_text))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.second_notification_open), openApp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "second_account"
        const val NOTIFICATION_ID = 41

        private const val TAG = "WebSessionService"
        private const val ACTION_START = "com.piptechnologies.openchat.action.START_WEB_SESSION"
        private const val ACTION_STOP = "com.piptechnologies.openchat.action.STOP_WEB_SESSION"

        /** Shows the notification and keeps the session alive; safe to call again while the service runs. */
        fun start(context: Context) {
            val intent = Intent(context, WebSessionService::class.java).setAction(ACTION_START)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // API 31+: ForegroundServiceStartNotAllowedException when called while the app is in the background.
                Log.w(TAG, "Could not start the web session service", e)
            }
        }

        /**
         * Removes the notification and stops the service. It goes through onStartCommand rather than stopService()
         * so it lands after any start still in flight, whose startForeground() is then already done.
         */
        fun stop(context: Context) {
            val intent = Intent(context, WebSessionService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                // A background app without a running foreground service may not start one, so there is nothing
                // queued to wait for: stop it directly.
                Log.w(TAG, "Stopping the web session service directly", e)
                context.stopService(Intent(context, WebSessionService::class.java))
            }
        }
    }
}
