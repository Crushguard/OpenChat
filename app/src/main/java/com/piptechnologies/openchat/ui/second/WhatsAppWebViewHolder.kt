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
     * Ends the session on this phone (§5.4): cookies, web storage, cache, form data and history are cleared, and
     * WhatsApp Web is loaded again only once the cookie store is empty, so it comes back at the QR code.
     */
    fun logout() {
        onMain {
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

        /** Link schemes handed to other apps; anything else (intent:, file:, blob: …) is never started. */
        val EXTERNAL_SCHEMES = setOf("http", "https", "mailto", "tel")
    }
}
