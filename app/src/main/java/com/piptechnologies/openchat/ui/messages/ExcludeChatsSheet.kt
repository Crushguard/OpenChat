package com.piptechnologies.openchat.ui.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.messages.NotificationText
import com.piptechnologies.openchat.ui.components.OcSwitch
import com.piptechnologies.openchat.ui.theme.OcTheme
import com.piptechnologies.openchat.ui.theme.sans

/**
 * §4.11 (ruling R15): "Exclude chats" and a 52 row per known conversation (avatar 30, name, switch; on =
 * excluded). The notification listener skips excluded chats from then on; the count feeds the tool
 * settings row. An unsaved sender's number stays left-to-right ([displayTitle]).
 */
@Composable
fun ExcludeChatsSheetContent(chats: List<ExcludableChat>, onToggle: (ExcludableChat) -> Unit) {
    val c = OcTheme.colors
    SheetColumn {
        SheetTitle(text = stringResource(R.string.exclude_title))
        if (chats.isEmpty()) {
            Text(
                text = stringResource(R.string.exclude_empty),
                style = OcTheme.type.body13_5,
                color = c.muted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(chats, key = { it.key }) { chat ->
                    SheetRowFrame(
                        modifier = Modifier.toggleable(value = chat.excluded, role = Role.Switch, onValueChange = { onToggle(chat) }),
                    ) {
                        ConversationAvatar(
                            initial = NotificationText.initialFor(chat.title),
                            // The tint the inbox gives this conversation (InboxBuilder's colorIndex).
                            colorIndex = Math.floorMod(chat.key.hashCode(), 5),
                            size = 30.dp,
                            textStyle = AvatarInitial30,
                        )
                        SheetRowLabel(text = displayTitle(chat.title), color = c.ink)
                        OcSwitch(checked = chat.excluded)
                    }
                }
            }
        }
    }
}

/** The initial in a 30 avatar: 12/700, the 42 avatar's 15/700 scaled down. */
private val AvatarInitial30 = sans(12.0, FontWeight.Bold)
