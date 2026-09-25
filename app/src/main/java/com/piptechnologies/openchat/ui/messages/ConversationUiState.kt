package com.piptechnologies.openchat.ui.messages

import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.CapturedMessage
import com.piptechnologies.openchat.core.messages.ConversationSummary
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.core.messages.MessageKind
import com.piptechnologies.openchat.core.messages.NotificationText
import com.piptechnologies.openchat.core.phone.DialCountries
import com.piptechnologies.openchat.core.phone.PhoneNumberNormalizer
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.isolate
import com.piptechnologies.openchat.ui.components.ltr
import com.piptechnologies.openchat.ui.components.pluralText
import com.piptechnologies.openchat.ui.components.uiText

/**
 * What the conversation shows (design map §4.9): the bar [title] and [subtitle] ([conversationSubtitle]:
 * "<+number or title> · seen by no one" in All, "… · N deleted" in Deleted only), the [messages] listed
 * (oldest first; only the deleted ones in Deleted only), the local copy of each photo by media id
 * ([thumbnails]), the digits for wa.me when the conversation is a number ([phoneDigits]), and [nowMs] for
 * the day pills.
 */
data class ConversationUiState(
    val mode: InboxMode,
    val title: String,
    val subtitle: UiText,
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

/**
 * The bar's second line for [summary] (null while the conversation is empty): the sender's number
 * ("+62 812 3456 7890") or, for a saved contact, its title, then "seen by no one" in All or "N deleted" in
 * Deleted only. The number is a left-to-right isolate ([ltr]) so a right-to-left language keeps its digits
 * in order, and a contact name is a first-strong isolate ([isolate]) so a Hebrew or Arabic name does not
 * reorder an English sentence (or a Latin name an Arabic one); isolates are for display only and never
 * reach the database or an intent.
 */
internal fun conversationSubtitle(summary: ConversationSummary?, mode: InboxMode): UiText {
    val title = summary?.title.orEmpty()
    val who = summary?.phoneNumber?.let { digits -> ltr(displayPhoneNumber(digits) ?: title) } ?: isolate(title)
    return when (mode) {
        InboxMode.ALL -> uiText(R.string.conversation_subtitle_all, who)
        InboxMode.DELETED -> {
            val deleted = summary?.deletedCount ?: 0
            pluralText(R.plurals.conversation_subtitle_deleted, deleted, who, deleted)
        }
    }
}

/**
 * [title] as the inbox row, the conversation bar and the exclude sheet show it: an unsaved sender's phone
 * number as a left-to-right isolate ([ltr]), so right-to-left layouts do not reverse its digit groups
 * ("0417 9922 813 62+"); a name unchanged. Display only.
 */
internal fun displayTitle(title: String): String = if (NotificationText.phoneNumberFrom(title) != null) ltr(title) else isolate(title)

/** "+62 812 3456 7890" for the digits of an international number, or null when no dial code prefixes them. */
internal fun displayPhoneNumber(digits: String): String? {
    val country = DialCountries.longestPrefixMatch(digits) ?: return null
    val national = digits.removePrefix(country.dialCode)
    if (national.isEmpty()) return null
    return PhoneNumberNormalizer.displayInternational(country.dialCode, national)
}
