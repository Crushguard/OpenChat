package com.piptechnologies.openchat.ui.messages

import com.piptechnologies.openchat.core.messages.CapturedMessage
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.core.messages.MessageKind
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.PhoneNumberNormalizer

/**
 * What the conversation shows (design map §4.9): the bar [title] and [subtitle] ("<+number or title> ·
 * seen by no one" in All, "… · N deleted" in Deleted only), the [messages] listed (oldest first; only the
 * deleted ones in Deleted only), the local copy of each photo by media id ([thumbnails]), the digits for
 * wa.me when the conversation is a number ([phoneDigits]), and [nowMs] for the day pills.
 */
data class ConversationUiState(
    val mode: InboxMode,
    val title: String,
    val subtitle: String,
    val messages: List<CapturedMessage>,
    val thumbnails: Map<Long, String>,
    val phoneDigits: String?,
    val nowMs: Long,
)

/** The messages a conversation lists: all of them, or only the ones the sender deleted in Deleted only. */
internal fun conversationMessages(messages: List<CapturedMessage>, mode: InboxMode): List<CapturedMessage> =
    if (mode == InboxMode.DELETED) messages.filter { it.isDeleted } else messages

/** A message shows as a photo only when it is a photo whose image was copied; otherwise its text shows. */
internal fun isPhotoBubble(message: CapturedMessage): Boolean = message.kind == MessageKind.PHOTO && message.mediaId != null

/** "+62 812 3456 7890" for the digits of an international number, or null when no dial code prefixes them. */
internal fun displayPhoneNumber(digits: String): String? {
    val country = DialCountries.longestPrefixMatch(digits) ?: return null
    val national = digits.removePrefix(country.dialCode)
    if (national.isEmpty()) return null
    return PhoneNumberNormalizer.displayInternational(country.dialCode, national)
}
