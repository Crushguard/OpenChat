package com.piptechnologies.openchat.e2e

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.piptechnologies.openchat.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Design map §4.12, §4.13, §5.3 (README "Recover deleted media"): a photo arriving in WhatsApp Images is copied by
 * the real media watcher; once the stand-in deletes the original, the copy shows in Deleted media under today's
 * header, can be saved to the gallery and deleted.
 */
@RunWith(AndroidJUnit4::class)
class MediaFlowTest : E2eTest() {
    @Test
    fun aPhotoDeletedFromTheChatAppIsRecovered_savedAndDeleted() {
        // The watcher starts only with the media permission, and runs with the listener.
        Grants.mediaPermissions()
        Grants.notificationListener(allowed = true)
        launchToHome()

        openTool(R.string.tool_media_title)
        compose.waitFor(hasTextOf(text(R.string.media_title)))
        compose.waitFor(hasTextOf(text(R.string.media_empty_nothing)))

        val name = "IMG-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-WA0001.jpg"
        Fixture.writeMedia(name)
        // The copy lands in the app's files/media (service/MediaCopier), in this very process.
        val copies = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "media")
        Waits.until("OpenChat to copy $name into ${copies.path}", timeoutMs = 60_000, pollMs = 500) {
            copies.listFiles()?.any { it.isFile && it.length() > 0 } == true
        }
        // Still present in the chat app's folder: not listed yet (ruling R7).
        assertFalse("a photo whose original still exists is not listed", compose.exists(mediaTile))

        Fixture.deleteMedia(name)
        compose.waitFor(hasTextOf(text(R.string.media_day_count, text(R.string.time_today), 1), ignoreCase = true), timeoutMs = 60_000)
        val tile = compose.waitFor(mediaTile)
        capture("grid")

        runCatching { tile.performScrollTo() }
        tile.performClick()
        compose.tap(hasTextOf(text(R.string.media_save)))
        compose.waitFor(hasTextOf(text(R.string.toast_saved)))
        capture("detail_saved")

        compose.tap(hasDescriptionOf(text(R.string.media_delete)))
        // The confirmation's Delete button (the trash icon only has a description).
        compose.tap(hasTextOf(text(R.string.media_delete)))
        compose.waitFor(hasTextOf(text(R.string.toast_deleted)))
        compose.waitFor(hasTextOf(text(R.string.media_empty_nothing)))
        assertFalse("the grid is empty", compose.exists(mediaTile))
        capture("grid_empty")
    }

    private companion object {
        /** A grid tile: tappable, with its "HH:mm" time chip. */
        val mediaTile = SemanticsMatcher("a media tile with a time chip") { node ->
            node.config.contains(SemanticsActions.OnClick) && node.texts().any { Regex("""\d{1,2}:\d{2}""").matches(it) }
        }
    }
}
