package com.piptechnologies.openchat.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A number a chat was opened with (Home recents): one row per (dialCode, nationalNumber); [app] is a `MessagingApp` name. */
@Entity(tableName = "recent_numbers", indices = [Index(value = ["dialCode", "nationalNumber"], unique = true)])
data class RecentNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dialCode: String,
    val nationalNumber: String,
    val app: String,
    val usedAt: Long,
)
