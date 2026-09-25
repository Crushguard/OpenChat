package com.piptechnologies.openchat.screenshots

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.InboxBuilder
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.ui.components.ConfirmSheetContent
import com.piptechnologies.openchat.ui.components.HatchedPlaceholder
import com.piptechnologies.openchat.ui.components.SheetPreviewFrame
import com.piptechnologies.openchat.ui.components.UiText
import com.piptechnologies.openchat.ui.components.ltr
import com.piptechnologies.openchat.ui.components.pluralText
import com.piptechnologies.openchat.ui.components.uiText
import com.piptechnologies.openchat.ui.messages.ConversationScreen
import com.piptechnologies.openchat.ui.messages.ConversationUiState
import com.piptechnologies.openchat.ui.messages.MessagesCallbacks
import com.piptechnologies.openchat.ui.messages.MessagesScreen
import com.piptechnologies.openchat.ui.messages.MessagesUiState
import com.piptechnologies.openchat.ui.messages.ToolSettingsSheetContent
import com.piptechnologies.openchat.ui.messages.clearMessagesSpec
import com.piptechnologies.openchat.ui.messages.conversationMessages
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The number the design shows in Ayu's conversation bar ("+62 812 3456 7890 · seen by no one"). */
private const val AYU_NUMBER = "+62 812 3456 7890"

/**
 * Messages inbox (All / Deleted only / empty), the conversation in both modes, the tool settings sheet
 * and the clear-all confirmation (design map §4.8–§4.10, §4.20), from the §6 fake data. The screens format
 * times in the default zone: render them in [Fakes.timeZone].
 */
object MessagesScenes {
    val unseenList = Scene("unseen_list") {
        MessagesScreen(state = inbox(InboxMode.ALL), callbacks = MessagesCallbacks())
    }

    val recoverMessagesList = Scene("recover_messages_list") {
        MessagesScreen(state = inbox(InboxMode.DELETED), callbacks = MessagesCallbacks())
    }

    val messagesEmpty = Scene("messages_empty") {
        MessagesScreen(state = emptyInbox(), callbacks = MessagesCallbacks())
    }

    val unseenConversation = Scene("unseen_conversation") {
        ConversationScreen(
            state = ayu(InboxMode.ALL, uiText(R.string.conversation_subtitle_all, ltr(AYU_NUMBER))),
            onBack = {},
            onOpenInWhatsApp = {},
            onOpenMedia = {},
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    val recoverMessagesConversation = Scene("recover_messages_conversation") {
        ConversationScreen(
            state = ayu(InboxMode.DELETED, pluralText(R.plurals.conversation_subtitle_deleted, 2, ltr(AYU_NUMBER), 2)),
            onBack = {},
            onOpenInWhatsApp = {},
            onOpenMedia = {},
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    val toolSettingsSheet = Scene("tool_settings_sheet") {
        SheetPreviewFrame(
            screen = { MessagesScreen(state = inbox(InboxMode.DELETED), callbacks = MessagesCallbacks()) },
        ) {
            // The texts MessagesRoute gives the sheet.
            ToolSettingsSheetContent(
                stringResource(R.string.messages_title),
                false,
                2,
                stringResource(R.string.tool_settings_clear_messages),
                {},
                {},
                {},
            )
        }
    }

    val dialogClearAll = Scene("dialog_clear_all") {
        SheetPreviewFrame(
            topRadius = 26.dp,
            screen = { MessagesScreen(state = inbox(InboxMode.DELETED), callbacks = MessagesCallbacks()) },
        ) {
            ConfirmSheetContent(clearMessagesSpec(), {}, {})
        }
    }

    val all: List<Scene> = listOf(
        unseenList,
        recoverMessagesList,
        messagesEmpty,
        unseenConversation,
        recoverMessagesConversation,
        toolSettingsSheet,
        dialogClearAll,
    )

    /** The five fake conversations: 7 unread and 4 deleted messages, recovery active, 2 chats excluded. */
    private fun inbox(mode: InboxMode): MessagesUiState = MessagesUiState(
        mode = mode,
        conversations = InboxBuilder.build(Fakes.messages, mode),
        unreadTotal = Fakes.messages.count { !it.seenLocally },
        deletedTotal = Fakes.messages.count { it.isDeleted },
        paused = false,
        excludedCount = 2,
        excludable = emptyList(),
        toolSettingsOpen = false,
        excludeOpen = false,
        confirmClear = false,
        nowMs = Fakes.now,
    )

    /** Deleted only on a fresh install: nothing captured yet. */
    private fun emptyInbox(): MessagesUiState = inbox(InboxMode.DELETED).copy(
        conversations = emptyList(),
        unreadTotal = 0,
        deletedTotal = 0,
        excludedCount = 0,
    )

    /** Ayu Lestari's conversation ([AYU_NUMBER]): 4 messages, 2 of them deleted, one a photo (media item 1). */
    private fun ayu(mode: InboxMode, subtitle: UiText): ConversationUiState {
        val messages = Fakes.messages.filter { it.conversationKey == Fakes.ayuKey }
        return ConversationUiState(
            mode = mode,
            title = messages.last().conversationTitle,
            subtitle = subtitle,
            messages = conversationMessages(messages, mode),
            thumbnails = mapOf(Fakes.media.first().id to Fakes.media.first().localPath),
            phoneDigits = "6281234567890",
            nowMs = Fakes.now,
        )
    }
}

/** [MessagesScenes] in English. */
class MessagesScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    private val defaultZone: TimeZone = TimeZone.getDefault()

    /** The screens format times in the default zone; the fake wall-clock times are UTC. */
    @Before
    fun useFakeTimeZone() {
        TimeZone.setDefault(Fakes.timeZone)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultZone)
    }

    @Test
    fun unseen_list() = paparazzi.snapshot(MessagesScenes.unseenList)

    @Test
    fun recover_messages_list() = paparazzi.snapshot(MessagesScenes.recoverMessagesList)

    @Test
    fun messages_empty() = paparazzi.snapshot(MessagesScenes.messagesEmpty)

    @Test
    fun unseen_conversation() = paparazzi.snapshot(MessagesScenes.unseenConversation)

    @Test
    fun recover_messages_conversation() = paparazzi.snapshot(MessagesScenes.recoverMessagesConversation)

    @Test
    fun tool_settings_sheet() = paparazzi.snapshot(MessagesScenes.toolSettingsSheet)

    @Test
    fun dialog_clear_all() = paparazzi.snapshot(MessagesScenes.dialogClearAll)
}
