package com.piptechnologies.openchat.ui.media

import androidx.annotation.StringRes
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.core.media.MediaCategory

// The words for each category, one full string per category and use, so no text is built from a category word.

/** Its pill on Deleted media: "Photos" … "Stickers". */
@get:StringRes
internal val MediaCategory.tabLabelRes: Int
    get() = when (this) {
        MediaCategory.PHOTO -> R.string.media_tab_photos
        MediaCategory.VIDEO -> R.string.media_tab_videos
        MediaCategory.AUDIO -> R.string.media_tab_audio
        MediaCategory.DOCUMENT -> R.string.media_tab_documents
        MediaCategory.STICKER -> R.string.media_tab_stickers
    }

/** The empty state title of its pill: "Nothing recovered yet" (Photos, Videos), "No audio yet" … */
@get:StringRes
internal val MediaCategory.emptyTitleRes: Int
    get() = when (this) {
        MediaCategory.PHOTO, MediaCategory.VIDEO -> R.string.media_empty_nothing
        MediaCategory.AUDIO -> R.string.media_empty_audio
        MediaCategory.DOCUMENT -> R.string.media_empty_documents
        MediaCategory.STICKER -> R.string.media_empty_stickers
    }

/** The media detail title: "Photo" … "Sticker". */
@get:StringRes
internal val MediaCategory.detailTitleRes: Int
    get() = when (this) {
        MediaCategory.PHOTO -> R.string.media_detail_title_photo
        MediaCategory.VIDEO -> R.string.media_detail_title_video
        MediaCategory.AUDIO -> R.string.media_detail_title_audio
        MediaCategory.DOCUMENT -> R.string.media_detail_title_document
        MediaCategory.STICKER -> R.string.media_detail_title_sticker
    }

/** The delete confirmation title: "Delete this photo?" … "Delete this sticker?". */
@get:StringRes
internal val MediaCategory.deleteTitleRes: Int
    get() = when (this) {
        MediaCategory.PHOTO -> R.string.delete_media_title_photo
        MediaCategory.VIDEO -> R.string.delete_media_title_video
        MediaCategory.AUDIO -> R.string.delete_media_title_audio
        MediaCategory.DOCUMENT -> R.string.delete_media_title_document
        MediaCategory.STICKER -> R.string.delete_media_title_sticker
    }
