package com.piptechnologies.openchat.ui.second

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import com.piptechnologies.openchat.core.web.ProbeResult
import com.piptechnologies.openchat.core.web.WebSession
import com.piptechnologies.openchat.platform.startActivitySafely
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the one WhatsApp Web [WebView] of the second account (§4.14, §5.4). It lives as long as the process, so
 * leaving the screen, rotating or navigating never reloads the session, and
 * [com.piptechnologies.openchat.service.WebSessionService] keeps the process alive while linked. The view is built
 * on the application context, so it never holds on to an Activity.
 *
 * A WebView belongs to the main thread: [get] must be called there, [probe] switches to it, and the other calls
 * post themselves to it when made from another thread.
 */
@Singleton
class WhatsAppWebViewHolder @Inject constructor(@ApplicationContext private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Main thread only. */
    private var webView: WebView? = null

    /** The session's WebView, created, configured and pointed at [WebSession.URL] on the first call. */
    @MainThread
    fun get(): WebView {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "WhatsAppWebViewHolder.get() must be called on the main thread"
        }
        return webView ?: create().also { webView = it }
    }

    /** Reloads WhatsApp Web; a view that is somewhere else (or has nothing loaded) goes back to [WebSession.URL]. */
    fun reload() {
        onMain {
            val view = webView
            when {
                view == null -> get()
                isSessionPage(view.url) -> view.reload()
                else -> view.loadUrl(WebSession.URL)
            }
        }
    }

    /**
     * Runs [WebSession.LINKED_PROBE_JS] in the page. The WebView answers once; a page that has not answered
     * within one probe interval reads as [ProbeResult.UNKNOWN], and a late answer after that timeout (or after
     * cancellation) is dropped, so the coroutine resumes exactly once.
     */
    suspend fun probe(): ProbeResult = withContext(Dispatchers.Main.immediate) {
        val view = get()
        withTimeoutOrNull(WebSession.PROBE_INTERVAL_MS) {
            suspendCancellableCoroutine<ProbeResult> { continuation ->
                view.evaluateJavascript(WebSession.LINKED_PROBE_JS) { result ->
                    if (continuation.isActive) continuation.resume(WebSession.parseProbe(result))
                }
            }
        } ?: ProbeResult.UNKNOWN
    }

    /**
     * Ends the session on this phone (§5.4). A view that is on WhatsApp Web first asks the page to drop its own
     * storage (IndexedDB databases, localStorage, sessionStorage); once the page reports that done, or after
     * [WIPE_TIMEOUT_MS] if it never does, [clearSession] runs: cookies, web storage, cache, form data and history
     * are cleared, and WhatsApp Web is loaded again only once the cookie store is empty, so it comes back at the
     * QR code. With no view yet, or a view that is somewhere else, [clearSession] runs at once.
     */
    fun logout() {
        onMain {
            val view = webView
            if (view != null && isSessionPage(view.url)) {
                PageWipe(view, ::clearSession).start()
            } else {
                clearSession()
            }
        }
    }

    /** Main thread. Everything outside the page: about:blank first, then cookies, storage, cache, form data, history. */
    private fun clearSession() {
        // Leave WhatsApp Web first: the old page stops running while its data is wiped, and a probe made in
        // the meantime no longer finds it linked.
        webView?.loadUrl(BLANK_PAGE)
        val cookies = CookieManager.getInstance()
        cookies.removeAllCookies {
            // Called back on the main thread, after the removal.
            cookies.flush()
            webView?.let { view ->
                view.clearHistory()
                view.loadUrl(WebSession.URL)
            }
        }
        WebStorage.getInstance().deleteAllData()
        webView?.let { view ->
            view.clearCache(true)
            view.clearFormData()
        }
    }

    /** Takes the view out of its parent, so another screen (or a new composition) can attach it. */
    fun detach() {
        onMain {
            webView?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION") // databaseEnabled: Web SQL storage, which §5.4 asks for.
    private fun create(): WebView {
        val view = WebView(context.applicationContext)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = WebSession.DESKTOP_USER_AGENT
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }
        view.webViewClient = SessionClient()
        view.webChromeClient = WebChromeClient()
        view.loadUrl(WebSession.URL)
        return view
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }

    /**
     * Runs [WIPE_STORAGE_JS] in [view] and calls [onDone] exactly once, on the main thread: when the page's
     * flag reads [WIPE_DONE] (polled every [WIPE_POLL_MS]), or after [WIPE_TIMEOUT_MS] if it never does, for
     * example because the page hung or a callback never came. `evaluateJavascript` reports a script's return
     * value at once, not the end of its promise chain, which is why the page sets a flag that is polled. A view
     * the holder dropped in the meantime (renderer death) is not waited for.
     */
    private inner class PageWipe(private val view: WebView, private val onDone: () -> Unit) : Runnable {
        private var done = false
        private val timeout = Runnable { finish() }

        fun start() {
            mainHandler.postDelayed(timeout, WIPE_TIMEOUT_MS)
            view.evaluateJavascript(WIPE_STORAGE_JS) { mainHandler.postDelayed(this, WIPE_POLL_MS) }
        }

        /** One poll of the page's flag. */
        override fun run() {
            if (done) return
            if (view !== webView) {
                finish()
                return
            }
            view.evaluateJavascript(WIPE_STATE_JS) { result ->
                if (done) return@evaluateJavascript
                if (result?.trim()?.removeSurrounding("\"") == WIPE_DONE) {
                    finish()
                } else {
                    mainHandler.postDelayed(this, WIPE_POLL_MS)
                }
            }
        }

        private fun finish() {
            if (done) return
            done = true
            mainHandler.removeCallbacks(timeout)
            mainHandler.removeCallbacks(this)
            onDone()
        }
    }

    private fun isSessionPage(url: String?): Boolean =
        url != null && Uri.parse(url).host.equals(SESSION_HOST, ignoreCase = true)

    private inner class SessionClient : WebViewClient() {
        /**
         * WhatsApp Web's own navigation (its redirects and every subframe included) stays in the view. A link to
         * another site, which the desktop page opens in a new tab, goes to the browser instead of replacing the
         * session page; schemes a browser cannot take are dropped.
         */
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            val staysInView = !request.isForMainFrame ||
                request.isRedirect ||
                url.host.equals(SESSION_HOST, ignoreCase = true)
            if (staysInView) return false
            val scheme = url.scheme?.lowercase()
            if (scheme != null && scheme in EXTERNAL_SCHEMES) {
                startActivitySafely(context, Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
            }
            return true
        }

        /**
         * API 26+. Unhandled, a renderer the system kills to reclaim memory takes the app down with it. The dead
         * view is dropped instead, and the next [get] builds a new one on the stored session.
         */
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            if (view === webView) {
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                webView = null
            }
            return true
        }
    }

    private companion object {
        /** The host of [WebSession.URL]. */
        const val SESSION_HOST = "web.whatsapp.com"

        const val BLANK_PAGE = "about:blank"

        /** How long a logout waits for the page to report its storage wiped before carrying on regardless. */
        const val WIPE_TIMEOUT_MS = 1_500L
        const val WIPE_POLL_MS = 100L
        const val WIPE_DONE = "done"

        /** Reads the flag [WIPE_STORAGE_JS] sets ("pending" until every request has answered, then "done"). */
        const val WIPE_STATE_JS = "window.__openchatWipe"

        /**
         * Drops the page's own storage: localStorage and sessionStorage at once, then every IndexedDB database.
         * A deletion the page's open connections block completes once the page is left (about:blank), so
         * "blocked" counts as issued. The flag reads "done" when every request has answered, on any error,
         * or at once where `indexedDB.databases` does not exist (WebView before Chrome 71).
         */
        const val WIPE_STORAGE_JS = "(function(){" +
            "try{localStorage.clear();sessionStorage.clear()}catch(e){}" +
            "window.__openchatWipe='pending';" +
            "var done=function(){window.__openchatWipe='done'};" +
            "try{indexedDB.databases().then(function(list){return Promise.all(list.map(function(db){" +
            "return new Promise(function(resolve){var req=indexedDB.deleteDatabase(db.name);" +
            "req.onsuccess=req.onerror=req.onblocked=function(){resolve()}})}))}).then(done,done)}" +
            "catch(e){done()}" +
            "return 'started'})()"

        /** Link schemes handed to other apps; anything else (intent:, file:, blob: …) is never started. */
        val EXTERNAL_SCHEMES = setOf("http", "https", "mailto", "tel")
    }
}
