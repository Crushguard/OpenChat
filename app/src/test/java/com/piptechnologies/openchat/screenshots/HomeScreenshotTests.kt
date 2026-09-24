package com.piptechnologies.openchat.screenshots

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.platform.CountrySource
import com.piptechnologies.openchat.platform.DetectedCountry
import com.piptechnologies.openchat.ui.components.ConfirmSheetContent
import com.piptechnologies.openchat.ui.components.SheetPreviewFrame
import com.piptechnologies.openchat.ui.home.CountryPickerSheetContent
import com.piptechnologies.openchat.ui.home.HomeCallbacks
import com.piptechnologies.openchat.ui.home.HomeScreen
import com.piptechnologies.openchat.ui.home.HomeToolStatus
import com.piptechnologies.openchat.ui.home.HomeUiState
import com.piptechnologies.openchat.ui.home.notOnWhatsAppSpec
import com.piptechnologies.openchat.ui.navigation.HomeTool
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Home and the three overlays it opens: country sheet, Send-with menu, not-on-WhatsApp sheet (design map §4.3–§4.6). */
class HomeScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    private val savedTimeZone: TimeZone = TimeZone.getDefault()

    /** The recents' "2h" / "Yesterday" labels are computed in the default zone; Fakes writes its times in UTC. */
    @Before
    fun pinTimeZone() {
        TimeZone.setDefault(Fakes.timeZone)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(savedTimeZone)
    }

    private val allApps = listOf(MessagingApp.WHATSAPP, MessagingApp.WHATSAPP_BUSINESS, MessagingApp.TELEGRAM)

    /** Tool rows before notification access and before a second account is linked. */
    private val setUpTools = listOf(
        HomeToolStatus(HomeTool.UNSEEN, "Read here. WhatsApp shows nothing"),
        HomeToolStatus(HomeTool.DELETED_MESSAGES, "Keeps what the sender deleted"),
        HomeToolStatus(HomeTool.MEDIA, "Photos, videos, voice notes, documents"),
        HomeToolStatus(HomeTool.SECOND, "A second WhatsApp inside this app"),
    )

    /** Tool rows with access granted: the Fakes totals (7 unread, 4 deleted, 9 media) and a linked second account. */
    private val liveTools = listOf(
        HomeToolStatus(HomeTool.UNSEEN, "7 unread, seen by no one"),
        HomeToolStatus(HomeTool.DELETED_MESSAGES, "4 messages the sender deleted"),
        HomeToolStatus(HomeTool.MEDIA, "9 items recovered this week"),
        HomeToolStatus(HomeTool.SECOND, "Linked"),
    )

    /** First run: Indonesia, empty field, no recents, tools in their set-up state. */
    private val empty = HomeUiState(
        country = Fakes.id,
        detected = null,
        nationalDigits = "",
        message = "",
        app = MessagingApp.WHATSAPP,
        availableApps = allApps,
        recents = emptyList(),
        firstRun = true,
        tools = setUpTools,
        accessGranted = false,
        countrySheetOpen = false,
        countryQuery = "",
        menuOpen = false,
        notOnWhatsApp = false,
        revealedRecentId = null,
        nowMs = Fakes.now,
    )

    /** "+62 812-3456-7890" pasted: the field reads "812 3456 7890", with a message, the clear button and Send enabled. */
    private val filled = empty.copy(nationalDigits = "81234567890", message = "Hi! Is the blue one still available?")

    @Test
    fun home_empty() {
        paparazzi.snapshot {
            OpenChatTheme {
                // First run opens Home with the number field focused (§4.2): the 1.5 dp green ring.
                val focusRequester = remember { FocusRequester() }
                HomeScreen(state = empty, callbacks = HomeCallbacks(), focusRequester = focusRequester)
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }
    }

    @Test
    fun home_filled() {
        paparazzi.snapshot {
            OpenChatTheme {
                HomeScreen(state = filled, callbacks = HomeCallbacks())
            }
        }
    }

    @Test
    fun home_recents() {
        paparazzi.snapshot {
            OpenChatTheme {
                HomeScreen(
                    state = empty.copy(
                        recents = Fakes.recents,
                        firstRun = false,
                        tools = liveTools,
                        accessGranted = true,
                        // The third row (+62 878 1201 5566, Telegram) slid open over its Delete panel.
                        revealedRecentId = Fakes.recents[2].id,
                    ),
                    callbacks = HomeCallbacks(),
                )
            }
        }
    }

    @Test
    fun country_picker() {
        paparazzi.snapshot {
            OpenChatTheme {
                SheetPreviewFrame(
                    heightFraction = 0.86f,
                    screen = { HomeScreen(state = empty, callbacks = HomeCallbacks()) },
                ) {
                    CountryPickerSheetContent(
                        query = "",
                        current = Fakes.id,
                        detected = DetectedCountry(Fakes.id, CountrySource.SIM),
                        onQuery = {},
                        onPick = {},
                        onClose = {},
                    )
                }
            }
        }
    }

    @Test
    fun send_app_selector() {
        paparazzi.snapshot {
            OpenChatTheme {
                HomeScreen(state = filled.copy(menuOpen = true), callbacks = HomeCallbacks())
            }
        }
    }

    @Test
    fun not_on_whatsapp() {
        paparazzi.snapshot {
            OpenChatTheme {
                SheetPreviewFrame(
                    topRadius = 26.dp,
                    screen = { HomeScreen(state = filled, callbacks = HomeCallbacks()) },
                ) {
                    ConfirmSheetContent(spec = notOnWhatsAppSpec("62", "81234567890"), onCancel = {}, onConfirm = {})
                }
            }
        }
    }
}
