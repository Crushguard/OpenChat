package com.piptechnologies.openchat.service

import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import com.piptechnologies.openchat.core.messages.NotificationText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The notification listener (§5.2): reads WhatsApp and WhatsApp Business chat notifications into the messages
 * store (deletions are detected as notifications are posted), tells the ingestor when they are removed, and runs the
 * media watcher while connected (§5.3). While recovery is paused it ignores notifications altogether. It only reads:
 * it never cancels, snoozes or otherwise acts on a notification.
 */
@AndroidEntryPoint
class WaNotificationListenerService : NotificationListenerService() {
    @Inject lateinit var parser: NotificationParser

    @Inject lateinit var ingestor: NotificationIngestor

    @Inject lateinit var mediaWatcher: MediaWatcher

    /** Notification work, one piece at a time and off the main thread; replaced when the listener reconnects. */
    private var scope: CoroutineScope = newScope()

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!scope.isActive) scope = newScope()
        guard("start the media watcher") { mediaWatcher.start() }
    }

    override fun onListenerDisconnected() {
        shutDown()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName !in NotificationText.watchedPackages) return
        scope.launch {
            try {
                val parsed = parser.parse(sbn) ?: return@launch
                // The paused check runs under the ingestor's lock, so events keep their delivery order.
                if (ingestor.onPosted(parsed, parser.images(sbn))) mediaWatcher.requestReconcile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not read a notification from ${sbn.packageName}", e)
            }
        }
    }

    /** Before API 26 a removal carries no reason, so there is nothing to act on. */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    /** API 26+ (below, the system calls the one-argument overload instead). */
    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        if (sbn == null || sbn.packageName !in NotificationText.watchedPackages) return
        val key = sbn.key ?: return
        scope.launch {
            try {
                ingestor.onRemoved(key, reason)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not handle a removed notification from ${sbn.packageName}", e)
            }
        }
    }

    private fun shutDown() {
        guard("stop the media watcher") { mediaWatcher.stop() }
        scope.cancel()
    }

    /** Runs [block], logging instead of crashing the listener when it fails. */
    private inline fun guard(action: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Could not $action", e)
        }
    }

    private companion object {
        const val TAG = "WaNotificationListener"

        /** Serial (parallelism 1) on the IO pool; a failure is logged, never thrown at the process. */
        fun newScope(): CoroutineScope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO.limitedParallelism(1) +
                CoroutineExceptionHandler { _, e -> Log.w(TAG, "Notification work failed", e) },
        )
    }
}
