package com.piptechnologies.openchat.core.media

enum class MediaCategory(val tabLabel: String, val detailTitle: String, val emptyTitle: String) {
    PHOTO("Photos", "Photo", "Nothing recovered yet"),
    VIDEO("Videos", "Video", "Nothing recovered yet"),
    AUDIO("Audio", "Audio", "No audio yet"),
    DOCUMENT("Documents", "Document", "No documents yet"),
    STICKER("Stickers", "Sticker", "No stickers yet");
    companion object { fun fromName(name: String?): MediaCategory = entries.firstOrNull { it.name == name } ?: DOCUMENT }
}
