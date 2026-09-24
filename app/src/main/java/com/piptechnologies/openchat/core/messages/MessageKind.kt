package com.piptechnologies.openchat.core.messages

/** What a captured message carries, read from its notification text by [NotificationText.kindOf]. */
enum class MessageKind { TEXT, PHOTO, VIDEO, VOICE, AUDIO, DOCUMENT, STICKER, GIF, CONTACT, LOCATION, DELETED, OTHER }
