package com.piptechnologies.openchat.core.send

data class RecentNumber(val id: Long, val dialCode: String, val nationalNumber: String, val app: MessagingApp, val usedAt: Long)
