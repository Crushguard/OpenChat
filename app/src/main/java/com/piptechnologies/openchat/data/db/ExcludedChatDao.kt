package com.piptechnologies.openchat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedChatDao {
    @Query("SELECT * FROM excluded_chats ORDER BY title")
    fun observeAll(): Flow<List<ExcludedChatEntity>>

    @Query("SELECT COUNT(*) FROM excluded_chats WHERE conversationKey = :key")
    suspend fun count(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExcludedChatEntity)

    @Query("DELETE FROM excluded_chats WHERE conversationKey = :key")
    suspend fun delete(key: String)
}
