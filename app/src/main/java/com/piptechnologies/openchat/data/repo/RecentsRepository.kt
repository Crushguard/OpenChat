package com.piptechnologies.openchat.data.repo

import com.piptechnologies.openchat.core.send.MessagingApp
import com.piptechnologies.openchat.core.send.RecentNumber
import com.piptechnologies.openchat.data.db.RecentNumberDao
import com.piptechnologies.openchat.data.db.RecentNumberEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The numbers chats were opened with, shown as Home recents (§5.1). */
interface RecentsRepository {
    /** At most five numbers, newest first. */
    fun observeRecent(): Flow<List<RecentNumber>>
    fun observeCount(): Flow<Int>

    /** Upserts the number (usedAt = now, keeping its existing id) with [app], then keeps only the five newest. */
    suspend fun record(dialCode: String, nationalNumber: String, app: MessagingApp)
    suspend fun delete(id: Long)
    suspend fun clear()
}

class RoomRecentsRepository @Inject constructor(private val dao: RecentNumberDao) : RecentsRepository {
    override fun observeRecent(): Flow<List<RecentNumber>> = dao.observeRecent().map { list -> list.map(::toRecentNumber) }

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override suspend fun record(dialCode: String, nationalNumber: String, app: MessagingApp) {
        val existing = dao.find(dialCode, nationalNumber)
        dao.upsert(
            RecentNumberEntity(
                id = existing?.id ?: 0,
                dialCode = dialCode,
                nationalNumber = nationalNumber,
                app = app.name,
                usedAt = System.currentTimeMillis(),
            )
        )
        dao.trimToFive()
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    override suspend fun clear() {
        dao.clear()
    }
}

private fun toRecentNumber(entity: RecentNumberEntity): RecentNumber = RecentNumber(
    id = entity.id,
    dialCode = entity.dialCode,
    nationalNumber = entity.nationalNumber,
    app = MessagingApp.fromName(entity.app) ?: MessagingApp.WHATSAPP,
    usedAt = entity.usedAt,
)
