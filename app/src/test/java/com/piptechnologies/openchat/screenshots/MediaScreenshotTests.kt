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
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Deleted media (the Photos grid and the empty Audio tab), the media detail of a photo and its delete
 * confirmation (design map §4.12, §4.13, §4.20), from the §6 fake data. Thumbnails and the preview are the
 * hatched placeholders the design draws. The screens format their times and days in the default zone
 * (rememberTimeFormatter): render them in [Fakes.timeZone].
 */
object MediaScenes {
    /** The 7 recovered photos (2 today, 5 yesterday), newest first. */
    private val photos: List<RecoveredMedia> = Fakes.media.filter { it.category == MediaCategory.PHOTO }

    val recoverMediaGrid = Scene("recover_media_grid") {
        MediaScreen(
            state = media(MediaCategory.PHOTO, MediaDayGrouper.group(photos, Fakes.now, Fakes.timeZone)),
            callbacks = MediaCallbacks(),
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    val mediaEmpty = Scene("media_empty") {
        MediaScreen(
            state = media(MediaCategory.AUDIO, emptyList()),
            callbacks = MediaCallbacks(),
            thumbnail = { _, modifier -> HatchedPlaceholder(modifier = modifier) },
        )
    }

    val recoverMediaDetail = Scene("recover_media_detail") {
        PhotoDetail()
    }

    val dialogDeleteMedia = Scene("dialog_delete_media") {
        SheetPreviewFrame(topRadius = 26.dp, screen = { PhotoDetail() }) {
            ConfirmSheetContent(spec = deleteMediaSpec(MediaCategory.PHOTO), onCancel = {}, onConfirm = {})
        }
    }

    val all: List<Scene> = listOf(recoverMediaGrid, mediaEmpty, recoverMediaDetail, dialogDeleteMedia)

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

    /**
     * The newest photo (today 14:25, deleted 14:26: "From Ayu Lestari · Today 14:25 · Deleted 14:26"), shown as 1 of 3
     * over the design's dark hatched preview.
     */
    @Composable
    private fun PhotoDetail() {
        MediaDetailScreen(
            state = MediaDetailUiState(
                item = photos.first(),
                index = 1,
                total = 3,
                nowMs = Fakes.now,
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

/** [MediaScenes] in English. */
class MediaScreenshotTests {
    @get:Rule
    val paparazzi = ScreenshotDevice.paparazzi()

    private val defaultZone: TimeZone = TimeZone.getDefault()

    /** The screens format their times and days in the default zone (rememberTimeFormatter); the fake wall-clock times are UTC. */
    @Before
    fun useFakeTimeZone() {
        TimeZone.setDefault(Fakes.timeZone)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultZone)
    }

    @Test
    fun recover_media_grid() = paparazzi.snapshot(MediaScenes.recoverMediaGrid)

    @Test
    fun media_empty() = paparazzi.snapshot(MediaScenes.mediaEmpty)

    @Test
    fun recover_media_detail() = paparazzi.snapshot(MediaScenes.recoverMediaDetail)

    @Test
    fun dialog_delete_media() = paparazzi.snapshot(MediaScenes.dialogDeleteMedia)
}
