package com.piptechnologies.openchat.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.core.media.MediaCategory
import com.piptechnologies.openchat.core.media.MediaDayGrouper
import com.piptechnologies.openchat.core.media.RecoveredMedia
import com.piptechnologies.openchat.ui.components.ConfirmSheetContent
import com.piptechnologies.openchat.ui.components.HatchedPlaceholder
import com.piptechnologies.openchat.ui.components.SheetPreviewFrame
import com.piptechnologies.openchat.ui.media.MediaCallbacks
import com.piptechnologies.openchat.ui.media.MediaDetailScreen
import com.piptechnologies.openchat.ui.media.MediaDetailUiState
import com.piptechnologies.openchat.ui.media.MediaScreen
import com.piptechnologies.openchat.ui.media.MediaUiState
import com.piptechnologies.openchat.ui.media.deleteMediaSpec
import com.piptechnologies.openchat.ui.theme.OpenChatTheme
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Deleted media (the Photos grid and the empty Audio tab), the media detail of a photo and its delete
 * confirmation (design map §4.12, §4.13, §4.20), from the §6 fake data. Thumbnails and the preview are the
 * hatched placeholders the design draws.
 */
class MediaScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    private val defaultZone: TimeZone = TimeZone.getDefault()

    /** The tiles format their times in the default zone; the fake wall-clock times are UTC. */
    @Before
    fun useFakeTimeZone() {
        TimeZone.setDefault(Fakes.timeZone)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultZone)
    }

    /** The 7 recovered photos (2 today, 5 yesterday), newest first. */
    private val photos: List<RecoveredMedia> = Fakes.media.filter { it.category == MediaCategory.PHOTO }

    @Test
    fun recover_media_grid() = snapshot {
        MediaScreen(
            state = media(MediaCategory.PHOTO, MediaDayGrouper.group(photos, Fakes.now, Fakes.timeZone)),
            callbacks = MediaCallbacks(),
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    @Test
    fun media_empty() = snapshot {
        MediaScreen(
            state = media(MediaCategory.AUDIO, emptyList()),
            callbacks = MediaCallbacks(),
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    @Test
    fun recover_media_detail() = snapshot {
        PhotoDetail()
    }

    @Test
    fun dialog_delete_media() = snapshot {
        SheetPreviewFrame(topRadius = 26.dp, screen = { PhotoDetail() }) {
            ConfirmSheetContent(spec = deleteMediaSpec(MediaCategory.PHOTO), onCancel = {}, onConfirm = {})
        }
    }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            OpenChatTheme {
                content()
            }
        }
    }

    /** The grid on [tab] with media access granted, recovery active and no sheet open. */
    private fun media(tab: MediaCategory, groups: List<MediaDayGrouper.Group>): MediaUiState = MediaUiState(
        tab = tab,
        groups = groups,
        hasPermission = true,
        paused = false,
        excludedCount = 0,
        toolSettingsOpen = false,
        confirmClear = false,
        nowMs = Fakes.now,
    )

    /** The newest photo (today 14:25, deleted 14:26), shown as 1 of 3 over the design's dark hatched preview. */
    @Composable
    private fun PhotoDetail() {
        MediaDetailScreen(
            state = MediaDetailUiState(
                item = photos.first(),
                index = 1,
                total = 3,
                meta = "From Ayu Lestari · Today 14:25 · Deleted 14:26",
                confirmDelete = false,
            ),
            onBack = {},
            onSave = {},
            onShare = {},
            onAskDelete = {},
            onDismissDelete = {},
            onConfirmDelete = {},
            preview = { _, modifier -> HatchedPlaceholder(modifier = modifier, dark = true) },
        )
    }
}
