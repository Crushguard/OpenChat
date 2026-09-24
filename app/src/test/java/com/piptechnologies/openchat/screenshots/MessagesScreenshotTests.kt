package com.piptechnologies.openchat.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.core.messages.InboxBuilder
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.ui.components.ConfirmSheetContent
import com.piptechnologies.openchat.ui.components.HatchedPlaceholder
import com.piptechnologies.openchat.ui.components.SheetPreviewFrame
import com.piptechnologies.openchat.ui.messages.ConversationScreen
import com.piptechnologies.openchat.ui.messages.ConversationUiState
import com.piptechnologies.openchat.ui.messages.MessagesCallbacks
import com.piptechnologies.openchat.ui.messages.MessagesScreen
import com.piptechnologies.openchat.ui.messages.MessagesUiState
import com.piptechnologies.openchat.ui.messages.ToolSettingsSheetContent
import com.piptechnologies.openchat.ui.messages.clearMessagesSpec
import com.piptechnologies.openchat.ui.messages.conversationMessages
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Messages inbox (All / Deleted only / empty), the conversation in both modes, the tool settings sheet
 * and the clear-all confirmation (design map §4.8–§4.10, §4.20), from the §6 fake data.
 */
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
    fun unseen_list() = snapshot {
        MessagesScreen(state = inbox(InboxMode.ALL), callbacks = MessagesCallbacks())
    }

    @Test
    fun recover_messages_list() = snapshot {
        MessagesScreen(state = inbox(InboxMode.DELETED), callbacks = MessagesCallbacks())
    }

    @Test
    fun messages_empty() = snapshot {
        MessagesScreen(state = emptyInbox(), callbacks = MessagesCallbacks())
    }

    @Test
    fun unseen_conversation() = snapshot {
        ConversationScreen(
            state = ayu(InboxMode.ALL, "+62 812 3456 7890 · seen by no one"),
            onBack = {},
            onOpenInWhatsApp = {},
            onOpenMedia = {},
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    @Test
    fun recover_messages_conversation() = snapshot {
        ConversationScreen(
            state = ayu(InboxMode.DELETED, "+62 812 3456 7890 · 2 deleted"),
            onBack = {},
            onOpenInWhatsApp = {},
            onOpenMedia = {},
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    @Test
    fun tool_settings_sheet() = snapshot {
        SheetPreviewFrame(
            screen = { MessagesScreen(state = inbox(InboxMode.DELETED), callbacks = MessagesCallbacks()) },
        ) {
            ToolSettingsSheetContent("Messages", false, 2, "Clear all kept messages", {}, {}, {})
        }
    }

    @Test
    fun dialog_clear_all() = snapshot {
        SheetPreviewFrame(
            topRadius = 26.dp,
            screen = { MessagesScreen(state = inbox(InboxMode.DELETED), callbacks = MessagesCallbacks()) },
        ) {
            ConfirmSheetContent(clearMessagesSpec(), {}, {})
        }
    }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            OpenChatTheme {
                content()
            }
        }
    }

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

    /** Ayu Lestari's conversation (+62 812 3456 7890): 4 messages, 2 of them deleted, one a photo (media item 1). */
    private fun ayu(mode: InboxMode, subtitle: String): ConversationUiState {
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
