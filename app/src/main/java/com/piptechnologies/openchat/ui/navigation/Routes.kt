package com.piptechnologies.openchat.ui.navigation

import android.net.Uri
import com.piptechnologies.openchat.core.messages.InboxMode

/** Which tool the notification access gate was opened for (Settings opens it without a tool behind it). */
enum class GateTool { UNSEEN, DELETED_MESSAGES, MEDIA, SETTINGS }

/** The four tool rows on Home. */
enum class HomeTool { UNSEEN, DELETED_MESSAGES, MEDIA, SECOND }

/**
 * Navigation routes (design map §3). Routes with arguments come as a pattern constant to register
 * the destination with (`GATE`, `MESSAGES`, …) and a builder to navigate with (`gate(tool)`, …).
 * The `ARG_*` constants are the argument names inside those patterns.
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"

    const val GATE = "gate/{tool}"
    fun gate(tool: GateTool) = "gate/${tool.name}"

    const val MESSAGES = "messages/{mode}"
    fun messages(mode: InboxMode) = "messages/${mode.name}"

    /** [key] is a conversation key ("com.whatsapp|ayu lestari"), so it is URI-encoded. */
    const val CONVERSATION = "conversation/{mode}/{key}"
    fun conversation(mode: InboxMode, key: String) = "conversation/${mode.name}/${Uri.encode(key)}"

    const val MEDIA = "media"
    const val MEDIA_DETAIL = "mediaDetail/{id}"
    fun mediaDetail(id: Long) = "mediaDetail/$id"

    const val SECOND = "second"
    const val SETTINGS = "settings"
    const val LANGUAGE = "language"
    const val CONTACT = "contact"

    const val ARG_TOOL = "tool"
    const val ARG_MODE = "mode"
    const val ARG_KEY = "key"
    const val ARG_ID = "id"
}
