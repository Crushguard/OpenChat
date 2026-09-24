package com.piptechnologies.openchat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentNumberDao {
    @Query("SELECT * FROM recent_numbers ORDER BY usedAt DESC LIMIT 5")
    fun observeRecent(): Flow<List<RecentNumberEntity>>

    @Query("SELECT COUNT(*) FROM recent_numbers")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM recent_numbers WHERE dialCode = :dialCode AND nationalNumber = :national LIMIT 1")
    suspend fun find(dialCode: String, national: String): RecentNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentNumberEntity)

    @Query("DELETE FROM recent_numbers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recent_numbers")
    suspend fun clear()

    @Query("DELETE FROM recent_numbers WHERE id NOT IN (SELECT id FROM recent_numbers ORDER BY usedAt DESC LIMIT 5)")
    suspend fun trimToFive()
}
